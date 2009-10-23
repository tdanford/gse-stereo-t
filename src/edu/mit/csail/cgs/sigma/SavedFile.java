/*
 * Created on Feb 12, 2008
 */
package edu.mit.csail.cgs.sigma;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.*;

public class SavedFile<X> {
    private LinkedList<X> lines;
    private File file;
    private Mapper<String,X> decoder;
    private Mapper<X,String> encoder;
    
    public SavedFile(Mapper<String,X> dec, Mapper<X,String> enc, File f) throws IOException { 
        decoder = dec;
        encoder = enc;
        file = f;
        lines = null;
        load();
    }

    public SavedFile(Mapper<String,X> dec, Mapper<X,String> enc, File f, Collection<X> vs) throws IOException { 
        decoder = dec;
        encoder = enc;
        lines = new LinkedList<X>(vs);
        file = f;
    }
    
    public File getFile() { return file; }
    public Collection<X> getValues() { return lines; }
    
    public void load() throws IOException { 
        if(file == null) { throw new IllegalStateException("'file' is null."); }
        lines = new LinkedList<X>();
        Parser<X> parser = new Parser<X>(file, decoder);
        while(parser.hasNext()) { 
            lines.addLast(parser.next());
        }
    }
    
    public void save() throws IOException { 
        if(file == null) { throw new IllegalStateException("'file' is null."); }
        if(lines == null) { throw new IllegalStateException("no values were given."); }
        PrintStream ps = new PrintStream(new FileOutputStream(file));
        for(X value : lines) { 
            ps.println(encoder.execute(value));
        }
        ps.close();
    }
}
