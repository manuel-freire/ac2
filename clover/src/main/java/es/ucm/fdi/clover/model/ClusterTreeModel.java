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
 * ClusterTreeModel.java
 *
 * Created on June 9, 2006, 2:30 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.model;

import es.ucm.fdi.clover.event.ClusteringChangeEvent;
import es.ucm.fdi.clover.event.ClusteringChangeListener;
import es.ucm.fdi.clover.event.HierarchyChangeEvent;
import es.ucm.fdi.clover.event.HierarchyChangeListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import org.apache.log4j.Logger;

/**
 *
 * @author mfreire
 */
public class ClusterTreeModel extends DefaultTreeModel implements
		ClusteringChangeListener, HierarchyChangeListener {

	private ClusteredGraph graph;
	private ClusterHierarchy hierarchy;
	private DefaultMutableTreeNode root;

	/** the logger */
	private Logger log = Logger.getLogger(ClusterTreeModel.class);

	/** list of listeners */
	private ArrayList listeners = new ArrayList();

	/**
	 * Creates a new instance of ClusterTreeModel
	 */
	public ClusterTreeModel() {
		super(new DefaultMutableTreeNode("<no clustering>"));
	}

	public void setClusteredGraph(ClusteredGraph graph) {
		if (hierarchy != graph.getHierarchy()) {
			if (hierarchy != null) {
				hierarchy.removeHierarchyChangeListener(this);
				graph.removeClusteringChangeListener(this);
			}
			log.debug("adding listeners to " + graph.getClass().getName() + "#"
					+ graph.hashCode());
			graph.getHierarchy().addHierarchyChangeListener(this);
			graph.addClusteringChangeListener(this);

			this.graph = graph;
			this.hierarchy = graph.getHierarchy();

			HierarchyChangeEvent evt = new HierarchyChangeEvent(graph
					.getHierarchy(), "tree switched hierarchy");
			evt.setRootChange(null);
			hierarchyChangePerformed(evt);
		}
	}

	public void hierarchyChangePerformed(HierarchyChangeEvent evt) {

		if (log.isDebugEnabled()) {
			log
					.debug("Received hierarchy change evt!\n"
							+ evt.getDescription());
		}

		if (evt.isRootChange()) {
			root = hierarchy.getRoot();
			System.err.println("Root is now " + root);
			setRoot(root);
			reload();
			return;
		}

		// removed clusters have lost their parent; cannot use 'removeFromParent'        
		for (Map.Entry<Cluster, ArrayList<Cluster>> e : evt
				.getRemovedClusters().entrySet()) {
			int[] positions = new int[e.getValue().size()];
			Object[] clusters = new Object[e.getValue().size()];
			Collections.sort(e.getValue(), new Comparator<Cluster>() {
				public int compare(Cluster a, Cluster b) {
					return a.getOldPos() - b.getOldPos();
				}
			});
			int i = 0;
			for (Cluster c : e.getValue()) {
				positions[i] = c.getOldPos();
				clusters[i] = c;
				i++;
			}
			nodesWereRemoved(e.getKey(), positions, clusters);
		}

		for (Map.Entry<Cluster, ArrayList<Cluster>> e : evt.getAddedClusters()
				.entrySet()) {
			int[] positions = new int[e.getValue().size()];
			int i = 0;
			for (Cluster c : e.getValue()) {
				positions[i] = e.getKey().getIndex(c);
				i++;
			}
			try {
				nodesWereInserted(e.getKey(), positions);
			} catch (Exception ex) {
				log.warn("Bad update on tree; reloading");
				reload();
				return;
			}
		}

		for (Cluster c : evt.getChangedClusters()) {
			nodeChanged(c);
		}
	}

	public ClusterHierarchy getHierarchy() {
		return hierarchy;
	}

	public void clusteringChangePerformed(ClusteringChangeEvent evt) {
		log.debug("Received structure change evt!");
		try {
			for (ClusteringChangeEvent.ClusteringAction a : evt.getCollapsed()) {
				nodeChanged(a.getCluster());
			}
			for (ClusteringChangeEvent.ClusteringAction a : evt.getExpanded()) {
				nodeChanged(a.getCluster());
			}
		} catch (Exception e) {
			log.warn("Caught exception while managing a CCE; reloading", e);
			reload();
		}
	}

	/**
	 * Returns the child of parent at index index in the parent's child array.
	 */
	public Object getChild(Object parent, int index) {
		return ((Cluster) parent).getChildAt(index);
	}

	/**
	 * Returns the child count of a parent
	 */
	public int getChildCount(Object parent) {
		return ((Cluster) parent).getChildCount();
	}

	/**
	 * Returns the index of child in parent.
	 */
	public int getIndexOfChild(Object parent, Object child) {
		return ((Cluster) parent).getIndex((Cluster) child);
	}

	/**
	 * Returns the root of the tree.
	 */
	public MutableTreeNode getRoot() {
		return root;
	}

	/**
	 * Returns true if node is a leaf.
	 */
	public boolean isLeaf(Object node) {
		return (((Cluster) node).isLeafCluster());
	}
}
