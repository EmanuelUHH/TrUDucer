package nats.truducer.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by felix on 07/02/17.
 */
public class ReplacementNode {

    private String name;
    private String depRel;
    private List<ReplacementNode> children = new ArrayList<>();
    private List<String> catchAllVars = new ArrayList<>();
    private ReplacementNode parent;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setDepRel(String depRel) {
        this.depRel = depRel;
    }

    public String getDepRel() {
        return this.depRel;
    }

    public void addChild(ReplacementNode child) {
        this.children.add(child);
    }

    public List<ReplacementNode> getChildren() {
        return this.children;
    }

    public void addCatchAllVar(String var) {
        this.catchAllVars.add(var);
    }

    public List<String> getCatchAllVars() {
        return this.catchAllVars;
    }

    public void setParent(ReplacementNode parent) {
        this.parent = parent;
    }

    public ReplacementNode getParent() {
        return this.parent;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        if (this.getDepRel() != null)
            sb.append(":" + this.getDepRel());
        sb.append("(");
        if (this.catchAllVars.size() > 0) {
            sb.append(this.catchAllVars.get(0));
            for (int i = 1; i < this.catchAllVars.size(); i++)
                sb.append(", " + this.catchAllVars.get(i));
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
