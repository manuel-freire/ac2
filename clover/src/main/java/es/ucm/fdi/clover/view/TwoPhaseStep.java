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
 * InterpolatedMovementStep.java
 *
 * Created on May 14, 2006, 9:27 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.view;

import java.util.Map;

/**
 * This is a bait-and-switch step. First time its 'init' is called, it does
 * doInitialAction - then, it proceed just as its delegate action.
 *
 * @author mfreire
 */
public abstract class TwoPhaseStep implements AnimationStep {

	private static AnimationStep dummyStep = new Dummy();
	private AnimationStep delegate = dummyStep;

	/**
	 * Should only be called once. Its okay to call it from within the
	 * init method.
	 */
	public void setDelegate(AnimationStep delegate) {
		this.delegate = delegate;
		delegate.init();
		//        System.err.println("Delegate initialized to be "+delegate.getClass().getSimpleName()+" during "+delegate.getDuration());
	}

	public void perform(float completion, Map changeMap) {
		delegate.perform(completion, changeMap);
	}

	public boolean isFinished() {
		return delegate.isFinished();
	}

	public void terminate(Map changeMap) {
		delegate.terminate(changeMap);
	}

	public long getDuration() {
		return delegate.getDuration();
	}

	public long getRate() {
		return delegate.getRate();
	}

	public abstract void init();

	/**
	 * Just so nothing breaks before setDelegate is called
	 */
	private static class Dummy implements AnimationStep {
		public void perform(float completion, Map changeMap) {
		}

		public boolean isFinished() {
			return true;
		}

		public void terminate(Map changeMap) {
		}

		public long getDuration() {
			return 0;
		}

		public long getRate() {
			return 0;
		}

		public void init() {
		}
	}
}
