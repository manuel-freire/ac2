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

import java.util.Collection;
import java.util.HashMap;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Slices can be viewed as clustered graphs, and this class helps you do just that.
 * It should only be used to create a clustering (a Cluster hierarchy that 
 * covers the whole graph) - things get simpler once the clustering is complete
 *
 * @author mfreire
 */
@SuppressWarnings( { "unchecked", "rawtypes" })
public class SliceGraph extends DefaultDirectedGraph {

	private Log log = LogFactory.getLog(SliceGraph.class);

	/** current slice (~= 'vertexSet' in graph at this moment) */
	private Slice slice;
	private BaseGraph g;

	/** 
	 * cluster for al present&past vertices. Can't use the 
	 * Cluster.getLastClusterFor(v), because the clustering is not ready yet
	 */
	private HashMap<Object, Cluster> verticesToClusters;

	/**
	 * Creates a new instance of SliceGraph. The clusters in the provided slice
	 * should be all leaf clusters, otherwise illegal edges will be added.
	 */
	public SliceGraph(Slice slice, BaseGraph g) {
		super(new DefaultEdgeFactory());
		this.slice = slice;
		this.g = g;

		Graphs.addAllVertices(g, Cluster.getVertices(slice));
		verticesToClusters = new HashMap<Object, Cluster>();

		for (Cluster c : slice) {
			if (!c.isRoot() || !c.isLeaf()) {
				throw new IllegalArgumentException(
						"Expected a Slice of root, child-less clusters");
			}
			verticesToClusters.put(c.getVertex(), c);
			Graphs.addAllEdges(this, g, c.getOutgoing());

			// it is possible to include vertices without outgoing or incoming edges
			addVertex(c.getVertex());
		}
	}

	/**
	 * Updates this graph, and the underlying slice, by collapsing a set
	 * into the given node.
	 */
	public Object clusterAndCollapse(Collection vertices) {

		for (Object v : vertices) {
			if (!containsVertex(v)) {
				log.warn("!!Cowardly refusing to include "
						+ g.getVertexLabel(v) + ": not in slice!!");
				throw new IllegalArgumentException(
						"Tried to cluster vertex not in slice");
			} else {
				log.debug("... located " + g.getVertexLabel(v) + " ... ");
			}
		}

		if (log.isDebugEnabled()) {
			StringBuffer sb = new StringBuffer();
			for (Object v : vertices) {
				sb.append(v + ", ");
			}
			log.debug("collapsing: \n\t"
					+ sb.substring(0, sb.length() - ", ".length()));
			log.debug("graph is now: \n\t" + this);
		}

		// create new
		Cluster c = new Cluster();
		addVertex(c.getVertex());
		verticesToClusters.put(c.getVertex(), c);

		// collapse old (but store into cluster)
		for (Object v : vertices) {
			c.add(verticesToClusters.get(v));
			verticesToClusters.put(v, c);
			removeVertex(v);
		}

		log.debug("New cluster looks like this: " + c.getVertex());

		// add outgoing (may add repeated edges - no problem)
		for (Edge out : c.getOutgoing()) {
			Cluster other = verticesToClusters.get(out.getTarget());
			log.debug("outgoing to :" + other);
			if (!c.isNodeDescendant(other)) {
				while (!(containsVertex(other.getVertex()))) {
					other = (Cluster) other.getParent();
				}
				if (log.isDebugEnabled()) {
					log.debug("other deemed to be : " + other);
				}
				addEdge(c.getVertex(), other.getVertex());
			}
		}

		// add incoming (may add repeated edges - no problem)
		for (Edge in : c.getIncoming()) {
			Cluster other = verticesToClusters.get(in.getSource());
			log.debug("incoming from :" + other);
			if (!c.isNodeDescendant(other)) {
				while (!(containsVertex(other.getVertex()))) {
					other = (Cluster) other.getParent();
				}
				if (log.isDebugEnabled()) {
					log.debug("other deemed to be :" + other);
				}
				addEdge(other.getVertex(), c.getVertex());
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("...done: \n\t" + this);
		}

		// collapse the slice
		slice.collapse(c);

		// return the vertex that represents this collapse
		return c.getVertex();
	}
}
