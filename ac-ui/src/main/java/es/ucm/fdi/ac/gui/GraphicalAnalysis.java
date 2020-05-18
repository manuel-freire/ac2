/**
 * AC - A source-code copy detector
 *
 *     For more information please visit:  http://github.com/manuel-freire/ac
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
 * GraphicalAnalysis.java
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * Contributors: -
 * Changelog:
 *     18-Apr-2006: first version (mfreire)
 */

package es.ucm.fdi.ac.gui;

import es.ucm.fdi.ac.Analysis;
import es.ucm.fdi.ac.test.Test;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import static es.ucm.fdi.util.I18N.m;
import es.ucm.fdi.util.MemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

/**
 * A small dialog that provides feedback on the progress of a test, and
 * allows the user to abort the test in any moment. Once finished, a
 * dialog offers to save results, and a callback is, uh, called.
 *
 * @author mfreire
 */
public class GraphicalAnalysis extends JDialog implements ActionListener {

	private static final Logger log = LogManager
			.getLogger(GraphicalAnalysis.class);

	private javax.swing.JButton jbCancelProgress;
	private javax.swing.JLabel jlProgress;
	private javax.swing.JProgressBar jpProgress;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JSeparator jSeparator1;

	private javax.swing.Timer t;
	private Test test;
	private String testName;
	private Analysis ac;
	private Runnable callback;
	private long startTime;
	private boolean isTestFinished = false;

	public GraphicalAnalysis(Analysis ac, String testName, Test test,
			Runnable callback) {
		this.ac = ac;
		this.callback = callback;
		this.testName = testName;
		this.test = test;
		initComponents();
		setSize(300, 200);
		setTitle(testName + ": " + m("Analysis.Preprocessing"));
		setLocationByPlatform(true);
		setVisible(true);
	}

	void start() {
		TestRunner runner = new TestRunner();
		Thread testThread = new Thread(runner);
		testThread.start();
		startTime = System.currentTimeMillis();
		t = new javax.swing.Timer(1000, this); // miliseconds
		t.setRepeats(true);
		t.start();
	}

	private class TestRunner implements Runnable {
		private String message;

		public void run() {
			try {
				ThreadContext.push("T-" + test);
				ac.prepareTest(test);
				java.awt.EventQueue.invokeLater(new Runnable() {
					public void run() {
						setTitle(testName + ": " + m("Analysis.Comparing"));
					}
				});

				ac.applyTest(test);
				isTestFinished = true;
				ThreadContext.pop();
			} catch (RuntimeException e) {
				java.io.StringWriter sw = new java.io.StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				message = "<html><b>" + e.toString() + "</b><br>" + "<pre>"
						+ sw.toString() + "</pre></html>";

				test.setCancelled(true);
				java.awt.EventQueue.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(null, message, "Error",
								JOptionPane.ERROR_MESSAGE);
						dispose();
					}
				});
			}
		}
	}

	public void actionPerformed(ActionEvent evt) {
		double p = test.getProgress();
		long elapsed = System.currentTimeMillis() - startTime;
		jpProgress.setValue((int) (p * 100));
		double v = (elapsed / p - elapsed) / 1000;
		int s = (int) v;
		int minutes = s / 60;
		int seconds = s % 60;
		int es = ((int) elapsed / 1000) % 60;
		int em = ((int) elapsed / 1000) / 60;
		jlProgress.setText("<html>" + m("Analysis.TimeSoFar") + " " + em
				+ " min, " + es + " s<br>" + m("Analysis.TimeRemaining") + " "
				+ minutes + " min, " + seconds + " s<br>"
				+ m("Analysis.Memory") + " " + MemUtils.getMemUsage()
				+ "</html>");
		if (isTestFinished) {
			t.stop();
			dispose();
			log.info("Total time elapsed: " + elapsed + " ms");
			callback.run();
		}
	}

	public void cancel() {
		test.setCancelled(true);
		t.stop();
		dispose();
		return;
	}

	public void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;
		jpProgress = new javax.swing.JProgressBar();
		jPanel2 = new javax.swing.JPanel();
		jbCancelProgress = new javax.swing.JButton();
		jlProgress = new javax.swing.JLabel();
		jSeparator1 = new javax.swing.JSeparator();

		getContentPane().setLayout(new java.awt.GridBagLayout());

		setTitle("...");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(26, 7, 7, 7);
		getContentPane().add(jpProgress, gridBagConstraints);

		jbCancelProgress.setText(m("Cancel"));
		jbCancelProgress.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cancel();
			}
		});

		jPanel2.add(jbCancelProgress);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		getContentPane().add(jPanel2, gridBagConstraints);

		jlProgress.setText(m("Analysis.TimeRemaining") + " ??? min");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.insets = new java.awt.Insets(7, 7, 7, 7);
		getContentPane().add(jlProgress, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
		gridBagConstraints.weighty = 1.0;
		getContentPane().add(jSeparator1, gridBagConstraints);
	}
}
