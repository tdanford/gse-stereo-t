package edu.mit.csail.cgs.sigma.networks;

import java.io.PrintStream;
import java.sql.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.mit.csail.cgs.datasets.motifs.WeightMatrix;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.ewok.verbs.motifs.WeightMatrixHit;
import edu.mit.csail.cgs.utils.SetTools;
import edu.mit.csail.cgs.utils.database.Sequence;

/**
 * @author tdanford
 *
 * RegulatoryUnit represents the "inbound" connectivity of the ntework for a single gene -- 
 * that is, it holds a data structure of which motif sites occur where (with relation to the 
 * TSS).  It also stores information about what regulatory connections have *changed* relative
 * to another species or strain, and it can handle the case where the gene is unique to this
 * particular species or strain (or is a placeholder for a gene that doesn't exist in this 
 * strain).  
 */
public class RegulatoryUnit {
	
	/*
	 * NORMAL = the gene exists in both strains
	 * STRAIN_UNIQUE = the gene is unique to "this" strain (and is missing in the "other" strain)
	 * STRAIN_LOST = a placeholder -- in this case, the gene==null, and the RegulatoryUnit object
	 *               is a stand-in for a gene that doesn't exist in this strain.
	 */
	public static enum StrainUniqueCode { STRAIN_UNIQUE, STRAIN_LOST, NORMAL };
    public static Pattern motifOffsetPattern = Pattern.compile("([^:]+):(-?\\d+)");
	
	private Gene gene;
	private Map<String,Set<Integer>> motifOffsets;
	
	/*
	 * The following fields keep track of changes of this regulatory unit, relative to 
	 * another strain.
	 */
	private LinkedList<RegulatoryChange> changes;
	private StrainUniqueCode code;
    
    /*
     * Expression and Regulation Information.
     */
    private double differential;
    private Ranking ranking;
	
	public RegulatoryUnit(Gene g) { 
		gene = g;
		motifOffsets = new TreeMap<String,Set<Integer>>();
		changes = new LinkedList<RegulatoryChange>();
		code = StrainUniqueCode.NORMAL;
        differential = 0.0;
        ranking = null;
	}
    
    public static PreparedStatement prepareInsertUnitStatement(
            java.sql.Connection cxn) 
        throws SQLException { 
        return cxn.prepareStatement(String.format("insert into regulatory_unit " +
                "(id, strain, chrom, start, end, strand, name) values " +
                "(%s, ?, ?, ?, ?, ?, ?)", Sequence.getInsertSQL(cxn, "id")));
    }
    
    public static PreparedStatement prepareInsertRelationStatement(
            java.sql.Connection cxn) 
        throws SQLException { 
        return cxn.prepareStatement("insert into regulatory_relation " +
                "(regunit, regulator, offsets) values (?, ?, ?)");
    }
    
    public int insertIntoDB(String strain, 
                            PreparedStatement unit, 
                            PreparedStatement relation, 
                            PreparedStatement lastID) throws SQLException { 
        int unitID = -1;
        
        unit.setString(1, strain);
        unit.setString(2, gene.getChrom());
        unit.setInt(3, gene.getStart());
        unit.setInt(4, gene.getEnd());
        unit.setString(5, String.valueOf(gene.getStrand()));
        unit.setString(6, gene.getID());
        unit.executeUpdate();
        
        ResultSet rs = lastID.executeQuery();
        if(rs.next()) { 
            unitID = rs.getInt(1); 
        }
        rs.close();
        
        if(unitID == -1) { 
            throw new IllegalStateException();
        }
        
        for(String regulator : motifOffsets.keySet()) { 
            StringBuilder sb = new StringBuilder();
            for(int offset : motifOffsets.get(regulator)) { 
                sb.append(sb.length() == 0 ? "" : ",");
                sb.append(String.valueOf(offset));
            }
            
            relation.setInt(1, unitID);
            relation.setString(2, regulator);
            relation.setString(3, sb.toString());
            
            relation.executeUpdate();
        }
        
        return unitID;
    }
    
    public void load(String line) { 
        String[] a = line.split("\\s+");
        if(!gene.getID().equals(a[0])) { throw new IllegalArgumentException(); }
        for(int i = 1; i < a.length; i++) { 
            Matcher m = motifOffsetPattern.matcher(a[i]);
            if(!m.matches()) { throw new IllegalArgumentException(line); }
            String motif = m.group(1);
            int offset = Integer.parseInt(m.group(2));
            if(!motifOffsets.containsKey(motif)) { motifOffsets.put(motif, new TreeSet<Integer>()); }
            motifOffsets.get(motif).add(offset);
        }
    }
   
    public void save(PrintStream ps) { 
        ps.print(gene.getID());
        for(String m : motifOffsets.keySet()) { 
            for(int off : motifOffsets.get(m)) { 
                ps.print(String.format(" %s:%d", m, off));
            }
        }
        ps.println();
    }
    public String getMotifOffsetsString() { 
    	StringBuilder sb=new StringBuilder();
        for(String m : motifOffsets.keySet()) { 
            for(int off : motifOffsets.get(m)) { 
                sb.append(String.format(" %s:%d", m, off));
            }
        }
        return sb.toString();
    }
	public Map<String,Set<Integer>> getMotifOffsets(){
		return motifOffsets;
	}
	public StrainUniqueCode getStrainCode() { return code; }
	public void setStrainCode(StrainUniqueCode c) { code = c; }
    
    public void setDifferential(double d) { differential = d; }
    public double getDifferential() { return differential; }
    
    public Ranking getRanking() { return ranking; }
    public void setRanking(Ranking r) { ranking = r; }
	
	public void updateChanges(RegulatoryUnit u) { 
		SetTools<String> tools = new SetTools<String>();
		changes.clear();
		Set<String> total = tools.union(motifOffsets.keySet(), u.motifOffsets.keySet());
		for(String m : total) { 
			int myCount = getMotifCount(m), otherCount = u.getMotifCount(m);
			int diff = otherCount - myCount;
			if(diff != 0) { 
				changes.add(new RegulatoryChange(m, diff > 0, Math.abs(diff)));
			}
		}
	}
	
	public Gene getGene() { return gene; }
	public Collection<RegulatoryChange> getChanges() { return changes; }

	public void addChange(RegulatoryChange c) { changes.add(c); }
	
	public int getMotifCount(String tf) { 
		return containsMotif(tf) ? motifOffsets.get(tf).size() : 0;
	}
	
	public Set<String> getRegulatingTFs() { return new TreeSet<String>(motifOffsets.keySet()); }
	
	public boolean containsMotif(String tf) { return motifOffsets.containsKey(tf); }
	
	public void addWeightMatrixHit(WeightMatrixHit h) { 
		WeightMatrix m = h.getMatrix();
		int middle = h.getStart() + (h.getWidth()/2);
		int offset = gene.getStrand() == '+' ? gene.getStart()-middle : middle-gene.getEnd();
		addMotif(m.name, offset);
	}
	
	public void addMotif(String tf, int offset) { 
		if(!motifOffsets.containsKey(tf)) { 
			motifOffsets.put(tf, new TreeSet<Integer>());
		}
		
		motifOffsets.get(tf).add(offset);
	}
	
	public int hashCode() { 
		return gene.hashCode();
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof RegulatoryUnit)) { return false; }
		RegulatoryUnit ru = (RegulatoryUnit)o;
		return ru.gene.equals(gene);
	}
	
	public class RegulatoryChange {
		
		private String name;
		private boolean gained;
		private int count;
		
		public RegulatoryChange(String n, boolean g, int c) { 
			name = n; 
			gained = g;
			count = c;
		}
		
		public int getCount() { return count; }
		public int getDiff() { return gained ? count : -count; }
		
		public String getName() { return name; }
		public boolean isGained() { return gained; }
		public boolean isRemoved() { return !gained; }
		
		public String toString() { return String.format("%s%s%d", name, (gained ? "+" : "-"), count); }
	}
}