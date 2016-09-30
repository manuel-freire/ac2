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
 * SimpleBoxLayout.java 
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
 * A very simple greedy layout. Adds a 'box' (a connected component of the graph)
 * in each iteration, placing the box as near as possible to the origin,
 * and defaulting to the corners ortherwise.
 *
 * @author mfreire
 */
public class SimpleBoxLayout extends LayoutAlgorithm {

	public static final int MIN_SEP = 5;
	private int currentBox = 0;

	private TreeSet<Point2D> points;
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
			//System.err.println("Packing component "+i+" yielded "+boxes.get(i).bounds);          
		}

		// initial point, and inline point comparator
		points = new TreeSet<Point2D>(new Comparator() {
			public int compare(Object o1, Object o2) {
				Point2D a = (Point2D) o1;
				Point2D b = (Point2D) o2;
				return (int) ((a.getX() * a.getX() + a.getY() * a.getY()) - (b
						.getX()
						* b.getX() + b.getY() * b.getY()));
			}
		});
		points.add(new Point(0, 0));
		currentBox = 0;
	}

	public void layout() {

		if (layoutFinished())
			return;

		CompBox box = boxes.get(currentBox);

		// find place for current component
		Point2D p0 = null;
		Rectangle2D b = box.bounds;
		for (Point2D p : points) {
			// System.err.println("considering "+p+"... ");
			b.setFrame(p.getX(), p.getY(), b.getWidth(), b.getHeight());
			boolean intersects = false;
			for (int i = 0; i < currentBox; i++) {
				if (b.intersects(boxes.get(i).bounds)) {
					// System.err.println("rejected: "+boxes.get(i).bounds);
					intersects = true;
					break;
				}
			}
			if (!intersects)
				break;
		}

		//System.err.println("Accepted!");
		points.add(new Point2D.Double(b.getX(), b.getY() + b.getHeight()));
		points.add(new Point2D.Double(b.getX() + b.getWidth(), b.getY()));

		for (Node n : box.nodes) {
			n.x0 += (float) b.getX();
			n.y0 += (float) b.getY();
			n.x += (float) b.getX();
			n.y += (float) b.getY();
		}

		currentBox++;
		if (currentBox == boxes.size()) {
			end();
		}
	}
}
