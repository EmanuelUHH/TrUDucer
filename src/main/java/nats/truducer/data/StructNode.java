package nats.truducer.data;

import java.util.List;

/**
 * Created by felix on 07/02/17.
 */
public interface StructNode {

    void setName(String name);
    void setDepRel(String depRel);
    void setPosTag(String posTag);
    void addChild(StructNode child);
    List<StructNode> getChildren();
    void setCatchallVar(String var);
    String getCatchallVar();
    void setParent(StructNode parent);
}
