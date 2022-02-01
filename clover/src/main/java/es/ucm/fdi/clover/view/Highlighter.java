/*
 * AC - A source-code copy detector
 *
 *     For more information please visit: http://github.com/manuel-freire/ac2
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
package es.ucm.fdi.clover.view;

import es.ucm.fdi.clover.event.ClusteringChangeEvent;
import es.ucm.fdi.clover.model.ViewGraph;
import es.ucm.fdi.clover.model.Cluster;
import es.ucm.fdi.clover.model.Edge;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphCell;
import org.jgraph.graph.GraphConstants;

/**
 * Applies or removes a highlight of the specified type to the specified vertices or edges.
 * Includes methods to calculate what to highlight or not for given clustering events.
 *
 * @author mfreire
 */
public class Highlighter {

	private Log log = LogFactory.getLog(Highlighter.class);

	/** the view this operates on */
	private BaseView view;
	/** currently highlighted edges */
	private HashSet<Edge> hEdges;
	/** currently highlighted vertices */
	private HashSet hVertices;

	/** the last animation plan created by this highlighter */
	private AnimationPlan lastPlan;
	/** the current animation plan (not yet in execution) */
	private AnimationPlan plan;
	/** the current animation token (to be used to avoid starting several plans for the same thing) */
	private Object animToken;
	/** if 'true', then there is something under highlight */
	private boolean highlighting;

	/** used to avoid losing the original line width when highlighting edges */
	private static String ORIG_LINE_WIDTH = "orig-line-width";

	public static float defaultHighlightVertexHue = 0.15f;

	/**
	 * Creates a new instance of ClusteringEventHighlight
	 */
	public Highlighter(BaseView view) {
		hEdges = new HashSet<Edge>();
		hVertices = new HashSet<Object>();
		this.view = view;
		highlighting = false;
		animToken = null;
	}

	/**
	 * resets the highlighting
	 * @return true if a new highlighting can be started
	 *         false if no highlighting should be done 
	 *  ('cause there's already another plan in execution)
	 */
	public boolean resetHighlight() {
		AnimationPlan c = view.getCurrentPlan();
		if (c != null && c.getPriority() >= AnimationPlan.ROLLOVER_PRIORITY) {
			log.debug("No highlight, current is more important: "
					+ c.getDescription());
			return false;
		}

		clearHighlight();
		plan = new SimpleHighlightPlan(view);
		hEdges.clear();
		hVertices.clear();
		return true;
	}

	public void addEdges(Collection<Edge> edges) {
		hEdges.addAll(edges);
		if (edges.isEmpty()) {
			return;
		}
		ArrayList<GraphCell> edgeCells = new ArrayList<GraphCell>();
		for (Edge e : edges) {
			edgeCells.add(ViewHelper.getEdgeCell(view, e));
		}
		HighlightEdgesStep hes = new HighlightEdgesStep(edgeCells, true);
		plan.mergeStep(hes);
	}

	public void addVertices(Collection vertices, float hue) {
		if (vertices.isEmpty()) {
			return;
		}
		hVertices.addAll(vertices);
		HighlightVerticesStep hvs = new HighlightVerticesStep(vertices, hue,
				true);
		plan.mergeStep(hvs);
	}

	/**
	 * Starts the currently configured highlight; should be preceeded by "resetHightlight" + 
	 * a set of addEdges and addVertices.
	 */
	public void startHighlight() {
		synchronized (view) {
			plan.run();
			highlighting = true;
			autoAbort(plan);
			lastPlan = plan;
			plan = null;
		}
		log.debug("Started a new highlight");
	}

	/**
	 * Tries to kill the plan after 2s by using a timer.
	 */
	private Timer abortTimer = null;
	private AbortListener abortListener = null;

	public void autoAbort(AnimationPlan p) {
		if (abortTimer == null) {
			if (abortListener == null) {
				abortListener = new AbortListener(p);
			}
			abortTimer = new Timer(2000, abortListener);
			abortTimer.setRepeats(false);
		}
		abortTimer.restart();
	}

	private class AbortListener implements ActionListener {
		private AnimationPlan p;

		private AbortListener(AnimationPlan p) {
			this.p = p;
		}

		public void actionPerformed(ActionEvent evt) {
			if (lastPlan == p && highlighting) {
				clearHighlight();
			}
		}
	}

	/**
	 * Clear current highlight (called by resetHighlight)
	 */
	public void clearHighlight() {
		synchronized (view) {

			if (!highlighting) {
				return;
			}
			highlighting = false;
			animToken = null;
			AnimationPlan current = view.getCurrentPlan();

			if (current == null || current instanceof SimpleHighlightPlan) {
				if (current != null) {
					current.terminate();
				}
				if (lastPlan.getMoves().isEmpty()) {
					lastPlan = null;
					return;
				} else {
					for (AnimationStep step : lastPlan.getMoves().get(0)) {
						((Reversible) step).reverse();
					}
					lastPlan.run();
					log.debug("Stopped the current highlight");
				}

			}
		}
	}

	/**
	 * Launches a highlight plan for a DOI change 
	 * centered on the vertex specified by 'cell'; if no highlight would
	 * result, and the vertex is visible, highlights the vertex instead
	 */
	public void startFocusChangePlan(DefaultGraphCell cell) {

		synchronized (view) {
			// check if other plan has already started with this, or we aren't allowed now
			if (animToken == cell || !resetHighlight()) {
				return;
			}
			animToken = cell;

			ViewGraph g = view.getViewGraph();

			if (cell != null && !ViewHelper.isVertex(cell)) {
				log
						.debug("Changing focus to a non-vertex cell is not supported (view is "
								+ view + ")");
				return;
			}

			Object v = null;
			v = ViewHelper.getVertex(cell);
			if (!(view.getViewGraph().containsVertex(v))) {
				log
						.debug("Changing focus to a non-visible cell is not supported");
				return;
			}

			// calculate affected vertices
			if (!(view instanceof ClusterView)) {
				throw new UnsupportedOperationException(
						"Focus changes not supported on non-clustered graphs");
			}

			ArrayList<Edge> edges = new ArrayList<Edge>();
			edges.addAll(((Set<Edge>) g.edgesOf(v)));
			addEdges(edges);

			ClusteringChangeEvent cce = ((ClusterView) view)
					.getPoIChangeEventFor(v);
			cce.commit(null);

			if (cce.getCollapsed().isEmpty() && cce.getExpanded().isEmpty()) {
				startSimpleVertexHighlightPlan(v, defaultHighlightVertexHue,
						true);
				return;
			}

			continueClusteringChangePlan(cce);
		}
	}

	/**
	 * Starts a highlight plan for a given clustering event
	 */
	public void startClusteringChangePlan(ClusteringChangeEvent cce,
			Object token) {
		synchronized (view) {
			// check if other plan has already started with this, or we aren't allowed now
			if (animToken == token || !resetHighlight()) {
				return;
			}
			animToken = token;

			cce.commit(null);
			continueClusteringChangePlan(cce);
		}
	}

	/**
	 * Adds highlighting to a vertex, and all its outgoing & incoming edges
	 */
	public void startSimpleVertexHighlightPlan(Object vertex, float vertexHue) {
		startSimpleVertexHighlightPlan(vertex, vertexHue, false);
	}

	/**
	 * Adds highlighting to a vertex, and all its outgoing & incoming edges
	 */
	private void startSimpleVertexHighlightPlan(Object vertex, float vertexHue,
			boolean skipInitialChecks) {
		synchronized (view) {

			if (!skipInitialChecks) {
				// check if other plan has already started with this, or we aren't allowed now
				if (animToken == vertex || !resetHighlight()) {
					return;
				}
				animToken = vertex;
			}

			ArrayList vertices = new ArrayList();
			vertices.add(vertex);
			ArrayList edges = new ArrayList();
			try {
				edges.addAll(view.getViewGraph().edgesOf(vertex));
			} catch (Exception e) {
				log.warn("Error trying to highlight; aborting highlight.", e);
				animToken = null;
				return;
			}
			addVertices(vertices, vertexHue);
			addEdges(edges);
			startHighlight();
		}
	}

	/**
	 * Adds highlighting for a clustering event to an already-initialized
	 * plan
	 */
	public void continueClusteringChangePlan(ClusteringChangeEvent cce) {

		log.debug("cce: " + cce.getDescription());
		HashSet vertices = new HashSet();
		HashSet collapsed = new HashSet();
		HashSet expanded = new HashSet();
		for (ClusteringChangeEvent.ClusteringAction ca : cce.getCollapsed()) {
			Cluster old = ca.getCluster();
			for (int i = 0; i < old.getChildCount(); i++) {
				collapsed.add(((Cluster) old.getChildAt(i)).getVertex());
			}
		}
		ViewGraph g = view.getViewGraph();
		collapsed.retainAll(g.vertexSet());
		vertices.addAll(collapsed);
		for (ClusteringChangeEvent.ClusteringAction ca : cce.getExpanded()) {
			if (ca.getLevel() > 0)
				continue;

			vertices.add(ca.getCluster().getVertex());
		}
		expanded.addAll(vertices);
		expanded.removeAll(collapsed);

		float expandHue = ((ClusterAnimator) view.getAnimator()).getExpandHue();
		addVertices(expanded, expandHue);

		float collapseHue = ((ClusterAnimator) view.getAnimator())
				.getCollapseHue();
		addVertices(collapsed, collapseHue);

		startHighlight();
	}

	/**
	 * A reversible step that highlights a series of edges
	 */
	private static class HighlightEdgesStep extends AbstractStep implements
			Reversible {
		public boolean forward;

		public HighlightEdgesStep(ArrayList<GraphCell> cells, boolean forward) {
			setCells(cells);
			this.forward = forward;
		}

		public void terminate(Map changeMap) {
			super.perform(0, changeMap);
		}

		public void perform(float completion, Map changeMap) {
			if (!forward) {
				completion = 1 - completion;
			}
			super.perform(completion, changeMap);
		}

		public void interpolate(GraphCell cell, float p, Map changeMap) {
			Float F;
			float f;

			if ((F = (Float) cell.getAttributes().get(ORIG_LINE_WIDTH)) == null) {
				f = GraphConstants.getLineWidth(cell.getAttributes());
				cell.getAttributes().put(ORIG_LINE_WIDTH, f);
			} else {
				f = (float) F;
			}

			HashMap map = new HashMap();

			if (p == 0) {
				GraphConstants.setLineWidth(map, f);
			} else {
				GraphConstants.setLineWidth(map, (float) (f * (2 * p + 1)));
			}
			changeMap.put(cell, map);
		}

		public void reverse() {
			forward = !forward;
		}

		public long getDuration() {
			return 500;
		}
	}

	/**
	 * A reversible step that highlights a series of vertices
	 */
	private class HighlightVerticesStep extends AbstractStep implements
			Reversible {
		private float hue;
		private Color color;
		private float currentP;
		private boolean forward;

		public HighlightVerticesStep(Collection vertices, float hue,
				boolean forward) {
			setCellsFromVertices(vertices, view);
			this.hue = hue;
			this.forward = forward;
		}

		public void perform(float completion, Map changeMap) {
			if (!forward) {
				completion = 1 - completion;
			}
			color = Color.getHSBColor(hue, completion, 1);
			super.perform(completion, changeMap);
		}

		public void terminate(Map changeMap) {
			super.perform(0, changeMap);
		}

		public void interpolate(GraphCell cell, float p, Map changeMap) {
			if (cell == null) {
				log.warn("trying to highlight a non-existing vertex");
				return;
			}
			Map map = (changeMap.containsKey(cell)) ? (Map) changeMap.get(cell)
					: new HashMap();
			Border b = GraphConstants.getBorder(cell.getAttributes());
			if (p == 0) {
				if (b instanceof CompoundBorder) {
					GraphConstants.setBorder(map, BorderFactory
							.createCompoundBorder(((CompoundBorder) b)
									.getOutsideBorder(), BorderFactory
									.createEmptyBorder()));
				} else {
					GraphConstants.setBorder(map, BorderFactory
							.createEmptyBorder());
				}
			} else {
				if (b instanceof CompoundBorder) {
					GraphConstants.setBorder(map, BorderFactory
							.createCompoundBorder(((CompoundBorder) b)
									.getOutsideBorder(), BorderFactory
									.createLineBorder(color, 3)));
				} else {
					GraphConstants.setBorder(map, BorderFactory
							.createLineBorder(color, 3));
				}
			}
			changeMap.put(cell, map);
		}

		public void reverse() {
			forward = !forward;
		}

		public long getDuration() {
			return 500;
		}
	}

	/**
	 * An interface that these steps implement; they're reversible
	 */
	private interface Reversible {
		void reverse();
	}

	/**
	 * A simple tagging class to know that this plan can be easily interrupted
	 */
	private class SimpleHighlightPlan extends AnimationPlan {
		private SimpleHighlightPlan(BaseView view) {
			super(view, AnimationPlan.ROLLOVER_PRIORITY);
		}
	}
}
