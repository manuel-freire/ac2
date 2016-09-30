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
 * ViewGraph.java
 *
 * Created on May 6, 2006, 8:33 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.model;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.ListenableDirectedWeightedGraph;

/**
 * This is a view for a graph. It is not subscribed to anything, but it acts
 * as a proxy for the actual BaseGraph being represented. The BaseGraph should
 * be updated via some kind of StructureChange events. The idea is to use an
 * Animator for this task, so that a change to the BaseGraph can be gradually
 * reflected on both the ViewGraph and the BaseView that it is supporting.
 *
 * @author mfreire
 */
public class ViewGraph extends ListenableDirectedWeightedGraph implements
		JGraphModelAdapter.CellFactory {

	public static final String LABEL = "clover_nodename";
	public static final String TOOLTIP = "clover_nodetooltip";
	public static final String EDGE_LENGTH = "clover_edgelength";

	/** adapter in use when interfacing with a JGraph "BaseView" */
	protected JGraphModelAdapter adapter;

	/** connectivity inspector; keeps your components always up-to-date */
	protected ConnectivityInspector connectivityInspector;

	/** the BaseGraph we are listening to (via structured changes) */
	private BaseGraph base;

	/** bounds to be used in the next vertex insert; used by specialized addVertex */
	private Rectangle2D nextBounds;

	/**
	 * Creates a new instance of ViewGraph
	 */
	public ViewGraph(BaseGraph base) {
		super(new DefaultDirectedWeightedGraph(new DefaultEdgeFactory()));
		setBase(base);
	}

	/**
	 * Returns an adapter that can be used to display this graph
	 * The adapter is subscribed to graph changes
	 */
	public JGraphModelAdapter getModelAdapter() {
		return getModelAdapter(null, null);
	}

	/**
	 * Returns an adapter that can be used to display this graph
	 * The adapter is subscribed to graph changes
	 */
	public JGraphModelAdapter getModelAdapter(AttributeMap vertexAttribs,
			AttributeMap edgeAttribs) {
		if (adapter == null) {

			if (vertexAttribs == null) {
				vertexAttribs = new AttributeMap();
				GraphConstants.setOpaque(vertexAttribs, true);
				GraphConstants.setAutoSize(vertexAttribs, true);
			}
			if (edgeAttribs == null) {
				edgeAttribs = new AttributeMap();
			}

			adapter = new JGraphModelAdapter((Graph) this, vertexAttribs,
					edgeAttribs, (JGraphModelAdapter.CellFactory) this);
		}
		return adapter;
	}

	/**
	 * Add a vertex at a desired position, instead of a random one. 
	 * This avoids "jumpy inserts".
	 */
	public void addVertex(Object v, Rectangle2D initialBounds) {
		nextBounds = initialBounds;
		addVertex(v);
	}

	/**
	 * Gets a list of all nodes
	 */
	public ArrayList getCells() {
		JGraphModelAdapter adapter = getModelAdapter();
		Iterator it = vertexSet().iterator();
		ArrayList al = new ArrayList();
		while (it.hasNext()) {
			al.add(adapter.getVertexCell(it.next()));
		}
		return al;
	}

	/**
	 * Creates and configures a JGraph edge-cell for a model edge
	 */
	public DefaultEdge createEdgeCell(Object o) {
		Edge e = (Edge) o;
		DefaultEdge de = new DefaultEdge(e);
		GraphConstants.setValue(de.getAttributes(), e);
		decorateEdgeCell(de);
		return de;
	}

	/**
	 * Creates and configures a JGraph cell for a model vertex
	 */
	public DefaultGraphCell createVertexCell(Object o) {
		DefaultGraphCell c = new DefaultGraphCell(o);
		GraphConstants.setValue(c.getAttributes(), o);
		decorateVertexCell(c);
		if (nextBounds != null) {
			GraphConstants.setBounds(c.getAttributes(), nextBounds);
			nextBounds = null;
		}
		return c;
	}

	public void decorateVertexCell(DefaultGraphCell c) {
		c.getAttributes().put(LABEL, getVertexLabel(c.getUserObject()));
		GraphConstants.setOpaque(c.getAttributes(), true);
		GraphConstants.setAutoSize(c.getAttributes(), true);
	}

	public void decorateEdgeCell(DefaultEdge de) {
		de.getAttributes().put(LABEL, getEdgeLabel((Edge) de.getUserObject()));
		GraphConstants.setLineEnd(de.getAttributes(),
				GraphConstants.ARROW_CLASSIC);
		GraphConstants.setLabelAlongEdge(de.getAttributes(), true);
	}

	/**
	 * Returns the connectivity inspector in use for this graph. It is also
	 * subscribed to the graph
	 */
	public ConnectivityInspector getConnectivityInspector() {
		if (connectivityInspector == null) {
			connectivityInspector = new ConnectivityInspector(this);
			addGraphListener(connectivityInspector);
		}
		return connectivityInspector;
	}

	public String getVertexLabel(Object o) {
		return base.getVertexLabel(o);
	}

	public String getEdgeLabel(Edge e) {
		return base.getEdgeLabel(e);
	}

	public String getVertexToolTip(Object o) {
		return null;
	}

	public String getEdgeToolTip(Edge e) {
		return null;
	}

	public BaseGraph getBase() {
		return base;
	}

	/**
	 * Should be called from within a BaseView, as part of initialization,
	 * before the JGraphAdapter is requested.
	 */
	public void setBase(BaseGraph base) {
		this.base = base;

		// sync with base - only here, rest should be done via structured-changes
		ArrayList vertices = new ArrayList(vertexSet());
		removeAllVertices(vertices);
		Graphs.addAllVertices(this, base.vertexSet());
		Graphs.addAllEdges(this, base, base.edgeSet());
	}

	protected Border focusedBorder = BorderFactory
			.createCompoundBorder(BorderFactory.createLineBorder(Color
					.getHSBColor(0.15f, 1f, 1f), 2), BorderFactory
					.createEmptyBorder());

	public void decorateVertexCell(DefaultGraphCell c, boolean b) {
		decorateVertexCell(c);
		if (b) {
			GraphConstants.setBorder(c.getAttributes(), focusedBorder);
		} else {
			GraphConstants.setBorder(c.getAttributes(), BorderFactory
					.createEmptyBorder());
		}
	}
}
