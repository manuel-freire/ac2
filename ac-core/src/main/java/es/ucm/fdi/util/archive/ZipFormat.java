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
 * ZipFormat.java
 *
 * Created on September 18, 2006, 6:25 PM
 *
 */

package es.ucm.fdi.util.archive;

import es.ucm.fdi.util.FileUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

/**
 * Manages the popular Zip format (PKZIP, Jar, War); uses Ant's buit-in
 * unzipper, which is more tolerant than the util.zip one.
 *
 * @author mfreire
 */
public class ZipFormat implements ArchiveFormat {

	private static byte[] zipMagic = new byte[] { 0x50, 0x4b }; // 0x03, 0x30 both found

	public String getArchiveExtensions() {
		return "(.*\\.zip)|(.*\\.jar)|(.*\\.war)";
	}

	private void assertIsZip(File source) throws IOException {
		try {
			if (!FileUtils.startMatches(new FileInputStream(source), zipMagic,
					0)) {
				throw new IOException("File is not a zip archive");
			}
		} catch (IOException ioe) {
			throw new IOException("File is not a zip archive", ioe);
		}
	}

	public ArrayList<String> list(File source) throws IOException {
		assertIsZip(source);

		ZipFile zf = new ZipFile(source);
		ArrayList<String> paths = new ArrayList<String>();

		Enumeration entries = zf.getEntries();
		while (entries.hasMoreElements()) {
			ZipEntry e = (ZipEntry) entries.nextElement();

			String name = FileUtils.toCanonicalPath(e.getName());
			if (e.isDirectory()) {
				continue;
			}

			paths.add(name);
		}
		return paths;
	}

	public void expand(File source, File destDir) throws IOException {
		assertIsZip(source);

		ZipFile zf = new ZipFile(source);
		byte[] b = new byte[512];

		try {
			//log.debug("Extracting zip: "+ficheroZip.getName());
			Enumeration entries = zf.getEntries();
			while (entries.hasMoreElements()) {
				ZipEntry e = (ZipEntry) entries.nextElement();

				// baskslash-protection: zip format expects only 'fw' slashes
				String name = FileUtils.toCanonicalPath(e.getName());

				if (e.isDirectory()) {
					//log.debug("\tExtracting directory "+e.getName());
					File dir = new File(destDir, name);
					dir.mkdirs();
					continue;
				}

				//log.debug("\tExtracting file "+name);
				File outFile = new File(destDir, name);
				if (!outFile.getParentFile().exists()) {
					//log.warn("weird zip: had to create parent: "+outFile.getParentFile());
					outFile.getParentFile().mkdirs();
				}
				FileOutputStream fos = new FileOutputStream(outFile);

				InputStream zis = zf.getInputStream(e);
				int len = 0;
				while ((len = zis.read(b)) != -1)
					fos.write(b, 0, len);
				fos.close();
				zis.close();
			}
		} finally {
			zf.close();
		}
	}

	public boolean extractOne(File source, String path, File dest)
			throws IOException {
		assertIsZip(source);

		ZipFile zf = new ZipFile(source);
		byte[] b = new byte[512];

		try {
			//log.debug("Extracting zip: "+ficheroZip.getName());        
			Enumeration entries = zf.getEntries();
			while (entries.hasMoreElements()) {
				ZipEntry e = (ZipEntry) entries.nextElement();

				// baskslash-protection: zip format expects only 'fw' slashes
				String name = FileUtils.toCanonicalPath(e.getName());

				//                System.err.println(" "+name+" =? "+path);
				if (!name.equals(path) || e.isDirectory())
					continue;

				if (!dest.getParentFile().exists()) {
					//log.warn("weird zip: had to create parent: "+outFile.getParentFile());
					dest.getParentFile().mkdirs();
				}
				FileOutputStream fos = new FileOutputStream(dest);

				InputStream zis = zf.getInputStream(e);
				int len = 0;
				while ((len = zis.read(b)) != -1)
					fos.write(b, 0, len);
				fos.close();
				zis.close();
				return true;
			}
		} finally {
			zf.close();
		}

		return false;
	}

	public void create(ArrayList<File> sources, File destFile, File baseDir)
			throws IOException {
		FileOutputStream fos = new FileOutputStream(destFile);
		ZipOutputStream zipoutputstream = new ZipOutputStream(fos);
		zipoutputstream.setMethod(ZipOutputStream.DEFLATED);
		byte[] rgb = new byte[1024];
		FileInputStream fis;

		//log.debug("Creating zip file: "+ficheroZip.getName());
		for (int i = 0; i < sources.size(); i++) {
			File file = (File) sources.get(i);
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
				for (int j = 0; j < children.length; j++) {
					sources.add(children[j]);
				}
				zipoutputstream.putNextEntry(entry);
				continue;
			}

			//log.debug("\tAdding file "+fileName);

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
	 * Simulates creation of a zip file, but returns only the size of the zip
	 * that results from the given input stream
	 */
	public int compressedSize(InputStream is) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ZipOutputStream zout = new ZipOutputStream(bos);
		zout.setMethod(ZipOutputStream.DEFLATED);
		ZipEntry entry = new ZipEntry("z");
		zout.putNextEntry(entry);
		return FileUtils.compressedSize(is, zout, bos);
	}
}
