package nats.truducer.data;

import cz.ufal.udapi.core.Node;

/**
 * The node classifier is used in place of the traditional sets of
 * source and target nodes in a transducer.
 * The classifier encodes the node sets in predicates.
 * An implementation could also encode this with actual sets.
 * The current implementation assumes that the source nodes are all uppercase,
 * while the target nodes are all lowercase, as is the case for UD.
 */
public class NodeClassifier {

    public boolean isSourceNode(Node node) {
        return node.getDeprel().equals(node.getDeprel().toUpperCase());
    }

    public boolean isTargetNode(Node node) {
        return node.getDeprel().equals(node.getDeprel().toLowerCase());
    }
}
