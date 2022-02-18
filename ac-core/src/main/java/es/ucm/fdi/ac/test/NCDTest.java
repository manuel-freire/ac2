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
package es.ucm.fdi.ac.test;

import es.ucm.fdi.ac.Submission;
import es.ucm.fdi.util.archive.ArchiveFormat;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jdom2.Element;

/**
 * This test measures how much distance there is from one subject to another,
 * based on the different sizes achieved after compression. Compression algorithm
 * and preprocessing can be changed.
 *
 * R. Cilibrasi, P.M.B. Vitanyi, Clustering by compression, IEEE Trans.
 *    Information Theory, 51:4(2005), 1523- 1545. Also:
 *    http://xxx.lanl.gov/abs/cs.CV/031204 (2003).
 *
 * @author mfreire
 */
public class NCDTest extends TokenizingTest {

	private static final Logger log = LogManager.getLogger(NCDTest.class);

	static final public String sizeKeySuffix = "_ncd_size";
	static final public String similarityKeySuffix = "_ncd_sim";

	private String sizeKey;
	private ArchiveFormat compressor;

	/**
	 * Creates a new instance of NCDTest
	 * @param compressor to use
	 */
	public NCDTest(ArchiveFormat compressor) {
		this(compressor, compressor.getClass().getSimpleName().replaceAll(
				"Format", ""));
	}

	/**
	 * Creates a new NCDTest with the given compressor and key.
	 */
	public NCDTest(ArchiveFormat compressor, String keyPrefix) {
		this.independentPreprocessing = true;
		this.independentSimilarity = true;
		this.compressor = compressor;
		sizeKey = keyPrefix + sizeKeySuffix;
		testKey = keyPrefix + similarityKeySuffix;
	}

	/**
	 * Configures this test
	 * @param e
	 */
	@Override
	public void loadFromXML(Element e) throws IOException {
		super.loadFromXML(e);
		sizeKey = e.getAttributeValue("sizeKey");
	}

	/**
	 * Saves state to an element
	 * @param e 
	 */
	protected void saveInner(Element e) throws IOException {
		super.saveInner(e);
		e.setAttribute("compressor", compressor.getClass().getName());
		e.setAttribute("sizeKey", sizeKey);
	}

	public int getCompSize(Submission s) {
		return ((Integer) s.getData(sizeKey)).intValue();
	}

	/**
	 * All subjects will have been preprocessed before similarity is 
	 * checked.
	 */
	public void preprocess(Submission s) {
		super.preprocess(s);

		String tokens = getTokens(s);

		int size = -1;
		try {
			size = compressor.compressedSize(new ByteArrayInputStream(tokens
					.getBytes()));
		} catch (IOException e) {
			log.warn("Exception during preprocess", e);
		}
		s.putData(sizeKey, Integer.valueOf(size));
	}

	/**
	 * @return a number between 0 (most similar) and 1 (least similar)
	 */
	public float similarity(Submission sa, Submission sb) {
		try {
			String tokens = getTokens(sa) + getTokens(sb);
			InputStream is = new ByteArrayInputStream(tokens.getBytes());
			int a = getCompSize(sa);
			int b = getCompSize(sb);
			int c = compressor.compressedSize(is);
			int m = Math.min(a, b);
			int M = a + b - m;
			return (float) (c - m) / (float) M;
		} catch (IOException e) {
			log.warn("Exception during similarity comparison", e);
		}
		return -1f;
	}
}
