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
 * Submission.java 
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * 18-Apr-2006: first version (mfreire)
 */
package es.ucm.fdi.ac;

import es.ucm.fdi.util.SourceFileCache;
import es.ucm.fdi.util.XMLSerializable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.jdom2.Element;

/**
 * An item that can be compared to others. 
 * Submissions store their sources, annotations, 
 * tokenized versions of these sources, test-results, 
 * intermediate test-results, and so on.
 *
 * @author mfreire
 */
public class Submission implements XMLSerializable {

	/**
	 * subject ID
	 */
	private final String id;
	/**
	 * subject internal index
	 */
	private final int internalId;
	/**
	 * source code, as a list of filename+filecontents
	 */
	private final ArrayList<Source> sources = new ArrayList<Source>();

	private final ArrayList<Annotation> annotations = new ArrayList();

	private final static String annotationKey = "annotation";

	/**
	 * Processed source, test run results, and so on the key 'annotations' is
	 * used to store an arraylist of annotations
	 */
	private final HashMap<String, Object> data = new HashMap<String, Object>();

	/**
	 * Basic constructor
	 * @param id used to identify this submission
	 * @param index used as an internal ID
	 */
	public Submission(String id, int index) {
		if (id.contains(".")) {
			id = id.substring(0, id.lastIndexOf("."));
		}
		this.id = id;
		this.internalId = index;
		data.put(annotationKey, annotations);
	}

	/**
	 * Adds a source-file
	 * @param f source-file to add
	 */
	public void addSource(File f) {
		String source = SourceFileCache.getSource(f);
		sources.add(new Source(source, f.getName()));
	}

	public String getId() {
		return id;
	}

	public int getInternalId() {
		return internalId;
	}

	public boolean containsSource(String sourceName) {
		boolean sourceExists = false;

		for (Source s : getSources()) {
			if (s.getFileName().equals(sourceName)) {
				sourceExists = true;
				break;
			}
		}

		return sourceExists;
	}

	public ArrayList<Source> getSources() {
		return sources;
	}

	public String getSourceCode(int i) {
		return sources.get(i).getCode();
	}

	public String getSourceName(int i) {
		return sources.get(i).getFileName();
	}

	public Object getData(String key) {
		return data.get(key);
	}

	public void putData(String key, Object value) {
		data.put(key, value);
	}

	@Override
	public String toString() {
		return getId();
	}

	public Element saveToXML() throws IOException {
		Element se = new Element("submission");
		se.setAttribute("id", id);
		for (Annotation annotation : annotations) {
			se.addContent(annotation.saveToXML());
		}
		return se;
	}

	public void loadFromXML(Element element) throws IOException {
		annotations.clear();
		// load annotations
		for (Element ae : (List<Element>) element.getChildren("annotation")) {
			Annotation a = new Annotation();
			a.loadFromXML(ae);
			addAnnotation(a);
		}
	}

	/**
	 * Source code
	 */
	public static class Source {

		private final String code;
		private final String fileName;

		public Source(String code, String fileName) {
			this.code = code;
			this.fileName = fileName;
		}

		/**
		 * @return the code
		 */
		public String getCode() {
			return code;
		}

		/**
		 * @return the fileName
		 */
		public String getFileName() {
			return fileName;
		}
	}

	public void addAnnotation(Annotation annotation) {
		annotations.add(annotation);
	}

	public Collection<Annotation> getAnnotations() {
		return annotations;
	}
}
