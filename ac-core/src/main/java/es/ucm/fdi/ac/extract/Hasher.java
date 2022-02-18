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
package es.ucm.fdi.ac.extract;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author mfreire
 */
public class Hasher {

	private TreeMap<byte[], ArrayList<FileTreeNode>> hashToFiles = new TreeMap<byte[], ArrayList<FileTreeNode>>(
			hashComparator);
	private static Comparator<byte[]> hashComparator = new Comparator<byte[]>() {

		@Override
		public int compare(byte[] a, byte[] b) {
			int c = 0;
			for (int i = 0; i < a.length; i++) {
				c = a[i] - b[i];
				if (c != 0) {
					break;
				}
			}
			return c;
		}
	};

	public static String showBytes(byte[] b) {
		StringBuilder sb = new StringBuilder("0x");
		for (int i = 0; i < b.length; i++) {
			sb.append(String.format("%x", b[i]));
		}
		return sb.toString();
	}

	public static String hash(String data) {
		return showBytes(hashBytes(data.getBytes(Charset.forName("UTF-8"))));
	}

	/**
	 * reads a string like 0xaabbccddeeff into a byte array like {0xaa,...,0xff}
	 * @param s
	 * @return
	 */
	public byte[] readBytes(String s) {
		byte[] b = new byte[s.length() / 2 - 1];
		for (int i = 2, j = 0; i < s.length(); i += 2, j++) {
			b[j] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
		}
		return b;
	}

	private final static String FN_BOUNDARY = " | ";

	public void save(PrintWriter w, FileTreeNode fn) {
		if (fn.getFile().isFile()) {
			w.write(fn.getFile().getAbsolutePath() + FN_BOUNDARY + " "
					+ fn.getSha1());
		} else if (fn.getFile().isDirectory()) {
			for (FileTreeNode child : fn.getChildren()) {
				save(w, child);
			}
		}
	}

	//        PrintWriter w = null;
	//        try {
	//            w = new PrintWriter(f);
	//            for ()
	//            return true;
	//        } catch (Exception e) {
	//            System.err.println(e);
	//            e.printStackTrace();
	//            return false;
	//        } finally {
	//            if (w != null) try { w.close(); } catch (Exception e2) {}
	//        }

	public void load(BufferedReader r, ArrayList<FileTreeNode> roots)
			throws IOException {
		HashMap<File, FileTreeNode> rootFiles = new HashMap<File, FileTreeNode>();
		for (FileTreeNode rn : roots) {
			rootFiles.put(rn.getFile(), rn);
		}

		String line = null;
		for (int l = 0; (line = r.readLine()) != null; l++) {
			int boundaryPos = line.indexOf(FN_BOUNDARY);
			String[] parts = line.split(" |( [|] )");
			if (parts.length != 3 || parts[0].length() != boundaryPos) {
				System.err.println("badly-formatted line " + l);
				continue;
			}
			/*
			File f = new File(parts[0]);
			long bytes = Long.parseLong(parts[1]);
			byte[] hash = readBytes(parts[2]);
			FileTreeNode root;
			File p = f;
			while ((p = p.getParentFile()) != null) {
				if ((root = rootFiles.get(p)) != null)
					break;
			}
			 */

			// FIXME: now, walk up the tree and overwrite the hash
		}
	}

	private FileTreeNode doHash(FileTreeNode fn) {

		if (fn.getFile().isFile()) {
			if (fn.getSha1() == null) {
				// may have been restored by a load, avoiding recomputation
				hashFile(fn);
			}
		} else {
			hashDir(fn);
		}

		ArrayList<FileTreeNode> friends = hashToFiles.get(fn.getSha1());
		if (friends == null) {
			friends = new ArrayList<FileTreeNode>();
			hashToFiles.put(fn.getSha1(), friends);
		}
		friends.add(fn);

		if (fn.getFile().isDirectory() && friends.size() > 1) {
			HashSet<FileTreeNode> toAvoid = new HashSet<FileTreeNode>();
			for (FileTreeNode friend : friends) {
				if (!friend.getFile().isDirectory()) {
					System.err.println("FOUND DIRECTORY SHA1 COLLISION!: "
							+ friend.getFile().getAbsolutePath() + " == "
							+ fn.getFile().getAbsolutePath());
				}
				if (friend.getChildren() == null)
					continue;
				toAvoid.addAll(friend.getChildren());
			}
			// toAvoid contains all children of same-hash nodes
			for (FileTreeNode friend : friends) {
				if (friend.getChildren() == null)
					continue;
				for (FileTreeNode child : friend.getChildren()) {
					ArrayList<FileTreeNode> childFriends = hashToFiles
							.get(child.getSha1());
					if (childFriends == null) {
						// if children are empty files, empty is to be expected
						continue;
					}
					if (!childFriends.isEmpty()) {
						// remove subsumed 'childFriends', since the parents are already friended
						//                        int before = childFriends.size();
						childFriends.removeAll(toAvoid);
						//                        System.err.println("removed " + (before - childFriends.size())
						//                                + " subsumed at " + fn.getFile().getAbsolutePath());
					}
				}
			}
		}

		return fn;
	}

	/**
	 * Removes the named node from the hash indices, so that it will no longer
	 * participate in any duplicate checks. This will not affect anything higher
	 * up in the hierarchy.
	 * @param fn
	 */
	public void unhash(FileTreeNode fn) {
		ArrayList<FileTreeNode> friends = hashToFiles.get(fn.getSha1());
		if (friends != null) {
			friends.remove(fn);
		}
		if (fn.getFile().isDirectory()) {
			for (FileTreeNode child : fn.getChildren()) {
				unhash(child);
			}
		}
	}

	public FileTreeNode hash(File f) {
		FileTreeNode root = new FileTreeNode(f, null);
		return doHash(root);
	}

	public TreeSet<byte[]> findMatchesIn(ArrayList<FileTreeNode> roots) {
		TreeSet<byte[]> results = new TreeSet<byte[]>(hashComparator);
		for (FileTreeNode fn : roots) {
			findMatchesIn(fn, results);
		}
		return results;
	}

	private void findMatchesIn(FileTreeNode fn, TreeSet<byte[]> results) {
		ArrayList<FileTreeNode> friends = hashToFiles.get(fn.getSha1());
		if (friends != null && friends.size() > 1) {
			results.add(fn.getSha1());
		}
		if (fn.getFile().isDirectory()) {
			for (FileTreeNode child : fn.getChildren()) {
				findMatchesIn(child, results);
			}
		}
	}

	public ArrayList<FileTreeNode> getDuplicates(byte[] hash) {
		return hashToFiles.get(hash);
	}

	private FileTreeNode hashDir(FileTreeNode fn) {
		if (fn == null) {
			throw new NullPointerException("empty fn");
		}

		byte[] directoryPrefix = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };

		// create a hash
		ArrayList<byte[]> all = new ArrayList<byte[]>();
		int totalBytes = directoryPrefix.length;
		all.add(directoryPrefix);
		for (FileTreeNode child : fn.getChildren()) {
			doHash(child);
			all.add(child.getSha1());
			totalBytes += child.getSha1().length;
		}
		Collections.sort(all, hashComparator);
		byte[] sorted = new byte[totalBytes + 5];
		int offset = 0;
		for (int i = 0; i < all.size(); i++) {
			System.arraycopy(all.get(i), 0, sorted, offset, all.get(i).length);
			offset += all.get(i).length;
		}

		fn.setSha1(hashBytes(sorted));
		return fn;
	}

	private FileTreeNode hashFile(FileTreeNode fn) {

		try (FileChannel fc=new RandomAccessFile(fn.getFile(), "r").getChannel()) {
			MessageDigest sha1Sun = MessageDigest.getInstance("SHA-1");
			ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
			while (fc.position() < fc.size()) {
				fc.read(buffer);
				buffer.flip();
				byte[] raw = new byte[buffer.limit()];
				System.arraycopy(buffer.array(), 0, raw, 0, raw.length);
				sha1Sun.update(raw);
				buffer.clear();
			}

			byte[] sun = sha1Sun.digest();
			sha1Sun.reset();
			buffer.clear();
			fn.setSha1(sun);
			return fn;
		} catch (Throwable e) {
			System.err.println(e);
			e.printStackTrace();
			return fn;
		} 
	}

	private static byte[] hashBytes(byte[] b) {
		try {
			MessageDigest sha1Sun = MessageDigest.getInstance("SHA-1");
			sha1Sun.update(b);
			byte[] digest = sha1Sun.digest();
			sha1Sun.reset();
			return digest;
		} catch (NoSuchAlgorithmException nsae) {
			System.err.println(nsae);
			return null;
		}
	}
}
