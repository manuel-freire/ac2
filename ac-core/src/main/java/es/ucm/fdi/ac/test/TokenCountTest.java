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
 * TokenCountTest.java 
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * Contributors: - 
 * Changelog: 
 *     20-Apr-2006: first version (mfreire)
 */

package es.ucm.fdi.ac.test;

import es.ucm.fdi.ac.Submission;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * This test counts the number of appearances of each token type. Distance
 * is "euclidean distance" when the counters are considered as N-dimensional
 * vectors.
 *
 * @author mfreire
 */
public class TokenCountTest extends TokenizingTest {

	static final public String SUBJECT_TOKVECTOR = "tokenvector";
	static final public String SUBJECT_TOKSIMILARITY = "tokensim";

	/** Creates a new instance of TokenCountTest */
	public TokenCountTest() {
		testKey = SUBJECT_TOKVECTOR;
	}

	/**
	 * All subjects will have been preprocessed before similarity is 
	 * checked.
	 */
	public void preprocess(Submission s) {
		super.preprocess(s);

		String tokens = getTokens(s);

		TreeMap<Integer, Integer> counter = new TreeMap<Integer, Integer>();
		StringTokenizer st = new StringTokenizer(tokens, "\n\r\t ");
		while (st.hasMoreTokens()) {
			int token = tokenizer.tokenId(st.nextToken());
			int count = counter.containsKey(token) ? counter.get(token) : 0;
			//            System.err.println(s.getId()+": incrementing "+token+" from "+count);
			count = count + 1;
			counter.put(token, count);
		}

		TreeMap<Integer, Double> normalizedCounter = new TreeMap<Integer, Double>();
		int total = 0;
		for (int k : counter.keySet()) {
			int c = counter.get(k);
			total += c * c;
		}
		double vectorLength = Math.sqrt(total);

		for (int k : counter.keySet()) {
			normalizedCounter.put(k, counter.get(k) / vectorLength);
			// System.err.println("prevalence of "+k+" = "+normalizedCounter.get(k));
		}

		s.putData(SUBJECT_TOKVECTOR, normalizedCounter);
	}

	/**
	 * @return a number between 0 (most similar) and 1 (least similar)
	 */
	public float similarity(Submission sa, Submission sb) {
		TreeMap<Integer, Double> ta, tb;
		ta = (TreeMap<Integer, Double>) sa.getData(SUBJECT_TOKVECTOR);
		tb = (TreeMap<Integer, Double>) sb.getData(SUBJECT_TOKVECTOR);
		ArrayList<Integer> keys = new ArrayList<Integer>(ta.keySet());
		keys.addAll(tb.keySet());
		Float distance = 0f;
		double total = 0;
		double ca, cb, dk, dm;
		double max = 0;
		for (int k : keys) {
			ca = ta.containsKey(k) ? ta.get(k) : 0;
			cb = tb.containsKey(k) ? tb.get(k) : 0;
			dk = ca - cb;
			// System.err.println("ca - cb = "+ca+" - "+cb+" = "+dk);
			total += dk * dk;
			max = Math.max(total, max);
		}

		//System.err.println(sa.getId() + "-"+sb.getId()+" total: "+total+" max: "+max);
		// maximum distance (2.0) is unnatainable; empirical overshooting
		return (float) Math.sqrt(total) * 3;
	}
}
