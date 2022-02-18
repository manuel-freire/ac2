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
package es.ucm.fdi.ac.dgram;

import es.ucm.fdi.ac.gui.Arrow;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author mfreire
 */
public class SimpleRenderer implements DendrogramRenderer {

	private static Color[] colorLUT = null;

	/**
	 * A fast hue-color lookup, avoiding creation of new colors;
	 * hue must be between 0 and 1,
	 * sat can be either 'false' (half-saturation) or 'true' (full).
	 */
	private static Color getColor(float hue, boolean sat) {
		int size = 1000;

		if (colorLUT == null) {
			colorLUT = new Color[size + 2];
			for (int i = 0; i < (size / 2) + 1; i++) {
				float f = i * 1f / (size / 2);
				float h = .3f * f;
				colorLUT[2 * i] = Color.getHSBColor(h, 1, 1);
				colorLUT[2 * i + 1] = Color.getHSBColor(h, 0.5f, 1);
			}
		}

		int i = Math.min((int) (hue * (size / 2)), colorLUT.length / 2 - 1);
		return sat ? colorLUT[2 * i] : colorLUT[2 * i + 1];
	}

	private void drawLine(Graphics2D g, double x0, double y0, double x1,
			double y1) {
		g.drawLine((int) x0, (int) y0, (int) x1, (int) y1);
	}

	private float paintBranch(Graphics2D g, int w, int h, Alloc a,
			DendrogramModel.DNode n) {
		DendrogramModel.DNode p = (DendrogramModel.DNode) n.getParent();
		float pd = (p != null) ? p.getDistance() : 1f;
		float r = 0;
		if (!n.isLeaf()) {
			float lo = Float.MAX_VALUE;
			float hi = Float.MIN_VALUE;
			for (int i = 0; i < n.getChildCount(); i++) {
				DendrogramModel.DNode child = (DendrogramModel.DNode) n
						.getChildAt(i);
				float x = paintBranch(g, w, h, a, child);
				lo = Math.min(lo, x);
				hi = Math.max(hi, x);
				r += x;
			}
			r /= n.getChildCount();
			//            g.setColor(Color.black);
			g.setColor(getColor(pd, true));
			drawLine(g, n.getDistance() * w, lo, n.getDistance() * w, hi);
			drawLine(g, n.getDistance() * w, r, pd * w, r);
		} else {
			r = a.getHeight();
			g.setColor(getColor(pd, true));
			drawLine(g, 0, a.getHeight(), pd * w, a.getHeight());
			g.setColor(Color.white);
			g.drawString("" + n.getUserObject(), 0, a.getHeight());
			g.setColor(Color.black);
			a.increment();
		}
		return r;
	}

	private static class Alloc {
		private float height = 0.0f;
		private float rowHeight;

		private Alloc(float rowHeight) {
			this.rowHeight = rowHeight;
			increment();
		}

		private float getHeight() {
			return height;
		}

		private void increment() {
			height += rowHeight;
		}
	}

	public void paint(Graphics g, Dendrogram dendrogram) {

		int h = dendrogram.getHeight();
		int w = dendrogram.getWidth();

		g.setColor(Color.black);
		g.fillRect(0, 0, w, h);

		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(Color.white);
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				0.6f));
		g2d.fill(new Rectangle2D.Float(0, 0, dendrogram.getCurrent() * w, h));

		Rectangle2D rs = g2d.getFontMetrics().getStringBounds("similar", g2d);
		rs.setRect(2 + w / 5, 0, rs.getWidth(), rs.getHeight() + 1);
		Rectangle2D rd = g2d.getFontMetrics().getStringBounds("different", g2d);
		rd.setRect(w - rd.getWidth() - w / 5, 0, rd.getWidth(),
				rd.getHeight() + 1);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				0.6f));

		double aw = Arrow.getArrowWidth();
		g2d.draw(Arrow.getArrow(false).createTransformedShape(
				AffineTransform.getTranslateInstance(rs.getMinX() - aw, 5)));
		g2d.draw(Arrow.getArrow(true).createTransformedShape(
				AffineTransform.getTranslateInstance(rd.getMaxX(), 5)));

		g2d.setColor(Color.white);
		g2d
				.drawString("similar", (float) rs.getX(), (float) rs
						.getHeight() - 1);
		g2d.drawString("different", (float) rd.getX(),
				(float) rd.getHeight() - 1);
		g2d.setComposite(AlphaComposite
				.getInstance(AlphaComposite.SRC_OVER, 1f));

		g.setColor(Color.BLACK);
		g.setFont(g.getFont().deriveFont(12));
		float rh = 12f; //h / dendrogram.getModel().getLeaves().size();                        

		float have = h;
		float want = rh * (dendrogram.getModel().getLeaves().size() + 1);
		//System.err.println("Drawing in "+w+"x"+h+"; would want " + want+"; scale is"+());

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		AffineTransform original = g2d.getTransform();
		original.concatenate(AffineTransform.getScaleInstance(1, have / want));
		g2d.setTransform(original);

		paintBranch(g2d, w, h, new Alloc(rh), dendrogram.getModel().getRoot());
	}
}