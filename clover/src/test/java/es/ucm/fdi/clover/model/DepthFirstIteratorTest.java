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
 * DepthFirstIteratorTest.java
 * JUnit based test
 *
 * Created on July 14, 2006, 11:28 AM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.model;

import es.ucm.fdi.clover.test.TestGraph;
import junit.framework.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import org.jgrapht.DirectedGraph;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgrapht.Graphs;

/**
 *
 * @author mfreire
 */
public class DepthFirstIteratorTest extends TestCase {

	private TestGraph tg;
	private Object first;

	public DepthFirstIteratorTest(String testName) {
		super(testName);
	}

	protected void setUp() throws Exception {
		/*
		 * Test graph to use is 
		 *
		 *      1
		 *  2      5
		 * 3 4->  6 7
		 *
		 * And of course, expected outcome is 1234567
		 */
		tg = new TestGraph("([1, 2, 3, 4, 5, 6, 7], "
				+ "[{1,2}, {2,3}, {2,4}, {1,5}, {5,6}, 5,7}])");
		first = "1";
	}

	protected void tearDown() throws Exception {
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(DepthFirstIteratorTest.class);

		return suite;
	}

	/**
	 * Test of hasNext method, of class eps.clover.model.DepthFirstIterator.
	 *
	 * tests that all elements are found, in correct order
	 */
	public void testHasNext() {

		DepthFirstIterator instance = new DepthFirstIterator(tg, first);

		int prev = 0, current;
		for (Object v : tg.vertexSet()) {
			boolean expResult = true;
			assertTrue(instance.hasNext());
			current = Integer.parseInt((String) instance.next());
			assertTrue(current > prev);
			prev = current;
		}
		assertFalse(instance.hasNext());
	}

	/**
	 * Test of remove method, of class eps.clover.model.DepthFirstIterator.
	 */
	public void testRemove() {

		DepthFirstIterator instance = new DepthFirstIterator(tg, first);

		boolean exception = false;
		instance.next();
		try {
			instance.remove();
		} catch (Exception e) {
			exception = true;
		}

		if (!exception) {
			fail("Expected exception on removal from DepthFirstIterator");
		}
	}
}
