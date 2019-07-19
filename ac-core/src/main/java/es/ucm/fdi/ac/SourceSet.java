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

package es.ucm.fdi.ac;

import es.ucm.fdi.ac.extract.CompositeFilter;
import es.ucm.fdi.ac.extract.FileTreeModel;
import es.ucm.fdi.ac.extract.FileTreeNode;
import es.ucm.fdi.ac.extract.Hasher;
import es.ucm.fdi.util.FileUtils;
import es.ucm.fdi.util.XMLSerializable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.swing.tree.TreePath;
import org.apache.log4j.Logger;
import org.jdom2.Element;

/**
 * Describes a source and a filters used to arrive to a set of
 * submissions to analyze.
 * 
 * @author mfreire
 */
public class SourceSet implements XMLSerializable {

	private static final Logger log = Logger.getLogger(SourceSet.class);

	private final ArrayList<FileTreeNode> sourceRoots = new ArrayList<FileTreeNode>();
	private final CompositeFilter rootFilter = new CompositeFilter();
	private final CompositeFilter sourceFilter = new CompositeFilter();

	private FileTreeNode filteredTree = null;

	public SourceSet() {
	}

	public SourceSet(File d) {
		sourceRoots.add(new FileTreeNode(d, null));
	}

	/**
	 * The ugly way to initialize a sourceSet. Discards all filter
	 * information, and exports everything to a temporary folder.
	 * @param dn 
	 */
	public SourceSet(FileTreeNode dn) throws IOException {
		File d = Files.createTempDirectory("ac-").toFile();
		d.deleteOnExit();
		export(dn, d);
		log.info("Exported sources to " + d.getAbsolutePath());

		FileTreeModel roots = new FileTreeModel();
		for (FileTreeNode root : sourceRoots) {
			roots.addSource(root);
		}
		filteredTree = (FileTreeNode) roots.getRoot();
	}

	public Element saveToXML() throws IOException {
		Element sourcesElement = new Element("sources");

		// Create and add roots node
		Hasher h = new Hasher();
		Element rootsElement = new Element("roots");
		for (FileTreeNode sourceRoot : sourceRoots) {
			// Create node
			Element rootElement = new Element("root");
			rootElement.setAttribute("path", sourceRoot.toString());
			rootElement.setAttribute("sha1", h.showBytes(h.hash(
					sourceRoot.getFile()).getSha1()));

			// Add node
			rootsElement.addContent(rootElement);
		}
		sourcesElement.addContent(rootsElement);

		Element mainFilterElement;
		// Create and add rootFilter node
		Element rootFilterElement = new Element("rootFilter");
		mainFilterElement = rootFilter.saveToXML();
		mainFilterElement.setName("filter");
		rootFilterElement.addContent(mainFilterElement);
		sourcesElement.addContent(rootFilterElement);

		// Create and add fileFilter node
		Element fileFilterElement = new Element("fileFilter");
		mainFilterElement = sourceFilter.saveToXML();
		mainFilterElement.setName("filter");
		fileFilterElement.addContent(mainFilterElement);
		sourcesElement.addContent(fileFilterElement);

		return sourcesElement;
	}

	private void loadFilter(Element filterElement, CompositeFilter target)
			throws IOException {
		target.clear();
		target.loadFromXML(filterElement);
	}

	public void loadFromXML(Element element) throws IOException {
		// Load sources
		Element rootsElement = element.getChild("roots");
		loadRoots(rootsElement);

		// Load root & source filters
		loadFilter(element.getChild("rootFilter"), rootFilter);
		loadFilter(element.getChild("rootFilter"), sourceFilter);

		filteredTree = null;
	}

	/**
	 * Applies all filters. May be expensive, depending on the filters.
	 */
	public void buildFilteredTree() {

		FileTreeModel roots = new FileTreeModel();
		for (FileTreeNode root : sourceRoots) {
			roots.addSource(root);
		}

		// Filter roots to reach actual inputs
		HashSet<FileTreeNode> valid = new HashSet<>();
		for (TreePath tp : roots.findWithFilter(rootFilter, false, false)) {
			valid.add((FileTreeNode) tp.getLastPathComponent());
		}
		FileTreeModel subTree = new FileTreeModel();
		for (FileTreeNode sn : valid) {
			log.info("Valid root: " + sn.getLabel());
			subTree.addSource(sn);
		}

		// Filter unnecessary files from each submission
		valid.clear();
		for (TreePath tp : subTree.findWithFilter(sourceFilter, true, false)) {
			valid.add((FileTreeNode) tp.getLastPathComponent());
		}
		boolean removedSomething = true;
		while (removedSomething) {
            removedSomething = false;
            ArrayList<FileTreeNode> leaves = subTree.getAllTerminals();
			for (FileTreeNode ln : leaves) {
				if (!valid.contains(ln)) {
					log.info("Removing bad: " + ln.getFile().getName());
					subTree.removeNodeFromParent(ln);
					removedSomething = true;
				}
			}
		}

		filteredTree = (FileTreeNode) subTree.getRoot();
	}

	/**
	 * @return the filtered tree. If you suspect that it may not be up-to-date,
	 * run buildFilteredTree first.
	 */
	public FileTreeNode getFilteredTree() {
		if (filteredTree == null) {
			buildFilteredTree();
		}
		return filteredTree;
	}

	/**
	 * Writes (exports) a FileTreeNode's contents to a directory.
	 * This allows all filters and roots to be discarded on the result.
	 * @param sourceTree to export
	 * @param targetDirectory where it will be saved
	 * @throws IOException 
	 */
	public void export(FileTreeNode sourceTree, File targetDirectory)
			throws IOException {
		for (FileTreeNode dn : sourceTree.getChildren()) {
			// each of these first-level nodes will be a source root
			File dir = new File(targetDirectory, dn.getLabel());
			dir.mkdir();
			FileTreeNode newRoot = new FileTreeNode(dir, null);
			sourceRoots.add(newRoot);
			for (FileTreeNode fn : dn.getLeafChildren()) {
				log.info("child for root " + dn.getLabel() + ": "
						+ fn.getLabel());
				File target = new File(dir, fn.getLabel());
				FileUtils.copy(fn.getFile(), target);
				FileTreeNode newNode = new FileTreeNode(target, newRoot);
				newRoot.insert(newNode, newRoot.getChildCount());
			}
		}
	}

	/**
	 * Load mount points given the roots element in sources node. After this
	 * call, every file under each mount point will be added to the sources field of
	 * this instance.
	 *
	 * @param rootsElement roots element in sources node
	 * @throws java.io.IOException if mount points can't be loaded, for example
	 * when a wrong digest is found.
	 */
	private void loadRoots(Element rootsElement) throws IOException {
		sourceRoots.clear();

		Hasher h = new Hasher();

		List<Element> roots = rootsElement.getChildren();
		for (Element e : roots) {
			try {
				File path = new File(e.getAttributeValue("path"));

				// Check mount point digest
				String sha1 = e.getAttributeValue("sha1");
				String digest = h.showBytes(h.hash(path).getSha1());
				if (!sha1.equals(digest)) {
					System.err.println("sha is: " + digest);
					throw new IOException("Wrong checksum for " + path
							+ ". Found something starting with "
							+ digest.substring(0, 10) + ", but expected "
							+ sha1);

				} else if (!path.exists()) {
					throw new IOException("Mount point '"
							+ path.getAbsolutePath() + "' not found.");
				}

				if (!path.isDirectory()) {
					System.err.println("Found mount point '"
							+ path.getAbsolutePath()
							+ "' but it isn't a  directory.");
				}

				// Add all roots: they'll be filtered afterwards
				sourceRoots.add(new FileTreeNode(path, null));

			} catch (IOException ex) {
				throw new IOException("Error loading roots", ex);
			}
		}
	}

}
