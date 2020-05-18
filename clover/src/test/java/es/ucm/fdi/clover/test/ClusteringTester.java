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
 * ClusteringTester.java
 *
 * Created on May 17, 2006, 11:39 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.test;

import es.ucm.fdi.clover.model.DepthFirstIterator;
import es.ucm.fdi.clover.model.SimpleRuleClusterer;
import es.ucm.fdi.clover.test.TestGraph;

/**
 *
 * @author mfreire
 */
public class ClusteringTester {

	private static boolean isRunning = true;

	/**
	 * Creates a new instance of ClusteringTester
	 */
	public ClusteringTester() {
	}

	public static void main(String args[]) {
		//System.err.println(tg.toString());

		//        Thread t = new Thread(new Runnable() {
		//            public void run() {
		//                try {Thread.sleep(10000);} catch (Exception e) {}
		//                if ( ! isRunning) return;
		//                System.err.println("== TIME'S UP ==");
		//                System.exit(255);
		//            }            
		//        });
		//        t.start();//                        

		int i = 10;
		TestGraph tg =
		//                new TestGraph(i, i);
		new TestGraph("([1, 2, 3, 4, 5, 6, 7, 8, 9], "
				+ "[{1,2}, {1,3}, {3,4}, {4,5}, {4,6}, {3,7}, {7,9}, {7,8}])");

		long t0 = System.currentTimeMillis();
		SimpleRuleClusterer src = new SimpleRuleClusterer();
		src.createHierarchy(tg, "1");
		isRunning = false;
		System.out.println("" + i + " " + (System.currentTimeMillis() - t0));

		//        for (int i=10; i<10000; i=Math.max(i+2,(int)(i*1.5))) {
		//            for (int j=0; j<10; j++) {
		//                TestGraph tg = new TestGraph(i, i*2);
		//                long t0 = System.currentTimeMillis();
		//                SimpleRuleClusterer src = new SimpleRuleClusterer();
		//                src.setRoot(""+0);
		//                src.createHierarchy(tg);   
		//                isRunning = false;
		//                System.out.println(""+i+" "+(System.currentTimeMillis() - t0));
		//            }
		//        }
	}
}
