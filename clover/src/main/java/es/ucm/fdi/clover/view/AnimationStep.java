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

import java.util.Map;

/**
 * A step in an animation, designed to be used from within an AnimationPlan. The
 * plan should provide all the timing, interrupting, and scheduling. The Step 
 * itself should be easy to extend, because it's the correct way (tm) of 
 * animating things in clover, and animations should be frequent. 
 * 
 * The design is similar to Piccolo's 'activities' - see 
 * http://www.cs.umd.edu/hcil/jazz/
 * We would have used Piccolo, but to write a complete graph overlay in 
 * 'shrimp' style was outside the scope. We just want good graph animations, and
 * dont need them to be too spiffy.
 * 
 * @author mfreire
 */
@SuppressWarnings("rawtypes")
interface AnimationStep {

	/**
	 * do the step. Move/change whatever was to be moved/changed.
	 * Calls with values of 0f and 1f are guaranteed.
	 *
	 * Should set isFinished() to true after 1 i reached, to prevent a call to
	 * terminate().
	 *
	 * @param completion varies between 0 (start) and 1 (end).
	 * @param changeMap the changes to be performed to graph cells
	 */
	void perform(float completion, Map changeMap);

	/**
	 * returns whether this step has finished or not. Steps may finish
	 * before their requested time.
	 *
	 * When this happens, neither perform() nor terminate() will get called.
	 *
	 */
	boolean isFinished();

	/**
	 * will be called to interrupt the step. Should leave things as 
	 * best as it can. Will NOT be called if isFinished() is true.
	 */
	void terminate(Map changeMap);

	/**
	 * queried to find the max time this should run
	 * @return the total time (in ms) this should be run
	 */
	long getDuration();

	/**
	 * queried to find the frequency this should be called at
	 * @return the desired rate of calls to perform, in ms
	 */
	long getRate();

	/**
	 * initialize the action. Called before any other method
	 * is required to give any meaningful results. Should only
	 * be called by the AnimationPlan immediatly before the step
	 * is run.
	 */
	void init();
}
