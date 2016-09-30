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
 * LayoutCritic.java
 *
 * Created on May 13, 2006, 2:50 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.layout;

import java.awt.geom.*;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.VertexView;

/**
 * This class measures the quality of a layout. It uses simple heuristics for 
 * this task, since there is no general agreement on what layout is better 
 * than another, and certain criteria can be very hard to measure.
 *
 * @author mfreire
 */
public class LayoutCritic {

	Logger log = Logger.getLogger(LayoutCritic.class);

	private float nodeOverlapPenalty = 100f;
	private float edgeCrossPenalty = 40f;
	private float edgeNodeCrossPenalty = 40f;
	private float fdlEnergyPenalty = 10f;

	private Node[] N;

	public LayoutCritic(Node[] N) {
		this.N = N;
	}

	public float getQuality() {
		int nodeOverlaps = getNodeOverlaps();
		int edgeNodeCrossings = getEdgeNodeCrossings();
		int edgeCrossings = getEdgeCrossings();
		float fdlEnergy = getFdlEnergy();
		float quality = nodeOverlaps * nodeOverlapPenalty + edgeNodeCrossings
				* edgeNodeCrossPenalty + edgeCrossings * edgeCrossPenalty
				+ fdlEnergy * fdlEnergyPenalty;
		log.debug("nn/ne/ee/fdl/t: " + nodeOverlaps + "/" + edgeNodeCrossings
				+ "/" + edgeCrossings + "/" + fdlEnergy + "/" + quality);
		return quality;
	}

	public int getComponentOverlaps() {
		int componentOverlaps = 0;
		ArrayList<CompBox> boxes;
		boxes = new ArrayList<CompBox>();
		for (Node n : N) {
			for (int i = boxes.size(); i < n.component + 1; i++) {
				boxes.add(new CompBox());
			}
			boxes.get(n.component).addNode(n);
		}
		for (CompBox a : boxes) {
			a.pack();
		}
		for (CompBox a : boxes) {
			for (CompBox b : boxes) {
				if (a == b)
					continue;
				if (a.bounds.intersects(b.bounds)) {
					componentOverlaps++;
				}
			}
		}
		return componentOverlaps;
	}

	private static class CompBox {
		public Rectangle2D bounds = new Rectangle2D.Double();
		public ArrayList<Node> nodes = new ArrayList<Node>();

		public void addNode(Node n) {
			nodes.add(n);
		}

		public void pack() {
			int MIN_SEP = 5;
			bounds = Node.getBounds(nodes, 1.0);
			bounds.setFrame(bounds.getX(), bounds.getY(), bounds.getWidth()
					+ MIN_SEP * 2, bounds.getHeight() + MIN_SEP * 2);
			for (Node n : nodes) {
				n.x0 -= bounds.getX() - MIN_SEP;
				n.y0 -= bounds.getY() - MIN_SEP;
				n.x -= bounds.getX() - MIN_SEP;
				n.y -= bounds.getY() - MIN_SEP;
			}
			bounds.setFrame(0, 0, bounds.getWidth(), bounds.getHeight());
		}
	}

	public int getNodeOverlaps() {
		Node a, b, c, d;
		Rectangle2D ra = new Rectangle2D.Float();
		Rectangle2D rb = new Rectangle2D.Float();

		int nodeOverlaps = 0;
		for (int i = 0; i < N.length; i++) {
			a = N[i];
			ra.setFrame(a.x0, a.y0, a.w, a.h);

			// node overlaps, & store edges for later
			for (int j = i + 1; j < N.length; j++) {
				b = N[j];
				rb.setFrame(b.x0, b.y0, b.w, b.h);
				if (ra.intersects(rb)) {
					nodeOverlaps++;
				}
			}
		}

		return nodeOverlaps;
	}

	public int getEdgeNodeCrossings() {
		Node a, b, c, d;
		Rectangle2D ra = new Rectangle2D.Float();
		Line2D line = new Line2D.Float();

		int edgeNodeCrossings = 0;
		for (int i = 0; i < N.length; i++) {
			a = N[i];
			ra.setFrame(a.x0, a.y0, a.w, a.h);

			// edge-node overlaps
			for (int j = 0; j < N.length; j++) {
				b = N[j];
				if (b == a)
					continue;

				for (int k = 0; k < b.edges.length; k++) {
					if (b.edges[k] > j)
						continue;
					c = N[b.edges[k]];
					if (c == a)
						continue;

					line.setLine(b.x, b.y, c.x, c.y);
					if (line.intersects(ra)) {
						log.debug("overlap: " + i + " & " + j + ","
								+ b.edges[k]);
						edgeNodeCrossings++;
					}
				}
			}
		}

		return edgeNodeCrossings;
	}

	public int getEdgeCrossings() {
		Node a, b, c, d;
		Line2D line = new Line2D.Float();
		Line2D otherLine = new Line2D.Float();

		int edgeCrossings = 0;
		for (int i = 0; i < N.length; i++) {
			a = N[i];

			// edge-edge crossings (will find double the actual number...)
			for (int k = 0; k < a.edges.length; k++) {
				if (a.edges[k] > i)
					continue;
				c = N[a.edges[k]];

				// line A->C, with idx of a < idx of c
				line.setLine(a.x, a.y, c.x, c.y);

				for (int j = i + 1; j < N.length; j++) {
					b = N[j];
					if (b == c)
						continue;

					for (int l = 0; l < b.edges.length; l++) {
						if (b.edges[l] > j)
							continue;
						d = N[b.edges[l]];
						if (d == a || d == c)
							continue;

						otherLine.setLine(b.x, b.y, d.x, d.y);
						if (line.intersectsLine(otherLine)) {
							log.debug("intersect: " + i + "," + a.edges[k]
									+ " - " + j + "," + b.edges[l]);
							edgeCrossings++;
						}
					}
				}
			}
		}

		return edgeCrossings;
	}

	public float getFdlEnergy() {
		VarLengthFDL vlfdl = new VarLengthFDL();
		vlfdl.init(N);
		vlfdl.calculateDistancesAndRepulsion();
		vlfdl.calculateAttraction();
		float fdlEnergy = vlfdl.simulateMove();
		return fdlEnergy;
	}

	public void setNodeOverlapPenalty(float nodeOverlapPenalty) {
		this.nodeOverlapPenalty = nodeOverlapPenalty;
	}

	public void setEdgeCrossPenalty(float edgeCrossPenalty) {
		this.edgeCrossPenalty = edgeCrossPenalty;
	}

	public void setEdgeNodeCrossPenalty(float edgeNodeCrossPenalty) {
		this.edgeNodeCrossPenalty = edgeNodeCrossPenalty;
	}

	public void setFdlEnergyPenalty(float fdlEnergyPenalty) {
		this.fdlEnergyPenalty = fdlEnergyPenalty;
	}
}
