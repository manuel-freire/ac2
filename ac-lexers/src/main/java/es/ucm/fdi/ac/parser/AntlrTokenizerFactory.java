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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Maintains instances of tokenizers for supported languages, returns a good one on demand.
 *
 * Created by mfreire on 22/07/16.
 */
public class AntlrTokenizerFactory implements Analysis.TokenizerFactory {

	private static final Logger log = LogManager
			.getLogger(AntlrTokenizerFactory.class);

	static HashMap<String, AntlrTokenizer> tokenizers;

	static private void addTokenizer(String antlrClass, String entryPoint,
			String... extensions) {
		String extensionRegex = "(" + String.join("|", extensions) + ")";
		tokenizers.put(extensionRegex, new AntlrTokenizer(antlrClass,
				entryPoint));
	}

	static void initTokenizers() {
        if (tokenizers != null) return;

        tokenizers = new HashMap<>();
        addTokenizer("es.ucm.fdi.ac.lexers.Java", "compilationUnit", "java");
        addTokenizer("es.ucm.fdi.ac.lexers.CPP14", "translationunit", "c", "cpp", "cxx", "h");
        addTokenizer("es.ucm.fdi.ac.lexers.vhdl", "design_file", "vhdl", "vhd");
        addTokenizer("es.ucm.fdi.ac.lexers.PHP", "htmlDocument", "php");
        addTokenizer("es.ucm.fdi.ac.lexers.XML", "document", "xml", "html", "htm");
        addTokenizer("es.ucm.fdi.ac.lexers.ECMAScript", "program", "js");
        addTokenizer("es.ucm.fdi.ac.lexers.Python", "root", "py");
        addTokenizer("es.ucm.fdi.ac.lexers.Pascal", "program", "pas", "inc", "pp");        
    }

	@Override
    public Tokenizer getTokenizerFor(Submission[] subs) {
        initTokenizers();

        HashMap<Tokenizer, Integer> votes = new HashMap<>();
        Tokenizer empty = new NullTokenizer();
        votes.put(empty, 0);
        Tokenizer best = empty;
        for (Submission sub : subs) {
            for (int i = sub.getSources().size()-1; i >= 0; i-- ) {
                String name = sub.getSourceName(i);
                String suffix = name.substring(name.lastIndexOf('.')+1);
                boolean found = false;
                for (Map.Entry<String, AntlrTokenizer> te : tokenizers.entrySet()) {
                    Tokenizer current = te.getValue();
                    if (suffix.matches(te.getKey())) {
                        found = true;
                        Integer v = votes.get(current);
                        int t = (v == null) ? 1 : v+1;
                        votes.put(current, t);
                        if (t > votes.get(best)) {
                            best = current;
                            log.debug("best is " + best + " with " + t);
                        }
                    }
                }
                if ( ! found) {
                    int t = votes.get(empty) + 1;
                    votes.put(empty, t);
                    if (t > votes.get(best)) {
                        best = empty;
                        log.debug("best is " + best + " with " + t);
                    }
                }
            }
        }
        log.info("chosen tokenizer: " + best + " with " + votes.get(best));
        return best;
    }
}
