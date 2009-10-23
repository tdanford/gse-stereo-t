/*
 * Author: tdanford
 * Date: May 10, 2008
 */
package edu.mit.csail.cgs.sigma.blots;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;

import javax.imageio.ImageIO;

public class Blot {
	
	private int width, height;
	private Vector<BlotColumn> cols;
	
	private File file;
	private BufferedImage image;
	
	public Blot(File f) throws IOException {
		file = f;
		image = ImageIO.read(file);
		width = image.getWidth();
		height = image.getHeight();
		cols = new Vector<BlotColumn>();
	}
	
	public Blot(BufferedReader br) throws IOException { 
		String line = br.readLine();
		file = new File(line);
		
		if(!file.exists()) { throw new IllegalArgumentException(line); }
		image = ImageIO.read(file);
		width = image.getWidth();
		height = image.getHeight();
		cols = new Vector<BlotColumn>();

		int numCols = Integer.parseInt(br.readLine().trim());
		for(int i = 0; i < numCols; i++) { 
			BlotColumn col = new BlotColumn(this, br.readLine());
			cols.add(col);
		}
	}
	
	public void save(PrintStream ps) {  
		ps.println(file.getAbsolutePath());
		ps.println(String.format("%d", cols.size()));
		for(BlotColumn c : cols) { 
			c.save(ps);
		}
	}
	
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public File getFile() { return file; }
	public Image getImage() { return image; }
	public int columns() { return cols.size(); }
	public BlotColumn getColumn(int i) { return cols.get(i); }
	public void addColumn(BlotColumn c) { cols.add(c); }
	
	public boolean containsPoint(Point p) { 
		return p.x >= 0 && p.y >= 0 && p.x <= width && p.y <= height;
	}
}
