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
package es.ucm.fdi.ac.parser;

import es.ucm.fdi.ac.Tokenizer;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.Trees;
import org.jdom2.Element;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Created by mfreire on 20/07/16.
 */
public class AntlrTokenizer implements Tokenizer {

	public static boolean parse = false;

	private class LanguageSupport {
		public final Constructor<?> lexerConstructor;
		public final Constructor<?> parserConstructor;
		public final Method parserMethod;

		public LanguageSupport(String prefix, String entryPoint) {
			final String lexerClassName = prefix + "Lexer";
			final String parserClassName = prefix + "Parser";

			ClassLoader loader = getClass().getClassLoader();

			try {
				final Class<? extends Lexer> lexerClass = loader.loadClass(
						lexerClassName).asSubclass(Lexer.class);
				final Class<? extends Parser> parserClass = loader.loadClass(
						parserClassName).asSubclass(Parser.class);
				parserMethod = parserClass.getMethod(entryPoint);
				lexerConstructor = lexerClass.getConstructor(CharStream.class);
				parserConstructor = parserClass
						.getConstructor(TokenStream.class);
			} catch (Exception e) {
				throw new IllegalArgumentException(
						"Could not initialize lexer/parser pair for " + prefix,
						e);
			}
		}
	}

	private static HashMap<String, LanguageSupport> languages = new HashMap<String, LanguageSupport>();

	private LanguageSupport language;

	public AntlrTokenizer(String lang, String entryPoint) {
		if (!languages.containsKey(lang)) {
			languages.put(lang, new LanguageSupport(lang, entryPoint));
		}
		this.language = languages.get(lang);
	}

	public void tokenize(String source, String sourceFile, PrintWriter out) {
		try {
			Lexer lexer = (Lexer) language.lexerConstructor
					.newInstance(new ANTLRInputStream(source));
			final CommonTokenStream tokens = new CommonTokenStream(lexer);
			tokens.fill();
			for (final Token tok : tokens.getTokens()) {
				out.print(tok.getType());
				out.print(" ");
				System.err.println(tok);
			}

			if (parse) {
				Parser parser = (Parser) language.parserConstructor
						.newInstance(tokens);
				parser.setErrorHandler(new BailErrorStrategy());
				ParserRuleContext parserRuleContext = (ParserRuleContext) language.parserMethod
						.invoke(parser);

				System.err.println(Trees
						.toStringTree(parserRuleContext, parser));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException(
					"Bad token in source, or failed to parse", e);
		} finally {
			out.flush();
		}
	}

	public void retrieveComments(String source, String sourceFile,
			PrintWriter out) {

	}

	public int tokenId(String token) {
		return 0;
	}

	public Element saveToXML() throws IOException {
		return null;
	}

	public void loadFromXML(Element element) throws IOException {

	}
}
