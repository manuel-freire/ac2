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
package es.ucm.fdi.clover.layout;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple force-directed layout algorithm. Different components do not repel each other,
 * and edge lengths are taken into account.
 *
 * @author  mfreire
 */
public class VarLengthFDL extends LayoutAlgorithm {
	private Log log = LogFactory.getLog(VarLengthFDL.class);

	// publicly visible attributes

	protected float idealDistance = 60; // ideal distance between vertices    
	protected int maxIterations = 600;
	protected int initialTemp = 30;
	protected float simmering = 2f; // simmering temperature

	// non-public attributes under here

	protected float[][] D; // holds squared distances for all nodes

	private float ideal2; // ideal squared
	protected float temp;
	protected int iterations = 0;
	protected float EPSILON = 0.0001f;
	protected float coolingConstant; // speed at which to cool

	public void init(Node N[]) {
		super.init(N);
		D = new float[N.length][N.length];

		ideal2 = idealDistance * idealDistance;

		// will cool to temperature of 'simmering' in maxIterations
		coolingConstant = (float) Math.pow(simmering / (double) initialTemp,
				1 / (double) maxIterations);

		iterations = 0;
		temp = initialTemp;
		if (N.length == 0) {
			end();
		}
	}

	public void layout() {
		calculateDistancesAndRepulsion();
		calculateAttraction();
		temp = Math.max(simmering, temp * coolingConstant);
		float energy = move(temp);

		if (iterations++ > maxIterations) {
			end();
		}
		log.debug("it: " + iterations + " energy:  " + energy);
	}

	/**
	 * called after a a layout is finished, to make sure it leaves everything
	 * consistent
	 */
	public void end() {
		super.end();
		D = null;
	}

	protected float repulsion(float d) {
		// return 0.01f / d*d; // Eades
		return ideal2 / d; // RF
	}

	protected float attraction(float d, float strength) {
		// return (d - idealDistance); // Eades        
		return d * d / idealDistance; // RF
	}

	/**
	 * Calculate distances and repulsive forces between all pairs of
	 * vertices. Could be optimized to require less calculations (using "boxes"
	 * of vertices instead of actual vertices).
	 */
	protected void calculateDistancesAndRepulsion() {
		Node n, m;
		float dx, dy, repF, d2, d;
		for (int i = 0; i < N.length; i++) {
			n = N[i];

			for (int j = i + 1; j < N.length; j++) {
				m = N[j];

				// ignore other components
				if (n.component != m.component)
					continue;

				dx = n.x - m.x;
				dy = n.y - m.y;

				d2 = dx * dx + dy * dy;
				// recently expanded clusters are placed at distance 0 from sources
				while (d2 < EPSILON) {
					n.x += Math.random() * 2f - 1f;
					n.y += Math.random() * 2f - 1f;
					dx = n.x - m.x;
					dy = n.y - m.y;
					d2 = dx * dx + dy * dy;
				}
				d = (float) Math.sqrt(dx * dx + dy * dy);
				D[i][j] = D[j][i] = d;

				repF = ideal2 / d;
				dx *= repF / d;
				dy *= repF / d;
				n.dx += dx;
				n.dy += dy;
				m.dx -= dx;
				m.dy -= dy;
			}
		}
	}

	/**
	 * Calculate attractive forces between all pairs of vertices connected
	 * by an edge. The strength is proportional to its length and "edge strength"
	 */
	protected void calculateAttraction() {
		Node n, m;
		float attF, dx, dy, d;
		for (int i = 0; i < N.length; i++) {
			n = N[i];

			for (int j = 0; j < n.edges.length; j++) {
				m = N[n.edges[j]];

				if (m == n) {
					// autoedges should not result in changes
					continue;
				}

				d = D[i][n.edges[j]] * n.strengths[j];

				attF = attraction(d, n.strengths[j]);
				dx = ((n.x - m.x) / d) * attF;
				dy = ((n.y - m.y) / d) * attF;
				n.dx -= dx;
				n.dy -= dy;
			}
		}
	}

	public float getIdealDistance() {
		return idealDistance;
	}

	public void setIdealDistance(float idealDistance) {
		this.idealDistance = idealDistance;
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public int getInitialTemp() {
		return initialTemp;
	}

	public void setInitialTemp(int initialTemp) {
		this.initialTemp = initialTemp;
	}
}
