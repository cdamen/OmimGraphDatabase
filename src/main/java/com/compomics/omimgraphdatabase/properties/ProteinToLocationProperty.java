/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.omimgraphdatabase.properties;

/**
 *
 * @author Caroline
 */
public enum ProteinToLocationProperty {
    PROTEIN_TO_LOCATION ("PROTEIN_TO_LOCATION");
	
	private String propertyName;
	private ProteinToLocationProperty(String propertyName) {
		this.propertyName = propertyName;
	}    
}
