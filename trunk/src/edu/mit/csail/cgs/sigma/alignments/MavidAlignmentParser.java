/*
 * Author: tdanford
 * Date: Aug 24, 2008
 */
package edu.mit.csail.cgs.sigma.alignments;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.sql.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.datasets.species.Organism;
import edu.mit.csail.cgs.datasets.alignments.*;
import edu.mit.csail.cgs.datasets.alignments.parsing.ClustalParser;
import edu.mit.csail.cgs.utils.*;
import edu.mit.csail.cgs.utils.database.ClobHandler;
import edu.mit.csail.cgs.utils.database.DatabaseFactory;
import edu.mit.csail.cgs.utils.database.Sequence;
import edu.mit.csail.cgs.utils.database.UnknownRoleException;

/**
 * Parses the MAVID-produced alignments specific to the Sigma project.
 * 
 * Takes a directory name as a parameter, and allows the parsing of all multiple 
 * alignments stored in CLUSTALW output files in that directory. 
 * 
 * @author tdanford
 */
public class MavidAlignmentParser {
	
	/**
	 * Usage: 
	 * 
	 * java edu.mit.csail.cgs.sigma.alignments.MavidAlignmentParser <dir> <cmd> [options...]
	 * 
	 * Where <dir> is the directory of the alignment files (Robin has specified a directory structure to this), usually something 
	 * in /afs/csail.mit.edu/group/psrg/projects/sigma/Data/Alignments/), and <cmd> is a string from the following set: 
	 * { "extract-fasta", "delete-genome", "insert-genome", "insert-alignments" }.  Usually, you're going to want 
	 * "insert-alignments", for which the only argument is a string which will be the name of the alignment version in the 
	 * database.  
	 */
	public static void main(String[] args) { 
		File dir = new File(args[0]);
		try {
			
			String cmd = args[1];
			if(cmd.equals(("extract-fasta"))) { 
				MavidAlignmentParser map = new MavidAlignmentParser(dir);
				map.extractAllFASTA();
				
			} else if (cmd.equals("delete-genome")) { 
				for(int i = 2; i < args.length; i++) { 
					deleteGenome(Integer.parseInt(args[i]));
				}
				
			} else if (cmd.equals("insert-genome")) { 
				MavidAlignmentParser map = new MavidAlignmentParser(dir);
				String tag = args[2];
				String genomeName = args[3];
				Organism org = Organism.getOrganism("Saccharomyces cerevisiae");

				Connection cxn = DatabaseFactory.getConnection("core");
				map.createNewGenome(cxn, org, genomeName, tag);
				DatabaseFactory.freeConnection(cxn);
				
			} else if (cmd.equals("insert-alignments")) { 
				MavidAlignmentParser map = new MavidAlignmentParser(dir);
				String version = args[2];
				InsertableAlignmentDataset ds = map.asInsertableDataset(version);
				if(ds.check()) { 
					AlignmentInserter.insertDataset(ds);
				} else { 
					System.err.println("Dataset failed check!");
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void deleteGenome(int id) throws UnknownRoleException, SQLException { 
		Connection cxn = DatabaseFactory.getConnection("core");
		Statement s = cxn.createStatement();
		s.executeUpdate("delete from chromsequence where id in (select id from chromosome where genome=" + id + ")");
		s.executeUpdate("delete from chromosome where genome=" + id);
		s.executeUpdate("delete from genome where id=" + id);
		s.close();
		DatabaseFactory.freeConnection(cxn);
		System.out.println("Deleted: " + id);
	}	

	private static Pattern clustalChromPatt = Pattern.compile("(.*)chr([^.]+)\\.clustalw");
	private static Pattern clustalSpeciesPatt = Pattern.compile("\\s*([^_]+)_chr.*");
	private static String lineBreaker = "\\s+";
	
	private File dir;
	private Map<String,File> clustalChromMap;
	private Set<String> species;
	private Map<String,Genome> tagToGenome;

	public MavidAlignmentParser(File alignDir) throws IOException {
		dir = alignDir;
		File[] clustalFiles = alignDir.listFiles(new FileFilter() {
			public boolean accept(File f) {
				return !f.isDirectory() && f.getName().endsWith(".clustalw");
			} 
		});
		clustalChromMap = new HashMap<String,File>();
		species = new TreeSet<String>();
		tagToGenome = new HashMap<String,Genome>();
		
		ClustalParser cp = new ClustalParser(clustalSpeciesPatt, lineBreaker);
		System.out.print("Loading MAVID Files ..."); System.out.flush();
		for(int i = 0; i < clustalFiles.length; i++) { 
			String name = clustalFiles[i].getName();
			Matcher m = clustalChromPatt.matcher(name);
			if(m.matches()) { 
				String chrom = m.group(2);
				clustalChromMap.put(chrom, clustalFiles[i]);
				species.addAll(cp.speciesScan(clustalFiles[i]));

				System.out.print(chrom + " "); System.out.flush();
			}
		}
		System.out.println();
		System.out.println(String.format("Species: %s", species.toString()));
		
		defaultGenomeTags();
	}
	
	public void defaultGenomeTags() { 
		try { 
			associateGenomeToTag("RM11", Organism.findGenome("RM11v2_chroms"));
			associateGenomeToTag("YJM789", Organism.findGenome("YJM789v1_chroms"));
			associateGenomeToTag("S1278b", Organism.findGenome("Sigmav6"));
			associateGenomeToTag("S288C", Organism.findGenome("SGDv1"));
		} catch(NotFoundException nfe) { 
			nfe.printStackTrace(System.err);
		}
	}
	
	public void associateGenomeToTag(String tag, Genome g) { 
		tagToGenome.put(tag, g);
	}
	
	public InsertableAlignmentDataset asInsertableDataset(String versionName) throws IOException { 
		InsertableAlignmentVersion version = new InsertableAlignmentVersion(versionName);
		InsertableAlignmentDataset ds = new InsertableAlignmentDataset(version);
		
		for(String chrom : clustalChromMap.keySet()) { 
			ClustalMultipleAlignment malign = parseClustalFile(chrom);
			String[] tags = malign.species();

			String speciesString = "";
			for(int i = 0; i < tags.length; i++) { speciesString += " " + tags[i]; }
			System.out.println(String.format("chrom %s, tags %s", chrom, speciesString));
			
			String params = "inserter=MavidAlignmentParser";
			InsertableAlignment alignment = new InsertableAlignment(params, version, 0.0);
			ds.alignments.add(alignment);

			int gappedLength = malign.gappedLength();

			for(int i = 0; i < tags.length; i++) { 
				Genome g = tagToGenome.get(tags[i]);

				int chromID = -1;
				try { 
					chromID = g.getChromID(chrom);
				} catch(Exception e) { 
					System.err.println(String.format("Couldn't find chrom \"%s\" in genome \"%s\"", 
						chrom, g.getVersion()));
					throw new IllegalStateException(e);
				}
				
				char[] bitString = malign.gappedString(tags[i]).toCharArray();
				int ungappedLength = malign.ungappedLength(tags[i]);
				int start = 0, end = ungappedLength-1;
				char strand = '+';
				
				InsertableAlignBlock ab = new InsertableAlignBlock(alignment, chromID, start, end, strand, bitString, gappedLength);
				ds.blocks.add(ab);
			}
		}
		
		return ds;
	}
	
	public Set<String> getChroms() { return clustalChromMap.keySet(); }
	
	public ClustalMultipleAlignment parseClustalFile(String chrom) throws IOException {
		File f = clustalChromMap.get(chrom);
		ClustalParser cp = new ClustalParser(f, clustalSpeciesPatt, "\\s+");
		return new ClustalMultipleAlignment(cp);
	}
	
	public void createNewGenome(Connection cxn, Organism org, String genomeName, String speciesTag) throws SQLException { 
		int orgID = org.getDBID();
		
		boolean ac = cxn.getAutoCommit();
		cxn.setAutoCommit(false);
		
		String insertGenomeStmt = String.format(
				"insert into genome (id, species, version, description) values " +
				"(%s, ?, ?, ?)", Sequence.getInsertSQL(cxn, "genome_id"));
		String insertChromStmt = String.format(
				"insert into chromosome (id, name, genome) values (%s, ?, ?)",
				Sequence.getInsertSQL(cxn, "chromosome_id"));
		String insertChromSeqStmt = "insert into chromsequence (id, sequence) values (?, ?)";

		//String insertChromSeqStmt = "insert into chromsequence (id, sequence) values (?, EMPTY_CLOB())";
		//String insertChromSeqStmt2 = "update chromsequence set sequence=? where id=?";
		
		PreparedStatement ps1, ps2, ps3; 
		Statement s = cxn.createStatement();
		
		int genomeID = -1;
		ps1 = cxn.prepareStatement(insertGenomeStmt);
		
		ps1.setInt(1, orgID);
		ps1.setString(2, genomeName);
		ps1.setString(3, "Automatically inserted by MavidAlignmentParser");
		ps1.executeUpdate();
		
		genomeID = getLastID(cxn, s, "genome_id");
		System.out.println(String.format("Inserted Genome: %s (%d)", genomeName, genomeID));
		
		ps2 = cxn.prepareStatement(insertChromStmt); 
		ps3 = cxn.prepareStatement(insertChromSeqStmt); 

		for(String chrom : clustalChromMap.keySet()) {
			try {
				ClustalMultipleAlignment malign = parseClustalFile(chrom);
				GappedAlignmentString gas = malign.getGappedAlignment(speciesTag);
				String ungapped = gas.ungappedString();
				
				ps2.setString(1, chrom);
				ps2.setInt(2, genomeID);
				ps2.executeUpdate();
				
				int chromID = getLastID(cxn, s, "chromosome_id");
				System.err.println(String.format("chromid: %d", chromID));
				
				ps3.setInt(1, chromID);
				ClobHandler.setClob(cxn, ps3, 2, ungapped);
				ps3.executeUpdate();

				/*
				ResultSet rs = s.executeQuery("select sequence from chromsequence where id=" + chromID + " for update");
				rs.next();
				Clob clob = rs.getClob(1);
				clob.setString((long)1, ungapped);
				rs.close();
				
				ps4.setClob(1, clob);
				ps4.setInt(2, chromID);
				ps4.executeUpdate();
				*/
				
				System.out.println(String.format("\t--> %s", chrom));
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		cxn.commit();
		cxn.setAutoCommit(ac);
		
		ps3.close();
		ps2.close();
		ps1.close();
		s.close();
	}
	
	private int getLastID(Connection cxn, Statement s, String seqname) throws SQLException { 
		ResultSet rs = s.executeQuery(Sequence.getLastSQLStatement(cxn, seqname));
		rs.next();
		int id = rs.getInt(1);
		rs.close();
		return id;
	}
	
	public void extractAllFASTA() throws IOException { 
		for(String sp : species) { 
			File f = new File(dir, String.format("%s_genome.fasta", sp));
			extractGenomeFASTA(sp, f);
			System.out.println(String.format("Extracted: %s", sp));
		}
	}
	
	public void extractGenomeFASTA(String species, File fasta) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(fasta));
		int lineWidth = 80;
		for(String chrom : clustalChromMap.keySet()) { 
			ClustalMultipleAlignment malign = parseClustalFile(chrom);
			GappedAlignmentString gas = malign.getGappedAlignment(species);
			String ungapped = gas.ungappedString();
			ps.println(String.format(">chr%s %d", chrom, ungapped.length()));
			for(int i = 0; i < ungapped.length(); i += lineWidth) { 
				ps.println(ungapped.substring(i, Math.min(i+lineWidth, ungapped.length())));
			}
		}
		ps.close();
	}
}

