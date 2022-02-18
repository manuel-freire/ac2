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
package es.ucm.fdi.ac.hist;

import es.ucm.fdi.ac.gui.Arrow;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.util.*;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import javax.swing.*;

/**
 * A histogram component. Renderers and Models are separate entities; the 
 * component itself brings them together, and is expected to support selection
 * operations and tooltips.
 *
 * @author mfreire
 */
public class Histogram extends JPanel {

	private HistogramModel model;
	private float current;
	private int levels = 100;
	private String text;

	private float[] bars;
	private float maxBar;
	private int oldLen = -1;

	private HistogramRenderer renderer;

	private static HashMap<Class<? extends HistogramRenderer>, HistogramRenderer> renderers = new HashMap<>();

	public Histogram(HistogramModel model, Class<? extends HistogramRenderer> renderClass) {
		setModel(model);
		setRenderer(renderClass);
		if (renderer == null) {
			throw new IllegalArgumentException("No renderer!");
		}
	}

	protected void updateCache(int len) {
		if (len != oldLen) {
			bars = new float[len];

			float base = 1.0f / levels;
			int max = model.getMaxBar(len, base);
			for (int i = 0; i < len; i++) {
				float x = i * 1.0f / len;
				int count = model.count(x - base / 2, x + base / 2);
				bars[i] = Math.min(1f, count * 1.f / max);
			}

			oldLen = len;
		}
	}

	public void paint(Graphics g) {
		super.paint(g);
		updateCache(renderer.getBarCount(this));
		renderer.paint(g, this);

		if (getHeight() > 15) {
			float w = getWidth();
			Graphics2D g2d = (Graphics2D) g;
			Rectangle2D rs = g2d.getFontMetrics().getStringBounds("similar",
					g2d);
			rs.setRect(2 + w / 5, 0, rs.getWidth(), rs.getHeight() + 1);
			Rectangle2D rd = g2d.getFontMetrics().getStringBounds("different",
					g2d);
			rd.setRect(w - rd.getWidth() - w / 5, 0, rd.getWidth(), rd
					.getHeight() + 1);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setComposite(AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER, 0.6f));

			//            g2d.setColor(Color.lightGray);
			//            g2d.fill(rs);
			//            g2d.fill(rd);

			g2d.setColor(Color.black);

			double aw = Arrow.getArrowWidth();
			g2d
					.draw(Arrow.getArrow(false).createTransformedShape(
							AffineTransform.getTranslateInstance(rs.getMinX()
									- aw, 5)));
			g2d.draw(Arrow.getArrow(true).createTransformedShape(
					AffineTransform.getTranslateInstance(rd.getMaxX(), 5)));

			g2d.drawString("similar", (float) rs.getX(),
					(float) rs.getHeight() - 1);
			g2d.drawString("different", (float) rd.getX(), (float) rd
					.getHeight() - 1);
			g2d.setComposite(AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER, 1f));
		}
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setCurrent(float current) {
		this.current = current;
	}

	public void setLevels(int levels) {
		this.levels = levels;
	}

	public void setModel(HistogramModel model) {
		boolean mustRepaint = (model != this.model);
		this.model = model;
		if (mustRepaint) {
			oldLen = -1;
			repaint();
		}
	}

	public HistogramModel getModel() {
		return model;
	}

	public Dimension getPreferredSize() {
		return new Dimension(200, 70);
	}

	public float getCurrent() {
		return current;
	}

	public int getLevels() {
		return levels;
	}

	public String getText() {
		return text;
	}

	public float[] getBars() {
		return bars;
	}

	public float getMaxBar() {
		return maxBar;
	}

	public HistogramRenderer getRenderer() {
		return renderer;
	}

	public void setRenderer(Class<? extends HistogramRenderer> c) {
		try {
			if (!renderers.containsKey(c)) {
				HistogramRenderer r = c.getConstructor().newInstance();
				renderers.put(c, r);
			}
			renderer = renderers.get(c);
		} catch (Exception e) {
			System.err
					.println("Error: imposible to instantiate renderer of class "
							+ c.getName());
			e.printStackTrace();
		}
	}

	public void setRenderer(HistogramRenderer renderer) {
		this.renderer = renderer;
	}
}