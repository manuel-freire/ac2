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
 * TableViz.java
 *
 * Created on December 21, 2007, 3:55 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package es.ucm.fdi.ac.tableviz;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashMap;
import javax.swing.JPanel;

/**
 * A zoomable table visualization. 
 * Uses pixel-display techniques to show a large amount of data at once.
 *
 * @author mfreire
 */
public class TableViz extends JPanel {

	private Dimension preferredSize = new Dimension(200, 200);

	private TableModel model;
	private TableRenderer renderer;
	private static HashMap<Class, TableRenderer> renderers = new HashMap<Class, TableRenderer>();

	public TableViz(TableModel model, Class renderClass) {
		setModel(model);
		setRenderer(renderClass);
		if (renderer == null) {
			throw new IllegalArgumentException("No renderer!");
		}
		addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				int n = e.getWheelRotation();
				updatePreferredSize(n);
			}
		});
	}

	private void updatePreferredSize(int n) {
		n = -n;
		double d = (double) n * 1.08;
		d = (n < 0) ? -1 / d : d;
		//        System.err.println("n = "+n+"; d = "+d);
		int w = (int) (getWidth() * d);
		int h = (int) (getHeight() * d);
		//        System.err.println("Preferred size from "+getWidth()+"x"+getHeight()+" to "+w+"x"+h+" (factor of "+d+")");
		preferredSize.setSize(w, h);
		getParent().doLayout();
	}

	public Dimension getPreferredSize() {
		return preferredSize;
	}

	public void paint(Graphics g) {
		super.paint(g);
		renderer.paint(g, this);
	}

	public void setModel(TableModel model) {
		boolean mustRepaint = (model != this.model);
		this.model = model;
		if (mustRepaint) {
			repaint();
		}
	}

	public TableModel getModel() {
		return model;
	}

	public TableRenderer getRenderer() {
		return renderer;
	}

	public void setRenderer(Class c) {
		try {
			if (!renderers.containsKey(c)) {
				TableRenderer r = (TableRenderer) c.newInstance();
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

	public void setRenderer(TableRenderer renderer) {
		this.renderer = renderer;
	}
}
