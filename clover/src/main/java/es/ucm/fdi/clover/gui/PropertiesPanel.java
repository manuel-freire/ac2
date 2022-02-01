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
package es.ucm.fdi.clover.gui;

import java.util.*;

/**
 *
 * @author  manu
 */
public class PropertiesPanel extends JFieldPanel {

	private Properties props;
	private String[] propNames;

	/** Creates a new instance of PropsFieldPanel */
	public PropertiesPanel(Properties props, String[] propNames,
			String[] niceNames) {
		super(null, niceNames, null, null, null);
		this.propNames = propNames;
		this.props = props;
		for (int i = 0; i < getFieldCount(); i++) {
			setValue(i, props.getProperty(propNames[i]));
		}
	}

	public void keepValues() {
		for (int i = 0; i < getFieldCount(); i++) {
			System.out.println("setting " + propNames[i] + " to value "
					+ getValue(i));
			props.put(propNames[i], getValue(i));
		}
	}

	public void restoreValues() {
		for (int i = 0; i < getFieldCount(); i++) {
			setValue(i, props.getProperty(propNames[i]));
		}
	}
}
