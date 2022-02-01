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
package es.ucm.fdi.ac.gui;

import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

/**
 * Returns an arrow shape, which fits in a a 15x10 box
 *
 * @author mfreire
 */
public class Arrow {
	private static Path2D leftArrow;
	private static Path2D rightArrow;

	public static double getArrowWidth() {
		return getArrow(true).getBounds2D().getWidth();
	}

	public static Path2D getArrow(boolean isRight) {
		if (leftArrow == null) {
			Polygon left = new Polygon(new int[] { 2, 0, 2, 1, 3, 1, 2 },
					new int[] { 0, 1, 2, 1, 1, 1, 0 }, 7);
			Polygon right = new Polygon(new int[] { 1, 3, 1, 2, 0, 2, 1 },
					new int[] { 0, 1, 2, 1, 1, 1, 0 }, 7);
			leftArrow = new Path2D.Float(left, AffineTransform
					.getScaleInstance(5, 5));
			rightArrow = new Path2D.Float(right, AffineTransform
					.getScaleInstance(5, 5));
		}

		return isRight ? rightArrow : leftArrow;
	}
}
