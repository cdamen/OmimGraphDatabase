package com.compomics.omimgraphdatabase;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;

/**
 *
 * @author Caroline
 */
public class TransverserQuery {
	/**
	 * Returns the interactions for a specific protein node.
	 * @param protein The protein node.
	 * @return Traverser object with all paths showing connected to other proteins.
	 */
    public static Traverser getTissueInteractions(final Node tissue) {
        TraversalDescription td = Traversal.description()
                .breadthFirst().relationships(Starter.InteractionType.PROTEIN_TO_TISSUE, Direction.OUTGOING )
                .evaluator(Evaluators.excludeStartPosition());
        return td.traverse(tissue);
    }
    
    public static Traverser getMIMInteractions(final Node mim) {
        TraversalDescription td = Traversal.description()
                .breadthFirst().relationships(Starter.InteractionType.PROTEIN_TO_MIM, Direction.OUTGOING )
                .evaluator(Evaluators.excludeStartPosition());
        return td.traverse(mim);
    }
    
     public static Traverser getProteinToProteinInteractions(final Node accessionTo) {
        TraversalDescription td = Traversal.description()
                .breadthFirst().relationships(Starter.InteractionType.PROTEIN_TO_PROTEIN_INTERACTION, Direction.OUTGOING )
                .evaluator(Evaluators.excludeStartPosition());
        return td.traverse(accessionTo);
    }
    
    /**
     * Get 
     * @param node1
     * @param node2
     * @return
     */
    public static Iterable<Path> getAllPaths(Node node1, Node node2) {
    	PathFinder<Path> allPaths = GraphAlgoFactory.allPaths(Traversal.expanderForAllTypes(), 10);
    	return allPaths.findAllPaths(node1, node2);
    }
    
    public static Traverser getPaths(Node protein){
    	TraversalDescription td = Traversal.description().depthFirst()
                .evaluator(Evaluators.excludeStartPosition());
        return td.traverse(protein);
    }
}  

