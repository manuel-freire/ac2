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

import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * Created by mfreire on 20/07/16.
 */
public class AntlrTokenizerTest extends TestCase {

	public AntlrTokenizer getTokenizer(String lang) {
		switch (lang) {
		case "java":
			return new AntlrTokenizer("es.ucm.fdi.ac.lexers.Java",
					"compilationUnit");
		case "c":
			return new AntlrTokenizer("es.ucm.fdi.ac.lexers.CPP14",
					"translationunit");

		case "pas":
			return new AntlrTokenizer("es.ucm.fdi.ac.lexers.Pascal", "program");
		default:
			throw new IllegalArgumentException("not a valid language: " + lang);
		}
	}

	public String tokenize(File input) {
		String name = input.getName();
		String ext = name.substring(name.lastIndexOf('.') + 1);
		AntlrTokenizer t = getTokenizer(ext);
		String code = null;
		try {
			code = new String(Files.readAllBytes(input.toPath()), Charset
					.forName("utf-8"));
		} catch (IOException ioe) {
			fail("Could not read file " + input + "; current wd = "
					+ new File(".").getAbsolutePath());
		}
		StringWriter sw = new StringWriter();
		t.tokenize(code, name, new PrintWriter(sw));
		return sw.toString();
	}

	@Test
	public void testJava() {
		tokenize(new File("src/test/resources/sample.java"));
	}

	@Test
	public void testCpp() {
		tokenize(new File("src/test/resources/sample.c"));
	}

	@Test
	public void testPascal() {
		tokenize(new File("src/test/resources/sample.pas"));
	}
}
