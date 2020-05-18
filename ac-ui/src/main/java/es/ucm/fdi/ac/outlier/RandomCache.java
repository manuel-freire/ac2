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
 * RandomCache.java
 *
 * Created on June 16, 2007, 12:28 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package es.ucm.fdi.ac.outlier;

import java.util.Random;

/**
 * Producing vast numbers of pseudorandom numbers with a certain 
 * distribution can require a long time. This class allows them to be 
 * generated in advance, and provides a circular buffer to retrieve them.
 * Statistics suffers, but speed should be greatly increased.
 * 
 * @author mfreire
 */
public class RandomCache {

	/** Caches many different pseudo-random numbers; this allows different
	    functions to be estimated with the same set of numbers */
	static double[] randomCache;
	static int offset;
	static Random r = new Random();

	/**
	 * Creates a new instance of RandomCache
	 */
	public static void init(int nSamples) {
		/* Generate the matrix of sample gaussians */
		if (randomCache == null || randomCache.length < nSamples) {
			randomCache = new double[nSamples];
			for (int i = 0; i < nSamples; i++) {
				randomCache[i] = r.nextGaussian();
				//                    System.err.print(String.format("%3.3f ", randomCache[i]));                
			}
			//                System.err.println();           
		}
	}

	public static void fill(double[] dest, int start, int n) {
		if (randomCache == null) {
			throw new IllegalArgumentException("Init cache first!");
		}
		if (randomCache.length < 2 * n) {
			System.err.println("WARNING: "
					+ "the length of the random cache is " + randomCache.length
					+ ", which is too close to " + n);
		}

		offset += r.nextInt(randomCache.length);

		while (n > 0) {
			offset = offset % randomCache.length;
			int remaining = randomCache.length - offset;
			int toCopy = Math.min(n, remaining);
			System.arraycopy(randomCache, offset, dest, start, toCopy);
			n -= toCopy;
			offset += toCopy;
			start += toCopy;
		}
	}

	public static void main(String args[]) {
		RandomCache.init(6);
		double d[] = new double[3];
		for (int i = 0; i < 12; i++) {
			RandomCache.fill(d, 0, 3);
			double m = 0;
			for (int j = 0; j < d.length; j++) {
				System.err.print(String.format("%3.3f ", d[j]));
				m += d[j];
			}
			System.err.println();
			//System.err.println(String.format("%2d - %3.3f, %+6.6f", i, d[0], m));
		}
	}
}
