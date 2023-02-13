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

import es.ucm.fdi.ac.parser.AntlrTokenizerFactory.TokenizerEntry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * Created by mfreire on 20/07/16.
 */
public class ParseTreeTest extends TestCase {

	static AntlrTokenizerFactory factory = new AntlrTokenizerFactory();

	public void parseTree(File input, File output) {
		String name = input.getName();
		TokenizerEntry te = TokenizerEntry.forName(name);
		AntlrTokenizer t = te.tokenizer;
		String code = null;
		try {
			code = new String(Files.readAllBytes(input.toPath()), Charset
					.forName("utf-8"));
		} catch (IOException ioe) {
			fail("Could not read file " + input + "; current wd = "
					+ new File(".").getAbsolutePath());
		}
		try (PrintWriter pw = new PrintWriter(new FileWriter(output))) {
			t.tokenize(code, name, pw, true, pw, te.treeRules);
		} catch (IOException ioe) {
			fail("Could not write to output file");
		}
	}	@Test
	public void testTree() {
		parseTree(new File("src/test/resources/huffman.c"), new File(
				"/tmp/output"));
	}
}
