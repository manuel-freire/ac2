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
 * DepthFirstIterator.java
 *
 * Created on May 17, 2006, 8:58 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import org.jgrapht.DirectedGraph;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A DepthFirstIterator that is not cross-component, is start-vertex enabled,
 * uses directed edges, and provides a hook for "vertexFinished". Besides
 * the usual "vertexStarted" and "vertexEnded"
 *
 * @author mfreire
 */
public class DepthFirstIterator implements Iterator {

	private Log log = LogFactory.getLog(DepthFirstIterator.class);

	private DirectedGraph g;
	private HashSet visited;
	private Stack<Iterator> notFinished;
	private Stack finished;
	private Object current;

	/**
	 * Creates a new instance of DepthFirstIterator
	 */
	public DepthFirstIterator(DirectedGraph g, Object start) {
		this.g = g;
		notFinished = new Stack<Iterator>();
		finished = new Stack();
		visited = new HashSet();

		encounterVertex(start, null);
		visited.add(start);
		finished.push(start);
		notFinished.push(g.outgoingEdgesOf(start).iterator());
		current = start;
	}

	private void findNext() {

		// take next unvisited node from stack
		while (!notFinished.isEmpty()) {

			Iterator i = notFinished.peek();
			while (i.hasNext()) {
				Edge e = (Edge) i.next();
				Object o = e.getTarget();
				if (visited.contains(o)) {
					encounterVertexAgain(o, e);
				} else {
					encounterVertex(o, e);
					visited.add(o);
					finished.push(o);
					notFinished.push(g.outgoingEdgesOf(o).iterator());
					current = o;
					return;
				}
			}

			// source vertex in iterator is now exhausted
			leaveVertex(finished.pop());
			notFinished.pop();
		}

		current = null;
	}

	/**
	 * Called when a vertex is located visited for the first time
	 */
	protected void encounterVertex(Object vertex, Edge e) {
		log.debug("Encountered " + vertex + " following " + e);
	}

	/**
	 * Called when a vertex is visited again
	 */
	protected void encounterVertexAgain(Object vertex, Edge e) {
		log.debug("Encountered " + vertex + " AGAIN following " + e);
	}

	/**
	 * Called when all this vertice's children have been visited
	 */
	protected void leaveVertex(Object vertex) {
		log.debug("Left " + vertex + " forever");
	}

	public boolean hasNext() {
		return current != null;
	}

	public Object next() {
		Object o = current;
		findNext();
		return o;
	}

	public void remove() {
		throw new UnsupportedOperationException("Remove not supported");
	}
}
