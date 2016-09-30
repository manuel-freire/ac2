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
 * Edge.java
 *
 * Created on July 11, 2006, 4:09 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.model;

/**
 * An Edge. This interface is used only for tagging purposes.
 *
 * @author mfreire
 */
public class Edge {
	protected Object src;
	protected Object dst;
	protected Object data;

	public Edge() {
		System.err.println("Built default edge... (this should not happen)");
		Thread.dumpStack();
	}

	public Edge(Object src, Object dst) {
		this.src = src;
		this.dst = dst;

		//        String s = getClass().getSimpleName();
		//        if ( ! s.equals("TestEdge") 
		//        //&& src instanceof String && dst instanceof String
		//                ) {
		//            System.err.println("Created a lowly edge: ");
		//            Thread.dumpStack();
		//        }
	}

	public Object getSource() {
		return src;
	}

	public Object getTarget() {
		return dst;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public Object getData() {
		return data;
	}
}
