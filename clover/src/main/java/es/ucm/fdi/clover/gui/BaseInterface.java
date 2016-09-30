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
 * CourseBuilder.java
 *
 * Created on 29 de abril de 2003, 13:18
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.gui;

import es.ucm.fdi.clover.gui.actions.CollapseAction;
import es.ucm.fdi.clover.gui.actions.LayoutLockAction;
import es.ucm.fdi.clover.gui.actions.SetupLayoutAction;
import es.ucm.fdi.clover.gui.actions.GroupInAction;
import es.ucm.fdi.clover.gui.actions.SaveAction;
import es.ucm.fdi.clover.gui.actions.LayoutAction;
import es.ucm.fdi.clover.gui.actions.OpenAction;
import es.ucm.fdi.clover.gui.actions.ClusterLockAction;
import es.ucm.fdi.clover.gui.actions.EditAction;
import es.ucm.fdi.clover.gui.actions.ZoomInAction;
import es.ucm.fdi.clover.gui.actions.SetFocusAction;
import es.ucm.fdi.clover.gui.actions.FreezeAction;
import es.ucm.fdi.clover.gui.actions.SetVisibleAction;
import es.ucm.fdi.clover.gui.actions.CreateViewAction;
import es.ucm.fdi.clover.gui.actions.CenterAction;
import es.ucm.fdi.clover.gui.actions.ExpandAction;
import es.ucm.fdi.clover.gui.actions.RedoNavAction;
import es.ucm.fdi.clover.gui.actions.UndoNavAction;
import es.ucm.fdi.clover.gui.actions.ZoomOutAction;
import es.ucm.fdi.clover.gui.actions.GroupOutAction;
import es.ucm.fdi.clover.gui.actions.SetupFilterAction;
import es.ucm.fdi.clover.gui.CloverSave.ClusterViewFactory;
import es.ucm.fdi.clover.model.BaseGraph;
import es.ucm.fdi.clover.model.Cluster;
import es.ucm.fdi.clover.model.ClusteredGraph;
import es.ucm.fdi.clover.view.BasePopupMenu;
import es.ucm.fdi.clover.view.BaseView;
import es.ucm.fdi.clover.view.ClusterView;
import es.ucm.fdi.clover.view.ViewHelper;
import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.EdgeView;

/**
 * Base interface for clover apps. The interface centralizes actions,
 * contains a common UI, and configures the logger. Clover can be used
 * without showing the interface, but one must be declared if only to 
 * allow default actions to work.
 *
 * @author  mfreire
 */
public abstract class BaseInterface extends JFrame implements
		ClusterViewFactory {

	private static Logger log = Logger.getLogger(BaseInterface.class);
	private final static String CLOVER_PROPS = "clover.properties";
	private final static String LOG_PROPS = "log4j.properties";

	/** used to register/deregister listeners from tabs */
	private int oldTab = -1;

	protected void loadProps() {
		try {
			ClassLoader loader = getClass().getClassLoader();
			props = new Properties();
			InputStream in = loader.getResourceAsStream(CLOVER_PROPS);
			props.load(in);
			in.close();
			PropertyConfigurator.configure(loader.getResource(LOG_PROPS));
		} catch (Exception e) {
			log.error("Unable to load properties: ", e);
			JOptionPane.showMessageDialog(this, "Unable to load properties, "
					+ "please verify that the following files are present: "
					+ CLOVER_PROPS + ", " + LOG_PROPS, "Warning",
					JOptionPane.WARNING_MESSAGE);
		}
	}

	protected void saveProps() {
		Date timeDate = new Date(System.currentTimeMillis());
		try {
			FileOutputStream out = new FileOutputStream(propsFileName);
			props
					.store(out, "-- prop file last changed at " + timeDate
							+ " --");
		} catch (Exception e) {
			log.error("Unable to save properties: ", e);
			JOptionPane.showMessageDialog(this, "Unable to save properties",
					"Warning", JOptionPane.WARNING_MESSAGE);
		}
	}

	/** Creates new form */
	public BaseInterface() {

		loadProps();
		actions = new HashMap();
		initActions();
		setIconImage(getImgIcon("img/clover.png").getImage());
		initComponents();
		initViewToolbar();
		setTitle("Clover2");

		setSize(600, 400);
	}

	/**
	 * The menu shown when you left-click anywhere on a view; to change
	 * default contents, subclass this and add the relevant entries in the
	 * populate() method. Remember to register them in the actions hashmap.
	 */
	public JPopupMenu createMenu(Point p, Object o) {
		return new BasePopupMenu(this, o, p);
	}

	/**
	 * A mouse listener that launches JPopupMenus if you left-click
	 * on the graph
	 */
	public static class AppMouseListener extends MouseAdapter {
		private BaseInterface app;

		public AppMouseListener(BaseInterface app) {
			this.app = app;
		}

		public void mouseClicked(MouseEvent e) {

			// right-click (context menu)
			if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON3) {
				JPopupMenu menu = null;
				if (e.getSource() instanceof BaseView) {
					// try to get an object vertex
					Object o = ViewHelper
							.getVertex(ViewHelper.getVertexCell(e));

					// if that fails, try out an edge vertex
					if (o == null) {
						DefaultGraphCell edgeCell = ViewHelper.getEdgeCell(e);
						o = ViewHelper.getEdge(edgeCell);
					}

					menu = app.createMenu(e.getPoint(), o);
				}
				if (e.getSource() instanceof DTree) {
					DTree t = (DTree) e.getSource();
					TreePath path = t.getPathForLocation(e.getX(), e.getY());
					if (path == null) {
						log.warn("Reloading tree (right-click shortcut)");
						if (t.getModel() instanceof DefaultTreeModel) {
							((DefaultTreeModel) t.getModel()).reload();
						}
						return;
					}
					Object o = path.getLastPathComponent();
					if (o instanceof Cluster) {
						o = ((Cluster) o).getVertex();
					}
					menu = app.createMenu(e.getPoint(), o);
				}
				menu.show((Component) e.getSource(), e.getX(), e.getY());
			}

			// left-click (main action)
			if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
				JPopupMenu menu = null;
				if (e.getSource() instanceof BaseView) {
					menu = app.createMenu(e.getPoint(), ViewHelper
							.getVertex(ViewHelper.getVertexCell(e)));
					((BasePopupMenu) menu).triggerDefaultAction();
				}
				if (e.getSource() instanceof DTree) {
					DTree t = (DTree) e.getSource();
					TreePath path = t.getPathForLocation(e.getX(), e.getY());
					Object o = path.getLastPathComponent();
					if (o instanceof Cluster) {
						o = ((Cluster) o).getVertex();
					}
					menu = app.createMenu(e.getPoint(), o);
					((BasePopupMenu) menu).triggerFirst(new String[] {
							"set visible", "edit" });
				}
			}

			// FIXME - debug only
			if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON2) {
				Collection<ClusterView> views = null;
				try {
					views = CloverSave.restore(((ClusteredGraph) app.getView()
							.getBase()).getBase(), app, new File("/tmp/f"));
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				for (ClusterView cv : views) {
					app.createNewGraphView(cv);
				}
			}
		}
	}

	/**
	 * Initialize all available actions
	 * (should be extended by subclassers with app-specific actions)
	 */
	protected void initActions() {
		Action[] a = new Action[] { new CenterAction(this),
				new CreateViewAction(this), new EditAction(this),
				new SetFocusAction(this), new SetVisibleAction(this),
				new FreezeAction(this), new UndoNavAction(this),
				new RedoNavAction(this), new GroupInAction(this),
				new GroupOutAction(this), new LayoutAction(this),
				new OpenAction(this), new SaveAction(this),
				new SetupFilterAction(this), new ZoomInAction(this),
				new ZoomOutAction(this), new ExpandAction(this),
				new CollapseAction(this), new SetupLayoutAction(this),
				new ClusterLockAction(this), new LayoutLockAction(this) };

		for (int i = 0; i < a.length; i++) {
			actions.put("" + ((Action) a[i]).getValue(Action.NAME), a[i]);
		}
	}

	/**
	 * Do command-line parsing
	 */
	public void parseCommandLine(String[] args) {
		if (args.length > 0) {
			miOpenActionPerformed(args[0]);
		}
	}

	/**
	 * This method is called from within the constructor to
	 * initialize the form.
	 */
	private void initComponents() {
		jtpGraph = new JTabbedPane();
		jtbView = new JToolBar();
		menuBar = new JMenuBar();
		fileMenu = new JMenu();
		miCreate = new JMenuItem();
		miSetupLayout = new JMenuItem();
		miSavePrefs = new JMenuItem();
		miOpen = new JMenuItem();
		miSaveXml = new JMenuItem();
		miSaveGxl = new JMenuItem();
		jSeparator1 = new JSeparator();
		miExit = new JMenuItem();
		miPrint = new JMenuItem();
		editMenu = new JMenu();
		miCut = new JMenuItem();
		miCopy = new JMenuItem();
		miPaste = new JMenuItem();
		helpMenu = new JMenu();
		miContents = new JMenuItem();
		miAbout = new JMenuItem();
		splitPane = new JSplitPane();

		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				exitForm(evt);
			}
		});

		jtpGraph.setAlignmentX(1.0F);
		jtpGraph.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				changeCurrentView();
			}
		});

		clusterTree = new ClusterTree();
		clusterTree.addMouseListener(new AppMouseListener(this));
		splitPane.add(jtpGraph, JSplitPane.LEFT);
		splitPane.add(new JScrollPane(clusterTree), JSplitPane.RIGHT);
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(1);

		getContentPane().add(splitPane, java.awt.BorderLayout.CENTER);

		getContentPane().add(jtbView, java.awt.BorderLayout.SOUTH);

		fileMenu.setText("File");
		miCreate.setText("Create new...");
		miCreate.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				miCreateActionPerformed(null);
			}
		});

		fileMenu.add(miCreate);

		miOpen.setText("Open...");
		miOpen.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				miOpenActionPerformed(null);
			}
		});

		fileMenu.add(miOpen);

		miSaveXml.setText("Export as XML...");
		miSaveXml.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				miSaveActionPerformed(null);
			}
		});

		fileMenu.add(miSaveXml);

		miSaveGxl.setText("Export as GXL...");
		miSaveGxl.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				miSaveGxlActionPerformed(null);
			}
		});

		fileMenu.add(miSaveGxl);

		miPrint.setText("Print");
		miPrint.setToolTipText("Print current graph");
		miPrint.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				miPrintActionPerformed(evt);
			}
		});

		fileMenu.add(miPrint);

		fileMenu.add(jSeparator1);

		miExit.setText("Exit");
		miExit.setToolTipText("Saves config & exits this application");
		miExit.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				miExitActionPerformed(evt);
			}
		});

		fileMenu.add(miExit);

		menuBar.add(fileMenu);

		editMenu.setText("Edit");
		miCut.setText("Cut");
		editMenu.add(miCut);

		miCopy.setText("Copy");
		editMenu.add(miCopy);

		miPaste.setText("Paste");
		editMenu.add(miPaste);

		miSetupLayout.setText("Setup view prefs");
		miSetupLayout.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				getAction("setup layout").actionPerformed(null);
			}
		});

		editMenu.add(miSetupLayout);

		miSavePrefs.setText("Save prefs");
		miSavePrefs.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				saveProps();
			}
		});

		editMenu.add(miSavePrefs);
		menuBar.add(editMenu);

		helpMenu.setText("Help");
		helpMenu.setHorizontalAlignment(SwingConstants.CENTER);
		helpMenu.setHorizontalTextPosition(SwingConstants.CENTER);

		miContents.setText("User guide");
		miContents.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				new HelpBrowser(null, getTitle() + " - Help", helpUrl, true);
			}
		});
		helpMenu.add(miContents);

		miAbout.setText("About");
		miAbout.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				showAboutDialog();
			}
		});
		helpMenu.add(miAbout);

		menuBar.add(helpMenu);

		setJMenuBar(menuBar);

		pack();
	}

	/**
	 * This is called whenever the user selects a different tab to display;
	 * it loads the contents of the cluster trees.
	 */
	protected void changeCurrentView() {
		if (getView() != null) {
			ClusterView view = getView();
			clusterTree.setView(view);
			oldTab = jtpGraph.getSelectedIndex();
		}
	}

	private ImageIcon getImgIcon(String name) {
		ClassLoader loader = getClass().getClassLoader();
		URL iconUrl = loader.getResource(name);
		return new ImageIcon(iconUrl);
	}

	protected void showAboutDialog() {
		String m = "<html><center>"
				+ "<h2>clover v2</h2>"
				+ "A <b>CL</b>uster-<b>O</b>riented <b>V</b>isualization <b>E</b>nvi<b>R</b>onment<br>"
				+ "<br>"
				+ "Manuel Freire Moran (manuel.freire@fdi.ucm.es) <br>"
				+ "</center><html>";

		ImageIcon cloverIcon = getImgIcon("img/clover.png");
		Image img = cloverIcon.getImage().getScaledInstance(64, 64,
				Image.SCALE_FAST);
		cloverIcon = new ImageIcon(img);

		JOptionPane.showMessageDialog(this, m, "About Clover",
				JOptionPane.INFORMATION_MESSAGE, cloverIcon);
	}

	protected void miTest4ActionPerformed(java.awt.event.ActionEvent evt) {
	}

	protected void miTest3ActionPerformed(java.awt.event.ActionEvent evt) {
	}

	protected void miTest2ActionPerformed(java.awt.event.ActionEvent evt) {
	}

	protected void miTest1ActionPerformed(java.awt.event.ActionEvent evt) {
	}

	private void miPrintActionPerformed(java.awt.event.ActionEvent evt) {
		if (getView() != null) {
			PrinterJob printJob = PrinterJob.getPrinterJob();
			printJob.setPrintable(getView());
			PageFormat pageFormat = printJob.pageDialog(new PageFormat());
			if (printJob.printDialog()) {
				try {
					printJob.print();
				} catch (Exception printException) {
					printException.printStackTrace();
				}
			}
		}
	}

	protected void miExitActionPerformed(java.awt.event.ActionEvent evt) {
		saveProps();
		doExit();
	}

	/** Exit the Application */
	protected void exitForm(java.awt.event.WindowEvent evt) {
		doExit();
	}

	protected void doExit() {
		System.exit(0);
	}

	protected void setActionProps(Action a, String iconFileName, String tooltip) {
		ClassLoader loader = getClass().getClassLoader();
		URL iconUrl = loader.getResource(iconFileName);
		ImageIcon icon = new ImageIcon(iconUrl);
		a.putValue(Action.SMALL_ICON, icon);
		a.putValue(Action.SHORT_DESCRIPTION, tooltip);
	}

	/**
	 * Subclass to provide suitable filterAction to execute when "filter" button
	 * in clicked on
	 */
	public void filterAction() {
	}

	/**
	 * Subclass to provide specific behaviours for layout/clustering setup
	 */
	public void layoutPrefsAction() {
		JDialog d = new PrefsDialog(this, props, PrefsDialog.LAYOUT_PAGE);
		d.setVisible(true);
		getView().recalculateDoI();
	}

	private void initViewToolbar() {
		jtbView.add(getAction("create view"));
		jtbView.add(getAction("setup filter"));
		jtbView.addSeparator();
		jtbView.addSeparator();
		jtbView.add(getAction("undo nav"));
		jtbView.add(getAction("redo nav"));
		jtbView.addSeparator();
		jtbView.add(getAction("group in"));
		jtbView.add(getAction("group out"));
		jtbView.addSeparator();
		jtbView.add(getAction("layout"));
		jtbView.add(getAction("setup layout"));
		jtbView.addSeparator();
		//jtbView.add(getAction("lock layout"));
		jtbView.add(getAction("lock clustering"));
		jtbView.addSeparator();
	}

	// save & load is graph-dependent

	public void miCreateActionPerformed(String courseName) {
		JOptionPane.showMessageDialog(this,
				"Course creation is not implemented for this type of courses",
				"Error: Not yet implemented", JOptionPane.WARNING_MESSAGE);
	}

	abstract public void miSaveActionPerformed(String fileName);

	abstract public void miSaveGxlActionPerformed(String fileName);

	abstract public void miOpenActionPerformed(String fileName);

	abstract public ClusterView createNewGraph(BaseGraph baseGraph);

	/**
	 * create a new graph view (long version)
	 */
	public void createNewGraphView(ClusterView view) {
		jtpGraph.addTab("View #" + (jtpGraph.getTabCount() + 1),
				new JScrollPane(view));
		view.addMouseListener(new AppMouseListener(this));
		jtpGraph.setSelectedIndex(jtpGraph.getTabCount() - 1);
	}

	/**
	 * create a new graph view (short version)
	 */
	public void createNewGraphView() {
		createNewGraphView(createNewGraph(baseGraph));
	}

	/**
	 * gets the currently displayed view
	 */
	public ClusterView getView() {
		int selected = jtpGraph.getSelectedIndex();
		if (selected == -1)
			return null;
		JScrollPane scroll = (JScrollPane) jtpGraph.getComponentAt(selected);
		return (ClusterView) scroll.getViewport().getComponent(0);
	}

	/**
	 * gets the currently displayed tree
	 * (some subclasses like to display several)
	 */
	public DTree getTree() {
		return clusterTree;
	}

	/**
	 * returns a list of all current views
	 */
	public ArrayList<ClusterView> getViews() {
		ArrayList<ClusterView> al = new ArrayList<ClusterView>();
		for (int i = 0; i < jtpGraph.getComponentCount(); i++) {
			JScrollPane jsp = (JScrollPane) jtpGraph.getComponentAt(i);
			al.add((ClusterView) jsp.getViewport().getView());
		}
		return al;
	}

	/**
	 * Provides access to the action registry.
	 * @return an action with this name, or null if none found.
	 */
	public Action getAction(String name) {
		return (Action) actions.get(name);
	}

	/**
	 * Removes all views
	 */
	public void removeAllViews() {
		jtpGraph.removeAll();
	}

	/**
	 * Provides access to the app's properties
	 */
	public Properties getProperties() {
		return props;
	}

	// properties file
	protected String propsFileName;
	protected Properties props;

	// My variables (not GUI-related)
	protected BaseGraph baseGraph;
	protected String helpUrl;

	// Storage for actions
	public HashMap<String, Action> actions;

	protected JSplitPane splitPane;
	protected ClusterTree clusterTree;
	protected JToolBar jtbView;
	protected JMenuItem miCreate;
	protected JMenuItem miPaste;
	protected JMenuItem miSavePrefs;
	protected JMenu fileMenu;
	protected JMenuItem miAbout;
	protected JMenuItem miExit;
	protected JMenuItem miSaveXml;
	protected JMenuItem miSaveGxl;
	protected JMenuBar menuBar;
	protected JMenuItem miPrint;
	protected JTabbedPane jtpGraph;
	protected JMenuItem miOpen;
	protected JMenuItem miContents;
	protected JMenuItem miCopy;
	protected JMenuItem miSetupLayout;
	protected JMenu editMenu;
	protected JSeparator jSeparator1;
	protected JMenuItem miCut;
	protected JMenu helpMenu;
	protected JMenu debugMenu;
	protected JMenuItem miTest1;
	protected JMenuItem miTest2;
	protected JMenuItem miTest3;
	protected JMenuItem miTest4;
}
