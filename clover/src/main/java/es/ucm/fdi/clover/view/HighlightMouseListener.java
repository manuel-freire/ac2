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
 * HighlightMouseListener.java
 *
 * Created on May 15, 2006, 8:25 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.view;

import es.ucm.fdi.clover.model.Edge;
import es.ucm.fdi.clover.model.ViewGraph;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphCell;
import org.jgraph.graph.GraphConstants;

/**
 * Highlights the currently hovered-over cell and all its outgoing and incoming
 * edges. Demonstrates the use of listeners and plans and animators to achieve
 * interaction.
 *
 * @author mfreire
 */
public class HighlightMouseListener extends MouseMotionAdapter {

	static private Logger log = Logger.getLogger(HighlightMouseListener.class);

	private BaseView view;

	private DefaultGraphCell current;

	private HashSet<AnimationPlan> runningPlans;

	/**
	 * Creates a new instance of HighlightMouseListener
	 */
	public HighlightMouseListener(BaseView view) {
		this.view = view;
		runningPlans = new HashSet<AnimationPlan>();
	}

	/**
	 * Exit from the previous cell, if any - and highlight this cell
	 * and all incoming and outgoing edges
	 */
	public void mouseMoved(MouseEvent e) {
		DefaultGraphCell c = ViewHelper.getCell(e);

		if (c == null && current != null) {
			unhighlight(current);
			current = null;
		}

		if (c == null || c == current) {
			return;
		}

		log.debug("mouse entered: " + e + "\n\t(Cell is " + c + ")");

		if (current != null) {
			unhighlight(current);
		}

		highlight(c);
		current = c;
	}

	private void preparePlan(DefaultGraphCell cell, boolean forward) {

		ViewGraph g = view.getViewGraph();

		// calculate affected cells
		ArrayList<GraphCell> edges = new ArrayList<GraphCell>();
		ArrayList<GraphCell> vertices = new ArrayList<GraphCell>();
		if (ViewHelper.isVertex(cell)) {
			Object v = ViewHelper.getVertex(cell);
			vertices.add(cell);
			for (Edge e : (Set<Edge>) g.edgesOf(v)) {
				edges.add(g.getModelAdapter().getEdgeCell(e));
			}
		} else {
			Edge e = ViewHelper.getEdge(cell);
			Object src = e.getSource();
			Object tgt = e.getTarget();
			edges.add(cell);
			vertices.add(g.getModelAdapter().getVertexCell(src));
			vertices.add(g.getModelAdapter().getVertexCell(tgt));
		}

		// resolve conflicts with old plans (FIXME: unneeded, not simultaneous...)
		HashSet<GraphCell> cellsInStep;
		ArrayList<AnimationPlan> notRunning = new ArrayList<AnimationPlan>();
		for (AnimationPlan p : runningPlans) {
			if (!p.isRunning()) {
				notRunning.add(p);
				continue;
			}
			for (ArrayList<AnimationStep> steps : p.getMoves()) {
				log.debug("  Entering into moves of " + p);
				for (AnimationStep as : steps) {
					log.debug(" - Entering into step: " + as);
					AbstractStep s = (AbstractStep) as;
					cellsInStep = new HashSet<GraphCell>(s.getCells());
					if (s instanceof HighlightEdgesStep) {
						cellsInStep.retainAll(edges);
					}
					if (s instanceof HighlightVerticesStep) {
						cellsInStep.retainAll(vertices);
					}
					for (GraphCell c : cellsInStep) {
						s.getCells().remove(c);
						log.debug(" -- Removed cell: " + c);
					}
				}
			}
		}
		runningPlans.removeAll(notRunning);

		// prepare plan
		HighlightEdgesStep hes = new HighlightEdgesStep();
		hes.setCells(edges);
		hes.forward = forward;
		HighlightVerticesStep hvs = new HighlightVerticesStep();
		hvs.setCells(vertices);
		hvs.forward = forward;
		AnimationPlan plan = new AnimationPlan(view,
				AnimationPlan.ROLLOVER_PRIORITY);
		plan.addStep(hes);
		plan.mergeStep(hvs);
		plan.run();
		runningPlans.add(plan);
	}

	private void highlight(DefaultGraphCell cell) {
		preparePlan(cell, true);
	}

	private void unhighlight(DefaultGraphCell cell) {
		preparePlan(cell, false);
	}

	private static class HighlightEdgesStep extends AbstractStep {
		public boolean forward;

		public void interpolate(GraphCell cell, float p, Map changeMap) {
			HashMap map = new HashMap();
			if (forward) {
				GraphConstants.setLineWidth(map, (float) (2 * p + 1));
			} else {
				p = 1 - p;
				GraphConstants.setLineWidth(map, (float) (2 * p + 1));
			}
			changeMap.put(cell, map);
		}
	}

	private static class HighlightVerticesStep extends AbstractStep {
		public boolean forward;

		public void interpolate(GraphCell cell, float p, Map changeMap) {
			HashMap map = new HashMap();
			if (forward) {
				if (p == 0) {
					GraphConstants.setOpaque(map, true);
				}
			} else {
				p = 1 - p;
				if (p == 0) {
					GraphConstants.setOpaque(map, false);
				}
			}
			Color c = Color.getHSBColor(.5f, p, 1f);
			GraphConstants.setGradientColor(map, c);
			changeMap.put(cell, map);
		}
	}
}
