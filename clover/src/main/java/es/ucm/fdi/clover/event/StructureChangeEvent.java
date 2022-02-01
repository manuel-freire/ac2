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
package es.ucm.fdi.clover.event;

import es.ucm.fdi.clover.model.Edge;
import es.ucm.fdi.clover.model.BaseGraph;

import java.util.ArrayList;

/**
 * This event represents a structured graph change. It allows several graph
 * manipulations to be 'batched' together, along with the order in which they
 * are to be represented. This allows listeners to represent the changes in a
 * structured manner, instead of as-they-come.
 *
 * @author mfreire
 */
public class StructureChangeEvent {

	/** type of change that this event describes */
	public enum ChangeType {
		/** this change really adds or removes edges */
		Normal,
		/** this change only modifies attributes of one or more edges or nodes */
		AttributeOnly,
		/** this change is due to visibility changes, and is not truly 'deep' */
		Clustering
	};

	private BaseGraph source;
	protected String description;

	private ChangeType changeType;

	protected ArrayList addedVertices;
	protected ArrayList<Edge> addedEdges;
	protected ArrayList removedVertices;
	protected ArrayList<Edge> removedEdges;

	/** vertices where only changes are in attributes or type */
	protected ArrayList changedVertices;
	/** edges where only changes are in attributes or type */
	protected ArrayList<Edge> changedEdges;

	/**
	 * Creates a new instance of StructureChangeEvent; default change type is
	 * 'Normal'.
	 */
	public StructureChangeEvent(BaseGraph source) {
		this(source, ChangeType.Normal);
	}

	/**
	 * Creates a new instance of StructureChangeEvent
	 */
	public StructureChangeEvent(BaseGraph source, ChangeType changeType) {
		this.source = source;
		this.changeType = changeType;

		addedVertices = new ArrayList();
		removedVertices = new ArrayList();
		addedEdges = new ArrayList<Edge>();
		removedEdges = new ArrayList<Edge>();
		changedVertices = new ArrayList();
		changedEdges = new ArrayList<Edge>();
	}

	/**
	 * @return the type of this event
	 */
	public ChangeType getChangeType() {
		return changeType;
	}

	public ArrayList getAddedVertices() {
		return addedVertices;
	}

	public ArrayList<Edge> getAddedEdges() {
		return addedEdges;
	}

	public ArrayList getRemovedVertices() {
		return removedVertices;
	}

	public ArrayList<Edge> getRemovedEdges() {
		return removedEdges;
	}

	public ArrayList getChangedVertices() {
		return changedVertices;
	}

	public ArrayList<Edge> getChangedEdges() {
		return changedEdges;
	}

	public BaseGraph getSource() {
		return source;
	}

	public String getDescription() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n\t Source: " + source.getClass().getSimpleName());
		sb.append("\n\t Type: " + changeType);
		sb.append("\n\t Description: " + description);
		if (!removedVertices.isEmpty()) {
			sb.append("\n\t RemoveV: ");
			for (Object v : getRemovedVertices()) {
				sb.append(source.getId(v) + " ");
			}
		}
		if (!removedEdges.isEmpty()) {
			sb.append("\n\t RemoveE: ");
			for (Edge e : getRemovedEdges()) {
				sb.append(source.getId(e.getSource()) + "->"
						+ source.getId(e.getTarget()) + " ");
			}
		}
		if (!addedVertices.isEmpty()) {
			sb.append("\n\t AddedV: ");
			for (Object v : getAddedVertices()) {
				sb.append(source.getId(v) + " ");
			}
		}
		if (!addedEdges.isEmpty()) {
			sb.append("\n\t AddedE: ");
			for (Edge e : getAddedEdges()) {
				sb.append(source.getId(e.getSource()) + "->"
						+ source.getId(e.getTarget()) + " ");
			}
		}
		if (!changedVertices.isEmpty()) {
			sb.append("\n\t ChangedV: ");
			for (Object v : getChangedVertices()) {
				sb.append(source.getId(v) + " ");
			}
		}
		if (!changedEdges.isEmpty()) {
			sb.append("\n\t ChangedE: ");
			for (Edge e : getChangedEdges()) {
				sb.append(source.getId(e.getSource()) + "->"
						+ source.getId(e.getTarget()) + " ");
			}
		}
		return sb.toString();
	}
}
