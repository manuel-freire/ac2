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
import es.ucm.fdi.clover.model.ClusteredGraph;
import java.util.ArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Contains changes performed to a clustering (to what is expanded and collapsed
 * within that clustering, actually).
 *
 * The event's contents are designed such that performing all collapses in order
 * and then all expansions in order will be correct. Doing things differently,
 * however, may not (example: expanding something may allow new, different things to 
 * be expanded).
 *
 * @author mfreire
 */
public class ClusteringChangeEvent {

	private Log log = LogFactory.getLog(ClusteringChangeEvent.class);

	/** ClusteredGraph over which this event was created */
	private ClusteredGraph source;
	/** textual description for this event */
	private String description;

	/** collapse-sets, ordered so that all can be carried out sequentially */
	private ArrayList<ClusteringAction> collapsed;
	/** expand-sets, ordered so that all can be carried out sequentially */
	private ArrayList<ClusteringAction> expanded;
	/** initial PoI (a vertex) */
	private Object initialPoI;
	/** final PoI (a vertex) */
	private Object finalPoI;
	/** if true, collapsed/expanded lists are already sorted */
	private boolean sorted;

	/** 
	 * useful when considering whether to include this in an undo-history;
	 * you'll probably not want to include the *undo* itself into the history...
	 */
	private boolean undoEvent;

	/**
	 * Creates a new instance of a ClusteringChangeEvent
	 * @param source the ClusterGraph where this has been executed
	 * @param initialPoI the initial point of interest before execution
	 * @param finalPoI the final PoI after execution
	 * @param description a description of the event
	 */
	public ClusteringChangeEvent(ClusteredGraph source, Object initialPoI,
			Object finalPoI, String description) {
		this.source = source;
		this.description = description;
		this.initialPoI = initialPoI;
		this.finalPoI = finalPoI;
		expanded = new ArrayList<ClusteringAction>();
		collapsed = new ArrayList<ClusteringAction>();
		sorted = false;
		undoEvent = false;
	}

	public Object getInitialPoI() {
		return initialPoI;
	}

	public Object getFinalPoI() {
		return finalPoI;
	}

	public Object getSource() {
		return source;
	}

	/**
	 * Returns an event that, if applied after this one, will undo the changes.
	 */
	public ClusteringChangeEvent getUndoEvent() {

		// recreate event, switching around finalPoI and initialPoI
		ClusteringChangeEvent cce = new ClusteringChangeEvent(
				(ClusteredGraph) source, finalPoI, initialPoI, description
						+ "-undo");

		int maxLevel;

		// reverse expansions (turn them into reverse order collapses)
		maxLevel = 0;
		for (ClusteringAction a : expanded) {
			cce.collapsed.add(new ClusteringAction(a));
			maxLevel = Math.max(a.level, maxLevel);
		}
		for (ClusteringAction a : cce.collapsed) {
			a.level = maxLevel - a.level;
		}

		// reverse collapses (turn them into reverse order expansions)
		maxLevel = 0;
		for (ClusteringAction a : collapsed) {
			cce.expanded.add(new ClusteringAction(a));
			maxLevel = Math.max(a.level, maxLevel);
		}
		for (ClusteringAction a : cce.expanded) {
			a.level = maxLevel - a.level;
		}

		cce.sorted = true;
		cce.undoEvent = true;

		return cce;
	}

	public String getDescription() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n  Source: " + source);
		sb.append("\n  Description: " + description);
		if (!expanded.isEmpty()) {
			sb.append("\n\t Expanded: ");
			for (ClusteringAction c : expanded) {
				sb.append("\n\t" + c.getCluster());
				sb.append("\n\t" + c.getLevel());
				if (c.structureChange != null) {
					sb.append("\n\t" + c.getStructureChange().getDescription());
				}
			}
		}
		if (!collapsed.isEmpty()) {
			sb.append("\n\t Collapsed: ");
			for (ClusteringAction c : collapsed) {
				sb.append("\n\t" + c.getCluster());
				sb.append("\n\t" + c.getLevel());
				if (c.structureChange != null) {
					sb.append("\n\t" + c.getStructureChange().getDescription());
				}
			}
		}
		return sb.toString();
	}

	public ArrayList<ClusteringAction> getExpanded() {
		return expanded;
	}

	public ArrayList<ClusteringAction> getCollapsed() {
		return collapsed;
	}

	/**
	 * Sort the changes in a StructuredChangeEvent into "levels", so that
	 * levels can be executed sequentially (and changes within a single
	 * level do not affect each other).
	 */
	private void sortChanges() {
		// changes were already sorted
		if (sorted)
			return;

		// sort out the clusters by level; could be much more efficient
		boolean somethingChanged;
		do {
			somethingChanged = false;
			for (ClusteringAction a : collapsed) {
				for (ClusteringAction b : collapsed) {
					// if b parent of a, a must be collapsed before b
					if (b.cluster == a.cluster.getParentCluster()) {
						if (b.level <= a.level) {
							b.level = a.level + 1;
							somethingChanged = true;
						}
					}
				}
			}
			for (ClusteringAction a : expanded) {
				for (ClusteringAction b : expanded) {
					// if a parent of b, a must be expanded before b
					if (a.cluster == b.cluster.getParentCluster()) {
						if (b.level <= a.level) {
							b.level = a.level + 1;
							somethingChanged = true;
						}
					}
				}
			}
		} while (somethingChanged);
		sorted = true;
	}

	/**
	 * populates the StructureChangeEvent and level fields, and send events
	 * to the graph. If it is not going to get sent anywhere, then pass in 
	 * null and you at least get the level fields right.
	 */
	public void commit(ClusteredGraph g) {

		// sort the changes, so that they can be executed sequentially by levels
		sortChanges();

		// if no graph, no need to actually *do* anything
		if (g == null)
			return;

		// then do the collapse levels
		StructureChangeEvent sce;
		int actionsLeft;

		actionsLeft = collapsed.size();
		for (int level = 0; /**/; level++) {

			for (ClusteringAction ca : collapsed) {
				if (ca.getLevel() != level)
					continue;

				sce = new StructureChangeEvent(g.getBase(),
						StructureChangeEvent.ChangeType.Clustering);
				g.prepareCollapse(ca.getCluster(), sce);
				ca.structureChange = sce;
				g.structureChangePerformed(sce);
				actionsLeft--;
			}

			// end condition: all levels processed
			if (actionsLeft == 0) {
				break;
			}
		}

		// and then the expand levels
		actionsLeft = expanded.size();
		for (int level = 0; /**/; level++) {

			for (ClusteringAction ca : expanded) {
				if (ca.getLevel() != level)
					continue;

				sce = new StructureChangeEvent(g.getBase(),
						StructureChangeEvent.ChangeType.Clustering);
				g.prepareExpand(ca.getCluster(), sce);
				log.debug("Expanding a cluster: "
						+ ca.getCluster().getListing(g) + ": \n"
						+ ca.getCluster().dump());
				ca.structureChange = sce;
				g.structureChangePerformed(sce);
				actionsLeft--;
			}

			// end condition: all levels processed
			if (actionsLeft == 0) {
				break;
			}
		}
	}

	public void addCollapsed(Cluster c) {
		collapsed.add(new ClusteringAction(c));
	}

	public void addExpanded(Cluster c) {
		expanded.add(new ClusteringAction(c));
	}

	public static class ClusteringAction {
		private Cluster cluster;
		/** it is only safe to execute level N when level N-1 is over */
		private int level = 0;
		private StructureChangeEvent structureChange;

		private ClusteringAction(Cluster cluster) {
			this.cluster = cluster;
		}

		private ClusteringAction(ClusteringAction ca) {
			this.cluster = ca.cluster;
			this.level = ca.level;
			this.structureChange = null;
		}

		public Cluster getCluster() {
			return cluster;
		}

		public StructureChangeEvent getStructureChange() {
			return structureChange;
		}

		public int getLevel() {
			return level;
		}
	}

	public boolean isUndoEvent() {
		return undoEvent;
	}
}
