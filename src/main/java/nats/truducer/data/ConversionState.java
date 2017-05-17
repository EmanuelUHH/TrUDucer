package nats.truducer.data;

import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by felix on 01/02/17.
 */
public class ConversionState {

    private Root tree = null;
    private List<DepTreeFrontierNode> frontier = new ArrayList<>();
    private Rule lastRule = null;

    private ConversionState() {

    }

    public ConversionState(Root tree) {
        this.tree = tree;
        DepTreeFrontierNode dtfn = new DepTreeFrontierNode(tree.getNode().getChildren());
        this.frontier.add(dtfn);
    }

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

    public void setLastRule(Rule rule) {
        this.lastRule = rule;
    }

    /**
     * The Rule which lead to this state.
     */
    public Rule getAppliedRule() {
        return this.lastRule;
    }
}
