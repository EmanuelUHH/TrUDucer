package nats.truducer.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A Frontier Node in the Match Tree, that means in a rule.
 * Not a Frontier Node in the actual Dependency Tree.
 */
public class FrontierNode implements StructNode {

    private List<MatchingNode> children = new ArrayList<>();
    private String catchallVar = null;
    private MatchingNode parent = null;

    @Override
    public void setName(String name) {
        throw new IllegalStateException("Frontier Nodes don't have names");
    }

    @Override
    public void setDepRel(String depRel) {
        throw new IllegalStateException("Frontier Nodes don't have deprels");

    }

    @Override
    public void setPosTag(String posTag) {
        throw new IllegalStateException("Frontier Nodes don't have posTags");

    }

    @Override
    public void addChild(StructNode child) {
        if (child instanceof MatchingNode)
            this.children.add((MatchingNode) child);
        else
            throw new IllegalStateException("FrontierNodes can only contain normal nodes");
    }

    public List<MatchingNode> getChildrenAsMn() {
        return this.children;
    }

    @Override
    public List<StructNode> getChildren() {
        List<StructNode> returnVal = new ArrayList<>();
        returnVal.addAll(this.children);
        return returnVal;
    }

    @Override
    public void setCatchallVar(String var) {
        if (this.catchallVar == null)
            this.catchallVar = var;
        else
            throw new IllegalStateException("This node already has a catchall var");
    }

    @Override
    public String getCatchallVar() {
        return this.catchallVar;
    }

    @Override
    public void setParent(StructNode parent) {
        if (parent instanceof MatchingNode)
            this.parent = (MatchingNode) parent;
        else
            throw new IllegalStateException("FrontierNodes can only be contained by normal nodes");

    }

    public MatchingNode getParent() {
        return this.parent;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (this.catchallVar != null) {
            sb.append(this.catchallVar);
            if (this.getChildren().size() > 0)
                sb.append(", ");
        }
        if (this.getChildren().size() > 0) {
            sb.append(this.getChildren().get(0).toString());
        }
        for (int i = 1; i < this.getChildren().size(); i++) {
            sb.append(", " + this.getChildren().get(i).toString());
        }
        sb.append("}");
        return sb.toString();
    }
}
