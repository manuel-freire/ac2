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
package es.ucm.fdi.ac.tableviz;

import es.ucm.fdi.ac.dgram.DendrogramModel;
import java.util.ArrayList;

/**
 * Model information for a Table Visualization. Two data items are kept - 
 * the actual data, and a permutation specifying what data is shown where.
 * The permutation can be a direct mapping, or obtained from a DendrogramModel.
 *
 * @author mfreire
 */
public class TableModel {

	private ArrayList<Object> labels;
	private float[][] data;
	private int[] perm;

	/** Creates a new instance of TableModel */
	public TableModel(int n) {
		data = new float[n][];
		perm = new int[n];
		labels = new ArrayList<Object>();
	}

	public void clearPerm() {
		for (int i = 0; i < perm.length; i++)
			perm[i] = i;
	}

	public void setPerm(int[] p) {
		System.arraycopy(p, 0, perm, 0, p.length);
	}

	public void setPerm(DendrogramModel dm) {
		dm.getLeafSortOrder(perm);
	}

	public void addLeaf(Object o, float[] distances) {
		int i = labels.size();
		data[i] = distances;
		perm[i] = i;
		labels.add(o);
	}

	public int getN() {
		return data.length;
	}

	public Object getLabel(int i) {
		return labels.get(perm[i]);
	}

	public float get(int i, int j) {
		return data[perm[i]][perm[j]];
	}
}
