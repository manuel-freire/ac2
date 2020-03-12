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
 * CompareDialog.java 
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * Contributors: - 
 * Changelog: 
 *     18-Apr-2006: first version (mfreire)
 */
package es.ucm.fdi.ac.gui;

import es.ucm.fdi.ac.Submission;

import es.ucm.fdi.ac.ptrie.Node;
import es.ucm.fdi.ac.ptrie.PTrie;
import es.ucm.fdi.ac.stringmap.Mapper;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import org.syntax.jedit.JEditTextArea;
import org.syntax.jedit.SyntaxDocument;
import org.syntax.jedit.tokenmarker.*;

import static es.ucm.fdi.util.I18N.m;

/**
 * Compares two sets of files side-by-side. Could be much better, I'm sure there
 * is some kind of 'diff' package for Java with syntax highlight out there.
 * 
 * @author  mfreire
 */
public class CompareDialog extends javax.swing.JDialog {

	private Submission subjectA = null;
	private Submission subjectB = null;

	/** Creates new form CompareDialog */
	public CompareDialog(Frame parent, boolean modal) {
		super(parent, modal);
		initComponents();
	}

	public CompareDialog(Frame parent, Submission a, Submission b) {
		this(parent, false);
		addSubmission(a, jTabbedPaneA);
		subjectA = a;
		addSubmission(b, jTabbedPaneB);
		subjectB = b;
		setSize(1200, 600);
	}

	public static void main(String args[]) {
		final File d = new File(args[0]);
		java.awt.EventQueue.invokeLater(new Runnable() {

			public void run() {
				try {
					File f1 = d.listFiles()[1];
					File f2 = d.listFiles()[2];
					Submission sa = new Submission("a", f1.getPath(), 0);
					sa.addSource(f1);
					Submission sb = new Submission("b", f2.getPath(), 1);
					sb.addSource(f2);
					System.err.println("f1: " + sa + " vs f2: " + sb);

					CompareDialog cd = new CompareDialog(null, sa, sb);
					cd.setBounds(100, 100, 1200, 700);
					cd.setVisible(true);
					cd.addWindowListener(new WindowAdapter() {

						@Override
						public void windowClosed(WindowEvent e) {
							System.exit(0);
						}
					});

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Highlights N most similar segments; currently uses 'exact' similarity
	 */
	public void startHighlight(int numSegs) {

		if (subjectA == null || subjectB == null) {
			return;
		}

		// find index of currently-represented programs
		int selA = jTabbedPaneA.getSelectedIndex();
		int selB = jTabbedPaneB.getSelectedIndex();

		// build PTrie (min run length = 10)
		PTrie pt = new PTrie();
		JEditTextArea aa = getJEditArea(0, selA);
		JEditTextArea ab = getJEditArea(1, selB);
		Mapper ma = new Mapper(aa.getText(), "\\p{javaWhitespace}+", "");
		Mapper mb = new Mapper(ab.getText(), "\\p{javaWhitespace}+", "");
		pt.add(ma.getDest(), subjectA);
		pt.add(mb.getDest(), subjectB);
		ArrayList<Node> l = new ArrayList<Node>();
		for (Node n : pt.findRare(2, 2)) {
			if (n.getData().length() > 10) {
				l.add(n);
			}
		}
		// sort by size (largest first)
		Collections.sort(l, new Comparator<Node>() {
			public int compare(Node a, Node b) {
				return (b.getEnd() - b.getStart())
						- (a.getEnd() - a.getStart());
			}
		});
		// choose first numSeg non-overlapping ones (requires O(N*N) checks...)
		ArrayList<Node> sel = new ArrayList<Node>();
		while (sel.size() < numSegs && !l.isEmpty()) {
			Node c = l.remove(0);
			boolean ok = true;
			for (Node n : sel) {
				if (n.overlaps(c)) {
					ok = false;
					break;
				}
			}
			if (ok) {
				sel.add(c);
				//                System.err.println("Added segment: " + c.getLength());
			}
		}

		// build the highlight
		CommonHighlighter ha = new CommonHighlighter(sel, ma, subjectA);
		CommonHighlighter hb = new CommonHighlighter(sel, mb, subjectB);
		ha.setPeer(hb);
		hb.setPeer(ha);
		aa.getPainter().addCustomHighlight(ha);
		aa.setRightClickPopup(ha);
		aa.setCaretVisible(false);
		ab.getPainter().addCustomHighlight(hb);
		ab.setRightClickPopup(hb);
		ab.setCaretVisible(false);
	}

	public JEditTextArea getJEditArea(int pos, int source) {
		return (JEditTextArea) ((pos == 0) ? jTabbedPaneA
				.getComponentAt(source) : jTabbedPaneB.getComponentAt(source));
	}

	public static JEditTextArea getSourcePanel(String source, String extension) {
		JEditTextArea jeta = new JEditTextArea();
		jeta.setHorizontalOffset(6);
		jeta.setDocument(new SyntaxDocument());
		if (extension.equalsIgnoreCase("java")) {
			jeta.setTokenMarker(new JavaTokenMarker());
		} else if (extension.equalsIgnoreCase("h")
				|| extension.equalsIgnoreCase("c")
				|| extension.equalsIgnoreCase("cpp")
				|| extension.equalsIgnoreCase("cc")
				|| extension.equalsIgnoreCase("c++")) {
			jeta.setTokenMarker(new CCTokenMarker());
		} else if (extension.equalsIgnoreCase("php")) {
			jeta.setTokenMarker(new PHPTokenMarker());
		} else if (extension.equalsIgnoreCase("js")) {
			jeta.setTokenMarker(new JavaScriptTokenMarker());
		} else if (extension.equalsIgnoreCase("xml")
				|| extension.equalsIgnoreCase("html")
				|| extension.equalsIgnoreCase("htm")) {
			jeta.setTokenMarker(new HTMLTokenMarker());
		} else if (extension.equalsIgnoreCase("py")) {
			jeta.setTokenMarker(new PythonTokenMarker());
		}
		jeta.setText(source);
		return jeta;
	}

	public static void addSubmission(Submission s, JTabbedPane jtp) {
		for (int i = 0; i < s.getSources().size(); i++) {
			String source = s.getSourceCode(i);
			String sourceName = s.getSourceName(i);
			String extension = sourceName
					.substring(sourceName.lastIndexOf('.') + 1);
			jtp.add(s.getId() + ":" + sourceName, getSourcePanel(source,
					extension));
			jtp.setToolTipTextAt(i, s.getOriginalPath());
		}
		if (s.getSources().isEmpty()) {
			jtp.add(new JLabel("<html>" + m("Compare.NoSources") + "</html>"));
		}
	}

	private String wrapText(String text, int maxCols) {
		StringBuilder sb = new StringBuilder();
		int n = 0;
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '\n') {
				n = 0;
			}
			sb.append(text.charAt(i));
			n++;
			if (n >= maxCols) {
				sb.append('\n');
				n = 0;
			}
		}
		return sb.toString();
	}

	public void wrapAndHighlight(int maxCols) {
		if (subjectA == null || subjectB == null) {
			return;
		}

		// find index of currently-represented programs
		int selA = jTabbedPaneA.getSelectedIndex();
		int selB = jTabbedPaneB.getSelectedIndex();

		JEditTextArea aa = getJEditArea(0, selA);
		JEditTextArea ab = getJEditArea(1, selB);
		aa.setText(wrapText(subjectA.getSourceCode(selA), maxCols));
		ab.setText(wrapText(subjectB.getSourceCode(selB), maxCols));
		startHighlight(Integer.parseInt(""
				+ jcbNumSimilarities.getSelectedItem()));
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		jSplitPane1 = new javax.swing.JSplitPane();
		jTabbedPaneA = new javax.swing.JTabbedPane();
		jTabbedPaneB = new javax.swing.JTabbedPane();
		jPanel1 = new javax.swing.JPanel();
		jPanel3 = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		jcbNumColumnsWrap = new javax.swing.JComboBox();
		jPanel2 = new javax.swing.JPanel();
		jPanel4 = new javax.swing.JPanel();
		jLabel3 = new javax.swing.JLabel();
		jcbNumSimilarities = new javax.swing.JComboBox();
		jLabel4 = new javax.swing.JLabel();
		jbHighlight = new javax.swing.JButton();
		jButton1 = new javax.swing.JButton();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

		jSplitPane1.setResizeWeight(0.5);
		jSplitPane1.setLeftComponent(jTabbedPaneA);
		jSplitPane1.setRightComponent(jTabbedPaneB);

		getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);

		jPanel3.setBackground(java.awt.Color.lightGray);

		jLabel1.setText("Wrap at");
		jPanel3.add(jLabel1);

		jcbNumColumnsWrap.setModel(new javax.swing.DefaultComboBoxModel(
				new String[] { "--", "60", "80", "100", "120" }));
		jcbNumColumnsWrap
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jcbNumColumnsWrapActionPerformed(evt);
					}
				});
		jPanel3.add(jcbNumColumnsWrap);

		jPanel1.add(jPanel3);
		jPanel1.add(jPanel2);

		jPanel4.setBackground(java.awt.Color.lightGray);

		jLabel3.setText("Highlight");
		jPanel4.add(jLabel3);

		jcbNumSimilarities.setModel(new javax.swing.DefaultComboBoxModel(
				new String[] { "5", "10", "20", "30" }));
		jcbNumSimilarities
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jcbNumSimilaritiesActionPerformed(evt);
					}
				});
		jPanel4.add(jcbNumSimilarities);

		jLabel4.setText("largest similar runs");
		jPanel4.add(jLabel4);

		jbHighlight.setText("highlight");
		jbHighlight.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbHighlightActionPerformed(evt);
			}
		});
		jPanel4.add(jbHighlight);

		jPanel1.add(jPanel4);

		jButton1.setText("close window");
		jButton1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton1ActionPerformed(evt);
			}
		});
		jPanel1.add(jButton1);

		getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
		// TODO add your handling code here:
		this.dispose();
	}//GEN-LAST:event_jButton1ActionPerformed

	private void jbHighlightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbHighlightActionPerformed
		jcbNumColumnsWrapActionPerformed(null);
	}//GEN-LAST:event_jbHighlightActionPerformed

	private void jcbNumColumnsWrapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jcbNumColumnsWrapActionPerformed
		int maxCols = Integer.MAX_VALUE;
		try {
			maxCols = Integer
					.parseInt("" + jcbNumColumnsWrap.getSelectedItem());
		} catch (NumberFormatException nfe) {
			// do nothing - this is expected, just leave max integer value.
		}
		wrapAndHighlight(maxCols);
	}//GEN-LAST:event_jcbNumColumnsWrapActionPerformed

	private void jcbNumSimilaritiesActionPerformed(
			java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jcbNumSimilaritiesActionPerformed
		jcbNumColumnsWrapActionPerformed(null);
	}//GEN-LAST:event_jcbNumSimilaritiesActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton jButton1;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel3;
	private javax.swing.JPanel jPanel4;
	private javax.swing.JSplitPane jSplitPane1;
	private javax.swing.JTabbedPane jTabbedPaneA;
	private javax.swing.JTabbedPane jTabbedPaneB;
	private javax.swing.JButton jbHighlight;
	private javax.swing.JComboBox jcbNumColumnsWrap;
	private javax.swing.JComboBox jcbNumSimilarities;
	// End of variables declaration//GEN-END:variables
}
