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
package es.ucm.fdi.clover.test;

import es.ucm.fdi.clover.gui.BaseInterface;
import es.ucm.fdi.clover.model.Cluster;
import es.ucm.fdi.clover.model.Edge;
import es.ucm.fdi.clover.view.BasePopupMenu;
import es.ucm.fdi.clover.view.ViewHelper;
import java.awt.Point;
import org.jgraph.graph.DefaultGraphCell;

/**
 *
 * @author mfreire
 */
public class TestPopupMenu extends BasePopupMenu {

	/** Creates a new instance of TestPopupMenu */
	public TestPopupMenu(BaseInterface app, Object o, Point p) {
		super(app, o, p);
	}

	/**
	 * Populates the menu
	 */
	protected void populate() {
		super.populate();

		// must be a valid graph vertex
		Object o = getObject();
		Object sel = ViewHelper.getVertex((DefaultGraphCell) getApp().getView()
				.getSelectionCell());

		boolean isEmpty = (o == null);
		boolean isEdge = !isEmpty && (o instanceof Edge);
		boolean isCluster = !isEmpty && (o instanceof Cluster.Vertex);

		if (!isEmpty && (!isCluster && !isEdge)) {
			this.add(getApp().getAction("test add vertex"));
			if (sel != null && sel != o) {
				this.add(getApp().getAction("test add edge"));
			}
		}

		if (isEmpty) {
			this.add(getApp().getAction("test add vertex"));
		}

		if (!isEmpty) {
			this.add(getApp().getAction("test delete"));
		}
	}
}
