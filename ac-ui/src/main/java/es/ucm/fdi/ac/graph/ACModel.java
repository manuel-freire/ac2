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
package es.ucm.fdi.ac.graph;

import es.ucm.fdi.ac.Analysis;
import es.ucm.fdi.ac.Submission;
import es.ucm.fdi.clover.event.StructureChangeEvent;

import es.ucm.fdi.clover.model.BaseGraph;
import es.ucm.fdi.clover.model.Edge;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jgrapht.EdgeFactory;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.UndirectedSubgraph;

/**
 * A graph model that builds JGraphT graphs suitable for JGraph display from
 * AC submissions with distance below a given threshold.
 *
 * @author mfreire
 */
@SuppressWarnings( { "unchecked", "rawtypes" })
public class ACModel extends BaseGraph implements EdgeFactory {

	private static final Logger log = LogManager.getLogger(ACModel.class);

	private Analysis ac;
	private String key;
	private float[][] F;
	private float maxValue;
	private Submission centerSubmission = null;
	private float thresholdDistance = 0;
	private HashMap<Submission, Integer> clusters = null;

	/**
	 * Creates a new instance of ACModel
	 */
	public ACModel(Analysis ac, String key) {

		this.ac = ac;
		this.key = key;
		maxValue = Float.MIN_VALUE;

		Submission[] S = ac.getSubmissions();
		F = new float[S.length][S.length];
		for (int i = 0; i < S.length; i++) {
			float row[] = (float[]) S[i].getData(key);
			for (int j = 0; j < S.length; j++) {
				F[i][j] = row[j];
				maxValue = Math.max(F[i][j], maxValue);
			}
		}
	}

	public float getMaxValue() {
		return maxValue;
	}

	public Object createEdge(Object sourceVertex, Object targetVertex) {
		throw new UnsupportedOperationException("must create from within model");
	}

	public Edge createEdge(Submission a, Submission b, float dist) {
		Edge e = new Edge(a, b);
		e.setData(dist);
		return e;
	}

	/**
	 * reload everything from the model. If no subject is currently selected as
	 * "center", generate global graph. Otherwise, generate individual graph
	 */
	public void load() {

		SimpleGraph<Object, Edge> tmp = new SimpleGraph<Object, Edge>(
				Edge.class);

		if (centerSubmission == null) {
			boolean[] valid = new boolean[ac.getSubmissions().length];
			// filter out those that are too dissimilar
			for (int i = 0; i < F.length; i++) {
				Submission a = ac.getSubmissions()[i];
				log.info("looking up clusters for " + a.getId());
				int ca = (clusters != null ? clusters.get(a) : -1);
				for (int j = 0; j < i; j++) {
					Submission b = ac.getSubmissions()[j];
					int cb = (clusters != null ? clusters.get(b) : -1);
					if (ca == cb && F[i][j] <= thresholdDistance) {
						// must be same cluster and under-threshold
						if (!valid[i]) {
							valid[i] = true;
							tmp.addVertex(a);
						}
						if (!valid[j]) {
							valid[j] = true;
							tmp.addVertex(b);
						}
						Edge e = createEdge(a, b, F[i][j]);
						tmp.addEdge(e.getSource(), e.getTarget(), e);
					}
				}
			}

		} else {

			// init stack
			Stack<DepthStackElement> active = new Stack<DepthStackElement>();
			active.push(new DepthStackElement(centerSubmission, 0));
			tmp.addVertex(centerSubmission);

			// add all vertices & edges that are ever going to make it
			while (!active.isEmpty()) {
				DepthStackElement current = active.pop();
				if (current.getDepth() >= 3)
					break;
				Submission s = current.getSubmission();
				float d[] = (float[]) s.getData(key);
				for (int i = 0; i < d.length; i++) {
					if (d[i] <= thresholdDistance) {
						Edge e = createEdge(s, ac.getSubmissions()[i], d[i]);
						if (e.getSource() == e.getTarget())
							continue;
						if (!tmp.containsVertex(e.getSource())) {
							tmp.addVertex(e.getSource());
							active.push(new DepthStackElement((Submission) e
									.getSource(), current.getDepth() + 1));
						}
						if (!tmp.containsVertex(e.getTarget())) {
							tmp.addVertex(e.getTarget());
							active.push(new DepthStackElement((Submission) e
									.getTarget(), current.getDepth() + 1));
						}
						tmp.addEdge(e.getSource(), e.getTarget(), e);
					}
				}
			}
		}

		removeRedundant(tmp);
	}

	private static class DepthStackElement {
		private Submission s;
		private int depth;

		public DepthStackElement(Submission s, int depth) {
			this.s = s;
			this.depth = depth;
		}

		public Submission getSubmission() {
			return s;
		}

		public int getDepth() {
			return depth;
		}
	}

	private void removeRedundant(SimpleGraph<Object, Edge> tmp) {

		// prepare changes to the graph
		StructureChangeEvent sce = new StructureChangeEvent(this);

		// remove extra vertices (those that are not in 'tmp')
		Set<Object> toRemoveVs = new HashSet<>(vertexSet());
		toRemoveVs.removeAll(tmp.vertexSet());
		sce.getRemovedVertices().addAll(toRemoveVs);

		// remove extra edges (those that did not make it into 'tmp')
		Set<Edge> toRemove = new HashSet<>(edgeSet());
		toRemove.removeAll(tmp.edgeSet());
		sce.getRemovedEdges().addAll(toRemove);

		// create and init new edges
		Set<Edge> toAdd = new HashSet<>(tmp.edgeSet());
		toAdd.removeAll(edgeSet());
		sce.getAddedEdges().addAll(toAdd);

		// find new vertices too        
		for (Edge e : toAdd) {
			if (!containsVertex(e.getSource())) {
				sce.getAddedVertices().add(e.getSource());
			}
			if (!containsVertex(e.getTarget())) {
				sce.getAddedVertices().add(e.getTarget());
			}
		}

		// make the changes
		structureChangePerformed(sce);
	}

	/**
	 * Remove redundant edges
	 */
	public static Set<Edge> removeRedundantEdges(SimpleGraph<Object, Edge> g,
			Submission center) {
		HashSet<Edge> toRetain = new HashSet<>();
		for (Set<Object> comp : new ConnectivityInspector<>(g).connectedSets()) {

			// build subgraph with component
			Set<Edge> compEdges = new HashSet<Edge>();
			for (Object v : comp) {
				compEdges.addAll(g.edgesOf(v));
			}

			if (compEdges.size() == 0) {
				// nothing to see here - move along
				continue;
			}

			UndirectedSubgraph<Object, Edge> ug = new UndirectedSubgraph<>(g, comp, compEdges);

			// find minimum spanning tree of this component; use Prim's algo
			// http://en.wikipedia.org/wiki/Prim%27s_algorithm
			PriorityQueue<PrimInfo> queue = new PriorityQueue<PrimInfo>();
			HashMap<Object, PrimInfo> V = new HashMap<Object, PrimInfo>();
			for (Object v : ug.vertexSet()) {
				float d = (queue.isEmpty()) ? 0 : Float.MAX_VALUE;
				PrimInfo pi = new PrimInfo(v, d);
				V.put(v, pi);
				queue.add(pi);
			}
			while (!queue.isEmpty()) {
				PrimInfo u = queue.poll();
				V.remove(u.getVertex());
				if (u.getEdge() != null) {
					toRetain.add(u.getEdge());
				}
				for (Edge e : (Set<Edge>) ug.edgesOf(u.getVertex())) {
					Object vv = (e.getTarget() != u.getVertex()) ? e
							.getTarget() : e.getSource();
					PrimInfo v = V.get(vv);
					if (v == null || v == u) {
					} else if (v.update(e)) {
						queue.remove(v);
						queue.add(v);
					} 
				}
			}

			// augment with |V| lowest-weight edges (some may already be present)
			PriorityQueue<Edge> lowest = new PriorityQueue<Edge>(ug.edgeSet()
					.size(), new Comparator<Edge>() {
				public int compare(Edge e1, Edge e2) {
					return Float.compare((Float) e1.getData(), (Float) e2
							.getData());
				}
			});
			for (Edge e : g.edgeSet()) {
				lowest.add(e);
			}
			for (int i = ug.vertexSet().size() - 1; i >= 0 && !lowest.isEmpty(); i--) {
				toRetain.add(lowest.poll());
			}

			// augment with centerSubmission-touching edges, if any
			if (center != null) {
				for (Edge e : g.edgeSet()) {
					if (e.getSource() == center || e.getTarget() == center) {
						toRetain.add(e);
					}
				}
			}
		}

		// prepare set of edges to remove 
		HashSet<Edge> all = new HashSet<Edge>(g.edgeSet());
		all.removeAll(toRetain);

		// remove them
		g.removeAllEdges(all);
		return all;
	}

	private static class PrimInfo implements Comparable<PrimInfo> {
		private Object vertex;
		private Edge edge;
		private float min;

		public PrimInfo(Object v, float d) {
			this.vertex = v;
			this.min = d;
			this.edge = null;
		}

		public int compareTo(PrimInfo other) {
			return Float.compare(min, other.min);
		}

		public boolean update(Edge e) {
			float omin = (Float) e.getData();
			if (omin < min) {
				min = omin;
				this.edge = e;
				return true;
			}
			return false;
		}

		private Submission getVertex() {
			return (Submission) vertex;
		}

		private Edge getEdge() {
			return edge;
		}
	}

	public String getEdgeLabel(Edge e) {
		return "";
	}

	public String getVertexLabel(Object o) {
		return ((Submission) o).getId();
	}

	public void setThresholdDistance(float thresholdDistance,
			HashMap<Submission, Integer> clusters) {
		if (this.thresholdDistance == thresholdDistance)
			return;
		this.clusters = clusters;
		this.thresholdDistance = thresholdDistance;
		load();
	}

	public void setCenterSubmission(Submission centerSubmission) {

		if (this.centerSubmission == centerSubmission)
			return;

		// if old is non-null and switches it, remove & re-add later
		if (this.centerSubmission != null
				&& containsVertex(this.centerSubmission)) {
			StructureChangeEvent sce = new StructureChangeEvent(this);
			sce.getRemovedVertices().add(this.centerSubmission);
			structureChangePerformed(sce);
		}
		// if next is non-null and visible, remove & re-add later
		if (centerSubmission != null && containsVertex(centerSubmission)) {
			StructureChangeEvent sce = new StructureChangeEvent(this);
			sce.getRemovedVertices().add(centerSubmission);
			structureChangePerformed(sce);
		}

		this.centerSubmission = centerSubmission;
		load();
	}

	public Submission getCenterSubmission() {
		return centerSubmission;
	}
}
