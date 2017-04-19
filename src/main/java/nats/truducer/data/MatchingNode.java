package nats.truducer.data;

import cz.ufal.udapi.core.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by felix on 18/01/17.
 */
public class MatchingNode implements StructNode{

    private Map<String, List<String>> expansions;
    private String posTag;
    private String depRel;
    private final List<StructNode> children = new ArrayList<>();
    private String name;
    private String catchAllVar;
    private StructNode parent;


    public MatchingNode(String name, String posTag, String depRel, Map<String, List<String>> expansions)
    {
        this.name = name;
        this.posTag = posTag;
        this.depRel = depRel;
        this.expansions = expansions;
    }


    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setDepRel(String depRel) {
        this.depRel = depRel;
    }

    @Override
    public void setPosTag(String posTag) {
        this.posTag = posTag;
    }

    @Override
    public void setParent(StructNode parent) {
        this.parent = parent;
    }

    @Override
    public void addChild(StructNode child) {
        this.children.add(child);
    }

    @Override
    public void setCatchallVar(String var) {
        if (this.catchAllVar == null)
            this.catchAllVar = var;
        else
            throw new IllegalStateException("This node already has a catchall var");
    }

    public StructNode getParent() {
        return this.parent;
    }

    public String getName() {
        return name;
    }

    public String getPosTag() {
        return posTag;
    }

    public String getDepRel() {
        return depRel;
    }

    @Override
    public String getCatchallVar() {
        return catchAllVar;
    }

    public List<StructNode> getChildren() {
        return this.children;
    }

    /**
     * Checks whether x (a pos tag or a deprel) is either a direct match for expansionVar
     * or an element of the list specified by the expansionVar
     */
    private boolean matchesExpansion(String x, String expansionVar) {
        return x.equals(expansionVar) || // direct match
                // x is an expansion and matches
                (expansions.get(expansionVar) != null && expansions.get(expansionVar).contains(x));
    }

    public Binding matches(Node node, Binding currentBinding) {
        Binding b = currentBinding;
        if (posTag != null) {
            if (!matchesExpansion(node.getXpos(), posTag))
                return null;
        }

        if (depRel != null) {
            if (depRel.startsWith("$")) {
                if (currentBinding.depRels.containsKey(depRel)) {
                    if (currentBinding.depRels.get(depRel).equals(node.getDeprel())) {
                        // DepRel is a var, the var is already bound, but it matches
                        b = b.putSingle(this.getName(), node);
                    } else {
                        // DepRel is a Var and it is already bound and it doesn't match
                        return null;
                    }
                } else {
                    // DepRel is a var, and the var is currently not bound
                    b = b.putDeprel(depRel, node.getDeprel());
                    b = b.putSingle(this.getName(), node);
                }
            } else {
                if (matchesExpansion(node.getDeprel(), depRel)) {
                    // DepRel is not a var, the DepRels match
                    b = b.putSingle(this.getName(), node);
                } else {
                    // DepRel is not a var, and it doesn't match
                    return null;
                }
            }
        } else {
            b = b.putSingle(this.getName(), node);
        }
        return b;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        if (this.getPosTag() != null)
            sb.append("." + this.getPosTag());
        if (this.getDepRel() != null)
            sb.append(":" + this.getDepRel());
        sb.append("(");
        if (this.catchAllVar != null) {
            sb.append(this.catchAllVar);
            if (this.getChildren().size() > 0)
                sb.append(", ");
        }
        if (this.getChildren().size() > 0) {
            sb.append(this.getChildren().get(0).toString());
        }
        for (int i = 1; i < this.getChildren().size(); i++) {
            sb.append(", " + this.getChildren().get(i).toString());
        }
        sb.append(")");
        return sb.toString();
    }
}
