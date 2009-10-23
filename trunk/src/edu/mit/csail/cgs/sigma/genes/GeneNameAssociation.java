package edu.mit.csail.cgs.sigma.genes;

import java.sql.*;
import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.Parser;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.utils.Pair;
import java.sql.Connection;
import edu.mit.csail.cgs.utils.database.DatabaseFactory;

public interface GeneNameAssociation {
	public boolean containsID(String id); 
	public boolean containsName(String name);
	public String getName(String id);
	public Collection<String> getIDs(String name);
	
	public Collection<String> allIDs();
	public Collection<String> allNames();
}

class GeneNameDBAssociation implements GeneNameAssociation {

    private Map<String,String> id2Name;
    private Map<String,Set<String>> name2Ids;
    
    public GeneNameDBAssociation(Genome g) throws SQLException { 
        id2Name = new TreeMap<String,String>();
        name2Ids = new TreeMap<String,Set<String>>();

        Connection c = g.getUcscConnection();
        Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("select name, value from sgdToName");
        
        while(rs.next()) { 
            String id = rs.getString(1);
            String name = rs.getString(2);
            id2Name.put(id, name);
            if(!name2Ids.containsKey(name)) { 
                name2Ids.put(name, new HashSet<String>());
            }
            name2Ids.get(name).add(id);
        }
        
        rs.close();
        s.close();
        DatabaseFactory.freeConnection(c);
    }

    public Collection<String> allIDs() { return id2Name.keySet(); }
    public Collection<String> allNames() { return name2Ids.keySet(); }

    public boolean containsID(String id) {
        return id2Name.containsKey(id);
    }

    public boolean containsName(String name) {
        return name2Ids.containsKey(name);
    }

    public Collection<String> getIDs(String name) {
        return name2Ids.containsKey(name) ? name2Ids.get(name) : new LinkedList<String>();
    }

    public String getName(String id) {
        return id2Name.get(id);
    } 
}

class GeneNameFileAssociation implements GeneNameAssociation {
	
    private Map<String,String> id2Name;
    private Map<String,Set<String>> name2Ids;
    
	public GeneNameFileAssociation(File f) throws IOException { 
        id2Name = new TreeMap<String,String>();
        name2Ids = new TreeMap<String,Set<String>>();
		
		Parser<Pair<String,String>> parser = new Parser<Pair<String,String>>(f, 
				new Mapper<String,Pair<String,String>>() {
					public Pair<String, String> execute(String a) {
						String[] ar = a.split("\\s+");
						return new Pair<String,String>(ar[0], ar[1]);
					} 
				});	
		
		while(parser.hasNext()) { 
			Pair<String,String> p = parser.next();
			String id = p.getFirst(), name = p.getLast();
			if(!id2Name.containsKey(id)) { id2Name.put(id, name); }
			if(!name2Ids.containsKey(name)) { 
				name2Ids.put(name, new TreeSet<String>());
			}
			name2Ids.get(name).add(id);
		}
	}
	
    public Collection<String> allIDs() { return id2Name.keySet(); }
    public Collection<String> allNames() { return name2Ids.keySet(); }

    public boolean containsID(String id) {
        return id2Name.containsKey(id);
    }

    public boolean containsName(String name) {
        return name2Ids.containsKey(name);
    }

    public Collection<String> getIDs(String name) {
        return name2Ids.containsKey(name) ? name2Ids.get(name) : new LinkedList<String>();
    }

    public String getName(String id) {
        return id2Name.get(id);
    } 
}