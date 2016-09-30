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
 * VerticalBarRenderer.java
 *
 * Created on September 6, 2006, 5:32 PM
 *
 */

package es.ucm.fdi.ac.tableviz;

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
public class SimpleRenderer implements TableRenderer {
	private float MAX_FONT = 12;

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

	public void paint(Graphics g, TableViz tv) {

		int h = tv.getHeight();
		int w = tv.getWidth();
		int n = tv.getModel().getN();

		g.setColor(Color.black);
		g.fillRect(0, 0, w, h);

		g.setColor(Color.BLACK);
		g.setFont(g.getFont().deriveFont(12));
		float rh = 12f;
		float want = rh * n;
		//System.err.println("Drawing in "+w+"x"+h+"; would want " + want+"; scale is"+());

		int x0 = 40;
		int y0 = 0;
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		AffineTransform original = g2d.getTransform();
		original.concatenate(AffineTransform.getScaleInstance(w / (want + x0),
				h / (want + y0)));
		g2d.setTransform(original);

		TableModel m = tv.getModel();
		Rectangle2D r = new Rectangle2D.Float(0, 0, rh, rh);
		for (int i = 0; i < n; i++) {
			g2d.setColor(Color.white);
			g.drawString("" + m.getLabel(i), 0, (int) (rh * (i + 1)) + y0);
			for (int j = 0; j < n; j++) {
				g2d.setColor(getColor(m.get(i, j), true));
				r.setRect(i * rh + x0, j * rh + y0, rh, rh);
				g2d.fill(r);
			}
		}
	}
}