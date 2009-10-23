package edu.mit.csail.cgs.sigma.litdata;

import java.util.*;
import java.io.File;
import java.io.IOException;

import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.litdata.huiyer.AffyAnnotationDecoder;
import edu.mit.csail.cgs.utils.*;

public class LitDataProperties {

	private SigmaProperties props;
	private ResourceBundle bundle;
	
	public LitDataProperties() { 
		this("default");
	}
	
	public LitDataProperties(String name) { 
		bundle = ResourceBundle.getBundle(String.format("edu.mit.csail.cgs.sigma.litdata.%s", name));
		props = new SigmaProperties();
	}
	
	public File getDirectory() { 
		String dirName = bundle.getString("directory");
		return new File(dirName);
	}
	
	public File getIyerFile() { 
		String filename = bundle.getString("iyer");
		return new File(getDirectory(), filename); 
	}

	public File getIyerPvaluesFile() { 
		String filename = bundle.getString("iyer_pvalues");
		return new File(getDirectory(), filename); 
	}

	public File getIyerMapFile() { 
		String filename = bundle.getString("iyer_map");
		return new File(getDirectory(), filename); 
	}


	public File getGPL90() { 
		String filename = bundle.getString("gpl90");
		return new File(getDirectory(), filename); 
	}

	public File getHarbisonFile() {
		String filename = bundle.getString("harbison");
		return new File(getDirectory(), filename); 
	}

	public File getGPL2529() { 
		String filename = bundle.getString("gpl2529");
		return new File(getDirectory(), filename); 
	}
	
	public File getGPL90Decoder() { 
		String filename = bundle.getString("gpl90_decoder");
		return new File(getDirectory(), filename); 
	}

	public File getGPL2529Decoder() { 
		String filename = bundle.getString("gpl2529_decoder");
		return new File(getDirectory(), filename); 
	}
	
	public MicroarrayExpression getDefaultExpression() {
		try {
			AffyAveraging affy1 = 
				new AffyAveraging(getGPL90Decoder(), new TableParser(getGPL90(), 500));
			/*
			AffyAveraging affy2 = 
				new AffyAveraging(getGPL2529Decoder(), new TableParser(getGPL2529()));
			*/
			return new AppendingExpression(affy1);
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public SigmaProperties getSigmaProperties() {
		return props;
	}

}
