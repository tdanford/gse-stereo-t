/*
 * Author: tdanford
 * Date: Jun 26, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.awt.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.viz.paintable.*;

public class GridFigurePainter<Ptb extends Paintable> extends AbstractPaintable {

	public static void main(String[] args) { 
		grid_viz(args);
	}
	
	public static void grid_viz(String[] args) { 
		try {
			WorkflowProperties props = new WorkflowProperties();
			
			//String key = "s288c";
			String key = args.length > 0 ? args[0] : "txns288c";
			String expt = args.length > 1 ? args[1] : "matalpha";
			
			String strain = props.parseStrainFromKey(key);
			
			// mat-a, mat-alpha, diploid
			
			MultiViz viz = new MultiViz(props, key);

			Genome genome = props.getSigmaProperties().getGenome(strain);

			Region region = new Region(genome, "11", 0, 10000);
			viz.setRegion(region);

			//SigmaGraph segGraph = NewSigmaGraph.loadGraph(viz.whole, expt);
			//SigmaGraphViz Viz = new SigmaGraphViz(segGraph, region);
			
			int idx = -1;

			idx = viz.addDataSegmentChannel(expt);
			
			if(viz.mvProps.showSteinmetz) { 
				viz.addSteinmetzData();
			}
			
			if(viz.mvProps.showMiura) { 
				viz.addMiuraData();
			}
			
			//viz.dsChannels.get(idx).loadTranscripts(testtrans);
			
			File dir = new File("C:\\Documents and Settings\\tdanford\\Desktop");
			File input = new File(dir, "figure-region-list.txt");
			
			GridFigurePainter gfp = new GridFigurePainter(viz);
			gfp.loadParamFile(input);
			
			boolean raster = true;
			File output =new File(dir, "grid.png");
			gfp.saveImage(output, 1000, 1000, raster);
 			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Paintable paintable;
	private ParamPaintable params;
	private String[] paramArray;
	
	public GridFigurePainter(Ptb p, ParamPaintable<Ptb> pp) { 
		paintable = p;
		params = pp;
		paramArray = null;
	}
	
	public GridFigurePainter(Paintable p) { 
		if(p instanceof AbstractRegionPaintable) { 
			paintable = p;
			params = new RegionParamPaintable((AbstractRegionPaintable)p);
			paramArray = null;
		} else { 
			throw new IllegalArgumentException();
		}
	}
	
	public void loadParamFile (File f) throws IOException { 
		paramArray = loadParams(f).toArray(new String[0]);
	}
	
	public int dim() { 
		int n = Math.max(1, paramArray != null ? paramArray.length : 0);
		int dn = (int)Math.round(Math.sqrt((double)n));
		if(paramArray != null && dn*dn < paramArray.length) { 
			dn += 1;
		}
		return dn;
	}
	
	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int n = dim();
		paintGrid(g, x1, y1, x2, y2, n, n);
	}
	
	public void paintGrid(Graphics g, int x1, int y1, int x2, int y2, int rows, int cols) {
		int w = x2-x1, h = y2-y1;
		
		int rowHeight = (int)Math.floor((double)h / (double)Math.max(1, rows));
		int colWidth = (int)Math.floor((double)w / (double)Math.max(1, cols));
		int dim = Math.max(10, Math.min(rowHeight, colWidth));
		rowHeight = colWidth = dim;
		
		g.setColor(Color.white);
		g.fillRect(x1, y1, w, h);
		
		for(int i = 0, r = 0; r < rows; r++) { 
			for(int c = 0; c < cols; c++, i++) { 
				int gx1 = x1 + c * colWidth;
				int gy1 = y1 + r * rowHeight;
				int gx2 = gx1 + colWidth;
				int gy2 = gy1 + rowHeight;
				params.setParam(paramArray[i]);
				paintable.paintItem(g, gx1, gy1, gx2, gy2);
				System.out.println(String.format("Painted: %s", paramArray[i]));
			}
		}
		
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.black);
		Stroke str = g2.getStroke();
		g2.setStroke(new BasicStroke(2.0f));
		
		for(int i = 0, r = 1; r < rows; r++) { 
			for(int c = 1; c < cols; c++, i++) { 
				int gx1 = x1 + c * colWidth;
				int gy1 = y1 + r * rowHeight;
				int gx2 = gx1 + colWidth;
				int gy2 = gy1 + rowHeight;

				if(r == 1) { 
					g2.drawLine(gx1, y1, gx1, y2);
				}
				
				if(c == 1) { 
					g2.drawLine(x1, gy1, x2, gy1);
				}
			}
		}
		g2.drawRect(x1+2, y1+2, w-4, h-4);
		
		g2.setStroke(str);
	}
	
	private Collection<String> loadParams(File f) throws IOException { 
		LinkedList<String> pms = new LinkedList<String>();
		String line;
		BufferedReader br = new BufferedReader(new FileReader(f));
		while((line = br.readLine()) != null) { 
			line = line.trim();
			if(line.length() > 0) { 
				pms.addLast(line);
			}
		}
		br.close();
		return pms;
	}
	
	public static interface ParamPaintable<P extends Paintable> {
		public void setParam(String prm);
	}
	
	public static class RegionParamPaintable implements ParamPaintable<AbstractRegionPaintable> {
		
		private Pattern regionPattern;
		private AbstractRegionPaintable paintable;
		
		public RegionParamPaintable(AbstractRegionPaintable p) { 
			this(p, "([^:]+):(\\d+)-(\\d+).*");
		}
		
		public RegionParamPaintable(AbstractRegionPaintable p, String pt) { 
			paintable = p;
			regionPattern = Pattern.compile(pt);
		}

		public void setParam(String prm) {
			Matcher m = regionPattern.matcher(prm);
			if(!m.matches()) { throw new IllegalArgumentException(prm); }
			String chrom = m.group(1);
			int start = Integer.parseInt(m.group(2));
			int end = Integer.parseInt(m.group(3));
			Genome g= paintable.getRegion().getGenome();
			paintable.setRegion(new Region(g, chrom, start, end));
		} 
	}
}
