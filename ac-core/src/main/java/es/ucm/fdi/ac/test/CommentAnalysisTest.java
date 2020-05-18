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
 * ComentAnalysisTest.java 
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * Contributors: - 
 * Changelog: 
 *     20-Jul-2006: first version (mfreire)
 */

package es.ucm.fdi.ac.test;

import es.ucm.fdi.ac.Submission;
import es.ucm.fdi.ac.Tokenizer;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Tests that analyze comments should implement this.
 * Since valid comments are language-dependent, the relevant parser is 
 * used to locate comment strings.
 *
 * @author mfreire
 */
public abstract class CommentAnalysisTest extends TokenizingTest {

	// key that identifies the comments for this file
	private final static String COMMENT_KEY = "COMMENT_KEY";

	public String getComments(Submission s) {
		return (String) s.getData(COMMENT_KEY);
	}

	/**
	 * Tokenizes the subject's sources (if they had not yet been tokenized)
	 */
	public void preprocess(Submission s) {
		super.preprocess(s);
		String data = (String) s.getData(COMMENT_KEY);
		if (data == null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			String currentFile = null;
			try {
				for (int i = 0; i < s.getSources().size(); i++) {
					currentFile = s.getId() + "/" + s.getSourceName(i);
					tokenizer.retrieveComments(s.getSourceCode(i), currentFile,
							pw);
				}
			} catch (Throwable tr) {
				System.err.println("Error retrieving comments from "
						+ currentFile + ":\n\t" + tr.getMessage());
				tr.printStackTrace();
			}
			pw.flush();
			sw.flush();
			data = sw.toString();
			s.putData(Tokenizer.TOKEN_KEY, data);
		}
	}
}
