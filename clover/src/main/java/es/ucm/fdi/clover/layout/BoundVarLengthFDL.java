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

import java.util.HashSet;

/**
 * Use same as a VarLengthFDL, but call setFreeNodes before use. FreeNodes
 * will behave just as in a VarLengthFDL - but others will be sort of 
 * anchored to their current positions.
 *
 * @author  manu
 */
@SuppressWarnings("all")
public class BoundVarLengthFDL extends VarLengthFDL {

	private Log log = LogFactory.getLog(BoundVarLengthFDL.class);

	/**
	 * this array is the only big difference with the traditional FDL.
	 * The positions in the array are the original positions of each node. 
	 * Node 'i' will be attracted to position 'i' with an elastic band. If the
	 * position has its X coordinate to Float.NaN, then there will be no 
	 * rubber band.
	 */
	protected float[][] F;

	private HashSet freeNodes = null;

	private float bandingConstant = 10.0f; // experimentally set

	/** 
	 * number of times to restart before considering "finish". Strangely enough,
	 * setting a low bandingConstant is worse than running the algorithm several times...
	 */
	private int numberOfRuns = 3;

	private int currentRun;

	/**
	 * the free nodes are those that will not be bound to stay near their
	 * current positions.
	 */
	public void setFreeNodes(HashSet freeNodes, int numberOfRuns) {
		this.freeNodes = freeNodes;
		this.numberOfRuns = numberOfRuns;
	}

	public void init(Node[] N) {
		super.init(N);
		F = new float[N.length][2];

		for (int i = 0; i < N.length; i++) {
			if (freeNodes != null && freeNodes.contains(N[i].peer)) {
				//                System.err.println(
				//                  ((DefaultGraphCell)((org.jgraph.graph.VertexView)N[i].peer).getCell()).getUserObject()
				//                  + " is free!");
				F[i][0] = Float.NaN;
				F[i][1] = Float.NaN;
			} else {
				F[i][0] = N[i].x;
				F[i][1] = N[i].y;
			}
		}
		currentRun = 0;
		//System.err.println("Max iterations: "+maxIterations);
	}

	/**
	 * Overrides defaults; called after a a layout would have been finished
	 * This version re-runs the layout N times
	 */
	public void end() {
		if (currentRun < numberOfRuns) {
			for (int i = 0; i < N.length; i++) {
				if (F[i][0] != Float.NaN) {
					F[i][0] = N[i].x;
					F[i][1] = N[i].y;
				}
			}
			currentRun++;

			iterations = 0;
			temp = initialTemp;
		} else {
			super.end();
			F = null;
			freeNodes = null;
		}
	}

	/**
	 * Overrides calculateAttraction; adds the 'rubber bands' to previous
	 * positions.
	 */
	public void calculateAttraction() {
		super.calculateAttraction();

		Node n, m;
		float dx, dy, d;
		for (int i = 0; i < N.length; i++) {

			// NaN is used to signal "not set" values
			if (Float.isNaN(F[i][0]))
				continue;

			n = N[i];
			dx = n.x - F[i][0];
			dy = n.y - F[i][1];
			// cancelled out in eq; d = (float)Math.sqrt(dx*dx + dy*dy);
			dx *= bandingConstant;
			dy *= bandingConstant;
			n.dx -= dx;
			n.dy -= dy;
		}
	}

	public float getBandingConstant() {
		return bandingConstant;
	}

	public void setBandingConstant(float bandingConstant) {
		this.bandingConstant = bandingConstant;
	}
}
