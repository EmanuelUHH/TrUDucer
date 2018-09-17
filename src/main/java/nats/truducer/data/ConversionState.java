package nats.truducer.data;

import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The ConversionState encodes information about a tree that is currently being converted.
 * In addition to the tree, the conversion frontier is kept track of.
 * This is done seperately instead of with actual nodes in the tree for technical reasons,
 * as to not mix content nodes and frontier nodes.
 */
public class ConversionState {

    private Root tree = null;
    private List<DepTreeFrontierNode> frontier = new ArrayList<>();
    private Rule appliedRule = null;

    // ** changed by Maximilian
    // here the nodes that where changed are stored
    private List<Node> changedNodes;

    private ConversionState() {

    }

    /**
     * Creates a conversion state with the given tree.
     * The classifier is used to find the frontier in the tree.
     * Usually initially there is only a single frontier node at the top of the tree.
     */
    public ConversionState(Root tree, NodeClassifier classifier) {
        this.tree = tree;
        findFrontier(tree.getNode(), classifier);
    }

    /**
     * Traverses the tree from the top and determines the location of the frontier by
     * looking for the first unconverted nodes, using the definition given in the nodeclassifier.
     */
    private void findFrontier(Node node, NodeClassifier classifier) {
        List<Node> children = node.getChildren();
        List<Node> unconvertedNodes = children.stream().filter(classifier::isSourceNode).collect(Collectors.toList());
        List<Node> convertedNodes = children.stream().filter(classifier::isTargetNode).collect(Collectors.toList());
        if (!unconvertedNodes.isEmpty()) {
            DepTreeFrontierNode dtfn = new DepTreeFrontierNode(unconvertedNodes);
            frontier.add(dtfn);
        }
        if (!convertedNodes.isEmpty()) {
            for (Node convertedNode : convertedNodes) {
                findFrontier(convertedNode, classifier);
            }
        }
    }

    /**
     * Creates a deep copy of a Conversion state.
     */
    public ConversionState deepCopy() {
        ConversionState copy = new ConversionState();
        copy.tree = this.tree.copyTree();
        List<Node> newNodes = copy.tree.getDescendants();
        for (DepTreeFrontierNode fn : this.frontier) { // copy frontier
            copy.frontier.add(fn.deepCopy(newNodes));
        }
        assert copy.frontier.size() == this.frontier.size();
        return copy;
    }

    /**
     * Merges frontier nodes that have the same parent.  Two nodes are replaced with a single node,
     * combining the children of the two seperate nodes.
     * This is necessary for certain rule matches and also makes sense intuitively.
     * This method should be called after the frontier is modified.
     */
    public void mergeNeighboringFrontierNodes() {
        List<DepTreeFrontierNode> newFrontier = new ArrayList<>(frontier);

        boolean frontierChanged = true;

        while (frontierChanged) {
            frontierChanged = false;
            List<DepTreeFrontierNode> frontierCopy = new ArrayList<>(frontier);
            for (DepTreeFrontierNode fn : frontierCopy) {
                for (DepTreeFrontierNode fn2 : frontierCopy) {
                    if (fn.equals(fn2)) {
                        continue;
                    }
                    if (fn.getParent().equals(fn2.getParent())) {
                        DepTreeFrontierNode newNode = new DepTreeFrontierNode(fn.getChildren());
                        newNode.addChildren(fn2.getChildren());
                        frontier.remove(fn);
                        frontier.remove(fn2);
                        frontier.add(newNode);
                        frontierChanged = true;
                        break;
                    }
                }
                if (frontierChanged)
                    break;
            }
        }

    }

    /**
     * Conversion is finished if there are no more frontier nodes.
     */
    public boolean finished() {
        return frontier.isEmpty();
    }

    public List<DepTreeFrontierNode> getFrontier() {
        return frontier;
    }

    public Root getTree() {
        return this.tree;
    }

    @Override
    public String toString() {
        if (this.finished())
            return "[]";
        StringBuilder sb = new StringBuilder("[" + getFrontier().get(0).toString());
        for (int i = 1; i < frontier.size(); i++) {
            sb.append(", " + frontier.get(i).toString());
        }
        sb.append("]");
        return sb.toString();
    }

    public void setAppliedRule(Rule rule) {
        this.appliedRule = rule;
    }

    /**
     * The Rule which lead to this state.
     */
    public Rule getAppliedRule() {
        return this.appliedRule;
    }

    public void setChangedNodes(List<Node> changedNodes) {
        this.changedNodes = changedNodes;
    }

    public List<Node> getChangedNodes() {
        return this.changedNodes;
    }
}
