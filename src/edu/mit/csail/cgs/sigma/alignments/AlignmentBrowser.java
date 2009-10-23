package edu.mit.csail.cgs.sigma.alignments;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.sql.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.datasets.alignments.*;
import edu.mit.csail.cgs.ewok.verbs.RefGeneGenerator;
import edu.mit.csail.cgs.sigma.GeneGenerator;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.utils.database.UnknownRoleException;

public class AlignmentBrowser implements edu.mit.csail.cgs.utils.Closeable {
	
	private static Pattern regionPattern = Pattern.compile("([^:]+):(\\d+)-(\\d+)");
	
	public static void main(String[] args) { 
		try {
			interactive();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void interactive() throws SQLException, IOException { 
		AlignmentProperties props = new AlignmentProperties();
		AlignmentBrowser browser = new AlignmentBrowser(props);
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line;
		String strain = "s288c";
		System.out.print(">"); System.out.flush();
		SigmaProperties sprops = props.getSigmaProperties();
		GeneGenerator sigmaGen = sprops.getGeneGenerator(strain);
		Genome genome = sprops.getGenome(strain);
		
		while((line = br.readLine()) != null) { 
			String geneName = line.trim();
			if(geneName.length() > 0) {
				Region query = null;
				
				Matcher m = regionPattern.matcher(geneName);
				if(m.matches()) { 
					String chrom = m.group(1);
					int start = Integer.parseInt(m.group(2));
					int end = Integer.parseInt(m.group(3));
					if(start <= end) { 
						query = new Region(genome, chrom, start, end);
					}
				} else { 
					Iterator<Gene> genes = sigmaGen.byName(geneName);
					if(genes.hasNext()) { 
						query = genes.next();
					}
				}
				
				if(query != null) { 
					Collection<MultipleAlignment> aligns = browser.findAlignmentsOverlapping(query);
					
					for(MultipleAlignment align : aligns) { 
						System.out.println(align.toString());
						System.out.println();
					}
				}
			}
			System.out.print(">"); System.out.flush();
		}
	}
	
	private AlignmentProperties props;
	private AlignmentLoader loader;
	private AlignmentVersion version;
	
	public AlignmentBrowser(AlignmentProperties ps) throws SQLException { 
		props = ps;
		loader = new AlignmentLoader();
		String versionName = props.getAlignmentVersionName();
		version = loader.loadAlignmentVersion(versionName);
	}
	
	public Collection<MultipleAlignment> findAlignmentsOverlapping(Region r) { 
		LinkedList<MultipleAlignment> aligns = new LinkedList<MultipleAlignment>();
		
		Collection<AlignBlock> blocks = version.getAlignBlocks(r);
		Set<Alignment> alignments = new LinkedHashSet<Alignment>();
		Vector<Integer[]> indices = new Vector<Integer[]>();
		
		for(AlignBlock block : blocks) { 
			Alignment a = block.getAlignment();
			alignments.add(a);
			int j1 = Math.max(r.getStart(), block.getStartPos());
			int j2 = Math.min(r.getEnd(), block.getStopPos());
			indices.add(new Integer[] { j1, j2 });
		}
		
		int i = 0;
		for(Alignment a : alignments) { 
			Integer[] inds = indices.get(i++);
			aligns.add(new MultipleAlignment(a).subAlignment(inds[0], inds[1]));
		}
		
		return aligns;
	}
	
	public void close() { 
		loader.close();
		loader = null;
		version = null;
	}
	
	public boolean isClosed() { 
		return loader == null;
	}
}
