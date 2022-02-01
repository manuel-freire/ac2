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

import javax.swing.*;
import java.util.Properties;

import es.ucm.fdi.clover.gui.BaseInterface;
import org.jgraph.graph.DefaultGraphCell;

/**
 * Increase cluster size (and therefore, decrease cluster number)
 *
 * @author  mfreire
 */
public class UndoNavAction extends BaseAction {

	public UndoNavAction(BaseInterface app) {
		super(app, "undo nav", "img/backward.png",
				"Undo last navigational Action");
	}

	public void actionPerformed(java.awt.event.ActionEvent e) {
		getView(e).prevNavAction();
	}
}
