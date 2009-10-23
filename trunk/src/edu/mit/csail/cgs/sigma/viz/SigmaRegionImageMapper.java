/*
 * Author: tdanford
 * Date: May 9, 2008
 */
package edu.mit.csail.cgs.sigma.viz;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.awt.*;

import edu.mit.csail.cgs.datasets.general.NamedRegion;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.ewok.verbs.MapperIterator;
import edu.mit.csail.cgs.sigma.Parser;
import edu.mit.csail.cgs.sigma.expression.BaseExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.SudeepExpressionProperties;

public class SigmaRegionImageMapper implements Mapper<Region,File> {
	
	public static void main(String[] args) {
		BaseExpressionProperties props = new SudeepExpressionProperties();
		String exptKey = args[0];
		File input = new File(args[1]);
		file_images(props, exptKey, input);
	}
	
	public static void file_images(BaseExpressionProperties props, String exptKey, File input) {
		File current = new File(".");
		int width = 800, height = 600;
		
		SigmaRegionImageMapper imager = new SigmaRegionImageMapper(props, current, width, height);
		imager.loadData(exptKey);
		
		try {
			Genome genome = props.getGenome(props.parseStrainFromExptKey(exptKey));
			Mapper<String,Region> decoder = new RegionDecoder(genome);
			Parser<Region> regions = new Parser<Region>(input, decoder);
			Iterator<File> images = new MapperIterator<Region,File>(imager, regions);
			while(images.hasNext()) { 
				File f = images.next();
				System.out.println(String.format("IMAGE: %s", f.getName()));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		imager.closeData();
	}
	
	public static class RegionDecoder implements Mapper<String,Region> { 
		private Pattern pattern = Pattern.compile("([^:]+):(\\d+)-(\\d+)");
		private Genome genome;
		public RegionDecoder(Genome g) { genome = g; }
		
		public Region execute(String a) { 
			Matcher m = pattern.matcher(a);
			String chrom = m.group(1);
			int start = Integer.parseInt(m.group(2));
			int end = Integer.parseInt(m.group(3));
			String[] array = a.split("\\s+");
			if(array.length > 1) { 
				String name = array[1];
				return new NamedRegion(genome, chrom, start, end, name);
			} else { 
				return new Region(genome, chrom, start, end);
			}
		}
	}
	
	private BaseExpressionProperties props;
	private SigmaRegionPainter painter;
	private String exptKey;
	private File baseDir;
	
	private int imageCount;
	private int width, height;
	
	public SigmaRegionImageMapper(BaseExpressionProperties bps, File dir, int w, int h) { 
		props = bps;
		baseDir = dir;
		imageCount = 0;
		width = w;
		height = h;
	}
	
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public void setWidth(int w) { width = w; }
	public void setHeight(int h) { height = h; }
	
	public void loadData(String ek) {
		exptKey = ek;
		painter = new SigmaRegionPainter(props.getSigmaProperties(), 
				props.parseStrainFromExptKey(exptKey));
		painter.addExprExpt(new ExprExptSelector(props, exptKey));
	}
	
	public void closeData() { 
		exptKey = null;
		painter = null;
	}

	public File execute(Region a) {
		painter.setRegion(a);
		painter.doLayout();
		
		String fname = String.format("%s_%dk_%dk", a.getChrom(), a.getStart()/1000, a.getEnd()/1000);
		if(a instanceof NamedRegion) { 
			NamedRegion nr = (NamedRegion)a;
			fname = nr.getName() + "_" + fname;
		}
		String filename = String.format("%s.%d.png", fname, imageCount++);
		File output = new File(baseDir, filename);
		
		try {
			painter.saveImage(output, width, height, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return output;
	}

}
