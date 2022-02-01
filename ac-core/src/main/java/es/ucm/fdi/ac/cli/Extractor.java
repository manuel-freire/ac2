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
package es.ucm.fdi.ac.cli;

import es.ucm.fdi.ac.extract.*;
import es.ucm.fdi.ac.extract.CompositeFilter.Operator;
import es.ucm.fdi.util.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Stack;

/**
 * This class extracts assignment source-files from archives or folders
 * to the selected destination; wildcards can be provided to match against, 
 * and preview of what would be extracted is also available.
 * 
 * This is a utility class, and has no non-static methods to it.
 *
 * @author mfreire
 */
public class Extractor {

	/**
	 * @param zipDir directory where archive files can be found (searched recursively!)
	 * @param zipFilter filter to select appropiate archive files to extract
	 * @param destDir directory where the archives should be extracted
	 * @param destFilter filter to select appropiate files from archive
	 */
	public static void extract(File zipDir, FileTreeFilter zipFilter,
			File destDir, FileTreeFilter destFilter) throws IOException {

		if (!destDir.exists() && !destDir.mkdirs()) {
			System.err.println("Error: no se pudo crear el directorio destino");
			return;
		}

		File tmpDir = File.createTempFile("ac_extract", "tmp");
		tmpDir.delete();
		if (!tmpDir.mkdir()) {
			System.err.println("Error: imposible crear directorio temporal");
		}

		for (File zf : FileUtils.listFiles(zipDir)) {
			if (!zipFilter.accept(zf))
				continue;

			String name = zf.getName().toLowerCase();
			String id = name.replaceAll("\\..*", "");
			String type = name.substring(name.lastIndexOf('.') + 1);

			System.out.println("Procesando '" + zf + "' => " + id + " " + type);

			tmpDir.mkdir();

			try {
				FileUtils.getArchiverFor(name).expand(zf, tmpDir);
			} catch (NullPointerException npe) {
				System.err.println("Error: no se pudo extraer el archivo '"
						+ zf + "':\n\tTipo de archivo no reconocido "
						+ "(ni .zip, ni .tgz, ni .tar, ni .rar)");
			} catch (Exception e) {
				System.err.println("Error: no se pudo extraer el archivo '"
						+ zf + "'");
				e.printStackTrace();
				continue;
			}

			File dd = new File(destDir, id);
			dd.mkdir();

			for (File f : FileUtils.listFiles(tmpDir)) {

				if (!destFilter.accept(f))
					continue;

				System.out.println("\tAceptado: " + f.getName());
				File df = new File(dd, f.getName());

				if (df.exists()) {
					System.err.println("Aviso: archivo duplicado - '"
							+ f.getPath() + "'; se ignora la segunda version");
					continue;
				}
				FileUtils.copy(f, df);
			}

			FileUtils.delete(tmpDir);
		}

		if (tmpDir.exists()) {
			FileUtils.delete(tmpDir);
		}
	}

	public static void main(String[] args) {

		//        args = new String[] {"/home/mfreire/pract/poo_0506/z2", "/tmp/q", 
		//            "AND", ".*\\.java", "NOT", ".*Busca.*", "END", "c:.*void +main.*"};

		String name = Extractor.class.getCanonicalName();

		String help = " Sintaxis: "
				+ name
				+ " <dir_zips> <dir_dest> [-z <patron_origen>] [-f <patron_dest>]\n"
				+ "    <dir_zips>: directorio que contiene los zips de donde extraer ficheros\n"
				+ "    <dir_dest>: destino para lo extraido - que es prefijado con el nombre\n"
				+ "       de su archivo, SIN incluir la extension\n"
				+ "    <patron>:   un patron que determina ficheros los ficheros a extraer / seleccionar \n"
				+ "     - AND, OR, NOT o END, se entiende que es un operador booleano\n"
				+ "         (END se usa para terminar los operandos a AND y OR)\n"
				+ "     - c:<patron>', se considera un patron respecto al contenido del fichero\n"
				+ "         (no se debe usar sobre ficheros comprimidos)\n"
				+ "     - 'p:', se trata de un patron respecto a la ruta completa del fichero\n"
				+ "     - 'e:', se trata de un patron respecto a la extension del fichero\n"
				+ "     - si empieza por otra cosa, se refieren al nombre del fichero\n\n"
				+ " EJ: extraer todos los ficheros .java que no se llamen 'Ejemplo' y contengan un 'main':\n"
				+ "    extract /tmp/zips /tmp/salida -f AND '.*\\.java' NOT '.*Ejemplo.*' 'c:.*void main.*'";

		if (args.length < 2 || args[0].equals("-h") || args[0].equals("--help")) {
			System.err.println(help);
			return;
		}

		File zipDir = new File(args[0]);
		if (!zipDir.exists() || !zipDir.isDirectory()) {
			System.err.println("Error: No encuentro el directorio origen"
					+ " '" + zipDir + "'\n\n" + help);
			return;
		}

		File destDir = new File(args[1]);
		if (destDir.exists() && !destDir.isDirectory()) {
			System.err
					.println("Error: El 'directorio destino' no es un directorio "
							+ " '" + destDir + "'\n\n" + help);
			return;
		}

		CompositeFilter zipFilter = new CompositeFilter();
		CompositeFilter destFilter = new CompositeFilter();

		int zi = 0;
		int fi = 0;
		for (int i = 2; i < args.length; i++) {
			if (args[i].equals("-z"))
				zi = i;
			if (args[i].equals("-f"))
				fi = i;
		}

		if (zi > 0) {
			int end = (fi > zi) ? Math.min(args.length, fi) : args.length;
			zipFilter = parseCompositeFilter(Arrays.copyOfRange(args, zi + 1,
					end));
		}
		if (fi > 0) {
			int end = (zi > fi) ? Math.min(args.length, zi) : args.length;
			destFilter = parseCompositeFilter(Arrays.copyOfRange(args, fi + 1,
					end));
		}

		System.err.println("Extrayendo...");
		System.err.println("\tzipFilter  = " + zipFilter);
		System.err.println("\tdestFilter = " + destFilter);

		try {
			extract(zipDir, zipFilter, destDir, destFilter);
		} catch (IOException ex) {
			System.err.println("Error extrayendo: " + ex);
			ex.printStackTrace();
		}
	}

	/**
	 * Creates a filter from a series of strings, each of which can be
	 * - an operator ( AND, OR, NOT )
	 * - a terminator ( END, ends previous open operator)
	 * - another thing ( a filter )
	 */
	public static CompositeFilter parseCompositeFilter(String[] args) {
		Stack<CompositeFilter> stack = new Stack<CompositeFilter>();
		for (String s : args) {
			if (s.equals("NOT") || s.equals("AND") || s.equals("OR")) {
				CompositeFilter f = new CompositeFilter();
				switch (s.charAt(0)) {
				case 'N':
					f.setOp(CompositeFilter.Operator.Nor);
					break;
				case 'A':
					f.setOp(CompositeFilter.Operator.And);
					break;
				case 'O':
					f.setOp(CompositeFilter.Operator.Or);
					break;
				}
				// push new operator
				stack.push(f);
			} else if (s.equals("END")
					|| ((stack.peek().getOp() == Operator.Nor) && (stack.peek()
							.getFilters().size() == 1))) {
				// end operator, and adds as filter to new top                
				CompositeFilter f = stack.pop();
				stack.peek().addFilter(f);
			} else if (s.startsWith("c:")) {
				String ss = s.substring("c:".length());
				stack.peek().addFilter(new ContentPatternFilter(ss));
			} else if (s.startsWith("p:")) {
				String ss = s.substring("p:".length());
				stack.peek().addFilter(new PathFilter(ss));
			} else if (s.startsWith("e:")) {
				String ss = s.substring("e:".length());
				stack.peek().addFilter(new FileExtensionFilter(ss));
			} else {
				stack.peek().addFilter(new FileNameFilter(s));
			}
		}
		return stack.pop();
	}
}
