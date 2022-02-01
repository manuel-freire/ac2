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
package es.ucm.fdi.ac.expression;

import java.util.ArrayList;

/**
 * An expression is something, probably with operands, that can be used to 
 * build bigger expressions. This is used by the extraction interface
 * to describe the expressions that are being built.
 *
 * @author mfreire
 */
public interface Expression {

	/**
	 * Returns a list of headers (to be used in a combobox)
	 */
	ArrayList<String> getHeaders();

	/**
	 * Set the header of the expression; this should change the expression
	 * to a different type, according to the header
	 */
	void setHeader(String header);

	/**
	 * Returns the current header
	 */
	String getHeader();

	/**
	 * Set the body of the expression
	 */
	void setBody(String string);

	/**
	 * Returns the 'meat' of the expression, excluding the header. Typically,
	 * headers will be chosen from a combobox, and contents inserted in a textfield
	 */
	String getBody();

	/**
	 * Sets the parent expression (when this changes, the parent will probably
	 * need to be notified as well).
	 */
	void setParentExpression(CompositeExpression e);
}
