package nats.drt.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by felix on 19/01/17.
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
