package edu.mit.csail.cgs.sigma.probnetwork;

import java.util.*;
import java.util.regex.*;

import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.Mapper;

public class YeastNetEntry {

	private String name1, name2;
	private double value;
	
	public YeastNetEntry(String line) { 
		String[] a = line.split("\\s+");
		name1 = a[0]; name2 = a[1];
		value = Double.parseDouble(a[2]);
	}
	
	public String getName1() { return name1; }
	public String getName2() { return name2; }
	public double getValue() { return value; }
	
	public int hashCode() {
		int code = 17;
		code += name1.hashCode(); code *= 37;
		code += name2.hashCode(); code *= 37;
		return code;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof YeastNetEntry)) { 
			return false; 
		}
		
		YeastNetEntry e = (YeastNetEntry)o;
		return name1.equals(e.name1) && name2.equals(e.name2) && value==e.value;
	}
	
	public String toString() { 
		return String.format("%s\t%s\t%.4f", name1, name2, value);
	}
	
	public static class ParsingMapper implements Mapper<String,YeastNetEntry> {
		public YeastNetEntry execute(String a) {
			return new YeastNetEntry(a);
		} 
	}
}
