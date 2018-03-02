package nats.truducer.data;

import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;

import java.util.ArrayList;
import java.util.List;

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

    private ConversionState() {

    }

    /**
     * Creates an initial Conversion state from the given tree, with a single frontier node,
     * right above the root of the tree.
     */
    public ConversionState(Root tree) {
        this.tree = tree;
        DepTreeFrontierNode dtfn = new DepTreeFrontierNode(tree.getNode().getChildren());
        // tree.getNode().getChildren() returns the first 'real' node, as getChildren() returns the
        // technical root.
        this.frontier.add(dtfn);
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
}
