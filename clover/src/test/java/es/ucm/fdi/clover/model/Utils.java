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
 * Utils.java
 *
 * Created on October 22, 2006, 10:40 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.model;

import java.util.Collection;

/**
 * Helper class for testing stuff on the eps.clover.model package
 *
 * @author mfreire
 */
public class Utils {

	public static Object getVertexForId(String id, BaseGraph g) {
		for (Object v : g.vertexSet()) {
			if (g.getId(v).equals(id))
				return v;
		}
		return null;
	}

	public static Cluster getClusterForId(String id, ClusteredGraph cg) {
		for (Cluster c : cg.getSlice()) {
			if (c.getListing(cg.getBase()).equals(id))
				return c;
		}
		return null;
	}

	public static boolean checkSameClusters(String[] list,
			Collection<Cluster> col, BaseGraph g) {
		if (list.length != col.size()) {
			System.out.println("Expected size " + list.length + ", found "
					+ col.size());
			System.out.println("Expected:");
			for (String s : list) {
				System.out.println("\t" + s);
			}
			System.out.println("Found:");
			for (Cluster c : col) {
				System.out.println("\t" + c.getListing(g));
			}

			return false;
		}
		for (Cluster c : col) {
			boolean found = false;
			for (String s : list) {
				if (s.equals(c.getListing(g))) {
					found = true;
					break;
				}
			}
			if (!found) {
				System.out.println("ERROR: did not find '" + c.getListing(g)
						+ "' in list");
				for (String s : list) {
					System.out.println("\t '" + c.getListing(g) + "' !=  '" + s
							+ "'");
				}

				return false;
			}
		}
		return true;
	}

	public static boolean checkSameRoot(Cluster root, Collection<Cluster> set) {
		for (Cluster c : set) {
			if (root != c.getRootCluster()) {
				System.out.println("The root of " + c + " is "
						+ c.getRootCluster() + " != " + root);
				return false;
			}
		}
		return true;
	}
}
