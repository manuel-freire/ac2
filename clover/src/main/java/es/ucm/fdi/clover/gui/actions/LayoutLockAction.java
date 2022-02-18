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

import es.ucm.fdi.clover.gui.BaseInterface;

/**
 * This action locks the layout - automatic layout will not be redone, and node
 * expansion/collapse will be stopped. No fisheye for you
 * @author  mfreire
 */
public class LayoutLockAction extends BaseAction {

	public LayoutLockAction(BaseInterface app) {
		super(app, "lock layout", "img/layoutunlock.png",
				"Toggle automatic layout on/off");
	}

	public void actionPerformed(java.awt.event.ActionEvent e) {
		//        Properties props = getApp(e).getProperties();
		//        CvGraph graph = getGraph(e);
		//        if (graph == null) return;
		//        
		//        graph.setAutoLayout( ! graph.getAutoLayout());
		//        if (graph.getAutoLayout()) {
		//            putValue(Action.SMALL_ICON, getIcon("img/layoutlock.png"));
		//        }
		//        else {
		//            putValue(Action.SMALL_ICON, getIcon("img/layoutunlock.png"));
		//        }
	}
}
