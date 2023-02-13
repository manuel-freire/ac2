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
package es.ucm.fdi.ac.walkers;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.util.*;

public class RuleWalker implements ParseTreeListener {

	static final Logger log = LogManager.getLogger(RuleWalker.class);

	private Set<String> retainedRules;
	private ReNode root;
	private ReNode parent;
	private Map<String, Integer> ids;

	public void enterEveryRule(ParserRuleContext ctx) {
		String ruleName = ctx.getClass().getSimpleName();
		int start = ((CommonToken) ctx.start).getStartIndex();
		int size = ((CommonToken) ctx.stop).getStopIndex() - start + 1;

		boolean added = false;
		if (ctx.getParent() == null) {
			// initial rule
			root = new ReNode(ctx);
			parent = root;
			added = true;
		} else if (retainedRules.contains(ruleName)) {
			// in list of good rules
			ReNode n = new ReNode(ctx);
			n.parent = parent;
			parent.children.add(n);
			parent = n;
			added = true;
			if (ruleName.toLowerCase().contains("identifier")) {
				String id = ctx.getText();
				ids.put(id, Integer.valueOf(ids.getOrDefault(id, 0) + 1));
			}
		}

		if (added)
			log.debug("{} - {} / {} from {}, size {}", added, ruleName, ctx
					.getText(), start, size);
	}

	public void exitEveryRule(ParserRuleContext ctx) {
		String ruleName = ctx.getClass().getSimpleName();
		if (retainedRules.contains(ruleName)) {
			parent = parent.parent;
		}
	}

	public void visitErrorNode(ErrorNode node) {
	}

	public void visitTerminal(TerminalNode node) {
	}

	public RuleWalker(String[] retainedRules) {
		this.retainedRules = Set.of(retainedRules);
	}

	public ReNode getRoot() {
		return root;
	}
}
