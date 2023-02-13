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
package es.ucm.fdi.ac.walkers;

import es.ucm.fdi.ac.parser.AntlrTokenizer;
import es.ucm.fdi.ac.parser.AntlrTokenizerFactory;
import es.ucm.fdi.ac.parser.AntlrTokenizerFactory.TokenizerEntry;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class SourceAligner implements ParseTreeListener {

	static final Logger log = LogManager.getLogger(SourceAligner.class);

	public ReNode parseTree(String source) {
		return null;
	}

	public void enterEveryRule(ParserRuleContext context) {

	}

	public void exitEveryRule(ParserRuleContext context) {

	}

	public void visitTerminal(TerminalNode node) {

	}

	public void visitErrorNode(ErrorNode node) {

	}

	public RuleWalker parseTree(File input, File output) {
		String name = input.getName();
		//retainedRules = Set.of(AntlrTokenizerFactory.TokenizerEntry.forName(name).treeRules);
		//ids = new HashMap<>();

		TokenizerEntry te = TokenizerEntry.forName(name);
		AntlrTokenizer t = te.tokenizer;
		String code = null;
		try {
			code = new String(Files.readAllBytes(input.toPath()), Charset
					.forName("utf-8"));
			log.info("Read {} bytes", code.length());
		} catch (IOException ioe) {
			log.warn("Could not read file " + input + "; current wd = "
					+ new File(".").getAbsolutePath());
		}
		try (PrintWriter out = new PrintWriter(new FileOutputStream(output))) {
			AntlrTokenizer.TokensAndParseTree tnt = t.tokenizeAndParse(code);
			tnt.tokens.fill();
			//root = null;
			ParseTreeWalker.DEFAULT.walk(this, tnt.context);
			ArrayList<P> ps = new ArrayList<>();
			//for (Map.Entry<String, Integer>e : ids.entrySet()) ps.add(new P(e));
			Collections.sort(ps);
			log.info(ps);
			
			//root.fixStartsAndEnds(tnt.tokens);
			//root.write(code, out, null);			
		} catch (Exception e) {
			log.warn("Could not write to output file", e);
		}
		// FIXME
		return null;
	}

	private static class P implements Comparable<P> {
		public String s;
		public int c;

		public P(Map.Entry<String, Integer> e) {
			this.s = e.getKey();
			this.c = e.getValue();
		}

		public String toString() {
			return "" + c + " -> " + s + "\n";
		}

		public int compareTo(P p) {
			return Integer.compare(c, p.c);
		}
	}

	public static void main(String[] args) {
		SourceAligner jaywalker = new SourceAligner();
		/*		
		 jaywalker.parseTree(
		 new File(
		 "/home/mfreire/code/ac2/ac-lexers/src/test/resources/JuegoEscoba.java"),
		 //						"/home/mfreire/code/ac2/ac-lexers/src/test/resources/huffman.c"),
		 new File("/tmp/output"));
		 */
		String f1 = "/home/mfreire/Dropbox/text/2022-02-analisis-copias-urjc/sus/14_JJMH_Ord/";
		String f2 = "/home/mfreire/Dropbox/text/2022-02-analisis-copias-urjc/sus/18_JGL_Ord/Clases/";
		jaywalker.parseTree(new File(f1 + "Partida.java"), new File(
				"/tmp/a.java"));
		jaywalker.parseTree(new File(f2 + "Partida.java"), new File(
				"/tmp/b.java"));
	}
}
