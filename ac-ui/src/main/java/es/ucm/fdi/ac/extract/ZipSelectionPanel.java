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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import org.syntax.jedit.JEditTextArea;
import static es.ucm.fdi.util.I18N.m;
import org.apache.log4j.Logger;

/**
 * A panel that allows selection of what is to be searched for copies. There
 * are two levels of selection: what "assignment files" (either folders or
 * archives) to consider, and what files to select for each. Nested filters can
 * be used to aid in the selection process; although "manual" selection
 * also works fine.
 *
 * @author  mfreire
 */
public class ZipSelectionPanel extends javax.swing.JPanel {

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
					FileTreeNode v = (FileTreeNode) value;
					if (v.getFile() == null) {
						s = "invisible-root";
					} else {
						s = v.getFile().getName();
					}
				}
				return super.getTreeCellRendererComponent(tree, s, sel,
						expanded, leaf, row, hasFocus);
			}
		});
		jtSources.addMouseListener(new TreeDoubleClickListener());

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
						File f = v.getFile();
						if (v.getParent() == selectedFilesModel.getRoot()) {
							s = f.getName();
							if (s.contains(".")) {
								s = s.substring(0, s.lastIndexOf("."));
							}
							if (v.getChildCount() == 0) {
								l.setFont(l.getFont().deriveFont(Font.BOLD));
								l.setForeground(Color.red.brighter());
							}
						} else {
							s = f.getName();
						}
					}
				}
				l.setText(s);
				return l;
			}
		});
		jtSelected.addMouseListener(new TreeDoubleClickListener());
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
					jd.setSize(600, 800);
					jd.setVisible(true);
					// user dimisses dialog (needed to guarantee 'edit' update)
					jd.dispose();

					// detect changes, decide what to do
					String next = textArea.getText();
					if (!next.equals(SourceFileCache.getSource(node.getFile()))) {
						log.info("Prompting for save-it or lose-it");
						int rc = JOptionPane
								.showConfirmDialog(
										null,
										"<html>"
												+ "Has realizado cambios en el fichero; elige<br>"
												+ " - 'si' para usarlos en la comparacion (sin modificar fichero original)<br>"
												+ " - 'no' para no usarlos (pero mantiene otros anteriores)<br>"
												+ " - 'cancelar' para recargar la version del fichero original<br>"
												+ "</html>");
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

			FileFilter zf = ((FilterExpression) e).getFilter();
			TreePath[] selPaths = fileTreeModel
					.findWithFilter(zf, false, false);
			jtSources.setSelectionPaths(selPaths);
		}
	}

	public void reloadFileSelTree(boolean fromScratch) {

		System.err.println("Reloading sel. files");

		if (fromScratch) {
			selectedFilesModel.clear();
			// Initializes with a fresh set of files
			FileFilter zf = filterExpression.getFilter();
			TreePath[] allPaths = fileTreeModel
					.findWithFilter(zf, false, false);
			for (TreePath tp : allPaths) {
				// System.err.println("Adding compressed source... "+tp);
				File f = fileTreeModel.getFileFor(tp);
				addSubmissionFile(f);
			}
		}

		// Purges out those that do not match the filter
		FileFilter ff = fileSelFilterExpression.getFilter();
		TreePath[] selPaths = selectedFilesModel
				.findWithFilter(ff, true, false);
		HashSet<FileTreeNode> valid = new HashSet<FileTreeNode>();
		for (TreePath tp : selPaths) {
			valid.add((FileTreeNode) tp.getLastPathComponent());
		}
		ArrayList<FileTreeNode> allNodes = selectedFilesModel.getAllTerminals();
		for (FileTreeNode n : allNodes) {
			if (!valid.contains(n)) {
				// System.err.println("Removing "+n.getFile());
				try {
					selectedFilesModel.removeNodeFromParent(n);
				} catch (Exception e) {
					System.err.println("could not remove " + n + " ("
							+ n.getFile().getAbsolutePath() + ") from parent");
				}
			}
		}
	}

	public class FileSelListener implements ExpressionListener {

		public void expressionChanged(Expression e, boolean test) {
			if (!test) {
				reloadFileSelTree(false);
			} else {
				FileFilter ff = ((FilterExpression) e).getFilter();
				TreePath[] selPaths = selectedFilesModel.findWithFilter(ff,
						true, false);
				jtSelected.setSelectionPaths(selPaths);
			}
		}
	}

	public void addFile(File f) {
		if (f == null) {
			return;
		}

		TreePath path = fileTreeModel.addSource(f);

		if (path != null) {
			jtSources.expandPath(path);
		}
	}

	public void addSubmissionFile(File f) {
		if (f == null) {
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

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		jSplitPane1 = new javax.swing.JSplitPane();
		jpLeftTree = new javax.swing.JPanel();
		jpLeftTreeButtons = new javax.swing.JPanel();
		jbAddTree = new javax.swing.JButton();
		jbRemoveTree = new javax.swing.JButton();
		jbDirect = new javax.swing.JButton();
		jspLeftTree = new javax.swing.JScrollPane();
		jSplitPane2 = new javax.swing.JSplitPane();
		jpCenterPanel = new javax.swing.JPanel();
		jpMainButtons = new javax.swing.JPanel();
		jbFindGuilty = new javax.swing.JButton();
		jbAccept = new javax.swing.JButton();
		jbCancel = new javax.swing.JButton();
		jSplitPane3 = new javax.swing.JSplitPane();
		jPanel5 = new javax.swing.JPanel();
		jspConditions = new javax.swing.JScrollPane();
		jPanel6 = new javax.swing.JPanel();
		jspFileConditions = new javax.swing.JScrollPane();
		jPanel1 = new javax.swing.JPanel();
		jspRightTree = new javax.swing.JScrollPane();
		jpRightTreeButtons = new javax.swing.JPanel();
		jbAddSubmissionTree = new javax.swing.JButton();
		jbRemoveSubmissionTree = new javax.swing.JButton();
		jbRemoveAllSubmissions = new javax.swing.JButton();

		setLayout(new java.awt.BorderLayout());

		jSplitPane1.setOneTouchExpandable(true);

		jpLeftTree.setBorder(javax.swing.BorderFactory
				.createTitledBorder(m("Extract.SourceSelection")));
		jpLeftTree.setLayout(new java.awt.BorderLayout());

		jpLeftTreeButtons.setLayout(new java.awt.GridBagLayout());

		jbAddTree.setText("+ ...");
		jbAddTree
				.setToolTipText("Incorpora los contenidos de un directorio a este panel. Nunca se modifican realmente los originales; todas las operaciones son \"virtuales\".");
		jbAddTree.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbAddTreeActionPerformed(evt);
			}
		});
		jpLeftTreeButtons.add(jbAddTree, new java.awt.GridBagConstraints());

		jbRemoveTree.setText("-");
		jbRemoveTree.setToolTipText(m("Extract.RemoveSelected.Tooltip"));
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
				.setToolTipText("Pasa todos los archivos seleccionados \"al otro lado\". Ojo: si se \"confirma\" el filtro de seleccion de entregas, se perderan los cambios.");
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

		jSplitPane1.setLeftComponent(jpLeftTree);

		jSplitPane2.setOneTouchExpandable(true);

		jpCenterPanel.setLayout(new java.awt.BorderLayout());

		jbFindGuilty.setText(m("Extract.AnalyzeDistances"));
		jbFindGuilty.setToolTipText(m("Extract.AnalyzeDistances.Tooltip"));
		jbFindGuilty.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbFindGuiltyActionPerformed(evt);
			}
		});
		jpMainButtons.add(jbFindGuilty);

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
		jSplitPane3.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
		jSplitPane3.setResizeWeight(0.5);

		jPanel5.setBorder(javax.swing.BorderFactory
				.createTitledBorder(m("Extract.SelectionFilter.Title")));
		jPanel5.setLayout(new java.awt.BorderLayout());
		jPanel5.add(jspConditions, java.awt.BorderLayout.CENTER);

		jSplitPane3.setTopComponent(jPanel5);

		jPanel6.setBorder(javax.swing.BorderFactory
				.createTitledBorder(m("Extract.AssignmentFilesFilter.Title")));
		jPanel6.setLayout(new java.awt.BorderLayout());
		jPanel6.add(jspFileConditions, java.awt.BorderLayout.CENTER);

		jSplitPane3.setBottomComponent(jPanel6);

		jpCenterPanel.add(jSplitPane3, java.awt.BorderLayout.CENTER);

		jSplitPane2.setLeftComponent(jpCenterPanel);

		jPanel1.setBorder(javax.swing.BorderFactory
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

		jSplitPane1.setRightComponent(jSplitPane2);

		add(jSplitPane1, java.awt.BorderLayout.CENTER);
	}// </editor-fold>//GEN-END:initComponents

	private void jbDirectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbDirectActionPerformed
		// TODO add your handling code here:
		// Don't do anything if nothing was selected
		if (jtSources.getSelectionPaths() != null) {
			for (TreePath tp : jtSources.getSelectionPaths()) {
				File f = ((FileTreeNode) tp.getLastPathComponent()).getFile();
				addSubmissionFile(f);
			}
		}
	}//GEN-LAST:event_jbDirectActionPerformed

	private void jbFindGuiltyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbFindGuiltyActionPerformed
		// TODO add your handling code here:
		MainGui main = new MainGui();
		try {
			SourceSet ss = new SourceSet((FileTreeNode) selectedFilesModel
					.getRoot());
			main.loadSources(ss);
			main.setVisible(true);
		} catch (IOException ioe) {
			log.error("Error exporting sources ", ioe);
		}
	}//GEN-LAST:event_jbFindGuiltyActionPerformed

	private void jbRemoveSubmissionTreeActionPerformed(
			java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbRemoveSubmissionTreeActionPerformed
		// TODO add your handling code here:                        
		for (TreePath tp : jtSelected.getSelectionPaths()) {
			selectedFilesModel.removeNodeFromParent((MutableTreeNode) tp
					.getLastPathComponent());
		}
		// FIXME: should insure that it is not added back again when the filter is applied

	}//GEN-LAST:event_jbRemoveSubmissionTreeActionPerformed

	private void jbAddSubmissionTreeActionPerformed(
			java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbAddSubmissionTreeActionPerformed
		// TODO add your handling code here:
		File f = FileUtils.chooseFile(this, m("Extract.SelectSubmissionDirs"),
				true, JFileChooser.FILES_AND_DIRECTORIES);
		addSubmissionFile(f);
		// FIXME: should insure that it not removed back again when the filter is applied
	}//GEN-LAST:event_jbAddSubmissionTreeActionPerformed

	private void jbCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbCancelActionPerformed
		// TODO add your handling code here:
		System.err.println("Cancelled!");
	}//GEN-LAST:event_jbCancelActionPerformed

	private void jbAcceptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbAcceptActionPerformed
		// TODO add your handling code here:
		System.err.println("Accepted!" + filterExpression.getFilter());
		File d = FileUtils.chooseFile(this, m("Extract.DestinationDirectory"),
				false, JFileChooser.DIRECTORIES_ONLY);
		uncompress(d);
	}//GEN-LAST:event_jbAcceptActionPerformed

	private void jbAddTreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbAddTreeActionPerformed
		// TODO add your handling code here:
		File f = FileUtils.chooseFile(this, m("Extract.SubmissionDirectory"),
				true, JFileChooser.FILES_AND_DIRECTORIES);
		addFile(f);
	}//GEN-LAST:event_jbAddTreeActionPerformed

	private void jbRemoveTreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbRemoveTreeActionPerformed
		// TODO add your handling code here:
		for (TreePath tp : jtSources.getSelectionPaths()) {
			fileTreeModel.removeNodeFromParent((MutableTreeNode) tp
					.getLastPathComponent());
		}
	}//GEN-LAST:event_jbRemoveTreeActionPerformed

	private void jbRemoveAllSubmissionsActionPerformed(
			java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbRemoveAllSubmissionsActionPerformed
		// TODO add your handling code here:
		((FileTreeModel) jtSelected.getModel()).clear();
	}//GEN-LAST:event_jbRemoveAllSubmissionsActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel5;
	private javax.swing.JPanel jPanel6;
	private javax.swing.JSplitPane jSplitPane1;
	private javax.swing.JSplitPane jSplitPane2;
	private javax.swing.JSplitPane jSplitPane3;
	private javax.swing.JButton jbAccept;
	private javax.swing.JButton jbAddSubmissionTree;
	private javax.swing.JButton jbAddTree;
	private javax.swing.JButton jbCancel;
	private javax.swing.JButton jbDirect;
	private javax.swing.JButton jbFindGuilty;
	private javax.swing.JButton jbRemoveAllSubmissions;
	private javax.swing.JButton jbRemoveSubmissionTree;
	private javax.swing.JButton jbRemoveTree;
	private javax.swing.JPanel jpCenterPanel;
	private javax.swing.JPanel jpLeftTree;
	private javax.swing.JPanel jpLeftTreeButtons;
	private javax.swing.JPanel jpMainButtons;
	private javax.swing.JPanel jpRightTreeButtons;
	private javax.swing.JScrollPane jspConditions;
	private javax.swing.JScrollPane jspFileConditions;
	private javax.swing.JScrollPane jspLeftTree;
	private javax.swing.JScrollPane jspRightTree;
	// End of variables declaration//GEN-END:variables
}
