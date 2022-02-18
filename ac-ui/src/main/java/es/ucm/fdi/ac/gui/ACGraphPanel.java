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
package es.ucm.fdi.ac.gui;

import es.ucm.fdi.ac.Analysis;
import es.ucm.fdi.ac.Submission;
import es.ucm.fdi.ac.graph.ACGraph;
import es.ucm.fdi.ac.graph.ACModel;
import es.ucm.fdi.ac.hist.ACHistogram;

import es.ucm.fdi.ac.hist.Histogram;
import es.ucm.fdi.ac.outlier.Hampel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.imageio.ImageIO;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jgraph.graph.DefaultGraphCell;

/**
 * Graphically displays the results of a given test. Allows the user
 * to select the cut-point, given a histogram that displays the frequency
 * of each distance.
 *
 * @author  mfreire
 */
public class ACGraphPanel extends javax.swing.JPanel {

	private static final Logger log = LogManager.getLogger(ACGraphPanel.class);

	private ACGraph acg;
	private Histogram histogram;
	private boolean suggestThresholds;

	private String testKey = null;
	private Analysis ac = null;

	public static void main(String args[]) {

		try {
			if (args.length != 2) {
				JOptionPane.showMessageDialog(null, "Error: missing args");
				return;
			}
			File din = new File(args[0]);
			File fres = new File(args[1]);
			if (!din.exists() || !din.isDirectory() || !fres.exists()) {
				JOptionPane.showMessageDialog(null, "Error: bad files");
				return;
			}

			Analysis ac = new Analysis();
			ACGraphPanel acgp = new ACGraphPanel(ac, null, false);

			JFrame jf = new JFrame();
			jf.getRootPane().setLayout(new BorderLayout());
			jf.getRootPane().add(acgp, BorderLayout.CENTER);
			jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			jf.setSize(600, 400);
			jf.setVisible(true);
		} catch (Exception e) {
			log.warn("UNCAUGHT EXCEPTION:" + e);
			e.printStackTrace();
		}
	}

	/**
	 * Updates distance label
	 */
	public void updateDistanceLabel(boolean tracking) {
		jlMaxDistance.setEnabled(false);
		jlNumSubmissions.setEnabled(false);

		float v = jsMaxDistance.getValue() * 1.0f / jsMaxDistance.getMaximum();
		int numEdges = histogram.getModel().numBelow(v);
		histogram.setCurrent(v);

		if (!tracking) {
			boolean cancelled = false;
			if (!cancelled)
				startLayout();
		}

		String s = (new java.text.DecimalFormat("0.00")).format(v);
		jlMaxDistance.setText("Max. Distance: " + s);
		jlMaxDistance.repaint();
		jlNumSubmissions.setText("Edges Shown: " + numEdges);
		jlNumSubmissions.repaint();

		histogram.repaint();
	}

	/**
	 * Creates new form ACGraphPanel
	 */
	public ACGraphPanel(Analysis ac, String testKey, boolean suggestThresholds) {
		acg = new ACGraph(ACGraph.createViewGraph(ac, testKey), ac, testKey);
		acg.addPropertyChangeListener(ACGraph.AC_SEL_CHANGE,
				new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						updateSel();
					}
				});

		this.suggestThresholds = suggestThresholds;
		this.testKey = testKey;
		this.ac = ac;

		histogram = ACHistogram.createBarHistogram(ac, testKey);
		initComponents();

		Submission[] ss = new Submission[ac.getSubmissions().length];
		System.arraycopy(ac.getSubmissions(), 0, ss, 0, ss.length);
		Arrays.sort(ss, new Comparator<Submission>() {
			public int compare(Submission o1, Submission o2) {
				return o1.getId().compareToIgnoreCase(o2.getId());
			}
		});
		DefaultComboBoxModel<Object> cbm = new DefaultComboBoxModel<>(ss);
		cbm.addElement("(none)");
		jcbCenter.setModel(cbm);
		jcbCenter.setSelectedItem("(none)");

		remove(jspGraph);
		jspGraph = new JScrollPane(acg);
		add(jspGraph, BorderLayout.CENTER);

		jpHist.add(histogram, BorderLayout.CENTER);
		revalidate();

		if (suggestThresholds) {
			// create a recomendation for the outliers threshold
			Analysis.Result[] R = ac.sortTestResults(testKey);
			double[] distances = new double[R.length];
			for (int i = 0; i < distances.length; i++) {
				distances[i] = R[i].getDist();
			}
			List<Double> tmpList = Hampel.hampel(distances);
			histogram.getModel().setHighlights(tmpList);
		}

		updateDistanceLabel(true);

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
		log.info("Selection change: \n\t" + s1 + "\n\t" + s2);
	}

	/**
	 * Starts layout
	 */
	private void startLayout() {
		jlMaxDistance.setEnabled(true);
		jlNumSubmissions.setEnabled(true);
		acg.start(jsMaxDistance.getValue() / 1000.0f, null);
		updateSel();
		// saveSnapshot();
	}

	/**
	 * Changes the center
	 */
	private void centerVertexChanged(Submission nextCenter) {
		log.debug("Next center will be " + nextCenter);
		Submission oldCenter = ((ACModel) acg.getBase()).getCenterSubmission();

		if (oldCenter == nextCenter) {
			// same as previous - no sweat
			return;
		} else if (nextCenter != null) {
			histogram.setLevels(50);
			histogram.setModel(ACHistogram.singleSubmissionModel(ac,
					nextCenter, testKey, suggestThresholds));
		} else if (nextCenter == null) {
			histogram.setLevels(100);
			histogram.setModel(ACHistogram.allSubmissionsModel(ac, testKey));
		}

		((ACModel) acg.getBase()).setCenterSubmission(nextCenter);
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
		jspGraph = new javax.swing.JScrollPane();
		jPanel1 = new javax.swing.JPanel();
		jpMaxDistance = new javax.swing.JPanel();
		jsMaxDistance = new javax.swing.JSlider();
		jlMaxDistance = new javax.swing.JLabel();
		jpHist = new javax.swing.JPanel();
		jlNumSubmissions = new javax.swing.JLabel();
		jlSub = new javax.swing.JLabel();
		jcbCenter = new javax.swing.JComboBox<>();
		jlCenter = new javax.swing.JLabel();
		jButton1 = new javax.swing.JButton();

		jpProgress.setLayout(new java.awt.BorderLayout(8, 0));

		jbStop.setText("stop layout");
		jbStop.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbStopActionPerformed(evt);
			}
		});
		jpProgress.add(jbStop, java.awt.BorderLayout.EAST);

		jpbProgress.setStringPainted(true);
		jpProgress.add(jpbProgress, java.awt.BorderLayout.CENTER);

		jlProgress.setText("Layout Progress");
		jpProgress.add(jlProgress, java.awt.BorderLayout.WEST);

		setLayout(new java.awt.BorderLayout());

		jspGraph.setViewportView(jPanel1);

		add(jspGraph, java.awt.BorderLayout.CENTER);

		jpMaxDistance.setToolTipText("Take Screenshot");
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
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
		jpMaxDistance.add(jsMaxDistance, gridBagConstraints);

		jlMaxDistance.setText("Max. Distance: 0.40");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 3, 2, 3);
		jpMaxDistance.add(jlMaxDistance, gridBagConstraints);

		jpHist.setPreferredSize(new java.awt.Dimension(0, 50));
		jpHist.setLayout(new java.awt.BorderLayout());
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 11, 11, 5);
		jpMaxDistance.add(jpHist, gridBagConstraints);

		jlNumSubmissions.setText("Edges shown: ?");
		jlNumSubmissions.setEnabled(false);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 3, 2, 3);
		jpMaxDistance.add(jlNumSubmissions, gridBagConstraints);

		jlSub.setText("none -- none");
		jlSub.setEnabled(false);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 3, 2, 3);
		jpMaxDistance.add(jlSub, gridBagConstraints);

		jcbCenter.setModel(new javax.swing.DefaultComboBoxModel<Object>(new Object[] {
				"(none)", "Item 2", "Item 3", "Item 4" }));
		jcbCenter.addItemListener(new java.awt.event.ItemListener() {
			public void itemStateChanged(java.awt.event.ItemEvent evt) {
				jcbCenterItemStateChanged(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
		jpMaxDistance.add(jcbCenter, gridBagConstraints);

		jlCenter.setText("Center");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.insets = new java.awt.Insets(2, 3, 2, 3);
		jpMaxDistance.add(jlCenter, gridBagConstraints);

		jButton1.setText("click!");
		jButton1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton1ActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 3;
		jpMaxDistance.add(jButton1, gridBagConstraints);

		add(jpMaxDistance, java.awt.BorderLayout.SOUTH);
	}// </editor-fold>//GEN-END:initComponents

	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
		double w = acg.getSize().getWidth();
		double h = acg.getSize().getHeight();
		BufferedImage bi = new BufferedImage((int) w, (int) h,
				BufferedImage.TYPE_INT_RGB);
		Graphics g = bi.getGraphics();
		acg.paint(g);
		File tmpFile = null;
		try {
			tmpFile = Files.createTempFile("screenshot_ac_", null).toFile();
		} catch (IOException ioe) {
			log.warn("Could not create temp file to hold screenshot", ioe);
		}
		log.info("Creating screenshot (" + w + " x " + h + ") at "
				+ tmpFile.getAbsolutePath());
		try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
			ImageIO.write(bi, "png", fos);
		} catch (IOException ioe) {
			log.warn("Error saving screenshot", ioe);
		}
	}//GEN-LAST:event_jButton1ActionPerformed

	private void jcbCenterItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jcbCenterItemStateChanged
		Submission selected = null;
		if (!jcbCenter.getSelectedItem().equals("(none)")) {
			selected = (Submission) jcbCenter.getSelectedItem();
		}
		centerVertexChanged(selected);
	}//GEN-LAST:event_jcbCenterItemStateChanged

	private void jsMaxDistanceStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jsMaxDistanceStateChanged
		updateDistanceLabel(jsMaxDistance.getValueIsAdjusting());
	}//GEN-LAST:event_jsMaxDistanceStateChanged

	private void jbStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbStopActionPerformed
	}//GEN-LAST:event_jbStopActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton jButton1;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JButton jbStop;
	private javax.swing.JComboBox<Object> jcbCenter;
	private javax.swing.JLabel jlCenter;
	private javax.swing.JLabel jlMaxDistance;
	private javax.swing.JLabel jlNumSubmissions;
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
