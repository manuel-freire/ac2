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
 * FilteredGraph.java
 *
 * Created on May 6, 2006, 8:23 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.model;

import es.ucm.fdi.clover.event.StructureChangeEvent;
import es.ucm.fdi.clover.event.StructureChangeListener;
import java.util.Collection;
import java.util.HashSet;

/**
 * A FilteredGraph is a variant of Subgraph where a specific Filter is used to 
 * decide what edges should be shown or hidden.
 *
 * @author mfreire
 */
public class FilteredGraph extends BaseGraph {

	/** the base this is built on */
	private BaseGraph base = null;
	private Filter filter = null;
	private UpstreamListener upstreamListener = null;

	/** the vertices that are currently being rejected */
	private HashSet<Object> rejectedCurrentVertices = null;
	/** 
	 * the edges that are currently being rejected; if any endpoint is
	 * rejected in 'rejectedCurrentVertices', the edge will not be listed here
	 */
	private HashSet<Edge> rejectedCurrentEdges = null;

	/**
	 * Creates a new instance of FilteredGraph
	 */
	public FilteredGraph(BaseGraph base, Filter filter) {
		super((DefaultEdgeFactory) base.getEdgeFactory());
		upstreamListener = new UpstreamListener();
		rejectedCurrentVertices = new HashSet<Object>();
		rejectedCurrentEdges = new HashSet<Edge>();

		setFilter(filter);
		setBase(base);
	}

	/**
	 * Graph must be using same EdgeFactory as current base. A filter must
	 * already be set.
	 */
	public void setBase(BaseGraph base) {
		if (base != null) {
			base.removeStructureChangeListener(upstreamListener);
		}
		this.base = base;
		base.addStructureChangeListener(upstreamListener);
		refilter();
	}

	/**
	 * Changes the filter
	 */
	public void setFilter(Filter filter) {
		this.filter = filter;
		if (base != null) {
			refilter();
		}
	}

	/**
	 * Listen to upstream changes in the graph
	 */
	private class UpstreamListener implements StructureChangeListener {
		public void structureChangePerformed(StructureChangeEvent evt) {
			updateFromUpstream(evt);
		}
	}

	/**
	 * Perform (possibly incremental) update; default is full refilter
	 */
	protected void updateFromUpstream(StructureChangeEvent evt) {
		refilter();
	}

	/**
	 * Calculates an operation that filters correctly all of base's vertices
	 * into those that should survive; filters only "delete" -- they never
	 * insert; that should be done above this level.
	 */
	protected void refilter() {

		// Creates the set of "to delete" vertices and edges (from *base*)
		HashSet<Object> rejectedBaseVertices = new HashSet<Object>();
		HashSet<Edge> rejectedBaseEdges = new HashSet<Edge>();

		for (Object v : base.vertexSet()) {
			if (filter.isVertexValid(v)) {
				for (Edge e : (Collection<Edge>) base.outgoingEdgesOf(v)) {
					if ((!rejectedBaseVertices.contains(e.getTarget()))
							&& (!filter.isEdgeValid(e))) {
						rejectedBaseEdges.add(e);
					}
				}
			} else {
				rejectedBaseVertices.add(v);
			}
		}

		StructureChangeEvent evt = new StructureChangeEvent(this);
		HashSet<Object> vertices = new HashSet();
		HashSet<Edge> edges = new HashSet();

		// if to-delete and not already-deleted, delete
		vertices.addAll(rejectedBaseVertices);
		vertices.removeAll(rejectedCurrentVertices);
		evt.getRemovedVertices().addAll(vertices);
		edges.addAll(rejectedBaseEdges);
		edges.removeAll(rejectedCurrentEdges);
		evt.getRemovedEdges().addAll(edges);

		// if not to-delete and already-deleted, restore; recycling rejectedCurrent...
		rejectedCurrentVertices.removeAll(rejectedBaseVertices);
		evt.getAddedVertices().addAll(rejectedCurrentVertices);
		rejectedCurrentEdges.removeAll(rejectedBaseEdges);
		evt.getAddedEdges().addAll(rejectedCurrentEdges);
		// uncovering vertices may uncover their edges
		for (Object recoveredVertex : rejectedCurrentVertices) {
			for (Edge e : (Collection<Edge>) base
					.outgoingEdgesOf(recoveredVertex)) {
				if ((!rejectedBaseVertices.contains(e.getTarget()))
						&& (!rejectedBaseEdges.contains(e))) {
					evt.getAddedEdges().add(e);
				}
			}
		}

		// effect change, and notify downstream
		structureChangePerformed(evt);
	}

	public BaseGraph getBase() {
		return base;
	}

	public Filter getFilter() {
		return filter;
	}
}
