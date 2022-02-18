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
import es.ucm.fdi.clover.model.Cluster;
import es.ucm.fdi.clover.model.ClusteredGraph;
import es.ucm.fdi.clover.view.ClusterView;

/**
 * Expand the current cluster, which must be visible (or have a visible ancestor).
 * All children of this cluster will be visible after his operation.
 *
 * @author  mfreire
 */
public class ExpandAction extends BaseAction {

	public ExpandAction(BaseInterface app) {
		super(app, "expand cluster", "img/zoomin.png",
				"Expand, so descendants of this cluster become visible");
	}

	public void actionPerformed(java.awt.event.ActionEvent e) {
		ClusterView v = getView(e);
		ClusteredGraph g = (ClusteredGraph) v.getBase();

		Object o = getOperand(e);
		Cluster c = g.getHierarchy().getRoot().clusterForVertex(o);
		Object ex = ((Cluster) c.getFirstChild()).getVertex();
		System.err.println("Expanding : " + ex);
		g.clusteringChangePerformed(g.createExpandEvent(ex));
	}
}
