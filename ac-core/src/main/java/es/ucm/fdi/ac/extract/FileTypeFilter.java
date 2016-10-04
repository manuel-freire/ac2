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

import java.io.File;
import java.io.IOException;
import org.jdom2.Element;

/**
 * A filter that uses some sort of pattern to determine validity
 *
 * @author mfreire
 */
public class FileTypeFilter extends FileTreeFilter {

	public enum Type {
		File, Directory
	};

	protected Type type;

	public FileTypeFilter() {
	}

	public FileTypeFilter(Type type) {
		this.type = type;
	}


	public boolean accept(File f) {
		switch (type) {
			case Directory:
				return f.isDirectory();
			case File:
				return f.isFile();
			default:
				throw new IllegalStateException("Invalid type in filter: " + type);
		}
	}
	public boolean accept(FileTreeNode ftn) {
		return accept(ftn.getFile());
	}

	public void loadFromXML(Element e) throws IOException {
		type = Type.valueOf(e.getAttributeValue("type"));
	}

	@Override
	public void saveInner(Element e) throws IOException {
		e.setAttribute("type", type.name());
	}
}
