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
 * FreezeAction.java
 *
 * Created on April 30, 2004, 6:02 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.gui.actions;

import javax.swing.*;
import java.util.Properties;
import java.awt.*;

import es.ucm.fdi.clover.gui.BaseInterface;

/**
 *
 * @author  mfreire
 */
public class ZoomOutAction extends BaseAction {

	public ZoomOutAction(BaseInterface app) {
		super(app, "zoom out", "img/zoomout.png",
				"Zoom out, and see more but smaller");
	}

	public void actionPerformed(java.awt.event.ActionEvent e) {
		getView(e).setScale(getView(e).getScale() / 1.5);
	}
}
