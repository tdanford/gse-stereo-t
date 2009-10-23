/*
 * Author: tdanford
 * Date: Sep 11, 2008
 */
package edu.mit.csail.cgs.sigma.litdata.huiyer;

import java.util.*;
import java.sql.*;

import edu.mit.csail.cgs.utils.database.*;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;

/**
 * Dataset-specific code for importing the Hu/Iyer tf knockout expression 
 * data into the expression schema in the database.
 * 
 * @author tdanford
 */
public class DatabaseInserter {

	private Connection cxn;
	private TFKOParser parser;
	
	public DatabaseInserter(TFKOParser p, Connection c) { 
		parser = p;
		cxn = c;
	}
	
	public void createProbePlatform() throws SQLException { 
		Statement s = cxn.createStatement();
		
		s.close();
	}
}
