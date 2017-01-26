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
 * ACGraph.java
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * Contributors: -
 * Changelog:
 *     18-Apr-2006: first version (mfreire)
 */

package es.ucm.fdi.ac.graph;

import es.ucm.fdi.ac.Analysis;
import es.ucm.fdi.ac.Submission;
import es.ucm.fdi.ac.gui.CompareDialog;
import es.ucm.fdi.clover.model.ViewGraph;
import es.ucm.fdi.ac.hist.Histogram;
import es.ucm.fdi.ac.hist.ACHistogram;
import es.ucm.fdi.ac.test.VarianceSubtest;

import es.ucm.fdi.clover.event.StructureChangeEvent;
import es.ucm.fdi.clover.layout.Node;
import es.ucm.fdi.clover.layout.ForceTreeLayout;
import es.ucm.fdi.clover.model.Edge;
import es.ucm.fdi.clover.view.Animator;
import es.ucm.fdi.clover.view.BaseView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ToolTipManager;

import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultPort;
import org.jgrapht.Graphs;

/**
 * An ACGraph graphically plots an AC, showing only vertices united by edges below
 * a given threshold. Clicking on a node selects it, clicking on an edge selects
 * both nodes it unites.
 *
 * @author mfreire
 */
public class ACGraph extends BaseView {

	public static final String AC_SEL_CHANGE = "ac_selection_change";

	private Analysis ac;
	private String testKey;
	private ACModel acm;
	private ArrayList<ArrayList> components;

	// histograms
	private HashMap<Submission, JPanel> histograms;
	private JTabbedPane jtpHist;
	private JDialog jdHistograms;

	private DefaultGraphCell first = null;
	private DefaultGraphCell second = null;

	public static ACViewGraph createViewGraph(Analysis ac, String testKey) {
		ACModel model = new ACModel(ac, testKey);
		model.load();
		return new ACViewGraph(model);
	}

	public ACGraph(ACViewGraph acv, Analysis ac, String testKey) {
		super(acv);
		this.acm = (ACModel) acv.getBase();
		this.ac = ac;
		this.testKey = testKey;
		this.setAntiAliased(true);
		this.setSelectionEnabled(true);
		this.addMouseListener(new MouseSelectionAdapter());
		this.setEditable(false);
		this.setDisconnectable(false);
		ToolTipManager.sharedInstance().registerComponent(this);
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
		this.setAnimator(new FastAnimator(this));
	}

	public ACModel getGraph() {
		return acm;
	}

	public void start(float maxDistance, HashMap<Submission, Integer> clusters) {
		acm.setThresholdDistance(maxDistance, clusters);
		components = findComponents();
		setSelectionCells(new Object[0]);
		repaint();
	}

	public DefaultGraphCell getFirst() {
		return first;
	}

	public DefaultGraphCell getSecond() {
		return second;
	}

	public ArrayList<ArrayList> findComponents() {

		// update node cache
		getAnimator().getLayoutManager().setNodes(this);

		// get connected components in cache
		ArrayList<ArrayList> connected = new ArrayList<ArrayList>();
		for (Node n : getAnimator().getLayoutManager().getNodes()) {
			while (n.component >= connected.size()) {
				connected.add(new ArrayList());
			}
			connected.get(n.component).add(((CellView) n.peer).getCell());
		}

		return connected;
	}

	public void paint(Graphics g) {
		super.paint(g);

		Rectangle r;

		// adjust scale factor for overlays
		((Graphics2D) g).scale(getScale(), getScale());

		// draw borders around first and second selected
		if (first != null) {
			r = getCellBounds(first).getBounds();
			g.setColor(Color.blue);
			g.drawRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
		}
		if (second != null) {
			r = getCellBounds(second).getBounds();
			g.setColor(Color.red);
			g.drawRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
		}

		// paint components
		if (components == null)
			return;
		g.setColor(new Color(200, 200, 230));

		for (ArrayList component : components) {
			Rectangle2D bounds = null;
			for (Object cell : component) {
				bounds = (bounds == null) ? getCellBounds(cell) : bounds
						.createUnion(getCellBounds(cell));
			}
			if (bounds != null) {
				r = bounds.getBounds();
				g.drawRect(r.x - 3, r.y - 3, r.width + 6, r.height + 6);
			}
		}
	}

	private static class FastAnimator extends Animator {
		FastAnimator(BaseView v) {
			super(v);
		}

		public void structureChangePerformed(StructureChangeEvent sce) {

			ViewGraph g = view.getViewGraph();

			// remove stuff
			g.removeAllVertices(sce.getRemovedVertices());
			for (Edge e : sce.getRemovedEdges()) {
				g.removeEdge(e.getSource(), e.getTarget());
			}
			// add everything
			Graphs.addAllVertices(g, sce.getAddedVertices());
			for (Edge e : sce.getAddedEdges()) {
				g.addEdge(e.getSource(), e.getTarget(), e);
			}

			// update view
			manager.setNodes(view);

			if (manager.getNodes().length == 0) {
				return;
			}

			manager.setAlgorithm(new ForceTreeLayout(60));
			manager.run();
			manager.applyChanges(view);
			manager.setAlgorithm(vlfdl);
			manager.setMaxTime(1000);
			manager.run();
			manager.setAlgorithm(celfdl);
			manager.run();
			manager.setAlgorithm(vbl);
			manager.run();
			manager.setAlgorithm(ftal);
			manager.run();
			manager.setAlgorithm(sbl);
			manager.run();
			manager.applyChanges(view);
		}
	}

	private class MouseSelectionAdapter extends MouseAdapter {

		public void mouseReleased(MouseEvent e) {
			if (e.getClickCount() == 2) {
				doCompare(e);
			} else {
				doSelect(e);
			}
		}

		public void doCompare(MouseEvent e) {
			Object o = getFirstCellForLocation(e.getX(), e.getY());
			if (o == null || !(o instanceof DefaultGraphCell)) {
				return;
			}

			DefaultGraphCell c = (DefaultGraphCell) o;
			if (!(c.getUserObject() instanceof Submission)) {
				CompareDialog cd = new CompareDialog(null, (Submission) first
						.getUserObject(), (Submission) second.getUserObject());
				cd.setVisible(true);
				return;
			}

			Submission s = (Submission) c.getUserObject();

			if (jtpHist == null) {
				jtpHist = new JTabbedPane();
				jdHistograms = new JDialog();
				jdHistograms
						.setTitle("Similarity histogram comparing the selected group to all others");
				jdHistograms.getContentPane().setLayout(new BorderLayout());
				jdHistograms.getContentPane().add(jtpHist, BorderLayout.CENTER);
				jdHistograms.setSize(600, 120);
				histograms = new HashMap<Submission, JPanel>();
				jdHistograms.setVisible(true);
			}

			if (histograms.containsKey(s)) {
				jtpHist.setSelectedComponent(histograms.get(s));
			} else {
				String key = testKey;
				if (key.startsWith(VarianceSubtest.SUBJECT_VARDIST_KEY)) {
					key = key.substring(VarianceSubtest.SUBJECT_VARDIST_KEY
							.length());
					//System.err.println("Using key = "+key);
				}

				// FIXME: should probably know whether to suggest or not; always suggesting here
				Histogram bh = ACHistogram.createBarHistogram(ac, s, key, true);
				jtpHist.add(s.getId(), bh);
				jtpHist.setSelectedComponent(bh);
				histograms.put(s, bh);
				jdHistograms.invalidate();
			}

			if (!jdHistograms.isShowing()) {
				jdHistograms.toFront();
			}
		}

		public void doSelect(MouseEvent e) {
			Object o = getFirstCellForLocation(e.getX(), e.getY());
			if (o == null) {
				first = second = null;
			} else if (o instanceof DefaultEdge) {
				DefaultEdge edge = (DefaultEdge) o;
				first = (DefaultGraphCell) ((DefaultPort) edge.getSource())
						.getParent();
				second = (DefaultGraphCell) ((DefaultPort) edge.getTarget())
						.getParent();
			} else if (o instanceof DefaultGraphCell) {
				if (first == o) {
					first = null;
				} else if (second == o) {
					second = null;
				} else if (first == null) {
					first = (DefaultGraphCell) o;
				} else if (second == null) {
					second = (DefaultGraphCell) o;
				} else {
					second = first;
					first = (DefaultGraphCell) o;
				}
			} else {
				first = second = null;
			}
			firePropertyChange(AC_SEL_CHANGE, 1, 2);
			repaint();
		}
	}
}
