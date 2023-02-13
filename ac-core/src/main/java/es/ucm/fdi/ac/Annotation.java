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
package es.ucm.fdi.ac;

import es.ucm.fdi.util.XMLSerializable;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import org.jdom2.Element;

import static es.ucm.fdi.util.I18N.m;

/**
 * An annotation represents a note (authored by the system or a human) 
 * regarding a given submission.
 *
 * @author mfreire
 */
public class Annotation implements XMLSerializable {

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy.MM.dd HH:mm;ss (Z)");

	/**
	 * Types of annotation
	 */
	public enum Label {
		/** same person, two submissions that are (understandably) very similar */
		Duplicate,
		/** different person, two submissions that are a confirmed, evil copy */
		Copy,
		/** both look similar but, according to author, are not an evil copy */
		Notcopy,
		/** the author is not sure if this is a copy, and wants to explain why */
		Suspect,
		/** other type of annotation */
		Other,
	};

	private LocalDateTime date;
	private String author;

	/** another submission's ID. Can be null for general comments */
	private String target;
	/** Commentary of the annotation. Can be empty */
	private String commentary;
	/** List of labels for this annotation. Cannot be empty */
	private final ArrayList<Label> labels = new ArrayList<Label>();

	/*
	 * localFile and targetFile atributes precisely indicate
	 * which files are objects of the annotation.
	 * Both arguments are optional, and are expected to be relative
	 * paths to the submission (for localFile) and the target attribute
	 * of the annotation (for targetFile).
	 */
	private String localFile;
	private String targetFile;

	/**
	 * Create an empty annotation instance
	 */
	public Annotation() {
	}

	public Annotation(String author, Label label) {
		this.author = author;
		this.date = LocalDateTime.now();
		labels.add(label);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Annotation [");
		for (Label label : getLabels()) {
			sb.append(" ").append(label.toString());
		}
		sb.append("]");
		if (commentary != null) {
			sb.append(": ").append(commentary);
		}
		return sb.toString();
	}

	/**
	 * @return the date
	 */
	public LocalDateTime getDate() {
		return date;
	}

	/**
	 * @param date the date to set
	 */
	public void setDate(LocalDateTime date) {
		this.date = date;
	}

	/**
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * @param author the author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * @return the localFile
	 */
	public String getLocalFile() {
		return localFile;
	}

	/**
	 * @param localFile the localFile to set
	 */
	public void setLocalFile(String localFile) {
		this.localFile = localFile;
	}

	/**
	 * @return the targetFile
	 */
	public String getTargetFile() {
		return targetFile;
	}

	/**
	 * @param targetFile the targetFile to set
	 */
	public void setTargetFile(String targetFile) {
		this.targetFile = targetFile;
	}

	/**
	 * @return the commentary
	 */
	public String getCommentary() {
		return commentary;
	}

	/**
	 * @param commentary the commentary to set
	 */
	public void setCommentary(String commentary) {
		this.commentary = commentary;
	}

	/**
	 * @return the target
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @param target the target to set
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * List of labels for this annotation. Cannot be empty 
	 * @return the labels
	 */
	public ArrayList<Label> getLabels() {
		return labels;
	}

	public Element saveToXML() throws IOException {
		Element element = new Element("annotation");

		// Add labels attribute
		StringBuilder labelsString = new StringBuilder();
		for (Label l : labels) {
			labelsString.append(l.toString());
			labelsString.append(" ");
		}
		element.setAttribute("labels", labelsString.toString().trim());

		// Add comment
		if (commentary != null) {
			element.setText(commentary.trim());
		}

		// Add optional attributes
		if (author != null) {
			element.setAttribute("author", author.trim());
		}
		if (date != null) {
			element.setAttribute("date", date
					.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		}
		if (target != null) {
			element.setAttribute("target", target);
		}
		if (localFile != null) {
			element.setAttribute("localFile", localFile);
		}
		if (targetFile != null) {
			element.setAttribute("targetFile", targetFile);
		}

		return element;
	}

	public void loadFromXML(Element element) throws IOException {
		String labelsAttribute = element.getAttributeValue("labels");
		labels.clear();
		for (String labelString : labelsAttribute.split(" ")) {
			labels.add(Label.valueOf(labelString.toLowerCase()));
		}

		author = element.getAttributeValue("author");
		target = element.getAttributeValue("target");

		localFile = element.getAttributeValue("localFile");
		if (localFile != null) {
			while (localFile.endsWith("/")) {
				localFile = localFile.substring(0, localFile.length() - 1);
			}
		}
		targetFile = element.getAttributeValue("targetFile");
		if (targetFile != null) {
			while (targetFile.endsWith("/")) {
				targetFile = targetFile.substring(0, targetFile.length() - 1);
			}
		}
		if (element.getAttributeValue("date") != null) {
			date = LocalDateTime.parse(element.getAttributeValue("date"));
		}

		if (element.getText() != null) {
			commentary = element.getText().trim();
		}
	}

	/**
	 * Tests the annotation validity
	 * @param a analysis to validate against
	 * @param s submission against which this annotation was made
	 * @param messages where errors will be written for human consumption; 
	 *      can be null
	 * @return true if valid
	 */
	public boolean isValid(Analysis a, Submission s, ArrayList<String> messages) {
		if (messages == null) {
			messages = new ArrayList<String>();
		}

		if (labels.isEmpty()) {
			messages.add(m("annotation.error.no-labels"));
		}
		if (localFile != null && !s.containsSource(localFile)) {
			messages.add(m("annotation.error.local-file-not-found", localFile));
		}
		if (target != null) {
			Submission other = a.getSubmission(target);
			if (other == null) {
				messages.add(m("annotation.error.target-not-found", target));
			} else {
				if (!other.containsSource(targetFile)) {
					messages.add(m("annotation.error.target-file-not-found",
							targetFile));
				}
			}
		}
		return !messages.isEmpty();
	}
}
