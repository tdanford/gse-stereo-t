/*
 * Author: tdanford
 * Date: Mar 10, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.viz.genome;

import java.util.*;
import java.awt.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.InputSegmentation;
import edu.mit.csail.cgs.sigma.viz.chroms.*;
import edu.mit.csail.cgs.sigma.viz.chroms.information.*;
import edu.mit.csail.cgs.sigma.viz.chroms.renderers.*;
import edu.mit.csail.cgs.viz.paintable.AbstractPaintable;
import edu.mit.csail.cgs.viz.paintable.PaintableFrame;
import edu.mit.csail.cgs.ewok.verbs.*;

public class ChromosomeView extends AbstractPaintable {

	private Genome genome;
	private String chrom;
	private int chromLength, maxChromLength;
	
	private SimpleChromosomePaintable chromPaintable;
	
	public ChromosomeView(Genome g, String c) throws IOException {
		
		genome = g;
		chrom = c;
		chromLength = genome.getChromLength(chrom);
		maxChromLength = chromLength;
		for(String chr : genome.getChromList()) { 
			maxChromLength = Math.max(maxChromLength, genome.getChromLength(chr));
		}
	
		chromPaintable = new SimpleChromosomePaintable(chromLength);
		chromPaintable.addPaintableChangedListener(this);
	}
	
	public int getChromLength() { return chromLength; }
	public Region getRegion() { return new Region(genome, chrom, 0, chromLength); }
	
	public void addUpperInformation(ChromInformation ci, ChromInformationRenderer cr) {   
		chromPaintable.addUpperInformation(ci, cr);
	}

	public void addMiddleInformation(ChromInformation ci, ChromInformationRenderer cr) {   
		chromPaintable.addMiddleInformation(ci, cr);
	}

	public void addLowerInformation(ChromInformation ci, ChromInformationRenderer cr) {   
		chromPaintable.addLowerInformation(ci, cr);
	}

	public PaintableFrame getFrame() { 
		return new PaintableFrame("Chromosome Painter", chromPaintable);
	}
	
	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) { 
		double chromFrac = (double)chromLength  /(double)maxChromLength;
		//int w = (int)Math.round((double)(x2-x1) * chromFrac);
		int w = x2-x1;
		int h = y2-y1;
		chromPaintable.paintItem(g, x1, y1, x1 + w, y1 + h);
	}
}


