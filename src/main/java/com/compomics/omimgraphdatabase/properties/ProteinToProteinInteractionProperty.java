/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.omimgraphdatabase.properties;

/**
 *
 * @author Caroline
 */
public enum ProteinToProteinInteractionProperty {
    PROTEIN_TO_PROTEIN_INTERACTION ("PROTEIN_TO_PROTEIN_INTERACTION");
	
	private String propertyName;
	private ProteinToProteinInteractionProperty(String propertyName) {
		this.propertyName = propertyName;
	}    
}
