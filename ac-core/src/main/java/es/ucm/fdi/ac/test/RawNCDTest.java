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
 * RawNCDTest.java 
 * Jan-4-2007: first version
 */

package es.ucm.fdi.ac.test;

import es.ucm.fdi.ac.Submission;
import es.ucm.fdi.ac.Submission.Source;
import es.ucm.fdi.util.archive.ArchiveFormat;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.jdom2.Element;

/**
 * This test measures how much distance there is from one subject to another,
 * based on the different sizes achieved after compression. Compression algorithm
 * and preprocessing can be changed. No tokenization is applied.
 *
 * R. Cilibrasi, P.M.B. Vitanyi, Clustering by compression, IEEE Trans.
 *    Information Theory, 51:4(2005), 1523- 1545. Also:
 *    http://xxx.lanl.gov/abs/cs.CV/031204 (2003).
 *
 * @author edelrosal, mfreire
 */
public class RawNCDTest extends Test {

	private static final Logger log = Logger.getLogger(RawNCDTest.class);

	static final public String rawNcdSimilaritySuffix = "_raw_ncd_sim";
	static final public String rawNcdSizeSuffix = "_raw_ncd_size";
	static final public String rawNcdSourceSuffix = "_raw_ncd_source";

	private String sizeKey;
	private String sourceKey;
	private ArchiveFormat compressor;
	private boolean ignoreWhitespace;

	/**
	 * Creates a new instance of RawNCDTest. Uses default options:
	 * the compressor extension as name, and no whitespace removal
	 * @param compressor to use
	 */
	public RawNCDTest(ArchiveFormat compressor) {
		this(compressor, false, compressor.getClass().getSimpleName()
				.replaceAll("Format", ""));
	}

	/**
	 * Creates a new RawNCDTest with the given compressor and key.
	 * @param compressor to use
	 * @param ignoreWhitespace if true, all runs of whitespace will be replaced
	 * with a single space during preprocessing
	 * @param keyPrefix prefix to use when storing test results
	 */
	public RawNCDTest(ArchiveFormat compressor, boolean ignoreWhitespace,
			String keyPrefix) {
		this.independentPreprocessing = true;
		this.independentSimilarity = true;
		this.compressor = compressor;
		this.ignoreWhitespace = ignoreWhitespace;
		testKey = keyPrefix + rawNcdSimilaritySuffix;
		sizeKey = keyPrefix + rawNcdSizeSuffix;
		sourceKey = keyPrefix + rawNcdSourceSuffix;
	}

	/**
	 * Configures this test
	 * @param e
	 */
	@Override
	public void loadFromXML(Element e) throws IOException {
		super.loadFromXML(e);
		sizeKey = e.getAttributeValue("sizeKey");
		sourceKey = e.getAttributeValue("sourceKey");
		ignoreWhitespace = Boolean.parseBoolean(e
				.getAttributeValue("ignoreWhitespace"));
	}

	/**
	 * Saves state to an element
	 * @param e 
	 */
	protected void saveInner(Element e) throws IOException {
		e.setAttribute("compressor", compressor.getClass().getName());
		e.setAttribute("sizeKey", sizeKey);
		e.setAttribute("sourceKey", sizeKey);
		e.setAttribute("ignoreWhitespace", "" + ignoreWhitespace);
	}

	private int getSize(Submission s) {
		return ((Integer) s.getData(sizeKey)).intValue();
	}

	/**
	 * All subjects will have been preprocessed before similarity is
	 * checked.
	 */
	public void preprocess(Submission s) {

		StringBuilder sourceString = new StringBuilder();
		for (Source source : s.getSources()) {
			sourceString.append(source);
		}
		String source = sourceString.toString();

		if (ignoreWhitespace) {
			source = source.replaceAll("\\p{Space}+", " ");
		}

		int size = -1;
		try {
			size = compressor.compressedSize(new ByteArrayInputStream(source
					.getBytes()));
		} catch (IOException e) {
			log.warn("Exception during preprocess", e);
		}
		//     System.out.println("compressed size for "+s.getId()+" is "+size);
		s.putData(sizeKey, new Integer(size));
		s.putData(sourceKey, source);
	}

	/**
	 * @return a number between 0 (most similar) and 1 (least similar)
	 */
	public float similarity(Submission sa, Submission sb) {
		try {
			String concatenation = (String) sa.getData(sourceKey)
					+ (String) sb.getData(sourceKey);
			int a = getSize(sa);
			int b = getSize(sb);
			int c = compressor.compressedSize(new ByteArrayInputStream(
					concatenation.getBytes()));
			int m = Math.min(a, b);
			int M = a + b - m;
			//       System.out.println("| zip ("+sa.getId()+" + "+sb.getId()+") | = "+c);
			return (float) (c - m) / (float) M;
		} catch (IOException e) {
			log.warn("Exception during similarity comparison", e);
		}
		return -1f;
	}
}
