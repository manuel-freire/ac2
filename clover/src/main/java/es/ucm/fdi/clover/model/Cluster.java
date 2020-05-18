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
 * Cluster.java
 *
 * Created on May 15, 2006, 10:14 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a group of vertices, some of which may also be cluster-vertices. 
 * Clusters contain other clusters, and use their userObjects to hold the
 * vertices they represent.
 *
 * Vertices can be of two types: <ul>
 * <li> "real vertices", originated from the ClusteredGraph's BaseGraph </li>
 * <li> "cluster vertices", created by the ClusteringEngine, and only ever displayed
 * within the ClusteredGraph.</li></ul>
 *
 * This class is optimized for many queries and few updates. It could be 
 * implemented, albeit much slower for some operations, with a lot less
 * data caches.
 *
 * @author mfreire
 */
public class Cluster extends DefaultMutableTreeNode {

	private Log log = LogFactory.getLog(Cluster.class);

	private HashSet<Edge> outgoing;
	private HashSet<Edge> incoming;

	/** list of 'same level' clusters this one has edges with */
	private ArrayList<Cluster> localOutgoingNeighbors;

	/** answers 'what leaves here' and 'in which cluster which leaves' */
	private HashMap<Object, Cluster> leavesToClusters;

	/** a list of contents; used by getContentsString, and cluster save/loading */
	private String listing = null;

	/** the old position this vertex occupied in its parent */
	private int oldPos = -1;

	/** 
	 * a name that identifies this cluster; must be set externally via setName;
	 * needs not be set
	 */
	private String name = null;

	/**
	 * Creates a new leaf cluster, wrapping up a real vertex
	 */
	public Cluster(BaseGraph g, Object v) {
		super(v);
		incoming = new HashSet<Edge>();
		outgoing = new HashSet<Edge>();
		leavesToClusters = new HashMap<Object, Cluster>();
		localOutgoingNeighbors = new ArrayList<Cluster>();

		incoming.addAll(g.incomingEdgesOf(v));
		//log.debug("For vertex "+v+" - incoming = "+g.inDegreeOf(v));
		outgoing.addAll(g.outgoingEdgesOf(v));
		//log.debug("For vertex "+v+" - outgoing = "+g.outDegreeOf(v));
		leavesToClusters.put(v, this);
	}

	/**
	 * Creates a new intermediate cluster, wrapping up a ClusterVertex
	 */
	public Cluster() {
		super();
		incoming = new HashSet<Edge>();
		outgoing = new HashSet<Edge>();
		leavesToClusters = new HashMap<Object, Cluster>();
		localOutgoingNeighbors = new ArrayList<Cluster>();

		setUserObject(new Vertex(this));
	}

	/**
	 * A class that represents a false, internal vertex. It is linked to this
	 * cluster.
	 */
	public static class Vertex {
		private Cluster c;

		private Vertex(Cluster c) {
			this.c = c;
		}

		public Cluster getCluster() {
			return c;
		}
	}

	/**
	 * Get a string with content listing
	 */
	public String getListing(BaseGraph base) {
		if (listing == null) {
			TreeSet<String> t = new TreeSet<String>();
			StringBuffer sb = new StringBuffer("{");
			for (Object v : getLeafVertices()) {
				t.add(base.getId(v));
			}
			if (t.isEmpty()) {
				return "EMPTY";
			}
			for (String s : t) {
				sb.append(s + ".");
			}
			sb.replace(sb.lastIndexOf("."), sb.length(), "}");
			listing = sb.toString();
		}
		return listing;
	}

	/**
	 * Parse a content listing (retrieves the relevant ids)
	 */
	public static ArrayList<String> parseListing(String l) {
		ArrayList<String> al = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(l, "{}.");
		while (st.hasMoreTokens()) {
			al.add(st.nextToken());
		}
		return al;
	}

	/**
	 * Adds a cluster to this one.
	 * Parent clusters should update their caches with these leaves; 
	 * Local edges should be created where warranted.
	 *
	 * (note that all MutableTreeNode adds/inserts will pass through here,
	 * unlike 'add(MutableTreeNode n)')
	 */
	public void insert(MutableTreeNode o, int index) {
		Cluster c = (Cluster) o;

		log.debug("Adding cluster: " + c + " to " + this);
		if (log.isDebugEnabled()) {
			StringBuffer sb = new StringBuffer();
			for (Object x : getLeafVertices()) {
				sb.append(x + " ");
			}
			log.debug("Current contents:\n\t" + sb.toString());
			sb = new StringBuffer();
			for (Object x : getLeafVertices()) {
				sb.append(x + " ");
			}
			log.debug("To add:\n\t" + sb.toString());
		}

		// update local neighbors
		updateLocalOutgoingAfterInsert(c);
		super.insert(c, index);
		incoming.addAll(c.getIncoming());
		outgoing.addAll(c.getOutgoing());
		leavesToClusters.putAll(c.leavesToClusters);

		// propagate upwards
		//log.debug("Propagating changes upwards...");
		for (Cluster p : getAncestors()) {
			p.getIncoming().addAll(c.getIncoming());
			p.getOutgoing().addAll(c.getOutgoing());
			for (Object v : c.getLeafVertices()) {
				p.leavesToClusters.putAll(c.leavesToClusters);
			}
			p.listing = null;
		}

		// clear content string
		listing = null;
	}

	/**
	 * Rebuild this cluster's caches from the base graph (an argument);
	 * all direct child clusters should be currently correct.
	 */
	private void rebuild() {
		if (isLeafCluster())
			return;

		incoming.clear();
		outgoing.clear();
		leavesToClusters.clear();
		localOutgoingNeighbors.clear();
		listing = null;

		// clear children's idea of their outgoing neighbors
		for (int i = 0; i < getChildCount(); i++) {
			Cluster c = (Cluster) getChildAt(i);
			c.localOutgoingNeighbors.clear();
		}

		// rebuild this & all children's ideas
		for (int i = 0; i < getChildCount(); i++) {
			Cluster c = (Cluster) getChildAt(i);
			incoming.addAll(c.incoming);
			outgoing.addAll(c.outgoing);
			leavesToClusters.putAll(c.leavesToClusters);
			updateLocalOutgoingAfterInsert(c);
		}
	}

	/**
	 * Root, recursive rebuild (ugly, but works)
	 */
	public void rebuildFromHere() {
		Enumeration e = depthFirstEnumeration();
		while (e.hasMoreElements()) {
			Cluster c = (Cluster) e.nextElement();
			c.rebuild();
		}
	}

	/**
	 * Cluster 'child' may or may not have been already inserted
	 */
	public void updateLocalOutgoingAfterInsert(Cluster child) {
		for (int i = 0; i < getChildCount(); i++) {
			Cluster n = (Cluster) getChildAt(i);
			if (n == child)
				continue;

			if (child.hasEdgesTo(n)) {
				child.localOutgoingNeighbors.add(n);
				log.debug("Found local edge: " + child + "->" + n);
			}
			if (n.hasEdgesTo(child)) {
				n.localOutgoingNeighbors.add(child);
				log.debug("Found local edge: " + n + "->" + child);
			}
		}
	}

	/**
	 * Removes a cluster from this one
	 * (note that all MutableTreeNode removes will pass through here,
	 * unlike 'remove(MutableTreeNode n)')
	 */
	public void remove(int index) {
		Cluster c = (Cluster) getChildAt(index);

		// update local neighbors
		updateLocalOutgoingAfterDelete(c);

		// store the old position prior to removal; useful to update stupid treeModels
		c.oldPos = index;
		super.remove(index);

		incoming.removeAll(c.getIncoming());
		outgoing.removeAll(c.getOutgoing());
		for (Object v : c.getLeafVertices()) {
			leavesToClusters.remove(v);
		}

		// propagate upwards
		for (Cluster p : getAncestors()) {
			p.getIncoming().removeAll(c.getIncoming());
			p.getOutgoing().removeAll(c.getOutgoing());
			for (Object v : c.getLeafVertices()) {
				p.leavesToClusters.remove(v);
			}
			p.listing = null;
		}

		// clear content string
		listing = null;
	}

	/**
	 * Cluster 'child' may or may not have been already deleted
	 */
	public void updateLocalOutgoingAfterDelete(Cluster child) {
		for (int i = 0; i < getChildCount(); i++) {
			Cluster n = (Cluster) getChildAt(i);
			if (n == child)
				continue;

			if (child.hasEdgesTo(n)) {
				child.localOutgoingNeighbors.remove(n);
			}
			if (n.hasEdgesTo(child)) {
				n.localOutgoingNeighbors.remove(child);
			}
		}
	}

	/**
	 * returns whether this is a terminal cluster (ie, it can have no children)
	 * or not
	 */
	public boolean isLeafCluster() {
		return !(getVertex() instanceof Cluster.Vertex);
	}

	/**
	 * valid only for leaf clusters, returns the original edge from this cluster to 
	 * another leaf cluster
	 */
	public Edge getLeafEdgeTo(Cluster c) {
		for (Edge e : outgoing) {
			if (e.getTarget().equals(c.getVertex())) {
				return e;
			}
		}
		return null;
	}

	/**
	 * checks whether there is a directed connection from this cluster
	 * to another one, that is, if any of the leafVertices of this cluster is connected
	 * to any of the leafVertices of the other cluster.
	 */
	public boolean hasEdgesTo(Cluster c) {
		return !edgesTo(c).isEmpty();
	}

	/**
	 * Find the outgoing edges between this cluster and another one
	 */
	public Set<Edge> edgesTo(Cluster c) {
		Set<Edge> candidates = (Set<Edge>) outgoing.clone();
		candidates.retainAll(c.getIncoming());
		return candidates;
	}

	/**
	 * returns a list of all "real" vertex leafVertices of this cluster
	 */
	public Set getLeafVertices() {
		return leavesToClusters.keySet();
	}

	/**
	 * returns the first leaf of a given cluster (which may be far, far down)
	 */
	public Object getFirstLeafVertex() {
		Cluster c = this;
		while (!c.isLeaf()) {
			c = (Cluster) c.getChildAt(0);
		}
		return c.getVertex();
	}

	/**
	 * The leaf cluster in this hierarchy that contains the given
	 * leaf vertex. This is an O(1) operation
	 * @return the last cluster that contains the given leaf vertex      
	 */
	public Cluster getLastClusterFor(Object v) {
		return leavesToClusters.get(v);
	}

	public HashSet<Edge> getOutgoing() {
		return outgoing;
	}

	public HashSet<Edge> getIncoming() {
		return incoming;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		Cluster c = this;
		sb.append("" + hashCode());
		while (!c.isRoot()) {
			c = c.getParentCluster();
			sb.insert(0, c.hashCode() + "_");
		}
		return sb.toString();
	}

	/**
	 * Returns the ancestors of this cluster (not including the 
	 * cluster itself); root cluster is last
	 */
	public List<Cluster> getAncestors() {
		ArrayList<Cluster> ancestors = new ArrayList<Cluster>();
		Cluster c = this;
		while (!c.isRoot()) {
			c = c.getParentCluster();
			ancestors.add(c);
		}
		return ancestors;
	}

	/**
	 * Returns all ancestors of a given group of clusters (not including
	 * themselves); root cluster is last, no cluster is repeated, 
	 * result is same as merging, without repeats, several calls to 
	 * "getAncestors()".
	 */
	public static List<Cluster> getAncestors(Collection<Cluster> clusters) {
		ArrayList<Cluster> al = new ArrayList<Cluster>();
		HashSet<Cluster> ancestors = new HashSet<Cluster>();
		for (Cluster n : clusters) {
			Cluster c = n;
			while (!c.isRoot()) {
				c = c.getParentCluster();
				if (ancestors.add(c)) {
					al.add(c);
				} else {
					// already added by someone
					break;
				}
			}
		}
		return al;
	}

	/**
	 * Returns the descendants of this cluster (not including the cluster itself).
	 * Depth-first order is used.
	 */
	public List<Cluster> getDescendants() {
		ArrayList<Cluster> al = new ArrayList<Cluster>();
		Enumeration e = depthFirstEnumeration();
		while (e.hasMoreElements()) {
			Cluster c = (Cluster) e.nextElement();
			if (c != this)
				al.add(c);
		}
		return al;
	}

	/**
	 * Returns all descendants of a given group of clusters; depth-first order
	 * is used. Does not return the clusters themselves
	 */
	public static List<Cluster> getDescendants(Collection<Cluster> clusters) {
		ArrayList<Cluster> al = new ArrayList<Cluster>();
		HashSet<Cluster> descendants = new HashSet<Cluster>();
		for (Cluster c : clusters) {
			Enumeration e = c.depthFirstEnumeration();
			for (e.nextElement(); e.hasMoreElements();/**/) {
				if (descendants.add((Cluster) e.nextElement())) {
					al.add(c);
				} else {
					// depth-first: if it had been added once, it will be added again
					break;
				}
			}
		}
		return al;
	}

	/**
	 * Returns the cluster for this vertex, works with all types of vertices
	 * that can be found in this cluster - both leaves and non-leaves.
	 * Returns null if it cannot be found in this cluster.
	 */
	public Cluster clusterForVertex(Object v) {
		if (v instanceof Vertex) {
			Cluster c = ((Vertex) v).getCluster();
			if (!c.isNodeAncestor(this)) {
				return null;
			}
			return ((Vertex) v).getCluster();
		} else {
			return leavesToClusters.get(v);
		}
	}

	/**
	 * returns the vertex that this wrapped in this cluster. 
	 * (This is the same thing as the userObject).
	 */
	public Object getVertex() {
		return userObject;
	}

	/**
	 * list of 'same level' clusters this one has edges with
	 * returns whether this is a terminal cluster (ie, it can have no children)
	 * or not
	 */
	public List<Cluster> localOutgoingNeighbors() {
		return localOutgoingNeighbors;
	}

	/**
	 * returns the root
	 */
	public Cluster getRootCluster() {
		return (Cluster) getRoot();
	}

	/**
	 * @return parent cluster
	 */
	public Cluster getParentCluster() {
		return (Cluster) getParent();
	}

	/**
	 * Returns all vertices (== userObjects) found in a set of clusters
	 */
	public static HashSet getVertices(Collection<Cluster> clusters) {
		HashSet vertices = new HashSet();
		for (Cluster c : clusters) {
			vertices.add(c.getVertex());
		}
		return vertices;
	}

	/**
	 * Dumps to logger - for debugging only
	 */
	public String dump() {
		return "Dumping " + getVertex().toString() + ":"
				+ dump(new StringBuffer(), "  ").toString();
	}

	/**
	 * internal dump helper
	 */
	private StringBuffer dump(StringBuffer sb, String prefix) {
		sb.append("\n" + prefix + this + ":" + " " + getVertex());
		sb.append("\n" + prefix + "  out: ");
		for (Edge e : outgoing)
			sb.append(e.getSource() + "->" + e.getTarget() + " ");
		sb.append("\n" + prefix + "  in:  ");
		for (Edge e : incoming)
			sb.append(e.getSource() + "->" + e.getTarget() + " ");
		sb.append("\n" + prefix + "  lon:  ");
		for (Cluster c : localOutgoingNeighbors) {
			sb.append(c + " (" + c.getVertex() + "), ");
		}
		sb.append("\n" + prefix + "  leaves: ");
		for (Object k : leavesToClusters.keySet()) {
			sb
					.append("" + k + "->" + leavesToClusters.get(k).getVertex()
							+ " ");
		}

		for (int i = 0; i < getChildCount(); i++) {
			sb.append(((Cluster) getChildAt(i)).dump(new StringBuffer(), prefix
					+ "  "));
		}
		return sb;
	}

	/**
	 * Returns all component edges of this one. Root must be common
	 * to both (and it *is* needed, because leaf vertices are not of type
	 * Cluster.Vertex).
	 * Component edges unite a base vertex to another base vertex; intermediate
	 * edges are not retrievable without major calculations...
	 * If one or more endpoints are not reachable from 'root', returns 'null'
	 */
	public static Set<Edge> componentEdgesOf(Cluster root, Edge e) {
		Object s = e.getSource();
		Object d = e.getTarget();
		Cluster src = (s instanceof Cluster.Vertex) ? ((Cluster.Vertex) s)
				.getCluster() : root.getLastClusterFor(s);
		Cluster dst = (d instanceof Cluster.Vertex) ? ((Cluster.Vertex) d)
				.getCluster() : root.getLastClusterFor(d);

		if (src == null || dst == null || src.getRootCluster() != root
				|| dst.getRootCluster() != root) {
			return null;
		}

		return src.edgesTo(dst);
	}

	/**
	 * Another version of the above; this one works as long as one of the
	 * two vertices is a Cluster.Vertex...
	 */
	public static Set<Edge> componentEdgesOf(Edge e) {
		HashSet<Edge> componentEdges = new HashSet<Edge>();
		Cluster src = (e.getSource() instanceof Cluster.Vertex) ? ((Cluster.Vertex) e
				.getSource()).getCluster()
				: null;
		Cluster dst = (e.getTarget() instanceof Cluster.Vertex) ? ((Cluster.Vertex) e
				.getTarget()).getCluster()
				: null;

		if (src == null && dst == null) {
			throw new IllegalArgumentException(
					"Either source or dest should be cluster vertices");
		}

		if (src != null && dst != null) {
			componentEdges.addAll(src.getOutgoing());
			componentEdges.retainAll(dst.getIncoming());
		} else if (src != null) {
			for (Edge ce : src.getOutgoing()) {
				if (e.getTarget() == ce.getTarget()) {
					componentEdges.add(ce);
				}
			}
		} else if (dst != null) {
			for (Edge ce : dst.getIncoming()) {
				if (e.getSource() == ce.getSource()) {
					componentEdges.add(ce);
				}
			}
		}
		return componentEdges;
	}

	/**
	 * Returns a symbolic name for this cluster; only available if set
	 * externally; not retained after reclustering.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets a symbolic name for the cluster
	 */
	public void setName(String name) {
		this.name = name;
	}

	int getOldPos() {
		return oldPos;
	}
}
