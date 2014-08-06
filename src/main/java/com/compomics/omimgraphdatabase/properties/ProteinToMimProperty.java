/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.omimgraphdatabase.properties;

/**
 *
 * @author Caroline
 */
public enum ProteinToMimProperty {
    PROTEIN_TO_MIM ("PROTEIN_TO_MIM");
	
	private String propertyName;
	private ProteinToMimProperty(String propertyName) {
		this.propertyName = propertyName;
	}
}
