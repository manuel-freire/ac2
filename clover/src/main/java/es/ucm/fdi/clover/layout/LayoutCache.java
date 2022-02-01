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

import es.ucm.fdi.clover.model.BaseGraph;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

/**
 * Stores previous layouts in a mixed LRU/max-available-mem approach
 *
 * @author mfreire
 */
public class LayoutCache {
	private Log log = LogFactory.getLog(LayoutCache.class);

	/** max elements in strong cache */
	private int maxSize;

	/** lru bookkeeping for strong cache */
	private TreeSet<SavedLayout> lru;

	/** the cache itself (lru'd + memory-bounded) */
	private HashMap<CacheKey, SavedLayout> cache;

	/** default size, used if nobody cares */
	private static int defaultSize = 10;

	/**
	 * Internal class that represents a cached view; cached views always use
	 * layout coordinates.
	 */
	private static class SavedLayout implements Comparable {
		protected int lastAccess;
		protected CacheKey key;
		protected HashMap<Object, Rectangle2D> data;

		/** global 'time' counter */
		private static int currentTime = 0;

		private SavedLayout(CacheKey key, HashMap<Object, Rectangle2D> data) {
			lastAccess = Integer.MIN_VALUE;
			updateAccess();
			this.key = key;
			this.data = data;
		}

		public void updateAccess() {
			lastAccess = currentTime++;
		}

		public CacheKey getKey() {
			return key;
		}

		public HashMap<Object, Rectangle2D> getData() {
			return data;
		}

		public int compareTo(Object o) {
			return lastAccess - ((SavedLayout) o).lastAccess;
		}
	}

	/**
	 * Creates a new instance, with default lru-enforced size
	 */
	public LayoutCache() {
		this(defaultSize);
	}

	/**
	 * Creates a new instance of LayoutCache
	 */
	public LayoutCache(int maxSize) {
		this.maxSize = maxSize;
		lru = new TreeSet<SavedLayout>();
		cache = new HashMap<CacheKey, SavedLayout>();
	}

	/**
	 * Change the max number of elements in strong cache
	 */
	public void setSize(int maxSize) {
		this.maxSize = maxSize;
		while (lru.size() > maxSize) {
			lru.remove(lru.first());
		}
	}

	/**
	 * Internal class to represent a cache hit. Includes both the actual hit
	 * data and the score that this match achieved
	 */
	public static class CacheHit {
		private HashMap<Object, Rectangle2D> data;
		private double score;

		public CacheHit(HashMap<Object, Rectangle2D> data, double score) {
			this.data = data;
			this.score = score;
		}

		public HashMap<Object, Rectangle2D> getData() {
			return data;
		}

		public double getScore() {
			return score;
		}
	}

	/**
	 * Find a match for a previous layout. Uses key to look for exact matches,
	 * and vertices to look for worse matches. If no perfect match and no
	 * sloppy match better than minScore is found, returns 'null'.
	 */
	public CacheHit get(CacheKey key, double minScore) {
		SavedLayout currentLayout = null;
		double currentScore;

		log.debug("Looking up key = " + key);

		currentLayout = cache.get(key);
		if (currentLayout == null) {
			log.debug("\tCache *near* miss, reverting to sloppy matching");
		} else {
			log.debug("\tCache hit; but checking secondary");
			currentScore = currentLayout.getKey().scoreAgainst(key);
			if (currentScore >= minScore) {
				log.debug("\tCache hit confirmed; score = " + currentScore);
				hit(currentLayout);
				return new CacheHit(currentLayout.getData(), currentScore);
			}
		}

		// sloppy match
		SavedLayout bestLayout = null;
		double bestScore = Double.MIN_VALUE;

		for (SavedLayout current : cache.values()) {
			if (current == null) {
				continue;
			}

			currentScore = current.getKey().scoreAgainst(key);
			if (currentScore > bestScore) {
				bestScore = currentScore;
				bestLayout = current;
				log.debug("\tBest sloppy is: " + bestScore);
			}
		}

		if (bestLayout == null || bestScore < minScore) {
			log.debug("\tCache miss is final: nothing, or not good enough ("
					+ bestScore + ")");
			return null;
		} else {
			log.debug("\tSloppy is good enough, returning; score = "
					+ bestScore);
			hit(bestLayout);
			return new CacheHit(bestLayout.getData(), bestScore);
		}
	}

	/**
	 * Add a new layout
	 */
	public void put(CacheKey key, HashMap<Object, Rectangle2D> layout) {
		log.debug("Added/refreshed layout for key " + key);
		SavedLayout toSave = new SavedLayout(key, layout);
		hit(toSave);
		cache.put(key, toSave);
	}

	/**
	 * Hit a layout: 
	 * - if in lru, remove it
	 * - if size is exceeded, remove least-recently-used until size ok
	 * - update timestamp
	 * - add again to lru list
	 */
	private void hit(SavedLayout sl) {
		lru.remove(sl);
		while (lru.size() >= maxSize) {
			if (log.isDebugEnabled()) {
				log.debug("Limit reached; removed:" + dumpSingle(lru.first()));
			}
			lru.remove(lru.first());
		}
		sl.updateAccess();
		ArrayList<SavedLayout> toRemove = new ArrayList<SavedLayout>();
		for (SavedLayout saved : lru) {
			if (cache.get(saved.getKey()) != saved) {
				toRemove.add(saved);
			}
		}
		lru.removeAll(toRemove);

		lru.add(sl);
		if (log.isDebugEnabled()) {
			log.debug("Added:" + dumpSingle(sl));
		}
	}

	/**
	 * Dumps the current contents of the layout cache - 
	 * good for debugging purposes
	 */
	public String dump(BaseGraph g) {
		StringBuffer sb = new StringBuffer();
		int i = 0;
		int check = 0;
		for (SavedLayout current : cache.values()) {
			i++;
			for (Rectangle2D r : current.getData().values()) {
				check += r.getCenterX() - r.getCenterY();
			}
			sb.append("" + i + ": " + current.lastAccess + " (" + check
					+ ") - " + current.key.save(g) + "\n");
		}
		return sb.toString();
	}

	public String dumpSingle(SavedLayout sl) {
		int check = 0;
		for (Rectangle2D r : sl.getData().values()) {
			check += r.getCenterX() - r.getCenterY();
		}
		return sl.lastAccess + " (" + check + ") - " + sl.getKey() + "\n";
	}

	/**
	 * Save a layoutCache to a jdom element
	 */
	public void save(Element e, BaseGraph g) {
		e.setAttribute("maxSize", "" + maxSize);
		for (CacheKey key : cache.keySet()) {
			SavedLayout sl = cache.get(key);
			if (sl == null)
				continue;

			Element entry = new Element("entry");
			entry.setAttribute("key", key.save(g));
			for (Object o : sl.getData().keySet()) {
				Rectangle2D r = sl.getData().get(o);
				Element box = new Element("box");
				box.setAttribute("id", "" + g.getId(o));
				box.setAttribute("x", "" + r.getX());
				box.setAttribute("y", "" + r.getY());
				box.setAttribute("w", "" + r.getWidth());
				box.setAttribute("h", "" + r.getHeight());
				entry.addContent(box);
			}
			e.addContent(entry);
		}
	}

	/**
	 * Load a layoutCache from a JDom element
	 */
	public void restore(Element e, BaseGraph g) {
		setSize(Integer.parseInt(e.getAttributeValue("maxSize")));
		for (Element entry : (List<Element>) e.getChildren()) {
			CacheKey key = new CacheKey(entry.getAttributeValue("key"), g);
			// System.err.println("processing "+entry.getAttributeValue("key"));
			HashMap<String, Object> idToVertex = key.getIdMappings(g);
			HashMap<Object, Rectangle2D> boxes = new HashMap<Object, Rectangle2D>();
			for (Element box : (List<Element>) entry.getChildren()) {
				Object o = idToVertex.get(box.getAttributeValue("id"));
				Rectangle2D rect = new Rectangle2D.Float(Float.parseFloat(box
						.getAttributeValue("x")), Float.parseFloat(box
						.getAttributeValue("y")), Float.parseFloat(box
						.getAttributeValue("w")), Float.parseFloat(box
						.getAttributeValue("h")));
				boxes.put(o, rect);
			}
			put(key, boxes);
		}
	}

	/**
	 * Empties the cache from all saved views
	 */
	public void clear() {
		lru.clear();
		cache.clear();
	}
}
