/*
 * Author: tdanford
 * Date: May 13, 2008
 */
package edu.mit.csail.cgs.sigma.blots;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

public class ParsingTextPanel extends JPanel {
	
	private JTextArea text;

	public ParsingTextPanel() { 
		super();
		
		text = new JTextArea();
		setLayout(new BorderLayout());
		add(new JScrollPane(text), BorderLayout.CENTER);
	}
	
	public String getRawText() { return text.getText(); }
	
	public void clear() { 
		text.setText("");
	}
}
