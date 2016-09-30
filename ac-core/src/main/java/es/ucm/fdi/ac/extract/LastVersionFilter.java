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
 * NamePatternFilter.java
 *
 * Created on September 11, 2006, 10:54 AM
 *
 */

package es.ucm.fdi.ac.extract;

import java.io.File;

/**
 * Avoids multiple versions of the same file (say, p1a01, p1a01v2, p1a01_v3),
 * applying a simple heuristic to select the best one. Returns 'false' for
 * all lower-versions.
 *
 * @author mfreire
 */
public class LastVersionFilter extends PatternFilter {

	private final FileNameFilter fnf = new FileNameFilter();

	/**
	 * Creates a new instance of NamePatternFilter
	 * @param prefixPattern such as "v"
	 */
	public LastVersionFilter(String prefixPattern) {
		super(prefixPattern);
	}

	public LastVersionFilter() {
	}

	public boolean accept(File f) {
		String name = f.getName();

		// obtain the common-prefix as it applies to this filename
		String suffix = name.replaceFirst(pattern, "");
		String prefix = name.substring(0, name.lastIndexOf(suffix));
		fnf.setPattern(prefix + ".*\\..+");

		// search for the biggest suffix (usually longest/highest numbered)
		String max = name;
		for (File o : f.getParentFile().listFiles(fnf)) {
			max = (o.getName().compareTo(max) > 0) ? o.getName() : max;
		}

		if (max.equals(name)) {
			return true;
		} else {
			System.err.println("Avoiding '" + name + "': '" + max
					+ "' is better " + "prefix = " + prefix + " suffix = "
					+ suffix);
			return false;
		}
	}

	@Override
	public String toString() {
		return "maxv='" + pattern + "'";
	}
}
