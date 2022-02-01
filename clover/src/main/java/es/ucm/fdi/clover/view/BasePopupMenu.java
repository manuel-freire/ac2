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
package es.ucm.fdi.clover.view;

import es.ucm.fdi.clover.gui.BaseInterface;
import es.ucm.fdi.clover.gui.actions.ContextAwareAction;
import es.ucm.fdi.clover.model.BaseGraph;
import es.ucm.fdi.clover.model.Cluster;
import es.ucm.fdi.clover.model.ClusteredGraph;
import es.ucm.fdi.clover.model.Edge;
import java.awt.Component;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jgraph.graph.DefaultGraphCell;

/**
 * This is the base popup menu for clover applications. 
 * 
 * To control its behaviour, applications have two options: either to 
 * use actions that implement the ContextAwareAction interface in their default
 * actions, or to subclass the BasePopupMenu to add new actions. The popup menu
 * to use is determined by the createMenu() call at BaseInterface creation time.
 *
 * @author mfreire
 */
public class BasePopupMenu extends JPopupMenu {

	/** base app; needed to get actions */
	private BaseInterface app;
	/** either an 'edge' or a 'vertex' or 'null' */
	private Object object;
	/** point where click happened */
	private Point point;

	/** internal; type of item */
	protected boolean isEmpty;
	protected boolean isEdge;
	protected boolean isCluster;
	protected boolean canExpand;
	protected boolean canCollapse;

	/**
	 * Creates a new instance of BasePopupMenu
	 */
	public BasePopupMenu(BaseInterface app, Object o, Point p) {
		this.app = app;
		this.object = o;
		this.point = p;

		isEmpty = (o == null);
		isEdge = !isEmpty && (o instanceof Edge);
		isCluster = !isEmpty && (o instanceof Cluster.Vertex);
		if (isCluster) {
			ClusterView v = app.getView();
			ClusteredGraph cg = (ClusteredGraph) v.getBase();
			Cluster c = ((Cluster.Vertex) o).getCluster();
			if (cg.getSlice().containsClusterOrAncestor(c)) {
				canExpand = true;
			}
		}
		canCollapse = !canExpand;

		populate();
	}

	public Object getObject() {
		return object;
	}

	public Point getPoint() {
		return point;
	}

	public BaseInterface getApp() {
		return app;
	}

	public static boolean canOperateOn(Action a, Object o) {
		if (a == null) {
			return false;
		} else if (a instanceof ContextAwareAction) {
			return ((ContextAwareAction) a).isValidOperand(o);
		}
		return true;
	}

	/**
	 * Triggers the default item for this object
	 */
	public void triggerDefaultAction() {
		Object o = getObject();

		if (isEmpty) {
			triggerFirst(new String[] { "center" });
		} else if (isCluster && canExpand) {
			triggerFirst(new String[] { "expand cluster" });
		} else if (isCluster) {
			triggerFirst(new String[] { "set visible" });
		} else if (!isEdge) {
			triggerFirst(new String[] { "edit" });
		}
	}

	/**
	 * Triggers the first string in preferences found to exist in the
	 * popup menu
	 */
	public void triggerFirst(String[] preferences) {
		HashSet<String> names = new HashSet<String>();
		for (Component c : getComponents()) {
			if (c instanceof JMenuItem) {
				names.add(((JMenuItem) c).getText());
			}
		}
		for (String name : preferences) {
			if (names.contains(name)) {
				app.getAction(name).actionPerformed(
						new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
								name));
				System.err.println("Triggered action " + name + " on "
						+ getObject());
				return;
			}
		}
	}

	/**
	 * Populates the menu
	 */
	protected void populate() {

		// must be a valid graph vertex
		Object o = getObject();

		System.err.println("Popup Object is: " + o);

		boolean isEmpty = (o == null);
		boolean isEdge = !isEmpty && (o instanceof Edge);
		boolean isCluster = !isEmpty && (o instanceof Cluster.Vertex);
		Action a = null;

		// basic: edit, (cut, copy [not clusters], paste) [not edges]        
		if (!isEmpty) {
			a = app.getAction("edit");
			if (canOperateOn(a, o)) {
				this.add(a);
			}
			a = app.getAction("cut");
			if (!isEdge && canOperateOn(a, o)) {
				this.add(a);
			}
			a = app.getAction("copy");
			if (!isEdge && !isCluster && canOperateOn(a, o)) {
				this.add(a);
			}
			a = app.getAction("paste");
			if (!isEdge && canOperateOn(a, o)) {
				this.add(a);
			}
			a = app.getAction("delete");
			if (canOperateOn(a, o)) {
				this.add(a);
			}
			this.addSeparator();
		}

		// visibility / layout
		if (!isEmpty && !isEdge) {
			this.add(app.getAction("freeze"));
			this.add(app.getAction("set focus"));

			if (!app.getView().getBase().containsVertex(o)) {
				this.add(app.getAction("set visible"));
			}
		}
		this.add(app.getAction("center"));
		if (isCluster) {
			ClusterView v = app.getView();
			ClusteredGraph cg = (ClusteredGraph) v.getBase();

			boolean canCollapse = true;
			// can expand only if self or parent currently visible
			Cluster c = ((Cluster.Vertex) o).getCluster();
			if (cg.getSlice().containsClusterOrAncestor(c)) {
				this.add(app.getAction("expand cluster"));
				canCollapse = false;
			}

			// can collapse only if children visible, but *not* self
			if (((Cluster.Vertex) o).getCluster().getParent() != null) {
				this.add(app.getAction("collapse cluster"));
			}
		} else if (!isEdge) {
			this.add(app.getAction("collapse cluster"));
		}

		this.addSeparator();
	}
}
