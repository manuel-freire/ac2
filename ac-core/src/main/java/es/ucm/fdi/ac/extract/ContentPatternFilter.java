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
 * NamePatternFilter.java
 *
 * Created on September 11, 2006, 10:54 AM
 *
 */

package es.ucm.fdi.ac.extract;

import es.ucm.fdi.util.SourceFileCache;
import java.io.File;

/**
 * Looks at file content. For your convenience, 
 * matching is whitespace-insensitive.
 * 
 * This is a slow filter.
 * 
 * @author mfreire
 */
public class ContentPatternFilter extends PatternFilter {

	public ContentPatternFilter(String pattern) {
		super(pattern);
	}

	public ContentPatternFilter() {
	}

	public boolean accept(FileTreeNode ftn) {
		return accept(ftn.getFile());
	}

	public boolean accept(File f) {
		String source = SourceFileCache.getSource(f);

		if (source == null) {
			System.err.println("File '" + f + "' could not be read!!!");
			return false;
		}

		return source.replaceAll("\\p{Space}+", " ").matches(pattern);
	}

	@Override
	public String toString() {
		return "has='" + pattern + "'";
	}
}
