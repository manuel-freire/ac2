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
 * CreateViewAction.java
 *
 * Created on April 30, 2004, 6:02 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.gui.actions;

import es.ucm.fdi.clover.model.ClusterViewGraph;
import es.ucm.fdi.clover.model.ClusteredGraph;
import es.ucm.fdi.clover.view.ClusterView;
import javax.swing.*;
import es.ucm.fdi.clover.gui.BaseInterface;

import java.util.*;

/**
 * Action that creates a new view with the same settings as this one.
 *
 * @author  mfreire
 */
public class CreateViewAction extends BaseAction {

	public CreateViewAction(BaseInterface app) {
		super(app, "create view", "img/insert.png",
				"Create a new view of this graph");
	}

	public void actionPerformed(java.awt.event.ActionEvent e) {
		ClusteredGraph cg = (ClusteredGraph) getView(e).getBase();
		getApp(e).createNewGraphView();
	}
}
