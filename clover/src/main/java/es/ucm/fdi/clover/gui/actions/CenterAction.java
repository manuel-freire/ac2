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
package es.ucm.fdi.clover.gui.actions;

import es.ucm.fdi.clover.view.BaseView;
import es.ucm.fdi.clover.view.ViewHelper;

import es.ucm.fdi.clover.gui.BaseInterface;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JViewport;
import org.jgraph.graph.GraphCell;

/**
 * Action that centers the graph display on a given cell.
 * Should be triggered from popup, or the operand set otherwise.
 *
 * @author  mfreire
 */
public class CenterAction extends BaseAction {

	/** Creates a new instance of FreezeAction */
	public CenterAction(BaseInterface app) {
		super(app, "center", "img/center.png",
				"Place this point/cell in the center of the graph");
	}

	public void actionPerformed(java.awt.event.ActionEvent e) {
		BaseView v = getApp(e).getView();
		Object o = getOperand(e);

		Point2D desiredCenter;
		GraphCell cell = ViewHelper.getVertexCell(v, o);
		if (cell != null) {
			Rectangle2D r = v.getCellBounds(cell);
			desiredCenter = new Point2D.Double(r.getCenterX(), r.getCenterY());
			desiredCenter = v.toScreen(desiredCenter);
		} else if (getPoint(e) != null) {
			JViewport jvp = (JViewport) v.getParent();
			desiredCenter = jvp.toViewCoordinates(getPoint(e));
		} else {
			System.err.println("Bad event " + e);
			return;
		}

		v.setCenter(desiredCenter);
	}
}
