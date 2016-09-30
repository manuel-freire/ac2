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
 * FileTreeNode.java
 *
 * Created on September 18, 2006, 5:39 PM
 *
 */

package es.ucm.fdi.ac.extract;

import es.ucm.fdi.util.FileUtils;
import es.ucm.fdi.util.SourceFileCache;
import es.ucm.fdi.util.XMLSerializable;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * The file nodes (cached from filesystem as needed; filtering
 * affects the cache). Node children are lazy-loaded by calling refresh as
 * needed.
 */
public class FileTreeNode implements MutableTreeNode, Comparable<FileTreeNode> {
	private File f;
	private byte[] sha1;
	private FileTreeNode p;
	private ArrayList<FileTreeNode> children;

	/**
	 * Deep copy constructor, required to drag&drop correctly
	 */
	public FileTreeNode(FileTreeNode original) {
		this(original.getFile(), null);
		if (original.getChildCount() != 0) {
			for (FileTreeNode c : original.getChildren()) {
				FileTreeNode cc = new FileTreeNode(c);
				cc.p = this;
			}
		}
	}

	public FileTreeNode(File source, FileTreeNode parent) {
		this(source, parent, false);
	}

	/**
	 * Creates a new FileTreeNode to wrap a single file. Adds a single level (or, in
	 * the case of an archive, adds the archives' content list -- at the very most).
	 * @param source
	 * @param parent
	 * @param isCompressed
	 */
	public FileTreeNode(File source, FileTreeNode parent, boolean isCompressed) {
		this.f = source;
		this.p = parent;
		children = null;

		if (f == null) {
			children = new ArrayList<FileTreeNode>();
		}

		if (isCompressed) {
			//System.err.println("Processing compressed file "+f);
			if (!FileUtils.canUncompress(f)) {
				throw new IllegalArgumentException(
						"Cannot list archive: unrecognized format");
			}
			initFromList(SourceFileCache.getArchiveListing(f));
		}
	}

	public byte[] getSha1() {
		return sha1;
	}

	public void setSha1(byte[] sha1) {
		this.sha1 = sha1;
	}

	private static class FileNameComparator implements Comparator<String> {
		public int compare(String s1, String s2) {
			String name1 = s1.substring(s1.lastIndexOf("/") + 1);
			String name2 = s2.substring(s2.lastIndexOf("/") + 1);
			String ext1 = name1.substring(name1.lastIndexOf(".") + 1)
					.toLowerCase();
			String ext2 = name2.substring(name2.lastIndexOf(".") + 1)
					.toLowerCase();

			int c1 = ext1.compareTo(ext2);
			return (c1 == 0) ? name1.compareTo(name2) : c1;
		}
	}

	private void initFromList(ArrayList<String> paths) {
		children = new ArrayList<FileTreeNode>();
		Collections.sort(paths, new FileNameComparator());
		for (String s : paths) {
			//System.err.println("Trying to add '"+s+"'");
			FileTreeNode n = new FileTreeNode(new File(f, s), this);
			n.children = new ArrayList();
			children.add(n);
			//System.err.println("Created inzipfile: "+n.getFile());            
		}
	}

	public File getFile() {
		return f;
	}

	public static class FileSorter implements Comparator<File> {
		public int compare(File o1, File o2) {
			return o1.getAbsolutePath().compareToIgnoreCase(
					o2.getAbsolutePath());
		}
	}

	public void refresh() {
		if (children == null) {
			children = new ArrayList<FileTreeNode>();

			// unix device files may yield "null" for f.listFiles() ...
			File[] files;
			if (f.isDirectory() && (files = f.listFiles()) != null) {
				Arrays.sort(files, new FileSorter());
				for (File c : files) {
					children.add(new FileTreeNode(c, this));
				}
			}
		}
	}

	public TreeNode getChildAt(int childIndex) {
		refresh();
		return children.get(childIndex);
	}

	public int getChildCount() {
		refresh();
		return children.size();
	}

	public TreeNode getParent() {
		return p;
	}

	public int getIndex(TreeNode node) {
		refresh();
		return children.indexOf(node);
	}

	public boolean getAllowsChildren() {
		return (f == null) ? true : (isLeaf() ? f.isDirectory() : true);
	}

	public boolean isLeaf() {
		refresh();
		return children.isEmpty();
	}

	public Enumeration children() {
		refresh();
		return Collections.enumeration(children);
	}

	public void insert(MutableTreeNode child, int index) {
		refresh();
		//            System.err.println("Inserting "+child+" into "+this+" at "+index);
		//            Thread.dumpStack();
		children.add(index, (FileTreeNode) child);
	}

	public void remove(int index) {
		refresh();
		children.remove(index);
	}

	public void remove(MutableTreeNode node) {
		refresh();
		children.remove(node);
	}

	public void setUserObject(Object object) {
		// not allowed
	}

	public void removeFromParent() {
		if (p != null) {
			p.remove(this);
			p = null;
		}
	}

	public void setParent(MutableTreeNode newParent) {
		if (p != null) {
			removeFromParent();
		}
		p = (FileTreeNode) newParent;
	}

	public ArrayList<FileTreeNode> getChildren() {
		return children;
	}

	public int compareTo(FileTreeNode o) {
		return getFile().getAbsolutePath().compareToIgnoreCase(
				o.getFile().getAbsolutePath());
	}
}
