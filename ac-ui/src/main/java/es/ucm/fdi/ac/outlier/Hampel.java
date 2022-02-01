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
package es.ucm.fdi.ac.outlier;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.math.stat.StatUtils;

/**
 * @author Manuel Cebrian (manuel.cebrian-at-uam.es)
 * FUNCTION THR = HAMPEL(DATA)
 * 
 * Description: this function calculates a threshold for detecting outliers
 * by means of the Hampel indentifier [1].
 * 
 * This statistic is calculated as |X-M|/(MAD/.6745), and compared to a value K,
 * i.e. if |X-M|/(MAD/.6745) > K then X is identified as an outlier. As in
 * plagiarism outliers are low distances, we have modified the Hampel
 * identified to take into account only the lowest distances. Thus, the
 * statistic is now (M-X)/(MAD/.6745), which gives high values to outliying
 * low distances, and low values (even negatives) to high distances.
 * 
 * Several values for K can be chosen 3.5 [1], 2.24 [2] or more sophisticaed
 * ones which are a function of the sample size [3].
 * In.. this implementation we have chosen the following standarization [3, p.783]
 * P(no outliers in the sample) = 1 - alpha. To do this, we mark as outliers
 * all X such that |X-M|/(MAD/.6745) > g(N,alpha). We are limited to use
 * alpha = 0.01 and alpha = 0.05, the reasons for this limitation are in
 * [3, p. 791].
 * 
 * Input: an array of distances between assignments.
 * 
 * Output: a threshold so that data &lt; thr are identified as outliers.
 * 
 * Bibliography:
 * 
 * [1] Frank R. Hampel, The Breakdown Points of the Mean Combined With Some
 * Rejection Rules. Technometrics, Vol. 25, 1985.
 * 
 * [2] Rand R. Wilcox, Appliying Contemporary Statistical Techniques,
 * Elsevier Science, 2003.
 * 
 * [3] Laurie Davies and Ursula Gather, The Identification of Multiple
 * Outliers, Journal of the American Statistical Association, 1993.
 */

public class Hampel {

	/** 
	 * median/stdDistDevEstimator is a  robust estimator for the deviation of 
	 * the standard distribution (see [2])
	 */
	private static final double stdDistDevEstimator = 0.6745;

	/** Default alpha values */
	private static final double[] defaultAlphaValues = new double[] { 0.01,
			0.05 };

	/** Default montecarlo size (number of gaussian samples) */
	private static final int nMontecarlos = 1000;

	/** A very small number; results will are guaranteed to fall between EPSILON and 1-EPSILON */
	private static final double EPSILON = 0.000001;

	/**
	 * Calculate the threshold identifier for this distribution, using 
	 * using default 'alpha' values
	 */
	public static List<Double> hampel(double[] array) {
		ArrayList<Double> al = new ArrayList<Double>();
		for (double alpha : defaultAlphaValues) {
			al.add(hampel(analyticK(array.length, alpha), array));
		}
		return al;
	}

	/**
	 * calculate the threshold identifier for a distribution, given a 
	 * 'k' value. Does not return negative values, even when Hampel generally
	 * could.
	 */
	public static double hampel(double k, double[] array) {

		double m = median(array);
		double s = madn(array);
		double v = Math.max(EPSILON, m - k * s);

		return Math.min(1 - EPSILON, v);
	}

	/**
	 * median absolute deviation from the median 
	 */
	public static double madn(double[] array) {
		double aux[] = new double[array.length];
		double madn = 0;
		double m = median(array);

		for (int i = 0; i < array.length; i++)
			aux[i] = Math.abs(array[i] - m);

		madn = median(aux) / stdDistDevEstimator;

		return madn;
	}

	/**
	 * median of an array
	 */
	public static double median(double[] array) {
		Arrays.sort(array); // sort the elements
		boolean even = (array.length % 2) == 0;
		double median = 0;

		if (even) {
			int rightNumber = array.length / 2;
			int leftNumber = rightNumber - 1;
			// return average of two center-elements
			median = (array[rightNumber] + array[leftNumber]) / 2;
		} else {
			// choose the center one            
			median = array[array.length / 2];
		}

		return median;
	}

	/**
	 * Ugly lookup of 'k' given alpha. Alpha must be either 0.01 or 0.05
	 * for this to work as expected.
	 */
	private static double analyticK(int n, double alpha) {
		double k;
		double logN = Math.log(n);
		double sqrtTwoLogN = Math.sqrt(2 * logN);
		double z_n_alpha = sqrtTwoLogN - Math.log(alpha / 2) / sqrtTwoLogN
				- (Math.log(logN) + Math.log(4 * Math.PI)) / (2 * sqrtTwoLogN);

		if (alpha == 0.05) {
			if ((n % 2) == 0) {//N even
				k = 1.483 * z_n_alpha + 21.61 * Math.pow(n + 1, -0.8655);
			} else {
				k = 1.483 * z_n_alpha + 14.43 * Math.pow(n - 3, -0.7939);
			}

			return k;

		} else if (alpha == 0.01) {
			if ((n % 2) == 0) {//N even
				k = 1.483 * z_n_alpha + 41.39 * Math.pow(n, -0.9143);
			} else {
				k = 1.483 * z_n_alpha + 24.48 * Math.pow(n - 5, -0.8236);
			}

			return k;
		} else {
			return Double.NaN;
			//            throw new IllegalArgumentException(
			//                    "Alpha IS RESTRICTED to be 0.01 or 0.05 ");
		}
	}

	/**
	 * Montecarlo estimation of 'k' given alpha
	 * @param n number of entries
	 * @param alpha value of free parameter for the Hampel identifier
	 * @return k
	 */
	public static double montecarloK(int n, double alpha) {

		double statistic[] = new double[nMontecarlos];
		double ra[] = new double[n]; // a random array

		System.err.println("initializing cache...");
		RandomCache.init(32 * 1024 * 1024);
		System.err.println("Starting calculations...");
		for (int i = 0; i < nMontecarlos; i++) {
			RandomCache.fill(ra, 0, n);

			double med = median(ra);
			double madN = madn(ra);

			// Absolute value of sample i
			for (int k = 0; k < n; k++) {
				ra[k] = Math.abs(ra[k]);
			}

			// Hampel statistic computation
			statistic[i] = (StatUtils.max(ra) - med) / madN;
		}

		// calculate result
		double result = StatUtils.percentile(statistic, 100 * (1 - alpha));

		return result;
	}

	/**
	 * To regenerate cache from command-line use
	 * java -cp classes:lib/commons-math-1.1.jar eps.outlier.Hampel
	 */
	public static void main(String args[]) throws Exception {

		System.err.println("init...");
		Hampel o = new Hampel();
		System.err.println("init OK");

		double[] enes = { 1, 10, 20, 40, 80, 160, 320, 640, 1280, 5120, 10240 }; // 11 rows
		double[] alfas = { 0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.2, 0.25,
				0.5, 0.75 }; // 10 cols
		double[] values = new double[enes.length * alfas.length];

		long startTime = System.currentTimeMillis();
		for (int i = 0, k = 0; i < enes.length; i++) {
			for (int j = 0; j < alfas.length; j++) {
				long partialStart = System.currentTimeMillis();
				values[k++] = o.montecarloK((int) enes[i], alfas[j]);
				long partialTime = System.currentTimeMillis() - partialStart;
				System.err.println("" + k + " down (" + (partialTime / 1000.0f)
						+ " s), " + (values.length - k) + " to go...");
			}
		}
		long endTime = System.currentTimeMillis();
		float seconds = (endTime - startTime) / 1000.0f;

		ArrayList<double[]> al = new ArrayList<double[]>();
		al.add(enes);
		al.add(alfas);
		Interpolator ip = new Interpolator(2, al, values);
		ip.saveGrid(new FileWriter("/tmp/saved_grid.txt"));

		System.err.println("Finished after " + seconds + " s");
	}
}
