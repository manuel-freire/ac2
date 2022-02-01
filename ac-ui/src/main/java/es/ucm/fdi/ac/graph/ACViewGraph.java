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
package es.ucm.fdi.ac.graph;

import es.ucm.fdi.ac.Submission;
import es.ucm.fdi.clover.model.Edge;
import es.ucm.fdi.clover.model.ViewGraph;
import java.awt.Color;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;

/**
 * A view for an 'AC' graph 
 *
 * @author mfreire
 */
public class ACViewGraph extends ViewGraph {

	/**
	 * Creates a new instance of ViewGraph
	 */
	public ACViewGraph(ACModel base) {
		super(base);
	}

	/**
	 * Return the lowest distance of them all
	 */
	private float getMinDistance(float[] A) {
		float min = Float.MAX_VALUE;
		for (int i = 0; i < A.length; i++) {
			if (A[i] > 0)
				min = Math.min(A[i], min);
		}

		return min;
	}

	/**
	 * Normalize a distance between a max and min value into a number from 
	 * 0 to 1
	 */
	public float normalize(double val, double min, double max) {
		return (float) ((val - min) / (max - min));
	}

	public void decorateVertexCell(DefaultGraphCell c) {
		super.decorateVertexCell(c);
		Submission s = (Submission) c.getUserObject();
		ACModel acm = (ACModel) getBase();

		c.getAttributes().put(ViewGraph.TOOLTIP, s.getId());

		String label = ((ACModel) getBase()).getCenterSubmission() == s ? "<html><h2><u>"
				+ s.getId() + "</u></h2></html>"
				: s.getId();
		c.getAttributes().put(ViewGraph.LABEL, label);

		c.getAttributes().put(ViewGraph.TOOLTIP,
				"<html><b>" + s.getId() + "</b></html>");
	}

	public void decorateEdgeCell(DefaultEdge de) {
		float sim = (Float) ((Edge) de.getUserObject()).getData();
		de.getAttributes().put(ViewGraph.EDGE_LENGTH, new Float(1f - sim));
		de.getAttributes().put(ViewGraph.LABEL, "");
		de.getAttributes().put(ViewGraph.TOOLTIP, "Distance: " + sim);

		float n = normalize(sim, 0f, ((ACModel) getBase()).getMaxValue());
		//System.err.println(sim);
		GraphConstants.setLineWidth(de.getAttributes(), .5f + ((1 - n)
				* (1 - n) * 9));
		Color color = Color.getHSBColor(n * .3f, .7f, 1f);
		GraphConstants.setLineColor(de.getAttributes(), color);
		GraphConstants
				.setLineEnd(de.getAttributes(), GraphConstants.ARROW_NONE);
	}
}
