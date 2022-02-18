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
package es.ucm.fdi.ac.hist;

import static es.ucm.fdi.util.I18N.m;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.ucm.fdi.ac.Analysis;
import es.ucm.fdi.ac.Submission;
import es.ucm.fdi.ac.gui.CompareDialog;
import es.ucm.fdi.ac.outlier.Hampel;

/**
 * Methods to support histogram use in AC
 *
 * @author mfreire
 */
public class ACHistogram {

	private static final Logger log = LogManager.getLogger(ACHistogram.class);

	/**
	 * Creates a new instance of ACHistogram
	 */
	public ACHistogram() {
	}

	/**
	 * Creates a new instance of HistogramModel, using values for all samples
	 */
	public static HistogramModel allSubmissionsModel(Analysis ac, String testKey) {
		HistogramModel m = new HistogramModel();
		Analysis.Result[] R = ac.sortTestResults(testKey);
		for (int i = 0; i < R.length; i++) {
			m.addLabelledPoint(R[i].getDist(), R[i].getA() + "," + R[i].getB());
		}
		return m;
	}

	/**
	 * Creates a new instance of HistogramModel; but using only the values
	 * for a single sample
	 */
	public static HistogramModel singleSubmissionModel(Analysis ac,
			Submission s, String testKey, boolean suggest) {
		HistogramModel m = new HistogramModel();
		boolean firstZero = true; // the first zero is ignored
		float[] f = (float[]) s.getData(testKey);
		if (suggest) {
			double[] d = new double[f.length];
			for (int i = 0; i < f.length; i++) {
				d[i] = f[i];
			}
			m.setHighlights(Hampel.hampel(d));
		}
		for (int i = 0; i < f.length; i++) {
			if (f[i] == 0 && firstZero) {
				firstZero = false;
				continue;
			}
			m.addLabelledPoint(f[i], ac.getSubmissions()[i].getId());
		}
		return m;
	}

	public static Histogram createBarHistogram(Analysis ac, String testKey) {
		Histogram h = new Histogram(allSubmissionsModel(ac, testKey),
				BarRenderer.class);
		h.setLevels(200);
		h.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				Histogram h = (Histogram) e.getSource();
				HistogramModel m = h.getModel();
				float o = ((float) e.getX()) / (float) h.getWidth();
				float f = m.getNearestPoint(o);
				StringBuffer sb = new StringBuffer("<html><b>" + f + "</b><br>");
				for (Object v : h.getModel().getLabelsForPoint(f)) {
					sb.append("&nbsp;" + v.toString() + "<br>");
				}
				sb.append("</html>");
				h.setToolTipText(sb.toString());
			}
		});
		return h;
	}

	public static Histogram createBarHistogram(Analysis ac, Submission s,
			String testKey, boolean suggest) {
		Histogram h = new Histogram(singleSubmissionModel(ac, s, testKey,
				suggest), BarRenderer.class);
		h.setLevels(100);
		return h;
	}

	public static Histogram createVerticalBarRenderer(Analysis ac,
			Submission s, String testKey, boolean suggest) {
		Histogram h = new Histogram(singleSubmissionModel(ac, s, testKey,
				suggest), VerticalBarRenderer.class);
		h.setLevels(100);
		return h;
	}

	public static Histogram createHueHistogram(Analysis ac, Submission subject,
			String testKey, boolean suggest) {
		Histogram h = new Histogram(singleSubmissionModel(ac, subject, testKey,
				suggest), HueRenderer.class);
		h.setLevels(100);
		return h;
	}

	private static class SimpleMouseListener extends MouseAdapter {
		private Histogram oldHist = null;
		private VerticalFisheyeLayout fisheye;

		public SimpleMouseListener(VerticalFisheyeLayout fisheye) {
			this.fisheye = fisheye;
		}

		public void mouseClicked(MouseEvent e) {
			Histogram h = (Histogram) e.getComponent();

			if ((e.getButton() & MouseEvent.BUTTON1) != 0) {
				HistogramModel m = h.getModel();
				float o = ((float) e.getX()) / h.getWidth();
				float f = m.getNearestPoint(o);
				String s = m.getLabelsForPoint(f).toString();
				JOptionPane.showMessageDialog(null, s, "Labels for " + f
						+ " (clicked on " + o + ")",
						JOptionPane.INFORMATION_MESSAGE);
			}

		}

		public void mouseDragged(MouseEvent e) {
		}

		public void mouseMoved(MouseEvent e) {
			//System.err.println("Entered... "+e);
			Histogram h = (Histogram) e.getComponent();
			Component c = h.getParent();
			while (!(c instanceof JPanel)) {
				c = c.getParent();
			}
			Point p = h.getLocation();
			e.translatePoint(p.x, p.y);
			fisheye.mouseMoved(e);

			if (h != oldHist) {
				h.setRenderer(BarRenderer.class);
				h.repaint();
				if (oldHist != null) {
					oldHist.setRenderer(HueRenderer.class);
					oldHist.repaint();
				}
				oldHist = h;
			}
		}
	}

	public static class HistogramTableModel extends AbstractTableModel
			implements ItemListener {

		// needed to show the code on double-click... bit ugly, but works
		private Analysis ac = null;
		private Histogram[] hs = null;
		private boolean suggestThresholds = false;

		public HistogramTableModel(Analysis ac, String testKey,
				boolean suggestThresholds) {
			this.ac = ac;
			this.suggestThresholds = suggestThresholds;
			int n = ac.getSubmissions().length;
			hs = new Histogram[n];
			for (int i = 0; i < n; i++) {
				hs[i] = ACHistogram.createHueHistogram(ac,
						ac.getSubmissions()[i], testKey, suggestThresholds);
				hs[i].setLevels(100);
				hs[i].setText(ac.getSubmissions()[i].getId());
			}
			sortBy(m("AC.Hist.ByValue"));
		}

		public String getColumnName(int i) {
			return m("AC.Hist.Histogram");
		}

		public int getColumnCount() {
			return 1;
		}

		public int getRowCount() {
			return hs.length;
		}

		public Object getValueAt(int row, int col) {
			return hs[row];
		}

		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				sortBy((String) e.getItem());
			}
		}

		public void sortBy(String s) {
			if (s.equals(m("AC.Hist.ByValue"))) {
				Arrays.sort(hs, new Comparator<Histogram>() {
					public int compare(Histogram a, Histogram b) {
						float fa = a.getModel().getLowest();
						float fb = b.getModel().getLowest();
						return Float.compare(fa, fb);
					}
				});
			} else if (s.equals(m("AC.Hist.ByGap"))) {
				Arrays.sort(hs, new Comparator<Histogram>() {
					public int compare(Histogram a, Histogram b) {
						float fa = a.getModel().getLowSkip();
						float fb = b.getModel().getLowSkip();
						return Float.compare(fb, fa);
					}
				});
			} else if (s.equals(m("AC.Hist.ByName"))) {
				Arrays.sort(hs, new Comparator<Histogram>() {
					public int compare(Histogram a, Histogram b) {
						return a.getText().compareTo(b.getText());
					}
				});
			} else {
				System.err.println("Error: unable to order by " + s);
			}
			fireTableDataChanged();
		}

		public Submission getSubmission(String id) {
			for (Submission s : ac.getSubmissions()) {
				if (s.getId().equals(id)) {
					return s;
				}
			}
			return null;
		}

		public Class<?> getColumnClass(int columnIndex) {
			return Histogram.class;
		}
	}

	/**
	 * Newfangled presentation, proudly displaying lots of JTable complexity
	 *
	 */
	public static JComponent createSortableHistogramPane(Analysis ac,
			String testKey, boolean suggestThresholds) {

		// initialize the base panel
		JPanel base = new JPanel();
		base.setLayout(new BorderLayout());

		// get the histogram panel ready ('ah')
		JTable resultTable = new JTable(new HistogramTableModel(ac, testKey,
				suggestThresholds));
		resultTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				JTable table = (JTable) e.getSource();
				if (e.getClickCount() == 1) {
					for (int i = 0; i < table.getRowCount(); i++) {
						if (table.isRowSelected(i)) {
							table.setRowHeight(i, 50);
						} else {
							table.setRowHeight(i, 15);
						}
					}
					table.doLayout();
				} else {
					// launch an editor for that chap
					int i = table.rowAtPoint(e.getPoint());
					Histogram h = (Histogram) table.getValueAt(i, 0);
					String id = h.getText();
					HistogramTableModel m = (HistogramTableModel) table
							.getModel();
					Submission one = m.getSubmission(id);

					Rectangle r = table.getCellRect(i, 0, true);
					float o = ((float) e.getX()) / (float) r.getWidth();
					float f = h.getModel().getNearestPoint(o);
					if (Math.abs(f - o) < 0.1) {
						String label = ""
								+ h.getModel().getLabelsForPoint(f).get(0);
						Submission another = m
								.getSubmission(label.split(" ")[0]);
						if (another == null) {
							log.warn("Could not find second target");
						} else {
							new CompareDialog(null, one, another)
									.setVisible(true);
						}
					} else {
						System.err.println("Error too large; "
								+ Math.abs(f - o)
								+ " not comparing against anyone");

						JDialog jd = new JDialog((JFrame) null, id, false);
						JTabbedPane jtp = new JTabbedPane();
						CompareDialog.addSubmission(one, jtp);
						jd.getContentPane().add(new JScrollPane(jtp));
						jd.setSize(600, 800);
						jd.setVisible(true);
					}
				}
			}
		});
		resultTable.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				JTable table = (JTable) e.getSource();
				Point p = e.getPoint();
				int i = table.rowAtPoint(p);
				Rectangle r = table.getCellRect(i, 0, true);
				if (r.contains(p)) {
					Histogram h = (Histogram) table.getValueAt(i, 0);
					HistogramModel m = h.getModel();
					float o = ((float) e.getX()) / (float) r.getWidth();
					float f = m.getNearestPoint(o);
					StringBuffer sb = new StringBuffer("<html><b>" + f
							+ "</b><br>");
					for (Object v : h.getModel().getLabelsForPoint(f)) {
						sb.append("&nbsp;" + v.toString() + "<br>");
					}
					sb.append("</html>");
					h.setToolTipText(sb.toString());
				}
			}
		});
		resultTable.setDefaultRenderer(Histogram.class,
				new TableCellRenderer() {
					public Component getTableCellRendererComponent(
							JTable table, Object value, boolean isSelected,
							boolean hasFocus, int row, int column) {
						Histogram h = (Histogram) table.getValueAt(row, column);
						if (isSelected) {
							h.setRenderer(BarRenderer.class);
						} else {
							h.setRenderer(HueRenderer.class);
						}
						return h;
					}
				});
		base.add(new JScrollPane(resultTable), BorderLayout.CENTER);

		// combo box to change presentation
		JPanel bottom = new JPanel();
		bottom.setLayout(new FlowLayout());
		String sortMethods[] = { m("AC.Hist.ByValue"), m("AC.Hist.ByGap"),
				m("AC.Hist.ByName") };
		JComboBox<String> combo = new JComboBox<>(sortMethods);
		bottom.add(new JLabel("Sorting criteria: "));
		bottom.add(combo);
		combo.setSelectedIndex(0);
		((HistogramTableModel) resultTable.getModel()).sortBy(sortMethods[0]);
		combo.addItemListener((ItemListener) resultTable.getModel());
		base.add(bottom, BorderLayout.SOUTH);

		return base;
	}

	/**
	 * Old fisheye version
	 */
	public static JComponent createFisheyeHueHistogramPane(Analysis ac,
			String testKey) {
		int n = ac.getSubmissions().length;
		Histogram[] hs = new Histogram[n];

		VerticalFisheyeLayout flayout = new VerticalFisheyeLayout(9, 70, 150, 1);
		JPanel ah = new JPanel() {
			private double oldWidth = -1;

			public Dimension getPreferredSize() {
				int width = (getParent() instanceof JViewport) ? ((JViewport) getParent())
						.getWidth()
						: getWidth();

				if (width != oldWidth) {
					oldWidth = width;

					// System.err.println("Queried for preferred size");
					return getLayout().preferredLayoutSize(this);
				}
				return super.getPreferredSize();
			}
		};

		ah.setLayout(flayout);
		SimpleMouseListener ml = new SimpleMouseListener(flayout);

		for (int i = 0; i < n; i++) {
			hs[i] = ACHistogram.createHueHistogram(ac, ac.getSubmissions()[i],
					testKey, false);
			hs[i].addMouseMotionListener(ml);
			hs[i].addMouseListener(ml);
			hs[i].setLevels(100);
			hs[i].setText(ac.getSubmissions()[i].getId() + " "
					+ hs[i].getModel().getLowest());
		}

		Arrays.sort(hs, new Comparator<Histogram>() {
			public int compare(Histogram a, Histogram b) {
				float fa = a.getModel().getLowest();
				float fb = b.getModel().getLowest();
				return Float.compare(fa, fb);
			}
		});

		ah.setBackground(Color.WHITE);
		for (int i = 0; i < n; i++) {
			ah.add(hs[i]);
		}

		JScrollPane jsp = new JScrollPane(ah,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		return jsp;
	}
}
