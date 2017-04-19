package nats.drt.data;

import cz.ufal.udapi.core.Node;

import javax.management.NotificationEmitter;
import java.util.ArrayList;
import java.util.List;

/**
 * A Frontier Node in the actual dependency tree.
 * the node is only a pseudo node, as it is not actually inserted into the tree.
 */
public class DepTreeFrontierNode {

    private final List<Node> children = new ArrayList<>();
    private final Node parent;

    public DepTreeFrontierNode(Node initialChild) {
        parent = initialChild.getParent().get();
        children.add(initialChild);
    }

    public DepTreeFrontierNode(List<Node> initialChildren) {
        this(initialChildren.get(0));
        for (int i = 1; i < initialChildren.size(); i++)
            addChild(initialChildren.get(i));
    }

    public void addChild(Node child) {
        assert child.getParent().get().equals(parent);
        children.add(child);
    }

    public List<Node> getChildren() {
        return this.children;
    }

    public Node getParent() {
        return parent;
    }

    private Node getClone(Node original, List<Node> clonedNodes) {
        for (Node clone : clonedNodes) {
            if (clone.getOrd() == original.getOrd())
                return clone;
        }
        throw new IllegalArgumentException("The clone is not in the given list.");
    }

    public DepTreeFrontierNode deepCopy(List<Node> newNodes) {
        List<Node> newChildren = new ArrayList<>();
        for (Node child : children) {
            newChildren.add(getClone(child, newNodes));
        }
        return new DepTreeFrontierNode(newChildren);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(parent);
        sb.append("(");
        for (Node child : children) {
            sb.append(child);
            sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}
