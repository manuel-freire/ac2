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
package es.ucm.fdi.clover.layout;

import es.ucm.fdi.clover.model.ViewGraph;
import es.ucm.fdi.clover.model.ClusteredGraph;
import es.ucm.fdi.clover.model.Cluster;
import es.ucm.fdi.clover.model.BaseGraph;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultGraphCell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A cache key to be used in saving/loading layouts. This is a simplified graph
 * structure, and can be used to match visible graph portions to positions through
 * a LayoutCache. No two visually different graphs shoud have the same CacheKey.
 *
 * @author mfreire
 */
public class CacheKey {

	private Log log = LogFactory.getLog(CacheKey.class);

	private HashMap<Object, HashSet<Object>> k = new HashMap<Object, HashSet<Object>>();

	private String cachedString = null;

	public CacheKey(Node[] nodes) {
		for (Node n : nodes) {
			HashSet<Object> out = new HashSet<Object>();
			for (int e : n.edges) {
				out.add(peerToObject(nodes[e].peer));
			}
			k.put(peerToObject(n.peer), out);
		}
	}

	public CacheKey(ViewGraph vg) {
		for (Object o : vg.vertexSet()) {
			HashSet<Object> out = new HashSet<Object>();
			for (Object e : vg.outgoingEdgesOf(o)) {
				out.add(vg.getEdgeTarget(e));
			}
			k.put(o, out);
		}
	}

	public CacheKey(String saved, BaseGraph base) {
		StringTokenizer st = new StringTokenizer(saved, "[]");

		// find all possible id-to-vertex mappings (ignore root cluster itself)
		HashMap<String, Object> idToVertex = new HashMap<String, Object>();
		if (base instanceof ClusteredGraph) {
			Cluster root = ((ClusteredGraph) base).getHierarchy().getRoot();
			for (Cluster c : root.getDescendants()) {
				idToVertex.put(base.getId(c.getVertex()), c.getVertex());
				log.debug("Added " + base.getId(c.getVertex()));
			}
		} else {
			for (Object v : base.vertexSet()) {
				idToVertex.put(base.getId(v), v);
			}
		}

		// do the loading part
		while (st.hasMoreTokens()) {
			HashSet<Object> out = new HashSet<Object>();
			StringTokenizer est = new StringTokenizer(st.nextToken(), ":,");
			String n = est.nextToken();
			Object o = idToVertex.get(n);
			log.debug("\tUsing " + o + " for " + n);
			while (est.hasMoreTokens()) {
				Object e = idToVertex.get(est.nextToken());
				out.add(e);
			}
			k.put(o, out);
		}
	}

	public String save(BaseGraph base) {
		StringBuffer sb = new StringBuffer();
		for (Object o : k.keySet()) {
			sb.append("[" + base.getId(o) + ":");
			for (Object e : k.get(o)) {
				sb.append("" + base.getId(e) + ",");
			}
			sb.replace(sb.length() - 1, sb.length(), "]");
		}
		return sb.toString();
	}

	public String toString() {
		if (cachedString == null) {
			StringBuffer sb = new StringBuffer();
			for (Object o : k.keySet()) {
				sb.append("[" + o + ":");
				for (Object e : k.get(o)) {
					sb.append("" + e + ",");
				}
				sb.replace(sb.length() - 1, sb.length(), "]");
			}
			cachedString = sb.toString();
		}
		return cachedString;
	}

	private Object peerToObject(Object peer) {
		Object o = ((DefaultGraphCell) ((CellView) peer).getCell())
				.getUserObject();
		return o;
	}

	public double scoreAgainst(CacheKey other) {
		int tMine = 0;
		int tOther = 0;
		int matched = 0;

		HashMap<Object, HashSet<Object>> otherKey = other.k;

		HashSet<Object> common = new HashSet(k.keySet());
		common.retainAll(otherKey.keySet());

		for (Object o : k.keySet()) {
			tMine += 1 + k.get(o).size();
		}
		for (Object o : otherKey.keySet()) {
			tOther += 1 + otherKey.get(o).size();
		}
		for (Object o : common) {
			matched++;
			HashSet<Object> otherEdges = otherKey.get(o);
			for (Object e : k.get(o)) {
				log.debug("\t Searching for " + e + ": "
						+ otherEdges.contains(e));
				if (otherEdges.contains(e))
					matched++;
			}
		}
		log.debug("Matched " + matched + " out of " + tMine + " mine & "
				+ tOther + " others");

		return matched / (double) Math.max(tMine, tOther);
	}

	/**
	 * Default hashCode behaviour is not useful
	 */
	public int hashCode() {
		return toString().hashCode();
	}

	public boolean equals(Object o) {
		return ((CacheKey) o).toString().equals(toString());
	}

	public HashMap<String, Object> getIdMappings(BaseGraph g) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		for (Object o : k.keySet()) {
			log.debug("\tLooking up " + o + "...");
			if (g.getId(o) == null) {
				log.debug(" \tMAL ROLLO...");
				continue;
			}
			map.put(g.getId(o), o);
		}
		return map;
	}
}
