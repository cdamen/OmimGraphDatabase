package com.compomics.omimgraphdatabase.properties;

/**
 *
 * @author Caroline
 */
public enum MIMProperty {
        MIMACCESSION ("mimAccession"),
        MIMTYPE("mimType");
	
	private String propertyName;
	private MIMProperty(String propertyName) {
		this.propertyName = propertyName;
	}

	public String toString() {
		return propertyName;
	}
}
