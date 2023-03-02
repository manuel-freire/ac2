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
package es.ucm.fdi.ac.parser;

import es.ucm.fdi.ac.*;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Maintains instances of tokenizers for supported languages, returns a good one on demand.
 *
 * Created by mfreire on 22/07/16.
 */
public class AntlrTokenizerFactory implements Analysis.TokenizerFactory {

	private static final Logger log = LogManager
			.getLogger(AntlrTokenizerFactory.class);

	public enum TokenizerEntry {
		// java
		JAVA("es.ucm.fdi.ac.lexers.Java", "compilationUnit",
				new String[] { "ClassBodyDeclarationContext" }, "java"),
		// cpp
		CPP("es.ucm.fdi.ac.lexers.CPP14", "translationUnit",
				new String[] { "DeclarationContext" }, "c", "cpp", "cxx", "h"),
		// vhdl
		VHDL("es.ucm.fdi.ac.lexers.vhdl", "design_file", "vhdl", "vhd"),
		// php
		PHP("es.ucm.fdi.ac.lexers.PHP", "htmlDocument", "php"),
		// xml
		XML("es.ucm.fdi.ac.lexers.XML", "document", "xml", "html", "htm"),
		// js
		JS("es.ucm.fdi.ac.lexers.ECMAScript", "program", "js"),
		// python
		PYTHON("es.ucm.fdi.ac.lexers.Python", "root", "py"),
		// pascal
		PASCAL("es.ucm.fdi.ac.lexers.Pascal", "program", "pas", "inc", "pp");

		public final String pattern;
		public final String[] treeRules;
		public final AntlrTokenizer tokenizer;

		TokenizerEntry(String antlrClass, String entryPoint,
				String... extensions) {
			this(antlrClass, entryPoint, null, extensions);
		}

		TokenizerEntry(String antlrClass, String entryPoint,
				String[] treeRules, String... extensions) {
			this.pattern = "(" + String.join("|", extensions) + ")";
			this.tokenizer = new AntlrTokenizer(antlrClass, entryPoint);
			this.treeRules = treeRules;
		}

		/**
		 * Given a file-name, returns a tokenizer that can handle the file-names'
		 * extension; or null if there is no such tokenizer.
		 * @param name of a file, with an extension indicative of its language.
		 *             Expects "foo.c" or "MyClass.Java", but not "c" or "java" alone
		 * @return a TokenizerEntry, or null if none can handle the file's extension
		 */
		public static TokenizerEntry forName(String name) {
			String suffix = name.substring(name.lastIndexOf('.') + 1);
			for (TokenizerEntry e : TokenizerEntry.values()) {
				if (suffix.matches(e.pattern)) {
					return e;
				}
			}
			return null;
		}
	}

	/**
	 * Returns a tokenizer for a filename with an extension.
	 * @param name of file. Only the extension is looked at.
	 * @return 1st tokenizer that matches the suffix of a given filename, 
	 * or null if none match
	 */
	public Tokenizer getTokenizerFor(String name) {
		TokenizerEntry e = TokenizerEntry.forName(name);
		return e == null ? null : e.tokenizer;
	}

	/**
	 * Returns a tokenizer for a series of submissions. This is done by looking
	 * at the most prevalent extensions - you can force a different tokenizer by
	 * calling getTokenizerFor() instead.
	 * @param subs to look at. Each file in a submission votes once.
	 * @return the most popular tokenizer, based on extensions. If there are 10
	 * files that end in .java, and 12 that end in .c, and 11 are not recognized,
	 * then a C tokenizer will be chosen. Note that files without tokenizers
	 * vote too - and if there are a lot of them, a NullTokenizer will be chosen.
	 */
	@Override
    public Tokenizer getTokenizerFor(Submission[] subs) {
        HashMap<Tokenizer, Integer> votes = new HashMap<>();
        Tokenizer empty = new NullTokenizer();
        Tokenizer best = empty;
		votes.put(best, 0);
        for (Submission sub : subs) {
            for (int i = sub.getSources().size()-1; i >= 0; i-- ) {
                Tokenizer found = getTokenizerFor(sub.getSourceName(i));                
                if (found == null) {
                    found = empty;
                }
				log.debug("vote for " + found + " based on " + sub.getSourceName(i));
                int v = votes.getOrDefault(found, 0) + 1;
                votes.put(found, v);
                if (v > votes.get(best)) {
                    best = found;
                    log.debug("best is " + best + " with " + v);
                }                
            }
        }
        log.info("chosen tokenizer: " + best + " with " + votes.get(best));
        return best;
    }
}
