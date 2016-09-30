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
 * FileTreeModel.java
 *
 * Created on September 17, 2006, 7:34 PM
 *
 */

package es.ucm.fdi.ac.extract;

import es.ucm.fdi.util.FileUtils;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Stack;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * A tree-model that is built upon a filesystem, but not supported by it
 * (allowing removes that are not echoed to the filesystem).
 *
 * @author mfreire
 */
public class FileTreeModel extends DefaultTreeModel {

	private FileTreeNode root;

	/**
	 * Creates a new instance of FileTreeModel
	 */
	public FileTreeModel() {
		super(new FileTreeNode(null, null));
		this.root = (FileTreeNode) getRoot();
	}

	/**
	 * Add a new file, compressed file or folder into the system.
	 * @param f
	 * @return
	 */
	public TreePath addSource(File f) {
		try {
			String name = f.getName();
			FileTreeNode n = new FileTreeNode(f, root, FileUtils
					.canUncompress(f));
			insertNodeInto(n, root, findIndexFor(n));
			TreePath p = new TreePath(new Object[] { root, n });
			return p;
		} catch (Exception e) {
			System.err.println("Error reading '" + f.getAbsolutePath() + "': "
					+ e);
		}
		return null;
	}

	/**
	 * Yes, this is insertion-sort, and I know it's evil. If it bites me, I'll
	 * change it. But it is much better than not sorting.
	 */
	private int findIndexFor(FileTreeNode n) {
		for (int pos = 0; pos < root.getChildCount(); pos++) {
			FileTreeNode brother = (FileTreeNode) root.getChildAt(pos);
			String other = brother.getFile().getAbsolutePath();
			if (n.getFile().getAbsolutePath().compareToIgnoreCase(other) < 0) {
				return pos;
			}
		}
		return root.getChildCount();
	}

	/**
	 * Returns the file for a given treepath
	 */
	public File getFileFor(TreePath tp) {
		return ((FileTreeNode) tp.getLastPathComponent()).getFile();
	}

	/**
	 * Finds all paths that match the given filter
	 */
	public TreePath[] findWithFilter(FileFilter ff, boolean onlyFiles,
			boolean recurseIfFound) {

		ArrayList<TreePath> al = new ArrayList<TreePath>();
		Stack<FileTreeNode> s = new Stack<FileTreeNode>();
		s.push(root);
		// System.err.println("FileFilter is "+ff);
		for (FileTreeNode n : root.getChildren()) {
			findInternal(n, ff, al, s, onlyFiles, recurseIfFound);
		}
		return al.toArray(new TreePath[al.size()]);
	}

	/**
	 * Finds all paths that match the given filter, starting from a node;
	 * If a parent matches, children will not be searched
	 */
	public void findInternal(FileTreeNode n, FileFilter ff,
			ArrayList<TreePath> found, Stack<FileTreeNode> s,
			boolean onlyFiles, boolean recurseIfFound) {

		n.refresh();

		s.push(n);

		//System.err.println("testing "+n.getFile());

		if (!onlyFiles || !n.getAllowsChildren()) {
			if (ff.accept(n.getFile())) {
				found.add(new TreePath(s.toArray()));
				//System.err.println("\t OK!");
				if (!recurseIfFound) {
					s.pop();
					return;
				}
			}
		}
		if (!n.isLeaf()) {
			for (FileTreeNode c : n.getChildren()) {
				findInternal(c, ff, found, s, onlyFiles, recurseIfFound);
			}
		}
		s.pop();
	}

	/**
	 * Returns a list with all terminal nodes
	 */
	public ArrayList<FileTreeNode> getAllTerminals() {
		ArrayList<FileTreeNode> al = new ArrayList<FileTreeNode>();
		getAllTerminals(root, al);
		return al;
	}

	/**
	 * Internal version of the above
	 */
	private void getAllTerminals(FileTreeNode n, ArrayList<FileTreeNode> al) {
		if (n.isLeaf()) {
			al.add(n);
		} else {
			for (FileTreeNode c : n.getChildren()) {
				getAllTerminals(c, al);
			}
		}
	}

	/**
	 * Clears the model
	 */
	public void clear() {
		root.getChildren().clear();
		reload();
	}
}
