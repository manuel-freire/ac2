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
package es.ucm.fdi.clover.model;

import es.ucm.fdi.clover.event.StructureChangeEvent;
import es.ucm.fdi.clover.event.StructureChangeListener;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import java.util.ArrayList;

/**
 * This class is the base for the clover graph vis. framework. You should
 * implement a subclass of BaseGraph capable of reading/writing to your underlying data
 * model. Once you have done that, clover should be ready to use
 * (although you can still customize many, many aspects further along the pipeline).
 *
 * Vertices can be of any type you want them to - just declare the 'V' template
 * parameter however you like. Edges must extend 'Edge', because access to their
 * 'getObject' method is required in certain cases (and we couldnt come up with
 * any reason for not requesting that; after all, most information is in the vertices...).
 *
 * @author mfreire
 */
@SuppressWarnings( { "unchecked", "rawtypes" })
public class BaseGraph extends DefaultDirectedWeightedGraph implements
		StructureChangeListener {

	private Log log = LogFactory.getLog(BaseGraph.class);

	protected ArrayList<StructureChangeListener> structureListeners;

	public BaseGraph() {
		this(new DefaultEdgeFactory());
	}

	public BaseGraph(DefaultEdgeFactory edgeFactory) {
		super(edgeFactory);
		structureListeners = new ArrayList<StructureChangeListener>();
	}

	public void addStructureChangeListener(StructureChangeListener l) {
		log.debug("StructureListener added: " + l);
		structureListeners.add(l);
	}

	public void removeStructureChangeListener(StructureChangeListener l) {
		structureListeners.remove(l);
	}

	protected void fireStructureChangeEvt(StructureChangeEvent evt) {
		for (StructureChangeListener l : structureListeners) {
			l.structureChangePerformed(evt);
		}
	}

	/**
	 * This is needed to store & recover the graph. The contract is that
	 * - the id must be unique for each vertex (including cluster vertices)
	 * - after loading anew, the id of each vertex equals the previously-saved one
	 *
	 * If this is not correctly implemented, saved clusterings and layouts may 
	 * not be restored correctly after a save.
	 *
	 * Change the default unless you are sure it will work!
	 *
	 * Identifiers must *not* contain anything in this set:
	 *    '"[]{},.:\n'
	 */
	public String getId(Object vertex) {
		return vertex.toString();
	}

	/**
	 * Dumps a list of edges and their types to stderr
	 */
	public String dumpEdgeTypes() {
		StringBuffer sb = new StringBuffer("Edge dump: ");
		for (Edge e : (Set<Edge>) edgeSet()) {
			Object a = e.getSource();
			Object b = e.getTarget();
			sb.append("  " + getVertexLabel(a) + "("
					+ a.getClass().getSimpleName() + ") -> "
					+ getVertexLabel(b) + "(" + b.getClass().getSimpleName()
					+ ") : [" + e.getClass().getSimpleName() + "]\n");
			sb.append("\t" + e.getData() + "\n");
		}
		return sb.toString();
	}

	/**
	 * updates the graph with upstream changes. Once updated, the event
	 * is passed along.
	 */
	public void structureChangePerformed(StructureChangeEvent evt) {
		removeAllVertices(evt.getRemovedVertices());
		removeAllEdges(evt.getRemovedEdges());
		Graphs.addAllVertices(this, evt.getAddedVertices());
		log.debug("BaseGraph event was: " + evt.getDescription());
		for (Edge e : evt.getAddedEdges()) {
			if (!containsVertex(e.getSource())) {
				throw new IllegalArgumentException("Source vertex not there: "
						+ getVertexLabel(e.getSource()));
			}
			if (!containsVertex(e.getTarget())) {
				throw new IllegalArgumentException("Target vertex not there: "
						+ getVertexLabel(e.getTarget()));
			}
			addEdge(e.getSource(), e.getTarget(), e);
		}

		fireStructureChangeEvt(evt);
	}

	public String getVertexLabel(Object o) {
		return "" + o;
	}

	public String getEdgeLabel(Edge e) {
		return "";
	}
}
