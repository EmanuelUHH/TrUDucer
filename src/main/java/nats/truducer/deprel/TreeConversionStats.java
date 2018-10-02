package nats.truducer.deprel;

import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;
import nats.truducer.data.Rule;

import java.util.ArrayList;
import java.util.List;

import static nats.truducer.deprel.TreeUtils.getById;

/**
 * Created by felix on 19/03/18.
 */
public class TreeConversionStats {

    private Root originalTree;
    private Root generatedTree;

    private List<Node> convertedNodes = new ArrayList<>();
    private List<Node> blockerNodes = new ArrayList<>();
    private List<Node> indirectlyNotConvertedNodes = new ArrayList<>();
    private List<Node> punctuationNodes = new ArrayList<>();
    private List<Rule> rulesUsed;

    public TreeConversionStats(Root originalTree, Root generatedTree) {
        this.originalTree = originalTree;
        this.generatedTree = generatedTree;
    }

    public void check() {
        for (Node n : generatedTree.getNode().getChildren()) {
            checkNode(n, originalTree);
        }
    }

    private void checkNode(Node node, Root original) {
        if (node.getDeprel().equals("ROOT")) {
            punctuationNodes.add(node);
        } else if (node.getDeprel().equals(getById(original, node.getOrd()).getDeprel())) {
            blockerNodes.add(node);
            for (Node n : node.getDescendants()) {
                indirectlyNotConvertedNodes.add(n);
            }
        } else {
            convertedNodes.add(node);
            for (Node n : node.getChildren()) {
                checkNode(n, original);
            }
        }
    }

    public Root getOriginalTree() {
        return originalTree;
    }

    public Root getGeneratedTree() {
        return generatedTree;
    }

    public List<Node> getConvertedNodes() {
        return convertedNodes;
    }

    public List<Node> getBlockerNodes() {
        return blockerNodes;
    }

    public List<Node> getIndirectlyNotConvertedNodes() {
        return indirectlyNotConvertedNodes;
    }

    public List<Node> getPunctuationNodes() {
        return punctuationNodes;
    }

    public boolean isTreeFullyConverted() {
        boolean noBlockers = getBlockerNodes().size() == 0;
        boolean noIndirectlyNotConverted = getIndirectlyNotConvertedNodes().size() == 0;
        return noBlockers && noIndirectlyNotConverted;
    }

    public void setRulesUsed(List<Rule> rulesUsed) {
        this.rulesUsed = rulesUsed;
    }

    public List<Rule> getRulesUsed() {
        return rulesUsed;
    }
}
