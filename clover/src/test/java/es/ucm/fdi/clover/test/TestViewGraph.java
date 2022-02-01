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
package es.ucm.fdi.clover.test;

import es.ucm.fdi.clover.model.ViewGraph;
import es.ucm.fdi.clover.model.BaseGraph;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import javax.swing.BorderFactory;
import org.jgrapht.ListenableGraph;
import org.jgrapht.alg.ConnectivityInspector;
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
public class TestViewGraph extends ViewGraph {

	/**
	 * Creates a new instance of ViewGraph
	 */
	public TestViewGraph(BaseGraph base) {
		super(base);
	}

	/**
	 * does not want auto-sizing OR opaque
	 */
	public JGraphModelAdapter getModelAdapter() {
		if (adapter == null) {
			AttributeMap vertexAttribs = new AttributeMap();
			GraphConstants.setOpaque(vertexAttribs, false);
			GraphConstants.setAutoSize(vertexAttribs, false);
			AttributeMap edgeAttribs = new AttributeMap();
			super.getModelAdapter(vertexAttribs, edgeAttribs);
		}
		return adapter;
	}

	public void decorateVertexCell(DefaultGraphCell c) {
		super.decorateVertexCell(c);
		GraphConstants.setOpaque(c.getAttributes(), false);
		HashMap attribs = ((TestGraph) getBase()).getAttribs();

		Rectangle bounds = (attribs == null) ? new Rectangle(30, 30, 30, 30)
				: (Rectangle) attribs.get(c.getUserObject());
		bounds = (bounds == null) ? new Rectangle(30, 30, 30, 30) : bounds;

		GraphConstants.setBounds(c.getAttributes(), bounds);
		GraphConstants.setBorder(c.getAttributes(), BorderFactory
				.createLineBorder(Color.gray));
	}
}
