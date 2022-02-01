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

/**
 * A simple random layout algorithm. Different distributions could be tested,
 * such as random placement inside circle, and so on.
 *
 * @author mfreire
 */
public class RandomLayout extends LayoutAlgorithm {
	private int maxX;
	private int maxY;

	public RandomLayout(int maxX, int maxY) {
		this.maxX = maxX;
		this.maxY = maxY;
	}

	public void layout() {
		for (Node n : N) {
			n.x0 = (float) Math.random() * (maxX - n.w);
			n.x = n.x0 + (n.w / 2);
			n.y0 = (float) Math.random() * (maxY - n.h);
			n.y = n.y0 + (n.h / 2);
			n.dx = n.dy = 0;
		}
		end();
	}
}
