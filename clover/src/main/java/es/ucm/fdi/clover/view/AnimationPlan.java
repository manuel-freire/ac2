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
 * AnimationPlan.java
 *
 * Created on May 14, 2006, 12:10 PM
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.swing.Timer;

/**
 * Animation plans are in charge of calling, when appropiate, the animation
 * steps that compose them. The sequence, frequency and duration of steps are
 * the core of the plan's responsibility. Also, at the end of each step,
 * the plan should commit results into the current graph.
 *
 * @author mfreire
 */
public class AnimationPlan {

	private Log log = LogFactory.getLog(AnimationPlan.class);

	// layout priority for a real change: high
	public static final int STRUCTURE_PRIORITY = 10;
	// layout priority for a relayout request: med
	public static final int RELAYOUT_PRIORITY = 2;
	// layout priority for a wimpy rollover: low
	public static final int ROLLOVER_PRIORITY = 1;

	/** executed sequentially, include one or more 'steps' */
	private ArrayList<ArrayList<AnimationStep>> moves;

	/** if true, plan is no longer running */
	private boolean isTerminated;

	/** the view that the plan is to be applied to */
	private BaseView view;

	/** the runner in charge of executing the plan; only one runner per view */
	private PlanRunner runner;

	/** the plan's priority. Higher-priority plans terminate lower-priority ones */
	private int priority;

	/** the slowdown factor; use > 1 to view slow-motion animations (as in, to debug them) */
	private int slowdownFactor = 1;

	/**
	 * Creates a new instance of AnimationPlan
	 */
	public AnimationPlan(BaseView view, int priority) {
		setView(view);
		setPriority(priority);
		moves = new ArrayList<ArrayList<AnimationStep>>();
	}

	/**
	 * Adds a new step to the plan. The step will start after
	 * everything else in the plan is finished.
	 */
	public void addStep(AnimationStep step) {
		ArrayList<AnimationStep> move = new ArrayList<AnimationStep>();
		move.add(step);
		moves.add(move);
	}

	/**
	 * Adds a concurrent step. The step will start at the same
	 * time that the current last step starts. If there is no
	 * 'current last step', this is equivalent to addStep.
	 */
	public void mergeStep(AnimationStep step) {
		if (moves.isEmpty()) {
			addStep(step);
			return;
		}
		ArrayList<AnimationStep> move = moves.get(moves.size() - 1);
		move.add(step);
	}

	/**
	 * @return true if this is the currently executing plan, false otherwise.
	 */
	public boolean isRunning() {
		return view.getCurrentPlan() == this;
	}

	/**
	 * @return the moves that make up this animation plan
	 */
	public ArrayList<ArrayList<AnimationStep>> getMoves() {
		return moves;
	}

	/**
	 * @return a string description of the plan, with all its steps
	 */
	public String getDescription() {
		StringBuffer sb = new StringBuffer();
		int i = 0;
		for (ArrayList<AnimationStep> m : moves) {
			i++;
			sb.append("\n  Move " + i + ": (" + m.size() + " steps): ");
			for (AnimationStep s : m) {
				sb.append(s.getClass().getSimpleName() + " of "
						+ s.getDuration() + " every " + s.getRate() + " + ");
			}
			sb.delete(sb.lastIndexOf(" + "), sb.length());
		}
		return sb.toString();
	}

	/**
	 * Resolves conflicts with other currently-executing plans, and starts to 
	 * execute this one instead. This method is thread-safe, ensuring that 
	 * if several plans try to execute at once, only one will be executing
	 * in any given moment; later plans will be added to the end of the current
	 * one.
	 */
	public void run() {
		if (runner == null) {
			runner = new PlanRunner();
		}

		if (moves.isEmpty()) {
			if (log.isDebugEnabled()) {
				Thread.dumpStack();
			}
			log.warn("Tried to run an empty plan...");
			return;
		}

		// acquire control of animation: other plan will not 'tick' during this code.
		synchronized (view) {
			AnimationPlan currentPlan = view.getCurrentPlan();
			if (currentPlan != null && currentPlan != this) {
				log.debug("RUNNING DIFFERENT PLAN! - was "
						+ currentPlan.getDescription());
				if (currentPlan.priority > priority) {
					log.warn("Refusing to run plan: priority " + priority
							+ " is lower than current (" + currentPlan + ")");
					return;
				}
				if (currentPlan.priority < priority) {
					log.info("Switching plan to " + getDescription());
					currentPlan.terminate();
					log.debug("SWITCHED TO:\n" + getDescription());
				} else {
					// same priority: keep up the good work
					log.info("Appending same-priority plan... "
							+ getDescription());
					for (ArrayList<AnimationStep> move : getMoves()) {
						currentPlan.addStep(move.get(0));
						for (int i = 1; i < move.size(); i++) {
							currentPlan.mergeStep(move.get(i));
						}
					}
					return;
				}
			}

			if (runner.isRunning()) {
				log.debug("CONTINUING WITH PLAN (re-run requested)" + "\n"
						+ getDescription());
			} else {
				log
						.debug("STARTING NEW PLAN " + this + "\n"
								+ getDescription());
				runner.run();
			}
			view.setCurrentPlan(this);
		}
	}

	/**
	 * This inner class actually executes the current plan. The idea is
	 * to wrap it up with a swing timer
	 */
	private class PlanRunner implements Runnable, ActionListener {
		private long startNanos;
		private long totalNanos;
		private long elapsedNanos;
		private long prevelapsedNanos;
		private long moveRate;
		private float completion;
		private boolean isFirstPass;
		private boolean isRunning;
		private Timer t;

		// move index (in list of moves)
		private int moveIndex;
		// steps in current move
		private ArrayList<AnimationStep> currentMove;

		public boolean isRunning() {
			return isRunning;
		}

		public void finish() {
			isRunning = false;
			if (t != null)
				t.stop();
			t = null;
			view.setCurrentPlan(null);
		}

		public void run() {
			currentMove = null;
			isRunning = true;
			log.debug(">> STARTING to run plan " + this + ": "
					+ getDescription());
			moveIndex = 0;
			next();
		}

		public void next() {

			// check for end
			if (moveIndex == moves.size() || !isRunning) {
				finish();
				return;
			}

			// if possible, go to next move
			currentMove = moves.get(moveIndex++);

			// prepare move
			totalNanos = 0;
			moveRate = 0;
			int stepNum = 0;
			for (AnimationStep s : currentMove) {
				try {
					s.init();
				} catch (Exception e) {
					log.error("Error initializing " + "step " + stepNum
							+ " of " + "move " + (moveIndex - 1) + " of "
							+ "plan " + getDescription(), e);
					log.error("Skipping whole step... ");
					finish();
					return;
				}
				log.debug("Just initialized concurrent step # " + stepNum
						+ ", a " + s.getClass().getSimpleName() + " during "
						+ s.getDuration() + " ms");
				moveRate = Math.max(moveRate, s.getRate());
				totalNanos = Math.max(totalNanos, s.getDuration());
				stepNum++;
			}
			totalNanos *= 1000000; // duration came in ms, we need ns

			startNanos = System.nanoTime();
			isFirstPass = true;

			if (log.isDebugEnabled()) {
				StringBuffer sb = new StringBuffer();
				for (AnimationStep s : currentMove) {
					sb.append("\n\t" + s + " (" + s.getClass().getName() + ")");
				}
				log.debug(">> Starting with NEW MOVE ( #" + (moveIndex - 1)
						+ totalNanos / 1000000 + " ms, with " + moveRate
						+ " ms refresh)" + sb.toString());
			}

			totalNanos *= slowdownFactor;

			// perform move within timer
			t = new javax.swing.Timer((int) moveRate * slowdownFactor, this);
			t.setInitialDelay(0);
			t.setRepeats(true);
			t.start();
		}

		/**
		 * This is called within the event thread to actually step things
		 */
		public void actionPerformed(ActionEvent evt) {

			// will not execute while another plan is trying to schedule self
			synchronized (view) {

				if (t == null) {
					log.warn(">> Plan was aborted, late timer event discarded");
					return;
				}

				HashMap map = new HashMap();

				long currentTime = System.nanoTime();

				prevelapsedNanos = elapsedNanos;
				elapsedNanos = currentTime - startNanos;

				if (isFirstPass) {
					elapsedNanos = 0;
					isFirstPass = false;
				}

				boolean allFinished = true;
				if (elapsedNanos < totalNanos) {
					completion = elapsedNanos / (float) totalNanos;
					if (log.isDebugEnabled()) {
						String s = (new java.text.DecimalFormat("0.000"))
								.format(completion * 100);
						log.debug(">> Next pass (" + elapsedNanos / 1000000
								+ " ms, " + s + "%)");
					}
					for (AnimationStep s : currentMove) {
						s.perform(completion, map);
						if (!s.isFinished()) {
							allFinished = false;
						}
						if (isTerminated) {
							terminate();
							break;
						}
					}
					commit(map);
				}

				if (elapsedNanos > totalNanos || allFinished) {
					t.stop();

					// clean up move
					map.clear();
					log.debug(">> " + this + ": Last pass");
					for (AnimationStep s : currentMove) {
						s.perform(1f, map);
					}
					commit(map);

					next();
				}
			}
		}
	}

	/**
	 * Should be called only from within event thread
	 */
	public void commit(HashMap map) {
		try {
			if (!map.isEmpty()) {
				view.getGraphLayoutCache().edit(map);
			}
		} catch (Exception e) {
			log.warn("Exception during commit, aborting", e);
			terminate();
		}
	}

	/**
	 * Should be called only from within event thread, as it
	 * uses commit
	 */
	public void terminate() {
		runner.finish();
		HashMap map = new HashMap();
		for (ArrayList<AnimationStep> m : moves) {
			try {
				for (AnimationStep s : m) {
					if (!s.isFinished())
						s.terminate(map);
				}
				if (!map.isEmpty()) {
					view.getGraphLayoutCache().edit(map);
				}
			} catch (Exception e) {
				log.warn(
						"Exception while terminating plan " + getDescription(),
						e);
				break;
			}
		}
	}

	public BaseView getView() {
		return view;
	}

	public void setView(BaseView view) {
		this.view = view;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}
}
