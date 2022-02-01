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

import es.ucm.fdi.util.XMLSerializable;

import java.io.FileFilter;
import java.io.IOException;
import org.jdom2.Element;

/**
 * A file-tree filter.
 * 
 * @author mfreire
 */
public abstract class FileTreeFilter implements XMLSerializable, FileFilter {

	/**
	 * Tests whether or not the specified node should be accepted
	 *
	 * @param node The node to be tested
	 * @return <code>true</code> if and only if <code>node</code>
	 * should be included in an "accepted" list
	 */
	public abstract boolean accept(FileTreeNode node);

	public Element saveToXML() throws IOException {
		Element filterElement = new Element("filter");
		filterElement.setAttribute("class", this.getClass().getName());
		saveInner(filterElement);
		return filterElement;
	}

	/**
	 * Subclasses should save their details here
	 * @param e 
	 */
	public abstract void saveInner(Element e) throws IOException;
}
