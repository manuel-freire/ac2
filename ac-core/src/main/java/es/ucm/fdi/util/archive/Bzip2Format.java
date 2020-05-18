/**
 * AC - A source-code copy detector
 *
 *     For more information please visit:  http://github.com/manuel-freire/ac
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
 * Bzip2Format.java
 *
 * Created on June 6, 2006, 6:25 PM
 *
 */

package es.ucm.fdi.util.archive;

import es.ucm.fdi.util.FileUtils;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Manages the BZip2 compression format. Ant's implementation is slow and requires
 * heaps of memory; the one in jaxlib is almost as fast as zip (!!).
 *
 * @author mfreire
 */
public class Bzip2Format implements ArchiveFormat {

	public String getArchiveExtensions() {
		return "(.*\\.bz2)";
	}

	public ArrayList<String> list(File source) throws IOException {
		throw new UnsupportedOperationException(
				"Bzip2 does not support listing");
	}

	public void expand(File source, File destDir) throws IOException {
		throw new UnsupportedOperationException(
				"Bzip2 does not support expansion");
	}

	public boolean extractOne(File source, String path, File dest)
			throws IOException {
		throw new UnsupportedOperationException(
				"Bzip2 does not support extraction");
	}

	public void create(ArrayList<File> sources, File destFile, File baseDir)
			throws IOException {
		throw new UnsupportedOperationException(
				"Bzip2 does not support creation");
	}

	/**
	 * Simulates creation of a compressed file, but returns only the size of the file
	 * that results from the given input stream
	 */
	public int compressedSize(InputStream is) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		return FileUtils.compressedSize(is,
				new BZip2CompressorOutputStream(bos), bos);
	}
}
