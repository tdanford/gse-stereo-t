/*
 * Author: tdanford
 * Date: Jun 11, 2008
 */
/**
 * 
 */
package edu.mit.csail.cgs.sigma.viz;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * @author tdanford
 *
 */
public class LabelChangeDialog extends JDialog implements Runnable {

	private Labeled labeled;
	private JTextField labelField;
	private JButton okButton, cancelButton;
	
	public LabelChangeDialog(JFrame parent, Labeled lbl) { 
		super(parent, "Label Change");
		labeled = lbl;
		
		Container c = (Container)getContentPane();
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout());
		buttons.add(okButton = new JButton("Ok"));
		buttons.add(cancelButton = new JButton("Cancel"));
		
		okButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				ok();
			}
		});
		
		cancelButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				cancel();
			}
		});
		
		JPanel textPanel = new JPanel();
		textPanel.setLayout(new FlowLayout());
		textPanel.add(labelField = new JTextField(labeled.getLabel()));
		
		c.add(textPanel, BorderLayout.CENTER);
		c.add(buttons, BorderLayout.SOUTH);
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	}
	
	public void ok() { 
		String lbl = labelField.getText();
		labeled.setLabel(lbl);
		dispose();
	}
	
	public void cancel() { 
		dispose();
	}
	
	public void run() { 
		setVisible(true);
		pack();
	}
}
