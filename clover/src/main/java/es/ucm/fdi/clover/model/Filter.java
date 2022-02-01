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

import org.jdom2.Element;

/**
 * A filter decides whether to let parts of a graph pass through to a subgraph
 * or not.
 *
 * @author mfreire
 */
public interface Filter {

	/**
	 * Serialization support; save filter settings to big string
	 */
	public void save(Element e);

	/**
	 * Serialization support; restore settings from previously created string
	 */
	public void restore(Element e);

	public boolean isEdgeValid(Edge e);

	public boolean isVertexValid(Object v);
}
