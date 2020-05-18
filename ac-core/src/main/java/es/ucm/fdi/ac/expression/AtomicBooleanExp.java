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
 * AtomicBooleanExp.java
 *
 * Created on September 16, 2006, 2:52 PM
 *
 */

package es.ucm.fdi.ac.expression;

import es.ucm.fdi.ac.extract.CompositeFilter;
import es.ucm.fdi.ac.extract.ContentPatternFilter;
import es.ucm.fdi.ac.extract.FileNameFilter;
import es.ucm.fdi.ac.extract.FileTreeFilter;
import es.ucm.fdi.ac.extract.PathFilter;
import es.ucm.fdi.ac.extract.PatternFilter;
import java.io.FileFilter;
import java.util.ArrayList;

import static es.ucm.fdi.util.I18N.m;

/**
 * An expression that evaluates to true or false.
 *
 * @author mfreire
 */
public class AtomicBooleanExp implements FilterExpression {

	private String header;
	private FileTreeFilter filter;

	private CompositeBooleanExp parent;

	/**
	 * Creates a new instance of AtomicBooleanExp
	 */
	public AtomicBooleanExp(FileTreeFilter filter) {
		this.filter = filter;
	}

	public AtomicBooleanExp() {
		setHeader(getHeaders().get(0));
		setBody(".*");
	}

	public void setParentExpression(CompositeExpression parent) {
		this.parent = (CompositeBooleanExp) parent;
		setHeader(getHeader());
	}

	private static ArrayList<String> headers = new ArrayList<String>();

	public ArrayList<String> getHeaders() {
		if (headers.isEmpty()) {
			headers.add(m("Filter.NameContains"));
			headers.add(m("Filter.NameMatches"));
			headers.add(m("Filter.NameEndsWith"));
			headers.add(m("Filter.PathContains"));
			headers.add(m("Filter.PathMatches"));
			headers.add(m("Filter.PathEndsWith"));
			headers.add(m("Filter.ContentContains"));
			headers.add(m("Filter.ContentMatches"));
		}
		return headers;
	}

	public String getHeader() {
		return header;
	}

	public String getBody() {
		String p = ((PatternFilter) filter).getPattern();
		if (header.endsWith(m("Filter.Contains"))) {
			p = p.substring(".*".length(), p.length() - ".*".length());
		} else if (header.endsWith(m("Filter.EndsWith"))) {
			p = p.substring(".*".length());
		}
		return p;
	}

	public void setBody(String body) {
		if (header.endsWith(m("Filter.Contains"))) {
			body = ".*" + body + ".*";
		} else if (header.endsWith(m("Filter.EndsWith"))) {
			body = ".*" + body;
		}
		((PatternFilter) filter).setPattern(body);
	}

	private FileTreeFilter createFilter(String header, String body) {
		if (header.endsWith(m("Filter.Contains"))) {
			body = ".*" + body + ".*";
		} else if (header.endsWith(m("Filter.EndsWith"))) {
			body = ".*" + body;
		}

		FileTreeFilter ff = null;
		if (header.startsWith(m("Filter.Name"))) {
			ff = new FileNameFilter(body);
		} else if (header.startsWith(m("Filter.Path"))) {
			ff = new PathFilter(body);
		} else if (header.startsWith(m("Filter.Content"))) {
			ff = new ContentPatternFilter(body);
		}
		return ff;
	}

	public FileTreeFilter getFilter() {
		return filter;
	}

	public void setHeader(String header) {
		String body = (filter == null) ? ".*" : getBody();
		filter = createFilter(header, body);
		this.header = header;

		if (parent != null) {
			int i = parent.getChildren().indexOf(this);
			((CompositeFilter) parent.getFilter()).getFilters().set(i, filter);
		}
	}
}
