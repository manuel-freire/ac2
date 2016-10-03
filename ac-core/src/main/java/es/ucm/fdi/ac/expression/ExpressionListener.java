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
 * ExpressionClient.java
 *
 * Created on September 16, 2006, 1:23 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package es.ucm.fdi.ac.expression;

/**
 * An interface that something interested in expression evaluation should implement.
 * The methods will be called only when the expression is tested. The result
 * should be to take the expression and evaluate it and show whatever is considered
 * interesting in an interesting way.
 *
 * @author mfreire
 */
public interface ExpressionListener {

	/**
	 * Graphically display the effects of this expression
	 */
	void expressionChanged(Expression e, boolean test);
}
