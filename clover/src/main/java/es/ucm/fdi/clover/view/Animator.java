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

import es.ucm.fdi.clover.event.StructureChangeEvent;
import es.ucm.fdi.clover.event.StructureChangeListener;
import es.ucm.fdi.clover.layout.LayoutCache;
import es.ucm.fdi.clover.layout.LayoutManager;
import es.ucm.fdi.clover.layout.LayoutCritic;
import es.ucm.fdi.clover.layout.Node;
import es.ucm.fdi.clover.layout.ForceTreeLayout;
import es.ucm.fdi.clover.model.BaseGraph;
import es.ucm.fdi.clover.model.Edge;
import es.ucm.fdi.clover.model.ViewGraph;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import org.jgrapht.Graphs;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgraph.event.GraphModelEvent;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;

import es.ucm.fdi.clover.layout.BoundVarLengthFDL;
import es.ucm.fdi.clover.layout.ConstantEdgeLengthFDL;
import es.ucm.fdi.clover.layout.FTALayout;
import es.ucm.fdi.clover.layout.SimpleBoxLayout;
import es.ucm.fdi.clover.layout.VarLengthFDL;
import es.ucm.fdi.clover.layout.VerticalBoxLayout;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

/**
 * An Animator is in charge of animating changes from one graph to another. 
 * Ideally, the "from" graph will be similar to the "to" graph, and the changes
 * will be small and easy to evidence.
 *
 * Initial layout is also performed by the animator - but in an instant manner.
 *
 * @author mfreire
 */
@SuppressWarnings( { "rawtypes", "unchecked" })
public class Animator implements StructureChangeListener {

	private Log log = LogFactory.getLog(Animator.class);

	/** manager - good to run layouts with */
	protected LayoutManager manager;

	/** view to operate on */
	protected BaseView view;

	/** layout cache (ours), to avoid 
	 * recalculating expensive layouts and provide mental map support 
	 */
	protected LayoutCache layoutCache;

	/** JGraph's layout cache, only used to update current cell positions */
	protected GraphLayoutCache glc;

	/** animation plan; allows incremental steps to be added after each other */
	protected AnimationPlan plan;

	protected VarLengthFDL vlfdl;
	protected BoundVarLengthFDL bvlfdl;
	protected VerticalBoxLayout vbl;
	protected FTALayout ftal;
	protected SimpleBoxLayout sbl;
	protected ConstantEdgeLengthFDL celfdl;

	// settings that get saved & restored
	protected int maxInterpolationTime = 2000;
	protected int initialLayoutArea = 300;
	protected int initialLayoutTime = maxInterpolationTime * 2;
	protected double cacheSloppynessLimit = 0.9;
	protected int incrementalRefinementPasses = 10;

	/**
	 * Creates a new instance of Animator
	 */
	public Animator(BaseView view) {
		vlfdl = new VarLengthFDL();
		bvlfdl = new BoundVarLengthFDL();
		bvlfdl.setMaxIterations(100);
		vbl = new VerticalBoxLayout();
		ftal = new FTALayout();
		sbl = new SimpleBoxLayout();
		celfdl = new ConstantEdgeLengthFDL();
		celfdl.setMaxIterations(80);
		celfdl.setIdealDistance(120);

		manager = new LayoutManager(view);
		layoutCache = new LayoutCache();
		setView(view);
	}

	public void setView(BaseView view) {
		if (this.view != null) {
			BaseGraph base = this.view.getBase();
			if (base != view.getBase()) {
				base.removeStructureChangeListener(this);
			}
		}
		this.view = view;
		glc = view.getGraphLayoutCache();
		view.getBase().addStructureChangeListener(this);
		start();
	}

	public LayoutManager getLayoutManager() {
		return manager;
	}

	public LayoutCache getLayoutCache() {
		return layoutCache;
	}

	public void setLayoutCache(LayoutCache layoutCache) {
		this.layoutCache = layoutCache;
	}

	public void start() {
		if (view.getViewGraph().vertexSet().size() == 0)
			return;

		manager.setNodes(view);

		manager.setAlgorithm(new ForceTreeLayout(120));
		manager.run();
		manager.applyChanges(view);
		manager.setAlgorithm(vlfdl);
		manager.setMaxTime(initialLayoutTime);
		manager.run();
		manager.setAlgorithm(celfdl);
		manager.run();
		manager.setAlgorithm(vbl);
		manager.run();
		manager.setAlgorithm(ftal);
		manager.run();
		manager.setAlgorithm(sbl);
		manager.run();
		manager.applyChanges(view);

		manager.addNodesToCache(layoutCache, view);
	}

	protected InterpolatedMovementStep incrementalLayoutStep(HashSet freeViews,
			int n) {
		return incrementalLayoutStep(freeViews, n, true);
	}

	protected InterpolatedMovementStep incrementalLayoutStep(HashSet freeViews,
			int n, boolean useOldPositions) {
		return incrementalLayoutStep(freeViews, n, true, cacheSloppynessLimit);
	}

	/**
	 * Designed to be called from outside to beautify already layed-out graphs
	 * (without destroying them too much)
	 */
	public void doIncrementalLayout() {
		AnimationPlan ap = new AnimationPlan(view,
				AnimationPlan.RELAYOUT_PRIORITY);
		ap.addStep(incrementalLayoutStep(new HashSet(),
				incrementalRefinementPasses, false, 2));
		log.debug("Starting ANIMATION PLAN to beautify layout");
		ap.run();
	}

	/**
	 * Performs incremental layout and configures an "InterpolatedMovementStep"
	 * to display it.
	 * @param freeViews view cells that should not be constrained to their current
	 *      positions
	 * @param n number of passes to perform
	 * @param useOldPositions if true, the 'current' positions are taken from 
	 *      the last layout performed in the manager (which may not be yet
	 *      visible on-screen). if false, the 'current' positions will be those
	 *      now visible on screen. Use 'true' to chain several layouts.
	 * @param minScore minimum score (in range 0-1) for a cache search to be 
	 *      considered a hit. Negative values mean "don't use cache".
	 * @return an AnimationStep that animates the transition from the old to the
	 *      new layout.
	 */
	protected InterpolatedMovementStep incrementalLayoutStep(HashSet freeViews,
			int n, boolean useOldPositions, double minScore) {

		manager.setNodes(view, useOldPositions);
		Rectangle2D bounds;
		bounds = Node.getBounds(manager.getNodes(), view.getLayoutZoom());
		Map m1 = Node.getChangeMap(manager.getNodes(),
				(int) bounds.getX() - 10, (int) bounds.getY() - 10);

		// check cache for a hit
		double cacheQuality = 0;

		if (minScore >= 0) {
			cacheQuality = manager.setNodesFromCache(layoutCache, view,
					minScore);
			if (cacheQuality > 0 && cacheQuality < 1) {
				log.debug("Using cached layout, old passes = " + n);
				// q=1 => n/=4; q=.63 => n/=1; q<.63, increments
				n = Math.max((int) (n * ((1 - cacheQuality) * 2 + .25)), 2);
				log.debug("\treduced to passes = " + n);
			}
		}

		// if not a perfect hit, some layout is still required
		if (cacheQuality < 1) {
			log.debug("Because cache was not perfect, layout out for n = " + n);

			LayoutCritic q = new LayoutCritic(manager.getNodes());
			q.getQuality();

			manager.setAlgorithm(bvlfdl);
			bvlfdl.setFreeNodes(freeViews, n * 2);
			manager.setMaxTime(maxInterpolationTime);
			manager.run();
			celfdl.setFreeNodes(freeViews, n * 2);
			manager.setAlgorithm(celfdl);
			manager.run();

			if (q.getNodeOverlaps() > 0 || q.getComponentOverlaps() > 0) {
				manager.setAlgorithm(vbl);
				manager.run();
				manager.setAlgorithm(ftal);
				manager.run();
				manager.setAlgorithm(sbl);
				manager.run();
			} else {
				log
						.debug("Avoided having to fix layout; critic said it was fine");
			}
		} else {
			log.debug("REUSING prior layout");
		}

		bounds = Node.getBounds(manager.getNodes(), view.getLayoutZoom());
		Map m2 = Node.getChangeMap(manager.getNodes(),
				(int) bounds.getX() - 10, (int) bounds.getY() - 10);

		// store in cache for later use
		manager.addNodesToCache(layoutCache, view);

		return new InterpolatedMovementStep(m1, m2);
	}

	protected HashSet verticesToViews(Collection vertices) {
		HashSet cellViews = new HashSet();
		JGraphModelAdapter adapter = view.getViewGraph().getModelAdapter();
		for (Object o : vertices) {
			GraphCell c = adapter.getVertexCell(o);
			cellViews.add(glc.getMapping(c, true));
		}
		return cellViews;
	}

	/**
	 * FIXME: once changes get really structured, this should reflect it.
	 */
	public void structureChangePerformed(StructureChangeEvent evt) {
		ViewGraph g = view.getViewGraph();
		HashSet free = new HashSet();

		plan = new AnimationPlan(view, AnimationPlan.STRUCTURE_PRIORITY);

		manager.setNodes(view);

		if (log.isDebugEnabled()) {
			log.debug("Change received: " + evt.getDescription());
		}

		// remove old vertices
		int n = 0;
		for (Object v : evt.getRemovedVertices()) {
			g.removeVertex(v);
			n++;
		}
		if (n > 0) {
			plan.addStep(incrementalLayoutStep(free, n + 2));
		}

		// remove old edges
		free.clear();
		n = 0;
		for (Edge e : evt.getRemovedEdges()) {
			g.removeEdge(e.getSource(), e.getTarget());
			free.add(e.getSource());
			free.add(e.getTarget());
			n++;
		}
		if (n > 0) {
			plan.addStep(incrementalLayoutStep(verticesToViews(free), n + 2));
		}

		// this is good for new-node-pos initialization (see below)
		free.addAll(evt.getAddedVertices());
		manager.setNodes(view, true);
		Map<Object, Rectangle2D> vertexPos = Node.getPositions(manager
				.getNodes(), view);

		// and new edges        
		n = 0;
		for (Edge e : evt.getAddedEdges()) {
			// initialize free end-vertices positions to that of the edge            
			Object src = e.getSource();
			Object dst = e.getTarget();

			//            System.err.println("Edge is "+e+" from "+src+" to "+dst+"; g is "+g+
			//                    ", and "+vertexPos.get(src)+" and "+vertexPos.get(dst));

			Object v = null;
			Object o = null;
			if (free.contains(src)) {
				v = src;
				o = dst;
			}
			if (free.contains(dst)) {
				v = (v == null) ? dst : v;
				o = src;
			}
			if (v != null) {
				if (vertexPos.get(o) != null) {
					g.addVertex(v, (Rectangle2D) vertexPos.get(o).clone());
				} else {
					//g.addVertex(v);                    
					// if things are missing, that's because this is really a clustering event...
					log
							.warn("WARNING: this should not be called (clustering stuff at the base Animator!)");
					//Thread.dumpStack();
					//return;
				}
			}
		}

		// add all new vertices (redundant adds dont trigger anything)
		Graphs.addAllVertices(g, evt.getAddedVertices());

		for (Edge e : evt.getAddedEdges()) {
			g.addEdge(e.getSource(), e.getTarget(), e);
			free.add(e.getSource());
			free.add(e.getTarget());
			n++;
		}
		if (n > 0) {
			plan.addStep(incrementalLayoutStep(verticesToViews(free), n + 3));
		}

		// this highlights a changed node, and updates its labels and stuff
		Map changes = new HashMap();
		for (Object v : evt.getChangedVertices()) {
			DefaultGraphCell c = ViewHelper.getVertexCell(view, v);
			if (c == null) {
				log.warn("Can't find cell for " + v + " in the graph!");
				for (Object v2 : view.getViewGraph().vertexSet()) {
					log.warn(" ... But I found " + v2);
				}
				continue;
			}
			log.debug("Redecorating vertex " + c);
			view.getViewGraph().decorateVertexCell(c);
			changes.put(c, c.getAttributes());
		}
		for (Edge e : evt.getChangedEdges()) {
			DefaultEdge de = ViewHelper.getEdgeCell(view, e);
			view.getViewGraph().decorateEdgeCell(de);
			changes.put(de, de.getAttributes());
		}
		// FIXME : add highlighting
		view.getGraphLayoutCache().edit(changes);

		// run the plan
		if (!plan.getMoves().isEmpty()) {
			log.debug("Starting ANIMATION PLAN to show off "
					+ evt.getDescription());
			plan.run();
		}
	}

	public LayoutManager getManager() {
		return manager;
	}

	/**
	 * Resyncs using a GraphModelEvent event; should be much more efficient
	 * than the alternative
	 */
	public void resyncFromEvent(GraphModelEvent evt) {
		GraphModelEvent.GraphModelChange c = evt.getChange();
		if (c.getRemoved() != null || c.getInserted() != null) {
			if (log.isDebugEnabled()) {
				log.debug("Could not use event " + evt
						+ " incrementally; ignoring");
			}
			return;
		}

		Node[] nodes = manager.getNodes();
		HashMap<Object, Rectangle2D> move = new HashMap<Object, Rectangle2D>();
		for (Object k : c.getPreviousAttributes().keySet()) {
			if (!(k instanceof DefaultGraphCell)) {
				log.warn("Change did not refer to a defaultgraphcell! - was "
						+ k + " (" + k.getClass().getName() + ")");
				continue;
			}
			DefaultGraphCell cell = (DefaultGraphCell) k;
			Rectangle2D bounds = GraphConstants.getBounds((Map) c
					.getPreviousAttributes().get(k));

			if (cell == null || bounds == null) {
				continue;
			}

			//System.err.println("Changed bounds of "+cell+" to "+bounds);
			move.put(ViewHelper.getVertex(cell), bounds);
		}

		if (!move.isEmpty()) {
			Node.setPositions(move, nodes, view);
			manager.addNodesToCache(layoutCache, view);
		}
	}

	/**
	 * Uses current view information to set the current layout; also
	 * stores it in the layout cache
	 */
	public void resync() {
		manager.setNodes(view);
		manager.addNodesToCache(layoutCache, view);
	}

	/**
	 * Used to debug the layout cache in use
	 */
	void dumpLayoutCache(BaseGraph g) {
		System.err.println("Layout cache is:\n" + layoutCache.dump(g));
	}

	/**
	 * Saves the animator's settings to a string
	 */
	public void save(Element e) {
		e.setAttribute("maxInterpolationTime", "" + maxInterpolationTime);
		e.setAttribute("initialLayoutArea", "" + initialLayoutArea);
		e.setAttribute("initialLayoutTime", "" + initialLayoutTime);
		e.setAttribute("cacheSloppynessLimit", "" + cacheSloppynessLimit);
		e.setAttribute("incrementalRefinementPasses", ""
				+ incrementalRefinementPasses);
	}

	/**
	 * Restores the animator's settings from a string
	 */
	public void restore(Element e) {
		maxInterpolationTime = Integer.parseInt(e
				.getAttributeValue("maxInterpolationTime"));
		initialLayoutArea = Integer.parseInt(e
				.getAttributeValue("initialLayoutArea"));
		initialLayoutTime = Integer.parseInt(e
				.getAttributeValue("initialLayoutTime"));
		cacheSloppynessLimit = Float.parseFloat(e
				.getAttributeValue("cacheSloppynessLimit"));
		incrementalRefinementPasses = Integer.parseInt(e
				.getAttributeValue("incrementalRefinementPasses"));
	}
}
