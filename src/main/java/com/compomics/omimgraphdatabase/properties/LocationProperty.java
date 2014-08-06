package com.compomics.omimgraphdatabase.properties;


public enum LocationProperty {
        LOCATION ("location");
	
	private String propertyName;
	private LocationProperty(String propertyName) {
		this.propertyName = propertyName;
	}

	public String toString() {
		return propertyName;
	}
}
