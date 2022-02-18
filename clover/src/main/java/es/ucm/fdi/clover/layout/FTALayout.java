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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Layout algorithm that avoids overlap of two nodes. Adapted from
 * Xiaodi Huang and Wei Lai's ACSC2003 paper:
 * "Force-Transfer: A New Approach to Removing Overlapping Nodes in Graph Layout"
 * Certain overlaps are not removed...
 *
 * @author mfreire
 */
@SuppressWarnings("all")
public class FTALayout extends LayoutAlgorithm {

	public static final int MIN_SEP = 5;
	private int iterations = 0;
	private int maxIterations = 4;

	public void init(Node[] N) {
		super.init(N);
		iterations = 0;
	}

	public void layout() {
		float[] center = findCenter();
		layoutFinished = !fixOverlaps(center);
		iterations++;
		if (iterations > maxIterations) {
			end();
		}
	}

	public float[] findCenter() {
		float coords[] = new float[2];
		coords[0] = (float) Node.getBounds(N, 1.0).getCenterX();
		coords[1] = (float) Node.getBounds(N, 1.0).getCenterY();
		return coords;
	}

	public void findTNS(TreeSet tns, Node[] S, int o) {
		for (int i = o + 1; i < S.length; i++) {
			float adx = Math.abs(S[o].x - S[i].x);
			float dhw = (int) ((S[o].w + S[i].w) / 2) + MIN_SEP;
			float ady = Math.abs(S[o].y - S[i].y);
			float dhh = (int) ((S[o].h + S[i].h) / 2) + MIN_SEP;
			if ((adx < dhw) && (ady < dhh)) {
				if (tns.add(S[i])) {
					findTNS(tns, S, i);
				}
			}
		}
	}

	private static class RComp implements Comparator {
		public int compare(Object a, Object b) {
			Node u = (Node) a;
			Node v = (Node) b;
			float dx = u.x0 - v.x0;
			float dy = u.y0 - v.y0;
			if (dx != 0)
				return (int) dx;
			if (dy != 0)
				return ((dy > 0) ? 1 : -1);
			return 0;
		}
	}

	private static class LComp implements Comparator {
		public int compare(Object a, Object b) {
			Node v = (Node) a;
			Node u = (Node) b;
			float dx = (u.x0 + u.w) - (v.x0 + v.w);
			float dy = (u.y0 + u.h) - (v.y0 + v.h);
			if (dx != 0)
				return (int) dx;
			if (dy != 0)
				return ((dy > 0) ? 1 : -1);
			return 0;
		}
	}

	private static class DComp implements Comparator {
		public int compare(Object a, Object b) {
			Node u = (Node) a;
			Node v = (Node) b;
			float dx = u.x0 - v.x0;
			float dy = u.y0 - v.y0;
			if (dy != 0)
				return (int) dy;
			if (dx != 0)
				return ((dx > 0) ? 1 : -1);
			return 0;
		}
	}

	private static class UComp implements Comparator {
		public int compare(Object a, Object b) {
			Node v = (Node) a;
			Node u = (Node) b;
			float dx = (u.x0 + u.w) - (v.x0 + v.w);
			float dy = (u.y0 + u.h) - (v.y0 + v.h);
			if (dy != 0)
				return (int) dy;
			if (dx != 0)
				return ((dx > 0) ? 1 : -1);
			return 0;
		}
	}

	public boolean fixOverlaps(float[] start) {

		Node[] S = (Node[]) N.clone();

		float dx, dy, delta;
		TreeSet tns;

		boolean hasChanged = false;

		// RHS
		Arrays.sort(S, new RComp());
		for (int i = 0; i < S.length; i++) {
			if (start[0] > S[i].x0 + S[i].w)
				continue;
			tns = new TreeSet(new RComp());
			findTNS(tns, S, i);
			for (Iterator ti = tns.iterator(); ti.hasNext(); /**/) {
				Node v = (Node) ti.next();
				dx = Math.abs(S[i].x0 + S[i].w - v.x0);
				dy = Math.min(Math.abs(S[i].y0 + S[i].h - v.y0), Math.abs(v.y0
						+ v.h - S[i].y0));
				delta = Math.min(dx, dy);
				if (delta == dx) {
					delta += MIN_SEP;
					for (Iterator tnsi = tns.iterator(); tnsi.hasNext(); /**/) {
						Node w = (Node) tnsi.next();
						w.x0 += delta;
						w.x += delta;
					}
					hasChanged = true;
					break;
				}
			}
		}

		// LHS
		Arrays.sort(S, new LComp());
		for (int i = 0; i < S.length; i++) {
			if (start[0] < S[i].x0)
				continue;
			tns = new TreeSet(new LComp());
			findTNS(tns, S, i);
			for (Iterator ti = tns.iterator(); ti.hasNext(); /**/) {
				Node v = (Node) ti.next();
				dx = Math.abs(v.x0 + v.w - S[i].x0);
				dy = Math.min(Math.abs(S[i].y0 + S[i].h - v.y0), Math.abs(v.y0
						+ v.h - S[i].y0));
				delta = Math.min(dx, dy);
				if (delta == dx) {
					delta += MIN_SEP;
					for (Iterator tnsi = tns.iterator(); tnsi.hasNext(); /**/) {
						Node w = (Node) tnsi.next();
						w.x0 -= delta;
						w.x -= delta;
					}
					hasChanged = true;
					break;
				}
			}
		}

		// DHS
		Arrays.sort(S, new DComp());
		for (int i = 0; i < S.length; i++) {
			if (start[1] > S[i].y0 + S[i].h)
				continue;
			tns = new TreeSet(new DComp());
			findTNS(tns, S, i);
			for (Iterator ti = tns.iterator(); ti.hasNext(); /**/) {
				Node v = (Node) ti.next();
				dy = Math.abs(S[i].y0 + S[i].h - v.y0);
				dx = Math.min(Math.abs(S[i].x0 + S[i].w - v.x0), Math.abs(v.x0
						+ v.w - S[i].x0));
				delta = Math.min(dx, dy);
				if (delta == dy) {
					delta += MIN_SEP;
					for (Iterator tnsi = tns.iterator(); tnsi.hasNext(); /**/) {
						Node w = (Node) tnsi.next();
						w.y0 += delta;
						w.y += delta;
					}
					hasChanged = true;
					break;
				}
			}
		}

		// UHS
		Arrays.sort(S, new UComp());
		for (int i = 0; i < S.length; i++) {
			if (start[1] < S[i].y0)
				continue;
			tns = new TreeSet(new UComp());
			findTNS(tns, S, i);
			for (Iterator ti = tns.iterator(); ti.hasNext(); /**/) {
				Node v = (Node) ti.next();
				dy = Math.abs(v.y0 + v.h - S[i].y0);
				dx = Math.min(Math.abs(S[i].x0 + S[i].w - v.x0), Math.abs(v.x0
						+ v.w - S[i].x0));
				delta = Math.min(dx, dy);
				if (delta == dy) {
					delta += MIN_SEP;
					for (Iterator tnsi = tns.iterator(); tnsi.hasNext(); /**/) {
						Node w = (Node) tnsi.next();
						w.y0 -= delta;
						w.y -= delta;
					}
					hasChanged = true;
					break;
				}
			}
		}
		return hasChanged;
	}
}
