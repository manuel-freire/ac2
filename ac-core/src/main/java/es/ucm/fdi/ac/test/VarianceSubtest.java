/**
 * AC - A source-code copy detector
 *
 *     For more information please visit:  http://github.com/manuel-freire/ac
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

/*
 * VarianceSubtest.java 
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * Contributors: - 
 * Changelog: 
 *     18-Apr-2006: first version (mfreire)
 */

package es.ucm.fdi.ac.test;

import es.ucm.fdi.ac.Submission;

/**
 * This test builds on a previous test, and is interested not in
 * the actual distances, but the 'outlier' property of the distances.
 *
 * @author mfreire
 */
public class VarianceSubtest extends TokenizingTest {

	static final public int MAX_OUTLIERS = 3;

	static final public String SUBJECT_MEAN = "vs_mean";
	static final public String SUBJECT_SIDE_STD_DEV = "vs_ssd";

	// augmented by real test name when queried for
	static final public String SUBJECT_VARDIST_KEY = "variance_subtest_on_";

	static final private String SUBJECT_VD_POS = "vs_pos";

	// if you lower this to 0, you would get back the original test
	private double outlierImportance = 0.5;

	private final String baseTestKey;
	private int currentPos;

	/** Creates a new instance of ZipTest */
	public VarianceSubtest(String baseTestKey, double outlierImportance) {
		this.baseTestKey = baseTestKey;
		this.outlierImportance = outlierImportance;
		this.currentPos = 0;
		testKey = SUBJECT_VARDIST_KEY + baseTestKey + outlierImportance;
	}

	/**
	 * All subjects will have been preprocessed before similarity is 
	 * checked.
	 */
	public void preprocess(Submission s) {
		super.preprocess(s);

		if (s.getData(baseTestKey) == null) {
			javax.swing.JOptionPane
					.showMessageDialog(
							new javax.swing.JFrame("Message"),
							"Before running one of the Variance Subtests you must run the test it depends on ("
									+ baseTestKey + ")");
			throw new RuntimeException("Submission " + s
					+ " has no value for key " + baseTestKey + ";"
					+ "The test that produces '" + baseTestKey
					+ "' must be run prior to this one");
		}

		float[] f = (float[]) s.getData(baseTestKey);

		s.putData(SUBJECT_MEAN, mean(f));
		s.putData(SUBJECT_SIDE_STD_DEV, sideStdDev(f, mean(f)));

		s.putData(SUBJECT_VD_POS, currentPos);
		currentPos++;
	}

	/**
	 * mean
	 */
	private double mean(float[] dist) {
		int l = dist.length;

		double mean = 0;
		for (int i = 0; i < l; i++) {
			mean += dist[i];
		}
		mean /= dist.length;

		return mean;
	}

	/**
	 * Left-side standard deviation.
	 * Only considers values that are smaller than the mean of the distribution
	 * (the "left side" if plotted with values growing from left to right)
	 */
	private double sideStdDev(float[] dist, double mean) {

		int l = dist.length;
		double d = 0;
		int n = 0;
		for (int i = 0; i < l; i++) {
			if (dist[i] >= mean)
				continue;
			double di = dist[i] - mean;
			d += di * di;
			n++;
		}
		d /= n;

		return Math.sqrt(d);
	}

	/**
	 * Considers A to be more similar to B if A and B are already
	 * more similar to each other (as measured by variance) than to 
	 * other submissions.
	 *
	 * Current algorithm:<ul>
	 * <li> count left-standard deviations of sa in sb's distribution
	 * <li> count left-standard deviations of sb in sa's distribution
	 * <li> use that to get to a 'dispersion' metric, with 1 = lowest dispersion
	 *   (currently using an empirical 1/(2**(devsAinB*devsBinA)) formula)
	 * <li> lower the original score based on this dispersion, using a number
	 *   in the range [outlierImportance, 1] 
	 *   (maximal dispersion => outlierImportance gets used)
	 * <ul>
	 * @return a number between 0 (most similar) and 1 (least similar)
	 */
	public float similarity(Submission sa, Submission sb) {
		try {
			float[] A = (float[]) sa.getData(baseTestKey);
			float[] B = (float[]) sb.getData(baseTestKey);
			int posA = (Integer) sa.getData(SUBJECT_VD_POS);
			int posB = (Integer) sb.getData(SUBJECT_VD_POS);

			// Note that these are in [0, 1]
			double sideStdDevA = (Double) sa.getData(SUBJECT_SIDE_STD_DEV);
			double meanA = (Double) sa.getData(SUBJECT_MEAN);
			double sideStdDevB = (Double) sb.getData(SUBJECT_SIDE_STD_DEV);
			double meanB = (Double) sb.getData(SUBJECT_MEAN);

			// And these in [0, ...[
			double devAinB = Math.max(meanB - B[posA], 0) / sideStdDevB;
			double devBinA = Math.max(meanA - A[posB], 0) / sideStdDevA;

			// A number in the range [0, 1], with 1 'least interesting'
			double idispAB = 1 / Math.pow(2, devAinB * devBinA);

			//            System.err.println(sa.getId() + " " + sb.getId() 
			//                + " sideA " + sideStdDevA + " sideB " + sideStdDevB
			//                + " f(" + devAinB + ", " + devBinA + ") = " + idispAB);

			return (float) ((idispAB * outlierImportance + (1 - outlierImportance)) * A[posB]);
		} catch (Exception e) {
			System.err.println("Exception: " + e);
			e.printStackTrace();
		}
		return -1f;
	}
}
