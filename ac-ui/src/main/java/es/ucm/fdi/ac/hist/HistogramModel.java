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
 * HistogramModel.java 
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * Contributors: - 
 * Changelog: 
 *     18-Apr-2006: first version (mfreire)
 *     6-Sep-2006: separated into model and view; this is the model
 */

package es.ucm.fdi.ac.hist;

import java.util.*;

/**
 * A generic histogram model, supporting datapoint labelling and efficient
 * range-count operations 
 *
 * @author mfreire
 */
public class HistogramModel {

	// the actual data that gets displayed
	private TreeMap<Float, DataPoint> data;
	// highlights, used to mark important values
	private List<Double> highlights = null;

	public HistogramModel() {
		data = new TreeMap<Float, DataPoint>();
	}

	private static class DataPoint {
		int count;
		ArrayList labels = new ArrayList();

		public DataPoint() {
		}

		public void increment() {
			count++;
		}

		public void increment(Object label) {
			count++;
			labels.add(label);
		}

		public int getCount() {
			return count;
		}

		public ArrayList getLabels() {
			return labels;
		}
	}

	public void addPoint(float f) {
		DataPoint v = data.get(f);
		if (v == null) {
			v = new DataPoint();
			data.put(f, v);
		}
		v.increment();
	}

	public void addLabelledPoint(float f, Object label) {
		DataPoint v = data.get(f);
		if (v == null) {
			v = new DataPoint();
			data.put(f, v);
		}
		v.increment(label);
	}

	public void addAllPoints(Collection<Float> F) {
		for (float f : F)
			addPoint(f);
	}

	public void addAllPoints(float[] F) {
		for (float f : F)
			addPoint(f);
	}

	public float getLowest() {
		return data.firstKey();
	}

	public float getNearestPoint(float f) {
		SortedMap<Float, DataPoint> low, high;

		// ensure 'f' is valid
		if (Float.isInfinite(f) || Float.isNaN(f)) {
			f = 0;
		}

		low = data.subMap(-Float.MAX_VALUE, f);
		high = data.subMap(f, Float.MAX_VALUE);

		// important check
		if (low.isEmpty()) {
			if (high.isEmpty()) {
				throw new NoSuchElementException(
						"Empty histogram, no nearest point");
			} else {
				return high.firstKey();
			}
		} else if (high.isEmpty()) {
			return low.lastKey();
		}

		float lower = low.lastKey();
		float higher = high.firstKey();
		return (f - lower > higher - f) ? higher : lower;
	}

	public ArrayList<Object> getLabelsForPoint(float f) {
		DataPoint v = data.get(f);
		return (v != null) ? v.getLabels() : null;
	}

	public int count(float min, float max) {
		int n = 0;
		if (min > max) {
			System.err.println("Error: min > max in HistogramModel.count()");
			return 0;
		}

		for (Float f : data.subMap(min, max).keySet()) {
			n += data.get(f).getCount();
		}

		return n;
	}

	/**
	 * divides the range in 'level' levels from 0 to 1, and tries to find which
	 * 'width' bar has greatest count
	 */
	public int getMaxBar(int levels, float width) {
		int max = 0;
		float dx = 1.0f / levels;
		float x0 = 0;
		for (int i = 0; i < levels; i++) {
			x0 = ((float) i) / levels;
			max = Math.max(max, count(x0, x0 + width));
		}
		return max;
	}

	public int numOver(float val) {
		return count(val, Float.MAX_VALUE);
	}

	public int numBelow(float val) {
		return count(Float.MIN_VALUE, val);
	}

	/**
	 * Returns a very imprecise measure of the "skip" found in the lowest positions
	 */
	public float getLowSkip() {
		float lowest = getLowest();
		Iterator<Float> it = data.keySet().iterator();
		float other = lowest;
		for (int i = 0; i < 3 && it.hasNext(); i++) {
			other = it.next();
		}
		return other - lowest;
	}

	/**
	 * Get highlights for this model
	 */
	public List<Double> getHighlights() {
		return highlights;
	}

	/**
	 * Set highlights for this histogram (by default, no highlights at all)
	 */
	public void setHighlights(List<Double> highlights) {
		this.highlights = highlights;
	}
}
