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
 * Freezes a cell into its position. This action is a toggle (invoking twice
 * unfreezes)
 *
 * @author  mfreire
 */
public class SetFocusAction extends BaseAction {

	public SetFocusAction(BaseInterface app) {
		super(app, "set focus", "img/frozen.png",
				"Set this vertex as the current focus");
	}

	public void actionPerformed(java.awt.event.ActionEvent e) {
		Object o = getOperand(e);
		getView(e).setCurrentPoI(o);
	}
}