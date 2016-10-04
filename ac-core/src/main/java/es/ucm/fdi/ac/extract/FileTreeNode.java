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
import es.ucm.fdi.util.archive.ArchiveFormat;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.regex.Pattern;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * The file nodes (cached from filesystem as needed; filtering
 * affects the cache). Node children are lazy-loaded by calling refresh as
 * needed.
 */
public class FileTreeNode implements MutableTreeNode, Comparable<FileTreeNode> {

	private static final Logger log = Logger.getLogger(FileTreeNode.class);

	/** backing file */
	private File f;
	/** original file, if any; only used when compressed, where f points to uncompressed version */
	private File original;
	/** hashcode for integrity verification purposes */
	private byte[] sha1;
	/** parent */
	private FileTreeNode p;

	/** null used to indicate "not fully initialized" */
	private ArrayList<FileTreeNode> children;

	/**
	 * Deep copy constructor, required to drag&drop correctly
	 */
	public FileTreeNode(FileTreeNode original) {
		this(original.getFile(), null);
		this.original = original.original;
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
	 * Creates a new FileTreeNode to wrap a single file. Adds a single level.
	 * @param source file to wrap
	 * @param parent of the new node
	 * @param isCompressed true if must uncompress to access
	 */
	public FileTreeNode(File source, FileTreeNode parent, boolean isCompressed) {
		this.f = source;
		this.p = parent;
		children = null;

		if (f == null) {
			children = new ArrayList<>();
		}

		if (isCompressed) {
			log.info("Adding compressed file: " + source);
			if (!FileUtils.canUncompressPath(f)) {
				throw new IllegalArgumentException(
						"Cannot list archive: unrecognized format");
			}
            try {
                File temp = Files.createTempDirectory("ac-temp").toFile();
                temp.deleteOnExit();
                FileUtils.getArchiverFor(f.getPath()).expand(f, temp);
                this.original = f;
                this.f = temp;
                log.info("Files for " + original.getPath() + " now at " + f.getPath());
            } catch (IOException ioe) {
                log.warn("error uncompressing bundled file for " + f, ioe);
            }
		}
	}

	public String getLabel() {
		if (original == null) {
			return f == null ? "invisible-root" : f.getName();
		} else {
			return original.getName();
		}
	}

	public byte[] getSha1() {
		return sha1;
	}

	public void setSha1(byte[] sha1) {
		this.sha1 = sha1;
	}

	public ArrayList<FileTreeNode> getLeafChildren() {
        ArrayList<FileTreeNode> al = new ArrayList<>();
        getLeafChildren(al);
        return al;
    }

	private void getLeafChildren(ArrayList<FileTreeNode> al) {
		refresh();
		if (getFile().isDirectory()) {
			for (FileTreeNode cn : getChildren()) {
				cn.getLeafChildren(al);
			}
		} else {
			if (getFile().exists()) {
				al.add(this);
			}
		}
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

	/**
	 * Returns a label-based path, instead of a real path.
	 * @return a label-based path which gives the impression of opening archives as folders, when they are
	 * actually uncompressed in temp folders that get stitched into the tree.
	 */
	public String getPath() {
		return (f == null) ? "" : getParent() == null ? "" + getLabel()
				: ((FileTreeNode) getParent()).getPath() + "/" + getLabel();
	}

	/**
	 * Returns the FileTreeNode that is a direct ancestor of the passed-in path.
	 * This can be 'this' (if no additional path-elements found in the path) or a few intermediate files (if not)
	 * @param path
	 * @return
	 */
	private FileTreeNode getOrCreateAncestor(String path) {
        String parentPath = getPath();
        if ( ! path.startsWith(parentPath)) {
            throw new IllegalArgumentException("Expected " + path + " to start with " + parentPath);
        }
        String newPath = path.substring(parentPath.length()+1);
        if ( ! newPath.contains("/")) {
            return this; // direct descendant
        } else {
            String newPathStart = newPath.substring(0, newPath.indexOf("/"));
            String targetPath = parentPath.isEmpty() ? newPathStart : parentPath + "/" + newPathStart;
            for (int i=0; i<getChildCount(); i++) {
                FileTreeNode child = (FileTreeNode)getChildAt(i);
                if (child.getPath().equals(targetPath)) {
                    return child.getOrCreateAncestor(path);
                }
            }
            FileTreeNode intermediateNode = new FileTreeNode(new File(targetPath), this, false);
            children.add(intermediateNode);
            intermediateNode.children = new ArrayList<>();
            return intermediateNode.getOrCreateAncestor(path);
        }
    }

	private void initFromList(ArrayList<String> paths) {
		children = new ArrayList<>();
		Collections.sort(paths, new FileNameComparator());
		for (String s : paths) {
            log.info("From within archive: '"+s+"'");
            FileTreeNode ancestor = getOrCreateAncestor(getPath() + "/" + s);
			boolean isCompressed = FileUtils.getArchiverFor(s) != null;
            FileTreeNode n = null;
            if (isCompressed) {
                try {
                    File temp = Files.createTempFile("ac-", "." + FileUtils.getExtension(s)).toFile();
                    FileUtils.getArchiverFor(f.getPath()).extractOne(f, s, temp);
                    log.info("Adding as inner path from temp archive '"+s+"'");
                    n = new FileTreeNode(temp, this, true);
                    n.original = new File(s);
                } catch (IOException ioe) {
                    log.warn("error uncompressing bundled file for " + f + "/" + s, ioe);
                }
            } else {
                log.info("Adding as inner path");
                n = new FileTreeNode(new File(s), this, false);
            }
			n.children = new ArrayList<>();
			ancestor.children.add(n);
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
			children = new ArrayList<>();

			// unix device files may yield "null" for f.listFiles() ...
			File[] files;
			if (f.isDirectory() && (files = f.listFiles()) != null) {
				Arrays.sort(files, new FileSorter());
				for (File c : files) {
					children.add(new FileTreeNode(c, this, FileUtils.canUncompressPath(c)));
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
