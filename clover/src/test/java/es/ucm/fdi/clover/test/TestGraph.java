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
 * TestGraph.java 
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * Contributors: - 
 * Changelog: 
 *     18-Apr-2006: first version (mfreire)
 */

package es.ucm.fdi.clover.test;

import es.ucm.fdi.clover.model.BaseGraph;
import es.ucm.fdi.clover.model.Edge;
import es.ucm.fdi.clover.event.StructureChangeEvent;
import java.awt.Color;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import org.jgrapht.ListenableGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.DijkstraShortestPath;

import org.jgrapht.ext.*;
import org.jgrapht.graph.*;
import org.jgrapht.*;

import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.AttributeMap;

import java.util.*;

/**
 * A graph that is generated 'randomly' with a given number of vertices and edges.
 * Not very random, but good enough to test layouts with.
 *
 * @author mfreire
 */
public class TestGraph extends BaseGraph {

	private int nVertices;
	private int nEdges;
	private HashMap attribs = new HashMap();
	private boolean acyclic;

	/** Creates a new instance of a TestModelAdapter */
	public TestGraph(int nVertices, int nEdges) {
		this(nVertices, nEdges, true);
	}

	public TestGraph(int nVertices, int nEdges, boolean acyclic) {
		this.nVertices = nVertices;
		this.nEdges = nEdges;
		this.acyclic = acyclic;

		generate();
	}

	private static class TestEdge extends Edge {
		private TestEdge(Object src, Object dst) {
			super(src, dst);
		}
	}

	/**
	 * ([0, 1, 2, 3, 4, 5, 6, 7, 8, 9], 
	 * [{0,1}, {0,2}, {0,4}, {0,5}, {0,6}, {0,7}, {0,8}, {1,4}, {1,8}, {2,4}, {5,3}, {3,9}])
	 *
	 */
	public TestGraph(String s) {
		String[] nodesEdges = s.split("\\], \\[");
		StringTokenizer st;
		st = new StringTokenizer(nodesEdges[0], "([, ");
		while (st.hasMoreTokens()) {
			addVertex(st.nextToken());
		}
		st = new StringTokenizer(nodesEdges[1], "([, {})]");
		while (st.hasMoreTokens()) {
			String a = st.nextToken();
			String b = st.nextToken();
			addEdge(a, b, new TestEdge(a, b));
			//            System.err.println("added "+a+" -> "+b);
		}
		//        System.err.println("Graph is ready: "+vertexSet().size()
		//            + " vertices and "+edgeSet().size() + " edges read OK");        
	}

	public String dump() {
		StringBuffer sb = new StringBuffer("([");
		for (Object v : vertexSet()) {
			sb.append(v.toString() + ", ");
		}
		sb.replace(sb.length() - ", ".length(), sb.length(), "], [");
		for (Edge e : (Set<Edge>) edgeSet()) {
			Object v1 = e.getSource();
			Object v2 = e.getTarget();
			sb.append("{" + v1.toString() + "," + v2.toString() + "}, ");
		}
		sb.replace(sb.length() - ", ".length(), sb.length(), "])");
		return sb.toString();
	}

	public void addAnotherOne(int i, int prev, StructureChangeEvent evt) {
		int x = (int) (Math.random() * 200);
		int y = (int) (Math.random() * 200);
		int w = (int) (Math.random() * 100) + 20;
		int h = (int) (Math.random() * 100) + 20;
		Rectangle r = new Rectangle(x, y, w, h);
		//attribs.put(""+i, r); 
		evt.getAddedVertices().add("" + i);

		if (prev != -1) {
			evt.getAddedEdges().add(new TestEdge("" + prev, "" + i));
		}
	}

	public void addAnotherOne(int i, int prev) {
		int x = (int) (Math.random() * 200);
		int y = (int) (Math.random() * 200);
		int w = 15;// (int)(Math.random()*100);
		int h = 15; //(int)(Math.random()*100);
		Rectangle r = new Rectangle(x, y, w, h);
		attribs.put("" + i, r);
		addVertex("" + i);

		if (prev != -1) {
			addEdge("" + prev, "" + i, new TestEdge("" + prev, "" + i));
		}
	}

	public void generate() {

		// create vertices, ensuring 1 component
		int edgesAdded = 0;
		for (int i = 0; i < nVertices; i++) {
			addAnotherOne(i, i - 1);
			edgesAdded++;
		}

		int maxEdges = nVertices * (nVertices - 1) / 2;
		nEdges = Math.min(nEdges, maxEdges);

		CycleDetector cd = new CycleDetector(this);

		// create random edges until limit reached
		for (/**/; edgesAdded < nEdges; edgesAdded++) {
			int i, j;
			while (true) {
				i = (int) (Math.random() * nVertices);
				j = (int) (Math.random() * (nVertices - 1));
				if (i == j)
					j = nVertices - 1;

				if (!addEdge("" + i, "" + j, new TestEdge("" + i, "" + j)))
					continue;
				if (acyclic && cd.detectCycles()) {
					removeEdge("" + i, "" + j);
					continue;
				}
				break;
			}
		}

		//        System.err.println("Graph is ready: "+vertexSet().size()
		//            + " vertices and "+edgeSet().size() + " edges OK");
	}

	public String getVertexLabel(Object o) {
		return o.toString();
	}

	public String getEdgeLabel(Edge e) {
		return "" + e.getSource() + "-" + e.getTarget();
	}

	public HashMap getAttribs() {
		return attribs;
	}
}
