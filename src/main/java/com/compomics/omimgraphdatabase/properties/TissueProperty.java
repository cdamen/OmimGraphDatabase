package com.compomics.omimgraphdatabase.properties;

/**
 * @author Caroline
 */
public enum TissueProperty {
    TISSUE("tissue");
	
	private String propertyName;
	private TissueProperty(String propertyName) {
		this.propertyName = propertyName;
	}
	
	public String toString() {
		return propertyName;
	}
    
}
