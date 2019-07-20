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
 * ACGraphPanel.java 
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * Contributors: - 
 * Changelog: 
 *     18-Apr-2006: first version (mfreire)
 */

package es.ucm.fdi.ac.gui;

import es.ucm.fdi.ac.Analysis;
import es.ucm.fdi.ac.Submission;
import es.ucm.fdi.ac.dgram.ACDendrogram;
import es.ucm.fdi.ac.graph.ACGraph;

import es.ucm.fdi.ac.dgram.Dendrogram;
import es.ucm.fdi.ac.dgram.DendrogramModel;
import es.ucm.fdi.ac.dgram.DendrogramModel.DNode;
import es.ucm.fdi.ac.dgram.DendrogramModel.LinkageModel;
import es.ucm.fdi.ac.dgram.SimpleRenderer;
import es.ucm.fdi.ac.outlier.Hampel;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

import es.ucm.fdi.util.FileUtils;
import org.apache.log4j.Logger;

import org.jgraph.graph.DefaultGraphCell;

import static es.ucm.fdi.util.I18N.m;

/**
 * Graphically displays the results of a given test. Allows the user
 * to select the cut-point, given a dendrogram that displays the a clustering of
 * all distances.
 *
 * @author  mfreire
 */
public class ACGraphPanelD extends JPanel {

	private static final Logger log = Logger.getLogger(ACGraphPanelD.class);

	private ACGraph acg;
	private Dendrogram dendrogram;

	private String testKey = null;
	private Analysis ac = null;

	/**
	 * Updates distance label
	 */
	public void updateDistanceLabel(boolean tracking) {
		jlMaxDistance.setEnabled(false);

		float v = jsMaxDistance.getValue() * 1.0f / jsMaxDistance.getMaximum();
		dendrogram.setCurrent(v);

		if (!tracking) {
			boolean cancelled = false;
			if (!cancelled)
				startLayout();
		}

		String s = (new java.text.DecimalFormat("0.00")).format(v);
		jlMaxDistance.setText("Max. Distance: " + s);
		jlMaxDistance.repaint();

		dendrogram.repaint();
	}

	/**
	 * Creates new form ACGraphPanel
	 */
	public ACGraphPanelD(Analysis ac, String testKey, boolean suggestThresholds) {
		acg = new ACGraph(ACGraph.createViewGraph(ac, testKey), ac, testKey);
		acg.addPropertyChangeListener(ACGraph.AC_SEL_CHANGE,
				new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						updateSel();
					}
				});

		this.testKey = testKey;
		this.ac = ac;

		dendrogram = ACDendrogram.createDendrogram(ac, testKey);
		initComponents();

		DefaultComboBoxModel cbm = new DefaultComboBoxModel(new Object[] {
				new DendrogramModel.SingleLinkage(),
				new DendrogramModel.AverageLinkage(),
				new DendrogramModel.CompleteLinkage() });
		jcbLinkageType.setModel(cbm);
		jcbLinkageType.setSelectedItem(1);

		jspGraph = new JScrollPane(acg);
		jSplitPane1.setLeftComponent(jspGraph);

		jpHist.add(new JScrollPane(dendrogram), BorderLayout.CENTER);
		revalidate();

		if (suggestThresholds) {
			// create a recomendation for the outliers threshold
			Analysis.Result[] R = ac.sortTestResults(testKey);
			double[] distances = new double[R.length];
			for (int i = 0; i < distances.length; i++) {
				distances[i] = R[i].getDist();
			}
			List<Double> tmpList = Hampel.hampel(distances);
			dendrogram.getModel().setHighlights(tmpList);
		}

		updateDistanceLabel(true);

		this.acg = acg;
		startLayout();
	}

	/**
	 * Updates current selection
	 */
	private void updateSel() {
		DefaultGraphCell c;
		String s1, s2;
		c = acg.getFirst();
		s1 = (c == null || !(c instanceof DefaultGraphCell)) ? "none"
				: ((Submission) c.getUserObject()).getId();
		c = acg.getSecond();
		s2 = (c == null || !(c instanceof DefaultGraphCell)) ? "none"
				: ((Submission) c.getUserObject()).getId();
		jlSub.setText(s1 + " -- " + s2);
		jlSub.setEnabled(acg.getFirst() != null && acg.getSecond() != null);
		jlSub.repaint();
	}

	/**
	 * Starts layout
	 */
	private void startLayout() {
		jlMaxDistance.setEnabled(true);
		float f = jsMaxDistance.getValue() / 1000.0f;
		ArrayList<DNode> al = dendrogram.getModel().getClustersAtLevel(f);
		HashMap<Submission, Integer> clusters = new HashMap<Submission, Integer>();
		int index = 0;
		ArrayList<DNode> subs = new ArrayList<DNode>(100);
		for (DNode d : al) {
			subs.clear();
			d.getLeaves(subs);
			for (DNode leaf : subs) {
				log.info("Added sub: "
						+ ((Submission) leaf.getUserObject()).getId());
				clusters.put((Submission) leaf.getUserObject(), index);
			}
			index++;
		}
		if (clusters.isEmpty()) {
			log.warn("No clusters were created - something failed");
		}
		acg.start(f, clusters.isEmpty() ? null : clusters);
		updateSel();
		// saveSnapshot();
	}

	/**
	 * Changes the center
	 */
	private void linkageTypeChanged(LinkageModel nextLinkage) {
		System.err.println("Next linkage will be " + nextLinkage);
		LinkageModel oldLinkage = dendrogram.getModel().getLinkage();

		if (oldLinkage.toString().equals(nextLinkage)) {
			// same as previous - no sweat
			return;
		} else if (nextLinkage != null) {
			dendrogram.setModel(ACDendrogram.allSubmissionsModel(ac, testKey,
					nextLinkage));
			dendrogram.setRenderer(new SimpleRenderer());
			dendrogram.repaint();
			startLayout();
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		jpProgress = new javax.swing.JPanel();
		jbStop = new javax.swing.JButton();
		jpbProgress = new javax.swing.JProgressBar();
		jlProgress = new javax.swing.JLabel();
		jSplitPane1 = new javax.swing.JSplitPane();
		jpMaxDistance = new javax.swing.JPanel();
		jsMaxDistance = new javax.swing.JSlider();
		jlMaxDistance = new javax.swing.JLabel();
		jpHist = new javax.swing.JPanel();
		jlNumSubjects = new javax.swing.JLabel();
		jlSub = new javax.swing.JLabel();
		jcbLinkageType = new javax.swing.JComboBox();
		jlCenter = new javax.swing.JLabel();
		jbTakeScreenshot = new javax.swing.JButton();
		jspGraph = new javax.swing.JScrollPane();
		jPanel1 = new javax.swing.JPanel();

		jpProgress.setLayout(new java.awt.BorderLayout(8, 0));

		jbStop.setText(m("AC.StopLayout"));
		jbStop.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbStopActionPerformed(evt);
			}
		});
		jpProgress.add(jbStop, java.awt.BorderLayout.EAST);

		jpbProgress.setStringPainted(true);
		jpProgress.add(jpbProgress, java.awt.BorderLayout.CENTER);

		jlProgress.setText(m("AC.LayoutProgress"));
		jpProgress.add(jlProgress, java.awt.BorderLayout.WEST);

		setLayout(new java.awt.BorderLayout());

		jSplitPane1.setResizeWeight(0.5);
		jSplitPane1.setOneTouchExpandable(true);

		jpMaxDistance.setLayout(new java.awt.GridBagLayout());

		jsMaxDistance.setMajorTickSpacing(100);
		jsMaxDistance.setMaximum(1000);
		jsMaxDistance.setMinorTickSpacing(1);
		jsMaxDistance.setValue(40);
		jsMaxDistance.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				jsMaxDistanceStateChanged(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
		jpMaxDistance.add(jsMaxDistance, gridBagConstraints);

		jlMaxDistance.setText(m("AC.MaxDistance"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 3, 2, 3);
		jpMaxDistance.add(jlMaxDistance, gridBagConstraints);

		jpHist.setPreferredSize(new java.awt.Dimension(0, 50));
		jpHist.setLayout(new java.awt.BorderLayout());
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 11, 11, 5);
		jpMaxDistance.add(jpHist, gridBagConstraints);

		jlNumSubjects.setText(m("AC.EdgesShown"));
		jlNumSubjects.setEnabled(false);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 3);
		jpMaxDistance.add(jlNumSubjects, gridBagConstraints);

		jlSub.setText("none -- none");
		jlSub.setEnabled(false);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 3, 2, 3);
		jpMaxDistance.add(jlSub, gridBagConstraints);

		jcbLinkageType.setModel(new javax.swing.DefaultComboBoxModel(
				new String[] { "(none)", "Item 2", "Item 3", "Item 4" }));
		jcbLinkageType.addItemListener(new java.awt.event.ItemListener() {
			public void itemStateChanged(java.awt.event.ItemEvent evt) {
				jcbLinkageTypeItemStateChanged(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 4;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
		jpMaxDistance.add(jcbLinkageType, gridBagConstraints);

		jlCenter.setText("Linkage");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.insets = new java.awt.Insets(2, 3, 2, 3);
		jpMaxDistance.add(jlCenter, gridBagConstraints);

		jbTakeScreenshot.setText(m("AC.saveImageButton"));
		jbTakeScreenshot.setToolTipText(m("AC.saveImageButtonTooltip"));
		jbTakeScreenshot.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbTakeScreenshotActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 5;
		gridBagConstraints.gridy = 2;
		jpMaxDistance.add(jbTakeScreenshot, gridBagConstraints);

		jSplitPane1.setRightComponent(jpMaxDistance);

		jspGraph.setViewportView(jPanel1);

		jSplitPane1.setLeftComponent(jspGraph);

		add(jSplitPane1, java.awt.BorderLayout.CENTER);
	}// </editor-fold>//GEN-END:initComponents

	private void jcbLinkageTypeItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jcbLinkageTypeItemStateChanged
		// TODO add your handling code here:
		LinkageModel selected = null;
		if (!jcbLinkageType.getSelectedItem().equals("(none)")) {
			selected = (LinkageModel) jcbLinkageType.getSelectedItem();
		}
		linkageTypeChanged(selected);
	}//GEN-LAST:event_jcbLinkageTypeItemStateChanged

	private void jbTakeScreenshotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbTakeScreenshotActionPerformed
		// TODO add your handling code here:   
		double w = acg.getSize().getWidth();
		double h = acg.getSize().getHeight();
		BufferedImage bi = new BufferedImage((int) w, (int) h,
				BufferedImage.TYPE_INT_RGB);
		Graphics g = bi.getGraphics();
		acg.paint(g);
		File ssFile = FileUtils.chooseFile(this, m("AC.saveDialog.Title"), false, JFileChooser.FILES_ONLY);
		if (ssFile == null)
			return;
		log.info("Creating screenshot (" + w + " x " + h + ") at "
				+ ssFile.getAbsolutePath());
		try (FileOutputStream fos = new FileOutputStream(ssFile)) {
			ImageIO.write(bi, "png", fos);
			JOptionPane.showMessageDialog(this, m("Test.resultsSavedOk"),
					m("DONE"), JOptionPane.INFORMATION_MESSAGE);
		} catch (IOException ioe) {
			log.warn("Error saving screenshot", ioe);
			JOptionPane.showMessageDialog(null, m("Test.errorSaving"),
					m("ERROR"), JOptionPane.ERROR_MESSAGE);
		}
		try {
			if(!ssFile.getName().contains(".png")){
				Files.move(Paths.get(ssFile.getAbsolutePath()), Paths.get(ssFile.getAbsolutePath() + ".png"));
			}
		} catch (IOException e) {
			log.warn("Channot add a extention to screenshot file");
			e.printStackTrace();
		}
	}//GEN-LAST:event_jbTakeScreenshotActionPerformed

	private void jsMaxDistanceStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jsMaxDistanceStateChanged
		// TODO add your handling code here:
		updateDistanceLabel(jsMaxDistance.getValueIsAdjusting());
	}//GEN-LAST:event_jsMaxDistanceStateChanged

	private void jbStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbStopActionPerformed
		// TODO add your handling code here:        
	}//GEN-LAST:event_jbStopActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JPanel jPanel1;
	private javax.swing.JSplitPane jSplitPane1;
	private javax.swing.JButton jbStop;
	private javax.swing.JButton jbTakeScreenshot;
	private javax.swing.JComboBox jcbLinkageType;
	private javax.swing.JLabel jlCenter;
	private javax.swing.JLabel jlMaxDistance;
	private javax.swing.JLabel jlNumSubjects;
	private javax.swing.JLabel jlProgress;
	private javax.swing.JLabel jlSub;
	private javax.swing.JPanel jpHist;
	private javax.swing.JPanel jpMaxDistance;
	private javax.swing.JPanel jpProgress;
	private javax.swing.JProgressBar jpbProgress;
	private javax.swing.JSlider jsMaxDistance;
	private javax.swing.JScrollPane jspGraph;
	// End of variables declaration//GEN-END:variables

}
