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
 * RandomLayout.java
 *
 * Created on May 17, 2006, 1:28 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.layout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeMap;

/**
 * A random layout designed to handle large graphs more efficiently than a 
 * fully-random one; 
 * - selects a 'center vertex' for each component; places at 0,0; adds to queue
 * - follows edges breadth-first from center (ignoring 'placed' vertices),
 *   assigns random position within R radius circle centered at parent+R*v, 
 *   where 'v' is the direction vector of perceived same-component repulsion forces
 *
 * @author mfreire
 */
public class ForceTreeLayout extends LayoutAlgorithm {
	private int radius = 100;

	public ForceTreeLayout(int radius) {
		this.radius = radius;
	}

	public void layout() {
		LinkedList<Integer> queue = new LinkedList<Integer>();
		HashSet<Integer> knownComponents = new HashSet<Integer>();
		int[] childToParent = new int[N.length];
		TreeMap<Integer, ArrayList<Integer>> placed = new TreeMap<Integer, ArrayList<Integer>>();

		// initialize children, queue
		for (int i = 0; i < N.length; i++) {
			childToParent[i] = -1;
		}
		for (int i = 0; i < N.length; i++) {
			Node n = N[i];
			if (!knownComponents.contains(n.component)) {
				childToParent[i] = i; // 'parent of self'; will prevent revisit
				knownComponents.add(n.component);
				setPos(n, 0, 0);
				for (int j : n.edges) {
					childToParent[j] = i;
					queue.add(j);
					ArrayList<Integer> al = new ArrayList<Integer>();
					al.add(i);
					placed.put(n.component, al);
					//System.err.println("Added "+j+" as first-born of "+childToParent[j]);
				}
			}
		}

		int CUTOFF_DISTANCE = 1000;

		// breadth-first ahoy
		while (!queue.isEmpty()) {
			int i = queue.removeFirst();
			//System.err.println("Processing "+i+", child of "+childToParent[i]);
			Node n = N[i];
			Node p = N[childToParent[i]];

			// calculate normalized 'repulsion vector' from previously-placed vertices
			float rpx = 0;
			float rpy = 0;
			for (int k : placed.get(n.component)) {
				if (N[k] == p)
					continue;
				float dx = p.x - N[k].x;
				float dy = p.y - N[k].y;
				float dpk = (float) Math.sqrt(dx * dx + dy * dy);
				if (dpk > CUTOFF_DISTANCE)
					continue;
				dpk *= dpk * dpk;
				rpx += dx / dpk; // = dx / d(p,x) * 1 / d(p,x)/\2
				rpy += dy / dpk;
			}
			float dr = (float) Math.sqrt(rpx * rpx + rpy * rpy);
			if (dr > 0) {
				rpx /= dr;
				rpy /= dr;
			}

			// calculate position along smallish circle (ie: guaranteed to go 'outwards')
			float rr = radius / 4;
			float ra = (float) (Math.random() * 2 * Math.PI);
			float rx = (float) (rr * Math.cos(ra));
			float ry = (float) (rr * Math.sin(ra));

			// place there; update prev. positions
			setPos(n, (int) (p.x + rpx * radius + rx), (int) (p.y + rpy
					* radius + ry));
			placed.get(n.component).add(i);
			//System.err.println("Fleeing from   : "+rpx+", "+rpy);
			//System.err.println("Assigning delta: "+ (n.x-p.x) + ", " + (n.y-p.y));

			// and add new recruits
			for (int j : n.edges) {
				if (childToParent[j] == -1) {
					childToParent[j] = i;
					queue.add(j);
				}
				//else System.err.println("Avoided adding "+j);
			}
		}
		System.err.println("Finished!");

		end();
	}

	public void setPos(Node n, int x, int y) {
		n.x0 = x - (n.w / 2);
		n.x = x;
		n.y0 = y - (n.h / 2);
		n.y = y;
		n.dx = n.dy = 0;
	}
}
