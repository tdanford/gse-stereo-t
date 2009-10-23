/**
 * 
 */
package edu.mit.csail.cgs.sigma.viz;

import java.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.motifs.WeightMatrix;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.expression.BaseExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.SudeepExpressionProperties;
import edu.mit.csail.cgs.sigma.genes.GeneAnnotationProperties;
import edu.mit.csail.cgs.sigma.motifs.MotifProperties;
import edu.mit.csail.cgs.sigma.motifs.Motifs;

/**
 * @author tdanford
 *
 */
public class SigmaRegionPainter extends RegionPainter {
	
	private Vector<RegionPainter> singlePainters;
	private Vector<ExpressionPainter> fgexprs, bgexprs;
    private Vector<DivergentSitePainter> dsPainters;

    private GenePainter genes;
	private MotifRegionPainter motifPainter;
	private MotifScorePainter motifScorePainter;
	
	private SigmaProperties sprops;
	private GeneAnnotationProperties gaps;

	private String strain;
	private MotifProperties mprops;
	private Motifs motifs;
    private boolean displayBgChannel;
    private int lastHeight;
	
	public SigmaRegionPainter(SigmaProperties eps, String strain) { 
        displayBgChannel = true;
		sprops = eps;
		gaps = new GeneAnnotationProperties(sprops, "default");
		mprops = new MotifProperties(sprops, "default");
		this.strain = strain;
		lastHeight = 1;
		
		motifs = new Motifs(mprops, strain);
        motifs.loadData();
        
        singlePainters = new Vector<RegionPainter>();
        fgexprs = new Vector<ExpressionPainter>();
        bgexprs = new Vector<ExpressionPainter>();
        dsPainters = new Vector<DivergentSitePainter>();
		
		genes = new GenePainter(gaps, strain);
		//genes.setPaintGenes(false);
        
		motifPainter = new MotifRegionPainter(motifs);
		motifScorePainter = new MotifScorePainter(null, Color.red);
	}
	
	public void addSingleExprPainter(RegionPainter ptr) { 
		singlePainters.add(ptr);
		dispatchChangedEvent();
	}
	
	public void addExprExpt(ExprExptSelector sel) {
		BaseExpressionProperties eprops = sel.getProps();
		String exptKey = sel.getExptKey();
		
		if(!strain.equals(eprops.parseStrainFromExptKey(exptKey))) { 
			throw new IllegalArgumentException(exptKey);
		}
		
        ExpressionPainter fgexpr = null; 
        ExpressionPainter bgexpr = null; 
        
        if(exptKey.endsWith("IME4")) { 
        	fgexpr = new ExpressionPainter(eprops, exptKey, true);
        	bgexpr = new ExpressionPainter(eprops, "original_sigma_mat_a", false);        	
        } else { 
        	fgexpr = new ExpressionPainter(eprops, exptKey, false);
        	bgexpr = new ExpressionPainter(eprops, exptKey, true);
        }
        
        fgexpr.setLogScale(true); 
        bgexpr.setLogScale(true);
        
        for(String key : eprops.getArrayExptKeys(exptKey)) { 
        	fgexpr.addTranscripts(eprops.getExpressionTranscriptFile(key));
        }

        DivergentSitePainter dsPainter = new DivergentSitePainter(eprops, exptKey);

        fgexprs.add(fgexpr);
        bgexprs.add(bgexpr);
        
        //fgexpr.setPaintLabel(false);
        bgexpr.setLabel("");
        
        dsPainters.add(dsPainter);
	}
	
	public Action createChangeLabelAction(ExpressionPainter pp) { 
		return new LabelChangeDialogAction(pp);
	}
	
	private class LabelChangeDialogAction extends AbstractAction {
		private Labeled labeled;
		public LabelChangeDialogAction(Labeled l) {
			super("Change Label");
			labeled = l;
		}
		public void actionPerformed(ActionEvent e) { 
			LabelChangeDialog dlg = new LabelChangeDialog(null, labeled);
			SwingUtilities.invokeLater(dlg);
		}
	}
	
	public ExpressionPainter getExpressionPainter(Point p) { 
		int h = lastHeight / fgexprs.size();
		int idx = p.y / h;
		return fgexprs.get(idx);
	}
	
	public void makeSelection(Point p) {
		int h = lastHeight / fgexprs.size();
		int idx = p.y / h;
		int y1 = idx * h;
		fgexprs.get(idx).addSelection(new Point(p.x, p.y-y1));
	}
	
	public void clearSelections() {
		for(int i = 0; i < fgexprs.size(); i++) { 
			fgexprs.get(i).clearSelections();
		}
	}
	
	public void setRegion(Region r) { 
		super.setRegion(r);
		
		for(int i = 0; i < singlePainters.size(); i++) { 
			singlePainters.get(i).setRegion(r);
		}
        
		for(int i = 0; i < fgexprs.size(); i++) { 
			fgexprs.get(i).setRegion(r);
        	bgexprs.get(i).setRegion(r); 
            fgexprs.get(i).rectifyMaxima(bgexprs.get(i));

            if(dsPainters.get(i) != null) { 
            	dsPainters.get(i).setRegion(r);
            }
		}
		
		motifPainter.setRegion(r);
		genes.setRegion(r);
        motifScorePainter.setRegion(r);
	}

	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.viz.RegionPainter#doLayout()
	 */
	@Override
	public void doLayout() {
		for(int i = 0; i < singlePainters.size(); i++) { 
			singlePainters.get(i).doLayout();
		}
		
		for(int i = 0; i < fgexprs.size(); i++) { 
			fgexprs.get(i).doLayout();
        	bgexprs.get(i).doLayout();
        	
        	if(dsPainters.get(i) != null) { 
        		dsPainters.get(i).doLayout();
        	}
		}
		genes.doLayout();
		motifPainter.doLayout();
		motifScorePainter.doLayout();
	}

	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.viz.RegionPainter#isReadyToPaint()
	 */
	@Override
	public boolean isReadyToPaint() {
		for(int i = 0 ; i < singlePainters.size(); i++) { 
			if(!singlePainters.get(i).isReadyToPaint()) { 
				return false;
			}
		}
		
		for(int i = 0; i < fgexprs.size(); i++) { 
			if(!fgexprs.get(i).isReadyToPaint()) { return false; }
			if(!bgexprs.get(i).isReadyToPaint()) { return false; }
		}
		return genes.isReadyToPaint() && motifPainter.isReadyToPaint() && 
            motifScorePainter.isReadyToPaint();
	}

	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.viz.RegionPainter#paintRegion(java.awt.Graphics2D, int, int, int, int)
	 */
	@Override
	public void paintRegion(Graphics2D g, int x1, int y1, int w, int hh) {
        g.setColor(Color.white);
        g.fillRect(x1, y1, w, hh);
        lastHeight = hh;

        int h = hh / Math.max(1, (fgexprs.size() + singlePainters.size()));
        int bottom = y1 + hh - h;
        
		int geneHeight = Math.max(h/2, 30);
		int h2 = h/2;
		motifPainter.paintItem(g, x1, bottom+h2-geneHeight/2, x1+w, bottom+h2+geneHeight/2);
        genes.paintItem(g, x1, bottom+h2-geneHeight/2, x1+w, bottom+h2+geneHeight/2);
        
        motifScorePainter.paintItem(g, x1, bottom+h2-geneHeight/2, x1+w, bottom+h2-geneHeight/2-geneHeight);

        bottom = y1;
        
        for(int i = 0; i < singlePainters.size(); i++, bottom += h) { 
        	singlePainters.get(i).paintItem(g, x1, bottom, x1+w, bottom+h);
        }
        
        for(int i = 0; i < fgexprs.size(); i++, bottom += h) { 
        	if(displayBgChannel) { 
        		bgexprs.get(i).paintItem(g, x1, bottom, x1+w, bottom+h);
        	}
        	fgexprs.get(i).paintItem(g, x1, bottom, x1+w, bottom+h);
        	if(dsPainters.get(i) != null) { 
        		dsPainters.get(i).paintItem(g, x1, 
        				bottom+h2-geneHeight/2, x1+w, bottom+h2+geneHeight/2);
        	}        		
        }
	}
    
    public void setDisplayBgChannel(boolean v) { 
        displayBgChannel = v;
        dispatchChangedEvent();
    }

	public Motifs getMotifs() { return motifs; } 
	public MotifRegionPainter getMotifPainter() { return motifPainter; } 
	public MotifScorePainter getMotifScorePainter() { return motifScorePainter; }

	public Collection<WeightMatrix> getMatrices() { 
		return motifs.getMatrices();
	}
}
