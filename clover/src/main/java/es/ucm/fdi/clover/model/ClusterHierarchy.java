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
 * ClusterHierarchy.java
 *
 * Created on May 7, 2006, 6:07 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.model;

import es.ucm.fdi.clover.event.HierarchyChangeEvent;
import es.ucm.fdi.clover.event.HierarchyChangeListener;
import es.ucm.fdi.clover.event.StructureChangeEvent;
import es.ucm.fdi.clover.event.StructureChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.jdom2.Element;

import org.apache.log4j.Logger;

/**
 * A ClusterHierarchy is used by a ClusteredGraph to maintain and update its
 * clusters. Also, the ClusterGraph only keeps "visible" the vertices/edges that should
 * be visible - the rest sleep safely out of view in the ClusterHierarchy.
 *
 * Note that ClusteringHierarchys listen to changes from the BaseGraph,
 * and that is how the listening ClusteredGraphs get to know of these
 * changes.
 *
 * @author mfreire
 */
public class ClusterHierarchy implements StructureChangeListener,
		HierarchyChangeListener {

	private static Logger log = Logger.getLogger(ClusterHierarchy.class);

	/** the root of the hierarchy */
	protected Cluster root;

	/** the engine is what builds & rebuilds the hierarchy when needed */
	protected ClusteringEngine engine;

	/** the BaseGraph this hierarchy is built upon */
	protected BaseGraph base;

	/** hierarchy change listeners */
	protected ArrayList<HierarchyChangeListener> hierarchyListeners;

	/**
	 * Uses a saved JDom element to restore a clusterhierarchy
	 */
	public ClusterHierarchy(Element e, BaseGraph base, ClusteringEngine engine) {
		this.engine = engine;
		this.base = base;
		hierarchyListeners = new ArrayList<HierarchyChangeListener>();
		restore(e);
		base.addStructureChangeListener(this);
	}

	/**
	 * Creates a new ClusterHierarchy, built upon the given BaseGraph,
	 * and to be built and maintained by the provided engine
	 */
	public ClusterHierarchy(BaseGraph base, Object rootVertex,
			ClusteringEngine engine) {
		this.engine = engine;
		hierarchyListeners = new ArrayList<HierarchyChangeListener>();
		setBaseGraph(base, rootVertex);
	}

	public ClusteringEngine getEngine() {
		return engine;
	}

	/**
	 * Replace the underlying graph. If a hierarchy was already built on the old
	 * one, signals everybody of the root change.
	 */
	public void setBaseGraph(BaseGraph base, Object rootVertex) {
		if (this.base != null) {
			this.base.removeStructureChangeListener(this);
		}
		this.base = base;

		HierarchyChangeEvent evt = null;
		if (root != null) {
			evt = new HierarchyChangeEvent(this, "Base graph changed");
			evt.setRootChange(root);
		}
		root = engine.createHierarchy(base, rootVertex);
		if (evt != null) {
			fireHierarchyChangeEvt(evt);
		}

		base.addStructureChangeListener(this);
	}

	public void addHierarchyChangeListener(HierarchyChangeListener l) {
		hierarchyListeners.add(l);
	}

	public void removeHierarchyChangeListener(HierarchyChangeListener l) {
		hierarchyListeners.remove(l);
	}

	/**
	 * Notify listeners of changes to this hierarchy
	 */
	protected void fireHierarchyChangeEvt(HierarchyChangeEvent evt) {
		for (HierarchyChangeListener l : hierarchyListeners) {
			l.hierarchyChangePerformed(evt);
		}
	}

	/**
	 * Responds to a change in the base graph it was built upon. This triggers
	 * an incremental clustering, resulting in a hierarchy update
	 *
	 * (which should capture the *whole* change, because this is the only nexus
	 * between clustered graph representations, cluster trees and the like, and
	 * the actual base graph).
	 */
	public void structureChangePerformed(StructureChangeEvent evt) {
		log.debug(evt.getDescription());
		HierarchyChangeEvent hce = new HierarchyChangeEvent(this, evt
				.getDescription());
		engine.updateHierarchy(root, base, evt, hce);
		hierarchyChangePerformed(hce);
	}

	public Cluster getRoot() {
		return root;
	}

	public BaseGraph getBase() {
		return base;
	}

	/**
	 * Saves the current hierarchy to an XML representation (via JDom)
	 */
	public void save(Element e) {
		save(root, e);
	}

	private void save(Cluster c, Element pe) {
		Element e = null;
		if (c.isLeafCluster()) {
			e = new Element("vertex");
			Object v = c.getVertex();
			e.setAttribute("id", base.getId(v));
		} else {
			e = new Element("cluster");
			if (c.getName() != null) {
				e.setAttribute("name", c.getName());
			}
			for (int i = 0; i < c.getChildCount(); i++) {
				save((Cluster) c.getChildAt(i), e);
			}
		}
		pe.addContent(e);
	}

	/**
	 * Restores the current hierarchy from an XML representation (the one
	 * that is generated by 'save')
	 */
	public void restore(Element e) {
		Element first = (Element) e.getChildren().get(0);

		HashMap<String, Object> map = new HashMap<String, Object>();
		for (Object v : base.vertexSet()) {
			map.put(base.getId(v), v);
		}

		if (first.getName().equals("vertex")) {
			root = new Cluster(base, map.get(first.getAttributeValue("id")));
		} else {
			root = new Cluster();
			if (first.getAttribute("name") != null) {
				root.setName(first.getAttributeValue("name"));
			}
			for (Element child : (List<Element>) first.getChildren()) {
				restore(child, root, map);
			}
		}
	}

	private void restore(Element e, Cluster parent, HashMap<String, Object> map) {
		if (e.getName().equals("vertex")) {
			Cluster c = new Cluster(base, map.get(e.getAttributeValue("id")));
			parent.add(c);
		} else {
			Cluster c = new Cluster();
			if (e.getAttribute("name") != null) {
				c.setName(e.getAttributeValue("name"));
			}
			for (Element child : (List<Element>) e.getChildren()) {
				restore(child, c, map);
			}
			parent.add(c);
		}
	}

	/**
	 * Debug-print a series of collections
	 */
	private String dump(String name, Collection<Cluster> col) {
		StringBuffer sb;
		sb = new StringBuffer(name + ": ");
		for (Cluster c : col) {
			sb.append(" " + c.getListing(base));
		}
		return sb.toString();
	}

	/**
	 * Debug-print a map
	 */
	private String dump(String name, HashMap<Cluster, Cluster> m) {
		StringBuffer sb;
		sb = new StringBuffer(name + ": ");
		for (Cluster c : m.keySet()) {
			sb.append(" " + c.getListing(base) + "->"
					+ m.get(c).getListing(base));
		}
		return sb.toString();
	}

	/**
	 * Creates an event that describes the changes from the current clustering
	 * to the new one. This method does *not* change the clustering; nothing
	 * changes until the event is applied.
	 * 
	 * When this event is applied, the changes will be 
	 * performed. 'newClustering' is the root of a *different* clustering
	 * from this one.
	 *
	 * If changes in the base graph have also occured, 'sce' should be
	 * non-null, and contain these changes.
	 */
	public void createChangeEventFor(Cluster newClustering,
			StructureChangeEvent structureChange, HierarchyChangeEvent hce) {

		log.debug("Old: " + root.dump());
		log.debug("New: " + newClustering.getRootCluster().dump());

		// if structure change was null, use a dummy now
		StructureChangeEvent sce = (structureChange == null) ? new StructureChangeEvent(
				base)
				: structureChange;

		Comparator clusterSizeComparator = new ClusterSizeComparator(base);

		// unmatched old-hierarchy/new-hierarchy clusters        
		TreeSet<Cluster> u1 = new TreeSet<Cluster>(clusterSizeComparator);
		TreeSet<Cluster> u2 = new TreeSet<Cluster>(clusterSizeComparator);

		// old/new leaf slices (finding changes from bottom to top)
		HashSet<Cluster> s1 = new HashSet<Cluster>();
		HashSet<Cluster> s2 = new HashSet<Cluster>();

		// initialize old base slice, and s1
		Set<Cluster> oldLeaves = new HashSet<Cluster>();
		for (Object v : root.getLeafVertices()) {
			Cluster c = root.getLastClusterFor(v);
			// add to slice, unless it was removed in the last 'sce''
			if (!sce.getRemovedVertices().contains(v)) {
				s1.add(c);
			}
		}

		// initialize new base slice, and s2 (and, on occasion, u2)
		for (Object v : newClustering.getLeafVertices()) {
			Cluster c = newClustering.getLastClusterFor(v);
			// if this is a new vertex, it will be unmatched; else, match with old
			if (sce.getAddedVertices().contains(v)) {
				u2.add(c);
			} else {
				hce.getMatchedClusters().put(c, root.getLastClusterFor(v));
			}
			s2.add(c);
		}

		log.debug("Entering main matching loop...");

		// repeat until s1 and s2 reach the root (when it is matched, no more matches are possible)
		while (!hce.getMatchedClusters().containsKey(root)) {

			log.debug("\n\t" + dump("s1", s1) + "\n\t" + dump("s2", s2)
					+ "\n\t" + dump("u1", u1) + "\n\t" + dump("u2", u2)
					+ "\n\t" + dump("mc", hce.getMatchedClusters()));

			// collapse 1 full level of each slice
			log.debug("Collapsing s1:");
			s1 = getParents(s1, base);
			log.debug("Collapsing s2:");
			s2 = getParents(s2, base);

			log.debug("\n\t" + dump("s1", s1) + "\n\t" + dump("s2", s2)
					+ "\n\t" + dump("u1", u1) + "\n\t" + dump("u2", u2)
					+ "\n\t" + dump("mc", hce.getMatchedClusters()));

			// if both have been fully collapsed, no sense in continuing
			if (s1.isEmpty() && s2.isEmpty()) {
				break;
			}
			u1.addAll(s1);
			u2.addAll(s2);

			// find matches between slices (the last vertex of each should always match...)
			matchLoop: while (true) {
				log.debug("<<< Alooping anew >>>");
				log.debug("\n\t" + dump("s1", s1) + "\n\t" + dump("s2", s2)
						+ "\n\t" + dump("u1", u1) + "\n\t" + dump("u2", u2)
						+ "\n\t" + dump("mc", hce.getMatchedClusters()));

				for (Cluster a : u1) {
					for (Cluster b : u2) {
						if (!hce.getMatchedClusters().containsKey(b)
								&& matchClusters(u1, u2, s1, s2, a, b, sce,
										hce, base)) {
							// after a match, u1, u2, hce and matchedClusters are updated
							continue matchLoop;
						}
					}
				}

				// if the 'continue' was not triggered, no matches were found
				break;
			}
		}

		// augment the event with all changes (ascendants of altered places)
		hce.augmentChangesWithAddedAndRemoved();

		// added/removed edges are transparently, uh, added or removed
		hce.getAddedEdges().addAll(sce.getAddedEdges());
		hce.getRemovedEdges().addAll(sce.getRemovedEdges());

		log.debug("Final event is: " + hce.getDescription());
	}

	/**
	 * compares two clusters against each other, judging first by size, then
	 * by contents
	 */
	public static class ClusterSizeComparator implements Comparator<Cluster> {
		public BaseGraph base;

		public ClusterSizeComparator(BaseGraph base) {
			this.base = base;
		}

		public int compare(Cluster o1, Cluster o2) {
			Set<Cluster> s1 = o1.getLeafVertices();
			Set<Cluster> s2 = o2.getLeafVertices();
			return (s1.size() != s2.size()) ? s1.size() - s2.size() : o1
					.getListing(base).compareTo(o2.getListing(base));
		}
	}

	/**
	 * A light match: no after-effects, just answers the question 
	 * 'is a equivalent to b despite the changes in sce happening to any of them'
	 */
	private boolean matches(Cluster a, Cluster b, StructureChangeEvent sce) {
		HashSet<Object> vtmp = null;

		log.debug("Matching up " + a.getListing(base) + " and "
				+ b.getListing(base) + "...");

		// any in 'a' that are not in 'b' had better be recently-deleted
		vtmp = new HashSet<Object>(a.getLeafVertices());
		vtmp.removeAll(b.getLeafVertices());
		if (!vtmp.isEmpty() && !sce.getRemovedVertices().containsAll(vtmp)) {
			log.debug("\tOld has vertices not in new (and not newly removed)");
			return false;
		}

		// any in 'b' that are not in 'a' had better be newly-added
		vtmp = new HashSet<Object>(b.getLeafVertices());
		vtmp.removeAll(a.getLeafVertices());
		if (!vtmp.isEmpty() && !sce.getAddedVertices().containsAll(vtmp)) {
			log.debug("\tNew has vertices not in old (and not newly added)");
			return false;
		}
		log.debug("\t Matched.");

		return true;
	}

	/**
	 * if cluster 'a' is equivalent, under the new clustering, to 'b'
	 * (has same leaves, with 'b' possibly getting 'added' vertices, but all
	 * ole ones are the same in both -- or 'b' has less vertices, these belonging
	 * all to the 'deleted' set)
	 * => mark unmatched children of 'a' as removed from 'a'
	 * => mark unmatched children of 'b' as added to 'a' (but use matches if possible)
	 * The HCE is updated to reflect additions and removals
	 *
	 * Note that if 'a' ~= 'b', then since 'b' is in the new clustering, 
	 * 'a' will be in the new clustering too.
	 */
	private boolean matchClusters(Set<Cluster> u1, Set<Cluster> u2,
			Set<Cluster> s1, Set<Cluster> s2, Cluster a, Cluster b,
			StructureChangeEvent sce, HierarchyChangeEvent hce, BaseGraph base) {

		log.debug("Matching " + a.getListing(base) + " against "
				+ b.getListing(base));

		if (!matches(a, b, sce)) {
			log.debug("No match is possible for this pair.");
			return false;
		}

		log.debug("Match made => removing unwanted children");

		// remove unmatched children of 'a' from 'a'
		for (int i = 0; i < a.getChildCount(); i++) {
			Cluster ca = (Cluster) a.getChildAt(i);
			if (u1.contains(ca)) {
				u1.remove(ca);
				log.debug("Hmm, " + ca
						+ " is in u1, lets see if I can remove it...");
				if (!hce.getMatchedClusters().containsValue(ca)) {
					hce.insertRemovedCluster(a, ca);
					log.debug("Removed " + ca.getListing(base) + " from "
							+ a.getListing(base));

					// remove descendants of 'ca' from the 'toRemove' from-list
					ArrayList<Cluster> allRemoved = new ArrayList(hce
							.getRemovedClusters().keySet());
					for (Cluster cca : allRemoved) {
						if (cca.isNodeAncestor(ca))
							hce.getRemovedClusters().remove(cca);
						log.debug("removed child " + cca.getListing(base)
								+ " accounted for by removed parent");
					}
				}
			} else {
				log.debug("\tConsidering demise of " + ca.getListing(base));
				boolean matchFound = false;
				for (int j = 0; j < b.getChildCount(); j++) {
					Cluster cb = (Cluster) b.getChildAt(j);
					if (matches(ca, cb, sce)) { //  && hce.getMatchedClusters().get(cb) != c) {
						log.debug("Not removing: match was found with "
								+ cb.getListing(base));
						matchFound = true;
						if (hce.getMatchedClusters().get(cb) != ca) {
							log
									.debug("[HEY] But there was a previous match already: removing it");
							matchFound = false;
						}
						break;
					}
				}
				if (!matchFound) {
					hce.insertRemovedCluster(a, ca);
					log.debug("Removed " + ca.getListing(base) + " from "
							+ a.getListing(base));
				}
			}
		}

		log.debug("And => adding new siblings");

		// add children of 'b' to 'a', using only matches
		for (int i = 0; i < b.getChildCount(); i++) {
			Cluster cb = (Cluster) b.getChildAt(i);

			// if matched, but still not a child, may have to add too
			if (hce.getMatchedClusters().containsKey(cb)) {
				Cluster ca = hce.getMatchedClusters().get(cb);

				// avoid adding if already there
				if (!a.isNodeChild(ca)) {
					log.debug("matched, but still not child");
					hce.insertAddedCluster(a, ca);
					log.debug("Added " + ca.getListing(base) + " to "
							+ a.getListing(base));
				}

				// if b itself is added, cb is redundant
				hce.getMatchedClusters().remove(cb);
			} else {
				// if unmatched, it is not currently a child of 'a'

				if (u2.contains(cb)) {
					log.debug("not matched and in U2");
					u2.remove(cb);
					hce.insertAddedCluster(a, cb);
					log.debug("Added " + cb.getListing(base) + " to "
							+ a.getListing(base));
				} else {
					// u2 does not contain cb... how can this be?
					log.debug("Not matched and NOT in unmatched ?!? "
							+ dump("un", u2));
					hce.insertAddedCluster(a, cb);
					log.debug("Added " + cb.getListing(base) + " to "
							+ a.getListing(base));
				}
			}

			// otherwise, it was matched *and* a child
		}

		// remove eclipsed clusters from s1, s2, u1 and u2
		// FIXME: removed, must iron out other bugs before "optimization time"

		log.debug("End-of-match: Matched new " + b + " ~= old " + a);

		u1.remove(a);
		u2.remove(b);

		// check to see if match could have been deeper

		// add the match
		hce.getMatchedClusters().put(b, a);

		return true;
	}

	/**
	 * Test engine. 
	 * "Creates" and
	 * "Updates" to whatever you pass in the constructor argument
	 */
	public static class TestEngine implements ClusteringEngine {
		public Cluster first, second;
		public HierarchyChangeEvent change;

		public TestEngine(Cluster first, Cluster second) {
			this.first = first;
			this.second = second;
		}

		public void save(Element e) {
		}

		public void restore(Element e) {
		}

		public Cluster createHierarchy(BaseGraph b, Object r) {
			return first;
		}

		public Cluster updateHierarchy(Cluster root, BaseGraph base,
				StructureChangeEvent sce, HierarchyChangeEvent hce) {
			hce.getSource().createChangeEventFor(second, sce, hce);
			change = hce;
			return second;
		}
	}

	/**
	 * Return all parents of a given set of clusters - avoiding those already
	 * present in the original set.
	 * (note that this need not be a slice)
	 */
	private HashSet<Cluster> getParents(HashSet<Cluster> s, BaseGraph base) {
		HashSet<Cluster> tmp = new HashSet<Cluster>();
		tmp.addAll(s);
		s.clear();
		for (Cluster c : tmp) {
			Cluster p = c.getParentCluster();
			if (p != null && !tmp.contains(p)) {
				// being a set, repeated addition has no consequences
				s.add(p);
				log.debug("...Added " + p.getListing(base) + " ("
						+ p.hashCode() + ")");
			}
		}
		return s;
	}

	/**
	 * Performs the specified hierarchyChange on this hierarchy. 
	 * After perfoming, rethrows the event to all listeners
	 */
	public void hierarchyChangePerformed(HierarchyChangeEvent evt) {

		// FIXME: this method has given many, many problems in the past

		log.debug("HierarchyChangeEvent received at ClusterHierarchy: "
				+ evt.getDescription());

		// remove removed clusters from their old parents
		for (Map.Entry<Cluster, ArrayList<Cluster>> e : evt
				.getRemovedClusters().entrySet()) {
			for (Cluster c : e.getValue()) {
				e.getKey().remove(c);
			}
		}

		// add added clusters to their new parents
		for (Map.Entry<Cluster, ArrayList<Cluster>> e : evt.getAddedClusters()
				.entrySet()) {
			for (Cluster c : e.getValue()) {
				if (c.getRootCluster() != root) {
					c = evt.getMatchedVersion(c);
				}
				e.getKey().add(c);
			}
		}

		// add added edges, and propagate up the tree
		for (Edge e : evt.getAddedEdges()) {
			Cluster p = null;
			Cluster c = null;
			Cluster src = root.getLastClusterFor(e.getSource());
			if (src == null) {
				// target is not inside the current graph... matched?
				log.debug("\t Warning... src is missing");
			}
			for (c = src; c != null; c = c.getParentCluster()) {
				c.getOutgoing().add(e);
			}
			if (src != null)
				p = src.getParentCluster();
			if (p != null)
				p.updateLocalOutgoingAfterInsert(src);
			Cluster dst = root.getLastClusterFor(e.getTarget());
			if (dst == null) {
				// target is not inside the current graph... matched?
				log.debug("\t Warning... dst is missing");
			}
			for (c = dst; c != null; c = c.getParentCluster()) {
				c.getIncoming().add(e);
			}
			if (dst != null)
				p = dst.getParentCluster();
			if (p != null)
				p.updateLocalOutgoingAfterInsert(dst);
		}

		// FIXME: implicit removes (such as after vertex removals)
		// will fail; caches must be updated for them to work.
		for (Edge e : evt.getRemovedEdges()) {
			Cluster p = null;
			Cluster c = null;
			Cluster src = root.getLastClusterFor(e.getSource());
			for (c = src; c != null; c = c.getParentCluster()) {
				c.getOutgoing().remove(e);
			}
			p = src.getParentCluster();
			if (p != null)
				p.updateLocalOutgoingAfterDelete(src);
			Cluster dst = root.getLastClusterFor(e.getTarget());
			for (c = dst; c != null; c = c.getParentCluster()) {
				c.getIncoming().remove(e);
			}
			p = (dst == null) ? null : dst.getParentCluster();
			if (p != null)
				p.updateLocalOutgoingAfterDelete(dst);
		}

		// rebuild the entire cluster hierarchy 
		// FIXME: should not be needed
		root.rebuildFromHere();

		// rethrow for listeners
		log.info("Refiring HCE to all " + hierarchyListeners.size()
				+ " listeners");
		fireHierarchyChangeEvt(evt);
	}
}
