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
 * XMLSerializable.java
 *
 * Created on June 30, 2009, 8:24 PM
 *
 */
package es.ucm.fdi.util;

import java.io.IOException;
import org.jdom2.Element;

/**
 * Can save state to and from a JDOM Element
 *
 * @author miguelinux
 */
public interface XMLSerializable {
	/**
	 * Saves state to a org.jdom.Element instance
	 *
	 * @return the Element created
	 * @throws IOException on error
	 */
	public Element saveToXML() throws IOException;

	/**
	 * Load state from a org.jdom.Element instance
	 *
	 * @param element the element to be read
	 * @throws IOException on error
	 */
	public void loadFromXML(Element element) throws IOException;
}
