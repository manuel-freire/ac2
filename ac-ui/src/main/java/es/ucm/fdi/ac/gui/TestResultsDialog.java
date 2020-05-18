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
 * TestResultsDialog.java 
 *
 * A dialog that shows the results of a test; several informative panels are
 * provided.
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * Contributors: - 
 * Changelog: 
 *     27-Apr-2006: first version (mfreire)
 */

package es.ucm.fdi.ac.gui;

import es.ucm.fdi.ac.Analysis;
import es.ucm.fdi.ac.hist.ACHistogram;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Comparator;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

/**
 * Graphically displays results for a given test
 */
public class TestResultsDialog extends javax.swing.JDialog {

	private boolean suggestThresholds = false;
	private Analysis ac;
	private String testKey;

	public TestResultsDialog(java.awt.Frame parent, Analysis ac, String testKey) {
		super(parent, false);
		initComponents();
		this.ac = ac;
		this.testKey = testKey;
		showResults();
		setSize(800, 600);
	}

	public void setSuggestThresholds(boolean suggest) {
		this.suggestThresholds = suggest;
	}

	public static class ResultTableModel extends AbstractTableModel {
		private Analysis.Result[] R = null;
		private String[] names = { "Distance", "One", "The other" };
		private int sortColumn = 0;

		/**
		 * Note: R must be initially sorted by first field
		 */
		public ResultTableModel(Analysis.Result[] R) {
			this.R = R;
		}

		public String getColumnName(int i) {
			return names[i];
		}

		public int getColumnCount() {
			return names.length;
		}

		public int getRowCount() {
			return R.length;
		}

		public Object getValueAt(int row, int col) {
			Analysis.Result r = R[row];
			if (col == 0)
				return r.getDist();
			else if (col == 1)
				return r.getA().getId();
			else
				return r.getB().getId();
		}

		public void sortBy(int col) {
			if (col != sortColumn) {
				sortColumn = col;
				if (col == 0) {
					Arrays.sort(R, new Comparator<Analysis.Result>() {
						public int compare(Analysis.Result a, Analysis.Result b) {
							return Float.compare(a.getDist(), b.getDist());
						}
					});
				} else if (col == 1) {
					Arrays.sort(R, new Comparator<Analysis.Result>() {
						public int compare(Analysis.Result a, Analysis.Result b) {
							return a.getA().getId().compareTo(b.getA().getId());
						}
					});
				} else {
					Arrays.sort(R, new Comparator<Analysis.Result>() {
						public int compare(Analysis.Result a, Analysis.Result b) {
							return a.getB().getId().compareTo(b.getB().getId());
						}
					});
				}
				fireTableDataChanged();
			}
		}

		public Analysis.Result getResultAt(int row) {
			return R[row];
		}
	}

	public void showResults() {
		setTitle(testKey);

		ACGraphPanel acgp = new ACGraphPanel(ac, testKey, suggestThresholds);
		ACGraphPanelD acgpd = new ACGraphPanelD(ac, testKey, suggestThresholds);
		ACTableViz actv = new ACTableViz(ac, testKey);

		Analysis.Result[] R = ac.sortTestResults(testKey);
		JTable resultTable = new JTable(new ResultTableModel(R));
		resultTable.getTableHeader().addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				JTableHeader header = (JTableHeader) e.getComponent();
				int col = header.getColumnModel().getColumnIndexAtX(e.getX());
				((ResultTableModel) header.getTable().getModel()).sortBy(col);
			}
		});
		resultTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					JTable table = (JTable) e.getSource();
					int row = table.rowAtPoint(e.getPoint());

					Analysis.Result result = ((ResultTableModel) table
							.getModel()).getResultAt(row);

					CompareDialog cd = new CompareDialog(null, result.getA(),
							result.getB());
					cd.setVisible(true);
				}
			}
		});

		JScrollPane table = new JScrollPane(resultTable);

		jtp.removeAll();
		jtp.add("Individual histograms", ACHistogram
				.createSortableHistogramPane(ac, testKey, suggestThresholds));
		jtp.add("Graph + Distance histogram", acgp);
		jtp.add("Graph + Dendrogram", acgpd);
		jtp.add("Visual table", actv);
		jtp.add("Table", table);
		jtp.setSelectedIndex(1);

		validate();
		repaint();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		jtp = new javax.swing.JTabbedPane();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		getContentPane().add(jtp, java.awt.BorderLayout.CENTER);

		pack();
	}// </editor-fold>//GEN-END:initComponents

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JTabbedPane jtp;
	// End of variables declaration//GEN-END:variables

}
