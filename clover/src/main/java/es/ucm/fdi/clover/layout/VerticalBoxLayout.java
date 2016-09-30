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
 * VerticalBoxLayout.java 
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * Contributors: - 
 * Changelog: 
 *     18-Apr-2006: first version (mfreire)
 */

package es.ucm.fdi.clover.layout;

import java.awt.geom.*;
import java.awt.Rectangle;
import java.awt.Point;
import java.util.*;

/**
 * Layout that separates components vertically and is very fast. Ideal before
 * undoing overlaps but before final layout.
 *
 * @author mfreire
 */
public class VerticalBoxLayout extends LayoutAlgorithm {

	public static final int MIN_SEP = 5;
	private int currentBox = 0;

	private Point2D currentPoint;
	private ArrayList<CompBox> boxes;

	private static class CompBox {
		public Rectangle2D bounds = new Rectangle2D.Double();
		public ArrayList<Node> nodes = new ArrayList<Node>();

		public void addNode(Node n) {
			nodes.add(n);
		}

		public void pack() {
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

	public void init(Node[] N) {
		super.init(N);

		// find all nodes of each component
		int nBoxes = 0;
		for (int i = 0; i < N.length; i++) {
			Node n = N[i];
			nBoxes = Math.max(nBoxes, n.component + 1);
		}
		//System.err.println("Found "+nBoxes+" components");
		boxes = new ArrayList<CompBox>(nBoxes);
		for (int i = 0; i < nBoxes; i++) {
			boxes.add(new CompBox());
		}
		for (int i = 0; i < N.length; i++) {
			Node n = N[i];
			boxes.get(n.component).addNode(n);
		}
		for (int i = 0; i < boxes.size(); i++) {
			boxes.get(i).pack();
		}
		currentPoint = (new Point(0, 0));
		currentBox = 0;
	}

	public void layout() {

		if (layoutFinished)
			return;

		CompBox box = boxes.get(currentBox);
		Rectangle2D b = box.bounds;
		b.setFrame(currentPoint.getX(), currentPoint.getY(), b.getWidth(), b
				.getHeight());
		currentPoint.setLocation(b.getX(), b.getY() + b.getHeight() + MIN_SEP);

		for (Node n : box.nodes) {
			n.x0 += (float) b.getX();
			n.y0 += (float) b.getY();
			n.x += (float) b.getX();
			n.y += (float) b.getY();
		}

		currentBox++;
		if (currentBox == boxes.size()) {
			layoutFinished = true;
		}
	}
}
