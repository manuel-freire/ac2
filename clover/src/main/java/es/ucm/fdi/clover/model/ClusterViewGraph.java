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

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;

import java.awt.Color;

/**
 * This is a view for a clustered graph. Has some clustering-specific
 * things not included in the plain ViewGraph
 *
 * @author mfreire
 */
public class ClusterViewGraph extends ViewGraph {

	private static Color clusterVertexColor = new Color(150, 150, 250);
	private static Color defaultVertexColor = new Color(200, 250, 150);

	/**
	 * Creates a new instance of ViewGraph
	 */
	public ClusterViewGraph(ClusteredGraph base) {
		super(base);
	}

	public void decorateVertexCell(DefaultGraphCell c) {
		super.decorateVertexCell(c);
		GraphConstants.setOpaque(c.getAttributes(), true);
		if (c.getUserObject() instanceof Cluster.Vertex) {
			GraphConstants.setBackground(c.getAttributes(), clusterVertexColor);
			Cluster clu = ((Cluster.Vertex) c.getUserObject()).getCluster();
			GraphConstants.setInset(c.getAttributes(), (int) (2 + 4 * Math
					.log(clu.getDescendants().size())));
		} else {
			GraphConstants.setBackground(c.getAttributes(), defaultVertexColor);
			GraphConstants.setInset(c.getAttributes(), 2);
		}

		GraphConstants.setAutoSize(c.getAttributes(), true);
		ClusteredGraph cg = (ClusteredGraph) getBase();
		if (c.getUserObject() == cg.getPointOfInterest()) {
			GraphConstants.setBorder(c.getAttributes(), focusedBorder);
		}
	}

	public void decorateEdgeCell(DefaultEdge de) {
		super.decorateEdgeCell(de);
		GraphConstants.setLineEnd(de.getAttributes(),
				GraphConstants.ARROW_CLASSIC);
		GraphConstants.setLabelAlongEdge(de.getAttributes(), true);
		GraphConstants.setLineColor(de.getAttributes(), Color.gray);
		Edge e = (Edge) de.getUserObject();
		if (e.getSource() instanceof Cluster.Vertex
				|| e.getTarget() instanceof Cluster.Vertex) {
			GraphConstants.setLineWidth(de.getAttributes(), 2);
			GraphConstants.setLineEnd(de.getAttributes(),
					GraphConstants.ARROW_SIMPLE);
			GraphConstants.setLineColor(de.getAttributes(), Color.green
					.darker());
		}
	}

	public String getVertexLabel(Object o) {
		if (o instanceof Cluster.Vertex) {
			Cluster c = ((Cluster.Vertex) o).getCluster();
			if (c.getName() != null) {
				return c.getName();
			} else {
				StringBuffer sb = new StringBuffer();
				for (Object v : c.getLeafVertices()) {
					sb.append(super.getVertexLabel(v) + " ");
				}
				return sb.toString();
			}
		}
		return super.getVertexLabel(o);
	}

	public String getEdgeLabel(Edge e) {
		if (e.getSource() instanceof Cluster.Vertex
				|| e.getTarget() instanceof Cluster.Vertex) {
			return "";
		}
		return super.getEdgeLabel(e);
	}

	public String getVertexToolTip(Object o) {
		return null;
	}

	public String getEdgeToolTip(Edge e) {
		return null;
	}
}
