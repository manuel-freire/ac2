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
 * MainGui.java
 *
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 * Contributors: - 
 * Changelog: 
 *     18-Apr-2006: first version (mfreire)
 */

package es.ucm.fdi.ac.gui;

import es.ucm.fdi.ac.Analysis;
import es.ucm.fdi.ac.SourceSet;

// tests
import es.ucm.fdi.ac.parser.AntlrTokenizerFactory;
import es.ucm.fdi.ac.test.NCDTest;
import es.ucm.fdi.ac.test.RawNCDTest;
import es.ucm.fdi.ac.test.Test;
import es.ucm.fdi.ac.test.TokenCountTest;
import es.ucm.fdi.ac.test.TokenizingTest;
import es.ucm.fdi.ac.test.VarianceSubtest;
import es.ucm.fdi.util.I18N;
import es.ucm.fdi.util.archive.ArchiveFormat;
import es.ucm.fdi.util.archive.Bzip2Format;
import es.ucm.fdi.util.FileUtils;
import es.ucm.fdi.util.archive.GzipFormat;
import es.ucm.fdi.util.archive.ZipFormat;

import java.awt.Desktop;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import static es.ucm.fdi.util.I18N.m;

import org.apache.log4j.Logger;

/**
 * A graphical interface for AC. Also provides a simple 
 * command-line interface to avoid a few steps.
 *
 * @author  mfreire
 */
public class MainGui extends javax.swing.JFrame {

	private static final Logger log = Logger.getLogger(MainGui.class);

	private Analysis ac;
	private GraphicalAnalysis analysis;

	private File saveFile;
	private File sourcesDir;

	private Test currentTest;
	private HelpBrowser helpBrowser;
	private HelpBrowser aboutBrowser;
	private JTabbedPane jtp;

	private HashMap<String, TestResultsDialog> testResults;
	private HashMap<Test, JCheckBoxMenuItem> testTypeCheckBoxes;
	private LinkedHashMap<String, Test> testNameLookup;

	private HashMap<String, String> testHelpMap = null;

	/** Creates new form MainGui */
	public MainGui() {
		initComponents();
		setTitle(m("Test.WindowTitle", ACVersion.getVersion()));
		pack();

		testResults = new HashMap<>();
		testTypeCheckBoxes = new HashMap<>();
		testNameLookup = new LinkedHashMap<>();

		ac = new Analysis();
		ac.setTokenizerFactory(new AntlrTokenizerFactory());
		saveFile = null;

		ArrayList<String> tks = new ArrayList<>();
		Test t;

		t = new NCDTest(new ZipFormat());
		addTestType("Zip NCD Test", t);
		tks.add(t.getTestKey());

		VarianceSubtest vt = new VarianceSubtest(t.getTestKey(), 0.5);
		addTestType("Variance Subtest on Zip NCD Test, 0.5", vt);
		tks.add(vt.getTestKey());

		vt = new VarianceSubtest(t.getTestKey(), 0.7);
		addTestType("Variance Subtest on Zip NCD Test, 0.7", vt);
		tks.add(vt.getTestKey());

		vt = new VarianceSubtest(t.getTestKey(), 1.0);
		addTestType("Variance Subtest on Zip NCD Test, 1.0", vt);
		tks.add(vt.getTestKey());

		t = new TokenCountTest();
		addTestType("Token counting test", new TokenCountTest());
		tks.add(t.getTestKey());

		vt = new VarianceSubtest(TokenCountTest.SUBJECT_TOKVECTOR, 0.5);
		addTestType("Variance Subtest on TokenCountTest", vt);
		tks.add(vt.getTestKey());

		ArchiveFormat afs[] = new ArchiveFormat[] { new ZipFormat(),
				new Bzip2Format(), new GzipFormat() };
		for (ArchiveFormat af : afs) {
			if (af instanceof ZipFormat) {
				continue;
			}
			t = new NCDTest(af);
			addTestType(af.getClass().getSimpleName().replace("Format", "")
					+ " NCD Test", t);
			tks.add(t.getTestKey());
		}
		for (ArchiveFormat af : afs) {
			t = new RawNCDTest(af);
			addTestType("Raw "
					+ af.getClass().getSimpleName().replace("Format", "")
					+ " NCD Test", t);
			tks.add(t.getTestKey());
		}

		jtfResults.setText(m("Test.None"));
		jtfSources.setText(m("Test.None"));

		ArrayList<String> names = new ArrayList<>(testNameLookup.keySet());
		jcbTests.setModel(new DefaultComboBoxModel(names.toArray()));

		// default test is zip test
		jcbTests.setSelectedIndex(0);

		// Initialize help panel
		initHelpPanel();
	}

	public void addTestType(String name, Test t) {
		JCheckBoxMenuItem jcbmi = new JCheckBoxMenuItem(name);
		ActionListener listener = new TestSelectionListener(name, t);
		jcbmi.addActionListener(listener);
		testTypeCheckBoxes.put(t, jcbmi);
		jmTest.add(jcbmi);
		testNameLookup.put(name, t);

		// if first test in list, select as default
		if (testTypeCheckBoxes.size() == 1) {
			listener.actionPerformed(null);
		}
	}

	private void initHelpPanel() {
		tbHelp.setEnabled(true);
		tbHelp.setSelected(true);
		updateHelpPanel();
		setSize(600, (int) getPreferredSize().getHeight() + jpHelp.getHeight());
		// Open hyperlinks in browser if Action.BROWSE is available
		helpHTMLPane.addHyperlinkListener(new HyperlinkListener() {
			/**
			 * Open URI with default browser
			 */
			public void hyperlinkUpdate(HyperlinkEvent e) {
				try {
					if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED
							&& Desktop.isDesktopSupported()) {
						Desktop desktop = Desktop.getDesktop();
						desktop.browse(new URI(e.getURL().toString()));
					}
				} catch (URISyntaxException ex) {
					System.err.println("Trying to open wron URI '"
							+ e.getURL().toString());
				} catch (IOException ex) {
					System.err.println("Couldn't open " + e.getURL()
							+ " with default browser.");
				}
			}
		});
	}

	/** 
	 * Load a test help file, adhering to the following structure:
	 * <helppairs>
	 *  <testhelp>
	 *   <testname>...</testname>
	 *   <helpcontent>...</helpcontent>
	 *  </testhelp>
	 *  ...
	 * </helppairs>
	 * Notice that 'helpcontent' should be XHTML: old-fashioned HTML may fail to
	 * validate.
	 */
	public HashMap<String, String> loadTestHelpFile(String fileName)
			throws IOException {
		HashMap<String, String> m = new HashMap<String, String>();
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		URL url = getClass().getClassLoader().getResource(fileName);
		try {
			Document doc = (new SAXBuilder()).build(url);
			for (Element th : doc.getRootElement().getChildren()) {
				m.put(th.getChildTextTrim("testname"), outputter
						.outputString(th.getChild("helpcontent")));
			}
		} catch (JDOMException | NullPointerException e) {
			throw new IOException("Impossible to read XML file for " + url, e);
		}
		return m;
	}

	/**
	 * Update jpHelp panel loading help content if necessary.
	 */
	private void updateHelpPanel() {
		// Load help content if necessary. Assume that 1st locale is always right
		if (testHelpMap == null && tbHelp.isSelected()) {
			try {
				testHelpMap = loadTestHelpFile(m("Test.HelpFile"));
			} catch (IOException ex) {
				System.err.println("Error loading Test Help File "
						+ m("Test.HelpFile"));
				testHelpMap = null;
				jpHelp.setVisible(false);
				return;
			}
		}

		// Update panel appearance
		String helpContent = m("Test.DefaultHelpMessage", jcbTests
				.getSelectedItem());
		if (testHelpMap != null) {
			if (testHelpMap.containsKey(jcbTests.getSelectedItem())) {
				helpContent = testHelpMap.get(jcbTests.getSelectedItem());
			}
		}

		helpHTMLPane.setText(helpContent);
		jpHelp.setVisible(tbHelp.isSelected());
	}

	private void updateHelpPanelSize() {
		Rectangle bounds = getBounds();
		if (tbHelp.isSelected()) {
			setBounds(new Rectangle(bounds.x, bounds.y, bounds.width,
					bounds.height + jpHelp.getHeight()));
		} else {
			setBounds(new Rectangle(bounds.x, bounds.y, bounds.width,
					bounds.height - jpHelp.getHeight()));
		}
	}

	private class TestSelectionListener implements ActionListener {
		private String name;
		private Test t;

		public TestSelectionListener(String name, Test t) {
			this.name = name;
			this.t = t;
		}

		public void actionPerformed(ActionEvent event) {
			updateTestMenu();
			currentTest = t;
			launchTest(t, jcbxSuggestThresholds.isSelected());
		}
	}

	public void updateTestMenu() {
		for (Test t : testTypeCheckBoxes.keySet()) {
			boolean haveResults = ac.hasResultsForKey(t.getTestKey());
			testTypeCheckBoxes.get(t).setSelected(haveResults);
			testTypeCheckBoxes.get(t).repaint();
		}
	}

	private URL loadUrl(String location) {
		ClassLoader loader = MainGui.class.getClassLoader();
		return loader.getResource(location);
	}

	public void launchTest(Test t, boolean suggestThresholds) {
		if (ac == null) {
			return;
		}

		if (ac.hasResultsForKey(t.getTestKey())) {
			showResults(t.getTestKey());
			return;
		}

		if (sourcesDir == null && jtfSources.isEnabled()) {
			return;
		}

		try {
			log.info("Starting test " + t);
			if (t instanceof TokenizingTest) {
				((TokenizingTest) t).setTokenizer(ac.chooseTokenizer());
			}
			// launch test 
			String tcn = t.getClass().getName();
			String tn = tcn.substring(tcn.lastIndexOf('.') + 1);
			analysis = new GraphicalAnalysis(ac, tn, t,
					new ShowResultsCallback(t));
			analysis.start();
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}

	/**
	 * internal class used from launchTest that will pop up results
	 * when analysis is finished
	 */
	private class ShowResultsCallback implements Runnable {
		private Test t;

		public ShowResultsCallback(Test t) {
			this.t = t;
		}

		public void run() {
			showResults(t.getTestKey());
			updateTestMenu();
		}
	}

	public void loadSources(File d) {
		if (d == null) {
			d = FileUtils.chooseFile(this, m("Test.SourceFolder"), true,
					JFileChooser.DIRECTORIES_ONLY);
			if (d == null)
				return;
		}
		try {
			SourceSet ss = new SourceSet(d);
			ac.loadSources(ss);
			clearAllResults();
		} catch (Exception e) {
			// FIXME - update string            
			JOptionPane.showMessageDialog(null, "Error reading source files",
					"Error", JOptionPane.ERROR_MESSAGE);
			System.err.println(e);
			e.printStackTrace();
		}
		jbConfirmTest.setEnabled(true);
		setSourcesDir(d);
	}

	public void loadSources(SourceSet ss) {
		try {
			ac.loadSources(ss);
			clearAllResults();
		} catch (Exception e) {
			// FIXME - update string
			JOptionPane.showMessageDialog(null, "Error reading source files",
					"Error", JOptionPane.ERROR_MESSAGE);
			System.err.println(e);
			e.printStackTrace();
		}
		jbConfirmTest.setEnabled(true);
		jtfSources.setText(m("Test.SourcesInMem"));
		jtfSources.setEnabled(false);
	}

	public void loadAnalysis(File f) {
		if (f == null) {
			f = FileUtils.chooseFile(this, m("Test.ResultsFile"), true,
					JFileChooser.FILES_ONLY);
			if (f == null)
				return;
		}
		try {
			clearAllResults();
			ac.loadFromFile(f);
			updateTestMenu();
		} catch (Exception e) {
			// FIXME - update string            
			JOptionPane.showMessageDialog(null, "Error loading results",
					"Error", JOptionPane.ERROR_MESSAGE);
			System.err.println(e);
			e.printStackTrace();
		}
		setSaveFile(f);
	}

	public void saveAnalysis(File f) {
		if (f == null) {
			f = FileUtils.chooseFile(this,
					m("Test.SourcesAndResults.ResultsFile"), false,
					JFileChooser.FILES_ONLY);
			if (f == null)
				return;
		}
		try {
			ac.saveToFile(f);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, m("Test.errorSaving"),
					m("ERROR"), JOptionPane.ERROR_MESSAGE);
			System.err.println(e);
			e.printStackTrace();
		}
		setSaveFile(f);
		JOptionPane.showMessageDialog(this, m("Test.resultsSavedOk"),
				m("DONE"), JOptionPane.INFORMATION_MESSAGE);
		return;
	}

	public void showResults(String testKey) {
		TestResultsDialog trd = (TestResultsDialog) testResults.get(testKey);

		if (trd == null) {
			trd = new TestResultsDialog(this, ac, testKey);
			testResults.put(testKey, trd);
			trd.setVisible(true);
			trd.setSuggestThresholds(jcbxSuggestThresholds.isSelected());
			trd.showResults();
		} else {
			if (!trd.isVisible()) {
				trd.setVisible(true);
			}
			trd.setSuggestThresholds(jcbxSuggestThresholds.isSelected());
			trd.showResults();
		}
	}

	public void clearAllResults() {
		for (TestResultsDialog trd : testResults.values()) {
			if (trd.isVisible())
				trd.dispose();
		}
		testResults.clear();
	}

	public void setSaveFile(File f) {
		saveFile = f;
		boolean acOk = (ac.getSubmissions().length > 0);
		jbSaveResults.setEnabled(acOk && !(f.exists() && f.isDirectory()));
		jbLoadResults.setEnabled(acOk && f.isFile() && f.exists());
		jtfResults.setText(f.getAbsolutePath());
		jtfResults.repaint();
	}

	public void setSourcesDir(File d) {
		sourcesDir = d;
		jbLoadSources.setEnabled(d.exists() && d.isDirectory());
		jtfSources.setText(d.getAbsolutePath());
		jtfSources.repaint();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.M
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		jpFiles = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		jtfSources = new javax.swing.JTextField();
		jtbChSources = new javax.swing.JButton();
		jbLoadSources = new javax.swing.JButton();
		jLabel2 = new javax.swing.JLabel();
		jtfResults = new javax.swing.JTextField();
		jbChResults = new javax.swing.JButton();
		jbLoadResults = new javax.swing.JButton();
		jbSaveResults = new javax.swing.JButton();
		jpTest = new javax.swing.JPanel();
		jLabel3 = new javax.swing.JLabel();
		jcbTests = new javax.swing.JComboBox();
		jbConfirmTest = new javax.swing.JButton();
		jcbxSuggestThresholds = new javax.swing.JCheckBox();
		tbHelp = new javax.swing.JToggleButton();
		jpHelp = new javax.swing.JPanel();
		jspHelp = new javax.swing.JScrollPane();
		helpHTMLPane = new javax.swing.JEditorPane();
		jmbBigMenu = new javax.swing.JMenuBar();
		jmFile = new javax.swing.JMenu();
		jmiNewAnalysis = new javax.swing.JMenuItem();
		jmiOpenAnalysis = new javax.swing.JMenuItem();
		jmiSaveAnalysis = new javax.swing.JMenuItem();
		jmTest = new javax.swing.JMenu();
		jmHelp = new javax.swing.JMenu();
		jmiHelp = new javax.swing.JMenuItem();
		jmiAbout = new javax.swing.JMenuItem();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setLocationByPlatform(true);
		setMinimumSize(new java.awt.Dimension(423, 250));
		getContentPane().setLayout(new java.awt.GridBagLayout());

		jpFiles.setBorder(javax.swing.BorderFactory
				.createTitledBorder(m("Test.SourcesAndResults.Title")));
		jpFiles.setLayout(new java.awt.GridBagLayout());

		jLabel1.setText(m("Test.SourcesAndResults.SourcesDir"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.insets = new java.awt.Insets(8, 4, 8, 4);
		jpFiles.add(jLabel1, gridBagConstraints);

		jtfSources.setText(m("Test.None"));
		jtfSources.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jtfSourcesActionPerformed(evt);
			}
		});
		jtfSources.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusGained(java.awt.event.FocusEvent evt) {
				jtfSourcesFocusGained(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(8, 4, 8, 0);
		jpFiles.add(jtfSources, gridBagConstraints);

		jtbChSources.setText("...");
		jtbChSources.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		jtbChSources.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
		jtbChSources.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jtbChSourcesActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 4);
		jpFiles.add(jtbChSources, gridBagConstraints);

		jbLoadSources.setText(m("Test.Load"));
		jbLoadSources.setEnabled(false);
		jbLoadSources.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbLoadSourcesActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.insets = new java.awt.Insets(8, 4, 8, 4);
		jpFiles.add(jbLoadSources, gridBagConstraints);

		jLabel2.setText(m("Test.SourcesAndResults.ResultsFile"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.insets = new java.awt.Insets(8, 4, 8, 4);
		jpFiles.add(jLabel2, gridBagConstraints);

		jtfResults.setText(m("Test.None"));
		jtfResults.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jtfResultsActionPerformed(evt);
			}
		});
		jtfResults.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusGained(java.awt.event.FocusEvent evt) {
				jtfResultsFocusGained(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(8, 4, 8, 0);
		jpFiles.add(jtfResults, gridBagConstraints);

		jbChResults.setText("...");
		jbChResults.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		jbChResults.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
		jbChResults.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbChResultsActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 4);
		jpFiles.add(jbChResults, gridBagConstraints);

		jbLoadResults.setText(m("Test.Load"));
		jbLoadResults.setEnabled(false);
		jbLoadResults.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbLoadResultsActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
		jpFiles.add(jbLoadResults, gridBagConstraints);

		jbSaveResults.setText(m("Test.Save"));
		jbSaveResults.setEnabled(false);
		jbSaveResults.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbSaveResultsActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 4;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
		jpFiles.add(jbSaveResults, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 1.0;
		getContentPane().add(jpFiles, gridBagConstraints);

		jpTest.setBorder(javax.swing.BorderFactory.createTitledBorder("Tests"));
		jpTest.setLayout(new java.awt.GridBagLayout());

		jLabel3.setText(m("Test.TestSel.Label"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		jpTest.add(jLabel3, gridBagConstraints);

		jcbTests.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jcbTestsActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		jpTest.add(jcbTests, gridBagConstraints);

		jbConfirmTest.setText(m("Test.TestSel.Confirm"));
		jbConfirmTest.setEnabled(false);
		jbConfirmTest.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jbConfirmTestActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		jpTest.add(jbConfirmTest, gridBagConstraints);

		jcbxSuggestThresholds.setSelected(true);
		jcbxSuggestThresholds.setText(m("Test.TestSel.SuggestThresholds"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jpTest.add(jcbxSuggestThresholds, gridBagConstraints);

		tbHelp.setText("?");
		tbHelp.setEnabled(false);
		tbHelp.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				tbHelpActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		jpTest.add(tbHelp, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 1.0;
		getContentPane().add(jpTest, gridBagConstraints);

		jpHelp.setBorder(javax.swing.BorderFactory
				.createTitledBorder(m("Test.Menu.Help")));
		jpHelp.setLayout(new java.awt.GridLayout(1, 0));

		jspHelp.setBorder(null);

		helpHTMLPane.setEditable(false);
		helpHTMLPane.setBackground(new java.awt.Color(238, 238, 238));
		helpHTMLPane.setBorder(null);
		helpHTMLPane.setContentType("text/html"); // NOI18N
		helpHTMLPane
				.setText("<html>   <head>Head</head>   <body>   This is <b>the</b> default <strong>body</strong>  <p style=\"margin-top: 0\">            </p>   </body> </html> ");
		helpHTMLPane.setMargin(new java.awt.Insets(15, 15, 15, 15));
		jspHelp.setViewportView(helpHTMLPane);

		jpHelp.add(jspHelp);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		getContentPane().add(jpHelp, gridBagConstraints);
		jpHelp.getAccessibleContext().setAccessibleName("");

		jmFile.setText(m("Test.Menu.File"));

		jmiNewAnalysis.setText("Nuevo Analisis");
		jmFile.add(jmiNewAnalysis);

		jmiOpenAnalysis.setText("Cargar Resultados");
		jmiOpenAnalysis.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jmiOpenAnalysisActionPerformed(evt);
			}
		});
		jmFile.add(jmiOpenAnalysis);

		jmiSaveAnalysis.setText("Guardar Resultados");
		jmiSaveAnalysis.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jmiSaveAnalysisActionPerformed(evt);
			}
		});
		jmFile.add(jmiSaveAnalysis);

		jmbBigMenu.add(jmFile);

		jmTest.setText("Test");
		jmbBigMenu.add(jmTest);

		jmHelp.setText(m("Test.Menu.Help"));
		jmHelp.setAlignmentX(0.0F);
		jmHelp.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jmHelp.setPreferredSize(new java.awt.Dimension(65, 15));

		jmiHelp.setText(m("Test.Menu.Help"));
		jmiHelp.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jmiHelpActionPerformed(evt);
			}
		});
		jmHelp.add(jmiHelp);

		jmiAbout.setText(m("Test.Menu.About"));
		jmiAbout.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jmiAboutActionPerformed(evt);
			}
		});
		jmHelp.add(jmiAbout);

		jmbBigMenu.add(jmHelp);

		setJMenuBar(jmbBigMenu);

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void jtfResultsFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtfResultsFocusGained
		// TODO add your handling code here:
		if (jtfResults.getText().contains(m("Test.None")))
			jtfResults.setText("");
	}//GEN-LAST:event_jtfResultsFocusGained

	private void jtfSourcesFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtfSourcesFocusGained
		// TODO add your handling code here:
		if (jtfSources.getText().contains(m("Test.None")))
			jtfSources.setText("");
	}//GEN-LAST:event_jtfSourcesFocusGained

	private void jbConfirmTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbConfirmTestActionPerformed
		// TODO add your handling code here:
		launchTest(currentTest, jcbxSuggestThresholds.isSelected());
	}//GEN-LAST:event_jbConfirmTestActionPerformed

	private void jcbTestsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jcbTestsActionPerformed
		// TODO add your handling code here:
		currentTest = testNameLookup.get((String) jcbTests.getSelectedItem());
		updateHelpPanel();
	}//GEN-LAST:event_jcbTestsActionPerformed

	private void jbSaveResultsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbSaveResultsActionPerformed
		// TODO add your handling code here:
		saveAnalysis(saveFile);
	}//GEN-LAST:event_jbSaveResultsActionPerformed

	private void jbLoadResultsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbLoadResultsActionPerformed
		// TODO add your handling code here:
		loadAnalysis(saveFile);
	}//GEN-LAST:event_jbLoadResultsActionPerformed

	private void jbChResultsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbChResultsActionPerformed
		// TODO add your handling code here:
		saveFile = FileUtils.chooseFile(this,
				m("Test.SourcesAndResults.ResultsFile"), false,
				JFileChooser.FILES_ONLY);
	}//GEN-LAST:event_jbChResultsActionPerformed

	private void jbLoadSourcesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbLoadSourcesActionPerformed
		// TODO add your handling code here:
		loadSources(sourcesDir);
	}//GEN-LAST:event_jbLoadSourcesActionPerformed

	private void jtfSourcesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jtfSourcesActionPerformed
		// TODO add your handling code here:
		setSourcesDir(new File(jtfSources.getText()));
	}//GEN-LAST:event_jtfSourcesActionPerformed

	private void jtfResultsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jtfResultsActionPerformed
		// TODO add your handling code here:
		setSaveFile(new File(jtfResults.getText()));
	}//GEN-LAST:event_jtfResultsActionPerformed

	private void jtbChSourcesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jtbChSourcesActionPerformed
		// TODO add your handling code here:
		loadSources((File) null);
	}//GEN-LAST:event_jtbChSourcesActionPerformed

	private void jmiSaveAnalysisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmiSaveAnalysisActionPerformed
		// TODO add your handling code here:
		saveAnalysis(saveFile);
	}//GEN-LAST:event_jmiSaveAnalysisActionPerformed

	private void jmiHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmiHelpActionPerformed
		// TODO add your handling code here:        
		if (helpBrowser == null) {
			helpBrowser = new HelpBrowser(this, m("Test.Menu.Help"),
					"index.html", true);
		}
		helpBrowser.setVisible(true);
	}//GEN-LAST:event_jmiHelpActionPerformed

	private void jmiOpenAnalysisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmiOpenAnalysisActionPerformed
		// TODO add your handling code here:
		loadAnalysis(null);
	}//GEN-LAST:event_jmiOpenAnalysisActionPerformed

	private void jmiAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmiAboutActionPerformed
		if (aboutBrowser == null) {
			aboutBrowser = new HelpBrowser(this, m("Test.Menu.About") + " (v "
					+ ACVersion.getVersion() + ")", "about-ac.html", false);
		}
		aboutBrowser.setVisible(true);
	}//GEN-LAST:event_jmiAboutActionPerformed

	private void tbHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbHelpActionPerformed
		updateHelpPanel();
		updateHelpPanelSize();
	}//GEN-LAST:event_tbHelpActionPerformed

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {

		I18N.setLang(Locale.getDefault().getLanguage());

		String help = "AC: Analysis - v" + ACVersion.getVersion() + "\n"
				+ m("Test.CommandLineHelp").replaceAll("[$]", "\n");

		System.err.println("Running under locale " + Locale.getDefault());

		List<String> allArguments = Arrays.asList(args);
		if (args.length > 0
				&& (allArguments.contains("-h") || allArguments.contains("--h")
						|| allArguments.contains("--help") || allArguments
						.contains("\\?"))) {
			System.err.println(help);
			return;
		}

		MainGui gui = new MainGui();
		gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		gui.setVisible(true);

		switch (args.length) {
		case 0: {
			break;
		}
		case 1: {
			File d = new File(args[0]);
			if (d.isDirectory()) {
				gui.loadSources(d);
			} else {
				// choose a sane default analysis
				gui.loadAnalysis(d);
			}
			gui.launchTest(new NCDTest(new ZipFormat()), false);
			break;
		}
		case 2: {
			gui.loadSources(new File(args[0]));
			gui.loadAnalysis(new File(args[1]));
			break;
		}
		default:
			System.err.println(help);
		}
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JEditorPane helpHTMLPane;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JButton jbChResults;
	private javax.swing.JButton jbConfirmTest;
	private javax.swing.JButton jbLoadResults;
	private javax.swing.JButton jbLoadSources;
	private javax.swing.JButton jbSaveResults;
	private javax.swing.JComboBox jcbTests;
	private javax.swing.JCheckBox jcbxSuggestThresholds;
	private javax.swing.JMenu jmFile;
	private javax.swing.JMenu jmHelp;
	private javax.swing.JMenu jmTest;
	private javax.swing.JMenuBar jmbBigMenu;
	private javax.swing.JMenuItem jmiAbout;
	private javax.swing.JMenuItem jmiHelp;
	private javax.swing.JMenuItem jmiNewAnalysis;
	private javax.swing.JMenuItem jmiOpenAnalysis;
	private javax.swing.JMenuItem jmiSaveAnalysis;
	private javax.swing.JPanel jpFiles;
	private javax.swing.JPanel jpHelp;
	private javax.swing.JPanel jpTest;
	private javax.swing.JScrollPane jspHelp;
	private javax.swing.JButton jtbChSources;
	private javax.swing.JTextField jtfResults;
	private javax.swing.JTextField jtfSources;
	private javax.swing.JToggleButton tbHelp;
	// End of variables declaration//GEN-END:variables

}
