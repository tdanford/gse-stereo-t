package edu.mit.csail.cgs.sigma.alignments;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;

public class SNPParser {
	
	private static Pattern strainPattern = Pattern.compile("(S[^_]+)_chr(.*)");
	
	private static String[] parseStrainChrom(String elmt) { 
		Matcher m = strainPattern.matcher(elmt);
		if(!m.matches()) { throw new IllegalArgumentException(elmt); }
		return new String[] { m.group(1), m.group(2) };
	}
	
	private LinkedList<SNP> snps;
	
	public SNPParser(WorkflowProperties ps) throws IOException { 
		this(new File(ps.getDirectory(), "SGDv1.Sigmav6.SNPs"));
	}
	
	public SNPParser(File f) throws IOException {
		snps = new LinkedList<SNP>();
		
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		
		while((line = br.readLine()) != null) { 
			String[] array = line.split("\\s+");
			String[] p1 = parseStrainChrom(array[0]);
			int loc1 = Integer.parseInt(array[1]);
			String[] p2 = parseStrainChrom(array[2]);
			int loc2 = Integer.parseInt(array[3]);
			
			SNP snp1 = new SNP(p1[0], p1[1], loc1);
			SNP snp2 = new SNP(p2[0], p2[1], loc2);
			
			snps.add(snp1);
			snps.add(snp2);
		}
		
		br.close();
	}

	public Iterator<SNP> allSNPs() { return snps.iterator(); }
	
	public Iterator<SNP> strainSNPs(String strain) { 
		return new FilterIterator<SNP,SNP>(new StrainFilter(strain), snps.iterator());
	}
	
	public Expander<Region,SNP> snpExpander(String strain) { 
		return new SNPOverlapExpander(strain);
	}
	
	private static class StrainFilter implements Filter<SNP,SNP> {
		private String strain;
		
		public StrainFilter(String str) { strain = str; }
		
		public SNP execute(SNP s) { 
			return s.strain.equals(strain) ? s : null;
		}
	}
	
	private class SNPOverlapExpander implements Expander<Region,SNP> { 
		private String strain;
		public SNPOverlapExpander(String str) { 
			strain = str;
		}
		
		public Iterator<SNP> execute(Region query) { 
			return new FilterIterator<SNP,SNP>(new SNPOverlapFilter(query), strainSNPs(strain));
		}
	}
	
	private static class SNPOverlapFilter implements Filter<SNP,SNP> { 
		private Region query;
		public SNPOverlapFilter(Region r) { query = r; }
		public SNP execute(SNP value) { 
			return value.overlaps(query) ? value : null;
		}
	}


}

