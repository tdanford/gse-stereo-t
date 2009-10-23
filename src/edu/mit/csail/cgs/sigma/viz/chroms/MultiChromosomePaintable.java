/*
 * Author: tdanford
 * Date: Oct 9, 2008
 */
package edu.mit.csail.cgs.sigma.viz.chroms;

import java.util.*;
import java.awt.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.RefGeneGenerator;
import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.genes.*;
import edu.mit.csail.cgs.sigma.viz.chroms.information.DiffExpressionChromScorer;
import edu.mit.csail.cgs.sigma.viz.chroms.information.ExpressionChromScorer;
import edu.mit.csail.cgs.sigma.viz.chroms.information.GeneChromPainter;
import edu.mit.csail.cgs.viz.NonOverlappingIntervalLayout;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.paintable.*;
import edu.mit.csail.cgs.sigma.expression.BaseExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.NewExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.SudeepExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.ewok.ExpressionProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.ewok.StandardProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.ewok.StandardTwoChannelProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionTwoChannelProbe;
import edu.mit.csail.cgs.sigma.expression.simulation.*;

public class MultiChromosomePaintable extends AbstractPaintable {
	
	public static void main(String[] args) { 
		BaseExpressionProperties props = new NewExpressionProperties("new_sudeep");
		String expt = "sigma_mata_1";
		String strain = props.parseStrainFromExptKey(expt);
		Genome g = props.getGenome(strain);
		
		System.out.println("Building Genome viewer...");
		MultiChromosomePaintable mcp = new MultiChromosomePaintable(g);
		
		System.out.println("Adding gene tracks...");
		GeneGenerator gen = props.getGeneGenerator(strain);
		mcp.addFeatureTrack(gen, '+');
		mcp.addFeatureTrack(gen, '-');
		
		System.out.println("Adding expression tracks...");
		//ExpressionProbeGenerator prober = new StandardProbeGenerator(props, expt);
		//mcp.addExpressionProbeTrack(prober, '+');
		//mcp.addExpressionProbeTrack(prober, '-');
		
		StandardTwoChannelProbeGenerator prober = new StandardTwoChannelProbeGenerator(props, expt);
		mcp.addDiffExpressionTrack(prober, null);
		//mcp.addDiffExpressionTrack(prober, '-');
		
		PaintableFrame pf = new PaintableFrame(strain, mcp);
	}
	
	private Genome genome;
	private Map<String,ChromosomePaintable> chromPaintables;
	
	public MultiChromosomePaintable(Genome g) { 
		chromPaintables = new TreeMap<String,ChromosomePaintable>();
		genome = g;
		
		Collection<String> chroms = g.getChromList();
		int max = 0;
		for(String chrom : chroms) { 
			chromPaintables.put(chrom, new ChromosomePaintable(g, chrom));
			max = Math.max(max, chromPaintables.get(chrom).getLength());
			chromPaintables.get(chrom).addPaintableChangedListener(this);
		}
		
		for(String chrom : chroms) { 
			chromPaintables.get(chrom).setMaxLength(max);
		}
	}
	
	public int size() { return chromPaintables.size(); }
	public Genome getGenome() { return genome; }
	
	private boolean matches(Character s1, Character s2) {
		if(s1 != null && s2 != null) {
			return s1 == s2;
		} else { 
			return true;
		}
	}
	
	public void addFeatureTrack(Expander<Region,? extends Region> exp, Character str) { 
		setEventPassthrough(false);
		for(String chrom : chromPaintables.keySet()) {
			
			Region chromRegion = new Region(genome, chrom, 0, genome.getChromLength(chrom)-1);
			GeneChromPainter ptr = new GeneChromPainter(chromRegion, exp, str);
			chromPaintables.get(chrom).addPainterTrack(ptr);
		}
		
		setEventPassthrough(true);
		dispatchChangedEvent();
	}
	
	public void addExpressionProbeTrack(Expander<Region,ExpressionProbe> exp, Character str) { 
		setEventPassthrough(false);
		
		for(String chrom : chromPaintables.keySet()) {
			ChromosomePaintable cpainter = chromPaintables.get(chrom);
			
			Region chromRegion = new Region(genome, chrom, 0, genome.getChromLength(chrom)-1);
			ExpressionChromScorer s = new ExpressionChromScorer(chromRegion,exp, str);

			for(int i = 0; i < cpainter.getNumPainters(); i++) {
				ChromPainter cp = cpainter.getPainter(i);
				if(matches(cp.strand(), s.strand())) { 
					if(cp instanceof GeneChromPainter) { 
						GeneChromPainter gcp = (GeneChromPainter)cp;
						s.filter(gcp.features());
					}
				}
			}
			
			chromPaintables.get(chrom).addScorerTrack(s);
		}
		
		setEventPassthrough(true);
		dispatchChangedEvent();
	}
	
	public void addDiffExpressionTrack(Expander<Region,ExpressionTwoChannelProbe> exp, Character str) {
		
		setEventPassthrough(false);
		
		for(String chrom : chromPaintables.keySet()) {
			ChromosomePaintable cpainter = chromPaintables.get(chrom);
			
			Region chromRegion = new Region(genome, chrom, 0, genome.getChromLength(chrom)-1);
			DiffExpressionChromScorer s = new DiffExpressionChromScorer(chromRegion,exp, str);
			
			LinkedList<Collection<Region>> regs = new LinkedList<Collection<Region>>();

			for(int i = 0; i < cpainter.getNumPainters(); i++) {
				ChromPainter cp = cpainter.getPainter(i);
				if(cp instanceof GeneChromPainter) { 
					GeneChromPainter gcp = (GeneChromPainter)cp;
					regs.addLast(gcp.features());
				}
			}

			s.filterAll(regs);
			chromPaintables.get(chrom).addScorerTrack(s);
		}
		
		setEventPassthrough(true);
		dispatchChangedEvent();
	}
	
	public void addLocationTrack(ChromLocations loc) {
		setEventPassthrough(false);
		for(String chrom : chromPaintables.keySet()) { 
			chromPaintables.get(chrom).addLocationTrack(loc);
		}
		
		setEventPassthrough(true);
		dispatchChangedEvent();
	}
	
	public void addPainterTrack(ChromPainter ptr) { 
		setEventPassthrough(false);
		for(String chrom : chromPaintables.keySet()) { 
			chromPaintables.get(chrom).addPainterTrack(ptr);
		}
		
		setEventPassthrough(true);
		dispatchChangedEvent();
	}
	
	public void addScorerTrack(ChromScorer s) { 
		setEventPassthrough(false);
		for(String chrom : chromPaintables.keySet()) { 
			chromPaintables.get(chrom).addScorerTrack(s);
		}
		
		setEventPassthrough(true);
		dispatchChangedEvent();
	}
	
	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2-x1, h = y2-y1;
		int tracks = chromPaintables.size()+1;

		int trackHeight = (int)Math.floor((double)h / (double)tracks);
		int trackSpacing = (int)Math.floor((double)trackHeight / (double)tracks);
		
		/*
		 * Paint background.
		 */
		g.setColor(Color.white);
		g.fillRect(x1, y1, w, h);
		
		/*
		 * Paint chromosomes, in rows. 
		 */
		int cy = y1 + trackSpacing;
		
		for(String chrom : chromPaintables.keySet()) { 
			ChromosomePaintable cp = chromPaintables.get(chrom);
			cp.paintItem(g, x1, cy, x2, cy+trackHeight);

			g.setColor(Color.black);
			g.drawString(chrom, x1+2, cy+trackHeight+trackSpacing);
			
			cy += trackHeight + trackSpacing;
		}
	}
}
