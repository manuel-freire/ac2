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
package es.ucm.fdi.ac.extract;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.ucm.fdi.util.FileUtils;

/**
 * A tree-model that is built upon a filesystem, but not supported by it
 * (allowing removes that are not echoed to the filesystem).
 *
 * @author mfreire
 */
public class FileTreeModel extends DefaultTreeModel {

	private static final Logger log = LogManager.getLogger(FileTreeModel.class);

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
	 * @param f file/archive/folder to add
	 * @return the tree-path for the newly added file/archive/folder
	 */
	public TreePath addSource(File f) {
		try {
			log.info("Adding source: " + f);
			FileTreeNode n = new FileTreeNode(f, root, FileUtils
					.canUncompressPath(f));
			insertNodeInto(n, root, findIndexFor(n, root));
			TreePath p = new TreePath(new Object[] { root, n });
			log.info("Resulting path: " + p);
			return p;
		} catch (Exception e) {
			log.warn("Error reading '" + f.getAbsolutePath() + "': " + e);
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Add a copy of a fileTreeNode from another tree into this one
	 * @param fn
	 * @return
	 */
	public TreePath addSource(FileTreeNode fn) {
		FileTreeNode n = new FileTreeNode(fn);
		insertNodeInto(n, root, findIndexFor(n, root));
		n.setParent(root);
		TreePath p = new TreePath(new Object[] { root, n });
		return p;
	}

	/**
	 * Returns correct index for ordered insertion of child in parent
	 * @param n node to insert
	 * @param parent to insert into
	 * @return index to insert into, from 0 to parent.getChildCount() inclusive
	 */
	private static int findIndexFor(FileTreeNode n, FileTreeNode parent) {
		String key = n.getFile().getAbsolutePath();
		for (int i = 0; i < parent.getChildCount(); i++) {
			String other = ((FileTreeNode) parent.getChildAt(i)).getFile()
					.getAbsolutePath();
			if (key.compareToIgnoreCase(other) < 0) {
				return i;
			}
		}
		return parent.getChildCount();
	}

	/**
	 * Returns the file for a given treepath
	 */
	public FileTreeNode getNodeFor(TreePath tp) {
		return (FileTreeNode) tp.getLastPathComponent();
	}

	/**
	 * Finds all paths that match the given filter
	 */
	public TreePath[] findWithFilter(FileTreeFilter ff, boolean onlyFiles,
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
	public void findInternal(FileTreeNode n, FileTreeFilter ff,
			ArrayList<TreePath> found, Stack<FileTreeNode> s,
			boolean onlyFiles, boolean recurseIfFound) {

		n.refresh();

		s.push(n);

		log.debug("testing " + n.getFile() + " path: " + n.getPath());

		if (!onlyFiles || !n.getAllowsChildren()) {
			if (ff.accept(n)) {
				found.add(new TreePath(s.toArray()));
				log.debug("found " + n.getFile() + " to match!");
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
