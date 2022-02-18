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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Configuration;

import static es.ucm.fdi.util.I18N.m;

/**
 * A panel that displays log results. Can be installed in a tab, or used independently.
 *
 * @author  mfreire
 */
public class LogPanel extends JPanel {
    
    public interface UnreadLineListener {
        void incrementUnreadLines();
        void resetUnreadLines();
    }

	private static Logger log = LogManager.getLogger(LogPanel.class);

    private UnreadLineListener unreadLineListener;
	private JTextPane logPane;
	private static SimpleAttributeSet highlighted;
    private JComboBox<String> levelList;

    final String SEPARATOR = "::";
    
    private ArrayList<SimpleMessage> messages = new ArrayList<>(); 

    private List<String> levels = List.of("error","warn","info","debug","trace");
    private int currentLevel = levels.indexOf("warn");

    private static class SimpleMessage {
        private int priority;
        private String title;
        private String text;
        private SimpleMessage(int priority, String title, String text) {
            this.priority = priority;
            this.title = title;
            this.text = text;
        }
        public boolean hasPriorityBetterOrEqualThan(int max) {
            return priority <= max;
        }
        public void display(Document doc) {
            try {
                doc.insertString(doc.getLength(), title, highlighted);
                doc.insertString(doc.getLength(), text, null);
            } catch (BadLocationException e) {
                log.warn("Bad insertion position", e);
            }
        }
    }

    public LogPanel(UnreadLineListener unreadLineListener) {
        this.unreadLineListener = unreadLineListener;
        highlighted = new SimpleAttributeSet();
		StyleConstants.setBold(highlighted, true);

        addAppender(new LogWriter(), "TabbedLogAppender");
        
        logPane = new JTextPane();
        logPane.setEditable(false);
        setLayout(new java.awt.BorderLayout());
        add(new JScrollPane(logPane), BorderLayout.CENTER);

        levelList = new JComboBox<String>(levels.toArray(new String[0]));
        levelList.setSelectedIndex(currentLevel);
        levelList.addItemListener((e) -> {
            currentLevel = levels.indexOf(levelList.getSelectedItem());
            repopulateDocument();
        });
        levelList.setBorder(BorderFactory.createTitledBorder(m("Log.Levels")));
        add(levelList, BorderLayout.NORTH);
	}

	private void addAppender(final Writer writer, final String writerName) {
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
			
			if (message.substring(0, Math.min(message.length(), 64)).contains(
					SEPARATOR)) {
				title = message.substring(0, message.indexOf(SEPARATOR));
				message = message
						.substring(title.length() + SEPARATOR.length());
			}
            title = title.replace("([])", "");

			String[] lines = message.split("\n");
			StringBuilder retained = new StringBuilder();
			for (String line : lines) {
				if (!line.trim().startsWith("at java")) {
					retained.append(line).append("\n");
				}
			}
            String level = title.substring(0, 5).toLowerCase().strip();
            SimpleMessage m = new SimpleMessage(levels.indexOf(level), 
                title, retained.toString());
            messages.add(m);
			if (m.hasPriorityBetterOrEqualThan(currentLevel)) {
                m.display(logPane.getDocument());
                unreadLineListener.incrementUnreadLines();
            }
		}

		@Override
		public void flush() throws IOException {
		}

		@Override
		public void close() throws IOException {
		}
    }    

    private void repopulateDocument() {
		try {
            Document doc = logPane.getDocument();
            doc.remove(0, doc.getLength());            
		} catch (BadLocationException e) {
			log.warn("Could not clear document", e);
        }
        unreadLineListener.resetUnreadLines();
        for (SimpleMessage m : messages) {
            if (m.hasPriorityBetterOrEqualThan(currentLevel)) {
                m.display(logPane.getDocument());
                unreadLineListener.incrementUnreadLines();
            }   
        }        
    }
}
