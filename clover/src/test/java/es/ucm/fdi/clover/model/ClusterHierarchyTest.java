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
 * ClusterHierarchyTest.java
 * JUnit based test
 *
 * Created on October 22, 2006, 4:19 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.model;

import java.util.ArrayList;
import java.util.HashMap;
import junit.framework.*;
import es.ucm.fdi.clover.event.HierarchyChangeEvent;
import es.ucm.fdi.clover.event.StructureChangeEvent;
import es.ucm.fdi.clover.test.TestGraph;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author mfreire
 */
public class ClusterHierarchyTest extends TestCase {
	private TestGraph tg;
	private ClusterHierarchy ch;
	private SimpleRuleClusterer src;
	private Object v1 = null, v3 = null, v7 = null, v4 = null, v5 = null;
	private Edge e34 = null;
	private Cluster r1 = null;

	public ClusterHierarchyTest(String testName) {
		super(testName);
	}

	protected void setUp() throws Exception {
		tg = new TestGraph("([1, 2, 3, 4, 5, 6, 7, 8, 9], "
				+ "[{1,2}, {1,3}, {3,4}, {4,5}, {4,6}, {3,7}, {7,9}, {7,8}])");
		src = new SimpleRuleClusterer();
		ch = new ClusterHierarchy(tg, "1", src);

		for (Object v : tg.vertexSet()) {
			if (v.toString().equals("3")) {
				v3 = v;
				for (Edge e : (Collection<Edge>) tg.outgoingEdgesOf(v3)) {
					if (e.getTarget().toString().equals("4")) {
						e34 = e;
						v4 = e34.getTarget();
					}
				}
			} else if (v.toString().equals("4"))
				v4 = v;
			else if (v.toString().equals("7"))
				v7 = v;
			else if (v.toString().equals("5"))
				v5 = v;
			else if (v.toString().equals("1"))
				v1 = v;
		}

		r1 = ch.getRoot();
	}

	protected void tearDown() {

	}

	public static Test suite() {
		TestSuite suite = new TestSuite(ClusterHierarchyTest.class);

		return suite;
	}

	/**
	 * test change of an edge
	 */
	public void testHierarchyChangePerformed1() {

		System.err.println("testRecreateHierarchy1");

		// detach: avoid event-based notification, and go manual
		tg.removeStructureChangeListener(ch);

		StructureChangeEvent sce = new StructureChangeEvent(tg);
		sce.getRemovedEdges().add(e34);
		sce.getAddedEdges().add(new Edge(v7, v4));
		tg.structureChangePerformed(sce);

		BaseGraph base = tg;
		Object rootVertex = v1;
		Cluster oldRoot = r1;
		HierarchyChangeEvent hce = new HierarchyChangeEvent(ch, "test change");

		ch.getEngine().updateHierarchy(r1, tg, sce, hce);
		assertEquals(5, hce.getMatchedClusters().size());

		System.err.println("before hce1 = " + hce.getDescription());
		ch.hierarchyChangePerformed(hce);
		System.err.println("after hce1 = " + hce.getDescription());

		// check that the hce got updated correctly
		for (ArrayList<Cluster> l : hce.getAddedClusters().values()) {
			assertTrue(Utils.checkSameRoot(r1, l));
		}

		// check that it was correctly applied
		String[] expected = new String[] { "{3.4.5.6.7.8.9}", "{4.5.6.7.8.9}",
				"{4.5.6}", "{1}", "{2}", "{3}", "{4}", "{5}", "{6}", "{7}",
				"{8}", "{9}" };
		assertTrue(Utils.checkSameClusters(expected, r1.getDescendants(), tg));
	}

	/**
	 * test change of an edge
	 */
	public void testHierarchyChangePerformed2() {

		System.err.println("testRecreateHierarchy2");

		// detach: avoid event-based notification, and go manual
		tg.removeStructureChangeListener(ch);

		StructureChangeEvent sce = new StructureChangeEvent(tg);
		Object v10 = 10;
		sce.getAddedVertices().add(v10);
		Edge e7_10 = new Edge(v7, v10);
		sce.getAddedEdges().add(e7_10);
		tg.structureChangePerformed(sce);

		BaseGraph base = tg;
		Object rootVertex = v1;
		Cluster oldRoot = r1;
		HierarchyChangeEvent hce = new HierarchyChangeEvent(ch, "test change");

		ch.getEngine().updateHierarchy(r1, tg, sce, hce);
		assertEquals(1, hce.getMatchedClusters().size());

		System.err.println("before hce2 = " + hce.getDescription());
		ch.hierarchyChangePerformed(hce);
		System.err.println("after hce2 = " + hce.getDescription());

		// check that the hce got updated correctly
		for (ArrayList<Cluster> l : hce.getAddedClusters().values()) {
			assertTrue(Utils.checkSameRoot(r1, l));
		}

		// check that it was correctly applied
		String[] expected = new String[] { "{10.3.4.5.6.7.8.9}", "{4.5.6}",
				"{10.7.8.9}", "{1}", "{2}", "{3}", "{4}", "{5}", "{6}", "{7}",
				"{8}", "{9}", "{10}" };
		assertTrue(Utils.checkSameClusters(expected, r1.getDescendants(), tg));

		// see if edge was duly added
		Cluster c7 = r1.getLastClusterFor(v7);
		Cluster c10 = r1.getLastClusterFor(v10);
		assertTrue(c7.hasEdgesTo(c10));

		// and parent must know of it too
		Cluster c7p = c7.getParentCluster();
		assertTrue(c7.localOutgoingNeighbors().contains(c10));
		System.err.println(c7.dump());
	}

	//
	//    /**
	//     * Test of save method, of class eps.clover.model.ClusterHierarchy.
	//     */
	//    public void testSave() {
	//        System.out.println("save");
	//        
	//        Element e = null;
	//        ClusterHierarchy instance = null;
	//        
	//        instance.save(e);
	//        
	//        // TODO review the generated test code and remove the default call to fail.
	//        fail("The test case is a prototype.");
	//    }
	//
	//    /**
	//     * Test of restore method, of class eps.clover.model.ClusterHierarchy.
	//     */
	//    public void testRestore() {
	//        System.out.println("restore");
	//        
	//        Element e = null;
	//        ClusterHierarchy instance = null;
	//        
	//        instance.restore(e);
	//        
	//        // TODO review the generated test code and remove the default call to fail.
	//        fail("The test case is a prototype.");
	//    }
	//
	//    /**
	//     * Test of createChangeEventFor method, of class eps.clover.model.ClusterHierarchy.
	//     */
	//    public void testCreateChangeEventFor() {
	//        System.out.println("createChangeEventFor");
	//        
	//        Cluster newClustering = null;
	//        StructureChangeEvent structureChange = null;
	//        HierarchyChangeEvent hierarchyChange = null;
	//        ClusterHierarchy instance = null;
	//        
	//        instance.createChangeEventFor(newClustering, structureChange, hierarchyChange);
	//        
	//        // TODO review the generated test code and remove the default call to fail.
	//        fail("The test case is a prototype.");
	//    }
	//
	//    /**
	//     * Test of hierarchyChangePerformed method, of class eps.clover.model.ClusterHierarchy.
	//     */
	//    public void testHierarchyChangePerformed() {
	//        System.out.println("hierarchyChangePerformed");
	//        
	//        HierarchyChangeEvent evt = null;
	//        ClusterHierarchy instance = null;
	//        
	//        instance.hierarchyChangePerformed(evt);
	//        
	//        // TODO review the generated test code and remove the default call to fail.
	//        fail("The test case is a prototype.");
	//    }     
}
