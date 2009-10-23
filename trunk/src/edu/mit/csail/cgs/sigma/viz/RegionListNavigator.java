/**
 * @author Timothy Danford
 */
package edu.mit.csail.cgs.sigma.viz;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.species.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class RegionListNavigator extends JFrame {

	private ArrayList<Location> locs;
	private LinkedList<NavigationListener> listeners;
	private DefaultListModel listModel;
	private JList list;
	
	public RegionListNavigator(File f) throws IOException { 
		this();
		parseFile(f);
	}
	
	public RegionListNavigator() { 
		super("Region List");
		locs = new ArrayList<Location>();
		listeners = new LinkedList<NavigationListener>();
		
		listModel = new DefaultListModel();

		list = new JList(listModel);
		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BorderLayout());
		listPanel.add(new JScrollPane(list), BorderLayout.CENTER);
		
		listPanel.setPreferredSize(new Dimension(200, 500));
		Container c = (Container)getContentPane();
		c.setLayout(new BorderLayout());
		c.add(listPanel, BorderLayout.CENTER);
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		list.addMouseListener(new MouseAdapter() { 
			public void mouseClicked(MouseEvent e) { 
				System.out.println(String.format("Click: %s", e.toString()));
				if(e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) { 
					int idx = list.locationToIndex(e.getPoint());
					System.out.println(String.format("\tidx: %d", idx));
					fireLocationSelection(idx);
				}
			}
		});
	}
	
	public void addNavigationListener(NavigationListener list) { 
		listeners.addLast(list);
	}
	
	public void removeNavigationListener(NavigationListener lst) { 
		listeners.remove(lst);
	}
	
	private void fireLocationSelection(int idx) { 
		if(idx < 0 || idx >= locs.size()) { 
			throw new IllegalArgumentException(String.format("Illegal location idx: %d", idx));
		}
		Location loc = locs.get(idx);
		for(NavigationListener listener : listeners) { 
			listener.navigateTo(loc.chrom, loc.start, loc.end);
		}
	}
	
	public void display() { 
		SwingUtilities.invokeLater(new Runnable() { 
			public void run() { 
				Point p = getLocation();
				setLocation(p.x, p.y + 50);
				setVisible(true);
				pack();
			}
		});
	}

	private void parseFile(File f) throws IOException { 
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line; 
		while((line = br.readLine()) != null) { 
			try { 
				Location loc = new Location(line);
				locs.add(loc);
				listModel.addElement(loc);
			} catch(IllegalArgumentException e) { 
				System.err.println(e.getMessage());
			}
		}
		br.close();
	}
	
	private static Pattern locationPattern = Pattern.compile("([^:\\s]+):(\\d+)-(\\d+)");
		
	private class Location { 
		public String chrom;
		public Integer start, end;
		
		public Location(String line) { 
			Matcher m = locationPattern.matcher(line);
			if(!m.find()) { 
				throw new IllegalArgumentException(String.format("Can't find location in string \"%s\"", line));
			}
			
			chrom = m.group(1);
			start = Integer.parseInt(m.group(2));
			end = Integer.parseInt(m.group(3));
		}
		
		public int hashCode() { 
			int code = 17;
			code += chrom.hashCode(); code *= 37;
			code += start; code *= 37;
			code += end; code *= 37;
			return code;
		}
		
		public boolean equals(Object o) { 
			if(!(o instanceof Location)) { return false; }
			Location loc = (Location)o;
			return loc.chrom.equals(chrom) && start.equals(loc.start) && end.equals(loc.end);
		}
		
		public String toString() { 
			return String.format("%s:%d-%d", chrom, start, end);
		}
	}
	
	
}
