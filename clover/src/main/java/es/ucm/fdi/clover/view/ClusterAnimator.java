/**
 * AC - A source-code copy detector
 *
 *     For more information please visit:  http://github.com/manuel-freire/ac
 *
 * ****************************************************************************
 *
 * This file is part of AC, version 2.x
 *
 * AC is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * AC is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with AC.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * AC - A source-code copy detector
 *
 *     For more information please visit:  http://github.com/manuel-freire/ac
 *
 * ****************************************************************************
 *
 * This file is part of AC, version 2.0
 *
 * AC is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * AC is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with AC.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * ClusterAnimator.java
 *
 * Created on May 6, 2006, 11:36 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.view;

import es.ucm.fdi.clover.event.ClusteringChangeEvent;
import es.ucm.fdi.clover.event.ClusteringChangeListener;
import es.ucm.fdi.clover.event.StructureChangeEvent;
import es.ucm.fdi.clover.layout.Node;
import es.ucm.fdi.clover.model.BaseGraph;
import es.ucm.fdi.clover.model.ClusteredGraph;
import es.ucm.fdi.clover.model.Edge;
import es.ucm.fdi.clover.model.ViewGraph;
import es.ucm.fdi.clover.view.InterpolatedMovementStep.AVertex;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.BorderFactory;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphCell;
import org.jgraph.graph.GraphConstants;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgraph.graph.VertexView;

/**
 * A ClusterAnimator understands and animates ClusteringEvents. These are emmited by 
 * ClusteredGraphs when parts are expanded or collapsed.
 *
 * @author mfreire
 */
public class ClusterAnimator extends Animator implements
		ClusteringChangeListener {

	private Log log = LogFactory.getLog(ClusterAnimator.class);

	private float expandHue = 0.336f; // green
	private float collapseHue = 0f; // red

	/**
	 * Creates a new instance of Animator
	 */
	public ClusterAnimator(ClusterView view) {
		super(view);
	}

	/**
	 * Overloads good old structure-change-performed, to avoid responding
	 * to clustering changes
	 */
	public void structureChangePerformed(StructureChangeEvent evt) {
		if (evt.getChangeType() == StructureChangeEvent.ChangeType.Clustering) {
			// will be notified through a higher-level clustering-changed-event, ignore
		} else {
			log.debug("ClusterAnimator applying received change: "
					+ evt.getChangeType());
			super.structureChangePerformed(evt);
		}
	}

	public void setView(BaseView view) {
		if (this.view != null) {
			BaseGraph base = this.view.getBase();
			if (base != view.getBase()) {
				((ClusteredGraph) view.getBase())
						.removeClusteringChangeListener(this);
			}
		}
		super.setView(view);
		((ClusteredGraph) view.getBase()).addClusteringChangeListener(this);
	}

	/**
	 * Respond to structured expansion/collapse events
	 *
	 *  - First animate collapses, by level, with very little time.
	 *    ([~.5s collapse] & highlight, switch, max .2s layout-think, .5s fix & unhighlight)
	 *  - Then animate expansions
	 *    (switch & highlight, ~.5s layout-think, [~1s expand] & unhighlight)
	 * 
	 * If any of the layout-think steps is found in cache, it is used instead.
	 * All the layout-think steps are stored in cache.
	 */
	public void clusteringChangePerformed(ClusteringChangeEvent evt) {
		log.debug("Received cluster-change-evt: " + evt.getDescription());
		plan = new AnimationPlan(view, AnimationPlan.STRUCTURE_PRIORITY);

		// initial hard sync
		manager.setNodes(view);

		// FIXME: not used for anything; "small" layouts are not being applied
		int actionsLeft = evt.getCollapsed().size() + evt.getExpanded().size();

		// used to store things that should move freely in layouts
		HashSet<VertexView> free = new HashSet<VertexView>();

		ArrayList<ClusteringChangeEvent.ClusteringAction> actionsInLevel = new ArrayList<ClusteringChangeEvent.ClusteringAction>();
		ArrayList<StructureChangeEvent> events = new ArrayList<StructureChangeEvent>();

		// animate collapses
		for (int level = 0; /**/; level++) {
			actionsInLevel.clear();
			events.clear();

			for (ClusteringChangeEvent.ClusteringAction ca : evt.getCollapsed()) {
				if (ca.getLevel() != level)
					continue;
				actionsInLevel.add(ca);
				events.add(ca.getStructureChange());
				actionsLeft--;
			}

			// end condition: all levels processed
			if (actionsInLevel.isEmpty()) {
				break;
			}

			// first you collapse, then you remove & layout
			collapseLevel(actionsInLevel, free, plan);

			plan
					.addStep(new SwitchAndUpdateLayoutStep(events,
							actionsLeft > 0));
		}

		// animate expansions
		for (int level = 0; /**/; level++) {
			actionsInLevel.clear();
			events.clear();

			for (ClusteringChangeEvent.ClusteringAction ca : evt.getExpanded()) {
				if (ca.getLevel() != level)
					continue;
				actionsInLevel.add(ca);
				events.add(ca.getStructureChange());
				actionsLeft--;
			}

			// end condition: all levels processed
			if (actionsInLevel.isEmpty()) {
				break;
			}

			// first you add & layout, and at the same time you highlight
			plan
					.addStep(new SwitchAndUpdateLayoutStep(events,
							actionsLeft > 0));

			expandLevel(actionsInLevel, free, plan);
		}

		log.debug("Final POI deemed to be " + evt.getFinalPoI());
		plan
				.addStep(new ShiftFocusStep(evt.getInitialPoI(), evt
						.getFinalPoI()));

		log
				.debug("Starting ANIMATION PLAN to show off "
						+ evt.getDescription());
		plan.run();
	}

	/**
	 * add some highlights on extinguished nodes, and start to migrate them
	 * towards the center.
	 */
	private void collapseLevel(
			ArrayList<ClusteringChangeEvent.ClusteringAction> actionsInLevel,
			HashSet<VertexView> free, AnimationPlan plan) {

		ViewGraph g = view.getViewGraph();

		// add in some decoration
		ArrayList removed = new ArrayList();
		for (ClusteringChangeEvent.ClusteringAction ca : actionsInLevel) {
			removed.addAll(ca.getStructureChange().getRemovedVertices());
		}
		plan.addStep(new WaitAndHighlightStep(removed, collapseHue));

		// collapse steps, all merged together
		manager.setNodes(view, true);
		Map<Object, Rectangle2D> positions = Node.getPositions(manager
				.getNodes(), view);
		for (ClusteringChangeEvent.ClusteringAction ca : actionsInLevel) {
			Collection toRemove = ca.getStructureChange().getRemovedVertices();
			plan.mergeStep(new WaitAndCollapseStep(toRemove));
		}
	}

	/**
	 * sets a highlight on nodes being added. Moving will be done by the 
	 * layout step we are adding the highlight to.
	 */
	private void expandLevel(
			ArrayList<ClusteringChangeEvent.ClusteringAction> actionsInLevel,
			HashSet<VertexView> free, AnimationPlan plan) {

		ArrayList added = new ArrayList();
		for (ClusteringChangeEvent.ClusteringAction ca : actionsInLevel) {
			StructureChangeEvent sce = ca.getStructureChange();
			added.addAll(sce.getAddedVertices());
		}

		// add in some decoration
		plan.mergeStep(new WaitAndHighlightStep(added, expandHue));
	}

	/**
	 * Just like the normal kind, but no effort to recenter
	 */
	protected InterpolatedMovementStep simpleLayoutStep(HashSet freeViews,
			int n, boolean useOldPositions) {

		manager.setNodes(view, useOldPositions);
		Rectangle2D bounds;

		bounds = Node.getBounds(manager.getNodes(), view.getLayoutZoom());
		bounds.setRect(Math.min(0, bounds.getX()), Math.min(0, bounds.getY()),
				bounds.getHeight(), bounds.getWidth());
		Map m1 = Node.getChangeMap(manager.getNodes(),
				(int) bounds.getX() - 10, (int) bounds.getY() - 10);

		manager.setAlgorithm(bvlfdl);
		bvlfdl.setFreeNodes(freeViews, n * 2);
		manager.setMaxTime(super.maxInterpolationTime);
		manager.run();
		manager.setAlgorithm(celfdl);
		manager.run();
		manager.setAlgorithm(ftal);
		manager.run();

		bounds = Node.getBounds(manager.getNodes(), view.getLayoutZoom());
		bounds.setRect(Math.min(0, bounds.getX()), Math.min(0, bounds.getY()),
				bounds.getHeight(), bounds.getWidth());
		Map m2 = Node.getChangeMap(manager.getNodes(),
				(int) bounds.getX() - 10, (int) bounds.getY() - 10);

		return new InterpolatedMovementStep(m1, m2);
	}

	private class SwitchAndUpdateLayoutStep extends TwoPhaseStep {
		private ArrayList<StructureChangeEvent> events;
		private boolean small;

		public SwitchAndUpdateLayoutStep(
				ArrayList<StructureChangeEvent> events, boolean small) {
			this.events = new ArrayList<StructureChangeEvent>(events);
			this.small = small;
		}

		public void init() {
			ViewGraph g = view.getViewGraph();
			HashSet free = new HashSet();
			manager.setNodes(view);
			Map<Object, Rectangle2D> pos = Node.getPositions(
					manager.getNodes(), view);
			for (StructureChangeEvent sce : events) {
				g.removeAllVertices(sce.getRemovedVertices());
				Rectangle2D dest = new Rectangle2D.Float();
				// collapse: dest is where any of its children is
				// expand: dest is where the parent is - no need to distinguish!
				dest = pos.get(sce.getRemovedVertices().get(0));
				for (Object v : sce.getAddedVertices()) {
					g.addVertex(v, dest);
				}
				for (Edge e : sce.getAddedEdges()) {
					g.addEdge(e.getSource(), e.getTarget(), e);
				}
				// no need to remove edges, as they all connected to removed vertices...

				free.addAll(sce.getAddedVertices());
				for (Edge e : sce.getAddedEdges()) {
					free.add(e.getSource());
					free.add(e.getTarget());
				}
			}

			AnimationStep step = incrementalLayoutStep(verticesToViews(free),
					2 + free.size(), false);
			setDelegate(step);
		}
	}

	private class WaitAndHighlightStep extends AbstractStep {
		private ArrayList vertices;
		private Color color;
		private float hue;

		public WaitAndHighlightStep(ArrayList vertices, float hue) {
			this.vertices = vertices;
			this.hue = hue;
		}

		public void init() {
			ViewGraph g = view.getViewGraph();
			setCellsFromVertices(vertices, view);
		}

		public void perform(float completion, Map changeMap) {
			color = Color.getHSBColor(hue, 1 - completion, 1);
			super.perform(completion, changeMap);
		}

		public void interpolate(GraphCell cell, float p, Map changeMap) {
			Map map = (changeMap.containsKey(cell)) ? (Map) changeMap.get(cell)
					: new HashMap();

			if (p > .99f) {
				GraphConstants
						.setBorder(map, BorderFactory.createEmptyBorder());
			} else {
				GraphConstants.setBorder(map, BorderFactory.createLineBorder(
						color, 3));
			}
			changeMap.put(cell, map);
		}
	}

	private class ShiftFocusStep implements AnimationStep {
		private Object oldFocus;
		private Object nextFocus;

		public ShiftFocusStep(Object oldFocus, Object nextFocus) {
			this.oldFocus = oldFocus;
			this.nextFocus = nextFocus;
		}

		public void terminate(Map changeMap) {
			DefaultGraphCell co = ViewHelper.getVertexCell(view, oldFocus);
			DefaultGraphCell cn = ViewHelper.getVertexCell(view, nextFocus);
			Map map = new HashMap();

			// hide old
			if (co != null) {
				view.getViewGraph().decorateVertexCell(co, false);
				changeMap.put(co, co.getAttributes());
			} else {
				// ignore. Not worth it to print message
				log.warn("Cannot highlight: invisible next focus cell '"
						+ oldFocus + "'");
			}

			// show new
			if (cn != null) {
				view.getViewGraph().decorateVertexCell(cn, true);
				changeMap.put(cn, cn.getAttributes());
			} else {
				log.warn("Cannot highlight: invisible next focus cell '"
						+ nextFocus + "'");
			}

			nextFocus = null;
		}

		public void perform(float completion, Map changeMap) {
			terminate(changeMap);
		}

		public boolean isFinished() {
			return nextFocus != null;
		}

		public long getDuration() {
			return 0;
		}

		public long getRate() {
			return 100;
		}

		public void init() {
		}
	}

	private class WaitAndCollapseStep extends InterpolatedMovementStep {
		private Collection removed;

		public WaitAndCollapseStep(Collection removed) {
			this.removed = removed;
		}

		public void init() {
			Map<Object, Rectangle2D> positions = Node.getPositions(manager
					.getNodes(), view);
			double maxDisplacement = 0;
			Point2D center = Node.getCenterCoords(positions, removed);
			for (Object v : removed) {
				Object cell = ViewHelper.getVertexCell(view, v);
				if (cell != null) {
					AVertex av = new AVertex(cell, positions.get(v), center);
					vertices.add(av);
					maxDisplacement = Math.max(maxDisplacement, av
							.getDisplacement());
				} else {
					log.warn("Cannot animate removal of non-displayed vertex "
							+ v);
				}
			}
			duration = (long) (rate * maxDisplacement / maxShiftPerFrame);
		}
	}

	public float getExpandHue() {
		return expandHue;
	}

	public float getCollapseHue() {
		return collapseHue;
	}
}