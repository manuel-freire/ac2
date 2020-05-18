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
 * BaseView.java
 *
 * Created on May 6, 2006, 8:55 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.view;

import es.ucm.fdi.clover.event.ClusteringChangeEvent;
import es.ucm.fdi.clover.event.ClusteringChangeListener;
import es.ucm.fdi.clover.model.Cluster;
import es.ucm.fdi.clover.model.ClusterHierarchy;
import es.ucm.fdi.clover.model.ClusteredGraph;
import es.ucm.fdi.clover.model.Slice;
import es.ucm.fdi.clover.model.ViewGraph;
import java.awt.Event;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import es.ucm.fdi.clover.model.BaseGraph;
import java.awt.event.ActionEvent;

import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import org.jdom2.Element;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;

/**
 * A visual component that represents a ClusterGraph. It includes some simple
 * interactions: cluster expansion/collapse, aura, layout history, and more.
 *
 * @author mfreire
 */
public class ClusterView extends BaseView implements ClusteringChangeListener {

	private Log log = LogFactory.getLog(ClusterView.class);

	/** vertices that are currently frozen (ie: wont be hidden or expanded by poi changes) */
	private HashSet frozen;

	/** focus size - area around the focus that will be expanded to same detail as focus */
	private int focusSize;

	/** max vertices in simultaneous view */
	private int maxClusters;

	/** if set, clustering will not change when mouse selects node */
	private boolean clusterLock;

	/** used to catch clicks and do things with them (such as change the POI) */
	private VertexCellClickListener vertexClickListener;

	/** used to highlight would-be-expanded nodes */
	private AuraMouseListener auraMouseListener;

	/** can be used to bypass the AuraMouseListener and highlight stuff directly */
	private Highlighter highlighter;

	/** a stack with the history for expansions&collapses followed until this point */
	private ArrayList<ClusteringChangeEvent> clusterNavHistory;

	/** the current point in the stack (= the index of the last event that was carried out) */
	private int currentNavIndex = 0;

	/**
	 * Restore from saved JDom element
	 */
	public ClusterView(ViewGraph viewGraph) {
		super(viewGraph);
		highlighter = new Highlighter(this);
		auraMouseListener = new AuraMouseListener();
		vertexClickListener = new VertexCellClickListener();
		addMouseListener(vertexClickListener);
		addMouseMotionListener(auraMouseListener);
		focusSize = 1;
		frozen = new HashSet();
		maxClusters = 16;
		clusterLock = false;
		clusterNavHistory = new ArrayList<ClusteringChangeEvent>();
		currentNavIndex = -1;

		// DEBUG ACTION TO DUMP HIERARCHY 
		this.getInputMap().put(KeyStroke.getKeyStroke('c'), "save-clustering");
		this.getActionMap().put("save-clustering", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				System.err.println("Dumping out: "
						+ ((ClusteredGraph) getBase()).getHierarchy().getRoot()
								.dump());
			}
		});
	}

	public ClusterHierarchy geHierarchy() {
		return ((ClusteredGraph) getBase()).getHierarchy();
	}

	public void setBase(BaseGraph base) {

		if (!(base instanceof ClusteredGraph)) {
			throw new IllegalArgumentException("Base must be a ClusteredGraph");
		}

		// register for clustering nav events notification (to be used for undo/redo)
		if (getBase() != null) {
			((ClusteredGraph) getBase()).removeClusteringChangeListener(this);
		}

		// perform first expansion
		ClusteredGraph cg = (ClusteredGraph) base;
		if (frozen == null) {
			frozen = new HashSet();
		}
		setCurrentPoI(cg.getHierarchy().getRoot().getFirstLeafVertex());
		cg.addClusteringChangeListener(this);

		setModel(new DefaultGraphModel());
		// this is where the base is actually set
		viewGraph.setBase(base);
		setModel(getViewGraph().getModelAdapter());

		if (animator == null) {
			animator = new ClusterAnimator(this);
		} else {
			animator.setView(this);
		}
	}

	public Slice getSlice() {
		return ((ClusteredGraph) getBase()).getSlice();
	}

	public ClusteringChangeEvent getPoIChangeEventFor(Object vertex) {
		if (vertex == null)
			return null;
		ClusteredGraph cg = (ClusteredGraph) getBase();
		ClusteringChangeEvent cce = cg.createPoIChangeEvent(vertex, frozen,
				getFocusSize(), getMaxClusters());
		return cce;
	}

	/**
	 * Locks/Unlocks clustering changes from happening. Default is unlocked.
	 */
	public void setClusterLock(boolean clusterLock) {
		if (this.clusterLock != clusterLock) {
			if (clusterLock) {
				removeMouseMotionListener(auraMouseListener);
			} else {
				addMouseMotionListener(auraMouseListener);
			}
		}
		this.clusterLock = clusterLock;
	}

	public boolean isClusterLock() {
		return clusterLock;
	}

	public boolean isFrozen(Object v) {
		return frozen.contains(v);
	}

	public void setFrozen(Object v, boolean b) {
		DefaultGraphCell cell = ViewHelper.getVertexCell(this, v);
		String label = (String) cell.getAttributes().get(ViewGraph.LABEL);
		if (b) {
			frozen.add(v);
			label = "[" + label + "]";
		} else {
			frozen.remove(v);
			label = label.replaceAll("[\\]\\[]", "");
		}
		Map map = new AttributeMap();
		map.put(ViewGraph.LABEL, label);
		getGraphLayoutCache().editCell(cell, map);
	}

	public Object getCurrentPoI() {
		return ((ClusteredGraph) getBase()).getPointOfInterest();
	}

	/**
	 * Changes the current focus; will trigger a visibility recalculation
	 * unless the clustering is 'locked'.
	 */
	public void setCurrentPoI(Object anotherPoI) {
		setCurrentPoI(anotherPoI, !clusterLock);
	}

	/**
	 * This one lets you decide whether to trigger Degree Of Interest 
	 * recalculation or not.
	 */
	public void setCurrentPoI(Object anotherPoI, boolean recalculateDoI) {

		if (anotherPoI == null) {
			return;
		}
		ClusteredGraph cg = (ClusteredGraph) getBase();
		cg.setPointOfInterest(anotherPoI);

		if (recalculateDoI) {
			recalculateDoI();
		}
	}

	/**
	 * Make a vertex visible
	 */
	public void makeVertexVisible(Object v) {
		ClusteredGraph cg = (ClusteredGraph) getBase();
		ClusteringChangeEvent cce = cg.createMakeVisibleEvent(v);
		if (cce != null) {
			cg.clusteringChangePerformed(cce);
		}
	}

	/**
	 * Get the focus size
	 */
	public int getFocusSize() {
		return focusSize;
	}

	/**
	 * Set the focus size
	 */
	public void setFocusSize(int focusSize) {
		this.focusSize = focusSize;
		if (!clusterLock) {
			recalculateDoI();
		}
	}

	/**
	 * Get the current max cluster number
	 */
	public int getMaxClusters() {
		return maxClusters;
	}

	/**
	 * Set the max number of clusters
	 */
	public void setMaxClusters(int maxClusters) {
		this.maxClusters = maxClusters;
		if (!clusterLock) {
			recalculateDoI();
		}
	}

	/**
	 * Force a recalculation of "degree of interest" (visibility).
	 * Recalculates the "degree of interest" of all vertices; only those with 
	 * the highest interest will remain visible; the rest will be collapsed.
	 */
	public void recalculateDoI() {
		ClusteredGraph cg = (ClusteredGraph) getBase();
		ClusteringChangeEvent cce = getPoIChangeEventFor(getCurrentPoI());
		if (cce == null) {
			return;
		}
		if (cce.getCollapsed().size() + cce.getExpanded().size() > 0) {
			cg.clusteringChangePerformed(cce);
		}
	}

	/**
	 * Navigation history "redo" operation
	 */
	public void nextNavAction() {
		if (currentNavIndex + 1 >= clusterNavHistory.size()) {
			log.debug("Already at end of history, aborted");
			return;
		}
		ClusteringChangeEvent cce = clusterNavHistory.get(currentNavIndex + 1);
		ClusteredGraph cg = (ClusteredGraph) getBase();
		cg.clusteringChangePerformed(cce);
		log.debug("Done. Now at " + currentNavIndex);
	}

	/**
	 * Navigation history "undo" operation
	 */
	public void prevNavAction() {
		if (currentNavIndex < 0) {
			log.debug("Already at start of history, aborted");
			return;
		}
		ClusteringChangeEvent cce = clusterNavHistory.get(currentNavIndex);
		ClusteredGraph cg = (ClusteredGraph) getBase();
		cg.clusteringChangePerformed(cce.getUndoEvent());
		log.debug("Done. Now at " + currentNavIndex);
	}

	/**
	 * Returns the whole navigatino history, as a set of ClusteringChangeEvents.
	 */
	public ArrayList<ClusteringChangeEvent> getClusterNavHistory() {
		return clusterNavHistory;
	}

	/**
	 * Update the navigation history after a navigation event.
	 * 'undo' events are ignored; and, when we are not top of the pile, 
	 * a non-undo event will purge future events from the history
	 */
	public void clusteringChangePerformed(ClusteringChangeEvent evt) {
		log.debug("Start of history update: size is "
				+ clusterNavHistory.size() + ", index is " + currentNavIndex);

		if (evt.isUndoEvent()) {
			currentNavIndex--;
		} else {
			if (clusterNavHistory.size() <= currentNavIndex + 1) {
				clusterNavHistory.add(evt);
				currentNavIndex++;
			} else {
				// within the stack; the navIndex points to the redo action OR a new action
				ClusteringChangeEvent other = clusterNavHistory
						.get(currentNavIndex + 1);
				if (evt == other) {
					// do nothing
					log
							.debug("(forward navigation in history - nothing to update)");
				} else {
					log
							.debug("Navigation is new; throwing away 'future' events");

					// store & purge history from future events
					while (currentNavIndex + 1 < clusterNavHistory.size()) {
						clusterNavHistory.remove(clusterNavHistory.size() - 1);
					}
					clusterNavHistory.add(evt);
				}
				currentNavIndex++;
			}
		}

		log.debug("End of history update: size is " + clusterNavHistory.size()
				+ ", index is " + currentNavIndex);
	}

	/**
	 * Highlights the currently hovered-over cell and all its outgoing and incoming
	 * edges. Demonstrates the use of listeners and plans and animators to achieve
	 * interaction.
	 *
	 * Includes "aura effect", highlighting the nodes that will be expanded
	 * and/or collapsed should the user click on the present one. The listener
	 * auto-inhibits if there is already any plan running.
	 */
	private class AuraMouseListener extends MouseMotionAdapter {

		private DefaultGraphCell lastCell;

		/**
		 * Exit from the previous cell, if any - and highlight this cell
		 * and all incoming and outgoing edges
		 */
		public void mouseMoved(MouseEvent e) {
			DefaultGraphCell c = ViewHelper.getVertexCell(e);
			if (c == null) {
				highlighter.clearHighlight();
			} else if (c != lastCell) {
				highlighter.startFocusChangePlan(c);
				lastCell = c;
			}
		}

		public void mouseExit(Event evt, int x, int y) {
			highlighter.clearHighlight();
			lastCell = null;
		}
	}

	/**
	 * Default mouse click listener
	 */
	private class VertexCellClickListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {

			int left = MouseEvent.BUTTON1;
			int right = MouseEvent.BUTTON3;
			int shift = MouseEvent.SHIFT_DOWN_MASK;

			if (e.getClickCount() == 1 && e.getButton() == right) {
				System.err.println("hi mom!");
				setSelectionCell(viewGraph.getModelAdapter().getVertexCell(
						getCurrentPoI()));
			}

			if (e.getClickCount() == 1 && e.getButton() == left) {

				//                System.err.println("Entering click handling: "+e.getModifiersExText(e.getModifiersEx()));
				DefaultGraphCell cell = ViewHelper.getVertexCell(e);
				if (cell == null)
					return;
				Object v = ViewHelper.getVertex(cell);
				if (v == null)
					return;

				// straight click, not shifted
				if ((e.getModifiersEx() & (shift)) == 0) {
					if (clusterLock || !(v instanceof Cluster.Vertex)) {
						setCurrentPoI(v);
					} else {
						Cluster c = ((Cluster.Vertex) v).getCluster();
						setCurrentPoI(((Cluster) c.getChildAt(0)).getVertex());
					}
					//                    System.err.println("Prev sel: " + getSelectionCell());
				}
				// straight click, shifted (= wants to select without triggering changes)
				else if ((e.getModifiersEx() & (shift)) == shift) {
					setSelectionCell(cell);
					//                    System.err.println("Changed sel to: " + getSelectionCell());
				}
			}
		}
	}

	/**
	 * Return the set of "frozen" vertices. These vertices will not partake
	 * in expansion or collapse operations
	 */
	public HashSet getFrozen() {
		return frozen;
	}

	/**
	 * Change the set of "frozen" vertices for a new one. 
	 * These vertices will not partake in expansion or collapse operations
	 */
	public void setFrozen(HashSet frozen) {
		this.frozen = frozen;
	}

	/**
	 * Returns the current vertex click listener; allows a vertex click to 
	 * be simulated programatically without an awt.Robot
	 */
	public VertexCellClickListener getVertexClickListener() {
		return vertexClickListener;
	}

	/**
	 * Return the set of "frozen" vertices. These vertices will not partake
	 * in expansion or collapse operations
	 */
	public AuraMouseListener getAuraMouseListener() {
		return auraMouseListener;
	}

	/**
	 * Returns the highlighter currently in use.
	 */
	public Highlighter getHighlighter() {
		return highlighter;
	}

	/**
	 * Changes the current highlighter for another one.
	 */
	public void setHighlighter(Highlighter highlighter) {
		this.highlighter = highlighter;
	}

	/**
	 * Saves this view as an element of a JDom tree.
	 */
	public void save(Element e) {
		super.save(e);

		StringBuffer sb = null;

		e.setAttribute("poiVertex", getBase().getId(getCurrentPoI()));
		e.setAttribute("focusSize", "" + getFocusSize());
		e.setAttribute("maxClusters", "" + maxClusters);
		e.setAttribute("isClusterLock", isClusterLock() ? "true" : "false");

		// frozen set (may be empty)
		sb = new StringBuffer();
		if (!frozen.isEmpty()) {
			for (Object v : frozen) {
				sb.append(getBase().getId(v) + ",");
			}
			sb.deleteCharAt(sb.length() - 1);
		}
		e.setAttribute("frozenVertices", sb.toString());

		// visible slice
		sb = new StringBuffer();
		for (Cluster c : ((ClusteredGraph) getBase()).getSlice()) {
			sb.append(getBase().getId(c.getVertex()) + ",");
		}
		sb.deleteCharAt(sb.length() - 1);
		e.setAttribute("visibleSlice", sb.toString());
	}

	/**
	 * Restores the current view from an element of a JDom tree.
	 */
	public void restore(Element e) {
		super.restore(e);

		StringTokenizer st = null;

		// restore slice
		st = new StringTokenizer(e.getAttributeValue("visibleSlice"), ",");
		ArrayList<String> ids = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			ids.add(st.nextToken().trim());
		}
		ClusteredGraph cg = (ClusteredGraph) getBase();
		String poiVertexName = e.getAttributeValue("poiVertex");
		HashMap<String, Cluster> map = cg.restoreSlice(ids, poiVertexName);

		// restore frozen
		st = new StringTokenizer(e.getAttributeValue("frozenVertices"), ",");
		frozen.clear();
		while (st.hasMoreTokens()) {
			frozen.add(map.get(st.nextToken().trim()).getVertex());
		}

		// restore remaining settings
		focusSize = Integer.parseInt(e.getAttributeValue("focusSize"));
		maxClusters = Integer.parseInt(e.getAttributeValue("maxClusters"));
		clusterLock = Boolean
				.parseBoolean(e.getAttributeValue("isClusterLock"));
	}
}
