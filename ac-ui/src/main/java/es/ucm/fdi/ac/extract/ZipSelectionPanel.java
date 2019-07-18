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
 * ZipSelectionPanel.java
 * Created on September 13, 2006, 7:58 PM
 *
 * Changelog:
 * 27-02-2009: Check selection when [>>] button is pushed
 */
package es.ucm.fdi.ac.extract;

import es.ucm.fdi.ac.SourceSet;
import es.ucm.fdi.ac.expression.CompositeBooleanExp;
import es.ucm.fdi.ac.expression.FilterExpression;
import es.ucm.fdi.ac.expression.ExpressionListener;
import es.ucm.fdi.ac.expression.Expression;
import es.ucm.fdi.ac.expression.CompositeExpressionPanel;
import es.ucm.fdi.ac.gui.CompareDialog;
import es.ucm.fdi.ac.gui.MainGui;
import es.ucm.fdi.util.FileUtils;
import es.ucm.fdi.util.SourceFileCache;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.syntax.jedit.JEditTextArea;
import static es.ucm.fdi.util.I18N.m;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * A panel that allows selection of what is to be searched for copies. There
 * are two levels of selection: what "assignment files" (either folders or
 * archives) to consider, and what files to select for each. Nested filters can
 * be used to aid in the selection process; although "manual" selection
 * also works fine.
 *
 * @author  mfreire
 */
public class ZipSelectionPanel extends JPanel {

	private static final Logger log = Logger.getLogger(ZipSelectionPanel.class);

	/**
	 * The left-hand-side, a view of the filesystem where submission roots
	 * can be selected.
	 */
	private FileTreeModel fileTreeModel;
	private CompositeExpressionPanel filterPanel;
	private CompositeBooleanExp filterExpression;
	private DTree jtSources;

	/**
	 * The right-hand side, for each selected submission roots shows its
	 * contents, and allows some of them to be filtered out.
	 */
	private FileTreeModel selectedFilesModel;
	private CompositeExpressionPanel fileSelFilterPanel;
	private CompositeBooleanExp fileSelFilterExpression;
	private DTree jtSelected;

	/** Creates new form ZipSelectionPanel */
	public ZipSelectionPanel() {
		initComponents();

		fileTreeModel = new FileTreeModel();
		jtSources = new DTree();
		jspLeftTree.setViewportView(jtSources);
		jtSources.setModel(fileTreeModel);
		jtSources.setRootVisible(false);
		jtSources.setLargeModel(true);
		jtSources.setCellRenderer(new DefaultTreeCellRenderer() {

			public Component getTreeCellRendererComponent(JTree tree,
					Object value, boolean sel, boolean expanded, boolean leaf,
					int row, boolean hasFocus) {

				String s = null;
				if (!(value instanceof FileTreeNode)) {
					s = value.toString();
				} else {
					s = ((FileTreeNode) value).getLabel();
				}
				return super.getTreeCellRendererComponent(tree, s, sel,
						expanded, leaf, row, hasFocus);
			}
		});
		jtSources.addMouseListener(new TreeDoubleClickListener());

		jtSources.setTransferHandler(new FileDropHandler(
				new FileDropHandler.FileDropListener() {
					@Override
					public void fileDropped(File file) {
						addSourceFile(file);
					}
				}));

		filterPanel = new CompositeExpressionPanel(null);
		filterExpression = new CompositeBooleanExp(new CompositeFilter());
		filterPanel.setExpression(filterExpression);
		filterPanel.addExpressionListener(new ZipSelListener());
		jspConditions.setViewportView(filterPanel);

		fileSelFilterPanel = new CompositeExpressionPanel(null);
		fileSelFilterExpression = new CompositeBooleanExp(new CompositeFilter());
		fileSelFilterPanel.setExpression(fileSelFilterExpression);
		fileSelFilterPanel.addExpressionListener(new FileSelListener());
		jspFileConditions.setViewportView(fileSelFilterPanel);

		selectedFilesModel = new FileTreeModel();
		jtSelected = new DTree();
		jspRightTree.setViewportView(jtSelected);
		jtSelected.setModel(selectedFilesModel);
		jtSelected.setRootVisible(false);
		jtSelected.setLargeModel(true);
		jtSelected.setCellRenderer(new DefaultTreeCellRenderer() {

			public Component getTreeCellRendererComponent(JTree tree,
					Object value, boolean sel, boolean expanded, boolean leaf,
					int row, boolean hasFocus) {

				JLabel l = (JLabel) super.getTreeCellRendererComponent(tree,
						"", sel, expanded, leaf, row, hasFocus);
				l.setFont(l.getFont().deriveFont(Font.PLAIN));

				String s = null;
				if (!(value instanceof FileTreeNode)) {
					s = value.toString();
				} else {
					FileTreeNode v = (FileTreeNode) value;
					if (v.getFile() == null) {
						s = "invisible-root";
					} else {
						s = v.getLabel();
						if (v.getParent() == selectedFilesModel.getRoot()) {
							if (s.contains(".")) {
								s = s.substring(0, s.lastIndexOf("."));
							}
							if (v.getChildCount() == 0) {
								l.setFont(l.getFont().deriveFont(Font.BOLD));
								l.setForeground(Color.red.brighter());
							}
						} else {
							s = v.getLabel();
						}
					}
				}
				l.setText(s);
				return l;
			}
		});
		jtSelected.addMouseListener(new TreeDoubleClickListener());
	}

	/**
	 * To enable source-file drag&drop from outside. See
	 * https://stackoverflow.com/a/39415436/15472
	 */
	final static class FileDropHandler extends TransferHandler {

		interface FileDropListener {
			void fileDropped(File file);
		}

		private FileDropListener listener;

		public FileDropHandler(FileDropListener listener) {
			this.listener = listener;
		}

		@Override
		public boolean canImport(TransferHandler.TransferSupport support) {
			for (DataFlavor flavor : support.getDataFlavors()) {
				if (flavor.isFlavorJavaFileListType()) {
					return true;
				}
			}
			return false;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean importData(TransferHandler.TransferSupport support) {
			if (!this.canImport(support))
				return false;

			List<File> files;
			try {
				files = (List<File>) support.getTransferable()
						.getTransferData(DataFlavor.javaFileListFlavor);
			} catch (UnsupportedFlavorException | IOException ex) {
				// should never happen (or JDK is buggy)
				return false;
			}

			for (File file: files) {
				if (listener != null) {
					listener.fileDropped(file);
				}
			}
			return true;
		}
	}

	public static class TreeDoubleClickListener extends MouseAdapter {

		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				DTree tree = (DTree) e.getComponent();
				TreePath path = tree.getPathForLocation(e.getX(), e.getY());
				if (path == null) {
					return;
				}
				FileTreeNode node = (FileTreeNode) path.getLastPathComponent();
				if (node.getAllowsChildren() == false) {
					String name = node.getFile().getName();
					String title = name;
					if (node.getParent() instanceof FileTreeNode) {
						FileTreeNode parent = ((FileTreeNode) node.getParent());
						if (parent.getFile() != null) {
							title = parent.getFile().getName() + "::" + name;
						}
					}

					JDialog jd = new JDialog((JFrame) null, name, true);
					JEditTextArea textArea = CompareDialog.getSourcePanel(
							SourceFileCache.getSource(node.getFile()), name
									.replaceAll("[^.]*\\.", ""));
					textArea.setEditable(true);
					jd.getContentPane().add(textArea);
					jd.setSize(600, 1000);
					jd.setVisible(true);
					// user dimisses dialog (needed to guarantee 'edit' update)
					jd.dispose();

					// detect changes, decide what to do
					String next = textArea.getText();
					if (!next.equals(SourceFileCache.getSource(node.getFile()))) {
						log.info("Prompting for save-it or lose-it");
						int rc = JOptionPane.showConfirmDialog(null,
								m("Extract.ChangedFile"));
						if (rc == JOptionPane.YES_OPTION) {
							log.info("User chose to save");
							SourceFileCache.setSource(node.getFile(), next);
						}
						if (rc == JOptionPane.CANCEL_OPTION) {
							log.info("User chose to discard");
							SourceFileCache.getSource(node.getFile(), true);
						}
					}
				}
			}
		}
	}

	public class ZipSelListener implements ExpressionListener {

		public void expressionChanged(Expression e, boolean test) {

			if (!test) {
				reloadFileSelTree(true);
			}

			FileTreeFilter zf = ((FilterExpression) e).getFilter();
			TreePath[] selPaths = fileTreeModel
					.findWithFilter(zf, false, false);
			jtSources.setSelectionPaths(selPaths);
		}
	}

	public void reloadFileSelTree(boolean fromScratch) {

		log.info("Reloading sel. files");

		if (fromScratch) {
			selectedFilesModel.clear();
			// Initializes with a fresh set of files
			FileTreeFilter zf = filterExpression.getFilter();
			TreePath[] allPaths = fileTreeModel
					.findWithFilter(zf, false, false);
			for (TreePath tp : allPaths) {
				addSubmissionNode(fileTreeModel.getNodeFor(tp));
			}
		}

		// Purges out those that do not match the filter
		FileTreeFilter ff = fileSelFilterExpression.getFilter();
		TreePath[] selPaths = selectedFilesModel
				.findWithFilter(ff, true, false);
		HashSet<FileTreeNode> valid = new HashSet<FileTreeNode>();
		for (TreePath tp : selPaths) {
			valid.add((FileTreeNode) tp.getLastPathComponent());
		}

		boolean removedSomething = true;
		while (removedSomething) {
			removedSomething = false;
			ArrayList<FileTreeNode> allNodes = selectedFilesModel
					.getAllTerminals();
			for (FileTreeNode n : allNodes) {
				if (!valid.contains(n)) {
					try {
						selectedFilesModel.removeNodeFromParent(n);
						removedSomething = true;
						log.info("removed " + n + " (" + n.getPath()
								+ ") from parent");
					} catch (Exception e) {
						log.warn("could not remove " + n + " (" + n.getPath()
								+ ") from parent", e);
					}
				}
			}
		}
	}

	public class FileSelListener implements ExpressionListener {

		public void expressionChanged(Expression e, boolean test) {
			if (!test) {
				reloadFileSelTree(false);
			} else {
				FileTreeFilter ff = ((FilterExpression) e).getFilter();
				TreePath[] selPaths = selectedFilesModel.findWithFilter(ff,
						true, false);
				jtSelected.setSelectionPaths(selPaths);
			}
		}
	}

	public void addSourceFile(File f) {
		log.info("Adding source: " + f);

		if (f == null || !f.exists()) {
			log.warn("Ignored: null or no longer there");
			return;
		}

		TreePath path = fileTreeModel.addSource(f);

		if (path != null) {
			jtSources.expandPath(path);
		}
	}

	/**
	 * Adds a single fileTreeNode from the sources tree.
	 * @param fn
	 */
	public void addSubmissionNode(FileTreeNode fn) {
		TreePath path = selectedFilesModel.addSource(fn);
		if (path != null) {
			jtSelected.expandPath(path);
		}
	}

	/**
	 * Adds a single file. If it happens to be a folder with folders, then each 1st-level subfolder will be
	 * considered a submission.
	 * @param f
	 */
	public void addSubmissionFile(File f) {
		log.info("Adding submission file: " + f);

		if (f == null || !f.exists()) {
			log.warn("Ignored: null or no longer there");
			return;
		}

		if (f.isDirectory()) {
			boolean isHierarchy = false;
			File[] listing = f.listFiles();
			Arrays.sort(listing, new FileTreeNode.FileSorter());
			for (File s : listing) {
				if (s.isDirectory()) {
					isHierarchy = true;
					break;
				}
			}
			if (isHierarchy) {
				for (File s : listing) {
					if (s.isDirectory()) {
						TreePath path = selectedFilesModel.addSource(s);
						if (path != null) {
							jtSelected.expandPath(path);
						}
					}
				}
				return;
			}
		}

		TreePath path = selectedFilesModel.addSource(f);
		if (path != null) {
			jtSelected.expandPath(path);
			jtSelected.scrollPathToVisible(path);
		}
	}

	public boolean uncompress(File dest) {

		try {
			if (!dest.exists()) {
				dest.mkdirs();
			}

			for (FileTreeNode n : selectedFilesModel.getAllTerminals()) {
				File p = ((FileTreeNode) n.getParent()).getFile();
				File f = n.getFile();

				String pname = p.getName();
				String id = (pname.contains(".")) ? pname.substring(0, pname
						.lastIndexOf(".")) : pname;

				File destDir = new File(dest, id);

				if (!destDir.exists()) {
					destDir.mkdir();
				}

				String name = f.getName();
				String contents = SourceFileCache.getSource(f);

				FileUtils.writeStringToFile(new File(destDir, name), contents);
			}
		} catch (IOException ioe) {
			log.error("error uncompressing files", ioe);
			JOptionPane.showMessageDialog(this, m("Extract.UncompressError"),
					m("Extract.ErrorTitle"), JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	private JScrollPane jspLeftTree;
	private JScrollPane jspRightTree;
	private JScrollPane jspConditions;
	private JScrollPane jspFileConditions;

	/**
	 * This method is called from within the constructor to
	 * initialize the form.
	 */
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		JSplitPane mainSplitPanel = new JSplitPane();
		JPanel jpLeftTree = new JPanel();
		JPanel jpLeftTreeButtons = new JPanel();
		JButton jbAddTree = new JButton();
		JButton jbRemoveTree = new JButton();
		JButton jbDirect = new JButton();
		jspLeftTree = new JScrollPane();
		JSplitPane jSplitPane2 = new JSplitPane();
		JPanel jpCenterPanel = new JPanel();
		JPanel jpMainButtons = new JPanel();
		JButton jbFindSuspects = new JButton();
		JButton jbAccept = new JButton();
		JButton jbCancel = new JButton();
		JSplitPane jSplitPane3 = new JSplitPane();
		JPanel centerPanel = new JPanel();
		jspConditions = new JScrollPane();
		JPanel jPanel6 = new JPanel();
		jspFileConditions = new JScrollPane();
		JPanel jPanel1 = new JPanel();
		jspRightTree = new JScrollPane();
		JPanel jpRightTreeButtons = new JPanel();
		JButton jbAddSubmissionTree = new JButton();
		JButton jbRemoveSubmissionTree = new JButton();
		JButton jbRemoveAllSubmissions = new JButton();

		setLayout(new java.awt.BorderLayout());

		mainSplitPanel.setOneTouchExpandable(true);

		jpLeftTree.setBorder(BorderFactory
				.createTitledBorder(m("Extract.SourceSelection")));
		jpLeftTree.setLayout(new java.awt.BorderLayout());

		jpLeftTreeButtons.setLayout(new java.awt.GridBagLayout());

		jbAddTree.setText("+ ...");
		jbAddTree.setToolTipText(m("Extract.AssignmentSelection.AddTooltip"));
		jbAddTree.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbAddTreeActionPerformed(evt);
			}
		});
		jpLeftTreeButtons.add(jbAddTree, new java.awt.GridBagConstraints());

		jbRemoveTree.setText("-");
		jbRemoveTree
				.setToolTipText(m("Extract.AssignmentSelection.RemoveTooltip"));
		jbRemoveTree.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbRemoveTreeActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
		jpLeftTreeButtons.add(jbRemoveTree, gridBagConstraints);

		jbDirect.setText(">>");
		jbDirect
				.setToolTipText(m("Extract.AssignmentSelection.MoveToOtherPaneTooltip"));
		jbDirect.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbDirectActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
		jpLeftTreeButtons.add(jbDirect, gridBagConstraints);

		jpLeftTree.add(jpLeftTreeButtons, java.awt.BorderLayout.SOUTH);
		jpLeftTree.add(jspLeftTree, java.awt.BorderLayout.CENTER);

		mainSplitPanel.setLeftComponent(jpLeftTree);

		jSplitPane2.setOneTouchExpandable(true);

		jpCenterPanel.setLayout(new java.awt.BorderLayout());

		jbFindSuspects.setText(m("Extract.AnalyzeDistances"));
		jbFindSuspects.setToolTipText(m("Extract.AnalyzeDistances.Tooltip"));
		jbFindSuspects.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbFindGuiltyActionPerformed(evt);
			}
		});
		jpMainButtons.add(jbFindSuspects);

		jbAccept.setText(m("Extract.ExtractToDirectories"));
		jbAccept.setToolTipText(m("Extract.ExtractToDirectoriesTooltip"));
		jbAccept.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbAcceptActionPerformed(evt);
			}
		});
		jpMainButtons.add(jbAccept);

		jbCancel.setText(m("Cancel"));
		jbCancel.setToolTipText(m("Extract.Exit"));
		jbCancel.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbCancelActionPerformed(evt);
			}
		});
		jpMainButtons.add(jbCancel);

		jpCenterPanel.add(jpMainButtons, java.awt.BorderLayout.SOUTH);

		jSplitPane3.setDividerLocation(200);
		jSplitPane3.setOrientation(JSplitPane.VERTICAL_SPLIT);
		jSplitPane3.setResizeWeight(0.5);

		centerPanel.setBorder(BorderFactory
				.createTitledBorder(m("Extract.SelectionFilter.Title")));
		centerPanel.setLayout(new java.awt.BorderLayout());
		centerPanel.add(jspConditions, java.awt.BorderLayout.CENTER);

		jSplitPane3.setTopComponent(centerPanel);

		jPanel6.setBorder(BorderFactory
				.createTitledBorder(m("Extract.AssignmentFilesFilter.Title")));
		jPanel6.setLayout(new java.awt.BorderLayout());
		jPanel6.add(jspFileConditions, java.awt.BorderLayout.CENTER);

		jSplitPane3.setBottomComponent(jPanel6);

		jpCenterPanel.add(jSplitPane3, java.awt.BorderLayout.CENTER);

		jSplitPane2.setLeftComponent(jpCenterPanel);

		jPanel1.setBorder(BorderFactory
				.createTitledBorder(m("Extract.AssignmentSelection.Title")));
		jPanel1.setLayout(new java.awt.BorderLayout());
		jPanel1.add(jspRightTree, java.awt.BorderLayout.CENTER);

		jpRightTreeButtons.setLayout(new java.awt.GridBagLayout());

		jbAddSubmissionTree.setText("+ ...");
		jbAddSubmissionTree.setToolTipText(m("Extract.AddSelected"));
		jbAddSubmissionTree
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jbAddSubmissionTreeActionPerformed(evt);
					}
				});
		jpRightTreeButtons.add(jbAddSubmissionTree,
				new java.awt.GridBagConstraints());

		jbRemoveSubmissionTree.setText("-");
		jbRemoveSubmissionTree.setToolTipText(m("Extract.RemoveSelected"));
		jbRemoveSubmissionTree
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jbRemoveSubmissionTreeActionPerformed(evt);
					}
				});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
		jpRightTreeButtons.add(jbRemoveSubmissionTree, gridBagConstraints);

		jbRemoveAllSubmissions.setText(m("Extract.Clean"));
		jbRemoveAllSubmissions
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jbRemoveAllSubmissionsActionPerformed(evt);
					}
				});
		jpRightTreeButtons.add(jbRemoveAllSubmissions,
				new java.awt.GridBagConstraints());

		jPanel1.add(jpRightTreeButtons, java.awt.BorderLayout.SOUTH);

		jSplitPane2.setRightComponent(jPanel1);

		mainSplitPanel.setRightComponent(jSplitPane2);

		add(mainSplitPanel, java.awt.BorderLayout.CENTER);
	}

	private void jbDirectActionPerformed(java.awt.event.ActionEvent evt) {
		// Don't do anything if nothing was selected
		if (jtSources.getSelectionPaths() != null) {
			for (TreePath tp : jtSources.getSelectionPaths()) {
				File f = ((FileTreeNode) tp.getLastPathComponent()).getFile();
				addSubmissionFile(f);
			}
		}
	}

	private void jbFindGuiltyActionPerformed(java.awt.event.ActionEvent evt) {
		MainGui main = new MainGui();
		try {
			SourceSet ss = new SourceSet((FileTreeNode) selectedFilesModel
					.getRoot());
			main.loadSources(ss);
			main.setVisible(true);
		} catch (IOException ioe) {
			log.error("Error exporting sources ", ioe);
		}
	}

	private void jbRemoveSubmissionTreeActionPerformed(
			java.awt.event.ActionEvent evt) {
		for (TreePath tp : jtSelected.getSelectionPaths()) {
			selectedFilesModel.removeNodeFromParent((MutableTreeNode) tp
					.getLastPathComponent());
		}
		// FIXME: should insure that it is not added back again when the filter is applied
	}

	private void jbAddSubmissionTreeActionPerformed(
			java.awt.event.ActionEvent evt) {

		File f = FileUtils.chooseFile(this, m("Extract.SelectSubmissionDirs"),
				true, JFileChooser.FILES_AND_DIRECTORIES);
		addSubmissionFile(f);
		// FIXME: should insure that it not removed back again when the filter is applied
	}

	private void jbCancelActionPerformed(java.awt.event.ActionEvent evt) {
		log.info("Cancelled!");
	}

	private void jbAcceptActionPerformed(java.awt.event.ActionEvent evt) {
		log.info("Accepted!" + filterExpression.getFilter());
		File d = FileUtils.chooseFile(this, m("Extract.DestinationDirectory"),
				false, JFileChooser.DIRECTORIES_ONLY);
		uncompress(d);
	}

	private void jbAddTreeActionPerformed(java.awt.event.ActionEvent evt) {
		File f = FileUtils.chooseFile(this, m("Extract.SubmissionDirectory"),
				true, JFileChooser.FILES_AND_DIRECTORIES);
		addSourceFile(f);
	}

	private void jbRemoveTreeActionPerformed(java.awt.event.ActionEvent evt) {
		for (TreePath tp : jtSources.getSelectionPaths()) {
			fileTreeModel.removeNodeFromParent((MutableTreeNode) tp
					.getLastPathComponent());
		}
	}

	private void jbRemoveAllSubmissionsActionPerformed(
			java.awt.event.ActionEvent evt) {
		((FileTreeModel) jtSelected.getModel()).clear();
	}
}
