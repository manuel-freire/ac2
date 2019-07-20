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
 * Analysis.java 
 *
 * 18-Apr-2006: first version (mfreire)
 */

package es.ucm.fdi.ac;

import es.ucm.fdi.ac.extract.FileTreeNode;
import es.ucm.fdi.ac.test.Test;
import es.ucm.fdi.util.XMLSerializable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import java.util.HashMap;
import java.util.HashSet;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Instantiates 'test @see{Submission}s' from a source directory 
 * and applies similarity @see{Test}s to them. 
 * Also supports loading results for later analysis, and automatic
 * language recognition (via file extensions...).
 *
 * @author mfreire
 */
public class Analysis implements XMLSerializable {

	private static final Logger log = Logger.getLogger(Analysis.class);

	private static final String VERSION_STRING = "2.0";

	/** Set of sources that are being analyzed. */
	private SourceSet sourceSet;

	/** The submissions being compared. */
	private Submission[] subs;

	/**
	 * Submissions by ID.
	 */
	private final HashMap<String, Submission> idsToSubs = new HashMap<String, Submission>();

	/** 
	 * Currently applied tests. 
	 * Results are available in each individual submission
	 */
	private final HashSet<Test> appliedTests;

	public interface TokenizerFactory {
		Tokenizer getTokenizerFor(Submission[] subs);
	}

	private static TokenizerFactory tokenizerFactory;

	public static void setTokenizerFactory(TokenizerFactory tokenizerFactory) {
		Analysis.tokenizerFactory = tokenizerFactory;
	}

	/**
	 * Minimal initialization
	 */
	public Analysis() {
		sourceSet = null;
		appliedTests = new HashSet<>();
		subs = new Submission[0];
	}

	/**
	 * Initialize subjects from a FileTreeNode.
	 *
	 * Allows complex filters and 'virtual' files (using the SourceFileCache).
	 * After filtering, the resulting filteredTree is expected to contain, on
	 * the first level, one folder per submission (folder name to be used as ID);
	 * and under each submission-folder, the files that will be analyzed.
	 * 
	 * @param sources to load
	 * @throws java.io.IOException on error
	 */
	public void loadSources(SourceSet sources) throws IOException {
		this.sourceSet = sources;
		FileTreeNode root = sources.getFilteredTree();

		if (root == null) {
			throw new IllegalArgumentException("nothing to analyze");
		}

		HashMap<String, Submission> unique = new HashMap<>();
		idsToSubs.clear();
		int i = 0;
		for (FileTreeNode dn : root.getChildren()) {
			addSubmissionRecursive(dn, unique, i);
		}

		subs = new Submission[unique.size()];
        i = 0;
		for (Submission s : unique.values()) {
            subs[i++] = s;
			idsToSubs.put(s.getId(), s);
		}
	}

	private void addSubmissionRecursive(FileTreeNode tn,
			HashMap<String, Submission> unique, int i) {
		if (tn.getFile().isDirectory()) {
			for (FileTreeNode fn : tn.getLeafChildren()) {
				addSubmissionRecursive(fn, unique, i);
				i++;
			}
		} else {
			if (tn.getFile().getName().startsWith(".")) // skip hidden file such as .DS_Store
				return;
			Submission s = new Submission(tn.getLabel(), tn.getPath(), 0);
			log.info("created sub " + s.getId());
			for (FileTreeNode fn : tn.getLeafChildren()) {
				log.debug(" - " + fn.getFile().getName());
				s.addSource(fn.getFile());
			}

			if (!unique.containsKey(s.getHash())) {
				unique.put(s.getHash(), s);
				s.setInternalId(i);
			} else {
				Submission p = unique.get(s.getHash());
				log.warn("Detected EXACT duplicate: " + s.getHash() + "\n"
						+ " - " + p.getId() + " (" + p.getOriginalPath()
						+ ")\n" + " - " + s.getId() + " ("
						+ s.getOriginalPath() + ")\n");
			}
		}
	}

	/**
	 * Choose the right tokenizer for a given file
	 */
	public Tokenizer chooseTokenizer() {
		return tokenizerFactory.getTokenizerFor(subs);
	}

	/**
	 * @return true if there are available results for this testKey
	 */
	public boolean hasResultsForKey(String testKey) {
		if (subs.length == 0) {
			return false;
		}
		Submission first = subs[0];
		return (first.getData(testKey) != null);
	}

	/**
	 * Preprocess files
	 */
	public void prepareTest(Test t) {
		t.setProgress(0f);
		t.setCancelled(false);

		t.init(subs);

		for (int i = 0; i < subs.length; i++) {
			if (t.isCancelled()) {
				return;
			}
			try {
				NDC.push("Pre-" + subs[i].getId());
				t.preprocess(subs[i]);
				NDC.pop();
			} catch (Throwable re) {
				throw new RuntimeException("Error during pre-processing "
						+ subs[i].getId(), re);
			}
			t.setProgress(i / (float) subs.length);
		}
		t.setProgress(1f);
	}

	private void applyParallelizedTest(Test t) {
		int nProc = Runtime.getRuntime().availableProcessors();
		int slices[] = calculateSliceSizes(nProc, subs.length);

		t.setProgress(0f);
		t.setCancelled(false);
		float[][] F = new float[subs.length][subs.length];

		// launch all jobs, first one with "monitor" set to "true"
		Thread threads[] = new Thread[nProc];
		SimilarityJob lastJob = null;
		for (int i = 0; i < nProc; i++) {
			lastJob = new SimilarityJob(F, t, slices[i], slices[i + 1], i == 0);
			threads[i] = new Thread(lastJob);
			threads[i].start();
		}

		// join them afterwards
		for (int i = 0; i < nProc; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException ie) {
				log.warn("I hate being interrupted. Test aborted");
				t.setCancelled(true);
			}
		}

		lastJob.end();
	}

	/**
	 * Apply a single test
	 */
	public void applyTest(Test t) {
		if (t.isIndependentSimilarity()) {
			applyParallelizedTest(t);
			return;
		}

		t.setProgress(0f);
		t.setCancelled(false);

		float[][] F = new float[subs.length][subs.length];
		SimilarityJob job = new SimilarityJob(F, t, 0, subs.length, true);
		job.run();
		job.end();
	}

	/**
	 * Return the limits of each slice in which to divide a large matrix-based job.
	 * Slice limits correspond to indices, where
	 * L[i+1]*L[i+1] - L[i]*L[i] = mSize*mSize/nJobs;
	 * @param mSize rows in the square matrix that is to be split up
	 * @return an array of nJobs+1 limits
	 */
	private static int[] calculateSliceSizes(int nJobs, int mSize) {
		int x = mSize * mSize / nJobs;
		int slices[] = new int[nJobs + 1];
		slices[0] = 0;
		slices[nJobs] = mSize;
		for (int i = 0; i < nJobs - 1; i++) {
			slices[i + 1] = (int) Math.sqrt(x + slices[i] * slices[i]);
		}
		return slices;
	}

	private class SimilarityJob implements Runnable {

		private final float[][] F;
		private final int startIdx, endIdx;
		private final Test t;
		private final boolean monitor;

		public SimilarityJob(float[][] F, Test t, int startIdx, int endIdx,
				boolean monitor) {
			this.F = F;
			this.t = t;
			this.startIdx = startIdx;
			this.endIdx = endIdx;
			this.monitor = monitor;
		}

		public void run() {
			log.debug("I am a job from " + startIdx + " to " + endIdx);
			int total = (endIdx * endIdx - startIdx * startIdx) / 2;
			for (int i = startIdx, k = 0; i < endIdx; i++) {
				for (int j = 0; j < i; j++, k++) {
					if (t.isCancelled()) {
						return;
					}
					try {
						F[i][j] = F[j][i] = t.similarity(subs[i], subs[j]);
					} catch (Throwable re) {
						t.setCancelled(true);
						throw new RuntimeException("Error comparando "
								+ subs[i].getId() + " con " + subs[j].getId(),
								re);
					}
					if (monitor) {
						t.setProgress(k / (float) total);
					}
				}
			}
		}

		/**
		 * ends the job for all threads; should only be called on one of them
		 */
		public void end() {
			if (!t.isCancelled()) {
				for (int i = 0; i < subs.length; i++) {
					subs[i].putData(t.getTestKey(), F[i]);
				}
				t.setProgress(1f);
				appliedTests.add(t);
			}
		}
	}

	/**
	 * Retrieve an already-run test by key
	 * @param key for the test
	 * @return the test that was run, or null if not found
	 */
	public Test getTestByKey(String key) {
		for (Test t : appliedTests) {
			if (t.getTestKey().equals(key)) {
				return t;
			}
		}
		return null;
	}

	public Submission[] getSubmissions() {
		return subs;
	}

	public HashSet<Test> getAppliedTests() {
		return appliedTests;
	}

	public Submission getSubmission(String id) {
		return idsToSubs.get(id);
	}

	/**
	 * Sort results by decreasing similarity
	 */
	public Result[] sortTestResults(String testKey) {
		Result[] P = new Result[subs.length * (subs.length - 1) / 2];
		for (int i = 0, k = 0; i < subs.length; i++) {
			float[] F = (float[]) subs[i].getData(testKey);
			for (int j = 0; j < i; j++, k++) {
				P[k] = new Result(subs[i], subs[j], F[j]);
			}
		}
		Arrays.sort(P);
		return P;
	}

	/**
	 * Inner class, representing a Result. Good for sorting
	 */
	public static class Result implements Comparable {
		private Submission a, b;
		private float d;

		public Result(Submission a, Submission b, float d) {
			this.a = a;
			this.b = b;
			this.d = d;
		}

		public int compareTo(Object o) {
			float f = d - ((Result) o).d;
			return f > 0 ? 1 : (f < 0 ? -1 : 0);
		}

		@Override
		public String toString() {
			return "" + d + " " + a.getId() + " " + b.getId();
		}

		public Submission getA() {
			return a;
		}

		public Submission getB() {
			return b;
		}

		public float getDist() {
			return d;
		}
	}

	/**
	 * Saves the analysis
	 * @return an element that contains the whole document
	 * @throws IOException 
	 */
	public Element saveToXML() throws IOException {
		Element root = new Element("analysis");
		root.setAttribute("version", VERSION_STRING);
		root.setAttribute("created", new SimpleDateFormat("dd/MM/yyyy HH:mm")
				.format(new Date()));

		root.addContent(sourceSet.saveToXML());

		Element annotations = new Element("annotations");
		for (Submission s : subs) {
			if (!s.getAnnotations().isEmpty()) {
				annotations.addContent(s.saveToXML());
			}
		}
		root.addContent(annotations);

		Element tests = new Element("tests");
		for (Test t : appliedTests) {
			if (!t.isCancelled()) {
				tests.addContent(t.saveToXML());
			}
		}
		root.addContent(tests);

		return root;
	}

	/**
	 * Loads the analysis
	 * @param root
	 * @throws IOException 
	 */
	public void loadFromXML(Element root) throws IOException {
		String version = root.getAttributeValue("version");
		if (!version.equals(VERSION_STRING)) {
			log.warn("Loading from different version (" + version + "); "
					+ " but this program uses save-version " + VERSION_STRING);
		}

		log.info("Loading sources...");
		sourceSet.loadFromXML(root.getChild("sources"));
		loadSources(sourceSet);

		log.info("Loading annotations...");
		for (Element se : root.getChild("annotations").getChildren()) {
			Submission sub = getSubmission(se.getAttributeValue("id"));
			sub.loadFromXML(se);
		}

		log.info("Loading tests...");
		ArrayList<Test> pendingTests = new ArrayList<Test>();
		for (Element te : root.getChild("tests").getChildren()) {
			String tcn = te.getAttributeValue("class");
			try {
				Test t = (Test) getClass().getClassLoader().loadClass(tcn)
						.newInstance();
				t.loadFromXML(te);
				pendingTests.add(t);
			} catch (Exception ex) {
				throw new IOException("Could not load test " + tcn);
			}
		}

		// now, run tests in an order that satisfies dependencies
		while (!pendingTests.isEmpty()) {
			boolean progress = false;
			for (Test candidate : pendingTests) {
				boolean dependenciesMet = false;
				for (String k : candidate.getRequires()) {
					if (!hasResultsForKey(k)) {
						log.info("Cannot execute " + candidate.getClass()
								+ " without " + k + ": postponing");
						dependenciesMet = false;
						break;
					}
				}
				if (dependenciesMet) {
					log.info("Dependencies for " + candidate.getClass()
							+ " satisfied, processing");
					progress = true;
					applyTest(candidate);
					pendingTests.remove(candidate);
				}
			}
			if (!progress) {
				throw new IOException(
						"Impossible to meet dependencies for tests");
			}
		}
	}

	/**
	 * Reads an analysis from a file
	 *
	 * @param f the file to read from
	 * @throws IOException on any error (may wrap invalid internal XML errors)
	 */
	public void loadFromFile(File f) throws IOException {
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(f);
			loadFromXML(doc.getRootElement());
		} catch (Exception e) {
			throw new IOException("Error loading from '" + f.getAbsolutePath()
					+ "' xml save file", e);
		}
	}

	/**
	 * Saves this analysis to a file
	 *
	 * @param f the file to write to 
	 * @throws java.io.IOException
	 */
	public void saveToFile(File f) throws IOException {
		FileOutputStream fos = null;
		try {
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			fos = new FileOutputStream(f);
			outputter.output(new Document(saveToXML()), fos);
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}
}
