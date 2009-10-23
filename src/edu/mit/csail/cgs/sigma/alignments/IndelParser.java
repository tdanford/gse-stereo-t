package edu.mit.csail.cgs.sigma.alignments;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;

public class IndelParser {
	
	private static Pattern strainPattern = Pattern.compile("(S[^_]+)_chr(.*)");
	
	private static String[] parseStrainChrom(String elmt) { 
		Matcher m = strainPattern.matcher(elmt);
		if(!m.matches()) { throw new IllegalArgumentException(elmt); }
		return new String[] { m.group(1), m.group(2) };
	}
	
	private LinkedList<Insertion> insertions;
	private LinkedList<Deletion> deletions;
	
	public IndelParser(WorkflowProperties ps) throws IOException { 
		this(new File(ps.getDirectory(), "SGDv1.Sigmav6.Indels"));
	}
	
	public IndelParser(File f) throws IOException {
		insertions = new LinkedList<Insertion>();
		deletions = new LinkedList<Deletion>();
		
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		
		while((line = br.readLine()) != null) { 
			String[] array = line.split("\\s+");
			
			boolean isInsert1 = array[0].equals("i");			
			String[] p1 = parseStrainChrom(array[1]);
			int loc1 = Integer.parseInt(array[2]);
			
			boolean isInsert2 = array[3].equals("i");						
			String[] p2 = parseStrainChrom(array[4]);
			int loc2 = Integer.parseInt(array[5]);
			
			int length = Integer.parseInt(array[6]);

			if(isInsert1) { 
				insertions.add(new Insertion(p1[0], p1[1], loc1, length));
			} else { 
				deletions.add(new Deletion(p1[0], p1[1], loc1, length));				
			}
			if(isInsert2) { 
				insertions.add(new Insertion(p2[0], p2[1], loc2, length));
			} else { 
				deletions.add(new Deletion(p2[0], p2[1], loc2, length));				
			}
		}
		
		br.close();
	}
	
	public Iterator<Insertion> allInsertions() { return insertions.iterator(); }
	public Iterator<Deletion> allDeletions() { return deletions.iterator(); }
	
	public Iterator<Insertion> strainInsertions(String strain) { 
		return new FilterIterator<Insertion,Insertion>(new InsertionStrainFilter(strain), allInsertions());
	}
	
	public Iterator<Deletion> strainDeletions(String strain) { 
		return new FilterIterator<Deletion,Deletion>(new DeletionStrainFilter(strain), allDeletions());
	}
	
	public Expander<Region,Insertion> insertionOverlapExpander(String strain) { 
		return new InsertionOverlapExpander(strain);
	}
	
	public Expander<Region,Deletion> deletionOverlapExpander(String strain) { 
		return new DeletionOverlapExpander(strain);
	}
	
	private class InsertionOverlapExpander implements Expander<Region,Insertion> { 
		private String strain;
		public InsertionOverlapExpander(String str) { 
			strain = str;
		}
		
		public Iterator<Insertion> execute(Region query) { 
			return new FilterIterator<Insertion,Insertion>(new InsertionOverlapFilter(query), strainInsertions(strain));
		}
	}
	
	private class DeletionOverlapExpander implements Expander<Region,Deletion> { 
		private String strain;
		public DeletionOverlapExpander(String str) { 
			strain = str;
		}
		
		public Iterator<Deletion> execute(Region query) { 
			return new FilterIterator<Deletion,Deletion>(new DeletionOverlapFilter(query), strainDeletions(strain));
		}
	}
	
	private static class InsertionOverlapFilter implements Filter<Insertion,Insertion> { 
		private Region query;
		public InsertionOverlapFilter(Region r) { query = r; }
		public Insertion execute(Insertion value) { 
			return value.overlaps(query) ? value : null;
		}
	}

	private static class DeletionOverlapFilter implements Filter<Deletion,Deletion> { 
		private Region query;
		public DeletionOverlapFilter(Region r) { query = r; }
		public Deletion execute(Deletion value) { 
			return value.overlaps(query) ? value : null;
		}
	}

	private static class InsertionStrainFilter implements Filter<Insertion,Insertion> {
		private String strain;
		
		public InsertionStrainFilter(String str) { strain = str; }
		
		public Insertion execute(Insertion s) { 
			return s.strain.equals(strain) ? s : null;
		}
	}
	
	private static class DeletionStrainFilter implements Filter<Deletion,Deletion> {
		private String strain;
		
		public DeletionStrainFilter(String str) { strain = str; }
		
		public Deletion execute(Deletion s) { 
			return s.strain.equals(strain) ? s : null;
		}
	}
}

