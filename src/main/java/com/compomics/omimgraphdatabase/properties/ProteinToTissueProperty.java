/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.omimgraphdatabase.properties;

/**
 *
 * @author Caroline
 */
public enum ProteinToTissueProperty {
        PROTEIN_TO_TISSUE ("PROTEIN_TO_TISSUE");
	
	private String propertyName;
	private ProteinToTissueProperty(String propertyName) {
		this.propertyName = propertyName;
	}

	public String toString() {
		return propertyName;
	}
}   

