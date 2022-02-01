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
package es.ucm.fdi.ac;

import java.io.IOException;
import java.io.PrintWriter;
import org.jdom2.Element;

/**
 * A tokenizer that does not really tokenize; can be used for non-supported
 * programming languages.
 *
 * @author mfreire
 */
public class NullTokenizer implements Tokenizer {

	/**
	 * Tokenize a java file into a buffer
	 */
	public void tokenize(String source, String sourceFile, PrintWriter out) {
		source = source.replaceAll("\\p{Space}+", " ");
		out.print(source);
	}

	/**
	 * Tokenize a java file into a buffer
	 */
	public void retrieveComments(String source, String sourceFile,
			PrintWriter out) {
		// do not do anything
	}

	public int tokenId(String t) {
		return Character.getNumericValue(t.charAt(0));
	}

	public Element saveToXML() throws IOException {
		Element e = new Element("tokenizer");
		e.setAttribute("class", getClass().getSimpleName());
		return e;
	}

	public void loadFromXML(Element element) throws IOException {
	}
}
