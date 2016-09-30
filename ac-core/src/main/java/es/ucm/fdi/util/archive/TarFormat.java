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
 * TarFormat.java
 *
 * Created on September 18, 2006, 6:33 PM
 *
 */

package es.ucm.fdi.util.archive;

import es.ucm.fdi.util.FileUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

/**
 *
 * @author mfreire
 */
public class TarFormat implements ArchiveFormat {

	private static final byte[] tarMagic = new byte[] { 0x75, 0x73, 0x74 };
	private static final int tarMagicOffset = 257;

	private static final byte[] gzMagic = new byte[] { 0x1f, (byte) 0x8b };

	public String getArchiveExtensions() {
		return "(.*\\.tar)|(.*\\.gz)|(.*\\.tgz)";
	}

	public ArrayList<String> list(File source) throws IOException {
		InputStream is = getTarInputStream(source);
		TarInputStream tis = new TarInputStream(is);
		TarEntry e;
		byte[] b = new byte[512];
		ArrayList<String> paths = new ArrayList<String>();

		while ((e = tis.getNextEntry()) != null) {

			String name = FileUtils.toCanonicalPath(e.getName());
			if (e.isDirectory()) {
				continue;
			}

			paths.add(name);
		}
		tis.close();

		return paths;
	}

	public void expand(File source, File destDir) throws IOException {
		InputStream is = getTarInputStream(source);
		TarInputStream tis = new TarInputStream(is);
		TarEntry e;
		byte[] b = new byte[512];

		//System.err.println("Extract begins here");
		//log.debug("Extracting zip: "+ficheroZip.getName());
		while ((e = tis.getNextEntry()) != null) {

			// baskslash-protection: zip format expects only 'fw' slashes
			//System.err.println("Found entry: "+e.getName());
			String name = FileUtils.toCanonicalPath(e.getName());

			if (e.isDirectory()) {
				//log.debug("\tExtracting directory "+e.getName());
				File dir = new File(destDir, name);
				dir.mkdirs();
				continue;
			}

			//System.err.println("\tExtracting file "+name);
			File outFile = new File(destDir, name);
			if (!outFile.getParentFile().exists()) {
				outFile.getParentFile().mkdirs();
			}
			FileOutputStream fos = new FileOutputStream(outFile);

			int len = 0;
			while ((len = tis.read(b)) != -1)
				fos.write(b, 0, len);
			fos.close();
		}
		tis.close();
	}

	private InputStream getTarInputStream(File tarFile) throws IOException {
		boolean isTar = FileUtils.startMatches(new FileInputStream(tarFile),
				tarMagic, tarMagicOffset);
		boolean isGz = isTar ? false : FileUtils.startMatches(
				new FileInputStream(tarFile), gzMagic, 0);

		if (isTar) {
			return new FileInputStream(tarFile);
		} else if (isGz) {
			InputStream is = new GZIPInputStream(new FileInputStream(tarFile));
			try {
				isTar = FileUtils.startMatches(is, tarMagic, tarMagicOffset);
			} finally {
				is.close();
			}
			if (!isTar) {
				throw new IOException(
						"Archive is Gz, but does not contain a Tar");
			}
			return new GZIPInputStream(new FileInputStream(tarFile));
		} else {
			throw new IOException("Archive is neither Tar nor Gz");
		}
	}

	public void create(ArrayList<File> sources, File destFile, File baseDir)
			throws IOException {
		throw new UnsupportedOperationException(
				"Cannot create TAR files... yet");
	}

	public int compressedSize(InputStream is) throws IOException {
		throw new UnsupportedOperationException(
				"Fast RAR compressed size calculation not supported");
	}

	public boolean extractOne(File source, String path, File dest)
			throws IOException {
		InputStream is = getTarInputStream(source);
		TarInputStream tis = new TarInputStream(is);
		TarEntry e;
		byte[] b = new byte[512];

		//System.err.println("Extract begins here");
		//log.debug("Extracting zip: "+ficheroZip.getName());
		while ((e = tis.getNextEntry()) != null) {

			String name = FileUtils.toCanonicalPath(e.getName());
			if (!name.equals(path) || e.isDirectory())
				continue;

			if (!dest.getParentFile().exists()) {
				dest.getParentFile().mkdirs();
			}
			FileOutputStream fos = new FileOutputStream(dest);

			int len = 0;
			while ((len = tis.read(b)) != -1)
				fos.write(b, 0, len);
			fos.close();
			tis.close();
			return true;
		}

		tis.close();
		return false;
	}
}
