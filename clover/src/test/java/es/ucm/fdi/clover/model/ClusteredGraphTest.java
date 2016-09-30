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
 * ClusteredGraphTest.java
 * JUnit based test
 *
 * Created on July 14, 2006, 11:47 AM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.model;

import es.ucm.fdi.clover.event.ClusteringChangeEvent;
import es.ucm.fdi.clover.event.HierarchyChangeEvent;
import es.ucm.fdi.clover.event.HierarchyChangeListener;
import es.ucm.fdi.clover.event.StructureChangeEvent;
import es.ucm.fdi.clover.event.StructureChangeListener;
import es.ucm.fdi.clover.test.TestGraph;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author mfreire
 */
public class ClusteredGraphTest extends TestCase {

	private TestGraph tg;
	private ClusterHierarchy ch;
	private SimpleRuleClusterer src;
	private Object v1, v3, v4, v5, v7;
	private Edge e34;
	private StoolPigeon stoolPigeon;

	private StructureChangeEvent sce;

	public ClusteredGraphTest(String testName) {
		super(testName);
	}

	@Override
	protected void setUp() throws Exception {

		tg = new TestGraph("([1,2,3,4,5,6,7,8,9], "
				+ "[{1,2},{1,3},{3,4},{4,5},{4,6},{3,7},{7,9},{7,8}])");

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
			} else if (v.toString().equals("7"))
				v7 = v;
			else if (v.toString().equals("5"))
				v5 = v;
			else if (v.toString().equals("1"))
				v1 = v;
		}

		stoolPigeon = new StoolPigeon();
		ch.addHierarchyChangeListener(stoolPigeon);

		sce = new StructureChangeEvent(tg);
	}

	@Override
	protected void tearDown() throws Exception {
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(ClusteredGraphTest.class);

		return suite;
	}

	/**
	 * Test of hierarchyChangePerformed method, 
	 * of class eps.clover.model.ClusteredGraph.
	 */
	public void testHierarchyChangePerformed_simpleAdd() {

		System.out.println("testHierarchyChangePerformed0");

		ClusteredGraph instance = new ClusteredGraph(ch);
		instance
				.clusteringChangePerformed(instance.createMakeVisibleEvent("8"));
		assertTrue(Utils.checkSameRoot(ch.getRoot(), instance.getSlice()));

		System.out.println("before:");
		for (Cluster c : instance.getSlice()) {
			System.out.println("\t" + c + " " + instance.getId(c.getVertex()));
		}

		// idea is to add a 'vertex 10' to 8, united with an edge
		sce = new StructureChangeEvent(tg);
		tg.addAnotherOne(10, 8, sce);
		System.out.println("simple add sce = " + sce.getDescription());
		tg.structureChangePerformed(sce);

		// ok, now everything should have changed a teensy bit:
		System.out.println(stoolPigeon.hce.getDescription());

		System.out.println("AFTER:");
		for (Cluster c : instance.getSlice()) {
			System.out.println("\t" + c + " " + instance.getId(c.getVertex()));
		}

		// check expected clusters there
		String[] expected = new String[] { "{1}", "{2}", "{3}", "{4.5.6}",
				"{7}", "{8}", "{9}", "{10}" };
		assertTrue(Utils.checkSameClusters(expected, instance.getSlice(), tg));

		// check edge between 7 and 8 exists
		assertTrue(instance.containsEdge(Utils.getVertexForId("8", instance),
				Utils.getVertexForId("10", instance)));
	}

	/**
	 * Test of structureChangePerformed method, of class eps.clover.model.ClusterHierarchy.
	 */
	public void testHierarchyChangePerformed_newCourseVariant() {
		System.out.println("structureChangePerformed");

		// build initial clustering
		Object aVertex = null;
		TestGraph tg2 = new TestGraph("[0, 1, 2, 3], [{0,1}, {1,2}, {1,3}]");
		HashMap<String, Cluster> clusters = new HashMap<String, Cluster>();
		for (Object o : tg2.vertexSet()) {
			clusters.put("" + o, new Cluster(tg2, o));
			aVertex = o;
		}
		clusters.put("123", new Cluster());
		clusters.get("123").add(clusters.get("1"));
		clusters.get("123").add(clusters.get("2"));
		clusters.get("123").add(clusters.get("3"));
		clusters.put("0123", new Cluster());
		clusters.get("0123").add(clusters.get("0"));
		clusters.get("0123").add(clusters.get("123"));

		StructureChangeEvent sce2 = new StructureChangeEvent(tg2);
		tg2.addAnotherOne(4, 3, sce2);
		tg2.structureChangePerformed(sce2);

		// build final clustering
		HashMap<String, Cluster> next = new HashMap<String, Cluster>();
		for (Object o : tg2.vertexSet()) {
			next.put("" + o, new Cluster(tg2, o));
		}
		next.put("123", new Cluster());
		next.get("123").add(next.get("1"));
		next.get("123").add(next.get("2"));
		next.get("123").add(next.get("3"));
		next.put("1234", new Cluster());
		next.get("1234").add(next.get("4"));
		next.get("1234").add(next.get("123"));
		next.put("01234", new Cluster());
		next.get("01234").add(next.get("1234"));
		next.get("01234").add(next.get("0"));

		// build CH and launch event
		ClusterHierarchy.TestEngine engine = new ClusterHierarchy.TestEngine(
				clusters.get("0123"), next.get("01234"));
		ClusterHierarchy ch2 = new ClusterHierarchy(tg2, aVertex, engine);
		ch2.structureChangePerformed(sce2);

		/*
		 * Added: 
		 * at 19479646 {0.1.2.3} 19479646
		 *         20955026 {1.2.3.4} 3816479_20955026
		 * Removed: 
		 * at 19479646 {0.1.2.3} 19479646
		 *         23788451 {1.2.3} 19479646_23788451
		 */
		HashMap<Cluster, ArrayList<Cluster>> result;
		result = engine.change.getAddedClusters();
		assertTrue(result.containsKey(clusters.get("0123")));
		assertTrue(result.get(clusters.get("0123")).contains(next.get("1234")));
		assertTrue(result.size() == 1);
		result = engine.change.getRemovedClusters();
		assertTrue(result.containsKey(clusters.get("0123")));
		assertTrue(result.get(clusters.get("0123")).contains(
				clusters.get("123")));
		assertTrue(result.size() == 1);
	}

	/**
	 * Test of hierarchyChangePerformed method, 
	 * of class eps.clover.model.ClusteredGraph.
	 */
	public void testHierarchyChangePerformed0() {

		System.out.println("testHierarchyChangePerformed0");

		ClusteredGraph instance = new ClusteredGraph(ch);
		instance
				.clusteringChangePerformed(instance.createMakeVisibleEvent("8"));
		instance
				.clusteringChangePerformed(instance.createMakeVisibleEvent("6"));
		// now, all vertices should be visible    
		assertTrue(Utils.checkSameRoot(ch.getRoot(), instance.getSlice()));

		System.out.println("before:");
		for (Cluster c : instance.getSlice()) {
			System.out.println("\t" + c + " " + instance.getId(c.getVertex()));
		}

		// idea is to move 8 from 789 to 456, and find an edge from 79 to 4568
		HierarchyChangeEvent hce = new HierarchyChangeEvent(ch, "testing...");
		Cluster c8 = ch.getRoot().clusterForVertex(
				Utils.getVertexForId("8", tg));
		Cluster c456 = Utils.getClusterForId("{4}", instance)
				.getParentCluster();
		hce.insertRemovedCluster(c8.getParentCluster(), c8);
		hce.insertAddedCluster(c456, c8);
		hce.augmentChangesWithAddedAndRemoved();
		ch.hierarchyChangePerformed(hce);

		// ok, now everything should have changed a teensy bit:
		System.out.println(stoolPigeon.hce.getDescription());

		System.out.println("AFTER:");
		for (Cluster c : instance.getSlice()) {
			System.out.println("\t" + c + " " + instance.getId(c.getVertex()));
		}

		// check expected clusters there
		String[] expected = new String[] { "{1}", "{2}", "{3}", "{4}", "{5}",
				"{6}", "{7}", "{8}", "{9}" };
		assertTrue(Utils.checkSameClusters(expected, instance.getSlice(), tg));

		// check edge between 7 and 8 exists
		assertTrue(instance.containsEdge(Utils.getVertexForId("7", instance),
				c8.getVertex()));
	}

	/**
	 * Test of hierarchyChangePerformed method, 
	 * of class eps.clover.model.ClusteredGraph.
	 */
	public void testHierarchyChangePerformed1() {

		System.out.println("testHierarchyChangePerformed1");

		ClusteredGraph instance = new ClusteredGraph(ch);
		instance
				.clusteringChangePerformed(instance.createMakeVisibleEvent("3"));
		// now, 1, 2, 3, 456, and 567 should be visible       
		assertTrue(Utils.checkSameRoot(ch.getRoot(), instance.getSlice()));

		System.out.println("before:");
		for (Cluster c : instance.getSlice()) {
			System.out.println("\t" + c + " " + instance.getId(c.getVertex()));
		}

		// idea is to move 8 from 789 to 456, and find an edge from 79 to 4568
		HierarchyChangeEvent hce = new HierarchyChangeEvent(ch, "testing...");
		Cluster c8 = ch.getRoot().clusterForVertex(
				Utils.getVertexForId("8", tg));
		Cluster c456 = Utils.getClusterForId("{4.5.6}", instance);
		hce.insertRemovedCluster(c8.getParentCluster(), c8);
		hce.insertAddedCluster(c456, c8);
		hce.augmentChangesWithAddedAndRemoved();
		ch.hierarchyChangePerformed(hce);

		// ok, now everything should have changed a teensy bit:
		System.out.println(stoolPigeon.hce.getDescription());

		System.out.println("AFTER:");
		for (Cluster c : instance.getSlice()) {
			System.out.println("\t" + c + " " + instance.getId(c.getVertex()));
		}

		// check expected clusters there
		String[] expected = new String[] { "{1}", "{2}", "{3}", "{4.5.6.8}",
				"{7.9}" };
		assertTrue(Utils.checkSameClusters(expected, instance.getSlice(), tg));

		// check edge between 79 and 4568 exists
		assertTrue(instance.containsEdge(Utils
				.getVertexForId("{7.9}", instance), c456.getVertex()));
	}

	/**
	 * Test of hierarchyChangePerformed method, 
	 * of class eps.clover.model.ClusteredGraph.
	 */
	public void testHierarchyChangePerformed2() {

		System.out.println("testHierarchyChangePerformed2");

		ClusteredGraph instance = new ClusteredGraph(ch);
		instance
				.clusteringChangePerformed(instance.createMakeVisibleEvent("3"));
		// now, 1, 2, 3, 456, and 567 should be visible       
		assertTrue(Utils.checkSameRoot(ch.getRoot(), instance.getSlice()));

		System.out.println("before:");
		for (Cluster c : instance.getSlice()) {
			System.out.println("\t" + c + " " + instance.getId(c.getVertex()));
		}

		// idea is to return 8 to its previous place; everything back to normal?
		HierarchyChangeEvent hce = new HierarchyChangeEvent(ch, "testing...");
		Cluster c8 = ch.getRoot().clusterForVertex(
				Utils.getVertexForId("8", tg));
		Cluster c456 = Utils.getClusterForId("{4.5.6}", instance);
		Cluster c789 = Utils.getClusterForId("{7.8.9}", instance);
		hce.insertRemovedCluster(c8.getParentCluster(), c8);
		hce.insertAddedCluster(c456, c8);
		hce.augmentChangesWithAddedAndRemoved();
		ch.hierarchyChangePerformed(hce);

		System.out.println("after1:");
		for (Cluster c : instance.getSlice()) {
			System.out.println("\t" + c + " " + instance.getId(c.getVertex()));
		}

		hce = new HierarchyChangeEvent(ch, "testing bis...");
		hce.insertRemovedCluster(c8.getParentCluster(), c8);
		hce.insertAddedCluster(c789, c8);
		hce.augmentChangesWithAddedAndRemoved();
		ch.hierarchyChangePerformed(hce);

		System.out.println("after2:");
		for (Cluster c : instance.getSlice()) {
			System.out.println("\t" + c + " " + instance.getId(c.getVertex()));
		}

		// check expected clusters there
		String[] expected = new String[] { "{1}", "{2}", "{3}", "{4.5.6}",
				"{7.8.9}" };
		assertTrue(Utils.checkSameClusters(expected, instance.getSlice(), tg));

		System.out
				.println(instance.getEdge(c789.getVertex(), c456.getVertex()));

		// check edge between 879 and 456 has disapeared
		assertTrue(!instance.containsEdge(c789.getVertex(), c456.getVertex()));
	}

	/**
	 * Test of hierarchyChangePerformed method, 
	 * of class eps.clover.model.ClusteredGraph.
	 */
	public void testHierarchyChangePerformed3() {

		System.out.println("testHierarchyChangePerformed2");

		ClusteredGraph instance = new ClusteredGraph(ch);
		instance
				.clusteringChangePerformed(instance.createMakeVisibleEvent("3"));
		// now, 12, 456, and 567 should be visible; no edges at all
		assertTrue(Utils.checkSameRoot(ch.getRoot(), instance.getSlice()));

		System.out.println("before:");
		for (Cluster c : instance.getSlice()) {
			System.out.println("\t" + c + " " + instance.getId(c.getVertex()));
		}

		// idea is to remove 3; result should be 1, 2, 456, 789
		sce.getRemovedVertices().add(Utils.getVertexForId("3", tg));
		tg.structureChangePerformed(sce);

		// ok, now everything should have changed a teensy bit:
		System.out.println(stoolPigeon.hce.getDescription());
		instance.clusteringChangePerformed(instance
				.createMakeVisibleEvent(((Cluster) ch.getRoot().getChildAt(0))
						.getVertex()));

		// check expected clusters there
		String[] expected = new String[] { "{1.2}", "{4.5.6}", "{7.8.9}" };
		assertTrue(Utils.checkSameClusters(expected, instance.getSlice(), tg));

		// check no edges at all
		assertTrue(instance.edgeSet().isEmpty());
	}

	public static class StoolPigeon implements HierarchyChangeListener,
			StructureChangeListener {
		public HierarchyChangeEvent hce;
		public StructureChangeEvent sce;

		public void hierarchyChangePerformed(HierarchyChangeEvent evt) {
			this.hce = evt;
		}

		public void structureChangePerformed(StructureChangeEvent evt) {
			System.out.println(">>> " + evt.getDescription());
			this.sce = evt;
		}
	}

	//    /**
	//     * Test of clusteringChangePerformed method, of class eps.clover.model.ClusteredGraph.
	//     */
	//    public void testClusteringChangePerformed() {
	//        System.out.println("clusteringChangePerformed");
	//        
	//        ClusteringChangeEvent evt = null;
	//        ClusteredGraph instance = null;
	//        
	//        instance.clusteringChangePerformed(evt);
	//        
	//        // TODO review the generated test code and remove the default call to fail.
	//        fail("The test case is a prototype.");
	//    }

	//    /**
	//     * Test of setHierarchy method, of class eps.clover.model.ClusteredGraph.
	//     */
	//    public void testSetHierarchy() {
	//        System.out.println("setHierarchy");
	//        
	//        ClusterHierarchy hierarchy = null;
	//        ClusteredGraph instance = null;
	//        
	//        instance.setHierarchy(hierarchy);
	//        
	//        // TODO review the generated test code and remove the default call to fail.
	//        fail("The test case is a prototype.");
	//    }

	//    /**
	//     * Test of prepareCollapse method, of class eps.clover.model.ClusteredGraph.
	//     */
	//    public void testPrepareCollapse() {
	//        System.out.println("prepareCollapse");
	//        
	//        Cluster c = null;
	//        StructureChangeEvent sce = null;
	//        ClusteredGraph instance = null;
	//        
	//        instance.prepareCollapse(c, sce);
	//        
	//        // TODO review the generated test code and remove the default call to fail.
	//        fail("The test case is a prototype.");
	//    }

	//    /**
	//     * Test of prepareExpand method, of class eps.clover.model.ClusteredGraph.
	//     */
	//    public void testPrepareExpand() {
	//        System.out.println("prepareExpand");
	//        
	//        Cluster c = null;
	//        StructureChangeEvent sce = null;
	//        ClusteredGraph instance = null;
	//        
	//        instance.prepareExpand(c, sce);
	//        
	//        // TODO review the generated test code and remove the default call to fail.
	//        fail("The test case is a prototype.");
	//    }
	//
	//    /**
	//     * Test of createCollapseEvent method, of class eps.clover.model.ClusteredGraph.
	//     */
	//    public void testCreateCollapseEvent() {
	//        System.out.println("createCollapseEvent");
	//        
	//        Object v = null;
	//        ClusteredGraph instance = null;
	//        
	//        ClusteringChangeEvent expResult = null;
	//        ClusteringChangeEvent result = instance.createCollapseEvent(v);
	//        assertEquals(expResult, result);
	//        
	//        // TODO review the generated test code and remove the default call to fail.
	//        fail("The test case is a prototype.");
	//    }

	//    /**
	//     * Test of createExpandEvent method, of class eps.clover.model.ClusteredGraph.
	//     */
	//    public void testCreateExpandEvent() {
	//        System.out.println("createExpandEvent");
	//        
	//        Object v = null;
	//        ClusteredGraph instance = null;
	//        
	//        ClusteringChangeEvent expResult = null;
	//        ClusteringChangeEvent result = instance.createExpandEvent(v);
	//        assertEquals(expResult, result);
	//        
	//        // TODO review the generated test code and remove the default call to fail.
	//        fail("The test case is a prototype.");
	//    }

	//    /**
	//     * Test of createMakeVisibleEvent method, of class eps.clover.model.ClusteredGraph.
	//     */
	//    public void testCreateMakeVisibleEvent() {
	//        System.out.println("createMakeVisibleEvent");
	//        
	//        Object v = null;
	//        ClusteredGraph instance = null;
	//        
	//        ClusteringChangeEvent expResult = null;
	//        ClusteringChangeEvent result = instance.createMakeVisibleEvent(v);
	//        assertEquals(expResult, result);
	//        
	//        // TODO review the generated test code and remove the default call to fail.
	//        fail("The test case is a prototype.");
	//    }

	/**
	 * Test of createPoIChangeEvent method, of class eps.clover.model.ClusteredGraph.
	 *
	 * Start from the clustered graph, change focus from old (all cluster) 
	 * to v3, see that the focus changes accordingly
	 */
	public void testCreatePoIChangeEvent() {

		Set frozen = new HashSet();
		int focusSize = 1;
		int maxClusters = 3;
		ClusteredGraph instance = new ClusteredGraph(ch);

		System.out.println("Starting POI change...");

		ClusteringChangeEvent expResult = null;
		ClusteringChangeEvent result = instance.createPoIChangeEvent(v3,
				frozen, focusSize, maxClusters);
		instance.clusteringChangePerformed(result);

		// expected output slice
		String[] expected = new String[] { "{7.8.9}", "{4.5.6}", "{1}", "{2}",
				"{3}" };

		// check to see if they're equal
		System.out.println("Got: ");
		for (Cluster c : instance.getSlice()) {
			System.out.print(" " + c.getListing(tg));
		}
		System.out.println("\n");

		assertEquals(expected.length, instance.getSlice().size());
		for (Cluster c : instance.getSlice()) {
			boolean found = false;
			for (String s : expected) {
				if (s.equals(c.getListing(tg))) {
					found = true;
				}
			}
			assertTrue(found);
		}
	}
}
