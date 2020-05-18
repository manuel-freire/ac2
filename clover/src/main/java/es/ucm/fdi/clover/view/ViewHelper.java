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
 * ViewHelper.java
 *
 * Created on May 15, 2006, 7:58 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.view;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;

import es.ucm.fdi.clover.model.Edge;

import org.jgrapht.ext.JGraphModelAdapter;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.Port;

/**
 * A helper class to ease the transition from Cells to Vertices and viceversa,
 * and from graph coordinates to cells.
 *
 * @author mfreire
 */
public class ViewHelper {

	public static BaseView getView(MouseEvent e) {
		return (e.getComponent() instanceof BaseView) ? (BaseView) e
				.getComponent() : null;
	}

	public static DefaultGraphCell getCell(MouseEvent e) {
		BaseView view = getView(e);
		return (DefaultGraphCell) view.getNextCellForLocation(null, e.getX(), e
				.getY());
	}

	public static DefaultGraphCell getCell(BaseView view, Point p) {
		return (DefaultGraphCell) view.getNextCellForLocation(null, p.getX(), p
				.getY());
	}

	/**
	 * Retrieve the first vertex cell found for a given MouseEvent point.
	 */
	public static DefaultGraphCell getVertexCell(MouseEvent e) {
		BaseView view = getView(e);
		if (view == null) {
			return null;
		}

		Object prev = null;

		Object o = view.getNextCellForLocation(prev, e.getX(), e.getY());
		while (!(o == null) && !(isVertex((DefaultGraphCell) o))
				&& !(o == prev)) {
			o = view.getNextCellForLocation(prev, e.getX(), e.getY());
			prev = o;
		}
		return (DefaultGraphCell) o;
	}

	/**
	 * Retrieve the first edge cell found for a given MouseEvent point.
	 */
	public static DefaultGraphCell getEdgeCell(MouseEvent e) {
		BaseView view = getView(e);
		if (view == null) {
			return null;
		}

		Object prev = null;

		Object o = view.getNextCellForLocation(prev, e.getX(), e.getY());
		while (!(o == null) && !(isEdge((DefaultGraphCell) o)) && !(o == prev)) {
			o = view.getNextCellForLocation(prev, e.getX(), e.getY());
			prev = o;
		}
		return (DefaultGraphCell) o;
	}

	public static DefaultGraphCell getVertexCell(BaseView view, Object v) {
		JGraphModelAdapter adapter = view.getViewGraph().getModelAdapter();
		return adapter.getVertexCell(v);
	}

	public static ArrayList getVertices(BaseView view,
			Collection<DefaultGraphCell> cells) {
		JGraphModelAdapter adapter = view.getViewGraph().getModelAdapter();
		ArrayList al = new ArrayList();
		for (DefaultGraphCell c : cells) {
			al.add(adapter.getVertexCell(c));
		}
		return al;
	}

	public static DefaultEdge getEdgeCell(BaseView view, Edge e) {
		JGraphModelAdapter adapter = view.getViewGraph().getModelAdapter();
		return adapter.getEdgeCell(e);
	}

	public static boolean isVertex(DefaultGraphCell cell) {
		return (!(cell instanceof DefaultEdge)) && (!(cell instanceof Port));
	}

	public static boolean isEdge(DefaultGraphCell cell) {
		return (cell instanceof DefaultEdge);
	}

	public static Object getVertex(DefaultGraphCell cell) {
		if (cell == null || !isVertex(cell)) {
			return null;
		}
		return cell.getUserObject();
	}

	public static Edge getEdge(DefaultGraphCell cell) {
		if (cell == null || !isEdge(cell)) {
			return null;
		}
		return (Edge) cell.getUserObject();
	}
}
