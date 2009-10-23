package edu.mit.csail.cgs.sigma;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.Mapper;

public class Parser<X> implements Iterator<X> {
	
	private File file;
	private long lineCount;
	private Mapper<String,X> mapper;
	private X nextValue;
	private BufferedReader br;
	
	public Parser(File f, Mapper<String,X> m) throws IOException {
		file = f;
		lineCount = 0;
		br = new BufferedReader(new FileReader(f));
		mapper = m;
		findNextValue();
	}
	
	public Parser(File f, Mapper<String,X> m, int header) throws IOException {
		file = f;
		lineCount = 0;
		br = new BufferedReader(new FileReader(f));
		mapper = m;
		for(int i = 0; i < header; i++) { br.readLine(); }
		findNextValue();
	}

	private void findNextValue() { 
		nextValue = null;
        String line = null;
        boolean parsedValue = false;
        try { 
            do { 
                while((line = br.readLine()) != null && (line = line.trim()).length() == 0) { 
                    lineCount += 1;
                }

                if(line != null) { 
                    try { 
                        nextValue = mapper.execute(line);
                        parsedValue = true;
                    } catch(Exception e) {
                        System.err.println(String.format("Parser Exception (%s : %d)", 
                                file.getName(), lineCount));
                        System.err.println(String.format("Line: \"%s\"", line));
                        e.printStackTrace(System.err);
                        parsedValue = false;
                    }
                }
            } while(!parsedValue && line != null);
			
		} catch(IOException e) {
			System.err.println(String.format("IOException (%s : %d)", 
					file.getName(), lineCount));
            System.err.println(String.format("Line: \"%s\"", line));
			e.printStackTrace(System.err);
			try {
				br.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			br = null;
		}
	}

	public boolean hasNext() {
		return nextValue != null;
	}

	public X next() {
		X val = nextValue;
		findNextValue();
		return val;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
