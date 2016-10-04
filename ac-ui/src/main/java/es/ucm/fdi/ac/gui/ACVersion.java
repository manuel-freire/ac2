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
package es.ucm.fdi.ac.gui;

/**
 * Returns the version-string to display on window titles
 *
 * Created by mfreire on 3/10/16.
 */
public class ACVersion {

	private static String version;

	public static String getVersion() {
		if (version == null) {
			// relies on the Implementation-Version META-INF/MANIFEST.MF property
			String v = MainGui.class.getPackage().getImplementationVersion();
			version = (v == null || v.isEmpty()) ? "[dev]" : v;
			System.err.println("Version: " + version);
		}
		return version;
	}
}
