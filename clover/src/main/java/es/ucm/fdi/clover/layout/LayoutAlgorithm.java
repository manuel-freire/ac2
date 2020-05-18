/**
 * AC - A source-code copy detector
 *
 *     For more information please visit:  http://github.com/manuel-freire/ac
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
 * LayoutAlgorithm.java 
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * Contributors: - 
 * Changelog: 
 *     18-Apr-2006: first version (mfreire)
 */

package es.ucm.fdi.clover.layout;

/**
 * A layout algorithm is something whose "layout" method you repeatedly
 * call until either you get bored or the layout gets bored (signaled by
 * returning 'true' from layoutFinished).
 *
 * The sequence, given an algorithm, is to
 * - call init with the nodes you want to layout
 * - cycle with layout until bored or layoutFinished
 * - if more layout is to be performed, call init.
 *
 * This sequence is just what LayoutManagers are supposed to do.
 *
 * @author  mfreire
 */
public abstract class LayoutAlgorithm {

	/** array of nodes to lay out*/
	protected Node[] N;

	/** queried to check if layout finished or not */
	protected boolean layoutFinished;

	/**
	 * layout cannot be called after instantiation or end() or isFinished()
	 * until after a call to init()
	 */
	public void init(Node[] N) {
		this.N = N;
		layoutFinished = false;
	}

	/**
	 * returns true if this layout has finished
	 */
	public boolean layoutFinished() {
		return layoutFinished;
	}

	/**
	 * called after a a layout is finished, to make sure it leaves everything
	 * consistent. It should also prevent further "layout" from being effective,
	 * until the next init is called
	 */
	public void end() {
		layoutFinished = true;
	}

	/**
	 * a step of layout. Should be relatively short, because individual steps
	 * don't get interrupted.
	 */
	public abstract void layout();

	/**
	 * Move nodes around; bounded version (will not allow movements greater than
	 * the bound, and will cap them instead)
	 */
	public float move(double bound) {
		Node n;
		double dev, length, total = 0;
		for (int i = 0; i < N.length; i++) {
			n = N[i];
			dev = n.dx * n.dx + n.dy * n.dy;
			length = Math.sqrt(dev);
			if (dev > bound) {
				dev = bound / length;
				n.dx *= dev;
				n.dy *= dev;
				length = dev;
			}
			n.x += n.dx;
			n.y += n.dy;
			n.x0 += n.dx;
			n.y0 += n.dy;
			n.dx /= 2;
			n.dy /= 2;
			total += length;
		}
		return (float) total;
	}

	/**
	 * Move nodes around; unbounded version
	 */
	public float move() {
		Node n;
		double dev, length, total = 0;
		for (int i = 0; i < N.length; i++) {
			n = N[i];
			dev = n.dx * n.dx + n.dy * n.dy;
			length = Math.sqrt(dev);
			n.x += n.dx;
			n.y += n.dy;
			n.x0 += n.dx;
			n.y0 += n.dy;

			total += length;
			n.dx = 0;
			n.dy = 0;
		}
		return (float) total;
	}

	/**
	 * same as move, but only returns how much movement would have taken place,
	 * not the movement itself.
	 */
	public float simulateMove() {
		Node n;
		double dev, length, total = 0;
		for (int i = 0; i < N.length; i++) {
			n = N[i];
			dev = n.dx * n.dx + n.dy * n.dy;
			length = Math.sqrt(dev);
			total += length;
		}
		return (float) total;
	}
}
