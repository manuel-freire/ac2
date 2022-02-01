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

import es.ucm.fdi.clover.event.HierarchyChangeEvent;
import es.ucm.fdi.clover.model.Cluster;
import es.ucm.fdi.clover.model.ClusterHierarchy;
import es.ucm.fdi.clover.model.ClusteredGraph;
import javax.swing.*;
import es.ucm.fdi.clover.gui.BaseInterface;

/**
 * Starts editing a given cell
 *
 * @author  mfreire
 */
public class EditAction extends BaseAction implements ContextAwareAction {

	/** Creates a new instance of FreezeAction */
	public EditAction(BaseInterface app) {
		super(app, "edit", "img/redo.png",
				"View or change this object's properties");

		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_E,
				java.awt.event.InputEvent.CTRL_MASK));
	}

	/**
	 * Default is to make only "Clusters" editable
	 */
	public void actionPerformed(java.awt.event.ActionEvent e) {
		Object o = getOperand(e);
		if (isValidOperand(o)) {
			String s = JOptionPane
					.showInputDialog("Enter new name for cluster");
			if (s != null) {
				((Cluster.Vertex) o).getCluster().setName(s);
				ClusterHierarchy h = ((ClusteredGraph) getView(e).getBase())
						.getHierarchy();
				HierarchyChangeEvent evt = new HierarchyChangeEvent(h,
						"name change");
				evt.getChangedClusters().add(((Cluster.Vertex) o).getCluster());
				evt.augmentChangesWithAddedAndRemoved();
				h.hierarchyChangePerformed(evt);
			}
		}
	}

	public boolean isValidOperand(Object o) {
		return (o instanceof Cluster.Vertex);
	}
}
