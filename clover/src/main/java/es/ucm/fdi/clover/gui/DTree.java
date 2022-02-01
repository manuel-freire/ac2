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

package es.ucm.fdi.clover.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.TooManyListenersException;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A tree component that extends the default JTree, allowing drag&drop
 * in a way similar to that found in NetBeans
 *
 * @author mfreire
 */
public class DTree extends JTree {

	private static Log log = LogFactory.getLog(DTree.class);

	private TreeBranch dragTreeBranch;
	private TreeBranch destTreeBranch;
	private static int pathMargin = 2;

	private boolean alwaysCopyOnDrag = false;
	private boolean copyOnThisDrag = false;

	private static ModelHelper defaultHelper = null;
	private ModelHelper modelHelper = null;

	/** 
	 * Creates a new DTree 
	 */
	public DTree() {
		super();

		if (defaultHelper == null) {
			defaultHelper = new DefaultModelHelper();
		}
		modelHelper = defaultHelper;

		setAutoscrolls(true);
		setTransferHandler(new TransferHandler("branch"));
		setDragEnabled(true);

		try {
			getDropTarget().addDropTargetListener(new Dropper(this));
		} catch (TooManyListenersException ex) {
			ex.printStackTrace();
		}

		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				// once this is executed, mouseDragged will not be signalled again until dropped
				JComponent c = (JComponent) e.getSource();
				TransferHandler handler = c.getTransferHandler();

				int onmask = MouseEvent.CTRL_DOWN_MASK
						| MouseEvent.BUTTON1_DOWN_MASK;
				int offmask = MouseEvent.SHIFT_DOWN_MASK;

				copyOnThisDrag = alwaysCopyOnDrag
						|| ((e.getModifiersEx() & (onmask | offmask)) == onmask);

				handler.exportAsDrag(c, e,
						(copyOnThisDrag ? TransferHandler.COPY
								: TransferHandler.MOVE));

				DTree srcTree = (DTree) e.getSource();
				TreePath[] paths = srcTree.getSelectionPaths();
				dragTreeBranch = new TreeBranch(srcTree, paths);
			}
		});
	}

	public void setAlwaysCopyOnDrag(boolean c) {
		alwaysCopyOnDrag = c;
	}

	public void setModelHelper(ModelHelper helper) {
		this.modelHelper = helper;
	}

	private static class Dropper implements DropTargetListener {

		private DTree tree;

		public Dropper(DTree tree) {
			this.tree = tree;
		}

		public void dragOver(DropTargetDragEvent dtde) {
			int x = (int) dtde.getLocation().getX();
			int y = (int) dtde.getLocation().getY();
			TreePath destPath = tree.getPathForLocation(x, y);
			int offset = 0;
			if (tree.getPathForLocation(x, y + DTree.pathMargin) != destPath) {
				offset = 1;
			} else if (tree.getPathForLocation(x, y - DTree.pathMargin) != destPath) {
				offset = -1;
			}
			tree.destTreeBranch = new TreeBranch(tree, destPath, offset);
			tree.repaint();
		}

		public void dragEnter(DropTargetDragEvent dtde) {
		}

		public void dropActionChanged(DropTargetDragEvent dtde) {
		}

		public void dragExit(DropTargetEvent dte) {
		}

		public void drop(DropTargetDropEvent dtde) {
		}
	}

	private static class TreeBranch {
		private DTree tree;
		private TreePath[] path;
		private int offset = 0; // -1 = before ; 0 = none/in ; 1 = after

		/**
		 * Source
		 */
		public TreeBranch(DTree tree, TreePath[] path) {
			this.tree = tree;
			this.path = path;
		}

		/**
		 * Dest
		 */
		public TreeBranch(DTree tree, TreePath path, int offset) {
			this(tree, new TreePath[] { path });
			this.offset = offset;
		}

		public String toString() {
			return " T:" + tree.hashCode() + " P:" + path + " O:" + offset;
		}
	}

	public interface ModelHelper {
		public void move(TreeModel sourceModel,
				ArrayList<TreeNode> sourceNodes, TreeModel destModel,
				TreeNode destParent, int index);

		public void copy(TreeModel sourceModel,
				ArrayList<TreeNode> sourceNodes, TreeModel destModel,
				TreeNode destParent, int index);
	}

	/** position of last line painted during a drag&drop operation; -1 if none */
	private int previousDroplineY = -1;

	public void paint(Graphics g) {

		// clear previous dropline (if any)
		if (previousDroplineY != -1) {
			g.setColor(Color.white);
			paintLine(g, 0, previousDroplineY, getWidth());
			g.setColor(Color.black);
			previousDroplineY = -1;
		}

		super.paint(g);

		// paint new dropline (if needed)
		if (destTreeBranch != null
				&& (destTreeBranch.path != null && destTreeBranch.offset != 0)) {
			if (destTreeBranch.offset != 0) {
				Rectangle rect = getPathBounds(destTreeBranch.path[0]);
				if (rect != null) {
					int y = (destTreeBranch.offset == 1) ? rect.y + rect.height
							: rect.y;
					paintLine(g, rect.x, y, rect.width);
					previousDroplineY = y;
				}
			}
		}
	}

	/**
	 * paints a line in to indicate a drop-point in the tree
	 */
	private void paintLine(Graphics g, int x, int y, int width) {
		int[] X = new int[] { x - 3, x + 2, x - 3 };
		int[] Y = new int[] { y - 4, y, y + 4 };
		g.fillPolygon(X, Y, 3);
		g.drawLine(x, y, x + width, y);
	}

	/**
	 * Used to drop stuff in the tree; must be called like this, since we are
	 * using swing's support to drag&drop user-defined 'properties'.
	 */
	public TreeBranch getBranch() {
		return dragTreeBranch;
	}

	/**
	 * Used to drop stuff in the tree; must be called like this, since we are
	 * using swing's support to drag&drop user-defined 'properties'.
	 */
	public void setBranch(TreeBranch sourceTreeBranch) {

		ArrayList<TreeNode> sources = new ArrayList<TreeNode>();
		for (TreePath tp : sourceTreeBranch.path) {
			sources.add((MutableTreeNode) tp.getLastPathComponent());
		}

		MutableTreeNode dest = (MutableTreeNode) destTreeBranch.path[0]
				.getLastPathComponent();
		MutableTreeNode destP = (MutableTreeNode) dest.getParent();

		// avoid inserting into self, or into children of self
		TreeNode t = dest;
		while (t != null) {
			if (sources.contains(t)) {
				return;
			}
			t = (TreeNode) t.getParent();
		}

		TreeModel sourceModel = sourceTreeBranch.tree.getModel();
		TreeModel destModel = destTreeBranch.tree.getModel();

		// calculate real parent and offset within parent
		TreeNode realDest;
		int offset;
		switch (destTreeBranch.offset) {
		case 0:
			if (!dest.isLeaf() || dest.getAllowsChildren()) {
				realDest = dest;
				offset = dest.getChildCount();
			} else {
				return;
			}
			break;
		case -1:
			offset = destP.getIndex(dest);
			realDest = destP;
			break;
		case 1:
			offset = destP.getIndex(dest) + 1;
			realDest = destP;
			break;
		default:
			// weird!
			return;
		}

		// do it!
		if (!sourceTreeBranch.tree.copyOnThisDrag) {
			modelHelper.move(sourceModel, sources, destModel, realDest, offset);
		} else {
			modelHelper.copy(sourceModel, sources, destModel, realDest, offset);
		}

		// clear line
		destTreeBranch = null;
		repaint();
	}

	/**
	 * Helper class with default move operations
	 */
	protected static class DefaultModelHelper implements ModelHelper {

		public void move(TreeModel sourceModel,
				ArrayList<TreeNode> sourceNodes, TreeModel destModel,
				TreeNode destParent, int index) {

			// remove the originals
			DefaultTreeModel dtm = (DefaultTreeModel) sourceModel;
			for (TreeNode n : sourceNodes) {
				dtm.removeNodeFromParent((MutableTreeNode) n);
			}

			// re-insert them where necessary
			dtm = (DefaultTreeModel) destModel;
			MutableTreeNode dp = (MutableTreeNode) destParent;
			for (TreeNode n : sourceNodes) {
				dtm.insertNodeInto((MutableTreeNode) n, dp, index);
			}
		}

		public void copy(TreeModel sourceModel,
				ArrayList<TreeNode> sourceNodes, TreeModel destModel,
				TreeNode destParent, int index) {

			ArrayList<TreeNode> copiedSources = (ArrayList<TreeNode>) sourceNodes
					.clone();

			// copy stuff
			for (int i = 0; i < sourceNodes.size(); i++) {
				Class c = sourceNodes.get(i).getClass();
				Constructor<TreeNode> constructor = null;
				try {
					constructor = c.getConstructor(c);
					TreeNode n = sourceNodes.get(i);
					sourceNodes.set(i, (TreeNode) constructor.newInstance(n));
				} catch (Exception e) {
					log
							.warn(
									"Need access to a copy constructor for class "
											+ c.getName()
											+ " in order to copy nodes around",
									e);
					return;
				}
			}

			// insert the copies
			DefaultTreeModel dtm = (DefaultTreeModel) destModel;
			MutableTreeNode dp = (MutableTreeNode) destParent;
			for (TreeNode n : sourceNodes) {
				dtm.insertNodeInto((MutableTreeNode) n, dp, index);
			}
		}
	}

	/**
	 * Small test-case to play around with
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		// TODO code application logic here
		JFrame jf = new JFrame();
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.getContentPane().add(
				new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(
						new DTree()), new JScrollPane(new DTree())));
		jf.setSize(400, 300);
		jf.setVisible(true);
	}
}
