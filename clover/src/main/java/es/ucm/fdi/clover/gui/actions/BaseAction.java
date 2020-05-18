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
 * BaseAction.java
 *
 * Created on May 2, 2004, 12:13 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.gui.actions;

import es.ucm.fdi.clover.view.BasePopupMenu;
import es.ucm.fdi.clover.view.ClusterView;
import es.ucm.fdi.clover.gui.BaseInterface;
import es.ucm.fdi.clover.view.BaseView;
import es.ucm.fdi.clover.view.ViewHelper;

import java.awt.Point;
import java.net.URL;
import java.util.HashMap;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jgraph.graph.DefaultGraphCell;

/**
 * Common superclass for all actions
 *
 * @author  mfreire
 */
public abstract class BaseAction extends AbstractAction {

	private Log log = LogFactory.getLog(BaseAction.class);

	/** HashMap with image cache */
	private static HashMap iconMap = new HashMap();

	/** The app that created this action */
	protected BaseInterface app;

	/** 
	 * Loads icons, using the image cache when available
	 */
	protected Icon getIcon(String name) {
		log.info("Trying to load " + name);
		if (!iconMap.containsKey(name)) {
			ClassLoader loader = this.getClass().getClassLoader();
			URL iconUrl = loader.getResource(name);
			ImageIcon icon = new ImageIcon(iconUrl);
			iconMap.put(name, icon);
		}
		return (Icon) iconMap.get(name);
	}

	/**
	 * Creates a new instance of BaseAction
	 */
	public BaseAction(BaseInterface app, String name, String iconName) {
		this(app, name, iconName, "");
	}

	/**
	 * Creates a new instance of BaseAction
	 */
	public BaseAction(BaseInterface app, String name, String iconName,
			String tooltip) {
		super(name);
		//System.err.println("loading icon for "+iconName);
		putValue(Action.SMALL_ICON, getIcon(iconName));
		putValue(Action.SHORT_DESCRIPTION, tooltip);
		this.app = app;
	}

	/**
	 * @return the CvGraph this action was triggered on
	 */
	protected ClusterView getView(java.awt.event.ActionEvent e) {
		if (!(e.getSource() instanceof JComponent))
			return app.getView();
		JComponent src = (JComponent) e.getSource();
		return app.getView();
	}

	/**
	 * @return the BaseInterface we are in
	 */
	protected BaseInterface getApp(java.awt.event.ActionEvent e) {
		return app;
	}

	/**
	 * @return the operand for this action (no operand         Sy
	    
	= null)
	 */
	protected Object getOperand(java.awt.event.ActionEvent e) {

		// direct call without displaying popup
		if (e.getSource() instanceof BasePopupMenu) {
			return ((BasePopupMenu) e.getSource()).getObject();
		}

		// direct call via keystroke from graph or tree; use selection
		if (e.getSource() instanceof BaseView) {
			BaseView v = ((BaseView) e.getSource());
			if (v.getSelectionCount() > 0) {
				DefaultGraphCell o = (DefaultGraphCell) v.getSelectionCell();
				return (ViewHelper.isEdge(o)) ? ViewHelper.getEdge(o)
						: ViewHelper.getVertex(o);
			}
			return null;
		}

		// FIXME: same should work for tree too...

		// normal call, via popup
		if (!(e.getSource() instanceof JComponent))
			return null;
		JComponent src = (JComponent) e.getSource();
		if (src.getParent() instanceof BasePopupMenu) {
			return ((BasePopupMenu) src.getParent()).getObject();
		} else {
			return null;
		}
	}

	/**
	 * @return the graph point relevant to this action
	 */
	protected Point getPoint(java.awt.event.ActionEvent e) {
		Object src = e.getSource();
		if (src instanceof JComponent
				&& ((JComponent) src).getParent() instanceof BasePopupMenu) {
			return ((BasePopupMenu) ((JComponent) src).getParent()).getPoint();
		} else if (src instanceof Point) {
			return (Point) src;
		} else {
			return null;
		}
	}
}
