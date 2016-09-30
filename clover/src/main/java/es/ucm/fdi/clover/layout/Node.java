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
 * Node.java
 *
 * Created on May 17, 2006, 12:41 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.layout;

import es.ucm.fdi.clover.model.ViewGraph;
import es.ucm.fdi.clover.model.Edge;

import es.ucm.fdi.clover.view.BaseView;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.jgrapht.Graph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgraph.graph.CellView;
import org.jgraph.graph.GraphCell;
import org.jgraph.graph.GraphConstants;

/**
 * Builds and maintains simple, array-ized representations of nodes for layout
 * purposes. Nodes need not be related to vertices (they can represent components,
 * for instance).
 *
 * Note that all node coordinates are layout-coordinates (which may differ
 * from actual view-coordinates by the layoutZoom factor).
 *
 * @author mfreire
 */
public class Node {

	public float x, y, x0, y0, w, h, dx, dy;
	public int component;
	public int[] edges;
	public float[] strengths;
	public boolean frozen;

	public Object peer;

	public void sync(Rectangle2D r, int comp, boolean frozen) {
		sync(r, comp, frozen, 1.0);
	}

	/**
	 * @param layoutZoom - the degree of zoom that is being used in the layout
	 */
	public void sync(Rectangle2D r, int comp, boolean frozen, double layoutZoom) {
		x0 = (float) (r.getX() / layoutZoom);
		y0 = (float) (r.getY() / layoutZoom);
		w = (float) r.getWidth();
		h = (float) r.getHeight();
		x = x0 + w / 2;
		y = y0 + h / 2;
		this.frozen = frozen;
		dx = 0;
		dy = 0;
		component = (comp == -1) ? 0 : comp;
	}

	public Node(Object o, Rectangle2D bounds, int component, boolean frozen) {
		this.peer = o;
		if (bounds != null)
			sync(bounds, component, frozen);
	}

	public Node(Object o, Rectangle2D bounds, int component, boolean frozen,
			double layoutZoom) {
		this.peer = o;
		if (bounds != null)
			sync(bounds, component, frozen, layoutZoom);
	}

	public String toString() {
		return "(" + x + "," + y + " " + w + "x" + h + ") - [" + dx + " " + dy
				+ "]";
	}

	public void sync(String s) {
		StringTokenizer st = new StringTokenizer(s, "(, x)-[]");
		x = Float.parseFloat(st.nextToken());
		y = Float.parseFloat(st.nextToken());
		w = Float.parseFloat(st.nextToken());
		h = Float.parseFloat(st.nextToken());
		dx = Float.parseFloat(st.nextToken());
		dy = Float.parseFloat(st.nextToken());
		x0 = x - w / 2;
		y0 = y - h / 2;
	}

	public Rectangle2D getBounds() {
		return getBounds(new Rectangle2D.Float());
	}

	public Rectangle2D getBounds(Rectangle2D r) {
		r.setFrame(x0, y0, w, h);
		return r;
	}

	/**
	 * returns the layout-center of masses of a series of Point2Ds identified by
	 * their keys (graph vertices)
	 */
	public static Point2D getCenterCoords(Map<Object, Rectangle2D> map,
			Collection vertices) {
		Point2D center = new Point2D.Float();
		Rectangle2D r;
		for (Object o : vertices) {
			r = map.get(o);
			if (r == null) {
				System.err.println("CenterCoords: no rectangle for " + o);
				continue; // vertex does not affect rectangle...
			}
			center.setLocation(center.getX() + r.getCenterX(), center.getY()
					+ r.getCenterY());
		}
		center.setLocation(center.getX() / vertices.size(), center.getY()
				/ vertices.size());
		return center;
	}

	/**
	 * Builds the simplified representation from the complex one
	 */
	public static Node[] loadNodes(BaseView view) {
		return loadNodes(null, view, null);
	}

	public static Node[] loadNodes(Node[] N, BaseView view) {
		return loadNodes(N, view, null);
	}

	public static Node[] loadNodes(Node[] N, BaseView view, Node[] oldBounds) {
		ConnectivityInspector ci = view.getViewGraph()
				.getConnectivityInspector();
		return loadNodes(N, view.getViewGraph(), view, view.getViewGraph()
				.getConnectivityInspector(), oldBounds);
	}

	/**
	 * g must be a subgraph of view
	 */
	public static Node[] loadNodes(Node[] N, Graph g, BaseView view,
			ConnectivityInspector ci, Node[] oldBounds) {

		double layoutZoom = view.getLayoutZoom();

		// find connectivity inspector
		if (ci == null) {
			ci = new ConnectivityInspector((UndirectedGraph) g);
		}

		// init nodes array
		if (N == null || N.length != g.vertexSet().size()) {
			N = new Node[g.vertexSet().size()];
		}

		// if old bounds exist, load a hashmap with them
		HashMap<Object, Rectangle2D> prevBounds = null;
		if (oldBounds != null) {
			prevBounds = new HashMap<Object, Rectangle2D>();
			for (Node n : oldBounds) {
				prevBounds.put(n.peer, new Rectangle2D.Float(n.x0, n.y0, n.w,
						n.h));
			}
		}

		// load hashmap with node->num mapping
		HashMap<Object, Integer> objectToNum = new HashMap<Object, Integer>();
		Iterator vertexIt = g.vertexSet().iterator();
		for (int i = 0; i < N.length; i++) {
			Object o = vertexIt.next();
			Object c = view.getViewGraph().getModelAdapter().getVertexCell(o);
			CellView v = view.getGraphLayoutCache().getMapping(c, true);

			Rectangle2D bounds;
			if (prevBounds != null && prevBounds.containsKey(v)) {
				bounds = prevBounds.get(v);
				N[i] = new Node(v, bounds, 0, false); //FIXME: fix broken freeze
			} else {
				bounds = v.getBounds();
				N[i] = new Node(v, bounds, 0, false, layoutZoom); //FIXME: fix broken freeze
			}
			objectToNum.put(o, new Integer(i));
		}

		// connectivity
		int component = 0;
		for (Set s : (List<Set>) ci.connectedSets()) {
			for (Object o : s) {
				int j = objectToNum.get(o).intValue();
				N[j].component = component;
			}
			component++;
		}

		// edges
		vertexIt = g.vertexSet().iterator();
		for (int i = 0; i < N.length; i++) {
			Object o = vertexIt.next();
			Set edgesOfVertex = g.edgesOf(o);
			N[i].edges = new int[edgesOfVertex.size()];
			N[i].strengths = new float[N[i].edges.length];
			Iterator<Edge> edgeIt = (Iterator<Edge>) edgesOfVertex.iterator();
			for (int j = 0; edgeIt.hasNext(); j++) {
				Edge e = edgeIt.next();
				int src = ((Integer) objectToNum.get(e.getSource())).intValue();
				int dst = ((Integer) objectToNum.get(e.getTarget())).intValue();
				GraphCell c = (GraphCell) view.getViewGraph().getModelAdapter()
						.getEdgeCell(e);
				Object el = (c != null) ? c.getAttributes().get(
						ViewGraph.EDGE_LENGTH) : null;
				N[i].edges[j] = (i == src) ? dst : src;
				N[i].strengths[j] = (el != null) ? ((Float) el).floatValue()
						: 1f;
			}
		}

		return N;
	}

	/**
	 * Find the bounds for all these nodes; returns view-coordinates if
	 * layoutZoom != 1
	 */
	public static Rectangle2D getBounds(Node[] N, double layoutZoom) {
		if (N.length == 0) {
			return new Rectangle2D.Float();
		}

		Rectangle2D current = new Rectangle2D.Float();
		Rectangle2D bounds = (Rectangle2D) N[0].getBounds().clone();
		bounds.setFrame(bounds.getX() * layoutZoom, bounds.getY() * layoutZoom,
				bounds.getWidth(), bounds.getHeight());
		for (int i = 1; i < N.length; i++) {
			N[i].getBounds(current);
			current.setFrame(current.getX() * layoutZoom, current.getY()
					* layoutZoom, current.getWidth(), current.getHeight());
			bounds = bounds.createUnion(current);
		}
		return bounds;
	}

	/** 
	 * returns view-coordinates
	 */
	public static Rectangle2D getBounds(Collection<Node> nodes,
			double layoutZoom) {
		if (nodes.size() == 0) {
			return new Rectangle2D.Float();
		}

		Rectangle2D current = new Rectangle2D.Float();
		Rectangle2D bounds = (Rectangle2D) nodes.iterator().next().getBounds();
		bounds.setFrame(bounds.getX() * layoutZoom, bounds.getY() * layoutZoom,
				bounds.getWidth(), bounds.getHeight());
		for (Node n : nodes) {
			current = n.getBounds(current);
			current.setFrame(current.getX() * layoutZoom, current.getY()
					* layoutZoom, current.getWidth(), current.getHeight());
			bounds = bounds.createUnion(current);
		}
		bounds
				.setFrame(bounds.getX() * layoutZoom, bounds.getY()
						* layoutZoom, bounds.getWidth() * layoutZoom, bounds
						.getHeight()
						* layoutZoom);
		return bounds;
	}

	public static Map getChangeMap(Node[] N, int x0, int y0) {
		return getChangeMap(N, x0, y0, 1.0);
	}

	/** 
	 * returns view-coordinates
	 *
	 * Generates a jgraph changeMap that can be applied to the graph
	 * to move the nodes to their new positions. This is specially usefull
	 * within an Animator...
	 */
	public static Map getChangeMap(Node[] N, int x0, int y0, double layoutZoom) {
		Map changeMap = new HashMap();

		// create the big edit
		for (int i = 0; i < N.length; i++) {
			Rectangle bounds = new Rectangle(
					(int) ((N[i].x0 - x0) * layoutZoom),
					(int) ((N[i].y0 - y0) * layoutZoom), (int) N[i].w,
					(int) N[i].h);

			Map tempMap = new HashMap();
			GraphConstants.setBounds(tempMap, bounds);
			changeMap.put(((CellView) N[i].peer).getCell(), tempMap);
		}

		return changeMap;
	}

	/**
	 * Returns layout-positions.
	 * @return a HashMap with vertices and their 2D positions
	 */
	public static HashMap<Object, Rectangle2D> getPositions(Node[] N,
			BaseView view) {
		return getPositions(N, view, true);
	}

	/**
	 * Returns layout-positions; may have to convert from view-positions.
	 * @param copyBounds if true, bounds will be copies, not originals
	 * @return a HashMap with vertices and their internal 2D positions
	 */
	public static HashMap<Object, Rectangle2D> getPositions(Node[] N,
			BaseView view, boolean copyBounds) {

		HashMap positions = new HashMap<Object, Rectangle2D>();
		JGraphModelAdapter adapter = view.getViewGraph().getModelAdapter();

		if (view.getLayoutZoom() != 1) {
			double m = 1 / view.getLayoutZoom();
			for (int i = 0; i < N.length; i++) {
				Rectangle2D r = N[i].getBounds();
				r = (Rectangle2D) r.clone();
				r.setFrame(r.getX() * m, r.getY() * m, r.getWidth() * m, r
						.getHeight()
						* m);
				Object v = adapter.getValue(((CellView) N[i].peer).getCell());
				positions.put(v, r);
			}
		} else {
			for (int i = 0; i < N.length; i++) {
				Rectangle2D r = N[i].getBounds();
				if (copyBounds)
					r = (Rectangle2D) r.clone();
				Object v = adapter.getValue(((CellView) N[i].peer).getCell());
				positions.put(v, r);
			}
		}

		return positions;
	}

	/**
	 * Operates on layout-positions
	 * Same as setPositions, but changes the centers, not the topleft-corner,
	 * so that they match the centers of the given rectangles.
	 * @param map a vertex->2D bounds map
	 */
	public static void setCenterPositions(Map<Object, Rectangle2D> map,
			Node[] N, BaseView view) {
		JGraphModelAdapter adapter = view.getViewGraph().getModelAdapter();

		for (int i = 0; i < N.length; i++) {
			Object v = adapter.getValue(((CellView) N[i].peer).getCell());
			Rectangle2D center = map.get(v);
			if (center != null) {
				N[i].x0 += (float) (center.getCenterX() - N[i].x);
				N[i].y0 += (float) (center.getCenterY() - N[i].y);
				N[i].x = (float) center.getCenterX();
				N[i].y = (float) center.getCenterY();
			}
		}
	}

	/**
	 * Operates on layout-positions.
	 * Sets the frames of the requested vertices to the requested values
	 * @param map a vertex->2D bounds map
	 */
	public static void setPositions(Map<Object, Rectangle2D> map, Node[] N,
			BaseView view) {
		JGraphModelAdapter adapter = view.getViewGraph().getModelAdapter();

		for (int i = 0; i < N.length; i++) {
			Object v = adapter.getValue(((CellView) N[i].peer).getCell());
			Rectangle2D r = map.get(v);
			if (r != null) {
				N[i].sync(r, N[i].component, false); // FIXME: borked 'frozen' nodes'
			}
		}
	}
}
