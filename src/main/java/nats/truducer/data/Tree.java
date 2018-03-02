package nats.truducer.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A tree as used on the left-hand side of a rule.
 * It is used for matching.  The root does not need to be a frontier node
 * which is why those things are stored seperately.
 */
public class Tree {

    public StructNode root;
    public FrontierNode frontierNode;

    public Tree(StructNode root) {
        this.root = root;
    }

    public List<String> getUsedNames() {
        List<String> names = new ArrayList<>();
        collectUsedNames(root, names);
        return names;
    }

    private void collectUsedNames(StructNode node, List<String> names) {
        names.add(node.getCatchallVar());
        if (node instanceof MatchingNode)
            names.add(((MatchingNode)node).getName());
        for (StructNode sn : node.getChildren())
            collectUsedNames(sn, names);
    }

    @Override
    public String toString() {
        return root.toString();
    }
}
