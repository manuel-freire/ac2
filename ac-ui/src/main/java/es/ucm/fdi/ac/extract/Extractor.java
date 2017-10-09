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
 * Extract.java
 *
 * Created on September 18, 2006, 12:08 PM
 */

package es.ucm.fdi.ac.extract;

import es.ucm.fdi.ac.gui.ACVersion;
import es.ucm.fdi.util.I18N;
import org.apache.log4j.Logger;

import static es.ucm.fdi.util.I18N.m;

import java.io.File;
import java.util.Locale;

/**
 * Interface to the submission selection & filtering interface
 *
 * @author  mfreire
 */
public class Extractor extends javax.swing.JFrame {

	private static final Logger log = Logger.getLogger(Extractor.class);

	private ZipSelectionPanel zsp;

	/**
	 * Creates new form Extract
	 */
	public Extractor() {
		zsp = new ZipSelectionPanel();
		getContentPane().add(zsp);
		setSize(1200, 800);
		setLocationByPlatform(true);
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setTitle(m("Extract.Title", ACVersion.getVersion()));
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
				Extractor e = new Extractor();
				for (String s : argCopy) {
					File f = new File(s);
					e.zsp.addSourceFile(f);
				}
			}
		});
	}
}
