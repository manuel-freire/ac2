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

import es.ucm.fdi.ac.extract.FileTreeModel;
import es.ucm.fdi.ac.extract.FileTreeNode;
import es.ucm.fdi.ac.parser.AntlrTokenizerFactory;
import es.ucm.fdi.ac.test.NCDTest;
import es.ucm.fdi.ac.test.Test;
import es.ucm.fdi.ac.test.TokenizingTest;
import es.ucm.fdi.util.archive.ZipFormat;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * A command-line interface to AC
 *
 * @author mfreire
 */
public class CommandLineMain {

	/**
	 * This launches a very simple analysis, comparing the subdirectories
	 * of the passed-in directory to each other.
	 * @param test to apply
	 * @param dirNames, each of which contains subdirectories to compare
	 * @throws IOException on IO error
	 */
	public static void simpleAnalysis(Test test, Collection<String> dirNames)
			throws IOException {

		// load all sources
		FileTreeModel ftm = new FileTreeModel();
		for (String dirName : dirNames) {
			File root = new File(dirName);
			if (root == null) {
				throw new IOException("No such file: " + dirName);
			}
			if (!root.isDirectory()) {
				throw new IOException("Not a directory: " + dirName);
			}
			for (File f : root.listFiles()) {
				ftm.addSource(f);
			}
		}
		SourceSet ss = new SourceSet((FileTreeNode) ftm.getRoot());
		Analysis ac = new Analysis();
		ac.loadSources(ss);

		// prepare tokenization
		Analysis.setTokenizerFactory(new AntlrTokenizerFactory());
		if (test instanceof TokenizingTest) {
			((TokenizingTest) test).setTokenizer(ac.chooseTokenizer());
		}

		// launch test
		ac.prepareTest(test);
		ac.applyTest(test);

		// report results
		Analysis.Result rs[] = ac.sortTestResults(test.getTestKey());
		System.out
				.println("Results in CSV format, sorted by increasing distance");
		System.out
				.println("Distance (0=same, 1=very different),StudentA,StudentB");
		for (Analysis.Result r : rs) {
			System.out.println(String.format("%1.2f,%s,%s", r.getDist(), r
					.getA(), r.getB()));
		}
		System.out.println("Test finished! Results available!");
		for (Submission s : ac.getSubmissions()) {
			for (Annotation a : s.getAnnotations()) {
				System.out.println("ANNOTATION FOR " + s.getId() + ":\n\t"
						+ "Target: " + a.getTarget() + "\n\t" + a.getLabels()
						+ " (created by " + a.getAuthor() + " at "
						+ a.getDate() + ")\n\t" + a.getCommentary());
			}
		}
	}

	/**
	 * Main entrypoint
	 *
	 * @param args
	 */
	public static void main(String... args) throws Exception {
		ArgumentParser parser = ArgumentParsers
				.newFor("ac2")
				.build()
				.defaultHelp(true)
				.description(
						"Compare assignment submissions in subfolders to each other.");
		parser
				.addArgument("dirs")
				.nargs("*")
				.help(
						"Directories to include in comparison. Subdirectories of each should be submissions");
		Namespace ns = null;
		try {
			ns = parser.parseArgs(args);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		}

		Test t = new NCDTest(new ZipFormat());
		simpleAnalysis(t, ns.getList("dirs"));
	}
}
