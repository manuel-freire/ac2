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
package es.ucm.fdi.clover.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.net.*;

/**
 * A very simple html browser with support for links
 *
 * @author mfreire
 */
public class HelpBrowser extends JDialog implements
		javax.swing.event.HyperlinkListener {

	private JEditorPane jep;
	private URL docUrl;

	public HelpBrowser(JFrame parent, String title, String helpUrl,
			boolean withHome) {
		super(parent, false);

		ClassLoader loader = this.getClass().getClassLoader();
		this.docUrl = loader.getResource(helpUrl);

		jep = new JEditorPane();
		jep.setEditable(false);
		jep.addHyperlinkListener(this);
		loadPage(docUrl);

		JButton homeButton = new JButton("home");
		homeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				try {
					jep.setPage(docUrl);
				} catch (Exception e) {
				}
			}
		});
		JButton closeButton = new JButton("dismiss");
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				try {
					setVisible(false);
				} catch (Exception e) {
				}
			}
		});

		JScrollPane jsp = new JScrollPane(jep);
		getContentPane().setLayout(new BorderLayout());
		if (withHome)
			getContentPane().add(homeButton, BorderLayout.NORTH);
		getContentPane().add(jsp, BorderLayout.CENTER);
		getContentPane().add(closeButton, BorderLayout.SOUTH);

		setTitle(title);
		setSize(600, 400);
	}

	/**
	 * After being closed, jumps back to prior position
	 */
	public void setVisible(boolean b) {
		super.setVisible(b);
		if (b == false) {
			loadPage(docUrl);
		}
	}

	/**
	 * Load a page
	 */
	public void loadPage(URL url) {
		try {
			jep.setPage(url);
		} catch (java.io.IOException ioe) {
			System.err.println("Error loading link: " + url);
		}
	}

	/**
	 * After a click
	 */
	public void hyperlinkUpdate(HyperlinkEvent event) {
		if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			loadPage(event.getURL());
		}
	}
}
