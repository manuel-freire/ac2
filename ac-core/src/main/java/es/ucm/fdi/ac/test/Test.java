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
package es.ucm.fdi.ac.test;

import es.ucm.fdi.ac.Submission;
import es.ucm.fdi.util.XMLSerializable;
import java.io.IOException;
import org.jdom2.Element;

/**
 * A test that can be applied to two subjects.
 *
 * @author mfreire
 */
public abstract class Test implements XMLSerializable {

	/** the test's key; may not contain spaces */
	protected String testKey;

	/** required keys */
	protected String[] requires = new String[0];

	/** provided keys */
	protected String[] provides = new String[0];

	/** true if test preprocessing is parallelizable */
	protected boolean independentPreprocessing = false;
	/** true if test similarity is parallelizable */
	protected boolean independentSimilarity = false;

	/** if true, test was canceled before finishing, and must be repeated */
	private boolean testCanceled;
	/** progress, in range [0,1] */
	private float progress;

	/**
	 * Configures this test
	 * @param e the jdom element to read settings from 
	 * @throws IOException on error
	 */
	public void loadFromXML(Element e) throws IOException {
		testKey = e.getAttributeValue("key");
		requires = attributeToStringArray(e.getAttributeValue("requires"));
		provides = attributeToStringArray(e.getAttributeValue("provides"));
	}

	/**
	 * Saves the test's configuration as a JDom element.
	 * @return saved configuration, or 'null' if nothing to save.
	 * @throws IOException on error
	 */
	public Element saveToXML() throws IOException {
		Element e = new Element("test");
		e.setAttribute("class", getClass().toString());
		e.setAttribute("key", getTestKey());
		e.setAttribute("requires", stringArrayToAttribute(requires));
		e.setAttribute("provides", stringArrayToAttribute(provides));
		saveInner(e);
		return e;
	}

	private static String stringArrayToAttribute(String[] array) {
		StringBuilder sb = new StringBuilder();
		for (String s : array) {
			sb.append(s).append(" ");
		}
		return sb.toString().trim();
	}

	private static String[] attributeToStringArray(String attribute) {
		return attribute.split("[, ]+");
	}

	/**
	 * Implemented by subclasses to save state in an element
	 * @param e 
	 * @throws IOException on error
	 */
	protected abstract void saveInner(Element e) throws IOException;

	/**
	 * Global initialization for the test
	 */
	public void init(Submission[] subjects) {
		// the default is to do nothing
	}

	/**
	 * All subjects will have been preprocessed before similarity is 
	 * checked.
	 */
	public abstract void preprocess(Submission s);

	/**
	 * Determine similarity between two subjects
	 * @return a number between 0 (most similar) and 1 (least similar)
	 */
	public abstract float similarity(Submission a, Submission b);

	/**    
	 * @return true if similarity calculation can be parallelized;
	 * default is false
	 */
	public final boolean isIndependentSimilarity() {
		return independentSimilarity;
	}

	/**    
	 * @return true if preprocessing calculation can be parallelized; 
	 * default is false
	 */
	public final boolean isIndependentPreprocessing() {
		return independentPreprocessing;
	}

	public boolean isCancelled() {
		return testCanceled;
	}

	public void setCancelled(boolean testCancelled) {
		this.testCanceled = testCancelled;
	}

	/**
	 * Returns current progress
	 */
	public float getProgress() {
		return progress;
	}

	public void setProgress(float progress) {
		this.progress = progress;
	}

	/**
	 * @return the testKey
	 */
	public String getTestKey() {
		return testKey;
	}

	/**
	 * @param testKey the testKey to set
	 */
	public void setTestKey(String testKey) {
		this.testKey = testKey;
	}

	public String[] getRequires() {
		return requires;
	}

	public String[] getProvides() {
		return provides;
	}
}
