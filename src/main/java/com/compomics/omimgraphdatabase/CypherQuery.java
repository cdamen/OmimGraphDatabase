package com.compomics.omimgraphdatabase;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;

/**
 *
 * @author Caroline
 */
public class CypherQuery {
    	private ExecutionEngine engine;
	public CypherQuery(ExecutionEngine engine) {
		this.engine = engine;
        }

// Cypherqueries.
    
    // alle nodes.    
    public ExecutionResult getAllNodes() {
      return engine.execute("START n=node(*) RETURN n LIMIT 20");
    }

    // alle nodes van een bepaald type.
    public ExecutionResult getNodesByType(String aType) {
        return engine.execute("START n=node:type(type='" + aType + "') RETURN n");
    }

    // alle proteinen met isoforms.
    public ExecutionResult getProteinByAccessionMatch(String aMatch) {
        return engine.execute("START protein=node:type(type='protein') WHERE protein.accession=~'.*" + aMatch + ".*' RETURN protein");
    }
    
    // protein met gegeven accessionummer
    public ExecutionResult getProteinByAccession(String aAccession) {
        return engine.execute("START protein=node:proteins(accession='" + aAccession + "') RETURN protein");
    }
    
    // alle proteinen in een gegeven weefsel.
    public ExecutionResult getProteinByTissue(String aTissue) {
        return engine.execute("START tissue=node:tissue(tissue='" + aTissue +"') "
                              + "MATCH (tissue)-[:PROTEIN_TO_TISSUE]-(protein) "
                              + "RETURN protein");
    }
    
    public ExecutionResult getTissueByProtein(String aAccession) {
        return engine.execute("START protein=node:protein(accession='" + aAccession + "') "
                                                + "MATCH (protein)-[:PROTEIN_TO_TISSUE]->(tissue) "
                                                + "RETURN tissue.tissue");

    }
    

    
    // aantal mimentries + mimentries bij gegeven proteine.
    public ExecutionResult mimCount (String aProtAccession){
        return engine.execute("START protein=node:type(type='protein') "
                              + "MATCH (protein)-[:PROTEIN_TO_MIM]->mim "
                              + "WHERE protein.accession='" + aProtAccession + "' "
                              + "WITH count(mim) as mimCount "
                              + "RETURN mimCount");
    }
    public ExecutionResult getMimByProteinAccession (String aProtAccession) {
        return engine.execute("START protein=node:type(type='protein') "
                              + "MATCH (protein)-[:PROTEIN_TO_MIM]->mim "
                              + "WHERE protein.accession='" + aProtAccession + "' "
                              + "RETURN mim.mimAccession");
    }
    
    public ExecutionResult getProteinByMimAndProtein (String aMim, String aAccession) {
        return engine.execute("START mim=node:mim(mimAccession='" + aMim + "'), proteinA=node:protein(accession='" + aAccession + "') "
                              + "MATCH (protein)-[:PROTEIN_TO_MIM]->(mim) "
                              + "WHERE (protein)-[:PROTEIN_TO_PROTEIN_INTERACTION]-(proteinA) "
                              + "RETURN protein.accession");
    }

    // aantal proteinen bij mimaccessienummer + proteinen.
    public ExecutionResult protCount (String aMimAccession){
         return engine.execute("START mim=node:type(type='mim') "
                               + "MATCH (mim)<-[:PROTEIN_TO_MIM]-(protein)" 
                               + "WHERE mim.mimAccession=~'.*" + aMimAccession + ".*'  "
                               + "WITH count(protein) as protCount "
                               + "RETURN protCount");
     }
    public ExecutionResult getProteinByMimAccession(String aMimAccession) {
       return engine.execute("START mim=node:type(type='mim') "
                             + "MATCH (mim)<-[:PROTEIN_TO_MIM]-(protein)" 
                             + "WHERE mim.mimAccession=~'.*" + aMimAccession + ".*'  "
                             + "RETURN protein.name");
    }
    
     //proteinen met een minimum aantal mimentries.
   public ExecutionResult getProteinWithXMims(int X) {
        return engine.execute("START protein=node:type(type='protein') "
                              + "MATCH (protein)-[:PROTEIN_TO_MIM]-(mim) "
                              + "WITH protein, count(mim) as mims "
                              + "WHERE mims > " + X + " "
                              + "RETURN protein");
    }
    
   /**
    * Returns the protein with a corresponding MIM entry.
    * @return 
    */
    public ExecutionResult getProteinWithMimEntry() {
        ExecutionResult result = engine.execute("START protein=node:type(type='protein') "
                                                + "MATCH (protein)-[:PROTEIN_TO_MIM]->(mim) "
                                                + "RETURN protein, mim");
        return result;
    }
    
    // mimentries gelinkt aan een minimum aantal proteinen.
    public ExecutionResult getMimWithXProteins(int X) {
        return engine.execute("START protein=node:type(type='protein') "
                              + "MATCH (mim)<-[:PROTEIN_TO_MIM]-(protein) "
                              + "WITH mim, count(protein) as proteins "
                              + "WHERE proteins > " + X + " "
                              + "RETURN mim.mimAccession");
    }    
    
    // subcellulaire locatie van proteinen gelinkt aan een bepaalde ziekte.
    public ExecutionResult getLocationAndTissueByMim(String aMimAccession){
        ExecutionResult result = engine.execute("START mim=node:mim(mimAccession='" + aMimAccession + "') "
                                                + "MATCH (mim)<-[:PROTEIN_TO_MIM]-(protein)-[:PROTEIN_TO_LOCATION]->(location) "
                                                + "RETURN distinct protein, location");
        return result;
    }
    public ExecutionResult getLocationByMim(String aMimAccession){
        return engine.execute("START mim=node:mim(mimAccession='" + aMimAccession + "') "
                                                + "MATCH (mim)<-[:PROTEIN_TO_MIM]-(protein)-[:PROTEIN_TO_LOCATION]->(location) "
                                                + "RETURN distinct location.location");
    }
    
    // gemeenschappellijke eiwitten van twee ziektes.
    public ExecutionResult getCommonProteins(String aMimAccession, String bMimAccession) {
        return engine.execute("START mimA=node:mim(mimAccession ='" + aMimAccession + "'),  mimB=node:mim(mimAccession ='" + bMimAccession + "')"
                              + "MATCH (mimA)<-[:PROTEIN_TO_MIM]-(protein)-[:PROTEIN_TO_MIM]->(mimB) "
                              + "RETURN protein.accession");
    }  
    
    // geeft relatie (voorlopig enkel index)
    public ExecutionResult getRelationship(String aAccession, String bAccession) {
        return engine.execute("START proteinA=node:protein(accession = '" + aAccession + "'), proteinB=node:protein(accession = '" + bAccession + "') "
                              + "MATCH (proteinA)-[r]-(proteinB) "
                              + "RETURN re");
    }
    
    // eiwitinteracties
    public ExecutionResult getProteinInteractions(String aAccession) {
        return engine.execute("START proteinA=node:protein(accession = '" + aAccession + "') "
                              + "MATCH (proteinA)-[:PROTEIN_TO_PROTEIN_INTERACTION]-(protein) "
                              + "RETURN distinct protein.name");
    }
    //proteinen met bepaald aantal interacties
    public ExecutionResult getProteinWithXInteractions(int X) {
        return engine.execute("START proteinA=node:type(type='protein') "
                              + "MATCH (proteinA)-[:PROTEIN_TO_PROTEIN_INTERACTION]->(protein) "
                              + "WITH protein, count(protein) as proteins "
                              + "WHERE proteins > " + X + " "
                              + "RETURN protein.accession");
    }
    public ExecutionResult countInteractions (String aAccession){
         return engine.execute("START proteinA=node:protein(accession='" + aAccession+ "') "
                               + "MATCH (proteinA)-[:PROTEIN_TO_PROTEIN_INTERACTION]->(protein)" 
                               + "WITH count(protein) as interactionCount "
                               + "RETURN interactionCount");
    }
    // alle proteinen op afstand 2 van een gegeven proteine.
    public ExecutionResult countProteinByProteinByProtein(String aAccession){
        return engine.execute("START proteinA=node:protein(accession='" + aAccession + "'), proteinB=node:type(type='protein') "
                              + "MATCH (proteinA)-[:PROTEIN_TO_PROTEIN_INTERACTION]->(proteinB)-[:PROTEIN_TO_PROTEIN_INTERACTION]->(protein) "
                              + "WITH count(protein) as protCount "
                              + "RETURN protCount");
    }
    public ExecutionResult getProteinByProteinByProtein(String aAccession){
        return engine.execute("START proteinA=node:protein(accession='" + aAccession + "'), proteinB=node:type(type='protein') "
                              + "MATCH (proteinA)-[:PROTEIN_TO_PROTEIN_INTERACTION]->(proteinB)-[:PROTEIN_TO_PROTEIN_INTERACTION]->(protein) "
                              + "RETURN protein.accession");
    }
    
    // interacting proteins in same tissue.
    public ExecutionResult countInteractingProteinsInSameTissue(String aAccession, String aTissue){
        return engine.execute ("START proteinA=node:protein(accession='" + aAccession + "'), tissue=node:tissue(tissue='" + aTissue + "') "
                              + "MATCH (tissue)<-[:PROTEIN_TO_TISSUE]-(protein) "
                              + "WHERE (tissue)<-[:PROTEIN_TO_TISSUE]-(proteinA)-[:PROTEIN_TO_PROTEIN_INTERACTION]->(protein) "
                              + "WITH count(protein) as proteins "
                              + "RETURN proteins");
    }    
    public ExecutionResult getInteractingProteinsInSameTissue(String aAccession, String aTissue){
        return engine.execute ("START proteinA=node:protein(accession='" + aAccession + "'), tissue=node:tissue(tissue='" + aTissue + "') "
                              + "MATCH (proteinA)-[:PROTEIN_TO_PROTEIN_INTERACTION]-(protein) "
                              + "WHERE (protein)-[:PROTEIN_TO_TISSUE]->(tissue)<-[:PROTEIN_TO_TISSUE]-(proteinA) "
                              + "RETURN distinct protein.name");
    }
    
    public ExecutionResult countProteinByProteinByProteinInSameTissue(String aAccession, String aTissue){
        return engine.execute ("START proteinA=node:protein(accession='" + aAccession + "'), proteinB=node:type(type='protein'), "
                              + "tissue=node:tissue(tissue='" + aTissue + "')  "
                              + "MATCH (proteinA)-[:PROTEIN_TO_PROTEIN_INTERACTION]->(proteinB)-[:PROTEIN_TO_PROTEIN_INTERACTION]->(protein) "
                              + "WHERE (proteinB)-[:PROTEIN_TO_TISSUE]->(tissue)<-[:PROTEIN_TO_TISSUE]-(protein) "
                              + "AND (proteinA)-[:PROTEIN_TO_TISSUE]->(tissue) "
                              + "WITH count(protein) as proteins "
                              + "RETURN proteins");  
    }
        
    // aantal nodes van een gegeven type. 
     public ExecutionResult countNodes(String aType) {
         return engine.execute("START n=node:type(type='" + aType + "') "
                               + "WITH count(n) as cnt "
                               + "RETURN cnt");
     } 
 
}
