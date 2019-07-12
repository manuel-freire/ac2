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
 * Interpolator.java
 *
 * Created on June 16, 2007, 12:33 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package es.ucm.fdi.ac.outlier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Instances of this class can interpolate an n-dimensional function, given an
 * n-dimensional grid of samples. Saving and loading these grids from and to a
 * a file is also supported.
 *
 * @author mfreire
 */
public class Interpolator {

	int dims;
	ArrayList<double[]> points;
	int nSamples[];
	double[] values;

	/** 
	 * Creates a new interpolator from a grid; values should contain values in
	 * a grid arrangement, where the first dimension varies fastest. For a function
	 * with two dimensions, dims=2, points contains 2 sorted lists 
	 * with the row and column coordinates, and values contains a list 
	 * of values in column-varies-fastest order. Eg.:
	 * 2, a b c / d e f, ad ae af bd be bf cd ce cf
	 */
	public Interpolator(int dims, ArrayList<double[]> points, double[] values) {
		this.dims = dims;
		this.points = points;
		this.values = values;
		if (dims != points.size()) {
			throw new IllegalArgumentException(
					"Dimensions of 'points' should match 'dims'");
		}
		int nValues = 1;
		nSamples = new int[dims];
		for (int i = 0; i < dims; i++) {
			nSamples[i] = points.get(i).length;
			nValues *= nSamples[i];
		}
		if (values.length != nValues) {
			throw new IllegalArgumentException("Interpolator expected "
					+ nValues + " grid points, received " + values.length
					+ " instead.");
		}
	}

	/**
	 * Load an interpolation grid from a stream; fail if read fails
	 *
	 * Format is
	 * nDims
	 * nDims-lines, each of them with many increasing values (say, d0 ... dn-1 values each)
	 * d0x...xdn-1 datapoints, corresponding to the values of the function at each spot.
	 *
	 */
	public Interpolator(InputStream is) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(is));

		// init dims
		String s = r.readLine();
		dims = Integer.parseInt(s);
		nSamples = new int[dims];

		// init grid points and sample counts in each axis
		int nValues = 1;
		points = new ArrayList<>();
		for (int i = 0; i < dims; i++) {
			s = r.readLine();
			StringTokenizer t = new StringTokenizer(s, " \n\r");
			double[] d = new double[t.countTokens()];
			for (int j = 0; j < d.length; j++) {
				d[j] = Double.parseDouble(t.nextToken());
			}
			points.add(d);
			nSamples[i] = d.length;
			nValues *= d.length;
		}

		// read values
		values = new double[nValues];
		int i = 0;
		while (r.ready() && (s = r.readLine()) != null) {
			StringTokenizer t = new StringTokenizer(s, " \n\r");
			while (t.hasMoreTokens()) {
				values[i++] = Double.parseDouble(t.nextToken());
				nValues--;
			}
		}
		if (nValues != 0) {
			throw new IOException("File finished too early - still missing "
					+ nValues + " values");
		}
	}

	/**
	 * Save an interpolation grid to a file. Format is
	 * dims | samples-first-dim | ... | samples-last-dim | values, where
	 * '|' represents a new line
	 */
	public void saveGrid(Writer w) throws IOException {
		w.write(dims + "\n");
		for (double[] d : points) {
			for (int i = 0; i < d.length; i++) {
				w.write(d[i] + " ");
			}
			w.write("\n");
		}
		for (int i = 0; i < values.length; i++) {
			w.write(values[i] + " ");
			if (((i + 1) % nSamples[dims - 1]) == 0) {
				w.write("\n");
			} else if (i == values.length - 1) {
				w.write("\n");
			}
		}
		w.close();
	}

	/**
	 * Offset in a serialized 'n-dimensional' array; last coordinate
	 * varies fastest
	 */
	public int getOffset(int[] pos) {
		int offset = pos[pos.length - 1];
		for (int i = pos.length - 2; i >= 0; i--) {
			offset += nSamples[i + 1] * pos[i];
		}
		//        print(pos);
		//        System.err.println(" --> " + offset);
		return offset;
	}

	/**
	 * converts a 2D matrix into serialized form; basically just appends
	 * all rows one after another...
	 */
	public static double[] serializeMatrix(double[][] matrix) {
		double[] out = new double[matrix.length * matrix[0].length];
		for (int i = 0, k = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				out[k++] = matrix[i][j];
			}
		}
		return out;
	}

	/**
	 * Simple 1-d linear interpolation
	 */
	public double linearInterpolate(double pos) {
		return linearInterpolate(new double[] { pos });
	}

	/**
	 * Simple 2-d linear interpolation; avoids creating an array
	 */
	public double linearInterpolate(double a, double b) {
		return linearInterpolate(new double[] { a, b });
	}

	/**
	 * Interpolates a value from a set of coordinates. Guaranteed to return a 
	 * grid value if it is requested, will perform a simple interpolation otherwise.
	 * Idea is to find 2^dim nearest grid values, and perform linear interpolation
	 * 'dim' times until only 1 interpolated value is left.
	 *
	 * Throws an illegalArgumentException if the requested coordinates fall 
	 * outside bounds.
	 */
	public double linearInterpolate(double[] coordinates) {
		int[] neighbor = new int[dims];

		// find neighbor N such that C falls between N and N+1
		for (int i = 0; i < dims; i++) {
			double d[] = points.get(i);
			if (coordinates[i] < d[0] || coordinates[i] >= d[nSamples[i] - 1]) {
				throw new IllegalArgumentException(
						"Coordinate value for dimension " + (i + 1) + " ("
								+ coordinates[i] + ") is off-grid!");
			}
			for (int j = 0; coordinates[i] > d[j]; j++) {
				neighbor[i] = j;
			}
		}
		//        print(neighbor);

		// find the values of closest grid points
		int twodims = 1 << dims;
		double[] rValues = new double[twodims];
		int r[] = new int[dims];
		for (int i = 0; i < twodims; i++) {
			double dist = 0, aux;
			for (int j = 0; j < dims; j++) {
				r[dims - j - 1] = neighbor[dims - j - 1]
						+ ((i & (1 << j)) != 0 ? 1 : 0);
			}
			rValues[i] = values[getOffset(r)];
		}
		//        print(rValues);

		// reduce dimensions (starting with first dimension) until only 1 left
		for (int i = 0; i < dims; i++) {
			double[] nextValues = new double[1 << (dims - i - 1)];
			double lo = points.get(i)[neighbor[i]];
			double hi = points.get(i)[neighbor[i] + 1];
			// w = 0 => was 'low'; w = 1 => was 'high''
			double whi = (coordinates[i] - lo) / (hi - lo);
			double wlo = 1 - whi;
			for (int j = 0; j < nextValues.length; j++) {
				nextValues[j] = wlo * rValues[j] + whi
						* rValues[j + nextValues.length];
			}
			rValues = nextValues;
			//            print(rValues);
		}

		return rValues[0];
	}

	private static void print(double[] o) {
		for (double i : o)
			System.err.print(i + " ");
		System.err.println();
	}

	private static void print(int[] o) {
		for (int i : o)
			System.err.print(i + " ");
		System.err.println();
	}

	public static void main(String args[]) throws Exception {
		ArrayList<double[]> al = new ArrayList<double[]>();

		/*
		 * a simple test with 2D, variable grid, and z = y*y + x
		 */
		al.add(new double[] { 1, 1.2, 1.5, 2, 3, 4 });
		al.add(new double[] { 1, 1.1, 2, 2.2, 2.5, 3, 4 });

		for (int i = 0; i < al.size(); i++) {
			double d[] = al.get(i);
		}

		// this part only good for 2d...
		double[] rows = al.get(0);
		double[] cols = al.get(1);
		double[][] values = new double[rows.length][cols.length];
		for (int i = 0; i < rows.length; i++) {
			for (int j = 0; j < cols.length; j++) {
				values[i][j] = rows[i] * rows[i] + cols[j];
			}
		}

		Interpolator ip = new Interpolator(2, al, serializeMatrix(values));
		StringWriter sw = new StringWriter();
		ip.saveGrid(sw);
		System.err.println(sw.toString());

		InputStream is = new ByteArrayInputStream(sw.toString().getBytes(
				StandardCharsets.UTF_8));
		ip = new Interpolator(is);

		/**
		 * interpolate and print deviations from exact result
		 */
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				double y = 1 + 0.2 * i;
				double x = 1 + 0.2 * j;

				double v = ip.linearInterpolate(y, x);
				double ev = y * y + x;
				double d = ev - v;
				System.err
						.println(String
								.format(
										"%3.3f %3.3f -> %3.3f [%3.3f]: err = %+3.3f (%+2.2f%%)",
										y, x, v, ev, d, 100.0 * d / v));
			}
		}
	}
}
