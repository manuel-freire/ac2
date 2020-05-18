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
 * VerticalBarRenderer.java
 *
 * Created on September 6, 2006, 5:32 PM
 *
 */

package es.ucm.fdi.ac.hist;

import java.awt.Color;
import java.awt.Graphics;

/**
 *
 * @author mfreire
 */
public class BarRenderer implements HistogramRenderer {
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
				float h = 1 - (.25f + .7f * f);
				colorLUT[2 * i] = Color.getHSBColor(h, 1, 1);
				colorLUT[2 * i + 1] = Color.getHSBColor(h, 0.5f, 1);
			}
		}

		int i = (int) (hue * (size / 2));
		return sat ? colorLUT[2 * i] : colorLUT[2 * i + 1];
	}

	private int hatHeight = 4;
	private int hatWidth = 5;

	public void paint(Graphics g, Histogram histogram) {

		int h = histogram.getHeight();
		int w = histogram.getWidth();

		g.setColor(Color.gray);
		g.fillRect(0, 0, (int) (histogram.getCurrent() * w), h);

		// second pass: sampling hats
		g.setColor(Color.BLACK);

		float step = 1f / histogram.getLevels();
		for (float x = 0; x < 1f; x += step) {
			int i = (int) (x * w);

			// do not draw empty hats; and only draw them where due
			if (histogram.getBars()[i] == 0)
				continue;
			int rh = (int) (histogram.getBars()[i] * (h - hatHeight));
			g.fillRect(i - (hatWidth / 2), h - rh - hatHeight, hatWidth,
					hatHeight + h);
			//            g.drawLine(i, h-rh, i, h);
		}

		// first pass: accurate as hell
		for (int i = 0; i < w; i++) {
			// do not draw empty bars
			if (histogram.getBars()[i] == 0)
				continue;

			float x = i * 1.0f / w;
			g.setColor(getColor(histogram.getBars()[i], histogram.getModel()
					.count(x, x + 1.0f / w) > 0));
			int rh = (int) (histogram.getBars()[i] * (h - hatHeight));
			g.drawLine(i, h - rh, i, h);
		}

		// highlights (if any)
		if (histogram.getModel().getHighlights() != null) {
			g.setColor(Color.GREEN.darker());
			for (double high : histogram.getModel().getHighlights()) {
				g.drawLine((int) (high * w), 0, (int) (high * w), h);
			}
		}

		// text (if any)
		if (histogram.getText() != null) {
			g.setColor(Color.black);
			g.drawString(histogram.getText(), 0, h
					- g.getFontMetrics().getMaxDescent());
		}
	}

	public int getBarCount(Histogram h) {
		return h.getWidth();
	}
}