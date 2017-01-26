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
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;

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

		ArrayList<String> paths = new ArrayList<String>();
        try (ZipFile zf = new ZipFile(source)) {
            Enumeration entries = zf.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry e = (ZipArchiveEntry) entries.nextElement();

                String name = FileUtils.toCanonicalPath(e.getName());
                if (e.isDirectory()) {
                    continue;
                }

                paths.add(name);
            }
		}
		return paths;
	}

	public void expand(File source, File destDir) throws IOException {
		assertIsZip(source);

        try (ZipFile zf = new ZipFile(source)) {
            byte[] b = new byte[512];

            Enumeration entries = zf.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry e = (ZipArchiveEntry)entries.nextElement();
                //log.debug("Extracting zip: "+ficheroZip.getName());

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
                try (
                        FileOutputStream fos = new FileOutputStream(outFile);
                        InputStream is = zf.getInputStream(e)
                ){
                    int len;
                    while ((len = is.read(b)) != -1) {
                        fos.write(b, 0, len);
                    }
                }
			}
		}
	}

	public boolean extractOne(File source, String path, File dest)
			throws IOException {
		assertIsZip(source);

		try (ZipFile zf = new ZipFile(source)) {
			byte[] b = new byte[512];

			//log.debug("Extracting zip: "+ficheroZip.getName());
			Enumeration entries = zf.getEntries();
			while (entries.hasMoreElements()) {
				ZipArchiveEntry e = (ZipArchiveEntry) entries.nextElement();

				// baskslash-protection: zip format expects only 'fw' slashes
				String name = FileUtils.toCanonicalPath(e.getName());

				//                System.err.println(" "+name+" =? "+path);
				if (!name.equals(path) || e.isDirectory())
					continue;

				if (!dest.getParentFile().exists()) {
					//log.warn("weird zip: had to create parent: "+outFile.getParentFile());
					dest.getParentFile().mkdirs();
				}

				try (
						FileOutputStream fos = new FileOutputStream(dest);
						InputStream is = zf.getInputStream(e)
				){
					int len;
					while ((len = is.read(b)) != -1) {
						fos.write(b, 0, len);
					}
					return true;
				}
			}
		}
		return false;
	}

	public void create(ArrayList<File> sources, File destFile, File baseDir)
			throws IOException {

        // to avoid modifying input argument
        ArrayList<File> toAdd = new ArrayList<>(sources);
        ZipArchiveOutputStream zos = null;

        try {
            zos = new ZipArchiveOutputStream(
                    new FileOutputStream(destFile));
            zos.setMethod(ZipArchiveOutputStream.DEFLATED);
            byte[] b = new byte[1024];

            //log.debug("Creating zip file: "+ficheroZip.getName());
            for (int i = 0; i < toAdd.size(); i++) {
                // note: cannot use foreach because sources gets modified
                File file = toAdd.get(i);

                // zip standard uses fw slashes instead of backslashes, always
                String baseName = baseDir.getAbsolutePath() + '/';
                String fileName = file.getAbsolutePath().substring(
                        baseName.length());
                if (file.isDirectory()) {
                    fileName += '/';
                }
                ZipArchiveEntry entry = new ZipArchiveEntry(fileName);

                // skip directories - after assuring that their children *will* be included.
                if (file.isDirectory()) {
                    //log.debug("\tAdding dir "+fileName);
                    for (File child : file.listFiles()) {
                        toAdd.add(child);
                    }
                    zos.putArchiveEntry(entry);
                    continue;
                }

                //log.debug("\tAdding file "+fileName);

                // Add the zip entry and associated data.
                zos.putArchiveEntry(entry);

                int n;
                try (FileInputStream fis = new FileInputStream(file)) {
                    while ((n = fis.read(b)) > -1) {
                        zos.write(b, 0, n);
                    }
                    zos.closeArchiveEntry();
                }
            }
        } finally {
            if (zos != null) {
                zos.finish();
                zos.close();
            }
        }
    }

	/**
	 * Simulates creation of a zip file, but returns only the size of the zip
	 * that results from the given input stream
	 */
	public int compressedSize(InputStream is) throws IOException {
		try (
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ZipArchiveOutputStream zout = new ZipArchiveOutputStream(bos);
		) {
			zout.setMethod(ZipArchiveOutputStream.DEFLATED);
			ZipArchiveEntry entry = new ZipArchiveEntry("z");
			zout.putArchiveEntry(entry);
			int n;
			byte[] bytes = new byte[1024];
			while ((n = is.read(bytes)) > -1) {
				zout.write(bytes, 0, n);
			}
			is.close();
			zout.closeArchiveEntry();
			zout.finish();
			return bos.size();
		}
	}
}
