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
package es.ucm.fdi.clover.model;

import es.ucm.fdi.clover.event.ClusteringChangeEvent;
import es.ucm.fdi.clover.event.ClusteringChangeListener;
import es.ucm.fdi.clover.event.HierarchyChangeEvent;
import es.ucm.fdi.clover.event.HierarchyChangeListener;
import es.ucm.fdi.clover.event.StructureChangeEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgrapht.Graphs;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

/**
 * A clustered graph maintains a clustering hierarchy over its base graph.
 * The hierarchy is incrementally updated when changes occur upstream, and may
 * be shared with other ClusterGraphs.
 *
 * What is *not* shared is the nodes and vertices shown, which occur after some
 * kind of DegreeOfImportance algorithm is applied to the clustering and 
 * the current position.
 *
 * All structural changes should go to to the base graph. Changes to the base
 * graph will then result in changes to the ClusterHierarchy, which will trigger
 * a HierarchyChangeEvent that can alter this graph. If that is the case,
 * suitable StructureChange s will be passed on to would-be listeners.
 *
 * @author mfreire
 */
@SuppressWarnings( { "unchecked", "rawtypes" })
public class ClusteredGraph extends BaseGraph implements
		HierarchyChangeListener, ClusteringChangeListener {

	private Log log = LogFactory.getLog(ClusteredGraph.class);

	/** the graph & hierarchy we represent */
	private ClusterHierarchy hierarchy;

	/** the root of the hierarchy, for convenience */
	private Cluster root;

	/** this is the currently visible slice */
	private Slice slice;

	/** this is the current 'point of interest' within the slice */
	private Object pointOfInterest;

	/** clustering listeners */
	private ArrayList<ClusteringChangeListener> clusteringListeners;

	/**
	 * The hierarchy must already be registered with the BaseGraph
	 * Creates a new instance of ClusteredGraph
	 */
	public ClusteredGraph(ClusterHierarchy hierarchy) {
		clusteringListeners = new ArrayList<ClusteringChangeListener>();
		setHierarchy(hierarchy);
	}

	public void addClusteringChangeListener(ClusteringChangeListener l) {
		clusteringListeners.add(l);
	}

	public void removeClusteringChangeListener(ClusteringChangeListener l) {
		clusteringListeners.remove(l);
	}

	/**
	 * Notify listeners of changes to clustering
	 */
	protected void fireClusteringChangeEvt(ClusteringChangeEvent evt) {
		for (ClusteringChangeListener l : clusteringListeners) {
			l.clusteringChangePerformed(evt);
		}
	}

	/**
	 * updates the graph when the hierarchy changes as a result of 
	 * upstream changes. Will probably trigger
	 * a cascaded structure-change event. Note that hierarchy changes 
	 * also reflect any changes to graph connectivity.
	 */
	public void hierarchyChangePerformed(HierarchyChangeEvent hce) {

		if (log.isDebugEnabled()) {
			log.debug("Processing a CHCEvent: " + hce.getDescription());
		}

		Object nextPoi = hce
				.getVisibleRepresentativeFor(pointOfInterest, slice);

		// if only the "changed" set is non-empty, this is a trivial change
		if (hce.getAddedClusters().isEmpty()
				&& hce.getRemovedClusters().isEmpty()
				&& hce.getAddedEdges().isEmpty()
				&& hce.getRemovedEdges().isEmpty()) {

			log.debug("\t A trivial event: easy");

			StructureChangeEvent sce = new StructureChangeEvent(this,
					StructureChangeEvent.ChangeType.AttributeOnly);
			for (Cluster c : slice) {
				if (hce.getChangedClusters().contains(c)) {
					sce.getChangedVertices().add(c.getVertex());
				}
			}
			structureChangePerformed(sce);
			log.info("Clustered graph processed simple-hce");
			return;
		}

		log.debug("\t Non trivial event...");

		if (log.isDebugEnabled()) {
			log.debug("<<BEFORE>>\nDumping slice: ");
			for (Cluster c : slice) {
				log.debug("\t" + getId(c.getVertex()) + " " + c);
			}

			log.debug("\nDumping edges: ");
			for (Edge e : (Set<Edge>) edgeSet()) {
				log.debug("\t" + getId(e.getSource()) + " "
						+ getId(e.getTarget()) + " ("
						+ e.getClass().getSimpleName() + ")");
			}
		}

		StructureChangeEvent sce = new StructureChangeEvent(this);
		Cluster root = hierarchy.getRoot();

		// change changed stuff (only if whatever changed was visible)
		for (Cluster c : slice) {
			if (hce.getChangedClusters().contains(c)) {
				sce.getChangedVertices().add(c.getVertex());
			}
		}

		// remove currently-visible but removed clusters & edges
		for (ArrayList<Cluster> l : hce.getRemovedClusters().values()) {
			for (Cluster c : l) {
				// if it was readded, then it must have a match
				if (hce.getMatchedClusters().containsValue(c))
					continue;

				// otherwise, remove self or descendants.
				if (slice.contains(c)) {
					sce.getRemovedVertices().add(c.getVertex());
					slice.remove(c);
				} else {
					for (Cluster d : slice.getDescendantsOf(c)) {
						sce.getRemovedVertices().add(d.getVertex());
						slice.remove(d);
					}
				}
			}
		}

		// fill in gaps in the slice left after removals; better than 'addedClusters'
		HashSet<Cluster> tmp = new HashSet<Cluster>(slice.findHoles(root));
		for (Cluster h : tmp) {
			log.debug("Found slice hole: " + h.getListing(getBase()));
			slice.add(h);
			sce.getAddedVertices().add(h.getVertex());
		}

		if (log.isDebugEnabled()) {
			log
					.debug("The hierarchy is now "
							+ getHierarchy().getRoot().dump());
		}

		// augment with the representatives of recently prolific parents
		for (Cluster p : hce.getAddedClusters().keySet()) {
			Cluster r = slice.getRepresentativeFor(p);
			if (r != null)
				tmp.add(r);
		}

		// add newly visible edges (holes-or-parents)
		for (Cluster a : tmp) {
			Object av = a.getVertex();
			boolean isHole = !containsVertex(av);
			boolean avIsCluster = av instanceof Cluster.Vertex;
			log.debug("Considering what to do with " + a.getListing(getBase())
					+ "... (hole=" + isHole + ")");

			// find missing edges between suspects (added/change-added) & oldies
			for (Cluster oc : slice) {
				Object ocv = oc.getVertex();
				boolean ocvIsCluster = ocv instanceof Cluster.Vertex;
				if (a != oc) {
					log.debug("Comparing against " + oc.getListing(getBase())
							+ "...");

					// check from new to existing...
					if (a.hasEdgesTo(oc) && (isHole || !containsEdge(av, ocv))) {
						Edge e = (avIsCluster || ocvIsCluster) ? new Edge(av,
								ocv) : a.edgesTo(oc).iterator().next();
						log.debug("Adding hole-edge: " + e);
						sce.getAddedEdges().add(e);
					}
					// check the other way around...
					if (oc.hasEdgesTo(a) && (isHole || !containsEdge(ocv, av))) {
						Edge e = (avIsCluster || ocvIsCluster) ? new Edge(ocv,
								av) : oc.edgesTo(a).iterator().next();
						log.debug("Adding hole-edge: " + e);
						sce.getAddedEdges().add(e);
					}
				}
			}
		}

		// remove/change edges (because of node removals and/or edge got removed)
		for (Edge ce : (Set<Edge>) edgeSet()) {

			Set<Edge> edges = Cluster.componentEdgesOf(root, ce);
			// returned if one of those endpoints is not attached to root
			if (edges == null) {
				continue;
			}

			int size = edges.size();
			edges.removeAll(hce.getRemovedEdges());
			if (edges.isEmpty()) {
				log.debug("Removing remove-edge: " + ce);
				sce.getRemovedEdges().add(ce);
			} else if (edges.size() < size) {
				// removed only some components: simply refresh
				log.debug("Removing remove-edge but still cluster: " + ce);
				sce.getChangedEdges().add(ce);
			}
		}

		// add newly-introduced edges (if both endpoints are already visible)
		for (Edge e : hce.getAddedEdges()) {
			Cluster source = root.clusterForVertex(e.getSource());
			Cluster target = root.clusterForVertex(e.getTarget());
			source = slice.getRepresentativeFor(source);
			target = slice.getRepresentativeFor(target);
			if (source != null && target != null && source != target) {
				// created here so we can check dupe 'edge addition'
				Edge ce = new Edge(source.getVertex(), target.getVertex());
				if (sce.getAddedEdges().contains(ce)) {
					log.warn("Edge is duped in the addedEdges section " + ce);
				}

				if (!sce.getAddedEdges().contains(ce)
						&& getEdge(source, target) == null
						&& source.hasEdgesTo(target)) {
					// add a simple edge
					if (source.edgesTo(target).size() == 1
							&& source.isLeafCluster() && target.isLeafCluster()) {
						log.debug("Added base edge for added-edge");
						sce.getAddedEdges().add(e);
					} else {
						log.debug("Added cluster edge for added-edge");
						sce.getAddedEdges().add(ce);
					}
				}
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("ClusteredGraph generates sce from hce: "
					+ sce.getDescription());
		}

		// refine the change (if add & delete same thing, do not add or delete it)
		refineStructureChange(sce, hce);

		// and now, execute the change; focus may or may not be found in animation...
		try {
			structureChangePerformed(sce);
		} catch (Exception e) {
			log.error("Error applying structure change on " + toString(), e);
		}

		// set the PoI; this will cause another animation, and the focus will be set
		setPointOfInterest(nextPoi);

		if (log.isDebugEnabled()) {
			log.debug("<<AFTER>>\nDumping slice: ");
			for (Cluster c : slice) {
				log.debug("\t" + getId(c.getVertex()) + " " + c);
			}

			log.debug("\nDumping edges: ");
			for (Edge e : (Set<Edge>) edgeSet()) {
				log.debug("\t" + getId(e.getSource()) + " "
						+ getId(e.getTarget()) + " ("
						+ e.getClass().getSimpleName() + ")");
			}
		}
		log.info("Clustered graph processed non-trivial HCE");
	}

	/**
	 * To move something around, it may have been added & replaced; should not 
	 * really repaint if this is the case.
	 */
	private void refineStructureChange(StructureChangeEvent sce,
			HierarchyChangeEvent hce) {
		ArrayList<Object> doNotTouch = new ArrayList<Object>(sce
				.getRemovedVertices());
		doNotTouch.retainAll(sce.getAddedVertices());
		sce.getRemovedVertices().removeAll(doNotTouch);
		sce.getAddedVertices().removeAll(doNotTouch);
	}

	/**
	 * updates the graph when a change is performed to the current clustering.
	 * For instance, a set of clusters is to be expanded or collapsed.
	 */
	public void clusteringChangePerformed(ClusteringChangeEvent evt) {

		// change PoI now, before firing change
		pointOfInterest = evt.getFinalPoI();

		// perform changes & fire normal event to everybody
		evt.commit(this);

		// fire events to listeners only interested in clustering
		fireClusteringChangeEvt(evt);
	}

	public BaseGraph getBase() {
		return hierarchy.getBase();
	}

	public ClusterHierarchy getHierarchy() {
		return hierarchy;
	}

	/**
	 * Changes the hierarchy in use for another one. This involves
	 * removing all previous vertices and substituting all of them
	 * for the new root cluster (now focused), and notifying all 
	 * listeners of the change.
	 */
	public void setHierarchy(ClusterHierarchy hierarchy) {
		if (this.hierarchy != null) {
			this.hierarchy.removeHierarchyChangeListener(this);
		}
		this.hierarchy = hierarchy;
		root = hierarchy.getRoot();
		slice = new Slice();
		slice.add(root);

		StructureChangeEvent sce = new StructureChangeEvent(this);
		sce.getRemovedVertices().addAll(vertexSet());
		sce.getAddedVertices().add(root.getVertex());

		pointOfInterest = root.getVertex();

		// commit the change to self & everybody else
		structureChangePerformed(sce);

		hierarchy.addHierarchyChangeListener(this);
	}

	public Slice getSlice() {
		return slice;
	}

	/**
	 * Simulates a collapse of 'c', annotating all changes into the provided 'sce'
	 */
	public void prepareCollapse(Cluster c, StructureChangeEvent sce) {
		log.debug("Collapsing '" + c + "' ...");

		sce.getAddedVertices().add(c.getVertex());
		log.debug("Added vertex: " + c.getVertex());

		ArrayList<Cluster> children = slice.collapse(c);
		for (Cluster child : children) {
			log.debug("Removing " + child.getVertex() + "...");

			// fix outgoing edges
			for (Edge out : (Set<Edge>) outgoingEdgesOf(child.getVertex())) {
				Object target = getEdgeTarget(out);
				if (c.clusterForVertex(target) == null) {
					// dest is outside cluster: a new edge
					Cluster dst = root.clusterForVertex(target);
					Edge e = new Edge(c.getVertex(), dst.getVertex());
					sce.getAddedEdges().add(e);
					log.debug(" added outgoing ext: " + e);
				} else {
					log.debug("Target contained: " + out.getTarget() + " is "
							+ c.clusterForVertex(out.getTarget()));
				}
				// old edge vanishes
				sce.getRemovedEdges().add(out);
				log.debug(" removed outgoing: " + out);
			}
			// fix incoming edges
			for (Edge in : (Set<Edge>) incomingEdgesOf(child.getVertex())) {
				if (c.clusterForVertex(in.getSource()) == null) {
					// dest is outside cluster: a new edge
					Cluster src = root.clusterForVertex(in.getSource());
					Edge e = new Edge(src.getVertex(), c.getVertex());
					sce.getAddedEdges().add(e);
					log.debug(" added incoming ext: " + e);
				}
				// old edge vanishes
				sce.getRemovedEdges().add(in);
				log.debug(" removed incoming: " + in);
			}

			// vertex has been accounted for
			sce.getRemovedVertices().add(child.getVertex());
			log.debug("remove collapsed child: " + child.getVertex());
		}
	}

	/**
	 * Simulates an expansion of 'c', annotating all changes into the provided 'sce'
	 */
	public void prepareExpand(Cluster c, StructureChangeEvent sce) {
		ArrayList<Cluster> children = slice.expand(c);

		log.debug("Expanding '" + c + "' ...");
		for (Cluster child : children) {
			log.debug("Adding vertex: " + child.getVertex());
			sce.getAddedVertices().add(child.getVertex());
		}

		// fix internal edges
		for (Cluster a : children) {
			log.debug("Adding local outgoing for " + a);
			for (Cluster n : a.localOutgoingNeighbors()) {
				log.debug("\tfound edge: " + a.getVertex() + "-->"
						+ n.getVertex());
				Edge e = (a.isLeafCluster() && n.isLeafCluster()) ? a
						.getLeafEdgeTo(n) : new Edge(a.getVertex(), n
						.getVertex());

				sce.getAddedEdges().add(e);
			}
		}

		// fix outgoing, external edges
		for (Edge out : (Set<Edge>) outgoingEdgesOf(c.getVertex())) {
			Cluster dst = root.clusterForVertex(out.getTarget());
			for (Cluster child : children) {
				if (child.hasEdgesTo(dst)) {
					Edge e = (child.isLeafCluster() && dst.isLeafCluster()) ? child
							.getLeafEdgeTo(dst)
							: new Edge(child.getVertex(), dst.getVertex());
					sce.getAddedEdges().add(e);
					log.debug("Adding outgoing external: " + e);
				}
			}
			// old edge has been substituted by immediate component edges
			log.debug("Removing outgoing external: " + out);
			sce.getRemovedEdges().add(out);
		}

		// fix incoming, external edges
		for (Edge in : (Set<Edge>) incomingEdgesOf(c.getVertex())) {
			Cluster src = root.clusterForVertex(in.getSource());
			for (Cluster child : children) {
				if (src.hasEdgesTo(child)) {
					Edge e = (src.isLeafCluster() && child.isLeafCluster()) ? src
							.getLeafEdgeTo(child)
							: new Edge(src.getVertex(), child.getVertex());
					sce.getAddedEdges().add(e);
					log.debug("Adding incoming external: " + e);
				}
			}
			// old edge has been substituted by immediate component edgess
			log.debug("Removing incoming external: " + in);
			sce.getRemovedEdges().add(in);
		}

		log.debug("Removing final: " + c.getVertex());
		sce.getRemovedVertices().add(c.getVertex());
	}

	/**
	 * Retrieves the pointOfInterest; this is always a vertex within the
	 * visible slice.
	 */
	public Object getPointOfInterest() {
		return pointOfInterest;
	}

	/**
	 * Sets the pointOfInterest; does not recalculate visibility, only expands
	 * the slice to include that pointOfInterest
	 */
	public void setPointOfInterest(Object v) {

		ClusteringChangeEvent cce = null;

		// make sure dest is visible
		Cluster c = root.clusterForVertex(v);
		if (!slice.contains(c)) {
			cce = createMakeVisibleEvent(v);
			if (cce != null)
				clusteringChangePerformed(cce);
		}

		// create an event to actually shift the focus
		cce = new ClusteringChangeEvent(this, pointOfInterest, v,
				"Change in PointOfInterest");
		clusteringChangePerformed(cce);
	}

	/**
	 * utility method that creates a simple collapse event for a vertex;
	 * if the vertex is not currently visible, all necessary parent's collapse
	 * will be added.
	 */
	public ClusteringChangeEvent createCollapseEvent(Object v) {
		Cluster c = root.clusterForVertex(v);
		HashSet<Cluster> set = new HashSet(Cluster.getAncestors(slice));
		set.removeAll(c.getAncestors());
		set.retainAll(c.getDescendants());
		set.add(c);
		Object nextPoI = pointOfInterest;
		for (Cluster collapsed : set) {
			if (collapsed.clusterForVertex(pointOfInterest) != null) {
				nextPoI = collapsed.getVertex();
				log.debug("Next POI for collapse will be " + v + " ("
						+ pointOfInterest + ") => " + nextPoI);
				break;
			}
		}
		ClusteringChangeEvent cce = new ClusteringChangeEvent(this,
				pointOfInterest, nextPoI, "simple collapse");
		for (Cluster collapsed : set) {
			log.debug("decided that this requires collapse of "
					+ collapsed.getVertex());
			cce.addCollapsed(collapsed);
		}
		return cce;
	}

	/**
	 * utility method that creates a simple expand event for a vertex;
	 * if the vertex is not currently visible, expansions will be added until it is
	 */
	public ClusteringChangeEvent createExpandEvent(Object v) {
		Cluster c = root.clusterForVertex(v).getParentCluster();

		if (c == null) {
			log
					.warn("Tried to expand a vertex without corresponding cluster: '"
							+ getBase().getVertexLabel(v) + "'");
			return null;
		}

		Set<Cluster> set = new HashSet<Cluster>();
		set.add(c);
		while (!slice.contains(c) && c != null) {
			c = c.getParentCluster();
			set.add(c);
		}
		if (slice.contains(c)) {
			set.add(c);
		}

		Object nextPoI = pointOfInterest;
		for (Cluster expanded : set) {
			if (expanded.clusterForVertex(pointOfInterest) != null) {
				Cluster nextCluster = root.clusterForVertex(v);
				if (!nextCluster.isLeafCluster()) {
					nextPoI = ((Cluster) nextCluster.getFirstChild())
							.getVertex();
				} else {
					nextPoI = nextCluster.getVertex();
				}
				log.debug("Next POI for expansion will be " + v + " ("
						+ pointOfInterest + ") => " + nextPoI);
				break;
			}
		}

		ClusteringChangeEvent cce = new ClusteringChangeEvent(this,
				pointOfInterest, nextPoI, "simple expand");

		for (Cluster expanded : set) {
			log.debug("decided that this requires expansion of "
					+ expanded.getVertex());
			cce.addExpanded(expanded);
		}

		return cce;
	}

	/**
	 * utility method that creates a simple collapse or expand event for a vertex,
	 * ensuring that after the call, the vertex will be visible. 
	 */
	public ClusteringChangeEvent createMakeVisibleEvent(Object v) {
		if (v == null) {
			return null;
		}
		Cluster c = root.clusterForVertex(v);
		if (c == null) {
			return null;
		}

		// FIXME: not very efficient
		List<Cluster> list = c.getDescendants();
		list.add(c);
		list.retainAll(slice);
		if (list.isEmpty()) {
			log.debug("decided this was an EXPANSION");
			return createExpandEvent(v);
		} else {
			log.debug("decided this was a COLLAPSE");
			return createCollapseEvent(v);
		}
	}

	/**
	 * Dump a vertex name on screen (ugly, but nice for debugging)
	 */
	public String nameFor(Object v) {
		if (v instanceof Cluster.Vertex) {
			return ((Cluster.Vertex) v).getCluster().getListing(getBase())
					+ ":" + v.hashCode();
		} else {
			return v + ":" + v.hashCode();
		}
	}

	/**
	 * Creates an event to perform a PoI change on this graph. That usually implies
	 * lots of expansions and collapses.
	 *
	 * Default is very, very simple.
	 */
	public ClusteringChangeEvent createPoIChangeEvent(Object nextPoI,
			Set frozen, int focusSize, int maxClusters) {

		log.debug("Switching focus from " + nameFor(pointOfInterest) + " to "
				+ nameFor(nextPoI));

		// initialize a distance table for all leaves
		HashMap<Object, Integer> distance = new HashMap<Object, Integer>();
		Cluster poic = hierarchy.getRoot().clusterForVertex(nextPoI);
		for (Object l : poic.getLeafVertices()) {
			distance.put(l, 0);
		}
		EdgeDrivenBFI bfi = new EdgeDrivenBFI(hierarchy.getBase(), poic
				.getFirstLeafVertex());
		bfi.distance = distance;
		int i = 0;
		while (bfi.hasNext() && ++i < 100) {
			bfi.next();
		}

		// collapse slice to include all frozen and a radius of focus-size around the poi
		Slice s2 = (Slice) slice.clone();
		HashSet<Cluster> toPreserve = new HashSet<Cluster>();
		for (Object o : frozen) {
			toPreserve.add(hierarchy.getRoot().clusterForVertex(o));
		}
		toPreserve.add(poic);
		s2.collapseAllExcept(toPreserve);

		if (s2.isEmpty()) {
			log
					.debug("Very difficult: no slice left after collapsing all non-frozen");
			Thread.dumpStack();
			return null;
		}

		// expand again until limit reached
		while (s2.size() < maxClusters) {
			log.debug("Slice has size " + s2.size() + ":" + s2.dump());
			Cluster mic = findMIC(s2, distance);
			if (mic == null) {
				// nothing left to expand
				break;
			}
			if (log.isDebugEnabled()) {
				log.debug("Most important cluster was " + mic + ":"
						+ mic.getVertex().hashCode());
			}
			if (mic == poic) {
				poic = (Cluster) poic.getChildAt(0);
				nextPoI = poic.getVertex();
			}
			s2.expand(mic);
		}

		log.debug("Actual focus shift from " + nameFor(pointOfInterest)
				+ " to " + nameFor(nextPoI));

		return createSliceChangeEvent(s2, nextPoI, "poi change from "
				+ pointOfInterest + " to " + nextPoI);
	}

	/** 
	 * Creates an event to transition from the present slice to another one
	 * @param s2 next slice (the destination slice)
	 * @param nextPoI destination PoI
	 * @param description of the change
	 */
	private ClusteringChangeEvent createSliceChangeEvent(Slice s2,
			Object nextPoI, String description) {

		ArrayList<Cluster> toExpand = new ArrayList<Cluster>();
		ArrayList<Cluster> toCollapse = new ArrayList<Cluster>();
		slice.diff(s2, toExpand, toCollapse);

		// if missing, find where the PoI has gone to
		if (nextPoI == null) {
			Cluster poic = root.clusterForVertex(pointOfInterest);
			if (poic != null && !s2.contains(poic)) {
				if (toExpand.contains(poic)) {
					while (toExpand.contains(poic)) {
						poic = (Cluster) poic.getFirstChild();
					}
					nextPoI = poic.getVertex();
					log.debug("E: Next POI for expansion will be ("
							+ pointOfInterest + ") => " + nextPoI);
				} else {
					while (poic != null && !s2.contains(poic)) {
						poic = poic.getParentCluster();
					}
					if (poic != null) {
						nextPoI = poic.getVertex();
						log.debug("C: Next POI for collapse will be ("
								+ pointOfInterest + ") => " + nextPoI);
					} else {
						log.warn("No POI found! " + poic + " (from) "
								+ pointOfInterest);
					}
				}
			}
		}

		// create the event
		ClusteringChangeEvent cce = new ClusteringChangeEvent(this,
				pointOfInterest, nextPoI, description);

		for (Cluster c : toCollapse) {
			cce.addCollapsed(c);
		}
		for (Cluster c : toExpand) {
			cce.addExpanded(c);
		}

		log.debug("Focus change is: " + cce.getDescription());

		return cce;
	}

	/**
	 * finds the "most important cluster" given a distance set and a slice
	 * to look in.
	 */
	private Cluster findMIC(Slice s, HashMap<Object, Integer> d) {
		Cluster best = null;
		double bestScore = Double.MAX_VALUE;
		for (Cluster c : s) {
			if (c.isLeaf())
				continue;

			int total = 0;
			Set leaves = c.getLeafVertices();
			for (Object v : leaves) {
				total += d.get(v);
			}
			double score = ((double) total) / leaves.size();
			if (score < bestScore || best == null) {
				best = c;
				bestScore = score;
			}
		}
		return best;
	}

	/**
	 * updates a distance table on the underlying base graph; 
	 * good for doi calculation
	 */
	private class EdgeDrivenBFI extends BreadthFirstIterator {
		public HashMap<Object, Integer> distance;
		private BaseGraph g;

		public EdgeDrivenBFI(BaseGraph g, Object start) {
			super(new AsUndirectedGraph(g), start);
			this.g = g;
		}

		protected void encounterVertex(Object vertex, Object edge) {
			super.encounterVertex(vertex, edge);
			int d = (edge == null) ? 0 : distance.get(Graphs.getOppositeVertex(
					g, edge, vertex)) + 1;
			distance.put(vertex, d);
			log.debug("-> D(" + vertex + ") = " + d);
		}
	}

	/**
	 * Implementation of getId for clustered graphs. If the underlying graph's
	 * getId is correct, this will produce consistent results.
	 */
	public String getId(Object vertex) {
		BaseGraph b = hierarchy.getBase();
		if (vertex instanceof Cluster.Vertex) {

			String s1 = ((Cluster.Vertex) vertex).getCluster().getListing(b);
			if (hierarchy.getRoot().clusterForVertex(vertex) == null) {
				log.warn("Id says use " + s1 + ", but it's no longer there...");
			}
			return s1;
		} else {
			return b.getId(vertex);
		}
	}

	public String getVertexLabel(Object v) {
		return hierarchy.getBase().getVertexLabel(v);
	}

	public String getEdgeLabel(Edge e) {
		return hierarchy.getBase().getEdgeLabel(e);
	}

	/**
	 * Restores a slice to the "visible" state; take each vertex id, and until 
	 * found, keep on expanding the next relevant cluster.
	 * Returns a big hashmap with all id->node mappings
	 */
	public HashMap<String, Cluster> restoreSlice(ArrayList<String> ids,
			String poiName) {

		Slice s2 = new Slice(getSlice());

		HashMap<String, Cluster> idToCluster = new HashMap<String, Cluster>();
		findClusterIds(hierarchy.getRoot(), idToCluster);

		Object poi = null;
		for (String id : ids) {
			Cluster toAdd = idToCluster.get(id);
			if (toAdd == null) {
				log.warn("error: cluster '" + id + "' not found");
				continue;
			}
			if (id.equals(poiName)) {
				poi = toAdd.getVertex();
			}
			s2.add(toAdd);
		}

		clusteringChangePerformed(createSliceChangeEvent(s2, poi,
				"restoring slice"));
		return idToCluster;
	}

	private void findClusterIds(Cluster c, HashMap<String, Cluster> idToCluster) {
		if (c.isLeafCluster()) {
			idToCluster.put(getId(c.getVertex()), c);
			return;
		}

		idToCluster.put(c.getListing(getBase()), c);
		for (int i = 0; i < c.getChildCount(); i++) {
			findClusterIds((Cluster) c.getChildAt(i), idToCluster);
		}
	}
}
