/*
 * Author: tdanford
 * Date: Oct 27, 2008
 */
package edu.mit.csail.cgs.sigma.localization;

import java.io.IOException;
import java.util.*;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.utils.models.Model;

public class LocalizationMain {

	public static void main(String[] args) { 
		String propname = args.length > 0 ? args[0] : "default";
		SigmaProperties sprops = new SigmaProperties();
		LocalizationProperties props = new LocalizationProperties(sprops, propname);

		try { 
			Parser<LocalizationEntry> entries = 
				new Parser<LocalizationEntry>(
						props.getLocalizationDataFile(),
						new LocalizationEntry.Decoder(), 1);

			while(entries.hasNext()) { 
				LocalizationEntry entry = entries.next();

				String orf = entry.getOrf();
				Set<String> orfLocales = entry.getLocalizations();
				ORFLocales ols = new ORFLocales(orf, orfLocales.size());
			}
		} catch(IOException e) { 
			e.printStackTrace(System.err);
		}
	}
	
	public static class ORFLocales extends Model { 
		public String name;
		public Integer localeCount;
		
		public ORFLocales(String o, int lc) { 
			name = o;
			localeCount = lc;
		}
	}
}
