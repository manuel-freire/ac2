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
package es.ucm.fdi.ac;

import static es.ucm.fdi.util.I18N.m;

import java.awt.Color;
import java.io.File;
import java.util.Locale;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import es.ucm.fdi.ac.gui.LogPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.ucm.fdi.ac.extract.ZipSelectionPanel;
import es.ucm.fdi.ac.gui.ACVersion;
import es.ucm.fdi.ac.gui.MainGui;
import es.ucm.fdi.util.I18N;

/**
 * Main graphical entry-point.
 * Builds a tabbed interface for selection, choice-of-analysis and result visualization.
 *
 * @author  mfreire
 */
public class Main extends javax.swing.JFrame implements
		ZipSelectionPanel.SelectionListener, LogPanel.UnreadLineListener {

	private Logger log = LogManager.getLogger(Main.class);

	private ZipSelectionPanel zsp;
	private JTabbedPane tabs;

	private int unreadLogLines = 0;

	@Override
	public void incrementUnreadLines() {
		unreadLogLines++;
		if (unreadLogLines == 0) {
			tabs.setBackgroundAt(1, tabs.getBackgroundAt(0));
		} else {
			tabs.setBackgroundAt(1, Color.PINK);
		}
		tabs.setTitleAt(1, m("Tabs.Log") + " (" + unreadLogLines + ")");
	}

	@Override
	public void resetUnreadLines() {
		unreadLogLines = -1;
		incrementUnreadLines();
	}

	/**
	 * Creates new UI
	 */
	public Main() {
		zsp = new ZipSelectionPanel(this);
		tabs = new JTabbedPane();
		tabs.add(m("Tabs.Extract"), zsp);

		tabs.add(m("Tabs.Log"), new JScrollPane(new LogPanel(this)));
		tabs.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				resetUnreadLines();
			}
		});
		getContentPane().add(tabs);

		setSize(1200, 800);
		setLocationByPlatform(true);
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setTitle(m("AC.Title", ACVersion.getVersion()));
		log.info("Launched version: " + ACVersion.getVersion());
		setLocationByPlatform(true);
		setVisible(true);
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		I18N.setLang(Locale.getDefault().getLanguage());
		final String[] argCopy = args.clone();
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				Main e = new Main();
				for (String s : argCopy) {
					File f = new File(s);
					e.zsp.addSourceFile(f);
				}
			}
		});
	}

	@Override
	public void selectionConfirmed(SourceSet ss) {
		MainGui main = new MainGui();
		tabs.add(m("Tabs.Tests"), main);
		//main.addToFrame(this);
		try {
			main.loadSources(ss);
		} catch (Exception e) {
			log.warn("Error loading selected sources", e);
		}
	}
}
