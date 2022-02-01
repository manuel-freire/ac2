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
import es.ucm.fdi.util.archive.ZipFormat;
import java.io.ByteArrayInputStream;

/**
 * This test measures how much distance there is from one subject to another,
 * based on the different sizes achieved after compression.
 *
 * R. Cilibrasi, P.M.B. Vitanyi, Clustering by compression, IEEE Trans.
 *    Information Theory, 51:4(2005), 1523- 1545. Also:
 *    http://xxx.lanl.gov/abs/cs.CV/031204 (2003).
 *
 * @author mfreire
 */
public class CommentZipTest extends CommentAnalysisTest {

	static final public String SUBJECT_COM_ZIPSIZE_KEY = "comzipsize";
	static final public String SUBJECT_COM_ZIPSIMILARITY = "comzipsim";

	static public ArchiveFormat compressor = new ZipFormat();

	/** Creates a new instance of ZipTest */
	public CommentZipTest() {
		testKey = SUBJECT_COM_ZIPSIMILARITY;
	}

	public int getZipSize(Submission s) {
		return ((Integer) s.getData(SUBJECT_COM_ZIPSIZE_KEY)).intValue();
	}

	/**
	 * All subjects will have been preprocessed before similarity is 
	 * checked.
	 */
	public void preprocess(Submission s) {
		super.preprocess(s);

		String comments = getComments(s);

		int size = -1;
		try {
			size = compressor.compressedSize(new ByteArrayInputStream(comments
					.getBytes()));
		} catch (Exception e) {
			System.err.println("Exception: " + e);
			e.printStackTrace();
		}
		s.putData(SUBJECT_COM_ZIPSIZE_KEY, new Integer(size));
	}

	/**
	 * @return a number between 0 (most similar) and 1 (least similar)
	 */
	public float similarity(Submission sa, Submission sb) {
		try {
			String tokens = getTokens(sa) + getTokens(sb);
			int a = getZipSize(sa);
			int b = getZipSize(sb);
			int c = compressor.compressedSize(new ByteArrayInputStream(tokens
					.getBytes()));
			int m = Math.min(a, b);
			int M = a + b - m;
			return (float) (c - m) / (float) M;
		} catch (Exception e) {
			System.err.println("Exception: " + e);
			e.printStackTrace();
		}
		return -1f;
	}
}
