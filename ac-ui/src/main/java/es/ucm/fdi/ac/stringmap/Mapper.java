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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.ucm.fdi.ac.stringmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Mapper can apply a regex substitution (typically removing 
 * 'redundant' stuff) to a string, while retaining the true offsets of things.
 * The data-structure is a simple array with the format
 * displacement n 
 * 
 * @author mfreire
 */
public class Mapper {
	ArrayList<Mapping> ms = new ArrayList<Mapping>();
	private String source;
	private String dest;

	public Mapper(String source, String replaceFrom, String replaceTo) {
		this.source = source;
		buildDisplacements(replaceFrom, replaceTo);
	}

	public Mapper(Mapper matcher, String replaceFrom, String replaceTo) {
		// match and output the body

	}

	private void buildDisplacements(String from, String to) {
		ms.clear();

		StringBuffer out = new StringBuffer(source.length());
		Pattern p = Pattern.compile(from);
		Matcher m = p.matcher(source);

		// add substituted patterns
		int toLen = to.length();
		int prev = 0;
		while (m.find(prev)) {
			out.append(source.substring(prev, m.start()));
			int matchLen = m.end() - m.start();
			if (matchLen != toLen) {
				ms.add(new Mapping(m.start(), m.end(), out.length(), out
						.length()
						+ toLen));
			}
			out.append(to);
			prev = m.end();
		}

		// add remnant
		out.append(source.substring(prev));

		dest = out.toString();
	}

	public ArrayList<Mapping> getMappings() {
		return ms;
	}

	public String getSource() {
		return source;
	}

	public String getDest() {
		return dest;
	}

	/**
	 * Maps an offset in the source document to an offset in the destination document.
	 * If the offset falls within a 'mapping', either the start ('biasForLow')
	 * or the end of the mapped mapping will be returned.
	 * @param offset
	 * @param biasForLow if true, and within a mapped segment, start of segment will be returned.
	 * @return
	 */
	public int map(int offset, boolean biasForLow) {

		if (offset < 0 || offset >= source.length()) {
			throw new IllegalArgumentException("out of bounds");
		}

		for (Mapping m : ms) {
			if (offset < m.a1) { // between prev and this
				int d = m.a1 - offset;
				return m.b1 - d;
			} else if (offset <= m.a2) { // inside
				return biasForLow ? m.b1 : m.b2;
			}
		}
		// not within any segment
		int d = source.length() - offset;
		return dest.length() - d;
	}

	/**
	 * Maps an offset in the destination document to an offset in the source document.
	 * If the offset falls within a reversed 'mapping', either the start ('biasForLow')
	 * or the end of the reversed mapped mapping will be returned.
	 * @param offset
	 * @param biasForLow if true, and within a mapped segment, start of segment will be returned.
	 * @return
	 */
	public int rmap(int offset, boolean biasForLow) {
		if (offset < 0 || offset > dest.length()) {
			throw new IllegalArgumentException("out of bounds: " + offset
					+ " not inside [ 0, " + dest.length() + "]");
		}

		for (Mapping m : ms) {
			if (offset < m.b1) { // between prev and this
				int d = m.b1 - offset;
				return m.a1 - d;
			} else if (offset <= m.b2) { // inside
				return biasForLow ? m.a1 : m.a2;
			}
		}
		// not within any segment
		int d = dest.length() - offset;
		return source.length() - d;
	}

	public static void printPositionInString(String s, int p) {
		System.err.println(s.substring(0, p) + "!" + s.substring(p));
		for (int i = 0; i < p; i++)
			System.err.print(" ");
		System.err.println("^___" + p);
	}

	public static void main(String args[]) {
		String s = "The explicit state of a matcher includes the start and end indices "
				+ "of the most recent successful match. It also includes the start "
				+ "and end indices of the input subsequence captured by each "
				+ "capturing group in the pattern as well as a total count of such "
				+ "subsequences. As a convenience, methods are also provided for "
				+ "returning these captured subsequences in string form. ";
		Mapper m = new Mapper(s, " of | it | as | the ", " ");
		//        for (Mapping mp : m.getMappings()) {
		//            System.err.println(mp + " '" + s.substring(mp.a1, mp.a2) + "' to '" + m.getDest().substring(mp.b1, mp.b2)+ "'");
		//        }
		//        System.err.println(s);
		//        System.err.println(m.getDest());
		for (int i = 0; i < 10; i++) {
			int r = (int) (Math.random() * m.getDest().length());
			printPositionInString(m.getDest(), r);
			printPositionInString(s, m.rmap(r, true));
			printPositionInString(s, m.rmap(r, false));
		}
	}

	/**
	 * describes, for two offsets in an origin space (a1, a2), the matching offsets in a target space (b1, b2)
	 */
	public static class Mapping {
		public int a1;
		public int a2;
		public int b1;
		public int b2;

		public Mapping(int a1, int a2, int b1, int b2) {
			this.a1 = a1;
			this.a2 = a2;
			this.b1 = b1;
			this.b2 = b2;
		}

		public String toString() {
			return a1 + ":" + a2 + " <-> " + b1 + ":" + b2;
		}

		private static AComp aInstance = new AComp();
		private static BComp bInstance = new BComp();

		public static Comparator<Mapping> getComp(boolean reverse) {
			return reverse ? bInstance : aInstance;
		}

		private static class AComp implements Comparator<Mapping> {
			public int compare(Mapping o1, Mapping o2) {
				return o2.a1 - o1.a1;
			}
		}

		private static class BComp implements Comparator<Mapping> {
			public int compare(Mapping o1, Mapping o2) {
				return o2.b1 - o1.b1;
			}
		}
	}
}
