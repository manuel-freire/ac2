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

import es.ucm.fdi.ac.extract.ZipSelectionPanel;
import es.ucm.fdi.ac.gui.ACVersion;
import es.ucm.fdi.ac.gui.MainGui;
import es.ucm.fdi.util.I18N;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import static es.ucm.fdi.util.I18N.m;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

/**
 * Main graphical entry-point.
 * Builds a tabbed interface for selection, choice-of-analysis and result visualization.
 *
 * @author  mfreire
 */
public class Main extends javax.swing.JFrame implements
		ZipSelectionPanel.SelectionListener {

	private Logger log = LogManager.getLogger(Main.class);

	private ZipSelectionPanel zsp;
	private JTextPane logPane;
	private JTabbedPane tabs;
	private int unreadLogLines = 0;
	private static SimpleAttributeSet highlighted;

	private void initLoggingAndStyles() {
		highlighted = new SimpleAttributeSet();
		StyleConstants.setBold(highlighted, true);

		addAppender(new LogWriter(), "TabbedLogAppender");
	}

	void addAppender(final Writer writer, final String writerName) {
		final LoggerContext context = LoggerContext.getContext(false);
		final Configuration config = context.getConfiguration();
		ConsoleAppender prev = config.getAppender("STDOUT");
		final Appender appender = WriterAppender.createAppender(
				(StringLayout) prev.getLayout(), null, writer, writerName,
				false, true);
		appender.start();
		config.addAppender(appender);
		updateLoggers(appender, config);
	}

	private void updateLoggers(final Appender appender,
			final Configuration config) {
		final Level level = null;
		final Filter filter = null;
		for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
			loggerConfig.addAppender(appender, level, filter);
		}
		config.getRootLogger().addAppender(appender, level, filter);
	}

	private class LogWriter extends Writer {
		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			String message = new String(cbuf, off, len);
			String title = "???";
			final String SEPARATOR = "::";
			if (message.substring(0, Math.min(message.length(), 64)).contains(
					SEPARATOR)) {
				title = message.substring(0, message.indexOf(SEPARATOR));
				message = message
						.substring(title.length() + SEPARATOR.length());
			}

			String[] lines = message.split("\n");
			StringBuilder retained = new StringBuilder();
			for (String line : lines) {
				if (!line.trim().startsWith("at java")) {
					retained.append(line).append("\n");
				}
			}

			logEvent(title, retained.toString());
			updateUnreadLogLines();
		}

		@Override
		public void flush() throws IOException {
		}

		@Override
		public void close() throws IOException {
		}
	}

	private void updateUnreadLogLines() {
		unreadLogLines++;
		if (unreadLogLines == 0) {
			tabs.setBackgroundAt(1, tabs.getBackgroundAt(0));
		} else {
			tabs.setBackgroundAt(1, Color.PINK);
		}
		tabs.setTitleAt(1, m("Tabs.Log") + " (" + unreadLogLines + ")");
	}

	/**
	 * Creates new UI
	 */
	public Main() {
		zsp = new ZipSelectionPanel(this);
		tabs = new JTabbedPane();
		tabs.add(m("Tabs.Extract"), zsp);
		logPane = new JTextPane();
		logPane.setEditable(false);
		tabs.add(m("Tabs.Log"), new JScrollPane(logPane));
		tabs.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				unreadLogLines = -1;
				updateUnreadLogLines();
			}
		});
		initLoggingAndStyles();
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

	public void logEvent(String eventTitle, String eventText) {
		try {
			Document doc = logPane.getDocument();
			doc.insertString(doc.getLength(), eventTitle, highlighted);
			doc.insertString(doc.getLength(), eventText, null);
		} catch (BadLocationException e) {
			log.warn("Bad insertion position", e);
		}
	}
}
