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

import es.ucm.fdi.clover.gui.CloverSave;
import es.ucm.fdi.clover.model.ViewGraph;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;

import javax.swing.ToolTipManager;
import org.jgraph.JGraph;

import es.ucm.fdi.clover.model.BaseGraph;
import es.ucm.fdi.clover.model.Edge;
import org.jdom2.Element;
import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.GraphCell;
import org.jgraph.graph.GraphConstants;

/**
 * This is what you get to see and interact with in Clover. All UI stuff is to
 * be found here. This is the base for the 'component' hierarchy, so you only
 * get the basic stuff at this level. No clusters, therefore no aura.
 *
 * @author mfreire
 */
public class BaseView extends JGraph implements Printable {

	public static final String scaleProperty = "scaleProperty";
	public static final String zoomProperty = "zoomProperty";

	/** what we are currently seeing (should be synced via animator with base) */
	protected ViewGraph viewGraph;

	/** the Animator (in charge of syncing viewGraph and actual base) */
	protected Animator animator;

	/** the AnimationPlan currently under execution, if any */
	private AnimationPlan currentPlan;

	/** allows a 'layout zoom' to be used; propagated on all view<->layout operations */
	private double layoutZoom;

	/** used to avoid saving the positions after the layout-zoom */
	private double oldLayoutZoom = -1;

	/**
	 * Creates a new instance of BaseView
	 */
	public BaseView() {
		this(new ViewGraph(new BaseGraph()));
	}

	public BaseView(ViewGraph viewGraph) {
		super(viewGraph.getModelAdapter());
		layoutZoom = 1.0;

		this.viewGraph = viewGraph;
		setBase(viewGraph.getBase());

		this.setAntiAliased(true);
		this.setSelectionEnabled(true);
		this.setEditable(false);
		this.setDisconnectable(false);
		this.setEdgeLabelsMovable(false);
		this.setPortsVisible(false);
		this.setMoveable(true);
		this.setBendable(false);

		ToolTipManager.sharedInstance().registerComponent(this);
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
		ToolTipManager.sharedInstance().setDismissDelay(1000 * 30);

		if (getParent() instanceof JViewport) {
			((JScrollPane) getParent().getParent())
					.setWheelScrollingEnabled(false);
		}

		this.getInputMap().put(KeyStroke.getKeyStroke('a'), "debug-lcache");
		this.getActionMap().put("debug-lcache", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				animator.dumpLayoutCache(getBase());
			}
		});

		this.getInputMap().put(KeyStroke.getKeyStroke('p'), "debug-plan");
		this.getActionMap().put("debug-plan", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				AnimationPlan p = getCurrentPlan();
				if (p != null) {
					System.err.println("Current plan: " + p.getDescription());
					System.err.println("isRunning: " + p.isRunning());
				} else {
					System.err.println("No plan.");
				}
			}
		});

		this.getInputMap().put(KeyStroke.getKeyStroke('s'), "save-lcache");
		this.getActionMap().put("save-lcache", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				ArrayList<ClusterView> al = new ArrayList<ClusterView>();
				al.add((ClusterView) e.getSource());
				try {
					CloverSave.save(al, new File("/tmp/f"));
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		});

		// allows very simple zooming
		this.addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				int i = e.getWheelRotation();

				double delta = 0.05;
				double scale = getScale();
				double next;
				if (scale < 1.0 || (layoutZoom == 1.0 && i > 0)) {
					// scale-unzoom
					next = (i < 0) ? Math.min(scale + delta, 1.0) : Math.max(
							delta, scale - delta);

					if (i < 0)
						setRelativeCenter(e.getPoint(), next / scale);

					setScale(next);
					firePropertyChange(scaleProperty, scale, next);
				} else {
					// do the 'position zoom' thing here
					next = (i < 0) ? Math.min(layoutZoom * (1.0 + delta), 4.0)
							: Math.max(layoutZoom * (1.0 - delta), 1.0);

					if (i < 0)
						setRelativeCenter(e.getPoint(), next / layoutZoom);

					setLayoutZoom(next);
					firePropertyChange(zoomProperty, scale, next);
				}

				// System.err.println("Layout-zoom: "+layoutZoom);
				// System.err.println("Scale: "+getScale());               
			}
		});

		this.getModel().addGraphModelListener(new GraphModelListener() {
			public void graphChanged(GraphModelEvent e) {
				// zoom changes are undistinguishable from user changes - distinguish them
				if (oldLayoutZoom != layoutZoom) {
					oldLayoutZoom = layoutZoom;
					return;
				}

				// only resyncs if there is no plan executing
				if (currentPlan == null) {
					animator.resyncFromEvent(e);
				}
			}
		});
	}

	public BaseGraph getBase() {
		return viewGraph.getBase();
	}

	public ViewGraph getViewGraph() {
		return viewGraph;
	}

	public void setRelativeCenter(Point2D p, double zoomDiff) {
		JViewport jvp = (JViewport) getParent();
		Point2D tl = jvp.getViewPosition();
		Point next = new Point();
		p.setLocation(Math.min(getWidth(), p.getX()), Math.min(getHeight(), p
				.getY()));
		next.setLocation(tl.getX() + (p.getX() - tl.getX())
				* (1 - (1 / zoomDiff)), tl.getY() + (p.getY() - tl.getY())
				* (1 - (1 / zoomDiff)));
		next.setLocation(Math.max(0, next.getX()), Math.max(0, next.getY()));

		Dimension nd = jvp.getViewSize();
		// next dims
		nd.setSize(nd.getWidth() / zoomDiff, nd.getHeight() / zoomDiff);
		// required dims
		Dimension rd = getPreferredSize();

		if (!next.equals(tl)
				&& (nd.getHeight() < rd.getHeight() || nd.getWidth() < rd
						.getWidth())) {
			//System.err.println("zoom-recenter: \nreq\t"+rd+"\nnxt\t"+nd);
			jvp.setViewPosition(next);
		}
		//        else {
		//            System.err.println("NO-recenter: \nreq\t"+rd+"\nnxt\t"+nd);
		//        }
	}

	/**
	 * If the parent is a viewport, shifts the viewport's window so that
	 * the selected 'view coordinates' appear centered; note that corrections
	 * for scale must be made since 'view coords' != 'view rect coords'
	 *
	 * FIXME: also, if any part of the results would be offscreen, 
	 * errors will crop up; stupid java libraries!.
	 */
	public void setCenter(Point2D desiredCenter) {
		JViewport jvp = (JViewport) getParent();

		Dimension nd = jvp.getViewSize();
		Dimension rd = getPreferredSize();
		if (nd.width > rd.width && nd.height > rd.height) {
			return;
		}

		// current view position: portion of the graph being represented
		Dimension viewSize = jvp.getExtentSize();
		Point2D currentCenter = jvp.getViewPosition();
		currentCenter.setLocation(currentCenter.getX() + viewSize.getWidth()
				/ 2, currentCenter.getY() + viewSize.getHeight() / 2);

		double dx = -currentCenter.getX() + desiredCenter.getX();
		double dy = -currentCenter.getY() + desiredCenter.getY();

		// calculate desired rectangle
		Point p = new Point(jvp.getViewPosition());
		p.setLocation(Math.max(0, p.getX() + dx), Math.max(0, p.getY() + dy));
		// scroll
		jvp.setViewPosition(p);
	}

	public void setBase(BaseGraph base) {
		if (viewGraph.getBase() != base) {
			viewGraph.setBase(base);
		}
		setModel(viewGraph.getModelAdapter());
		if (animator == null) {
			animator = new Animator(this);
		} else {
			animator.setView(this);
		}
	}

	/**
	 * Returns the label for a given vertex or edge cell
	 */
	public String convertValueToString(Object cell) {
		String label = null;

		if (cell instanceof CellView) {
			cell = ((CellView) cell).getCell();
		}

		if (cell instanceof GraphCell && cell != null) {
			Map attribs = ((GraphCell) cell).getAttributes();
			label = (String) attribs.get(ViewGraph.LABEL);
			if (label == null) {
				Object o = GraphConstants.getValue(attribs);
				label = (cell instanceof DefaultEdge) ? viewGraph
						.getEdgeLabel((Edge) o) : viewGraph.getVertexLabel(o);
			}
			if (label != null && label.length() == 0) {
				return null;
			}
		}

		return (label != null) ? label : super.convertValueToString(cell);
	}

	/**
	 * Returns a tool-tip for the current location, which can be over
	 * either a vertex or an edge cell.
	 */
	public String getToolTipText(MouseEvent event) {
		Object cell = getFirstCellForLocation(event.getX(), event.getY());
		String tooltip = null;

		if (cell instanceof GraphCell && cell != null) {
			Map attribs = ((GraphCell) cell).getAttributes();
			tooltip = (String) attribs.get(ViewGraph.TOOLTIP);
			if (tooltip == null) {
				Object o = GraphConstants.getValue(attribs);
				tooltip = (cell instanceof DefaultEdge) ? viewGraph
						.getEdgeToolTip((Edge) o) : viewGraph
						.getVertexToolTip(o);
			}
			if (tooltip != null && tooltip.length() == 0) {
				tooltip = null;
			}
		}

		//        else {
		//            tooltip = ""+event.getPoint().getX()+","+event.getPoint().getY();
		//        }

		return tooltip;
	}

	public Animator getAnimator() {
		return animator;
	}

	public void setAnimator(Animator animator) {
		if (this.animator != null) {
			getBase().removeStructureChangeListener(this.animator);
		}
		this.animator = animator;
		animator.setView(this);
	}

	public AnimationPlan getCurrentPlan() {
		return currentPlan;
	}

	public void setCurrentPlan(AnimationPlan currentPlan) {
		this.currentPlan = currentPlan;
	}

	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
			throws PrinterException {
		throw new UnsupportedOperationException("Printing not yet supported");
	}

	/**
	 * Behaviour for large scales is to place nodes further apart
	 */
	public void setLayoutZoom(double val) {
		animator.getManager().setNodes(this);
		this.layoutZoom = val;
		// FIXME: animate?
		animator.getManager().applyChanges(this);
	}

	public double getLayoutZoom() {
		return layoutZoom;
	}

	public void save(Element e) {
		e.setAttribute("zoom", "" + (layoutZoom > 1 ? layoutZoom : getScale()));

		String topCorner = "" + getVisibleRect().getX() + ","
				+ getVisibleRect().getY();
		e.setAttribute("topCorner", topCorner);
	}

	public void restore(Element e) {
		float f = Float.parseFloat(e.getAttributeValue("zoom"));
		if (f > 1)
			setLayoutZoom(f);
		else
			setScale(f);

		String[] p = e.getAttributeValue("topCorner").split(",");

		//      FIXME: cannot be called unless parent has been set...        
		//        ((JViewport)getParent()).setViewPosition(
		//                new Point((int)Float.parseFloat(p[0]), (int)Float.parseFloat(p[1])));        
	}
}
