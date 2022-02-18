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
package es.ucm.fdi.clover;

import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * Miscelaneous utilities that are used throughout the project.
 * All utilities here should be in the form of 'public static' methods.
 *
 * @author mfreire
 */
public class Util {

	public static String DEFAULT_FILE_ENCODING = "UTF-8";

	/**
	 * Copies a file from source to destination. Uses then 'nio' package
	 * for great justice (well, actually only compactness and speed).
	 */
	public static void copy(File source, File destination) throws IOException {
		FileInputStream in = new FileInputStream(source);
		FileOutputStream out = new FileOutputStream(destination);
		byte[] buffer = new byte[1024 * 16];
		int len;
		try {
			while ((len = in.read(buffer)) != -1)
				out.write(buffer, 0, len);
		} finally {
			in.close();
			out.close();
		}
		//        Fails on my SDK, unknown reason...        
		//        java.nio.channels.FileChannel in = null, out = null;
		//        try {          
		//            in = new FileInputStream(source).getChannel();
		//            log.debug("Source size is "+in.size());
		//            out = new FileOutputStream(destination).getChannel();
		//            log.debug("Dest size is "+out.size());
		//            in.transferTo(0, in.size(), out);
		//        } finally {
		//            if (in != null) in.close();
		//            if (out != null) out.close();
		//        }
	}

	/**
	 * Deletes a file or directory recursively. Returns 'true' if ok, or
	 * 'false' on error.
	 */
	public static boolean delete(File file) {
		// Recursive call
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (!delete(files[i]))
					return false;
			}
		}
		// File deletion
		if (!file.delete()) {
			System.err.println("Imposible to delete file "
					+ file.getAbsolutePath());
			return false;
		}
		return true;
	}

	/**
	 * Clears a directory of all its files. Will fail miserably
	 * if any of them are actually directories
	 */
	public static void clearFiles(File dir) throws IOException {
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++)
			files[i].delete();
	}

	/**
	 * Deletes a file or directory recursively. Returns 'true' if ok, or
	 * 'false' on error.
	 */
	public static int getDiskUsage(File file) {
		long usage = 0;

		// Recursive call
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				usage += getDiskUsage(files[i]);
			}
		}
		// Local size
		usage += file.length();

		return (int) usage;
	}

	/**
	 * Simulates creation of a zip file, but returns only the size of the zip
	 * that results from the given input stream
	 */
	public static int zipSize(InputStream is) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ZipOutputStream zipoutputstream = new ZipOutputStream(bos);
		zipoutputstream.setMethod(ZipOutputStream.DEFLATED);

		ZipEntry entry = new ZipEntry("z");
		zipoutputstream.putNextEntry(entry);

		// dump file
		int n;
		byte[] rgb = new byte[1024];
		while ((n = is.read(rgb)) > -1)
			zipoutputstream.write(rgb, 0, n);
		is.close();

		zipoutputstream.closeEntry();
		zipoutputstream.close();
		return bos.size();
	}

	/**
	 * Creates a zip file with the given name & contents.
	 * Ripped off a web example.
	 * Does not create any directories included in 'base'
	 */
	@SuppressWarnings("rawtypes")
	public static void createZip(File ficheroZip, ArrayList archivos,
			File baseDir) throws Exception {
		FileOutputStream fos = new FileOutputStream(ficheroZip);
		ZipOutputStream zipoutputstream = new ZipOutputStream(fos);
		zipoutputstream.setMethod(ZipOutputStream.DEFLATED);
		byte[] rgb = new byte[1024];
		FileInputStream fis;
		CRC32 crc32 = null;

		//log.debug("Creating zip file: "+ficheroZip.getName());
		for (int i = 0; i < archivos.size(); i++) {
			File file = (File) archivos.get(i);
			int n;

			// zip standard uses fw slashes instead of backslashes, always
			String baseName = baseDir.getAbsolutePath() + '/';
			String fileName = file.getAbsolutePath().substring(
					baseName.length());
			if (file.isDirectory())
				fileName += '/';
			ZipEntry entry = new ZipEntry(fileName);
			entry.setSize(file.length());
			entry.setTime(file.lastModified());

			// skip directories - after assuring that their children *will* be included.
			if (file.isDirectory()) {
				//log.debug("\tAdding dir "+fileName);
				File[] children = file.listFiles();
				for (int j = 0; j < children.length; j++)
					archivos.add(children[j]);
				zipoutputstream.putNextEntry(entry);
				continue;
			}

			//log.debug("\tAdding file "+fileName);

			// Calculate the CRC-32 value.
			crc32 = new CRC32();
			fis = new FileInputStream(file);
			while ((n = fis.read(rgb)) > -1)
				crc32.update(rgb, 0, n);
			fis.close();
			entry.setCrc(crc32.getValue());

			// Add the zip entry and associated data.
			zipoutputstream.putNextEntry(entry);
			fis = new FileInputStream(file);
			while ((n = fis.read(rgb)) > -1)
				zipoutputstream.write(rgb, 0, n);
			fis.close();
			zipoutputstream.closeEntry();
		}

		zipoutputstream.close();
	}

	/**
	 * Extracts the contents of a zip file to the specified directory
	 * The zipfile should *not* have directories, because they will not be
	 * created and it will fail.
	 */
	public static void extractZip(File ficheroZip, File dest)
			throws IOException {
		InputStream fis = new BufferedInputStream(new FileInputStream(
				ficheroZip));
		ZipInputStream zis = new ZipInputStream(fis);
		ZipEntry e;
		byte[] b = new byte[512];

		//log.debug("Extracting zip: "+ficheroZip.getName());
		while ((e = zis.getNextEntry()) != null) {

			// baskslash-protection: zip format expects only 'fw' slashes
			String name = e.getName().replace('\\', '/');
			name = name.replaceAll("\\.\\./", "");

			if (e.isDirectory()) {
				//log.debug("\tExtracting directory "+e.getName());
				File dir = new File(dest, name);
				dir.mkdirs();
				continue;
			}

			//log.debug("\tExtracting file "+name);
			File outFile = new File(dest, name);
			if (!outFile.getParentFile().exists()) {
				//log.warn("weird zip: had to create parent: "+outFile.getParentFile());
				outFile.getParentFile().mkdirs();
			}
			FileOutputStream fos = new FileOutputStream(outFile);

			int len = 0;
			while ((len = zis.read(b)) != -1)
				fos.write(b, 0, len);
			fos.close();
		}
		zis.close();
	}

	/**
	 * Reads a file to a string, specifying an encoding
	 */
	public static String readFileToString(File f, String encoding)
			throws IOException {
		char buffer[] = new char[(int) (f.length() * 1.5)];
		FileInputStream fis = new FileInputStream(f);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis,
				encoding));
		br.read(buffer, 0, (int) f.length());
		br.close();
		return new String(buffer, 0, (int) f.length());
	}

	/**
	 * Reads a file to a string, automagically choosing the correct encoding
	 * (internally, it will be converted to UTF-8, which is what Java uses)
	 *
	 * ONLY USE WHEN ENCODING IS UNKNOWN
	 */
	public static String readFileToString(File f) throws IOException {
		String s1 = readFileToString(f, "UTF-8");
		String s2 = readFileToString(f, "ISO-8859-1");
		int badChars1 = 0;
		int badChars2 = 0;
		for (int i = 0; i < s1.length(); i++) {
			if (s1.charAt(i) == '?')
				badChars1++;
			if (s2.charAt(i) == '?')
				badChars2++;
		}
		return (badChars1 < badChars2) ? s1 : s2;
	}

	/**
	 * Writes a string to a file
	 */
	public static void writeStringToFile(File f, String s) throws IOException {
		FileOutputStream fos = new FileOutputStream(f);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos,
				DEFAULT_FILE_ENCODING));
		bw.write(s);
		bw.close();
	}

	/**
	 * Converts a full path into a relative one
	 */
	public static String relativePath(String fullPath) {
		int pathPos = 0;
		pathPos = Math.max(pathPos, fullPath.lastIndexOf('=') + 1); // a form
		pathPos = Math.max(pathPos, fullPath.lastIndexOf('/') + 1); // *nix
		pathPos = Math.max(pathPos, fullPath.lastIndexOf('\\') + 1); // windows
		return fullPath.substring(pathPos);
	}

	/**
	 * Create a file from a 'relative' file URL 
	 */
	public static File getFileFromUrl(String url, File base) {

		// check to see if absolute (no '.' as first char) - and ignore base if so
		try {
			if (!url.startsWith("file://.")) {
				java.net.URI fileURI = new java.net.URI(url);
				return new File(fileURI);
			}
		} catch (java.net.URISyntaxException urise) {
			throw new IllegalArgumentException("Bad URL, cannot process " + url);
		}

		// return a file using the base
		return new File(base, url.substring("file://.".length()));
	}

	/**
	 * Ask the user to provide a file or directory
	 */
	public static File chooseFile(Component p, String message, boolean toOpen,
			int fileType) {
		JFileChooser jfc = new JFileChooser();
		jfc.setDialogTitle("Selecciona " + message);
		jfc.setFileSelectionMode(fileType);
		File f = null;
		while (f == null) {
			int rc = (toOpen ? jfc.showOpenDialog(p) : jfc.showSaveDialog(p));
			if (rc == JFileChooser.CANCEL_OPTION) {
				f = null;
				break;
			}

			f = jfc.getSelectedFile();
			if (f == null || (!f.exists() && toOpen)
					|| (fileType == JFileChooser.FILES_ONLY && f.isDirectory())) {
				JOptionPane.showMessageDialog(null, "Error: " + message
						+ " invalido", "Error", JOptionPane.ERROR_MESSAGE);
				f = null;
				continue;
			}
		}
		return f;
	}
}
