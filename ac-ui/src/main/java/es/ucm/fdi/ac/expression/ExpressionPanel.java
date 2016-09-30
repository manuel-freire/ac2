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
 * ExpressionPanel.java
 *
 * Created on September 16, 2006, 2:03 PM
 *
 */

package es.ucm.fdi.ac.expression;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.InvalidDnDOperationException;
import java.io.IOException;

import javax.swing.JPanel;
import java.util.ArrayList;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * The common abstract class for all expression panels (= a component that
 * shows an expression, with or without children).
 *
 * @author mfreire
 */
public abstract class ExpressionPanel extends JPanel {

	protected boolean selected;
	protected CompositeExpressionPanel parentPanel;
	protected ArrayList<ExpressionListener> el;

	private DragSource dragSource;
	private DragGestureListener dgListener;
	private DragSourceListener dsListener;
	private DropTargetListener dtListener;
	private DropTarget dropTarget;

	public ExpressionPanel(CompositeExpressionPanel parentPanel) {
		this.el = new ArrayList<ExpressionListener>();
		this.parentPanel = parentPanel;

		if (parentPanel != null) {
			float f = 0.6f;
			CompositeExpressionPanel p = parentPanel;
			while (p != null) {
				f += 0.1f;
				p = p.parentPanel;
			}
			setBorder(new CompoundBorder(new EmptyBorder(2, 1, 2, 1),
					new LineBorder(Color.getHSBColor(f, 0.5f, 1), 4)));
		}

		this.dragSource = DragSource.getDefaultDragSource();
		this.dgListener = new DGListener();
		this.dsListener = new DSListener();
		this.dtListener = new DTListener();

		// component, ops, listener, accepting
		this.dropTarget = new DropTarget(this,
				DnDConstants.ACTION_COPY_OR_MOVE, dtListener, true);
		// component, action, listener
		this.dragSource.createDefaultDragGestureRecognizer(this,
				DnDConstants.ACTION_COPY_OR_MOVE, this.dgListener);
	}

	private class DTListener implements DropTargetListener {
		public void dragEnter(DropTargetDragEvent e) {
			System.err.println("Howdy!!");
			if (!isDragOk(e)) {
				e.rejectDrag();
				return;
			}
			ExpressionPanel.this.setBackground(Color.green);
			e.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
		}

		public void dragOver(DropTargetDragEvent e) {
			System.err.println("1 Howdy!!");
			if (!isDragOk(e)) {
				e.rejectDrag();
				return;
			}
			e.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
		}

		public void dropActionChanged(DropTargetDragEvent e) {
			System.err.println("2 Howdy!!");
			if (!isDragOk(e)) {
				e.rejectDrag();
				return;
			}
			e.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
		}

		public void dragExit(DropTargetEvent e) {
			System.err.println("3 Howdy!!");
			ExpressionPanel.this.setBackground(Color.blue);
		}

		public void drop(DropTargetDropEvent dtde) {
			System.err.println("DroppinG!");
		}

		public boolean isDragOk(DropTargetDragEvent e) {
			System.err.println("Yes, its okay !!! Do it !!!");
			return true;
		}
	}

	protected DragSourceListener getDragSourceListener() {
		return dsListener;
	}

	public static class DGListener implements DragGestureListener {
		public void dragGestureRecognized(DragGestureEvent dge) {
			System.err
					.println("Starting drag gesture on " + dge.getComponent());
			{
				try {
					Transferable transferable = new ExpressionTransferable(dge
							.getComponent());
					//initial cursor, transferable, dsource listener
					dge.startDrag(DragSource.DefaultCopyNoDrop, transferable,
							((ExpressionPanel) dge.getComponent())
									.getDragSourceListener());
				} catch (InvalidDnDOperationException idoe) {
					System.err.println(idoe);
				}
			}
		}
	}

	public static class DSListener implements DragSourceListener {
		public void dragEnter(DragSourceDragEvent dsde) {
			System.err.println("Drag enter: " + dsde);
		}

		public void dragOver(DragSourceDragEvent dsde) {
			System.err.println("Drag over: " + dsde);
		}

		public void dropActionChanged(DragSourceDragEvent dsde) {
			System.err.println("Drop act. changed: " + dsde);
		}

		public void dragExit(DragSourceEvent dse) {
			System.err.println("Drop exit: " + dse);
		}

		public void dragDropEnd(DragSourceDropEvent dsde) {
			System.err.println("Drop end: " + dsde);
		}
	}

	public static class ExpressionTransferable implements Transferable {
		private ExpressionPanel data;
		public static DataFlavor[] flavors = new DataFlavor[] { new DataFlavor(
				ExpressionPanel.class, "ExpressionPanel") };

		public ExpressionTransferable(Object o) {
			data = (ExpressionPanel) o;
		}

		public DataFlavor[] getTransferDataFlavors() {
			System.err.println("Called!");
			return flavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			System.err.println("Queried flavor " + flavor.toString());
			return true;
		}

		public Object getTransferData(DataFlavor flavor)
				throws UnsupportedFlavorException, IOException {
			System.err.println("Queried flavor " + flavor.toString()
					+ " for DATA!");
			return data;
		}
	}

	public void addExpressionListener(ExpressionListener l) {
		el.add(l);
	}

	public void removeExpressionListener(ExpressionListener l) {
		el.remove(l);
	}

	/**
	 * Avoids having to set listeners for all branches -
	 * leave local list empty to inherit
	 */
	private ArrayList<ExpressionListener> getExpressionListeners() {
		ArrayList<ExpressionListener> list = el;
		ExpressionPanel ep = this;
		while (ep.el.isEmpty() && ep.parentPanel != null) {
			ep = ep.parentPanel;
		}

		list = ep.el;

		return list;
	}

	public void test(boolean wasTest) {
		for (ExpressionListener l : getExpressionListeners()) {
			l.expressionChanged(getExpression(), wasTest);
			//            System.err.println("Notifying "+l);
		}
	}

	public boolean isSelected() {
		return selected;
	}

	private static Color defaultBackground = null;
	private static Color selectedBackground = null;

	public void setSelected(boolean selected) {
		if (defaultBackground == null) {
			defaultBackground = getBackground();
			selectedBackground = Color.getHSBColor(0.6f, 0.3f, 1f);
		}

		this.selected = selected;
		Color back = selected ? selectedBackground : defaultBackground;
		fillWithColor(this, back);
	}

	private void fillWithColor(Component comp, Color c) {
		if (comp instanceof JPanel) {
			comp.setBackground(c);
			comp.repaint();
			for (Component child : ((Container) comp).getComponents()) {
				fillWithColor(child, c);
			}
		}
	}

	private static Image background;

	public abstract void setExpression(Expression e);

	public abstract Expression getExpression();
}
