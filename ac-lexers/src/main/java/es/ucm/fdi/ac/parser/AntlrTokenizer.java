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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.HashMap;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.jdom2.Element;

import es.ucm.fdi.ac.Tokenizer;

/**
 * Created by mfreire on 20/07/16.
 */
public class AntlrTokenizer implements Tokenizer {

	private static final Logger log = LogManager
			.getLogger(AntlrTokenizer.class);

	@SuppressWarnings("unchecked")
	private class LanguageSupport {
		public final Constructor<? extends Lexer> lexerConstructor;
		public final Constructor<? extends Parser> parserConstructor;
		public final Method parserMethod;

		public LanguageSupport(String prefix, String entryPoint) {
			final String lexerClassName = prefix + "Lexer";
			final String parserClassName = prefix + "Parser";

			ClassLoader loader = getClass().getClassLoader();

			try {
				final Class<? extends Lexer> lexerClass = loader.loadClass(
						lexerClassName).asSubclass(Lexer.class);
				final Class<? extends Parser> parserClass = (Class<? extends Parser>) loader
						.loadClass(parserClassName);
				parserMethod = parserClass.getMethod(entryPoint);
				lexerConstructor = lexerClass.getConstructor(CharStream.class);
				parserConstructor = parserClass
						.getConstructor(TokenStream.class);
			} catch (Exception e) {

				log.error("Could not initialize lexer/parser pair for "
						+ prefix, e);
				throw new RuntimeException(
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

	public static class TokensAndParseTree {
		public final CommonTokenStream tokens;
		public final ParserRuleContext context;

		public TokensAndParseTree(CommonTokenStream tokens,
				ParserRuleContext context) {
			this.tokens = tokens;
			this.context = context;
		}
	}

	public TokensAndParseTree tokenizeAndParse(String source) {
		try {
			CommonTokenStream tokens = new CommonTokenStream(lexerFor(source));
			tokens.fill();
			Parser parser = language.parserConstructor.newInstance(tokens);
			ParserRuleContext context = callEntryPointOnParser(parser);
			return new TokensAndParseTree(tokens, context);
		} catch (Exception e) {
			log.error("Error building lexer/parser pair for source", e);
			throw new RuntimeException(
					"Error building lexer/parser pair for source", e);
		}
	}

	public ParserRuleContext callEntryPointOnParser(Parser p) {
		try {
			return (ParserRuleContext) language.parserMethod.invoke(p);
		} catch (Exception e) {
			throw new IllegalArgumentException("Error invoking entry point", e);
		}
	}

	public Lexer lexerFor(String source) {
		try {
			return language.lexerConstructor.newInstance(CharStreams
					.fromString(source));
		} catch (Exception e) {
			throw new IllegalArgumentException("Error building lexer", e);
		}
	}

	public CommonTokenStream tokenStreamFor(String source, String sourceFile) {
		try {
			Lexer lexer = lexerFor(source);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			tokens.fill();
			return tokens;
		} catch (Exception e) {
			log.warn("Error tokenizing {}", sourceFile, e);
			throw new IllegalArgumentException(
					"Error tokenizing " + sourceFile, e);
		}
	}

	/**
	 * Outputs a (single-space-delimited, base-32 by default) token stream with the 
	 * result of tokenizing a source.
	 * @param source to tokenize
	 * @param sourceFile name, to include in logs/error reports, if any
	 * @param out output
	 */
	@Override
	public void tokenize(String source, String sourceFile, PrintWriter out) {
		tokenize(source, sourceFile, out, false, null, null);
	}

	public void tokenize(String source, String sourceFile, PrintWriter out,
			boolean parse, PrintWriter treeOut, String[] rules) {
		Writer debugWriter = null;
		try {
			CommonTokenStream tokens = tokenStreamFor(source, sourceFile);
			if (log.isDebugEnabled()) {
				try {
					debugWriter = new BufferedWriter(new FileWriter(Files
							.createTempFile(
									"tokens-" + ThreadContext.peek() + "-",
									".txt").toFile()));
				} catch (IOException ioe) {
					log.warn("Could not create debugWriter", ioe);
				}
			}

			for (final Token tok : tokens.getTokens()) {
				out.print(tokenToString(tok));
				if (log.isDebugEnabled()) {
					log.debug(tok);
					if (debugWriter != null) {
						debugWriter.write(tokenToString(tok));
					}
				}
			}
		} catch (IOException ioe) {
			throw new RuntimeException(
					"Error writing tokens for " + sourceFile, ioe);
		} finally {
			out.flush();
			if (log.isDebugEnabled() && debugWriter != null) {
				try {
					debugWriter.close();
				} catch (IOException ioe) {
					log.warn("Could not close debugWriter", ioe);
				}
			}
		}
	}

	private String tokenToString(Token token) {
		return "" + Integer.toString(token.getType(), 32) + " ";
	}

	public int tokenId(String token) {
		return Integer.parseInt(token, 32);
	}

	public void retrieveComments(String source, String sourceFile,
			PrintWriter out) {
		throw new UnsupportedOperationException("Not yet supported");
	}

	@Override
	public Element saveToXML() throws IOException {
		throw new UnsupportedOperationException("Not yet supported");
	}

	@Override
	public void loadFromXML(Element element) throws IOException {
		throw new UnsupportedOperationException("Not yet supported");
	}
}
