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
package es.ucm.fdi.clover.model;

import es.ucm.fdi.clover.event.HierarchyChangeEvent;
import es.ucm.fdi.clover.event.StructureChangeEvent;
import java.util.ArrayList;
import junit.framework.*;
import es.ucm.fdi.clover.test.TestGraph;
import java.util.Collection;

/**
 *
 * @author mfreire
 */
public class SimpleRuleClustererTest extends TestCase {

	private TestGraph tg;
	private ClusterHierarchy ch;
	private SimpleRuleClusterer src;
	private Object v1 = null, v3 = null, v7 = null, v4 = null, v5 = null;
	private Edge e34 = null;
	private Cluster r1 = null;

	public SimpleRuleClustererTest(String testName) {
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

	public static Test suite() {
		TestSuite suite = new TestSuite(SimpleRuleClustererTest.class);

		return suite;
	}

	public void test30NodeGraph() {

	}

	/**
	 * Test of createHierarchy method, of class eps.clover.model.SimpleRuleClusterer.
	 */
	public void testCreateHierarchy() {
		BaseGraph base = tg;
		Object rootVertex = v1;
		SimpleRuleClusterer instance = new SimpleRuleClusterer();

		Cluster result = instance.createHierarchy(base, rootVertex);

		// expected output of simple clustering (only descendants of root)
		String[] expected = new String[] { "{1}", "{2}", "{3}", "{4}", "{5}",
				"{6}", "{7}", "{8}", "{9}", "{7.8.9}", "{4.5.6}",
				"{3.4.5.6.7.8.9}" };

		// check to see if they're equal
		assertTrue(Utils.checkSameClusters(expected, result.getDescendants(),
				tg));
	}

	//    /**
	//     * Test of buildCluster method, of class eps.clover.model.SimpleRuleClusterer.
	//     */
	//    public void testBuildCluster() {
	//        System.out.println("buildCluster");
	//        
	//        SliceGraph graph = null;
	//        ArrayList vertices = null;
	//        Object rootVertex = null;
	//        SimpleRuleClusterer instance = new SimpleRuleClusterer();
	//        
	//        Cluster.Vertex expResult = null;
	//        Cluster.Vertex result = instance.buildCluster(graph, vertices, rootVertex);
	//        assertEquals(expResult, result);
	//        
	//        // TODO review the generated test code and remove the default call to fail.
	//        fail("The test case is a prototype.");
	//    }

	/**
	 * Test of recreateHierarchy method, of class eps.clover.model.SimpleRuleClusterer.
	 *
	 * test change of an edge
	 */
	public void testRecreateHierarchy1() {

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
		HierarchyChangeEvent hce = new HierarchyChangeEvent(ch,
				"empty test change");

		SimpleRuleClusterer instance = new SimpleRuleClusterer();

		Cluster c789 = r1.getLastClusterFor(v7).getParentCluster();
		Cluster c3456789 = (Cluster) c789.getPath()[1];
		//        System.err.println("Old dump: "+ch.getRoot().dump());
		Cluster result = instance.recreateHierarchy(base, rootVertex, sce, hce);
		//        System.err.println("New dump: "+result.dump());

		System.err.println("hce: " + hce.getDescription());

		assertEquals(5, hce.getMatchedClusters().size());

		// expected output is "add {4.5.6.7.8.9} remove {7.8.9} {4.5.6} change 
		// {3.4.5.6.7.8.9}, {1.2.3.4.5.6.7.8.9}"

		assertEquals(1, hce.getRemovedClusters().size());
		assertEquals(2, hce.getRemovedClusters().values().iterator().next()
				.size());

		assertEquals(1, hce.getAddedClusters().size());
		assertEquals("{4.5.6.7.8.9}", hce.getAddedClusters().values()
				.iterator().next().get(0).getListing(tg));

		assertEquals(2, hce.getChangedClusters().size());
		assertTrue(hce.getChangedClusters().contains(r1));
		assertTrue(hce.getChangedClusters().contains(c3456789));

		assertEquals(1, hce.getRemovedEdges().size());
		assertEquals(1, hce.getAddedEdges().size());
	}

	/**
	 * Test of recreateHierarchy method, of class eps.clover.model.SimpleRuleClusterer.
	 *
	 * test vertex addition
	 */
	public void testRecreateHierarchy2() {
		System.err.println("testRecreateHierarchy2");

		// detach: avoid event-based notification, and go manual
		tg.removeStructureChangeListener(ch);

		StructureChangeEvent sce = new StructureChangeEvent(tg);
		Object v10 = "10";
		sce.getAddedVertices().add(v10);
		sce.getRemovedEdges().add(e34);
		sce.getAddedEdges().add(new Edge(v7, v4));
		sce.getAddedEdges().add(new Edge(v4, v10));
		tg.structureChangePerformed(sce);

		BaseGraph base = tg;
		Object rootVertex = v1;
		Cluster oldRoot = r1;
		HierarchyChangeEvent hce = new HierarchyChangeEvent(ch,
				"empty test change");

		SimpleRuleClusterer instance = new SimpleRuleClusterer();

		Cluster result = instance.recreateHierarchy(base, rootVertex, sce, hce);
		System.err.println("hce2 = " + hce.getDescription());

		// expected output is "add {4.5.6.7.8.9.10} remove {7.8.9} change {4.5.6.10}, ..."
		assertEquals(1, hce.getRemovedClusters().size());
		assertEquals(2, hce.getRemovedClusters().values().iterator().next()
				.size());

		assertEquals(2, hce.getAddedClusters().size());
		// one should be {10.4.5.6.7.8.9}, the other {10}

		assertEquals(1, hce.getRemovedEdges().size());
		assertEquals(2, hce.getAddedEdges().size());

		// would contain the '10' if it had already been added... not the case
		String[] expected = new String[] { "{4.5.6}", "{1.2.3.4.5.6.7.8.9}",
				"{3.4.5.6.7.8.9}" };
		assertTrue(Utils.checkSameClusters(expected, hce.getChangedClusters(),
				tg));
	}

	/**
	 * Test of recreateHierarchy method, of class eps.clover.model.SimpleRuleClusterer.
	 *
	 * test vertex addition
	 */
	public void testRecreateHierarchyRemoveV3() {
		System.err.println("testRecreateHierarchyRemoveV3");

		// detach: avoid event-based notification, and go manual
		tg.removeStructureChangeListener(ch);

		StructureChangeEvent sce = new StructureChangeEvent(tg);
		sce.getRemovedVertices().add(Utils.getVertexForId("3", tg));
		tg.structureChangePerformed(sce);

		BaseGraph base = tg;
		Object rootVertex = v1;
		Cluster oldRoot = r1;
		HierarchyChangeEvent hce = new HierarchyChangeEvent(ch,
				"empty test change");

		SimpleRuleClusterer instance = new SimpleRuleClusterer();

		Cluster result = instance.recreateHierarchy(base, rootVertex, sce, hce);
		System.err.println("hceRV3 = " + hce.getDescription());

		// expected output is "add {1.2} {7.8.9} {4.5.6} 
		// remove {3.4.5.6.7.8.9} {1} {2} (because 1 and 2 go to {1,2})
		// change {1.2.3.4.5.6.7.8.9}

		assertEquals(1, hce.getRemovedClusters().size());
		assertEquals(3, hce.getRemovedClusters().values().iterator().next()
				.size());
		assertEquals(1, hce.getAddedClusters().size());
		assertEquals(3, hce.getAddedClusters().values().iterator().next()
				.size());
		assertEquals(0, hce.getRemovedEdges().size());
		assertEquals(0, hce.getAddedEdges().size());

		// would contain the '10' if it had already been added... not the case
		String[] expected = new String[] { "{1.2.3.4.5.6.7.8.9}" };
		assertTrue(Utils.checkSameClusters(expected, hce.getChangedClusters(),
				tg));
	}

	/**
	 * Test of recreateHierarchy method, of class eps.clover.model.SimpleRuleClusterer.
	 *
	 * Test removal of a vertex
	 */
	public void testRecreateHierarchy3() {

		System.err.println("testRecreateHierarchy3");

		// detach: avoid event-based notification, and go manual
		tg.removeStructureChangeListener(ch);

		StructureChangeEvent sce = new StructureChangeEvent(tg);
		sce.getRemovedVertices().add(v4);
		tg.structureChangePerformed(sce);

		BaseGraph base = tg;
		Object rootVertex = v1;
		Cluster oldRoot = r1;
		HierarchyChangeEvent hce = new HierarchyChangeEvent(ch,
				"empty test change");

		SimpleRuleClusterer instance = new SimpleRuleClusterer();

		Cluster c456 = r1.getLastClusterFor(v4).getParentCluster();
		Cluster result = instance.recreateHierarchy(base, rootVertex, sce, hce);

		System.err.println("hce3 = " + hce.getDescription());

		// removes {3.4.5.6.7.8.9} ({4} and {4.5.6} are implicit)
		assertEquals(1, hce.getRemovedClusters().size());

		// adds {1.2.3.7.8.9}, only
		assertEquals(1, hce.getAddedClusters().size());
		assertEquals("{1.2.3.7.8.9}", hce.getAddedClusters().values()
				.iterator().next().get(0).getListing(tg));

		// there should be no removed edges
		assertEquals(0, hce.getRemovedEdges().size());

		// and remember that '4' won't dissapear until the change is performed
		String[] expected = new String[] { "{1.2.3.4.5.6.7.8.9}" };

		assertTrue(Utils.checkSameClusters(expected, hce.getChangedClusters(),
				tg));
		assertTrue(hce.getChangedClusters().contains(r1));
	}

	/**
	 * Test of recreateHierarchy method, of class eps.clover.model.SimpleRuleClusterer.
	 *
	 * Test removal of a vertex
	 */
	public void testRecreateHierarchy4() {

		System.err.println("testRecreateHierarchy4");

		// detach: avoid event-based notification, and go manual
		tg.removeStructureChangeListener(ch);

		StructureChangeEvent sce = new StructureChangeEvent(tg);
		sce.getRemovedVertices().add(v5);
		tg.structureChangePerformed(sce);

		BaseGraph base = tg;
		Object rootVertex = v1;
		Cluster oldRoot = r1;
		HierarchyChangeEvent hce = new HierarchyChangeEvent(ch,
				"empty test change");

		SimpleRuleClusterer instance = new SimpleRuleClusterer();

		Cluster result = instance.recreateHierarchy(base, rootVertex, sce, hce);
		System.err.println("hce4 = " + hce.getDescription());

		// removes {5}
		assertEquals(1, hce.getRemovedClusters().size());
		assertEquals(oldRoot.getLastClusterFor(v5).getParentCluster(), hce
				.getRemovedClusters().keySet().iterator().next());

		assertEquals(0, hce.getAddedClusters().size());
		assertEquals(0, hce.getRemovedEdges().size());

		// remember, remember, the 5th of november stays there until changed
		String[] expected = new String[] { "{1.2.3.4.5.6.7.8.9}",
				"{3.4.5.6.7.8.9}", "{4.5.6}" };

		assertTrue(Utils.checkSameClusters(expected, hce.getChangedClusters(),
				tg));
		for (ArrayList<Cluster> l : hce.getAddedClusters().values()) {
			assertTrue(Utils.checkSameRoot(r1, l));
		}
	}

	public void testRecreateHierarchy2_1() {

		System.err.println("testRecreateHierarchy2_1");

		tg = new TestGraph("([0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11], "
				+ "[{0,1}, {1,2}, {2,3}, {3,4}, {4,5}, {6,7}, "
				+ " {7,11}, {7,8}, {8,9}, {0,9}, {6,8}, {3,7}, {1,9}, {5,10}])");
		ch = new ClusterHierarchy(tg, "0", src);
		r1 = ch.getRoot();
		Object v2 = null, v0 = null;
		for (Object o : tg.vertexSet()) {
			if (o.toString().equals("2"))
				v2 = o;
			if (o.toString().equals("0"))
				v0 = o;
		}

		tg.removeStructureChangeListener(ch);
		StructureChangeEvent sce = new StructureChangeEvent(tg);
		sce.getRemovedVertices().add(v2);
		tg.structureChangePerformed(sce);

		System.err.println("sce2_1 = " + sce.getDescription());

		BaseGraph base = tg;
		Object rootVertex = v0;
		Cluster oldRoot = r1;
		HierarchyChangeEvent hce = new HierarchyChangeEvent(ch,
				"empty test change");

		SimpleRuleClusterer instance = new SimpleRuleClusterer();

		Cluster result = instance.recreateHierarchy(base, rootVertex, sce, hce);

		System.err.println("hce2_1 = " + hce.getDescription());

		// removes {5}; but much bigger will disappear: {1.10.11.2.3.4.5.6.7.8.9}
		assertEquals(1, hce.getRemovedClusters().size());
		// adds another biggie: {0.1.10.11.2.3.4.5.6.7.8.9}
		assertEquals(1, hce.getAddedClusters().size());
		assertEquals(0, hce.getRemovedEdges().size());

		String[] expected = new String[] { "{0.1.10.11.2.3.4.5.6.7.8.9}" };

		assertTrue(Utils.checkSameClusters(expected, hce.getChangedClusters(),
				tg));
	}
}
