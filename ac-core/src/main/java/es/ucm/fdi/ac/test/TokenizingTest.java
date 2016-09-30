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
 * TokenizingTest.java
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * Contributors: -
 * Changelog:
 *     21-Apr-2006: first version (mfreire)
 */

package es.ucm.fdi.ac.test;

import es.ucm.fdi.ac.Submission;
import es.ucm.fdi.ac.Tokenizer;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.log4j.Logger;
import org.jdom2.Element;

/**
 * Provides support for obtaining tokens from a central repository, 
 * and tokenizes "on-demand". Tests that require tokenization should 
 * extend this class.
 *
 * @author mfreire
 */
public abstract class TokenizingTest extends Test {

	private static final Logger log = Logger.getLogger(TokenizingTest.class);

	protected Tokenizer tokenizer;

	/**
	 * Set the tokenizer to use during testing (usually only preprocessing)
	 * @param t
	 */
	public void setTokenizer(Tokenizer t) {
		this.tokenizer = t;
	}

	/**
	 * Retrieve tokens for the given subject
	 * @param s
	 * @return 
	 */
	public String getTokens(Submission s) {
		return (String) s.getData(Tokenizer.TOKEN_KEY);
	}

	/**
	 * Configures this test
	 * @param e
	 */
	@Override
	public void loadFromXML(Element e) throws IOException {
		super.loadFromXML(e);
		Element te = e.getChild("tokenizer");
		try {
			String tokenizerClassName = te.getAttributeValue("class");
			tokenizer = (Tokenizer) getClass().getClassLoader().loadClass(
					tokenizerClassName).newInstance();
			tokenizer.loadFromXML(e);
		} catch (Exception ex) {
			throw new IOException("Error loading tokenizer", ex);
		}
	}

	/**
	 * Saves state to an element
	 * @param e 
	 */
	protected void saveInner(Element e) throws IOException {
		e.addContent(tokenizer.saveToXML());
	}

	/**
	 * Tokenizes the subject's sources (if they had not yet been tokenized)
	 * @param s
	 */
	public void preprocess(Submission s) {
		String tokens = (String) s.getData(Tokenizer.TOKEN_KEY);
		if (tokens == null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			String currentFile = null;
			//            System.out.println(s.getId() + ":");
			try {
				for (int i = 0; i < s.getSources().size(); i++) {
					currentFile = s.getId() + "/" + s.getSourceName(i);
					tokenizer.tokenize(s.getSourceCode(i), currentFile, pw);
				}
			} catch (Throwable tr) {
				log.warn("Error tokenizing " + currentFile + " from " + s, tr);
			}
			pw.flush();
			sw.flush();
			tokens = sw.toString();
			s.putData(Tokenizer.TOKEN_KEY, tokens);
		}
	}
}
