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
 * SimpleRuleClusterer.java
 *
 * Created on May 17, 2006, 7:48 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.model;

import es.ucm.fdi.clover.event.HierarchyChangeEvent;
import es.ucm.fdi.clover.event.StructureChangeEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;

/**
 * A clustering engine that uses rules to build clusters. 
 * Similar to a graph grammar; whenever a vertex's vincinity matches a rule, the 
 * rule is applied (and all affected vertices are converted to a new cluster).
 *
 * Rules are applied in strict order; rule 'i+1' will only be applied if no vertex
 * matches rule 'i'.
 *
 * @author mfreire
 */
public class SimpleRuleClusterer implements ClusteringEngine {

	private static Logger log = Logger.getLogger(SimpleRuleClusterer.class);

	protected ArrayList<ClusteringRule> rules;

	public SimpleRuleClusterer() {
		rules = new ArrayList<ClusteringRule>();
		initRules();
	}

	/**
	 * Sets the rules that will be used, and the order in which they will
	 * be called. 
	 */
	public void initRules() {
		rules.add(new SingleParentOfTerminals());
		rules.add(new SingleParentOfAlmostTerminals());
		rules.add(new SharedParentOfTerminals());
		rules.add(new ParentOfSomething());
	}

	/**
	 * Creates a hierarchy. Performs this task by applying the rules until none 
	 * is applicable, and then bunching everything into a big cluster with 
	 * the rootVertex as first vertex.
	 */
	public Cluster createHierarchy(BaseGraph base, Object rootVertex) {

		// build base clusters for everything
		Slice clusters = new Slice();
		for (Object v : base.vertexSet()) {
			clusters.add(new Cluster(base, v));
		}

		// and build the hierarchy from that
		return buildHierarchy(base, clusters, rootVertex);
	}

	/**
	 * Hierarchy creation: the real thing
	 */
	private Cluster buildHierarchy(BaseGraph base, Slice clusters,
			Object rootVertex) {

		SliceGraph graph = new SliceGraph(clusters, base);

		// build the rest of the clusters by repeated application of rules
		while (clusters.size() > 1) {

			log.info("== Next iteration: " + clusters.size()
					+ " contestants left");
			if (log.isDebugEnabled()) {
				log.debug(">> Contestant/vertices:");
				for (Object v : graph.vertexSet()) {
					log.debug("\t " + base.getVertexLabel(v));
				}
				log.debug(">> Contestants/clusters:");
				for (Cluster c : clusters) {
					log.debug("\t "
							+ base.getVertexLabel(c.getFirstLeafVertex())
							+ " (" + c.getDescendants().size() + ")");
				}
			}

			// iterate through all vertices, checking rules
			boolean somethingMatched = false;
			for (ClusteringRule cr : rules) {
				log.debug("SWITCHING to " + cr.getDescription());
				for (Object v : graph.vertexSet()) {
					log.debug("Evaluating " + v.getClass().getSimpleName());
					ArrayList al = cr.applyTo(graph, v);
					if (al != null) {
						rootVertex = buildCluster(graph, al, rootVertex);
						if (log.isDebugEnabled()) {
							for (Object x : al) {
								log.debug("\t" + x.getClass().getSimpleName());
							}
							log.debug("NEW CLUSTER (" + cr.getDescription()
									+ "):");
							//cv.getCluster().dump();
						}
						somethingMatched = true;
						break;
					}
				}
				if (somethingMatched) {
					break;
				}
				log.debug("Did not match anything with " + cr.getDescription());
			}

			// if the rules have all failed, bunch all together
			if (!somethingMatched) {
				// cycles, multiple components... just bunch all together
				log.debug("NO RULES AVAILABLE: Just bunching everything up");
				ArrayList al = new ArrayList();
				// if the root vertex still hasn't been clustered, cluster it now
				if (graph.containsVertex(rootVertex)) {
					al.add(rootVertex);
				} else {
					log.warn("Unable to find root when bunching all together: "
							+ rootVertex);
				}
				for (Object v : graph.vertexSet()) {
					if (v != rootVertex) {
						al.add(v);
					}
				}

				buildCluster(graph, al, rootVertex);
			}
		}

		return clusters.iterator().next();
	}

	public Cluster.Vertex buildCluster(SliceGraph graph, ArrayList vertices,
			Object rootVertex) {
		Cluster.Vertex cv = (Cluster.Vertex) graph.clusterAndCollapse(vertices);
		if (vertices.contains(rootVertex)) {
			rootVertex = cv;
		}
		return cv;
	}

	/**
	 * recreate a hierarchy after receiving a StructureChangeEvent     
	 */
	public Cluster recreateHierarchy(BaseGraph base, Object rootVertex,
			StructureChangeEvent sce, HierarchyChangeEvent hce) {

		Slice clusters = new Slice();
		for (Object v : base.vertexSet()) {
			clusters.add(new Cluster(base, v));
		}
		Cluster newRoot = buildHierarchy(base, clusters, rootVertex);
		hce.getSource().createChangeEventFor(newRoot, sce, hce);
		return newRoot;
	}

	/**
	 * Update the hierarchy with a set of structural changes; does not really
	 * perform the update, it just returns an "hce" that describes what should
	 * be done.
	 */
	public Cluster updateHierarchy(Cluster root, BaseGraph base,
			StructureChangeEvent sce, HierarchyChangeEvent hce) {

		// if removals or insertions of vertices or edges, recalculate clustering
		Cluster nextRoot = root;
		if ((sce.getRemovedEdges().size() > 0 || sce.getRemovedVertices()
				.size() > 0)
				|| (sce.getAddedEdges().size() > 0 || sce.getAddedVertices()
						.size() > 0)) {

			// handle complex changes: rebuild and watch what's different
			log.debug("recreating...");
			nextRoot = recreateHierarchy(base, root.getVertex(), sce, hce);
		} else {
			// handle simple changes: only if not fully recalculated
			for (Object v : sce.getChangedVertices()) {
				Cluster c = root.clusterForVertex(v);
				hce.getChangedClusters().add(c);
				hce.getChangedClusters().addAll(c.getAncestors());
			}
		}

		return nextRoot;
	}

	public void save(Element e) {
		// no options; but could save the clusteringRules in use
	}

	public void restore(Element e) {
		// no options; but could restore the relevant clusteringRules
	}

	/**
	 * has children, 
	 * no children have other children
	 * no children have other parents!
	 *<code>
	 *   x
	 *  /|\
	 * y y y
	 *</code>
	 */
	public static class SingleParentOfTerminals extends ClusteringRule {
		public ArrayList applyTo(DirectedGraph g, Object v) {
			ArrayList vertices = new ArrayList();
			vertices.add(v);
			for (Object o : outgoingNeighborsOf(g, v)) {
				//log.debug(""+o+": in "+inDegreeOf(g,o)+" out "+outDegreeOf(g,o));
				if (inDegreeOf(g, o) > 1 || outDegreeOf(g, o) > 0) {
					return null;
				} else {
					vertices.add(o);
				}
			}
			if (vertices.size() > 1) {
				return vertices;
			}
			return null;
		}

		public String getDescription() {
			return "single-parent-of-terminals";
		}
	}

	/**
	 * has children that are exclusive to this parent
	 *<code>
	 *   x   ?
	 *  /|\ /
	 * y y z
	 *</code>
	 */
	public static class SharedParentOfTerminals extends ClusteringRule {
		public ArrayList applyTo(DirectedGraph g, Object v) {
			ArrayList vertices = new ArrayList();
			vertices.add(v);
			for (Object o : outgoingNeighborsOf(g, v)) {
				//log.debug(""+o+": in "+inDegreeOf(g,o)+" out "+outDegreeOf(g,o));
				if (inDegreeOf(g, o) > 1) {
					continue;
				} else if (outDegreeOf(g, o) > 0) {
					return null;
				} else {
					vertices.add(o);
				}
			}
			if (vertices.size() > 1) {
				return vertices;
			}
			return null;
		}

		public String getDescription() {
			return "shared-parent-of-terminals";
		}
	}

	/**
	 * has children, 
	 * all children have a single child (or no children at all),
	 * child is not original parent, 
	 * child has no parents outside this lot.
	 *<code>
	 *   x
	 *  /|\
	 * y y y
	 *  \|
	 *   z
	 *</code>
	 */
	public static class SingleParentOfAlmostTerminals extends ClusteringRule {
		public ArrayList applyTo(DirectedGraph g, Object v) {
			ArrayList vertices = new ArrayList();
			vertices.add(v);
			Object child = null;

			HashSet brothers = new HashSet();
			for (Object o : outgoingNeighborsOf(g, v)) {
				if (inDegreeOf(g, o) > 1 || outDegreeOf(g, o) > 1) {
					return null;
				} else {
					brothers.add(o);
				}
			}

			for (Object o : brothers) {
				if (outDegreeOf(g, o) == 1) {
					for (Object c : outgoingNeighborsOf(g, o)) {
						if (child == null) {
							child = c;
							for (Object parent : incomingNeighborsOf(g, v)) {
								if (!brothers.contains(parent)) {
									return null;
								}
							}
							vertices.add(child);
						} else if (!child.equals(c)) {
							//log.debug("discarded R2 for "+v
							//        +" because child of "+o+" is "+oe.oppositeVertex(o)
							//        +" instead of "+child);
							return null;
						}
					}
					vertices.add(o);
				}
			}
			if (vertices.size() > 1) {
				return vertices;
			}
			return null;
		}

		public String getDescription() {
			return "single-parent-of-almost-terminals";
		}
	}

	/**
	 * has children that are exclusive to this parent (but does not accept
	 * shared parenthood, and clusters those that have their own children)
	 *<code>
	 *   x   
	 *  /|\ /
	 * y y z
	 *   |
	 *   w
	 *</code>
	 */
	public static class ParentOfNonterminals extends ClusteringRule {
		public ArrayList applyTo(DirectedGraph g, Object v) {
			ArrayList vertices = new ArrayList();
			vertices.add(v);
			for (Object o : outgoingNeighborsOf(g, v)) {
				if (inDegreeOf(g, o) > 1) {
					continue;
				} else {
					vertices.add(o);
				}
			}
			if (vertices.size() > 1) {
				return vertices;
			}
			return null;
		}

		public String getDescription() {
			return "parent-of-nonterminals";
		}
	}

	/**
	 * has children, 
	 * & the children may have multiple parents, but
	 * no children of their own
	 * (only applied if the other two fail)
	 *<code>
	 *   x
	 *  /|\
	 * y y y
	 * ? ? ?
	 * a b c
	 *</code>
	 */
	public static class ParentOfSomething extends ClusteringRule {
		public ArrayList applyTo(DirectedGraph g, Object v) {
			ArrayList vertices = new ArrayList();
			vertices.add(v);
			for (Object o : outgoingNeighborsOf(g, v)) {
				if (outDegreeOf(g, o) > 0) {
					return null;
				} else {
					vertices.add(o);
				}
			}
			if (vertices.size() > 1) {
				return vertices;
			}
			return null;
		}

		public String getDescription() {
			return "parent-of-something";
		}
	}

	/**
	 * A rule, to be executed by this clusterer. 
	 * If a rule is applied, a complete re-run will occur.
	 */
	public static abstract class ClusteringRule {

		// shared "everything goes" default validator
		private static EdgeValidator defaultValidator = new DefaultValidator();

		private EdgeValidator validator = defaultValidator;
		private boolean reversed = false;

		/**
		 * Applies this rule to the vertex 'v' in the current graph
		 * @param g the graph
		 * @param v the vertex to try to apply it to
		 * @return an ArrayList with the nodes to place in the next cluster, 
		 *    the order is important (the first may be chosen as representative).
		 *    If the rule does not match, it should return 'null'.
		 */
		public abstract ArrayList applyTo(DirectedGraph g, Object v);

		/**
		 * Returns the valid outgoing edges of this vertex
		 */
		public ArrayList outgoingNeighborsOf(DirectedGraph g, Object v) {
			ArrayList neighbors = new ArrayList();
			for (Edge e : (Set<Edge>) (reversed ? g.incomingEdgesOf(v) : g
					.outgoingEdgesOf(v))) {
				if (validator.isEdgeValid(e)) {
					neighbors.add(Graphs.getOppositeVertex(g, e, v));
				}
			}
			return neighbors;
		}

		/**
		 * Returns the valid incoming edges of this vertex
		 */
		public ArrayList incomingNeighborsOf(DirectedGraph g, Object v) {
			ArrayList neighbors = new ArrayList();
			for (Edge e : (Set<Edge>) (reversed ? g.outgoingEdgesOf(v) : g
					.incomingEdgesOf(v))) {
				if (validator.isEdgeValid(e)) {
					neighbors.add(Graphs.getOppositeVertex(g, e, v));
				}
			}
			return neighbors;
		}

		/**
		 * Returns the count of valid outgoing edges of this vertex
		 */
		public int outDegreeOf(DirectedGraph g, Object v) {
			int count = 0;
			for (Edge e : (Set<Edge>) (reversed ? g.incomingEdgesOf(v) : g
					.outgoingEdgesOf(v))) {
				if (validator.isEdgeValid(e))
					count++;
			}
			return count;
		}

		/**
		 * Returns the count of valid incoming edges of this vertex
		 */
		public int inDegreeOf(DirectedGraph g, Object v) {
			int count = 0;
			for (Edge e : (Set<Edge>) (reversed ? g.outgoingEdgesOf(v) : g
					.incomingEdgesOf(v))) {
				if (validator.isEdgeValid(e))
					count++;
			}
			return count;
		}

		public void setValidator(EdgeValidator validator) {
			this.validator = validator;
		}

		public void setReversed(boolean reversed) {
			this.reversed = reversed;
		}

		private static class DefaultValidator implements EdgeValidator {
			public boolean isEdgeValid(Edge e) {
				return true;
			}
		}

		/**
		 * Returns a description of the rule
		 */
		public abstract String getDescription();
	}

	/**
	 * Used by clustering-rules to check whether a given edge should be considered
	 * or not.
	 */
	public interface EdgeValidator {
		public boolean isEdgeValid(Edge e);
	}
}
