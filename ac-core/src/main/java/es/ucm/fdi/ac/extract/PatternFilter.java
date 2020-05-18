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
 * PatternFilter.java
 *
 * Created on September 16, 2006, 5:22 PM
 *
 */

package es.ucm.fdi.ac.extract;

import java.io.IOException;
import org.jdom2.Element;

/**
 * A filter that uses some sort of pattern to determine validity
 *
 * @author mfreire
 */
public abstract class PatternFilter extends FileTreeFilter {

	protected String pattern;

	protected PatternFilter() {
	}

	protected PatternFilter(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * Returns this filter's pattern
	 * @return 
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * Sets this patterns filter to something else
	 * @param pattern
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public void loadFromXML(Element element) throws IOException {
		setPattern(element.getText().trim());
	}

	@Override
	public void saveInner(Element e) throws IOException {
		e.setAttribute("class", this.getClass().getName());
		e.setText(getPattern().trim());
	}
}
