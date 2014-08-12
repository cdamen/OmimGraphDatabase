package com.compomics.omimgraphdatabase;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.impl.util.FileUtils;
import scala.collection.Iterator;


public class GraphDb {
    private GraphDatabaseService graphDb;
     
    public GraphDb(String dbPath, boolean cleanStart) {
        // Clear the database.
        if(cleanStart) clearDatabase(dbPath);
        
        // Start the database.
        startDatabase(dbPath);
    }
    
    public void startDatabase(String dbPath) {
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(dbPath);
        //registerShutdownHook(graphDb);
    }
   
    public GraphDatabaseService getGraphDatabase(){
        return graphDb;
    }

    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
        // 
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                    graphDb.shutdown();
            }
        });
    }
	
    public static void printResult(String msg, ExecutionResult result, String column) {
        System.out.println(msg);
        ResourceIterator<Object> columnAs = result.columnAs(msg);
        while (columnAs.hasNext()) {
            final Object value = columnAs.next();
            if (value instanceof Node) {
                Node n = (Node)value;
                //System.out.println("[");
                for (String key : n.getPropertyKeys()) {
                    System.out.println("{ " + key + " : " + n.getProperty(key)	+ "; id: " + n.getId() + " } ");
                }
               //System.out.println("]");
            } else {
                System.out.println("{ " + column + " : " + value + " } ");
            }
        }
    }
    
    public static void printMultipleColumnResult(String msg, ExecutionResult result) {
        System.out.println(msg);
        List<String> columns = result.columns();
        for (Map<String, Object> map : result) {
            for (String col : columns) {
                Node n = (Node) map.get(col);
                for (String key : n.getPropertyKeys()) {
                    System.out.println("{ " + key + " : " + n.getProperty(key) + "; id: " + n.getId() + " } ");
                }
            }
        }
    }

    /**
     * Clears the database.
     * @param dbPath Database path.
     */
    private void clearDatabase(String dbPath) {
        try {
            FileUtils.deleteRecursively(new File(dbPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }   
}
