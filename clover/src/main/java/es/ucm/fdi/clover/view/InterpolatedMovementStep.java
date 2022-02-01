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
package es.ucm.fdi.clover.view;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.VertexView;

/**
 * This animation step will move a series of nodes from one position to another,
 * following the most direct path to get there.
 *
 * @author mfreire
 */
public class InterpolatedMovementStep implements AnimationStep {

	protected long duration;
	protected long rate = 30; // maybe this is too low...
	protected long maxShiftPerFrame = 30; // maybe a bit too high...

	protected ArrayList<AVertex> vertices;

	/**
	 * Creates a new instance of InterpolatedMovementStep
	 */
	public InterpolatedMovementStep(Map start, Map end) {
		vertices = new ArrayList<AVertex>();
		double maxDisplacement = 0;
		for (Object key : start.keySet()) {
			AVertex v = new AVertex(key, start.get(key), end.get(key));
			vertices.add(v);
			maxDisplacement = Math.max(maxDisplacement, v.getDisplacement());
		}
		this.duration = (long) (rate * maxDisplacement / maxShiftPerFrame);
	}

	/**
	 * Specialty for subclassers
	 */
	protected InterpolatedMovementStep() {
		vertices = new ArrayList<AVertex>();
	}

	public void perform(float completion, Map changeMap) {
		completion = distort(completion);
		for (AVertex vertex : vertices) {
			vertex.interpolate(completion, changeMap);
		}
	}

	public boolean isFinished() {
		return false;
	}

	public void terminate(Map changeMap) {
		perform(1, changeMap);
	}

	public long getDuration() {
		return duration;
	}

	public long getRate() {
		return rate;
	}

	protected static class AVertex {
		public DefaultGraphCell cell;
		public Rectangle2D end;
		public double x0, y0, x1, y1;

		public AVertex(Object c, Object sm, Object em) {
			cell = (DefaultGraphCell) c;
			Rectangle2D start = GraphConstants.getBounds((Map) sm);
			end = GraphConstants.getBounds((Map) em);
			x0 = start.getX();
			y0 = start.getY();
			x1 = end.getX();
			y1 = end.getY();
		}

		public AVertex(Object c, Rectangle2D start, Point2D dest) {
			cell = (DefaultGraphCell) c;
			x0 = start.getX();
			y0 = start.getY();
			x1 = dest.getX() - start.getWidth() / 2;
			y1 = dest.getY() - start.getHeight() / 2;
			end = new Rectangle2D.Double(x1, y1, start.getWidth(), start
					.getHeight());
		}

		public int getDisplacement() {
			double dx = x1 - x0;
			double dy = y1 - y0;
			return (int) Math.sqrt(dx * dx + dy * dy);
		}

		public void interpolate(float p, Map changeMap) {
			double q = 1 - p;
			Rectangle2D bounds = new Rectangle2D.Float();
			bounds.setFrame(x0 * q + x1 * p, y0 * q + y1 * p, end.getWidth(),
					end.getHeight());
			Map cellMap = (changeMap.containsKey(cell)) ? (Map) changeMap
					.get(cell) : new HashMap();
			GraphConstants.setBounds(cellMap, bounds);
			changeMap.put(cell, cellMap);
		}
	}

	/**
	 * Distorts the advance, to give the impression of gradual acceleration &
	 * deceleration.
	 */
	public float distort(float x) {
		return (float) Math.sin(x * Math.PI / 2.0);
	}

	public void init() {
	}
}
