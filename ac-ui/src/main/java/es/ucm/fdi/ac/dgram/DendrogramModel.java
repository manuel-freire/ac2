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
package es.ucm.fdi.ac.dgram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Model information for a dendrogram. Dendrograms have information in 2 dimensions:
 * two clusters can unite at certain points.
 *
 * @author mfreire
 */
public class DendrogramModel {

	private static final Logger log = LogManager
			.getLogger(DendrogramModel.class);

	private float[] highlights;
	private float[][] OD;
	private HashMap<DNode, Integer> leafToInt;
	private HashSet<DNode> nodes;
	private LinkageModel linkage;

	/** Creates a new instance of DendrogramModel */
	public DendrogramModel(int nLeaves, LinkageModel linkage) {
		this.linkage = linkage;
		OD = new float[nLeaves][];
		leafToInt = new HashMap<>();
		nodes = new HashSet<>();
	}

	public void addLeaf(Object o, float[] distances) {
		DNode next = new DNode(0f);
		next.setUserObject(o);
		OD[nodes.size()] = distances;
		leafToInt.put(next, nodes.size());
		nodes.add(next);
		if (nodes.size() == distances.length)
			commit();
	}

	public LinkageModel getLinkage() {
		return linkage;
	}

	public ArrayList<DNode> getClustersAtLevel(double level) {
		ArrayList<DNode> al = new ArrayList<DNode>();
		addClustersAbove(getRoot(), level, al);
		return al;
	}

	private void addClustersAbove(DNode n, double level,
			Collection<DNode> clusters) {
		if (n.getDistance() <= level)
			return;
		for (int i = 0; i < n.getChildCount(); i++) {
			DNode c = (DNode) n.getChildAt(i);
			clusters.add(c);
			addClustersAbove(c, level, clusters);
		}
	}

	private class NodePair implements Comparable<NodePair> {
		private DNode a;
		private DNode b;
		private float weight;

		public NodePair(DNode a, DNode b, float weight) {
			this.a = a;
			this.b = b;
			this.weight = weight;
		}

		public boolean intersects(NodePair o) {
			return a == o.a || b == o.b || a == o.b || b == o.a;
		}

		public int compareTo(NodePair o) {
			int c = Float.compare(weight, o.getWeight());
			return (c != 0) ? c : this.toString().compareTo(o.toString());
		}

		public float getWeight() {
			return weight;
		}

		public DNode getA() {
			return a;
		}

		public DNode getB() {
			return b;
		}

		public String toString() {
			return "" + a + ", " + b;
		}
	}

	/**
	 * Run the clustering algorithm. Uses a priority queue (ordering by link mode - derived
	 * clustering distances) instead of an array; extraction-insertion are O(log(N)), 
	 * (but cleanup still requires O(N)). On the other hand, this method scales 
	 * to higher-than-binary branches in the future.
	 *
	 */
	private void commit() {

		PriorityQueue<NodePair> queue = new PriorityQueue<NodePair>();
		for (DNode a : nodes) {
			for (DNode b : nodes) {
				if (a == b)
					break;

				float d = OD[leafToInt.get(a)][leafToInt.get(b)];
				queue.add(new NodePair(a, b, d));
			}
		}

		int totalRequired = nodes.size();
		int step = Math.max(totalRequired / 10, 1);
		int total = 0;
		long startTime = System.currentTimeMillis();

		while (nodes.size() > 1) {
			if (log.isDebugEnabled()) {
				if ((++total) % step == 0) {
					log.debug("progress: " + total);
				}
			}

			// remove lowest
			NodePair low = queue.poll();

			//            System.err.println("" + (i++) + "=====\n");
			//            for (DNode n : nodes) System.err.println(n);
			//            System.err.println("Extracted "+low);

			// remove discarded alternatives, enshrine chosen one
			DNode m = new DNode(low.getWeight());
			m.add(low.getA());
			m.add(low.getB());
			nodes.remove(low.getA());
			nodes.remove(low.getB());
			for (Iterator<NodePair> it = queue.iterator(); it.hasNext(); /**/) {
				if ((it.next()).intersects(low)) {
					it.remove();
				}
			}
			nodes.add(m);
			//            System.err.println("Added " + m);

			// add new groupings             
			for (DNode n : nodes) {
				if (n != m) {
					float d = linkage.distance(n, m, OD, leafToInt);
					queue.add(new NodePair(n, m, d));
				}
			}
		}
		float secs = (System.currentTimeMillis() - startTime) / 1000;

		log.info("Construction using " + linkage + " required " + secs + " s");
	}

	public DNode getRoot() {
		return nodes.iterator().next();
	}

	public Collection<DNode> getLeaves() {
		return leafToInt.keySet();
	}

	/**
	 * Returns the current leaf sort order as a 0-based permutation 
	 * @param ra return array
	 */
	public void getLeafSortOrder(int[] ra) {
		int pos = 0;
		for (DNode n : getSortedLeaves()) {
			ra[pos++] = leafToInt.get(n);
		}
	}

	public ArrayList<DNode> getSortedLeaves() {
		return getRoot().getLeaves(new ArrayList<DNode>());
	}

	public void setHighlights(List<Double> tmpList) {
		if (tmpList.size() == 0)
			return;
		highlights = new float[tmpList.size()];
		int i = 0;
		for (double d : tmpList) {
			highlights[i++] = (float) d;
		}
	}

	public interface LinkageModel {
		float distance(DNode a, DNode b, float[][] dt, Map<DNode, Integer> m);
	}

	public static class DNode extends DefaultMutableTreeNode {
		private float distance;

		public DNode(float distance) {
			this.distance = distance;
		}

		public float getDistance() {
			return distance;
		}

		public ArrayList<DNode> getLeaves(ArrayList<DNode> al) {
			if (isLeaf())
				al.add(this);
			else
				for (int i = 0; i < getChildCount(); i++) {
					((DNode) getChildAt(i)).getLeaves(al);
				}
			return al;
		}

		public void dump(StringBuilder sb, String indent) {
			if (isLeaf())
				sb.append(indent + getUserObject() + "\n");
			else {
				sb.append(indent + distance + "\n");
				for (int i = 0; i < getChildCount(); i++) {
					((DNode) getChildAt(i)).dump(sb, indent + "  ");
				}
			}
		}

		public StringBuilder dump() {
			StringBuilder sb = new StringBuilder();
			dump(sb, "  ");
			return sb;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (DNode ln : getLeaves(new ArrayList<DNode>())) {
				sb.append(ln.getUserObject() + ",");
			}
			return sb.toString();
		}
	}

	public static class SingleLinkage implements LinkageModel {
		private ArrayList<DNode> leavesInA = new ArrayList(100);
		private ArrayList<DNode> leavesInB = new ArrayList(100);

		public float distance(DNode a, DNode b, float[][] dt,
				Map<DNode, Integer> m) {
			leavesInA.clear();
			a.getLeaves(leavesInA);
			leavesInB.clear();
			b.getLeaves(leavesInB);
			float d = Float.MAX_VALUE;
			for (DNode aa : leavesInA) {
				int i = m.get(aa);
				for (DNode bb : leavesInB) {
					int j = m.get(bb);
					d = Math.min(d, dt[i][j]);
				}
			}
			return d;
		}

		public String toString() {
			return "Single linkage";
		}
	}

	public static class AverageLinkage implements LinkageModel {
		private ArrayList<DNode> leavesInA = new ArrayList(100);
		private ArrayList<DNode> leavesInB = new ArrayList(100);

		public float distance(DNode a, DNode b, float[][] dt,
				Map<DNode, Integer> m) {
			leavesInA.clear();
			a.getLeaves(leavesInA);
			leavesInB.clear();
			b.getLeaves(leavesInB);
			double total = 0;
			int n = 0;
			for (DNode aa : leavesInA) {
				int i = m.get(aa);
				for (DNode bb : leavesInB) {
					int j = m.get(bb);
					total += dt[i][j];
					n++;
				}
			}
			return (float) (total / n);
		}

		public String toString() {
			return "Average linkage";
		}
	}

	public static class CompleteLinkage implements LinkageModel {
		private ArrayList<DNode> leavesInA = new ArrayList(100);
		private ArrayList<DNode> leavesInB = new ArrayList(100);

		public float distance(DNode a, DNode b, float[][] dt,
				Map<DNode, Integer> m) {
			leavesInA.clear();
			a.getLeaves(leavesInA);
			leavesInB.clear();
			b.getLeaves(leavesInB);
			float d = Float.MIN_VALUE;
			for (DNode aa : leavesInA) {
				int i = m.get(aa);
				for (DNode bb : leavesInB) {
					int j = m.get(bb);
					d = Math.max(d, dt[i][j]);
				}
			}
			return d;
		}

		public String toString() {
			return "Complete linkage";
		}
	}
}
