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

package es.ucm.fdi.clover.test;

import es.ucm.fdi.clover.event.StructureChangeEvent;
import es.ucm.fdi.clover.gui.actions.BaseAction;
import es.ucm.fdi.clover.model.Cluster;
import es.ucm.fdi.clover.model.ClusteredGraph;

import es.ucm.fdi.clover.gui.BaseInterface;
import es.ucm.fdi.clover.model.Edge;
import es.ucm.fdi.clover.view.ViewHelper;
import org.jgraph.graph.DefaultGraphCell;

/**
 * Increase cluster size (and therefore, decrease cluster number)
 *
 * @author  mfreire
 */
public class TestAddEdgeAction extends BaseAction {

	public TestAddEdgeAction(BaseInterface app) {
		super(app, "test add edge", "img/insert.png",
				"Insert an edge into the graph, uniting two vertices");
	}

	public void actionPerformed(java.awt.event.ActionEvent e) {

		Object o = getOperand(e);
		if (o instanceof Cluster.Vertex) {
			// not adding anything to an "intermediate" vertex; muck up my model
			return;
		}

		Object src = ViewHelper.getVertex((DefaultGraphCell) getApp(e)
				.getView().getSelectionCell());

		ClusteredGraph cg = (ClusteredGraph) getApp(e).getView().getBase();
		TestGraph tg = (TestGraph) cg.getBase();

		StructureChangeEvent sce = new StructureChangeEvent(tg);
		sce.getAddedEdges().add(new Edge(src, o));
		tg.structureChangePerformed(sce);
	}
}
