package com.compomics.omimgraphdatabase.properties;

/**
 *
 * @author Caroline
 */
public enum ProteinProperty {
        ACCESSION ("accession"),
        SEQUENCE ("sequence"),
        NAME ("name");
	
	private String propertyName;
	private ProteinProperty(String propertyName) {
		this.propertyName = propertyName;
	}

	public String toString() {
		return propertyName;
	}
}
