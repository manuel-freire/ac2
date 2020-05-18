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
 * TestGui.java
 *
 * Created on October 24, 2005, 10:46 AM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.test;

import es.ucm.fdi.clover.gui.BaseInterface;
import es.ucm.fdi.clover.event.StructureChangeEvent;
import es.ucm.fdi.clover.model.BaseGraph;
import es.ucm.fdi.clover.model.ClusterHierarchy;
import es.ucm.fdi.clover.model.ClusterViewGraph;
import es.ucm.fdi.clover.model.ClusteredGraph;
import es.ucm.fdi.clover.model.Edge;
import es.ucm.fdi.clover.model.SimpleRuleClusterer;
import es.ucm.fdi.clover.view.ClusterView;
import java.awt.Point;
import org.jdom2.Element;

import java.awt.event.*;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author mfreire
 */
public class TestGui extends BaseInterface {

	private static Log log = LogFactory.getLog(TestGui.class);

	/** Creates a new instance of TestGui */
	public TestGui() {
		JMenu testMenu = new JMenu("test");
		JMenuItem test1 = new JMenuItem("remove 3rd");
		testMenu.add(test1);
		JMenuItem test2 = new JMenuItem("add 10th");
		testMenu.add(test2);
		super.menuBar.add(testMenu);
		test1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				BaseGraph bg = ((ClusteredGraph) getView().getBase()).getBase();
				Object o = null;
				int n = 3;
				for (Object v : bg.vertexSet()) {
					if (n-- == 0)
						break;
					o = v;
				}
				System.err.println("Removing " + o);
				StructureChangeEvent sce = new StructureChangeEvent(bg);
				sce.getRemovedVertices().add(o);
				bg.structureChangePerformed(sce);
			}
		});
		test2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				BaseGraph bg = ((ClusteredGraph) getView().getBase()).getBase();
				Object o = null;
				int n = 8;
				for (Object v : bg.vertexSet()) {
					if (n-- == 0)
						break;
					o = v;
				}
				System.err.println("Adding '10' to " + o);
				StructureChangeEvent sce = new StructureChangeEvent(bg);
				Object a = "10";
				sce.getAddedVertices().add(a);
				sce.getAddedEdges().add(new Edge(o, a));
				bg.structureChangePerformed(sce);
			}
		});
	}

	/**
	 * Initialize action array (and allow action lookup)
	 */
	protected void initActions() {
		Action[] a = new Action[] { new TestAddVertexAction(this),
				new TestAddEdgeAction(this), new TestRemoveAction(this) };

		super.initActions();

		// NOTE: edit will overwrite super's edit

		for (int i = 0; i < a.length; i++) {
			actions.put((String) ((Action) a[i]).getValue(Action.NAME), a[i]);
		}
	}

	/**
	 * does not save anything
	 */
	public void miSaveActionPerformed(String fileName) {
		return;
	}

	/**
	 * does not save anything either
	 */
	public void miSaveGxlActionPerformed(String fileName) {
		return;
	}

	/**
	 * does not open anything
	 */
	public void miOpenActionPerformed(String fileName) {
		return;
	}

	/**
	 * 'create new' selected: create and open a new graph
	 */
	public void miCreateActionPerformed(String fileName) {
		log.info("creating model");

		//String s = "([0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29], [{0,1}, {1,2}, {2,3}, {3,4}, {4,5}, {5,6}, {6,7}, {7,8}, {8,9}, {9,10}, {10,11}, {11,12}, {12,13}, {13,14}, {14,15}, {15,16}, {16,17}, {17,18}, {18,19}, {19,20}, {20,21}, {21,22}, {22,23}, {23,24}, {24,25}, {25,26}, {26,27}, {27,28}, {28,29}, {6,13}, {18,22}, {11,29}, {3,9}, {20,28}, {3,21}, {11,18}, {11,20}, {21,8}, {4,25}, {16,20}, {3,16}, {8,18}, {8,11}, {9,22}, {3,19}, {10,22}, {9,20}, {18,20}, {4,9}])";
		String s = "([1, 2, 3, 4, 5, 6, 7, 8, 9], "
				+ "[{1,2}, {1,3}, {3,4}, {4,5}, {4,6}, {3,7}, {7,9}, {7,8}])";
		this.baseGraph = new TestGraph(s);
		//this.baseGraph = new TestGraph(10, 14);
		//System.err.println(((TestGraph)baseGraph).dump());
		//        this.baseGraph = new TestGraph("([1, 2, 3, 4, 5, 6, 7, 8, 9], " +
		//                "[{1,2}, {1,3}, {3,4}, {4,5}, {4,6}, {3,7}, {7,9}, {7,8}])");
		//        new TestGraph(
		//                "([0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11], " +
		//                "[{0,1}, {1,2}, {2,3}, {3,4}, {4,5}, {6,7}, " +
		//                " {7,11}, {7,8}, {8,9}, {0,9}, {6,8}, {3,7}, {1,9}, {5,10}])"                
		//                );
		log.info("model created!");
		jtpGraph.removeAll();
		createNewGraphView(createNewGraph(baseGraph));
	}

	public static class TestClusterViewGraph extends ClusterViewGraph {
		public TestClusterViewGraph(ClusteredGraph base) {
			super(base);
		}

		public String getEdgeLabel(es.ucm.fdi.clover.model.Edge e) {
			return e.getClass().getSimpleName();
		}
	}

	/**
	 * @return customized popu menu for app
	 */
	public JPopupMenu createMenu(Point p, Object o) {
		return new TestPopupMenu(this, o, p);
	}

	/**
	 * opens a graph
	 */
	public ClusterView createNewGraph(BaseGraph baseGraph) {
		if (log.isDebugEnabled()) {
			log.debug(baseGraph.dumpEdgeTypes());
		}
		ClusterHierarchy ch = new ClusterHierarchy(baseGraph, "" + 0,
				new SimpleRuleClusterer());
		ClusterView cv = new ClusterView(new TestClusterViewGraph(
				new ClusteredGraph(ch)));
		return cv;
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		try {
			TestGui tg = new TestGui();
			tg.miContents.setEnabled(false);
			tg.setVisible(true);
			tg.miCreateActionPerformed(null);
		} catch (Exception e) {
			log.fatal(e);
			e.printStackTrace();
		}
	}

	public ClusterView createClusterView(ClusterHierarchy h, Element e) {
		return new ClusterView(new TestClusterViewGraph(new ClusteredGraph(h)));
	}
}
