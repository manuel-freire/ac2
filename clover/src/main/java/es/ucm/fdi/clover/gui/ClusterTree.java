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
 * ClusterTree.java
 *
 * Created on July 28, 2003, 11:26 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.gui;

import es.ucm.fdi.clover.event.ClusteringChangeEvent;
import es.ucm.fdi.clover.event.HierarchyChangeEvent;
import es.ucm.fdi.clover.view.ClusterView;
import es.ucm.fdi.clover.view.ViewHelper;
import es.ucm.fdi.clover.view.Highlighter;
import es.ucm.fdi.clover.model.Cluster;
import es.ucm.fdi.clover.model.ClusterHierarchy;
import es.ucm.fdi.clover.model.ClusterTreeModel;
import es.ucm.fdi.clover.model.ClusteredGraph;
import es.ucm.fdi.clover.model.ViewGraph;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.apache.log4j.Logger;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.DefaultGraphCell;

/**
 * A tree that allows finegrained control over expansion and collapse of graph clusters. 
 * Also allows cluster editing, by dragging&dropping clusters into or out of each other.
 *
 * @author  mfreire
 */
public class ClusterTree extends DTree implements GraphSelectionListener {

	static private Logger log = Logger.getLogger(ClusterTree.class);

	/** 
	 * the ClusterView to follow (tree nodes 'visible' in the view will be highlighted). 
	 * Must be set prior to component use 
	 */
	private ClusterView view;

	/** 
	 * Empty constructor.
	 * (does not bind to anything) 
	 */
	public ClusterTree() {
		addExpansionListener();
		setModel(new ClusterTreeModel());
		setCellRenderer(new ClusterTree.CheckedCellRenderer());
		this.

		setToggleClickCount(3);

		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {

				TreePath p = getPathForLocation(e.getX(), e.getY());
				if (view == null || view.getHighlighter() == null) {
					return;
				}
				if (p == null) {
					view.getHighlighter().clearHighlight();
					return;
				}

				log.debug("Mousing over " + p.getLastPathComponent());

				Object v = ((Cluster) p.getLastPathComponent()).getVertex();
				if (v == null) {
					return;
				}

				// vertex visible: show where it can be found
				if (view.getBase().containsVertex(v)) {
					view.getHighlighter().startSimpleVertexHighlightPlan(v,
							Highlighter.defaultHighlightVertexHue);
					return;
				}

				// vertex is not visible: show hypothetical clustering change
				ViewGraph g = view.getViewGraph();
				ClusteringChangeEvent cce;
				cce = ((ClusteredGraph) view.getBase())
						.createMakeVisibleEvent(v);
				if (cce == null) {
					return;
				}

				view.getHighlighter().startClusteringChangePlan(cce, v);
			}
		});

		setModelHelper(new DefaultClusterTreeModelHelper());
	}

	/**
	 * Edits a cluster-vertex node (actually -- just changes the name)
	 */
	public void editNode(Cluster node) {
		String s = JOptionPane.showInputDialog(this, "Cluster name",
				"Customize cluster name", JOptionPane.INFORMATION_MESSAGE);

		// user may cancel the dialog
		if (s == null) {
			return;
		}

		node.setName(s);
		((DefaultTreeModel) getModel()).nodeChanged(node);
	}

	/**
	 * How to do drag&drop on the clustering tree; this generates suitable
	 * HierarchyChangedEvents, and signals them to the actual hierarchy it's
	 * suscribed to, and *when* the hierarchy changes, it effects these
	 * changes on itself.
	 *
	 */
	public static class DefaultClusterTreeModelHelper implements
			DTree.ModelHelper {

		public void move(TreeModel sourceModel,
				ArrayList<TreeNode> sourceNodes, TreeModel destModel,
				TreeNode destParent, int index) {

			try {
				ClusterHierarchy h = ((ClusterTreeModel) sourceModel)
						.getHierarchy();
				HierarchyChangeEvent hce = new HierarchyChangeEvent(h,
						"Drag&Drop request");
				for (TreeNode n : sourceNodes) {
					Cluster c = (Cluster) n;
					hce.insertRemovedCluster(c.getParentCluster(), c);
					hce.insertAddedCluster((Cluster) destParent, c);
				}
				hce.augmentChangesWithAddedAndRemoved();
				h.hierarchyChangePerformed(hce);
				log.info("User asked for this hce to be performed: "
						+ hce.getDescription());
			} catch (Exception e) {
				log.warn("Exception trying to move clusters in tree: " + e);
				e.printStackTrace();
			}
		}

		public void copy(TreeModel sourceModel,
				ArrayList<TreeNode> sourceNodes, TreeModel destModel,
				TreeNode destParent, int index) {

			// copy not allowed here - because the leaves themselves should not be duped
			throw new UnsupportedOperationException(
					"Copy not allowed on cluster trees");
		}
	}

	/**
	 * Change the view that will be followed. Must be called before the tree is shown
	 * (otherwise, there will be no tree to show at all)
	 */
	public void setView(ClusterView view) {
		if (this.view == null) {
			// first time
		} else {
			if (this.view == view) {
				return;
			}
			this.view.removeGraphSelectionListener(this);
		}
		this.view = view;

		if (view != null) {
			view.addGraphSelectionListener(this);
			((ClusterTreeModel) getModel())
					.setClusteredGraph((ClusteredGraph) view.getBase());
			setCollapseState();
		}
	}

	/**
	 * Use different color for nodes that are visible
	 */
	protected class CheckedCellRenderer extends DefaultTreeCellRenderer {
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {
			Component c = super.getTreeCellRendererComponent(tree, value, sel,
					expanded, leaf, row, hasFocus);

			if (view == null) {
				return c;
			}

			if (view.getBase().containsVertex(((Cluster) value).getVertex())) {
				setBackgroundNonSelectionColor(new Color(0.1f, 0.6f, .9f));
				setBackgroundSelectionColor(new Color(1f, 0.6f, 1f));
			} else {
				setBackgroundNonSelectionColor(null);
				setBackgroundSelectionColor(null);
			}

			return c;
		}
	}

	/**
	 * Get a string representation for that node.
	 */
	public String convertValueToText(Object value, boolean selected,
			boolean expanded, boolean leaf, int row, boolean hasFocus) {
		return (view == null) ? value.toString() : view.getViewGraph()
				.getVertexLabel(((Cluster) value).getVertex());
	}

	/**
	 * When this is called, the tree will be collapsed as much as possible, but 
	 * keeping all nodes visible in the graph visible. Never collapses root.
	 */
	public void setCollapseState() {

		for (int i = 1; i < getRowCount(); i++) {
			TreePath p = getPathForRow(i);
			Cluster c = (Cluster) p.getLastPathComponent();
			if (view.getSlice().contains(c) && isExpanded(i)) {
				collapseRow(i);

				// restart the count - may not be necessary
				i = 0;
			}
		}
	}

	/**
	 * Should be called in the constructor to set a mouse listener.
	 */
	private void addExpansionListener() {
		MouseListener ml = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int selRow = getRowForLocation(e.getX(), e.getY());
				TreePath selPath = getPathForLocation(e.getX(), e.getY());
				if (selRow != -1) {
					if (e.getButton() == MouseEvent.BUTTON1) {
						select(selRow, selPath);
					} else if (e.getButton() == MouseEvent.BUTTON3
							&& e.getClickCount() > 1) {
						setVisible(selRow, selPath);
					}
				}
			}
		};

		addMouseListener(ml);
	}

	/**
	 * Selects a row (here and in the underlying view)
	 */
	protected void select(int selRow, TreePath selPath) {
		log.debug("::single click::");
		Cluster c = (Cluster) selPath.getLastPathComponent();
		DefaultGraphCell cell = ViewHelper.getVertexCell(view, c.getVertex());
		view.setSelectionCell(cell);
	}

	/**
	 * double click a visible node means "i want this cell expanded"
	 * double click a non-visible node means "i want this cell visible"
	 */
	protected void setVisible(int selRow, TreePath selPath) {
		log.debug("::double click::");
		Cluster c = (Cluster) selPath.getLastPathComponent();
		DefaultGraphCell cell = ViewHelper.getVertexCell(view, c.getVertex());

		// this reports AFTER doing whatever on the tree. So we have to reverse
		ClusteredGraph cg = (ClusteredGraph) view.getBase();

		if (cg.containsVertex(c.getVertex())) {
			return;
		}

		view.makeVertexVisible(c.getVertex());
	}

	/**
	 * Used to respond to changes in the graph - like focusCell & the like    
	 */
	public void valueChanged(GraphSelectionEvent e) {
		log.debug("::graph sel event::");
		Object[] cells = e.getCells();
		for (int i = 0; i < cells.length; i++) {
			log.debug("\t" + cells[i].getClass() + "\t" + e.isAddedCell(i));
			if (e.isAddedCell(i)) {

				// find cell index in tree
				for (int j = 0; j < getRowCount(); j++) {
					Object o = (Object) getPathForRow(j).getLastPathComponent();
					if (o == cells[i]) {
						setSelectionRow(j);
						break;
					}
				}
			}
		}
	}
}
