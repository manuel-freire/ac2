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
package es.ucm.fdi.ac.extract;

import java.io.File;

/**
 * Looks in the path for clues on whether to accept or reject.
 * @author mfreire
 */
public class PathFilter extends PatternFilter {

	public PathFilter(String pattern) {
		super(pattern);
	}

	public PathFilter() {
	}

	public boolean accept(FileTreeNode ftn) {
		return ftn.getPath().matches(pattern);
	}

	public boolean accept(File f) {
		return f.getPath().matches(pattern);
	}

	@Override
	public String toString() {
		return "path='" + pattern + "'";
	}
}
