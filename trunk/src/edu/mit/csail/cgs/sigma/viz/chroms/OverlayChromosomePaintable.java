/*
 * Author: tdanford
 * Date: Feb 19, 2009
 */
package edu.mit.csail.cgs.sigma.viz.chroms;

import java.util.*;
import java.awt.*;

import edu.mit.csail.cgs.viz.paintable.AbstractPaintable;

public class OverlayChromosomePaintable extends AbstractPaintable {
	
	private int size;
	private Map<ChromInformation,ArrayList<ChromInformationRenderer>> info;
	
	public OverlayChromosomePaintable() {
		size = 0;
		info = new LinkedHashMap<ChromInformation,ArrayList<ChromInformationRenderer>>();
	}
	
	public void recalculate(int units) { 
		for(ChromInformation information : info.keySet()) {
			information.recalculate(units);
		}
	}
	
	public void addInformation(ChromInformation inf, ChromInformationRenderer rend) {
		if(!info.containsKey(inf)) { 
			info.put(inf, new ArrayList<ChromInformationRenderer>());
		}
		info.get(inf).add(rend);
		size += 1;
	}

	public int size() {
		return size; 
	}
	
	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		Graphics2D g2 = (Graphics2D)g;
		
		int w = x2-x1, h = y2-y1;
		
		for(ChromInformation information : info.keySet()) { 
			for(ChromInformationRenderer renderer : info.get(information)) { 
				renderer.paintInformation(information, g2, x1, y1, x2, y2);
			}
		}
	}

}
