package com.compomics.omimgraphdatabase.parser;

import java.util.List;

import com.compomics.util.protein.Protein;

/**
 *
 * @author Caroline
 */
public class UniprotFile {
 
	private String filename;

	private List<Protein> uniprotEntries;
	
	public UniprotFile(String filename, List<Protein> uniprotEntries) {
		this.filename = filename;
		this.uniprotEntries = uniprotEntries;
	}
    
	public List<Protein> getUniprotEntries() {
		return uniprotEntries;
	}
	
	public String getFilename() {
		return filename;
	}
    
}
