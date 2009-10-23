/*
 * Author: tdanford
 * Date: May 22, 2008
 */
package edu.mit.csail.cgs.sigma.viz;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import edu.mit.csail.cgs.sigma.motifs.*;
import edu.mit.csail.cgs.datasets.motifs.WeightMatrix;

public class MatrixComboBox extends JComboBox {
	
	private LinkedList<MatrixSelectionListener> listeners;
	private ComboBoxModel model;
	
	public MatrixComboBox() { 
		super();
		listeners = new LinkedList<MatrixSelectionListener>();
		model = getModel();

		this.addItem(new MatrixWrapper(null));
		
		this.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				MatrixWrapper wrapper = (MatrixWrapper)getSelectedItem();
				WeightMatrix m = wrapper.matrix;
				for(MatrixSelectionListener listener : listeners) { 
					listener.matrixSelected(m);
				}
			}
		});
	}
	
	public void addMatrixSelectionListener(MatrixSelectionListener l) { 
		listeners.add(l);
	}
	
	public void removeMatrixSelectionListener(MatrixSelectionListener l) { 
		listeners.remove(l);
	}
	
	public void addWeightMatrix(WeightMatrix m) { 
		addItem(new MatrixWrapper(m));
	}

	public static interface MatrixSelectionListener { 
		public void matrixSelected(WeightMatrix m);
	}
	
	private class MatrixWrapper { 
		public WeightMatrix matrix;
		
		public MatrixWrapper(WeightMatrix m) { 
			matrix = m;
		}
		
		public String toString() { 
			return matrix != null ? matrix.name : "NONE";
		}
	}
}
