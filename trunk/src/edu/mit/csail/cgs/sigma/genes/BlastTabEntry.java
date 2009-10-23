/*
 * Created on Mar 12, 2008
 */
package edu.mit.csail.cgs.sigma.genes;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.*;

/*
  Query id,
  Subject id,
  percent of identity,
  alignment length,
  number of mismatches (not including gaps),
  number of gap openings,
  start of alignment in query,
  end of alignment in query,
  start of alignment in subject,
  end of alignment in subject,
  expected value,
  bit score.
 */

public class BlastTabEntry {

    private String query, subject;
    private double pctIdentity;
    private int alignLength, mismatches, gapOpens;
    private int queryAlignStart, queryAlignEnd;
    private int subjectAlignStart, subjectAlignEnd;
    private double expScore, bitScore;
    
    public BlastTabEntry(String line) { 
        String[] a = line.split("\t");
        if(a.length != 12) { throw new IllegalArgumentException(line); }
        query = a[0];
        subject = a[1];
        pctIdentity = Double.parseDouble(a[2]);
        alignLength = Integer.parseInt(a[3]);
        mismatches = Integer.parseInt(a[4]);
        gapOpens = Integer.parseInt(a[5]);
        queryAlignStart = Integer.parseInt(a[6]);
        queryAlignEnd = Integer.parseInt(a[7]);
        subjectAlignStart = Integer.parseInt(a[8]);
        subjectAlignEnd = Integer.parseInt(a[9]);
        expScore = Double.parseDouble(a[10]);
        bitScore = Double.parseDouble(a[11]);
    }
    
    public String getQuery() { return query; }
    public String getSubject() { return subject; }
    public double getPctIdentity() { return pctIdentity; }
    public int getAlignLength() { return alignLength; }
    public int getMismatches() { return mismatches; }
    public int getGapOpens() { return gapOpens; }
    public int getQueryStart() { return queryAlignStart; } 
    public int getQueryEnd() { return queryAlignEnd; }
    public int getSubjectStart() { return subjectAlignStart; }
    public int getSubjectEnd() { return subjectAlignEnd; }
    
    public double getExpectedScore() { return expScore; }
    public double getBitScore() { return bitScore; }
    
    public int hashCode() { 
        int code = 17;
        code += query.hashCode(); code *= 37;
        code += subject.hashCode(); code *= 37;
        code += queryAlignStart; code *= 37;
        code += subjectAlignStart; code *= 37;
        return code; 
    }
    
    public boolean equals(Object o) { 
        if(!(o instanceof BlastTabEntry)) { return false; }
        BlastTabEntry b = (BlastTabEntry)o;
        if(!query.equals(b.query)) { return false; }
        if(!subject.equals(b.subject)) { return false; }
        if(queryAlignStart != b.queryAlignStart) { return false; }
        if(queryAlignEnd != b.queryAlignEnd) { return false; }
        if(subjectAlignStart != b.subjectAlignStart) { return false; }
        if(subjectAlignEnd != b.subjectAlignEnd) { return false; }
        return true;
    }
    
    public static class ParsingMapper implements Mapper<String,BlastTabEntry> {
        public BlastTabEntry execute(String a) {
            return new BlastTabEntry(a);
        } 
    }
    
    public static class ExpectedScoreFilter implements Filter<BlastTabEntry,BlastTabEntry> {
        
        private double expectedScoreThreshold;
        
        public ExpectedScoreFilter(double espt) { 
            expectedScoreThreshold = espt;
        }

        public BlastTabEntry execute(BlastTabEntry a) {
            if(a.getExpectedScore() <= expectedScoreThreshold) { 
                return a;
            } else { 
                return null;
            }
        } 
    }
}
