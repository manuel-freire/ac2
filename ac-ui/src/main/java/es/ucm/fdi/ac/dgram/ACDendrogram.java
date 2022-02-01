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
package es.ucm.fdi.ac.dgram;

import es.ucm.fdi.ac.Analysis;
import es.ucm.fdi.ac.Submission;
import es.ucm.fdi.ac.Tokenizer;
import es.ucm.fdi.ac.test.TokenizingTest;
import es.ucm.fdi.ac.dgram.DendrogramModel.DNode;
import java.util.ArrayList;
import java.util.Map;

/**
 * A helper class to generate dendrograms from AC subjects and a given key.
 * Since it is a factory class, it only contains static methods.
 *
 * @author mfreire
 */
public class ACDendrogram {

	public static DendrogramModel allSubmissionsModel(Analysis ac,
			String testKey, DendrogramModel.LinkageModel linkage) {
		DendrogramModel m = new DendrogramModel(ac.getSubmissions().length,
				linkage);
		for (Submission s : ac.getSubmissions()) {
			m.addLeaf(s, (float[]) s.getData(testKey));
		}
		return m;
	}

	public static Dendrogram createDendrogram(Analysis ac, String testKey) {
		return createDendrogram(ac, testKey,
				new DendrogramModel.AverageLinkage());
	}

	public static Dendrogram createDendrogram(Analysis ac, String testKey,
			DendrogramModel.LinkageModel linkage) {
		Dendrogram d = new Dendrogram(
				allSubmissionsModel(ac, testKey, linkage), SimpleRenderer.class);
		return d;
	}

	public static class TestLinkage implements DendrogramModel.LinkageModel {

		private int nextInternalId;
		private TokenizingTest t = null;
		private Submission[] subs;

		public TestLinkage(Submission[] subs, TokenizingTest t) {
			this.t = t;
			this.subs = subs;
			this.nextInternalId = subs.length;
		}

		private ArrayList<DNode> leavesInNode = new ArrayList(100);

		private Submission prepare(DNode n, String id) {
			Submission sub = null;
			if (!n.isLeaf()) {
				leavesInNode.clear();
				n.getLeaves(leavesInNode);
				sub = new Submission(id, "", nextInternalId++);
				StringBuilder sb = new StringBuilder();
				for (DNode nn : leavesInNode) {
					sb.append(t.getTokens((Submission) nn.getUserObject()));
				}
				sub.putData(Tokenizer.TOKEN_KEY, sb.toString());
				t.preprocess(sub);
			} else {
				sub = (Submission) n.getUserObject();
				if (sub.getData(t.getTestKey()) == null) {
					t.preprocess(sub);
				}
			}
			return sub;
		}

		public float distance(DNode a, DNode b, float[][] dt,
				Map<DNode, Integer> m) {
			Submission sa = prepare(a, "AA");
			Submission sb = prepare(b, "BB");
			if (a.isLeaf() && b.isLeaf()) {
				int ia = -1;
				for (int i = 0; i < subs.length; i++) {
					if (subs[i].getId().equals(sa.getId())) {
						ia = i;
						break;
					}
				}
				if (ia != -1 && sb.getData(t.getTestKey()) != null) {
					return ((float[]) sb.getData(t.getTestKey()))[ia];
				}
			}
			return t.similarity(sa, sb);
		}

		public String toString() {
			return "Test linkage [" + t.getTestKey() + "]";
		}
	}
}
