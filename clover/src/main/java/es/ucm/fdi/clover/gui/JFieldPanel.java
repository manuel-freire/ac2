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
 * FieldPanel.java
 *
 * Created on 9 de abril de 2003, 13:32
 * Original Author: Manuel Freire (manuel.freire@uam.es)
 */

package es.ucm.fdi.clover.gui;

import java.awt.*;
import javax.swing.*;

/**
 *
 * @author  manu
 */
public class JFieldPanel extends JPanel {

	public static String fieldLabel = "Field";
	public static String valueLabel = "Value";

	/** Simple constructor */
	public JFieldPanel(int fieldNumber) {
		initComponents(fieldNumber);
	}

	/** Creates a new instance of FieldPanel */
	public JFieldPanel(String tituloCampos, String[] campos,
			String tituloValores, String[] valores, boolean[] habilitado) {

		// crea los componentes y los dispone        
		initComponents(campos.length);

		// los inicializa
		lCampos.setText((tituloCampos == null) ? fieldLabel : tituloCampos);
		lValores.setText((tituloValores == null) ? valueLabel : tituloValores);

		for (int i = 0; i < campos.length; i++) {
			lc[i].setText(campos[i]);
			tf[i].setEditable((habilitado == null) ? true : habilitado[i]);
			tf[i].setText((valores == null) ? "" : valores[i]);
		}
	}

	public String getValue(String campo) {
		for (int i = 0; i < lc.length; i++) {
			if (lc[i].getText().equals(campo)) {
				return tf[i].getText();
			}
		}

		return null;
	}

	public String getValue(int i) {
		if (i > tf.length)
			return null;
		return tf[i].getText();
	}

	public void setValue(int i, String val) {
		tf[i].setText(val);
	}

	public int getFieldCount() {
		return tf.length;
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 */
	private void initComponents(int nFields) {
		java.awt.GridBagConstraints gridBagConstraints;
		setLayout(new GridBagLayout());

		// titulos
		lCampos = new JLabel();
		lValores = new JLabel();

		lCampos.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		lCampos.setFont(new java.awt.Font("Dialog", 1, 12));
		lCampos.setBackground(Color.darkGray);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new java.awt.Insets(10, 9, 10, 9);
		add(lCampos, gridBagConstraints);

		lValores.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		lValores.setFont(new java.awt.Font("Dialog", 1, 12));
		lValores.setBackground(Color.darkGray);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
		add(lValores, gridBagConstraints);

		// campos
		lc = new JLabel[nFields];
		tf = new JTextField[nFields];

		GridBagConstraints gbcLv = new java.awt.GridBagConstraints();
		gbcLv.gridx = 0;
		gbcLv.gridy = 1;
		gbcLv.fill = java.awt.GridBagConstraints.BOTH;
		gbcLv.weightx = 0;
		gbcLv.weighty = 0;
		gbcLv.insets = new java.awt.Insets(3, 3, 10, 15);

		GridBagConstraints gbcTf = new java.awt.GridBagConstraints();
		gbcTf.gridx = 1;
		gbcTf.gridy = 1;
		gbcTf.gridwidth = GridBagConstraints.REMAINDER;
		gbcTf.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbcTf.weightx = 1f;
		gbcTf.weighty = 0;
		gbcTf.insets = new java.awt.Insets(3, 3, 5, 10);

		for (int i = 0; i < nFields; i++) {
			lc[i] = new JLabel();
			lc[i].setAlignmentX(JLabel.RIGHT_ALIGNMENT);
			add(lc[i], gbcLv);
			gbcLv.gridy++;

			tf[i] = new JTextField();
			tf[i].setBackground(java.awt.Color.white);
			tf[i].setColumns(32);
			add(tf[i], gbcTf);
			gbcTf.gridy++;
		}
	}

	private JLabel lCampos;
	private JLabel lValores;
	protected JTextField[] tf;
	protected JLabel[] lc;
}
