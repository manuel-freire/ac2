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
package es.ucm.fdi.ac.ptrie;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A PATRICIA trie implementation
 *
 * @author mfreire
 */
public class PTrie {

	// locations of substrings of less than X's characters are ignored
	private int minSubstringSize = 5;

	private Node root;
	private int size;

	// optimization; allows storing start-end indexes ('substrings') in Nodes
	private HashMap<Object, String> documents;

	boolean isUpToDate;

	/**
	 * Creates a new PTrie
	 */
	public PTrie() {
		root = new Node("", 0, 0);
		documents = new HashMap<Object, String>();
		isUpToDate = false;
	}

	public void updateStats() {
		if (!isUpToDate) {
			root.updateStats();
			isUpToDate = true;
		}
	}

	/**
	 * Retrieves all strings with frequencies between minFreq and maxFreq
	 */
	public ArrayList<Node> findRare(int minFreq, int maxFreq) {
		updateStats();

		ArrayList<Node> found = new ArrayList<Node>();
		root.findRare(found, minFreq, maxFreq);
		return found;
	}

	/**
	 * Find a string in the trie; returns the relevant node.
	 * If it runs out of node before it runs out of string, return 'null'
	 */
	public Node find(String s) {

		Node start = root;
		boolean found = false;

		// the inner loop iterates the tree
		int offset = 0;
		int len = s.length();

		while (true) {

			char nextChar = s.charAt(offset);
			Node next = start.getChild(nextChar);

			if (next == null) {
				// not found
				return null;
			} else {
				// found next node that starts that way

				String nextData = next.getData();
				int nextLen = nextData.length();

				for (int p = 0; p < next.getData().length(); p++) {
					char a = s.charAt(offset + p);
					char b = next.getData().charAt(p);

					if (a != b) {
						return null;
					} else {
						if (offset + p + 1 == len) {
							// exact match
							return next;
						}
					}
				}

				// ran out of 'data'; iterate to next node
				offset += next.getData().length();
				start = next;
			}
		}
	}

	/**
	 * Add a single string (ignoring its substrings, since that's
	 * a job for an outer loop).
	 */
	private void add(String s, int offset, Object base) {

		Node start = root;
		int depth = 0;
		boolean found = false;

		// the inner loop iterates the tree
		while (!found) {

			//          System.err.println("adding '"+s+"'+"+offset+"='"+s.substring(offset)+"'("+depth+") to \n"
			//                    +start.print(new StringBuffer(), 0));

			int len = s.length() - offset;

			char nextChar = s.charAt(offset);
			Node next = start.getChild(nextChar);

			if (next == null) {
				// add a new node
				//System.err.println("Adding node for "+nextChar);
				next = new Node(documents.get(base), offset, offset + len);
				if (depth + len >= minSubstringSize) {
					next.addLocation(base, offset - depth);
				}
				start.putChild(nextChar, next);
				found = true;
			} else {
				// found previous node that starts that way
				String nextData = next.getData();
				int nextLen = nextData.length();

				if (nextLen == 0) {
					System.err.println("Error: 'next' has zero length");
					System.err.println("adding '" + s + "'+" + offset + "='"
							+ s.substring(offset) + "'(" + depth + ") to \n"
							+ start.print(new StringBuffer(), 0));
					throw new IllegalArgumentException("Erro");
				}

				for (int p = 0; /**/; p++) {
					char a = s.charAt(offset + p);
					char b = nextData.charAt(p);
					if (a != b) {
						// split & merge: new middle variation
						//System.err.println("not the same: full-split");

						// 'n' keeps the old prefix
						Node n = new Node(next.getSource(), next.getStart(),
								next.getStart() + p);
						start.putChild(nextData.charAt(0), n);

						// 'm' gets the new suffix
						Node m = new Node(s, offset + p, offset + len);
						if (depth + len >= minSubstringSize) {
							m.addLocation(base, offset - depth);
						}
						n.putChild(a, m);

						// the old 'next' keeps the old suffix
						next.setData(nextData.substring(p));
						n.putChild(b, next);

						found = true;
						break;
					} else if (p + 1 == len || p + 1 == nextLen) {
						if (p + 1 < nextLen) {
							// ran out of 's+offset'; split & merge
							//System.err.println("half-split");

							// 'n' keeps the old prefix
							Node n = new Node(next.getSource(),
									next.getStart(), next.getStart() + p + 1);
							if (depth + len >= minSubstringSize) {
								n.addLocation(base, offset - depth);
							}
							start.putChild(nextData.charAt(0), n);

							// the old 'next' keeps the old suffix
							next.setData(nextData.substring(p + 1));
							n.putChild(nextData.charAt(p + 1), next);
							found = true;
						} else if (p + 1 < len) {
							// ran out of 'data'; jump to next node
							//System.err.println("iterating...");
							offset += p + 1;
							depth += p + 1;
							start = next;
							found = false;
						} else {
							// perfect match; store here
							//System.err.println("perfect match");
							if (depth + len >= minSubstringSize) {
								next.addLocation(base, offset - depth);
							}
							found = true;
						}
						break;
					}
				}
			}
		}
	}

	/**
	 * Adds all substrings of 's'
	 */
	public void add(String s, Object base) {

		if (!documents.containsKey(base)) {
			documents.put(base, s);
		}

		int len = s.length();
		for (int i = 0; i < len; i++) {
			add(s, i, base);
		}
	}

	/**
	 * Adds all substrings of 's' of length < maxLen
	 */
	public void add(String s, Object base, int maxLen) {

		if (!documents.containsKey(base)) {
			documents.put(base, s);
		}

		int len = s.length();
		for (int i = 0; i < len; i++) {
			if (len - i > maxLen) {
				add(s.substring(0, i + maxLen), i, base);
			} else {
				add(s, i, base);
			}
		}
	}

	public void clearData() {
		System.err.println("Cleared " + clearData(root) + " nodes");
	}

	private int clearData(Node n) {
		int count = 1;
		//n.setData("nada");
		n.killLocalLocations();
		for (char c : n.getChildrenChars()) {
			count += clearData(n.getChild(c));
		}
		return count;
	}

	/**
	 * Dump a representation of the trie
	 */
	public void dump() {
		System.err.println("---\n"
				+ root.print(new StringBuffer(), 0).toString());
	}
}
