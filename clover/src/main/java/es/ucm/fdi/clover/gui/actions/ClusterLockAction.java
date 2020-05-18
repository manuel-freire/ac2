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
 * ClusterLockAction.java
 *
 * Created on November 7, 2004
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.gui.actions;

import es.ucm.fdi.clover.view.ClusterView;
import javax.swing.*;
import java.util.Properties;

import es.ucm.fdi.clover.gui.BaseInterface;

/**
 *
 * @author  mfreire
 */
public class ClusterLockAction extends BaseAction {

	public ClusterLockAction(BaseInterface app) {
		super(app, "lock clustering", "img/clusterunlock.png",
				"Toggle clustering changes on/off");
	}

	public void actionPerformed(java.awt.event.ActionEvent e) {
		ClusterView view = getView(e);
		view.setClusterLock(!view.isClusterLock());
		if (view.isClusterLock()) {
			putValue(Action.SMALL_ICON, getIcon("img/clusterlock.png"));
		} else {
			putValue(Action.SMALL_ICON, getIcon("img/clusterunlock.png"));
		}

	}
}
