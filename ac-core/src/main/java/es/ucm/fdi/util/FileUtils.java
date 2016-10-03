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
 * FileUtils.java 
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * Contributors: - 
 * Changelog:
 *     18-Dic-2004: first version, for the Wotan project
 *     18-Apr-2006: adapted for use in AC
 */

package es.ucm.fdi.util;

import es.ucm.fdi.util.archive.ArchiveFormat;
import es.ucm.fdi.util.archive.ZipFormat;
import es.ucm.fdi.util.archive.TarFormat;
import es.ucm.fdi.util.archive.RarFormat;
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Locale;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;

import static es.ucm.fdi.util.I18N.m;

/**
 * Miscellaneous utilities that are used throughout the project.
 * All utilities here should be in the form of 'public static' methods.
 *
 * @author mfreire
 */
public class FileUtils {

	public static final Logger log = Logger.getLogger(FileUtils.class);

	public static String UTF_8 = "UTF-8";
	public static String LATIN_1 = "ISO-8859-1";

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
		for (int i = 0; i < files.length; i++) {
			files[i].delete();
		}
	}

	/**
	 * Returns the size (in bytes) of a file or directory. 
	 * Note that actual file-system use may be higher, due to book-keeping.
	 * @param file to count
	 * @return number of bytes used by all files in or under 'file'
	 */
	public static int getDiskUsage(File file) {
		long usage = 0;

		if (file.isDirectory()) {
			// Recursive call
			for (File f : file.listFiles()) {
				usage += getDiskUsage(f);
			}
		} else {
			// Local size
			usage += file.length();
		}

		return (int) usage;
	}

	// little static list of accepted formats
	private static final ArchiveFormat[] formats = new ArchiveFormat[] {
			new ZipFormat(), new TarFormat(), new RarFormat() };

	/**
	 * Returns 'true' if the given file can be uncompressed (may fail, because
	 * it is based on matching the extension against a known list...)
	 */
	public static boolean canUncompress(File f) {
		return f.exists() && f.isFile() && getArchiverFor(f.getName()) != null;
	}

	/**
	 * @param fileName 
	 * @return the extension of this filename (that is, whatever follows the 
	 * last '.', or the empty string, if there is none)
	 */
	public static String getExtension(String fileName) {
		int pos = fileName.lastIndexOf('.');
		return (pos == -1) ? "" : fileName.substring(pos + 1);
	}

	/**
	 * @param fileName
	 * @return the ArchiveFormat to use for a given extension
	 */
	public static ArchiveFormat getArchiverFor(String fileName) {
		String extension = getExtension(fileName);
		for (ArchiveFormat af : formats) {
			if (extension.matches(af.getArchiveExtensions()))
				return af;
		}
		return null;
	}

	/**
	 * Canonicalizes a path, transforming windows '\' to unix '/', and 
	 * stripping off any './' or '../' occurrences, and trimming 
	 * start and end whitespace
	 */
	public static String toCanonicalPath(String name) {
		name = name.replaceAll("\\\\", "/").trim();
		name = name.replaceAll("(\\.)+/", "");
		return name;
	}

	/**
	 * Lists all files in a given location (recursive)
	 */
	public static ArrayList<File> listFiles(File dir) {
		ArrayList<File> al = new ArrayList<File>();
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				al.addAll(listFiles(f));
			} else {
				al.add(f);
			}
		}
		return al;
	}

	/**
	 * @param resourceName resource to attempt to stream
	 * @return null if the given resourceName is not available internally through 
	 * the classLoader, the inputStream otherwise.
	 */
	public static InputStream resourceStream(String resourceName) {
		return FileUtils.class.getClassLoader().getResourceAsStream(
				resourceName);
	}

	public static boolean resourceExists(String resourceName) {
		InputStream is = resourceStream(resourceName);
		if (is != null) {
			try {
				is.close();
			} catch (IOException ioe) {
				// for internal resources, this is really not to be expected
				throw new IllegalStateException(
						"could not close resource stream", ioe);
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @param resourceName resource to attempt to stream
	 * @return null if the given resourceName is not available internally through 
	 * the classLoader, the inputStream otherwise.
	 */
	public static Reader resourceReader(String resourceName) {
		try {
			return new BufferedReader(new InputStreamReader(
					resourceStream(resourceName), UTF_8));
		} catch (UnsupportedEncodingException uee) {
			// utf-8 is guaranteed to be available...
			throw new IllegalStateException("This is a bug in your VM", uee);
		}
	}

	/**
	 * Read bytes from a stream. Used by all "one-shot-reads".
	 * @param is stream to read
	 * @param length number of bytes to read; if less are read, an exception is thrown
	 * @return bytes in the file
	 * @throws IOException on error
	 */
	public static byte[] readStreamToBytes(InputStream is, int length)
			throws IOException {
		byte buffer[] = new byte[length];
		BufferedInputStream bis = null;
		try {
			bis = new BufferedInputStream(is);
			int read = bis.read(buffer, 0, length);
			if (read != length) {
				throw new IOException("not enough bytes: wanted " + length
						+ ", read only " + read);
			}
			return buffer;
		} finally {
			if (bis != null) {
				bis.close();
			}
		}
	}

	/**
	 * Read bytes from a file. Used by all "one-shot-reads".
	 * @param f file to read
	 * @param max if non-zero, at most 'max' bytes will be read
	 * @return bytes in the file
	 * @throws IOException on error
	 */
	public static byte[] readFileToBytes(File f, int max) throws IOException {
		int length = (int) f.length();
		if (max > 0 && max < length) {
			length = max;
		}
		try {
			return readStreamToBytes(new FileInputStream(f), length);
		} catch (IOException ioe) {
			throw new IOException("could not read bytes from "
					+ f.getAbsolutePath(), ioe);
		}
	}

	/**
	 * Reads a file to a string, specifying an encoding.
	 * @param f to read
	 * @param encoding to use 
	 * @return the resulting string
	 * @throws java.io.IOException
	 */
	public static String readFileToString(File f, String encoding)
			throws IOException {
		return new String(readFileToBytes(f, 0), encoding);
	}

	/**
	 * Reads a file to a string, guessing the correct encoding.
	 * @param f to read
	 * @return the resulting string
	 * @throws java.io.IOException
	 */
	public static String readFileToString(File f) throws IOException {
		byte contents[] = readFileToBytes(f, 0);
		String s1 = new String(contents, UTF_8);
		String s2 = new String(contents, LATIN_1);
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
	 * @param f file to write
	 * @param s string to write into it
	 * @throws java.io.IOException
	 */
	public static void writeStringToFile(File f, String s) throws IOException {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(f), UTF_8));
			bw.write(s);
		} catch (IOException ioe) {

		} finally {
			if (bw != null) {
				bw.close();
			}
		}
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
	 * Check the magic in a file
	 * @param is input stream to look into
	 * @param magic to expect in the first bytes
	 * @param offset of first byte to match; leave at 0 for start-to-end
	 * @return true if matches
	 */
	public static boolean startMatches(InputStream is, byte[] magic, int offset)
			throws IOException {
		byte[] startOfFile = readStreamToBytes(is, offset + magic.length);
		if (startOfFile.length != offset + magic.length) {
			return false; // file not long enough
		} else {
			for (int i = offset, j = 0; i < startOfFile.length; i++, j++) {
				if (startOfFile[i] != magic[j]) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * Utility method to calculate compressed sizes. Many compression algorithms define 
	 * "output streams" which make this very easy to implement. The output stream should
	 * wrap the "bos".
	 */
	public static int compressedSize(InputStream is, OutputStream os,
			ByteArrayOutputStream bos) throws IOException {

		// dump file into output, counting the number of total output bytes
		int n;
		byte[] bytes = new byte[1024];
		while ((n = is.read(bytes)) > -1) {
			os.write(bytes, 0, n);
		}
		is.close();
		os.close();
		return bos.size();
	}

	/**
	 * Ask the user to provide a file or directory
	 */
	public static File chooseFile(Component p, String message, boolean toOpen,
			int fileType) {
		JFileChooser jfc = new JFileChooser();
		jfc.setDialogTitle(m("FileChooser.Title", message));
		jfc.setFileSelectionMode(fileType);
		File f = null;
		while (f == null) {
			int rc = (toOpen ? jfc.showOpenDialog(p) : jfc.showSaveDialog(p));
			if (rc == JFileChooser.CANCEL_OPTION) {
				f = null;
				break;
			}

			f = jfc.getSelectedFile();
			String error = null;
			if (f == null) {
				error = m("FileChooser.NothingChosen");
			} else if (!f.exists() && toOpen) {
				error = m("FileChooser.MustExist", m("FileChooser.Directory"));
			} else if (fileType == JFileChooser.FILES_ONLY && f.isDirectory()) {
				error = m("FileChooser.MustBeFile");
			}

			if (error != null) {
				JOptionPane.showMessageDialog(null, m(
						"FileChooser.ErrorMessage", message),
						m("FileChooser.ErrorTitle"), JOptionPane.ERROR_MESSAGE);
				f = null;
				continue;
			}
		}
		return f;
	}
}
