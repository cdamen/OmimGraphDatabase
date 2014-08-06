package com.compomics.omimgraphdatabase;

import com.compomics.omimgraphdatabase.properties.TissueProperty;
import static org.neo4j.helpers.collection.MapUtil.map;

import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.cypher.javacompat.*;

import scala.collection.immutable.Map;

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

    // alle nodes met een gegeven (gedeelte) accessienummer.
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
                              + "RETURN protein.accession LIMIT 20");
    }
    
    // aan mimentries + mimentries bij gegeven proteine.
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
                             + "RETURN protein.accession");
    }
    
     //proteinen met een minimum aantal mimentries.
   public ExecutionResult getProteinWithXMims(int X) {
        return engine.execute("START protein=node:type(type='protein') "
                              + "MATCH (protein)-[:PROTEIN_TO_MIM]-(mim) "
                              + "WITH protein, count(mim) as mims "
                              + "WHERE mims > " + X + " "
                              + "RETURN protein.accession");
    }
   
//   public ExecutionResult getProteinWithXMims() {
//           ExecutionResult result = engine.execute("START protein=node:type(type='protein') "
//                                              + "MATCH (protein)-[:PROTEIN_TO_MIM]-(mim) "
//                                              + "RETURN mim, protein");
//            String table = "";
//            String header = "";
//            boolean first = true;
////            while (result.hasNext()){
////              Map<String, Object>  row = result.next();
//            for ( Map<String, Object> row : result) ) {
//                for ( Entry<String, Object> column : row.entrySet() ) {
//                    if(first) {
//                        header += column.getKey() + "\t";  
//                    }
//                    table += column.getValue() + "\t";
//                }
//                if(first) {
//                    header += "\n";
//                    first = false;
//                }
//                table += "\n";
//    return result;
//}}
    
    // mimentries gelinkt aan een minimum aantal proteinen.
    public ExecutionResult getMimWithXProteins(int X) {
        return engine.execute("START protein=node:type(type='protein') "
                              + "MATCH (mim)<-[:PROTEIN_TO_MIM]-(protein) "
                              + "WITH mim, count(protein) as proteins "
                              + "WHERE proteins > " + X + " "
                              + "RETURN mim.mimAccession");
    }    
    
    // subcellulaire locatie van proteinen gelinkt aan een bepaalde ziekte.
    public ExecutionResult getLocationByMim(String aMimAccession){
        return engine.execute("START mim=node:type(type='mim') "
                              + "MATCH (mim)<-[:PROTEIN_TO_MIM]-(protein)-[:PROTEIN_TO_LOCATION]->(location) "
                              + "WHERE mim.mimAccession=~'" + aMimAccession + "'  "
                              + "RETURN distinct location");
    }
    
    // gemeenschappellijke eiwitten van twee ziektes.
    public ExecutionResult getCommonProteins(String aMimAccession, String bMimAccession) {
        return engine.execute("START mimA=node:mim(mimAccession ='" + aMimAccession + "'),  mimB=node:mim(mimAccession ='" + bMimAccession + "')"
                              + "MATCH (mimA)<-[:PROTEIN_TO_MIM]-(protein)-[:PROTEIN_TO_MIM]->(mimB) "
                              + "RETURN protein.accession");
    }
     

     // aantal nodes van een gegeven type. 
     public ExecutionResult countNodes(String aType) {
         return engine.execute("START n=node:type(type='" + aType + "') "
                               + "WITH count(n) as cnt "
                               + "RETURN cnt");
     } 
     
//     // aantal mimentries + mimaccessions gelinkt aan proteinen die voorkomen bij mimentries met een minimum aantal proteinen
//     public ExecutionResult getMimByMimViaProtein() {
//         return engine.execute("START protein=node:type(type='protein') "
//                               + "MATCH (mim)-[:PROTEIN_TO_MIM]-(protein)-[:PROTEIN_TO_MIM]-(mim) "
//                               + "WHERE mim.id != mim2.id "        
//                               + "RETURN mim.mimAccession");
//     }
}