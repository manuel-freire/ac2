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
 * ClusteringEngine.java
 *
 * Created on May 15, 2006, 5:21 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package es.ucm.fdi.clover.model;

import es.ucm.fdi.clover.event.HierarchyChangeEvent;
import es.ucm.fdi.clover.event.StructureChangeEvent;
import org.jdom2.Element;

/**
 * A clustering engine understands how to build and update a ClusteringHierarchy.
 * It can also be asked to provide the importance of a vertex based on 
 * focus distance. The idea is that ClusteringEngines are application-specific,
 * while ClusteringHierarchies are not.
 *
 * @author mfreire
 */
public interface ClusteringEngine {

	/**
	 * Serialization support: save the state of the clustering (interesting
	 * for quick restore of inner state; user-modified clusterings and the like)
	 */
	void save(Element e);

	/**
	 * Serialization support: restore the clustering's state
	 */
	void restore(Element e);

	/**
	 * Create a new hierarchy from the given base graph, starting at the
	 * given rootVertex
	 */
	Cluster createHierarchy(BaseGraph base, Object rootVertex);

	/**
	 * Update a hierarchy from the given BaseGraph (which has already been
	 * updated with the changes in the sce), currently rooted at root, 
	 * and store the results in the provided hce.
	 * Return the new root.
	 */
	Cluster updateHierarchy(Cluster root, BaseGraph base,
			StructureChangeEvent sce, HierarchyChangeEvent hce);
}
