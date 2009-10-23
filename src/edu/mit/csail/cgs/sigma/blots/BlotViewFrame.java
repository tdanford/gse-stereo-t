/*
 * Author: tdanford
 * Date: May 13, 2008
 */
package edu.mit.csail.cgs.sigma.blots;

import java.io.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import edu.mit.csail.cgs.viz.utils.FileChooser;

public class BlotViewFrame extends JFrame {
	
	public static void main(String[] args) { 
		File f1 = new File("C:\\Documents and Settings\\tdanford\\Desktop\\" +
		"blots\\stacie_blots\\ribonortherns1-6_small.png");
		File f2 = new File("C:\\Documents and Settings\\tdanford\\Desktop\\" +
		"blots\\stacie_blots\\flo11.png");
		try {
			Blot b = new Blot(f2);
			BlotViewPanel panel = new BlotViewPanel(b);
			BlotViewFrame frame = new BlotViewFrame(panel);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private BlotViewPanel panel;
    private JButton addColumn, addRow, addQuad;
    private ParsingTextPanel textPanel;
	
	public BlotViewFrame(BlotViewPanel p) { 
		super("Blot View");
		panel = p;
		
		Container c= (Container)getContentPane();
		c.setLayout(new BorderLayout());
		c.add(panel, BorderLayout.CENTER);
		
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new BorderLayout());
		c.add(inputPanel, BorderLayout.SOUTH);

		inputPanel.add(textPanel = new ParsingTextPanel(), BorderLayout.CENTER);
		textPanel.setBorder(new TitledBorder("Description"));
        
        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());
        inputPanel.add(buttons, BorderLayout.SOUTH);
        
        buttons.add(addColumn = new JButton("Add Column"));
        addColumn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	String description = textPanel.getRawText();
            	panel.addLastAsColumn(description);
            	textPanel.clear();
            } 
        });
        
        buttons.add(addRow = new JButton("Add Row"));
        addRow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	String description = textPanel.getRawText();
            	panel.addLastAsRow(description);
            	textPanel.clear();
            } 
        });
        
        buttons.add(addQuad = new JButton("Add Box"));
        addQuad.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	panel.addQuad();
            } 
        });
        
        setJMenuBar(createMenuBar());
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		pack();
	}
	
	public void saveImage() { 
		FileChooser chooser = new FileChooser(this);
        File f = chooser.choose();
        panel.saveImage(f);
	}
	
	public void save(File f) { 
        try {
			PrintStream ps = new PrintStream(new FileOutputStream(f));
			panel.getBlot().save(ps);
			ps.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void load(File f) { 
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			Blot b = new Blot(br);
			br.close();
			panel.setBlot(b);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Action createExitAction() { 
		return new AbstractAction("Exit") { 
			public void actionPerformed(ActionEvent e) { 
				System.exit(0);
			}
		};
	}
	
	public Action createSaveAction() { 
		return new AbstractAction("Save") {
			public void actionPerformed(ActionEvent e) {
				FileChooser chooser = new FileChooser(BlotViewFrame.this);
		        File f = chooser.choose();
		        if(f != null) { 
		        	save(f);
		        }
			} 
		};
	}
	
	public Action createLoadAction() { 
		return new AbstractAction("Open") {
			public void actionPerformed(ActionEvent e) {
				FileChooser chooser = new FileChooser(BlotViewFrame.this);
		        File f = chooser.choose();
		        if(f != null) { 
		        	load(f);
		        }
			} 
		};
	}
	
	public Action createSaveImageAction() { 
		return new AbstractAction("Save Image...") {
			public void actionPerformed(ActionEvent e) {
				saveImage();
			} 
		};
	}
	
	public JMenuBar createMenuBar() { 
		JMenuBar bar = new JMenuBar();
		JMenu menu = null;
		JMenuItem item = null;
		
		bar.add(menu = new JMenu("File"));
		menu.add(item = new JMenuItem(createLoadAction()));
		menu.add(item = new JMenuItem(createSaveAction()));
		menu.add(item = new JMenuItem(createSaveImageAction()));
		menu.add(item = new JMenuItem(createExitAction()));
		
		return bar;
	}
}
