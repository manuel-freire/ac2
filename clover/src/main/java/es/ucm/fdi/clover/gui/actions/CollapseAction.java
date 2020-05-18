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
 * FreezeAction.java
 *
 * Created on April 30, 2004, 6:02 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.gui.actions;

import es.ucm.fdi.clover.model.Cluster;
import es.ucm.fdi.clover.model.ClusteredGraph;
import es.ucm.fdi.clover.view.ClusterView;
import javax.swing.*;
import java.util.Properties;

import es.ucm.fdi.clover.gui.BaseInterface;
import org.jgraph.graph.DefaultGraphCell;

/**
 * Collapse a cluster, which must have visible children (and therefore not
 * be visible itself).
 * After the collapse, none of these children will be visible.
 *
 * @author  mfreire
 */
public class CollapseAction extends BaseAction {

	public CollapseAction(BaseInterface app) {
		super(app, "collapse cluster", "img/zoomout.png",
				"Collapse, so any descendants of this cluster become hidden;"
						+ " also works for leaves.");
	}

	public void actionPerformed(java.awt.event.ActionEvent e) {
		ClusterView v = getView(e);
		ClusteredGraph g = (ClusteredGraph) v.getBase();

		Object o = getOperand(e);
		Cluster p = g.getHierarchy().getRoot().clusterForVertex(o)
				.getParentCluster();
		System.err.println("Collapsing : " + p.getVertex());
		g.clusteringChangePerformed(g.createCollapseEvent(p.getVertex()));
	}
}
