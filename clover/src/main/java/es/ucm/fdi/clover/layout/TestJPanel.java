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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;

/**
 * A very simplistic "graph display component", good for testing out
 * layouts step-by-step.
 *
 * @author mfreire
 */
public class TestJPanel extends JPanel {

	private Dimension preferredSize = new Dimension(100, 100);
	private Node[] N;

	public TestJPanel() {
		setBackground(Color.white);
	}

	public Dimension getPreferredSize() {
		return preferredSize;
	}

	public void setNodes(Node[] N) {
		this.N = N;
		repaint();
	}

	public void paint(java.awt.Graphics g) {
		super.paint(g);

		if (N == null) {
			g.drawOval(10, 10, 10, 10);
			return;
		}

		Rectangle2D b = Node.getBounds(N, 1.0);
		preferredSize.setSize(b.getWidth(), b.getHeight());

		g.setColor(Color.yellow);
		g.drawOval((int) (b.getCenterX() - b.getX()), (int) (b.getCenterY() - b
				.getY()), 5, 5);

		for (Node n : N) {

			// the displacement (rect)
			g.setColor(Color.red);
			g.drawRect((int) (n.x0 + n.dx - b.getX()), (int) (n.y0 + n.dy - b
					.getY()), (int) n.w, (int) n.h);

			// the displacement (line)
			g.setColor(Color.green);
			g.drawLine((int) (n.x - b.getX()), (int) (n.y - b.getY()),
					(int) (n.x + n.dx - b.getX()),
					(int) (n.y + n.dy - b.getY()));

			// the node
			g.setColor(Color.blue);
			g.drawRect((int) (n.x0 - b.getX()), (int) (n.y0 - b.getY()),
					(int) n.w, (int) n.h);
			g.setColor(Color.black);
			g.drawString(n.peer.toString(), (int) (n.x0 - b.getX()),
					(int) (n.y0 + n.h - b.getY()));

			// lines
			g.setColor(Color.red.darker());
			for (int j : n.edges) {
				Node m = N[n.edges[j]];
				g.drawLine((int) (m.x - b.getX()), (int) (m.y - b.getY()),
						(int) (n.x - b.getX()), (int) (n.y - b.getY()));
			}
		}
	}

}
