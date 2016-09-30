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
 * ClusteringTester.java
 *
 * Created on May 17, 2006, 11:39 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.test;

import es.ucm.fdi.clover.event.ClusteringChangeEvent;
import es.ucm.fdi.clover.model.Cluster;
import es.ucm.fdi.clover.model.ClusterHierarchy;
import es.ucm.fdi.clover.model.ClusterViewGraph;
import es.ucm.fdi.clover.model.ClusteredGraph;
import es.ucm.fdi.clover.model.DepthFirstIterator;
import es.ucm.fdi.clover.model.SimpleRuleClusterer;
import es.ucm.fdi.clover.test.TestGraph;

import es.ucm.fdi.clover.view.ClusterView;
import es.ucm.fdi.clover.view.BaseView;
import es.ucm.fdi.clover.view.HighlightMouseListener;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.geom.*;

import org.jgrapht.*;

import org.apache.log4j.*;

/**
 *
 * @author mfreire
 */
public class CGTest extends JFrame {

	private static Logger log = Logger.getLogger(CGTest.class);

	int state = 0;
	private ClusteredGraph cg;
	private Timer t;

	/**
	 * Creates a new instance of ClusteringTester
	 */
	public CGTest() {

		TestGraph tg =
		//                new TestGraph("([1, 2, 3, 4, 5, 6], [{1,2}, {1,3}, {1,4}, {4,5}, {4,6}])");        
		//                new TestGraph("([1, 2, 3, 4, 5, 6, 7, 8, 9], " +
		//                "[{1,2}, {1,3}, {3,4}, {4,5}, {4,6}, {3,7}, {7,9}, {7,8}])");
		//                new TestGraph(10, 14);
		new TestGraph(
				"([0, 1, 2, 3, 4, 5, 6, 7, 8, 9], "
						+ "[{0,1}, {1,2}, {2,3}, {3,4}, {4,5}, {5,6}, {6,7}, {7,8}, {8,9}, {0,9}, {6,8}, {3,7}, {1,9}])");
		ViewTester vt = new ViewTester(tg);
		vt.setVisible(true);
		ClusterHierarchy ch = new ClusterHierarchy(tg, "1",
				new SimpleRuleClusterer());
		log.info("Hierarchy OK");
		log.debug(ch.getRoot().dump());
		cg = new ClusteredGraph(ch);
		//        
		//            Cluster root = cg.getHierarchy().getRoot();
		//            Cluster c = root.clusterForVertex("5").getParentCluster();
		//            ClusteringChangeEvent cce;
		//            cce = cg.createExpandEvent(root.getVertex());
		////        cg.clusteringChangePerformed(cce);
		////        log.info("Expanded root: \n\t"+cg);
		////        log.info("Expanded root: \n\t"+cce.getDescription());
		////        cce = cg.createExpandEvent(c.getVertex());
		//            cce.addExpanded(c);
		//            cg.clusteringChangePerformed(cce);
		//            log.info("Expanded again: \n\t"+cg);
		//            log.info("Expanded again: \n\t"+cce.getDescription());

		setTitle("hi mom!");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		getContentPane().add(new ClusterView(new ClusterViewGraph(cg)),
				BorderLayout.CENTER);
		setVisible(true);
		setSize(800, 600);
		//        t = new Timer(5000, new StuffChanger());
		//        t.setRepeats(true);
		//        t.start();
		//        
		//        Timer t2 = new Timer(1000, new Ticker());
		//        t2.setRepeats(true);
		//        t2.start();
	}

	public class Ticker implements ActionListener {
		private long t0;

		public Ticker() {
			t0 = System.currentTimeMillis();
		}

		public void actionPerformed(ActionEvent evt) {
			setTitle("" + (System.currentTimeMillis() - t0) / 1000);
		}
	}

	public class StuffChanger implements ActionListener {

		public void actionPerformed(ActionEvent evt) {
			ClusteringChangeEvent cce;
			Cluster root = cg.getHierarchy().getRoot();
			Cluster c = root.clusterForVertex("5").getParentCluster();
			Cluster cp = c.getParentCluster();

			log.info("ROUND AND ROUND! " + state);

			switch (state++) {
			case 0:
				cce = cg.createExpandEvent(root.getVertex());
				log.info("(1): \n\t" + cce.getDescription());
				cce.addExpanded(cp);
				log.info("(2): \n\t" + cce.getDescription());
				cce.addExpanded(c);
				log.info("(3): \n\t" + cce.getDescription());
				cg.clusteringChangePerformed(cce);
				break;

			case 1:
				// probably broken....
				cce = cg.createCollapseEvent(c.getVertex());
				log.info("Collapsed again: \n\t" + cg);
				cce.addCollapsed(cp);
				cce.addCollapsed(root);
				cg.clusteringChangePerformed(cce);
				break;
			////                case 3:                
			////                    cce = cg.createCollapseEvent(root.getVertex());
			//                    cce.addCollapsed(root);
			//                    cg.clusteringChangePerformed(cce);
			//                    log.info("Collapsed to start: \n\t"+cg);
			//                    break;   
			//                    
			//                case 2:
			//                    state = 0;
			}
		}
	}

	public static void main(String args[]) {
		new CGTest();
		if (false) {
			TestGraph tg = new TestGraph(
					"([1, 2, 3, 4, 5, 6], [{1,2}, {1,3}, {1,4}, {4,5}, {4,6}])");
			ClusterHierarchy ch = new ClusterHierarchy(tg, "1",
					new SimpleRuleClusterer());
			log.info("Hierarchy OK");
			log.debug(ch.getRoot().dump());
			Cluster root = ch.getRoot();
			Cluster c = root.clusterForVertex("5").getParentCluster();

			ClusteredGraph cg = new ClusteredGraph(ch);
			ClusteringChangeEvent cce;
			cce = cg.createExpandEvent(root.getVertex());
			//        cg.clusteringChangePerformed(cce);
			//        log.info("Expanded root: \n\t"+cg);
			//        log.info("Expanded root: \n\t"+cce.getDescription());
			//        cce = cg.createExpandEvent(c.getVertex());
			cce.addExpanded(c);
			cg.clusteringChangePerformed(cce);
			log.info("Expanded again: \n\t" + cg);
			log.info("Expanded again: \n\t" + cce.getDescription());
			cce = cg.createCollapseEvent(c.getVertex());
			//        cg.clusteringChangePerformed(cce);
			//        log.info("Collapsed again: \n\t"+cg);
			//        log.info("Collapsed again: \n\t"+cce.getDescription());
			//        cce = cg.createCollapseEvent(root.getVertex());
			cce.addCollapsed(root);
			cg.clusteringChangePerformed(cce);
			log.info("Collapsed to start: \n\t" + cg);
			log.info("Collapsed to start: \n\t" + cce.getDescription());
		}
	}
}
