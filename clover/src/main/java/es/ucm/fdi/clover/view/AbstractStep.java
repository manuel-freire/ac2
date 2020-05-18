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
 * InterpolatedMovementStep.java
 *
 * Created on May 14, 2006, 9:27 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.view;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.jgraph.graph.GraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.VertexView;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Animates a smooth change in a series of attributes for a series of cells. 
 * The default is to not do anything with the chosen attribute - it is up
 * to subclasses to decide what should change. This is only a utility class designed
 * to make things easier. 
 *
 * @author mfreire
 */
public abstract class AbstractStep implements AnimationStep {

	protected Log log = LogFactory.getLog(AbstractStep.class);

	private long duration = 500;
	private long rate = 1000 / 25;
	private boolean finished = false;

	private ArrayList<GraphCell> cells;

	public void perform(float completion, Map changeMap) {
		if (cells == null)
			return;
		for (GraphCell cell : cells) {
			interpolate(cell, completion, changeMap);
		}
	}

	public void init() {
	}

	public ArrayList<GraphCell> getCells() {
		return cells;
	}

	public boolean isFinished() {
		return finished;
	}

	public void terminate(Map changeMap) {
		perform(1, changeMap);
		setFinished(true);
	}

	public long getDuration() {
		return duration;
	}

	public long getRate() {
		return rate;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	/**
	 * this gets executed for every cell on every pass, with p varying smoothly from 
	 * 0 to 1.
	 */
	abstract public void interpolate(GraphCell cell, float p, Map changeMap);

	public void setCells(ArrayList<GraphCell> cells) {
		this.cells = cells;
	}

	public void setCellsFromVertices(Collection vertices, BaseView view) {
		if (cells == null) {
			cells = new ArrayList<GraphCell>();
		}
		for (Object v : vertices) {
			if (ViewHelper.getVertexCell(view, v) == null) {
				System.err.println("Vertex '" + v + "' has no view (?!)");
			}
			cells.add(ViewHelper.getVertexCell(view, v));
		}
	}
}
