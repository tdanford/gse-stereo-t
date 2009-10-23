/*
 * Author: tdanford
 * Date: Dec 16, 2008
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.workflow.models.FileInputData;
import edu.mit.csail.cgs.utils.models.ModelInput;
import edu.mit.csail.cgs.utils.models.ModelInputIterator;

/** 
 * @author tdanford
 * 
 */
public class WorkflowPackageReader 
	extends ModelInputIterator<FileInputData> 
	implements Iterator<FileInputData>, Closeable {
	
	public WorkflowPackageReader() { 
		this(System.in);
	}
	
	public WorkflowPackageReader(File f) throws IOException { 
		this(new FileInputStream(f));
	}

	public WorkflowPackageReader(InputStream is) {
		super(new ModelInput.LineReader<FileInputData>(FileInputData.class, is));
	}
	
}
