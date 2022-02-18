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
package es.ucm.fdi.clover.layout;

import es.ucm.fdi.clover.model.ViewGraph;
import es.ucm.fdi.clover.view.BaseView;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.Observable;

/**
 * A layout container. Supports running layouts until certain
 * requirements hold (for instance, the layout is finished, or a timeout
 * expires, or a given number of iterations have been performed).
 *
 * The Observer and Runnable interfaces are implemented, allowing the layout to 
 * proceed offline and periodically notify observers of the latest advances.
 *
 * @author  mfreire
 */
@SuppressWarnings("all")
public class LayoutManager extends Observable implements Runnable {

	private Log log = LogFactory.getLog(LayoutManager.class);

	/** if false, the layout should stop immediately */
	private boolean canContinue;

	/** if true, a layout is running */
	private boolean isRunning;

	/** the current layout algorithm */
	protected LayoutAlgorithm algorithm;

	/** max layout time in ms */
	protected int maxTime = 1 * 1000;

	/** the node set to lay out */
	protected Node[] N;

	/** the set of vertices that was last layed out; good for fast cache lookups */
	protected CacheKey cacheKey;

	/** 
	 * constructor
	 */
	public LayoutManager(Node[] N) {
		algorithm = null;
		isRunning = false;
		setNodes(N);
	}

	/**N
	 * an easier to use constructor
	 */
	public LayoutManager(BaseView view) {
		this(Node.loadNodes(view));
	}

	/**
	 * Stops the layout on the next iteration
	 */
	public void setCanContinue(boolean b) {
		canContinue = b;
	}

	/**
	 * Lay out a graph with a given layoutAlgorithm and a set of constraints.
	 *
	 * Periodic updates are sent to all observers, and upon successful finish, 
	 * these observers are de-registered.
	 */
	public synchronized void run() {

		if (isRunning) {
			log.warn("Layout " + this + " already running: ignoring request");
			return;
		}
		canContinue = true;
		isRunning = true;

		try {
			// init
			algorithm.init(N);

			log.debug("starting layout (maxTime = " + maxTime + ")... ");

			long startTime = System.currentTimeMillis();
			long currentTime = startTime;
			int iterations;
			for (iterations = 1; !algorithm.layoutFinished(); iterations++) {

				// layout some
				algorithm.layout();

				// check end
				currentTime = System.currentTimeMillis();
				if ((!canContinue) || (currentTime - startTime) > maxTime) {
					break;
				}
			}

			// cleanup & sync again
			log
					.debug(""
							+ algorithm.getClass().getSimpleName()
							+ " ended: "
							+ (algorithm.layoutFinished() ? "finished"
									: " interrupted") + " after "
							+ (currentTime - startTime) + " ms and "
							+ iterations + " cycles");
			algorithm.end();
		} catch (Exception e) {
			log
					.warn(
							"layout ended with exception. Probably the app should be restarted",
							e);
		}

		commit("end");
		isRunning = false;
	}

	/**
	 * Notify observers that the layout has finished
	 */
	public void commit(String s) {
		notifyObservers(s);
		Thread.yield();
	}

	/**
	 * Applies changes to the nodes on to the view
	 */
	public void applyChanges(BaseView view) {
		Rectangle2D bounds = Node.getBounds(N, view.getLayoutZoom());
		Map map = Node.getChangeMap(N, (int) bounds.getX() - 10, (int) bounds
				.getY() - 10, view.getLayoutZoom());
		view.getGraphLayoutCache().edit(map);
		view.repaint();
		log.info("Updated view with " + N.length + " cell positions");
	}

	/**
	 * Sets the nodes to layout, using a node array;
	 * Note that this does not use any cache - and invalidates any cache
	 * that may be used (FIXME)
	 */
	public void setNodes(Node[] nodes) {
		N = nodes;
		cacheKey = new CacheKey(nodes);
	}

	/**
	 * Sets the nodes to layout, using the positions found in the 
	 * specified view
	 */
	public void setNodes(BaseView view) {
		setNodes(view, false);
	}

	/**
	 * Sets the nodes to layout, using the positions found in the 
	 * specified view
	 */
	public void setNodes(BaseView view, boolean useOldPositions) {
		N = Node.loadNodes(N, view, (useOldPositions) ? N : null);
		ViewGraph vg = view.getViewGraph();
		cacheKey = new CacheKey(vg);
	}

	/**
	 * Loads from cache, if cache contains a good enough hit. Returns the score
	 */
	public double setNodesFromCache(LayoutCache cache, BaseView view,
			double minScore) {
		LayoutCache.CacheHit hit;
		hit = cache.get(cacheKey, minScore);
		if (hit == null) {
			return 0;
		} else {
			Node.setPositions(hit.getData(), N, view);
			return hit.getScore();
		}
	}

	/**
	 * Adds the current nodes to the given cache. Uses the view to translate
	 * from cellviews to graph vertices
	 */
	public void addNodesToCache(LayoutCache cache, BaseView view) {
		cache.put(cacheKey, Node.getPositions(N, view));
	}

	/**
	 * Retrieves the nodes being layed out
	 */
	public Node[] getNodes() {
		return N;
	}

	/** 
	 * Getter for property maxIt.
	 * @return Value of property maxIt.
	 */
	public int getMaxTime() {
		return this.maxTime;
	}

	/** 
	 * Setter for property maxIt.
	 * @param maxIt New value of property maxIt.
	 */
	public void setMaxTime(int maxTime) {
		this.maxTime = maxTime;
	}

	/** 
	 * Getter for property algorithm.
	 * @return Value of property algorithm.
	 */
	public LayoutAlgorithm getAlgorithm() {
		return this.algorithm;
	}

	/** 
	 * Setter for property algorithm.
	 * @param algorithm New value of property algorithm.
	 */
	public void setAlgorithm(LayoutAlgorithm algorithm) {
		this.algorithm = algorithm;
	}
}
