/*
 * Author: tdanford
 * Date: May 5, 2008
 */
package edu.mit.csail.cgs.sigma.utils;

import java.util.*;
import java.sql.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.database.*;

/**
 * Run me with the following three arguments:
 * 1: name of the genome for which you want the tables output  (i.e., "SGDv1")
 * 2: the filename where the contents of sgdGene will go.
 * 3: the filename where the contents of sgdOther will go.
 */
public class DumpGeneTables {
	
	public static void main(String[] args) { 
		loadTables(args);
	}

    public static void loadTables(String[] args) { 
        try {
            Genome g = Organism.findGenome(args[0]);
            DumpGeneTables dumper = new DumpGeneTables(g);

            int i = 1;
            while(i < args.length) { 
            	if(args[i].equals("sgdGene")) { 
            		String table = args[i+1];
            		String filename = args[i+2];
            		dumper.loadSGDGeneTable(new File(filename), table);
            		i += 3;
            	} else if (args[i].equals("sgdOther")) { 
            		String table = args[i+1];
            		String filename = args[i+2];
            		dumper.loadSGDOtherTable(new File(filename), table);
            		i += 3;
            	} else { 
            		throw new IllegalArgumentException("Unknown command line option: " + args[i]);
            	}
            }
            
            dumper.close();
            
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void dumpTables(String[] args) { 
        try {
            Genome g = Organism.findGenome(args[0]);
            DumpGeneTables dumper = new DumpGeneTables(g);
            File sgdGeneOutput = new File(args[1]);
            File sgdOtherOutput = new File(args[2]);
            
            dumper.dumpSGDGeneTable(sgdGeneOutput);
            dumper.dumpSGDOtherTable(sgdOtherOutput);

            dumper.close();
            
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private Genome genome;
    private java.sql.Connection cxn;
    
    public DumpGeneTables(Genome g) throws SQLException {
        genome = g;
        cxn = genome.getUcscConnection();
    }
    
    public void close() { 
        DatabaseFactory.freeConnection(cxn);
    }
    
    public void loadSGDGeneTable(File input, String tableName) throws SQLException, IOException { 
    	BufferedReader br = new BufferedReader(new FileReader(input));
    	String line = null;
    	
        String template = "insert into %s (name, chrom, strand, txStart, txEnd, " +
        "cdsStart, cdsEnd, exonCount, exonStarts, exonEnds, proteinID) values " +
        "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cxn.prepareStatement(String.format(template, tableName));
    	
    	while((line = br.readLine()) != null) {
    		if(line.length() > 0) { 
    			String[] a = line.split("\t");
    			if(a.length != 10) { 
    				System.err.println(String.format("Poorly formatted line: \"%s\"", line));
    			} else { 
    				String name = a[0];
    				String chrom = a[1];
    				char strand = a[2].charAt(0);
    				int txStart = Integer.parseInt(a[3]);
    				int txEnd = Integer.parseInt(a[4]);
    				int cdsStart = Integer.parseInt(a[5]);
    				int cdsEnd = Integer.parseInt(a[6]);
    				int exonCount = Integer.parseInt(a[7]);
    				String exonStarts = a[8];
    				String exonEnds = a[9];

    				ps.setString(1, name);
    				ps.setString(2, chrom);
    				ps.setString(3, String.valueOf(strand));
    				ps.setInt(4, txStart);
    				ps.setInt(5, txEnd);
    				ps.setInt(6, cdsStart);
    				ps.setInt(7, cdsEnd);
    				ps.setInt(8, exonCount);
    				ps.setString(9, exonStarts);
    				ps.setString(10, exonEnds);
    				ps.setString(11, name);
    				
    				ps.executeUpdate();
    			}
    		}
    	}
    	
    	ps.close();
    	br.close();
    }

    public void loadSGDOtherTable(File input, String tableName) throws SQLException, IOException { 
    	BufferedReader br = new BufferedReader(new FileReader(input));
    	String line = null;
    	
        String template = "insert into %s (chrom, chromStart, chromEnd, name, " +
                "score, strand, type) values (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cxn.prepareStatement(String.format(template, tableName));
    	
    	while((line = br.readLine()) != null) {
    		if(line.length() > 0) { 
    			String[] a = line.split("\t");
    			if(a.length != 7) { 
    				System.err.println(String.format("Poorly formatted line: \"%s\"", line));
    			} else { 
    				String chrom = a[0];
    				int chromStart = Integer.parseInt(a[1]);
    				int chromEnd = Integer.parseInt(a[2]);
    				String name = a[3];
    				double score = Double.parseDouble(a[4]);
    				char strand = a[5].charAt(0);
    				String type = a[6];
    				
    				ps.setString(1, chrom);
    				ps.setInt(2, chromStart);
    				ps.setInt(3, chromEnd);
    				ps.setString(4, name);
    				ps.setDouble(5, score);
    				ps.setString(6, String.valueOf(strand));
    				ps.setString(7, type);
    				
    				ps.executeUpdate();
    			}
    		}
    	}
    	
    	ps.close();
    	br.close();
    }

    public void dumpSGDGeneTable(File output) throws SQLException, IOException {
        String queryTemplate = "select name, chrom, strand, txStart, txEnd, " +
        "cdsStart, cdsEnd, exonCount, exonStarts, exonEnds, proteinID from %s";
        String query = String.format(queryTemplate, "sgdGene");
        PrintStream ps = new PrintStream(new FileOutputStream(output));
        String lineTemplate = "%s\t%s\t%c\t%d\t%d\t%d\t%d\t%d\t%s\t%s\t%s";

        Statement stmt = cxn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        ps.println("name\tchrom\tstrand\ttxStart\ttxEnd\t" +
        "cdsStart\tcdsEnd\texonCount\texonStarts\texonEnds\tproteinID");

        while(rs.next()) { 
            String name = rs.getString(1);
            String chrom = rs.getString(2);
            char strand = rs.getString(3).charAt(0);
            int txStart = rs.getInt(4);
            int txEnd = rs.getInt(5);
            int cdsStart = rs.getInt(6);
            int cdsEnd = rs.getInt(7);
            int exonCount = rs.getInt(8);
            String exonStarts = rs.getString(9);
            String exonEnds = rs.getString(10);
            String proteinID = rs.getString(11);

            ps.println(String.format(lineTemplate,
                    name, chrom, strand, txStart, txEnd, cdsStart, cdsEnd, exonCount,
                    exonStarts, exonEnds, proteinID));
        }

        rs.close();
        stmt.close();

        ps.close();
    }
    
    public void dumpSGDOtherTable(File output) throws SQLException, IOException { 
        String queryTemplate = "select chrom, chromStart, chromEnd, name, " +
                "score, strand, type from %s";
        String query = String.format(queryTemplate, "sgdOther");
        PrintStream ps = new PrintStream(new FileOutputStream(output));
        String lineTemplate = "%s\t%d\t%d\t%s\t%f\t%c\t%s";

        Statement stmt = cxn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        ps.println("chrom, chromStart, chromEnd, name, " +
                "score, strand, type");

        while(rs.next()) { 
            String chrom = rs.getString(1);
            int chromStart = rs.getInt(2);
            int chromEnd = rs.getInt(3);
            String name = rs.getString(4);
            double score = rs.getDouble(5);
            char strand = rs.getString(6).charAt(0);
            String type = rs.getString(7);
            ps.println(String.format(lineTemplate, chrom, chromStart, chromEnd,
                    name, score, strand, type));
        }

        rs.close();
        stmt.close();

        ps.close();
    }
    
}
