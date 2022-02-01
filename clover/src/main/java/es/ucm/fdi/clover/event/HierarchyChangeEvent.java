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
package es.ucm.fdi.clover.event;

import es.ucm.fdi.clover.model.Cluster;
import es.ucm.fdi.clover.model.Edge;
import es.ucm.fdi.clover.model.ClusterHierarchy;
import es.ucm.fdi.clover.model.Slice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Describes changes in a hierarchy (possibly as a result of upstream changes to
 * the graph it was built upon).
 *
 * If the whole hierarchy has changed (root has been substituted) then
 * 'oldRoot' will point to the old root (and no other changes); otherwise,
 * oldRoot will be null. Note that a root substution may have an old root of
 * 'null'.
 *
 * When 'added' clusters do not belong to the original clustering, it is still
 * possible to reuse old clusters as their children; method is to use the included
 * 'matched' map; if any descendant of the added cluster is found to be matched,
 * the matched version should be used instead (by substituting the 'alien' child
 * for the 'old' one).
 *
 * @author mfreire
 */
public class HierarchyChangeEvent {

	private Log log = LogFactory.getLog(HierarchyChangeEvent.class);

	/** event source (hierarchy on top of which these changes have taken place) */
	private ClusterHierarchy source;

	/** description */
	private String description;

	/** clusters that have been removed; order is 'fromWhom', 'what'; must be local */
	private HashMap<Cluster, ArrayList<Cluster>> removedClusters;
	/** clusters that have been added; order is 'toWhom', 'what'; may be alien */
	private HashMap<Cluster, ArrayList<Cluster>> addedClusters;
	/** correspondences from children of 'alien' added to children of removed clusters */
	private HashMap<Cluster, Cluster> matchedClusters;
	/** clusters that have been changed (includes all ascendants of added/removed) */
	private HashSet<Cluster> changedClusters;

	/** added edges - also if source and/or destination have just been added */
	private HashSet<Edge> addedEdges;
	/** removed edges - only if source and destination are still there */
	private HashSet<Edge> removedEdges;

	/** old root; null unless the root of the hierarchy has been replaced */
	private Cluster oldRoot = null;
	private boolean rootChange = false;

	/**
	 * Creates a new instance of HierarchyChangeEvent
	 */
	public HierarchyChangeEvent(ClusterHierarchy source, String description) {
		this.source = source;
		addedClusters = new HashMap<Cluster, ArrayList<Cluster>>();
		removedClusters = new HashMap<Cluster, ArrayList<Cluster>>();
		matchedClusters = new HashMap<Cluster, Cluster>();
		changedClusters = new HashSet<Cluster>();
		addedEdges = new HashSet<Edge>();
		removedEdges = new HashSet<Edge>();
		this.description = description;
	}

	/**
	 * Returns true only if this involves a root change
	 */
	public boolean isRootChange() {
		return rootChange;
	}

	/**
	 * Changes this to a 'root changed' event
	 */
	public void setRootChange(Cluster oldRoot) {
		this.oldRoot = oldRoot;
		addedClusters.clear();
		removedClusters.clear();
		changedClusters.clear();
		addedEdges.clear();
		removedEdges.clear();
		rootChange = true;
	}

	public ClusterHierarchy getSource() {
		return source;
	}

	/**
	 * Vertex v may have had its cluster deleted, or otherwise may no longer be
	 * present in the current display. Get the nearest equivalent in the updated
	 * hierarchy. Used to shift pointOfInterest after one of these events
	 */
	public Object getVisibleRepresentativeFor(Object v, Slice s) {
		log.debug("VisibleRepresentative for " + v + " being sought... ");
		if (v instanceof Cluster.Vertex) {
			log.debug("\t it is a cluster: "
					+ ((Cluster.Vertex) v).getCluster().hashCode() + ")");
		}
		if (isRootChange()) {
			log.debug("\t root change: returning root");
			return source.getRoot().getVertex();
		} else {
			Cluster c = source.getRoot().getLastClusterFor(v);
			if (c == null) {
				// one of the ancestors, or this vertex itself, has been deleted
				if (v instanceof Cluster.Vertex) {
					c = ((Cluster.Vertex) v).getCluster();
				} else {
					// it was a real vertex; and it was deleted
					log
							.debug("\t it was a real vertex; and it got deleted; returning root");
					return source.getRoot().getVertex();
				}

				while (!matchedClusters.containsKey(c)) {
					c = c.getParentCluster();
					if (c == null) {
						// did not find any matched within ancestors...
						log
								.debug("\t could not match within ancestors; returning root");
						return source.getRoot().getVertex();
					}
				}
				// found attached antecessor; use to get a representative
				log.debug("\t found a matching ancestor; using it.");
				return getVisibleRepresentativeFor(c.getVertex(), s);
			} else {
				// the vertex is still attached
				Cluster rep = s.getRepresentativeFor(c);
				if (rep == null) {
					ArrayList<Cluster> desc = s.getDescendantsOf(c);
					if (desc.isEmpty()) {
						log
								.warn("\t still attached, but could not find anything to represent vertex "
										+ v);
						Thread.dumpStack();
						return source.getRoot().getVertex();
					}
					log.debug("\t still attached, found ancestor: " + v);
					return desc.get(0).getVertex();
				} else {
					log.debug("\t still there, or found representative: "
							+ rep.getListing(source.getBase()));
					return rep.getVertex();
				}
			}
		}
	}

	/**
	 * Perform 'matched substitution' on the specified alien cluster.
	 * The 'alien' should either be one of the 'added', or a descendant thereof
	 */
	public Cluster getMatchedVersion(Cluster alien) {
		if (matchedClusters.containsKey(alien)) {
			Cluster c = matchedClusters.get(alien);
			matchedClusters.remove(c);
			if (!c.isRoot()) {
				c.getParentCluster().remove(c);
			}
			return c;
		}

		for (int i = alien.getChildCount() - 1; i >= 0; i--) {
			Cluster alienChild = (Cluster) alien.getChildAt(i);

			// after this, all possible children will be local
			Cluster matched = getMatchedVersion(alienChild);
			if (alienChild != matched) {
				// if the entire child was matched, use the matched version
				alien.remove(i);
				alien.insert(matched, i);
			}
		}
		return alien;
	}

	/**
	 * Augments the event with all changes derived from adding/removing
	 */
	public void augmentChangesWithAddedAndRemoved() {
		// all places where stuff was added
		changedClusters.addAll(getAddedClusters().keySet());
		// all places where stuff was removed
		changedClusters.addAll(getRemovedClusters().keySet());
		// and all ascendants thereof
		changedClusters.addAll(Cluster.getAncestors(changedClusters));
	}

	// helper for 'getDescription'
	private void appendMapping(HashMap<Cluster, ArrayList<Cluster>> map,
			StringBuffer sb) {
		for (Map.Entry<Cluster, ArrayList<Cluster>> e : map.entrySet()) {
			sb.append("\n\t at " + e.getKey().hashCode() + " "
					+ e.getKey().getListing(source.getBase()) + " "
					+ e.getKey());
			for (Cluster c : e.getValue()) {
				if (c == null)
					sb.append("\n\t\t null");
				else
					sb.append("\n\t\t " + c.hashCode() + " "
							+ c.getListing(source.getBase()) + " " + c);
			}
		}
	}

	public String getDescription() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n  Source: " + source);
		sb.append("\n  Description: " + description);
		if (!addedClusters.isEmpty()) {
			sb.append("\n\t Added: ");
			appendMapping(addedClusters, sb);
		}
		if (!removedClusters.isEmpty()) {
			sb.append("\n\t Removed: ");
			appendMapping(removedClusters, sb);
		}
		if (!matchedClusters.isEmpty()) {
			sb.append("\n\t Matched: ");
			for (Map.Entry<Cluster, Cluster> e : matchedClusters.entrySet()) {
				sb.append("\n\t " + e.getKey().hashCode() + " "
						+ e.getKey().getListing(source.getBase()) + " "
						+ e.getKey() + "~= " + e.getValue().hashCode() + " "
						+ e.getValue().getListing(source.getBase()) + " "
						+ e.getValue());
			}
		}
		if (!changedClusters.isEmpty()) {
			sb.append("\n\t Changed: ");
			for (Cluster c : changedClusters)
				sb.append("\n\t " + c.hashCode() + " "
						+ c.getListing(source.getBase()) + " " + c);
		}
		if (!addedEdges.isEmpty()) {
			sb.append("\n\t Added edges: ");
			for (Edge e : addedEdges)
				sb.append("\n\t " + source.getBase().getId(e.getSource())
						+ "->" + source.getBase().getId(e.getTarget()) + " ");
		}
		if (!removedEdges.isEmpty()) {
			sb.append("\n\t Removed edges: ");
			for (Edge e : removedEdges)
				sb.append("\n\t " + source.getBase().getId(e.getSource())
						+ "->" + source.getBase().getId(e.getTarget()) + " ");
		}
		return sb.toString();
	}

	public void insertRemovedCluster(Cluster parent, Cluster removed) {
		ArrayList<Cluster> al = removedClusters.get(parent);
		if (al == null) {
			al = new ArrayList<Cluster>();
			removedClusters.put(parent, al);
		}
		if (!al.contains(removed)) {
			al.add(removed);
		} else {
			log.warn("Tried to remove vertex for a second time: " + removed);
			Thread.dumpStack();
		}
	}

	public void insertAddedCluster(Cluster parent, Cluster added) {
		ArrayList<Cluster> al = addedClusters.get(parent);
		if (al == null) {
			al = new ArrayList<Cluster>();
			addedClusters.put(parent, al);
		}
		if (!al.contains(added)) {
			al.add(added);
		}
	}

	public HashMap<Cluster, Cluster> getMatchedClusters() {
		return matchedClusters;
	}

	public HashMap<Cluster, ArrayList<Cluster>> getAddedClusters() {
		return addedClusters;
	}

	public HashMap<Cluster, ArrayList<Cluster>> getRemovedClusters() {
		return removedClusters;
	}

	public HashSet<Cluster> getChangedClusters() {
		return changedClusters;
	}

	public HashSet<Edge> getAddedEdges() {
		return addedEdges;
	}

	public HashSet<Edge> getRemovedEdges() {
		return removedEdges;
	}
}
