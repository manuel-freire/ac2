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

import java.util.ArrayList;

import java.util.HashMap;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.JViewport;

/**
 * A vertical fisheye layout manager
 *
 * @author mfreire
 */
public class VerticalFisheyeLayout implements LayoutManager,
		MouseMotionListener {

	private int minHeight = 9;
	private int maxHeight = 60;
	private int radius = 100;
	private int margin = 1;
	private int lastY = -radius;

	public VerticalFisheyeLayout(int minHeight, int maxHeight, int radius,
			int margin) {
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
		this.radius = radius;
		this.margin = margin;
	}

	private ArrayList<CompData> comps = new ArrayList<CompData>();
	private HashMap<Component, CompData> compToData = new HashMap<Component, CompData>();

	public void addLayoutComponent(String name, Component comp) {
		CompData cd = new CompData(comp);
		compToData.put(comp, cd);
		comps.add(cd);
		//System.err.println("Adding comp: "+comp);
	}

	public void removeLayoutComponent(Component comp) {
		comps.remove(compToData.get(comp));
		compToData.remove(comp);
	}

	public Dimension preferredLayoutSize(Container parent) {
		int width = (parent.getParent() instanceof JViewport) ? ((JViewport) parent
				.getParent()).getWidth()
				: parent.getWidth();

		return new Dimension(width, estimateSize(lastY == -1 ? comps.size() / 2
				: lastY));
	}

	public Dimension minimumLayoutSize(Container parent) {
		return preferredLayoutSize(parent);
	}

	@SuppressWarnings("unused")
	private int estimateSize(int focusPos) {
		int y = 0;
		int dy = 0;
		for (CompData cd : comps) {
			int d = Math.abs(y + dy - focusPos);
			int height = minHeight;

			if (d < radius) {
				int added = (int) ((maxHeight - minHeight) * Math.cos(d * 1.0
						/ radius * (Math.PI / 2)));
				height += added;
				dy += added;
			}
			y += height + margin;
		}
		return y;
	}

	public void layoutContainer(Container parent) {
		int width = (parent.getParent() instanceof JViewport) ? ((JViewport) parent
				.getParent()).getWidth()
				: parent.getWidth();

		for (Component c : parent.getComponents()) {
			if (!compToData.containsKey(c))
				addLayoutComponent(null, c);
		}

		int y = 0;
		int dy = 0;
		for (CompData cd : comps) {
			int d = Math.abs(y + dy - lastY);
			int height = minHeight;

			if (d < radius) {
				int added = (int) ((maxHeight - minHeight) * Math.cos(d * 1.0
						/ radius * (Math.PI / 2)));
				height += added;
				dy += added;
			}

			//System.out.println("y = " + y + ": height set to "+height);

			cd.r.setBounds(0, y, width, height);
			//System.err.println("Setting bounds: "+cd.r);
			cd.c.setBounds(cd.r);
			y += height + margin;
		}
	}

	public void mouseDragged(MouseEvent e) {
		// ignored
	}

	public void mouseMoved(MouseEvent e) {
		if (e.getY() != lastY) {
			lastY = e.getY();
			// System.err.println("=== Repaint triggered; y = "+lastY);
			layoutContainer((Container) e.getComponent());
		}
	}

	private static class CompData {
		Component c;
		Rectangle r;

		public CompData(Component c) {
			this.c = c;
			this.r = new Rectangle();
		}
	}
}
