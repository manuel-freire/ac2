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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.ucm.fdi.ac.gui;

import es.ucm.fdi.ac.ptrie.Location;
import es.ucm.fdi.ac.ptrie.Node;
import es.ucm.fdi.ac.stringmap.Mapper;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import org.syntax.jedit.JEditTextArea;
import org.syntax.jedit.TextAreaPainter.Highlight;

/**
 *
 * @author mfreire
 */
public class CommonHighlighter extends JPopupMenu implements Highlight {

	private JEditTextArea ta;
	private Highlight next;
	// list of (colored) intervals
	private ArrayList<Interval> intervals;
	private CommonHighlighter peer;

	/**
	 * Allows one highlighter to reach out and touch another highlighter
	 * (mostly for scrolling-the-textarea purposes).
	 * @param peer
	 */
	public void setPeer(CommonHighlighter peer) {
		this.peer = peer;
	}

	public CommonHighlighter(ArrayList<Node> sel, Mapper m, final Object base) {
		this.intervals = new ArrayList<Interval>(sel.size());

		// for debugging only (print in order); disable to get consistent colors!
		//            sortNodes(sel, base);

		// color increment
		float ci = 1f / sel.size();
		for (int i = 0; i < sel.size(); i++) {
			Node n = sel.get(i);
			int j = (i % 2 == 0) ? i / 2 : sel.size() / 2 + i;
			Color color = Color.getHSBColor(j * ci, 0.10f, 1f);
			for (Location l : n.getLocations()) {
				if (l.getBase() == base) {
					Interval in = new Interval(m.rmap(l.getOffset(), true), m
							.rmap(l.getOffset() + n.getStringLength(), false),
							color, n, l);
					// debugging
					//                        showSnip("" + base + " " + in.so + "->" + in.eo + " : ", 
					//                                n.getString());
					intervals.add(in);
				}
			}
		}
	}

	private static void sortNodes(ArrayList<Node> sel, final Object base) {
		Collections.sort(sel, new Comparator<Node>() {

			public int compare(Node o1, Node o2) {
				int low1 = Integer.MAX_VALUE;
				int low2 = Integer.MAX_VALUE;
				for (Location l : o1.getLocations()) {
					if (l.getBase() == base) {
						low1 = Math.min(low1, l.getOffset());
					}
				}
				for (Location l : o2.getLocations()) {
					if (l.getBase() == base) {
						low2 = Math.min(low2, l.getOffset());
					}
				}
				return low1 - low2;
			}
		});
	}

	/**
	 * Debugging - show (abridged) interval of a long string
	 */
	private static void showSnip(String prefix, String s) {
		String fixed = s.replaceAll("\\p{javaWhitespace}", "_");
		System.err.println(prefix
				+ fixed.substring(0, Math.min(fixed.length(), 20)) + " ... "
				+ fixed.substring(Math.max(0, fixed.length() - 20)) + " ("
				+ s.length() + ")");
	}

	/**
	 * Populates the menu when an offset is clicked
	 * @param offset
	 */
	private void populate(int offset) {
		// clear all contents
		removeAll();

		int nIntervals = 0;

		// iterate for all intervals, to find the ones that apply
		for (Interval in : intervals) {

			// if not in interval, skip it!
			if (in.so > offset || offset > in.eo) {
				continue;
			}

			// separator from previous ones (only if /not/ first)
			if (nIntervals++ > 0) {
				add(new JSeparator(JSeparator.HORIZONTAL));
			}

			// color-coded label
			int lo = ta.getLineOfOffset(offset);
			int loo = offset - ta.getLineStartOffset(lo);
			JLabel jl = new JLabel("#" + nIntervals + ": " + (in.eo - in.so)
					+ " chars @" + lo + ":" + loo);
			jl.setBackground(in.color);
			jl.setOpaque(true);
			add(jl);

			// if the interval node repeats, offer to scroll to next/prev
			for (Interval other : intervals) {
				if (other.node == in.node && other.loc != in.loc) {
					int line = ta.getLineOfOffset(other.so);
					int lineOffset = other.so - ta.getLineStartOffset(line);
					JumpItem ji = new JumpItem(
							" at " + line + ":" + lineOffset, ta, line,
							lineOffset);
					add(ji);
				}
			}

			// offer to jump to other side's equivalent interval (will exist)
			for (Interval other : peer.intervals) {
				if (other.node == in.node) {
					int line = peer.ta.getLineOfOffset(other.so);
					int lineOffset = other.so
							- peer.ta.getLineStartOffset(line);
					JumpItem ji = new JumpItem(" there at " + line + ":"
							+ lineOffset, peer.ta, line, lineOffset);
					add(ji);
				}
			}
		}
	}

	private static class JumpItem extends JMenuItem implements ActionListener {
		private int line;
		private int lineOffset;
		private JEditTextArea area;

		public JumpItem(String text, JEditTextArea area, int line,
				int lineOffset) {
			super(text);
			this.area = area;
			this.line = line;
			this.lineOffset = lineOffset;
			addActionListener(this);
		}

		public void actionPerformed(ActionEvent evt) {
			area.scrollTo(line, lineOffset);
		}
	}

	@Override
	public void show(Component invoker, int x, int y) {
		populate(ta.xyToOffset(x, y));
		super.show(invoker, x, y);
	}

	private static class Interval {
		public int so, eo; // start, end
		public Color color;
		public Node node;
		public Location loc;

		public Interval(int so, int eo, Color color, Node node, Location loc) {
			this.so = so;
			this.eo = eo;
			this.color = color;
			this.node = node;
			this.loc = loc;
		}
	}

	public void init(JEditTextArea textArea, Highlight next) {
		this.ta = textArea;
		this.next = next;
	}

	public void paintHighlight(Graphics gfx, int line, int y) {
		FontMetrics fm = ta.getPainter().getFontMetrics();
		y += fm.getLeading() + fm.getMaxDescent();
		int height = fm.getHeight();

		// data of line to draw
		int lineStart = ta.getLineStartOffset(line);
		int lineEnd = ta.getLineEndOffset(line);

		int x1, x2;
		for (Interval in : intervals) {

			// if no line extremes in interval, and interval not within extremes, skip
			if ((in.so > lineStart || lineStart > in.eo)
					&& (in.so > lineEnd || lineEnd > in.eo)
					&& !(in.so > lineStart && lineEnd > in.eo)) {
				continue;
			}

			int sl = ta.getLineOfOffset(in.so);
			int el = ta.getLineOfOffset(in.eo);

			// find start and end offset
			if (sl == el) { // single-line
				x1 = ta._offsetToX(line, in.so - lineStart);
				x2 = ta._offsetToX(line, in.eo - lineStart);
			} else if (line == sl) { // start of multiple-line
				x1 = ta._offsetToX(line, in.so - lineStart);
				x2 = ta.getWidth();
				//                    showSnip("highlightin' start : ", ta.getText(in.so, in.eo));
				//                    showSnip("should match ", in.node.getString());
			} else if (line == el) { // end of multiple-line
				x1 = 0;
				x2 = ta._offsetToX(line, in.eo - lineStart);
				//                    showSnip("highlightin' end : ", ta.getText(in.so, in.eo));
				//                    showSnip("should match ", in.node.getString());
			} else { // middle of multiple-line
				x1 = 0;
				x2 = ta.getWidth();
			}

			gfx.setColor(in.color);
			gfx.setXORMode(Color.WHITE);
			if (x1 > x2) {
				gfx.fillRect(x2, y, (x1 - x2), height);
			} else {
				gfx.fillRect(x1, y, (x2 - x1), height);
			}
			gfx.setPaintMode();
		}
	}

	private String oldToolTip = "";

	@Override
	public String getToolTipText(MouseEvent evt) {
		int pos = ta.xyToOffset(evt.getX(), evt.getY());
		int line = ta.getLineOfOffset(pos);
		int linePos = pos - ta.getLineStartOffset(line);
		String newToolTip = "" + line + ":" + linePos + " [" + pos + "]";
		if (!newToolTip.equals(oldToolTip)) {
			oldToolTip = newToolTip;
			ta.repaint();
		}
		return newToolTip;
	}
}
