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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A set of clusters where no cluster is parent or child of any
 * other cluster. 'Slice' because this is a slice, a cut-set, of the cluster tree.
 * 
 * In Clover, a clustered graph can only display slices of its clustering tree -
 * this simplifies certain operations (such as expand/collapse), and introduces
 * a small drawback in flexibility (can't show the children without expanding
 * the whole parent - unless you redo that part of the clustering).
 *
 * Note that a slice does not require a real clustering hierarchy to be of use - 
 * half-built hierarchies will do just as fine, and Slices come very handy in
 * such cases to get intermediate graphs to recluster with.
 *
 * Utility methods are provided to do vertex->cluster mapping (the other
 * way around is implicit in the vertex)
 *
 * @author mfreire
 */
@SuppressWarnings( { "unchecked", "rawtypes" })
public class Slice extends HashSet<Cluster> {

	private Log log = LogFactory.getLog(Slice.class);

	/**
	 * Empty constructor
	 */
	public Slice() {
		super();
	}

	/**
	 * Creates a new instance of Slice
	 */
	public Slice(Collection<Cluster> collection) {
		super();
		addAll(collection);
	}

	/**
	 * Expands cluster 'c' (must be present), substitutes it for
	 * its children, and returns the children
	 */
	public ArrayList<Cluster> expand(Cluster c) {
		ArrayList<Cluster> al = new ArrayList<Cluster>();
		remove(c);
		for (int i = c.getChildCount() - 1; i >= 0; i--) {
			Cluster child = (Cluster) c.getChildAt(i);
			add(child);
			al.add(child);
		}
		return al;
	}

	/**
	 * Collapses cluster 'c' (its children must be present),
	 * substitutes its children for it, and returns the children
	 */
	public ArrayList<Cluster> collapse(Cluster c) {
		ArrayList<Cluster> al = new ArrayList<Cluster>();
		add(c);
		for (int i = c.getChildCount() - 1; i >= 0; i--) {
			Cluster child = (Cluster) c.getChildAt(i);
			remove(child);
			al.add(child);
		}
		return al;
	}

	/**
	 * Calculates the expand/collapse operations needed to transform
	 * this slice into s2.
	 * Collapse operations include redundant collapses, and
	 * Expand operations include redundant expansions
	 */
	public int diff(Slice s2, ArrayList<Cluster> toExpand,
			ArrayList<Cluster> toCollapse) {
		Slice s1 = (Slice) this.clone();
		List<Cluster> ancestors;

		if (log.isDebugEnabled()) {
			log.debug("DIFFING: \n\t" + this.dump() + "\n\t" + s2.dump());
		}

		// nodes that are parents in s1 and children in s2 have been collapsed
		List<Cluster> collapsed = Cluster.getAncestors(s1);
		collapsed.retainAll(s2);
		log.debug("retained collapsed: " + toCollapse.size());

		// redundant collapses
		HashSet avoidRedundancy = new HashSet(toCollapse);
		ancestors = Cluster.getAncestors(s1);
		for (Cluster c : collapsed) {
			List<Cluster> descendants = c.getDescendants();
			descendants.add(c);
			descendants.retainAll(ancestors);
			for (Cluster c2 : descendants) {
				if (avoidRedundancy.add(c2)) {
					toCollapse.add(c2);
				}
			}
			log.debug("augmented collapsed to: " + toCollapse.size());
		}

		// nodes that are not parents in s1, but are parents in s2, have been expanded
		List<Cluster> expanded = Cluster.getAncestors(s2);
		expanded.retainAll(s1);
		log.debug("retained expanded: " + toExpand.size());

		// redundant expansions
		avoidRedundancy.clear();
		avoidRedundancy.addAll(toExpand);
		ancestors = Cluster.getAncestors(s2);
		for (Cluster c : expanded) {
			List<Cluster> descendants = c.getDescendants();
			descendants.add(c);
			descendants.retainAll(ancestors);
			for (Cluster c2 : descendants) {
				if (avoidRedundancy.add(c2)) {
					toExpand.add(c2);
				}
			}
			log.debug("augmented expanded to: " + toExpand.size());
		}

		return toCollapse.size() + toExpand.size();
	}

	/**
	 * dumps contents of slice to String
	 */
	public String dump() {
		StringBuffer sb = new StringBuffer();
		for (Cluster c : this) {
			sb.append(c.toString() + ", ");
		}
		return sb.toString();
	}

	/**
	 * Collapse this slice into the minimal set that will include all clusters in
	 * toPreserve
	 */
	public void collapseAllExcept(Set<Cluster> toPreserve) {

		Cluster root = iterator().next().getRootCluster();

		// collapse all before starting to expand
		clear();

		// and add root, just in case
		add(root);

		for (Cluster c : toPreserve) {
			List<Cluster> ancestors = c.getAncestors();
			log.debug("FOUND " + ancestors.size() + " ancestors for " + c);

			// reverse ancestor list, to start from root...
			Collections.reverse(ancestors);
			for (Cluster a : ancestors) {
				if (contains(a)) {
					log.debug("Expanding ancestor: " + a);
					expand(a);
				}
			}
		}
		log.debug("Finished 'collapseAllExcept'!");
	}

	/**
	 * Finds all clusters reachable from this one through at least
	 * one outgoing edge
	 */
	public ArrayList<Cluster> clustersWithEdgesFrom(Cluster c) {
		ArrayList<Cluster> neighbors = new ArrayList<Cluster>();
		for (Cluster o : this) {
			if (o != c && c.hasEdgesTo(o)) {
				neighbors.add(o);
			}
		}
		return neighbors;
	}

	/**
	 * Returns the youngest ancestor of 'c' (or possibly, 'c' itself) found in the
	 * the slice; or null if none found.
	 */
	public Cluster getRepresentativeFor(Cluster c) {
		while (c != null) {
			if (contains(c))
				return c;
			c = c.getParentCluster();
		}
		return null;
	}

	/**
	 * Returns a list of all clusters that descend from 'c' in the current slice
	 * or an empty list if none found. Does not include 'c' itself.
	 */
	public ArrayList<Cluster> getDescendantsOf(Cluster c) {
		ArrayList<Cluster> al = new ArrayList<Cluster>();
		for (Cluster a : this) {
			if (a.isNodeAncestor(c))
				al.add(a);
		}
		return al;
	}

	/**
	 * Returns true if this slice contains 'c' or any of its ancestors
	 * (if this returns false, it means 'c' is below the current slice)
	 */
	public boolean containsClusterOrAncestor(Cluster c) {
		return (getRepresentativeFor(c) != null);
	}

	/**
	 * Used to find 'holes' in the current slice; starting from the root, each
	 * cluster should either be itself in the slice, or have an ancestor there. 
	 * In other words, each 'ancestor' is either itself in the slice, or *all*
	 * of its children are 'covered' in the same recursive manner.
	 */
	public ArrayList<Cluster> findHoles(Cluster root) {
		ArrayList<Cluster> missing = new ArrayList<Cluster>();
		findHoles(root, missing);
		return missing;
	}

	/**
	 * returns 'true' if either 'c' is in the present slice, or all its
	 * children are present (or all their children, and so on and so forth).
	 */
	public boolean isCovered(Cluster c) {
		// directly in slice
		if (contains(c)) {
			return true;
		}

		// no children: impossible to be indirectly covered
		if (c.isLeafCluster()) {
			return false;
		}

		// indirectly in slice
		for (int i = c.getChildCount() - 1; i >= 0; i--) {
			if (!isCovered((Cluster) c.getChildAt(i))) {
				// a hole!
				return false;
			}
		}

		// no holes
		return true;
	}

	/**
	 * returns 'true' if neither c, nor any of its children 
	 * are visible in the current slice
	 */
	public boolean isUncovered(Cluster c) {
		if (contains(c)) {
			return false;
		}
		if (c.isLeafCluster()) {
			return true;
		}
		for (int i = c.getChildCount() - 1; i >= 0; i--) {
			if (!isUncovered((Cluster) c.getChildAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * finds holes in the current slice, starting search from cluster 'c'.
	 * After this returns, the minimum number of non-slice clusters needed to 
	 * make sure that 'c's leaves are present in the current slice 
	 * will have been added to 'missing'
	 */
	private void findHoles(Cluster c, ArrayList<Cluster> missing) {

		// covered, directly or indirectly
		if (isCovered(c)) {
			return;
		}

		// ok, we have holes. Where are they? (expected: at most all-but-one)
		boolean[] found = new boolean[c.getChildCount()];
		boolean noneFound = true;
		for (int i = c.getChildCount() - 1; i >= 0; i--) {
			found[i] = isCovered((Cluster) c.getChildAt(i));
			if (found[i]) {
				noneFound = false;
			}
		}

		if (noneFound && isUncovered(c)) {
			// if none found, add self to plug hole
			missing.add(c);
			return;
		}

		// decided not to add self: so add missing children
		for (int i = c.getChildCount() - 1; i >= 0; i--) {
			if (!found[i]) {
				findHoles((Cluster) c.getChildAt(i), missing);
			}
		}
	}
}
