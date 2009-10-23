package edu.mit.csail.cgs.sigma.genes;

import java.util.*;
import java.util.regex.*;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.utils.SetTools;

public class Promoter extends Region {
    
    private static SetTools<String> tools = new SetTools<String>();
	
	private Set<String> rightIDs, leftIDs;

	public Promoter(Genome g, String chrom, int start, int end) {
		super(g, chrom, start, end);
        rightIDs = new TreeSet<String>();
        leftIDs = new TreeSet<String>();
	}
	
    public void addRightIdentifier(String id) { 
        rightIDs.add(id);
    }
    
    public void addLeftIdentifier(String id) { 
        leftIDs.add(id);
    }
    
    public String toString() { 
    	StringBuilder sb = new StringBuilder();
    	if(leftIDs.size() > 0) {
    		for(String s : leftIDs) { 
    			sb.append(s + " ");
    		}
    		sb.append("<- ");
    	}
 
    	sb.append(super.toString());
    	if(rightIDs.size() > 0) {
    		sb.append(" ->");
    		for(String s : rightIDs) { 
    			sb.append(" " + s);
    		}
    	}
    	return sb.toString();
    }
    
	public Collection<String> getDownstreamIDs() { return tools.union(rightIDs, leftIDs); }
    public Set<String> getRightIDs() { return rightIDs; }
    public Set<String> getLeftIDs() { return leftIDs; }
    
    public boolean isRightRegulating() { return rightIDs.size() > 0; }
    public boolean isLeftRegulating() { return leftIDs.size() > 0; }
    public boolean isDivergent() { return isRightRegulating() && isLeftRegulating(); }
    public boolean isConvergent() { return !isRightRegulating() && !isLeftRegulating(); }
	
	public static class PromoterComparator implements Comparator<Promoter> { 
		public int compare(Promoter p1, Promoter p2) { 
			if(p1.getWidth() > p2.getWidth()) { return -1; }
			if(p1.getWidth() < p2.getWidth()) { return 1; }
			return p1.compareTo(p2);
		}
	}
    
	public static class Encoder implements Mapper<Promoter,String> {

		public String execute(Promoter a) {
            StringBuilder sb = new StringBuilder();
			String msg = String.format("%s\t%d\t%d", a.getChrom(), a.getStart(), a.getEnd());
            sb.append(msg);
            for(String id : a.leftIDs) { 
                sb.append(String.format("\tleft:%s", id));
            }
            for(String id : a.rightIDs) { 
                sb.append(String.format("\tright:%s", id));
            }
            return sb.toString();
		} 
	}
	
	private static Pattern promoterPattern = Pattern.compile("([^\\s]+)\\s+(\\d+)\\s+(\\d+)\\s*(.*)");
    private static Pattern leftPattern = Pattern.compile("left\\:(.*)");
    private static Pattern rightPattern = Pattern.compile("right\\:(.*)");
    //private static Pattern leftPattern = Pattern.compile("(.*)");
    //private static Pattern rightPattern = Pattern.compile("(.*)");
	
	public static class Decoder implements Mapper<String,Promoter> { 
		private Genome genome;
		
		public Decoder(Genome g) { 
			genome = g;
		}
		
		public Promoter execute(String a) {
			Matcher m = promoterPattern.matcher(a);
			if(m.matches()) { 
				String chrom = m.group(1);
				int start = Integer.parseInt(m.group(2));
				int end = Integer.parseInt(m.group(3));
				Promoter p = new Promoter(genome, chrom, start, end);

                String tags = m.group(4);
                //System.out.println(String.format("\"%s\"", tags));
                String[] array = tags.split("\\s+");
                if(tags.length() > 0) { 
                    for(int i = 0; i < array.length; i++) { 
                        Matcher mm = null;
                        if((mm = leftPattern.matcher(array[i])).matches()) { 
                            p.addLeftIdentifier(mm.group(1));
                        } else if((mm = rightPattern.matcher(array[i])).matches()) { 
                            p.addRightIdentifier(mm.group(1));
                        } else { 
                            System.err.println(String.format("ERROR: \"%s\"", array[i]));
                            throw new IllegalArgumentException(array[i]);
                        }
                    }
                }
                
				return p;
			} else {
				throw new IllegalArgumentException(a);
			}
		}
	}
}
