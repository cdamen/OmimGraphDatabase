package com.compomics.omimgraphdatabase;

import java.util.*;
import java.io.*;
import java.io.File;
import java.net.URI;
import java.io.OutputStream;
import java.lang.*;

import com.compomics.dbtoolkit.io.implementations.SwissProtDBLoader;
import com.compomics.omimgraphdatabase.properties.ProteinProperty;
import com.compomics.omimgraphdatabase.properties.TissueProperty;
import com.compomics.omimgraphdatabase.properties.MIMProperty;
import com.compomics.omimgraphdatabase.properties.LocationProperty;

import com.teradata.jdbc.jdbc_4.io.TDNetworkIOIF;

import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.kernel.impl.util.StringLogger;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import java.lang.invoke.MethodHandles;
import org.apache.derby.tools.sysinfo;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.importer.spi.DatabaseImporter;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.neo4j.cypherdsl.grammar.ForEach;
import org.neo4j.tooling.GlobalGraphOperations;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.openide.util.Lookup;
import org.springframework.data.graph.neo4j.rest.support.RestGraphDatabase;




	
public class Starter {
    
    private Neo4jGraph graph;
    private Neo4jGraph indexGraph;
    private GraphDb graphDb;   
//    private RestGraphDatabase restGraphDb;            //neo4j online (niet gebruikt)
//    private DatabaseImporter importer;                //gehpi
//    private ProjectController projectController;      //gephi
//    private Workspace workspace;                      //gephi
    private SwissProtDBLoader dbLoader;
    private Index<Vertex> proteinIndex;
    private Index<Vertex> tissueIndex;
    private Index<Vertex> mimIndex;
    private Index<Vertex> typeIndex;
    private Index<Vertex> locationIndex;

    
    private HashMap<String, Vertex> tissueCache = new HashMap<String, Vertex>();
    private HashMap<String, Vertex> mimCache = new HashMap<String, Vertex>();
    private HashMap<String, Vertex> locationCache = new HashMap<String, Vertex>();
   


    
    
    /**
     * Constructor that creates the graph database.
     * 
     * @param aDBFile   File with the database file to open. Should be readable.
     * @throws IOException when the database file specified could not be read,
     *                     or if a folder was specified instead of a file.
     */
    public Starter(File aDBFile) throws IOException {
        this.proteinIndex = null;
        this.graph = null;
        
        // Check whether the file exists!
        if(!aDBFile.exists()) {
            throw new IOException("For the database to open, you specified a file that does not exist ('" + aDBFile.getAbsolutePath() + "')!");
        }
        
        // Check whether we have a file, not a folder!
        if(aDBFile.isDirectory()) {
            throw new IOException("For the database to open, you specified a folder ('" + aDBFile.getAbsolutePath() + "'), not a file!");
        }
        
        this.dbLoader = new SwissProtDBLoader();
        if(!dbLoader.canReadFile(aDBFile)) {
            throw new IOException("Cannot read file '" + aDBFile.getAbsolutePath() + "'! Not a UniProtKB/Swiss-Prot DAT file!");
        }
        this.dbLoader.load(aDBFile.getAbsolutePath());
    }
    
    public void connectDatabase(String aFilename, boolean aCleanDB) throws IOException {
        // Initialize graph database.
        graphDb = new GraphDb(aFilename, aCleanDB);
//            restGraphDb = new RestGraphDatabase(new URI("http://localhost:7474/webadmin"));   //neo4j localhost

        //graphDatabase maken
        GraphDatabaseService graphDbService = graphDb.getGraphDatabase();
        this.graph = new Neo4jGraph(graphDbService);
        this.indexGraph = new Neo4jGraph(graphDbService);
        
        // Als we met een cleane DB moesten beginnen, zullen we die best vullen.
        if(aCleanDB) {
            fillGraphDatabase();
        }
    }
    
    public void fillGraphDatabase() throws IOException {
            // Indices maken.
            setupIndices();

           
            // Door alle eiwitten in DB gaan
            String entry;
            // HashMap die alle eiwitvertexen zal bevatten, met als key de accessienummer.
            HashMap<String, Vertex> proteinAccessionToVertexMap = new HashMap<String, Vertex>();
            // Set van alle paarsgewijze interacties.
            HashSet<String[]> interactionsSet = new HashSet<String[]>();
            while((entry = dbLoader.nextRawEntry()) != null) {
                // Raw entry omzetten in HashMap
                HashMap proteinHashMap = dbLoader.processRawData(entry);
                // Properties uit HashMap halen
                // Eerst protein
                String accession = (String) proteinHashMap.get("AC");
                // meerdere accessienummers per lijn, gescheiden door ;
                // de eerste is de primary, die alleen willen we hebben.
                if(accession.indexOf(";") > 0) {
                    accession = accession.substring(0, accession.indexOf(";"));
                }
                String sequence = (String) proteinHashMap.get("  ");
                // Create protein vertex
                Vertex proteinVertex = createProteinVertex(accession, sequence);
                // Store protein vertex in accession to vertex lookup map.
                proteinAccessionToVertexMap.put(accession, proteinVertex);
                
                //Nu tissue
                HashSet<String> tissueSet = new HashSet<String>();
                ArrayList<HashMap> rnSections = (ArrayList<HashMap>) proteinHashMap.get("[RN]");
                // Voor elk element in de rnSections list, de HashMap
                // met keys en values opvragen, en daaruit telkens de
                // RC value(s) halen.
                for(HashMap rnSection:rnSections) {
                    if(rnSection.containsKey("RC") && !((String)rnSection.get("RC")).trim().equals("")) {
                        //  Vraag de RC lijn op
                        String tissue = (String)rnSection.get("RC");
                        // Controleer of we wel degelijk een tissue vast hebben
                        // (er zijn meerdere soorten RC lijnen!)
                        if(tissue.startsWith("TISSUE=")) {
                            // Verwijder de TISSUE= leader
                            tissue = tissue.substring(7);
                            // Er kunnen meerdere tissues opgegeven zijn,
                            // gescheiden door ;
                            String[] tissues = tissue.split(";");
                            for(int i=0; i<tissues.length; i++) {
                                // Each of these tissues can further be divided into individual tissues
                                // formatted as: 'Amygdala, Brain, and Uterus'. Note that whitespace
                                // and 'and' are sometimes optional...
                                String[] singleTissues = tissues[i].split("\\s*,\\s*(?:and)?\\s*");
                                for(int j=0; j<singleTissues.length; j++) {
                                    tissueSet.add(singleTissues[j]);
                                }
                            }
                        }
                    }
                }
                 //We hebben alle unieke tissues voor dit eiwit in de tissueSet collectie.
                 //Nu gaan we elk van deze in de databank opslaan (als ze er al niet in zitten!)
                 //en ze aan dit eiwit linken.
                Iterator<String> iter = tissueSet.iterator();
                while(iter.hasNext()) {
                    String tissue = iter.next();
                    Vertex tissueVertex = createTissueVertex(tissue);       
                    createProteinToTissueEdge(proteinVertex, tissueVertex);
                }
                    
              //Comments: nog splitsen in verschillende zaken: FUNCTION, INTERACTION, ...
              HashSet<String> locationSet = new HashSet<String>();
              String comments = (String) proteinHashMap.get("CC");
              if (comments.contains("-!- ")){
                    String[] commentParts = comments.split("-!- ");
                    for(int i=0;i<commentParts.length;i++) {
                        String commentPart = commentParts[i];
                        if(commentPart.startsWith("INTERACTION:")) {
                            //elke lijn nodig tot :
                            //elke lijn aparte vertex, die normaal al bestaat --> edge maken
                            commentPart = commentPart.substring(12).trim();
                            String[] interactionPart = commentPart.split("\n");
                            for(int j=0; j<interactionPart.length; j++) {
                                // Als een INTERACTION sectie gevolgd wordt door de copyright notice,
                                // wordt deze laatste ook nog als interaction aangezien. Om dit te voorkomen,
                                // stoppen we met parsen als we het begin van de copyright lijn vinden.
                                if(interactionPart[j].startsWith("----------")) {
                                    break;
                                }
                                // Een eiwit kan met zichzelf interageren, en dan is dat anders aangegegeven.
                                // Dit wordt hier afgevangen.
                                if(interactionPart[j].startsWith("Self;")) {
                                    interactionsSet.add(new String[]{accession, accession});
                                } else if(interactionPart[j].contains(":")) {
                                    String interactorAccession = interactionPart[j].substring(0,interactionPart[j].indexOf(":"));
                                    // Remove isoform specification.
                                    if(interactorAccession.contains("-")) {
                                        interactorAccession = interactorAccession.substring(0, interactorAccession.indexOf("-"));
                                    }
                                    // Sommige interagerende eiwitten zijn NIET van humane oorsprong,
                                    // en deze zullen dus ook niet gevonden worden in onze protein accession map.
                                    // Daarom maken we er hier een speciaal eiwit van - een xeno-eiwit.
                                    if(interactionPart[j].contains("(xeno)")) {
                                        // Maak een nieuwe xenoproteinvertex.
                                        Vertex xenoProtein = createXenoProteinVertex(interactorAccession);
                                        proteinAccessionToVertexMap.put(interactorAccession, xenoProtein);
                                    }
                                    interactionsSet.add(new String[] {accession,interactorAccession});    
                                } else {
                                    System.out.println("Weird interaction line found: " + interactionPart[j]);
                                }
                            }
                        }if (commentPart.startsWith("SUBCELLULAR LOCATION:")) {
                             commentPart = commentPart.substring(21).trim();
//                             System.out.println(commentPart);
                             String[] location = commentPart.split("\\.");
                                for(int j=0; j<location.length; j++){ 
                                //unieke subcellulaire locaties in HashSet
                                    // Note= skippen tot het einde
                                    if (!location[j].startsWith(" Note=")){
                                        locationSet.add(location[j]);
                                    }
                                    if (location[j].startsWith(" Note=")){
                                        break;
                                    }   
                                }                                                      
                    }
                    Iterator<String> iterator = locationSet.iterator();
                    while(iterator.hasNext()) {
                        String location = iterator.next();
                        Vertex locationVertex = createLocationVertex(location);       
                        createProteinToLocationEdge(proteinVertex, locationVertex);
                        }
                    } 
                }

                //DR lines: de MIM entries eruit halen.
                String DRs = (String) proteinHashMap.get("DR");
                int currentIndex = -1;
                int startIndex = 0;
                while( (currentIndex = DRs.indexOf("MIM;", startIndex)) >= 0) {
                    startIndex = currentIndex+4;
                    String mimString = DRs.substring(startIndex, DRs.indexOf(".", startIndex));
                    // Nu hebben we een string van de vorm " 12345; gene."
                    // Deze parsen we nog een beetje verder.
                    if(!mimString.contains(";")) {
                        System.out.println("Special MIM string: " + mimString + "\n --> Skipping it!");
                        continue;
                    }
                    int semicolonIndex = mimString.indexOf(";");
                    String mimAccession = mimString.substring(0, semicolonIndex).trim();
                    String mimType = mimString.substring(semicolonIndex+1).trim();
                    // Nu nog een MIM vertex maken, en aan het eiwit linken.
                    Vertex mimVertex = createMIMVertex(mimAccession, mimType);
                    createProteinToMIMEdge(proteinVertex, mimVertex);
                }
            }
            
            // Alle eiwitten zijn ingelezen en opgeslaan.
            // Nu moeten we alle bijgehouden interacties nog aanmaken.
            Iterator<String[]> iterator = interactionsSet.iterator();
            while(iterator.hasNext()) {
                // Haal de accessienummers op.
                String[] accessions = iterator.next();
                String accessionFrom = accessions[0];
                String accessionTo = accessions[1];
                // Zoek de eiwitvertexen op in de Map.
                if(!proteinAccessionToVertexMap.containsKey(accessionFrom)){
                    System.err.println(" *** Serious issue! Protein '" + accessionFrom + "', encountered in DB as an originator of interaction, is not found in the proteinvertex map!\n *** Skipping it!");
                    continue;
                }
                if(!proteinAccessionToVertexMap.containsKey(accessionTo)){
                    System.err.println(" *** Protein '" + accessionTo + "', encountered in DB as a destination of interaction, is not found in the proteinvertex map!\n *** Skipping it!");
                    continue;
                }
                Vertex proteinFrom = proteinAccessionToVertexMap.get(accessionFrom);
                Vertex proteinTo = proteinAccessionToVertexMap.get(accessionTo);
                createProteinToProteinInteractionEdge(proteinFrom, proteinTo);
            }
    }
    

    

    public Vertex createProteinVertex(String accession, String sequence) {
        Vertex vertex = graph.addVertex(null);

        vertex.setProperty(ProteinProperty.ACCESSION.toString(), accession);
        proteinIndex.put(ProteinProperty.ACCESSION.toString(), accession, vertex);

        vertex.setProperty(ProteinProperty.SEQUENCE.toString(), sequence);
        proteinIndex.put(ProteinProperty.SEQUENCE.toString(), sequence, vertex);

        typeIndex.put("type", "protein", vertex);
        
        return vertex;
    }

    public Vertex createXenoProteinVertex(String accession) {
        Vertex vertex = graph.addVertex(null);

        vertex.setProperty(ProteinProperty.ACCESSION.toString(), accession);

        typeIndex.put("type", "xenoprotein", vertex);
        
        return vertex;
    }


    public Vertex createTissueVertex (String tissue){
        // EERST KIJKEN OF AL BESTAAT!!!! (cfr. lazy caching)
        Vertex tissueVertex = null;
        
        if(tissueCache.containsKey(tissue)) {
            tissueVertex = tissueCache.get(tissue);
        } else {
            tissueVertex = graph.addVertex(null);
            tissueVertex.setProperty(TissueProperty.TISSUE.toString(), tissue);
            tissueIndex.put(TissueProperty.TISSUE.toString(), tissue, tissueVertex);
            typeIndex.put("type", "tissue", tissueVertex);
            
            tissueCache.put(tissue, tissueVertex);
        }
        
        return tissueVertex;
    }

    public Vertex createMIMVertex (String mimAccession, String mimType){
        // EERST KIJKEN OF AL BESTAAT!!!! (cfr. lazy caching)
        Vertex mimVertex = null;
        
        if(mimCache.containsKey(mimAccession)) {
            mimVertex = mimCache.get(mimAccession);
        } else {
            mimVertex = graph.addVertex(null);
            
            mimVertex.setProperty(MIMProperty.MIMACCESSION.toString(), mimAccession);
            mimIndex.put(MIMProperty.MIMACCESSION.toString(), mimAccession, mimVertex);
                        
            mimVertex.setProperty(MIMProperty.MIMTYPE.toString(), mimType);
            mimIndex.put(MIMProperty.MIMTYPE.toString(), mimAccession, mimVertex);            

            typeIndex.put("type", "mim", mimVertex);
            
            mimCache.put(mimAccession, mimVertex);
        }
        
        return mimVertex;
    }
    
        public Vertex createLocationVertex(String location){
        Vertex locationVertex = null;
        
        if(locationCache.containsKey(location)) {
            locationVertex = locationCache.get(location);
        } else {
            locationVertex = graph.addVertex(null);
            locationVertex.setProperty(LocationProperty.LOCATION.toString(), location);
            locationIndex.put(LocationProperty.LOCATION.toString(), location, locationVertex);
            typeIndex.put("type", "location", locationVertex);
            
            locationCache.put(location, locationVertex);
        }
        
        return locationVertex;
    }
    
    
    //Edge
    private void createProteinToTissueEdge(Vertex proteinVertex, Vertex tissueVertex) {
        graph.addEdge(null, proteinVertex, tissueVertex, "PROTEIN_TO_TISSUE");
    }

    private void createProteinToMIMEdge(Vertex proteinVertex, Vertex mimVertex) {
        graph.addEdge(null, proteinVertex, mimVertex, "PROTEIN_TO_MIM");
    }

    private void createProteinToProteinInteractionEdge(Vertex proteinFrom, Vertex proteinTo){
        graph.addEdge(null, proteinFrom, proteinTo, "PROTEIN_TO_PROTEIN_INTERACTION");
    }
    
    private void createProteinToLocationEdge(Vertex proteinVertex, Vertex locationVertex){
        graph.addEdge(null, proteinVertex, locationVertex, "PROTEIN_TO_LOCATION");
    }

  
    // Interaction type
    public static enum InteractionType implements RelationshipType {
        PROTEIN_TO_TISSUE,
        PROTEIN_TO_MIM,
        PROTEIN_TO_PROTEIN_INTERACTION,
        PROTEIN_TO_LOCATION
    }
   
    public void setupIndices() {
        // Protein index
        proteinIndex = indexGraph.getIndex("proteins", Vertex.class);
        if (proteinIndex == null) {
            proteinIndex = indexGraph.createIndex("proteins", Vertex.class);
            ((TransactionalGraph) indexGraph).stopTransaction(TransactionalGraph.Conclusion.SUCCESS); 
        }

        // Tissue index
        tissueIndex = indexGraph.getIndex("tissue", Vertex.class);
        if (tissueIndex == null) {
            tissueIndex = indexGraph.createIndex("tissue", Vertex.class);
            ((TransactionalGraph) indexGraph).stopTransaction(TransactionalGraph.Conclusion.SUCCESS); 
        }

        // MIM index
        mimIndex = indexGraph.getIndex("mim", Vertex.class);
        if (mimIndex == null) {
            mimIndex = indexGraph.createIndex("mim", Vertex.class);
            ((TransactionalGraph) indexGraph).stopTransaction(TransactionalGraph.Conclusion.SUCCESS); 
        }
        
        // Location index
        locationIndex = indexGraph.getIndex("location", Vertex.class);
        if (locationIndex == null){
            locationIndex = indexGraph.createIndex("location", Vertex.class);
            ((TransactionalGraph) indexGraph).stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
        }
        
        
        // Type index
        typeIndex = indexGraph.getIndex("type", Vertex.class);
        if (typeIndex == null) {
            typeIndex = indexGraph.createIndex("type", Vertex.class);
            ((TransactionalGraph) indexGraph).stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
        }
    }


    // een GraphML bestand maken van de graph
    public static void exportGraphML(Graph outputGraph, File outputFile) throws FileNotFoundException, IOException {
        GraphMLWriter writer = new GraphMLWriter(outputGraph);
        writer.setNormalize(true);
        writer.outputGraph(new FileOutputStream(outputFile));
    } 

    
    public Graph getGraph() {
        return this.graph;
    }
    
    public void queryProteins() {
	ExecutionEngine engine = new ExecutionEngine(graphDb.getGraphDatabase(), StringLogger.SYSTEM);
	CypherQuery cypherQuery = new CypherQuery(engine);       
        
        //alle nodes.
//        GraphDb.printResult("Get all nodes.", cypherQuery.getAllNodes(), "n");
        //alle xeno's.
//        GraphDb.printResult("Retrieving all xenoproteins:", cypherQuery.getNodesByType("xenoprotein"), "n");
        //alle nodes van een bepaald type.
//        GraphDb.printResult("Retrieving all MIM entries:", cypherQuery.getNodesByType("tissue"), "n");
//        GraphDb.printResult("Retrieving all sublocation entries:", cypherQuery.getNodesByType("location"), "n");
        //alle proteinen met isoforms.
//        GraphDb.printResult("Protein isoforms detected:", cypherQuery.getProteinByAccessionMatch("-"), "protein");
        //proteine zoeken op accessienummer.
//        GraphDb.printResult("Protein Q9P0K1:", cypherQuery.getProteinByAccessionMatch("Q9P0K"), "protein");
//        GraphDb.printResult("Retrieving specific protein:", cypherQuery.getProteinByAccession("Q8R511"), "protein"); 
        //alle proteinen in een specifiek weefsel.
//        GraphDb.printResult("Get protein by tissue.", cypherQuery.getProteinByTissue("Skin"), "protein.accession");
        
        //alle proteinen die minstens X mimentries hebben.
//        GraphDb.printResult("Get proteins with 10 mimentries.", cypherQuery.getProteinWithXMims(10), "protein.accession");
        //alle mims bij speciefiek proteine
        GraphDb.printResult("Count mims attached to protein.", cypherQuery.mimCount("P02545"), "mimCount");
        GraphDb.printResult("Get mims attached to protein.", cypherQuery.getMimByProteinAccession("P02545"), "mim.mimAccession");
        
        //alle mimentries die met een minimum aantal proteinen gelinkt zijn.
//        GraphDb.printResult("Get mims with 10 proteins.", cypherQuery.getMimWithXProteins(10), "mim.mimAccession");        
        //alle proteinen bij een specifieke mimaccessienummer.
//        GraphDb.printResult("Count proteins attached to mimaccession.", cypherQuery.protCount("142830"), "protCount");
//        GraphDb.printResult("Get protein by mimaccession.", cypherQuery.getProteinByMimAccession("142800"), "protein.accession");

        //Subcellulaire locatie van proteinen gelinkt aan een bepaalde mim.
//        GraphDb.printResult("Get subcellular location by mim.", cypherQuery.getLocationByMim("252010"), "location");
        
        //gemeenschappelijke eiwitten bij mims.
//        GraphDb.printResult("Get common proteins.", cypherQuery.getCommonProteins("604967", "604966"), "protein.accession");
        
        //aantal nodes van een bepaald type.
//        GraphDb.printResult("Count nodes of a given type.", cypherQuery.countNodes("tissue"), "cnt");
        
        //werkt nog niet
//        GraphDb.printResult("Get mimbymim.", cypherQuery.getMimByMimViaProtein(), "mim.mimAccession");
        
        //relationship
        //werkt nog niet
//        GraphDb.printResult("Get relationship.", cypherQuery.getRelationship(), "r");
    }
    

    public void closeGraphDb() throws IOException {
        if(graph != null) {
            graph.shutdown();
        } else {
            System.out.println("Database did not exist.");
        }
    }
    

	
    public static void main(String[] args) {
        File file = new File("C:/Users/Caroline/Documents/geneeskunde/HP/database uniprotein human/uniprot_sprot_human.dat");
        /* dit moet het bestand met alle nodes en edges zijn
         * C:\Users\Caroline\Documents\geneeskunde\HP\Neo4j\data\graph.db
         * met behulp van Gremlin naar graphdboutput.xml schrijven
         */
//        File graphDbFile = new File ("C:/Users/Caroline/Documents/geneeskunde/HP/graphdboutput.xml");
        Starter starter = null;
        try {
            starter = new Starter(file);        
            // true om db te cleanen en opnieuw op te vullen (=cleanstart)
            // false om bestaande db te gebruiken
            starter.connectDatabase("C:/Users/Caroline/Documents/geneeskunde/HP/GraphDatabase", false);
            starter.queryProteins();
            Starter.exportGraphML(starter.getGraph(), new File ("C:/Users/Caroline/Documents/geneeskunde/HP/outputGraph.GraphML"));
//            GephiGraph.makeGraph(graphDbFile);
            starter.closeGraphDb();
        } catch(IOException ioe) {
            System.err.println("Lap! Vodden! " + ioe.getMessage());
            ioe.printStackTrace();
        } finally {
            System.out.println("\n\n** Shutting down database");
            if(starter != null) {
                try {
                    starter.closeGraphDb();
                    System.out.println(" --> Database shut down normally.");
                } catch(IOException ioe) {
                    System.out.println(" !! Error shutting down DB: " + ioe.getMessage());
                }
            }
        }
    }
}