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

import java.io.*;
import java.lang.management.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;

/**
 * A simple prefix tree implementation.
 * Each node contains:</ul>
 * <li>a list of locations where it occurs</li>
 * <li>a list of children, indexed by first char</li>
 * </ul>
 * The main operation is adding new elements with
 * <code>add(String s, Locator l)</code>
 *
 * @author mfreire
 */
public class Main {

	/** Creates a new instance of Main */
	public Main() {
	}

	public static String readFileToString(File f) {
		try {
			char buffer[] = new char[(int) (f.length() * 1.5)];
			FileInputStream fis = new FileInputStream(f);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			br.read(buffer, 0, (int) f.length());
			br.close();
			return new String(buffer, 0, (int) f.length());
		} catch (IOException ioe) {
			System.err.println("Error reading " + f);
			ioe.printStackTrace();
			return null;
		}
	}

	private static MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();

	public static void showMem() {
		mbean.gc();
		long used = mbean.getHeapMemoryUsage().getUsed();
		long max = mbean.getHeapMemoryUsage().getMax();
		NumberFormat f = DecimalFormat.getInstance();
		f.setMaximumFractionDigits(2);
		f.setMinimumFractionDigits(2);
		System.err.println(f.format((double) used / max) + ":" + used);
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		PTrie p = new PTrie();

		if (false) {

			/*p.add("consigue un magnifico juego", "A");
			p.add("consigue un magnifico", "K");
			p.add("magnifico juego", "Z");
			p.add("aportacion o movilizacion desde otras entidades", "B");
			p.add("y si no lo consigues te devolvemos tu aportacion", "C");
			p.add("si logramos convencer a las otras entidades", "D");*/

			p.add("quien lo desencapotara aaaaaaaaaaaaaaaaaaa", "juanito");
			p.add("el cielo esta encapotado aaaaaaaa", "pepito");

			p.updateStats();
			p.dump();

		} else {

			System.err.println("reading in...");

			showMem();

			int i = 0;
			File d = new File("/home/mfreire/dev/ac/test2/poo_p1_0506");
			for (File dg : d.listFiles()) {
				if (!dg.isDirectory() || (++i > 20))
					continue;
				String name = dg.getName();
				for (File f : dg.listFiles()) {
					System.err.println(f);
					p.add(readFileToString(f).replaceAll("\\p{Space}+", " "),
							name, 20);
				}

				showMem();
				p.clearData();
				//showMem();

				//if (i++ == 2) break; //System.exit(-1);
			}

			System.err.println("updating...");

			p.updateStats();

			showMem();

			//p.dump();

			//        Node m = p.find("o");
			//        System.err.println("Found in "+m.getDistinctLocations().size()+" docs");
			//        for (Location l : m.getLocations()) {
			//            System.err.println("  "+l);
			//        }

		}

		System.err.println("searching for scarce...");
		for (Node n : p.findRare(2, 3)) {
			System.err.println(n.getString() + ": ");
			System.err.print("  ");
			HashMap<Object, Integer> counters = new HashMap<Object, Integer>();
			for (Location l : n.getLocations()) {
				Object o = l.getBase();
				int c = (counters.containsKey(o) ? counters.get(o) : 0);
				c++;
				counters.put(o, c);
			}
			for (Object o : counters.keySet()) {
				System.err.print(" " + o + "(" + counters.get(o) + ")");
			}
			System.err.println();
		}

	}
}
