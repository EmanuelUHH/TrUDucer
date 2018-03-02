package nats.truducer.data;

import cz.ufal.udapi.core.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A binding from variables in rule trees to actual nodes in the tree that the rule is matched upon.
 */
public class Binding {
    public Map<String, Node> singles = new HashMap<>();
    public Map<String, List<Node>> catchalls = new HashMap<>();
    // the mapping of deprels is a special mechanism to not only match nodes but also dependency relations.
    // A rule can contain a '$depRel' to match a dependency relation and this '$depRel' can then be used on
    // the right-hand side of the rule as well.
    public Map<String, String> depRels = new HashMap<>();
    public List<Node> nodesAboveFrontier = new ArrayList<>();

    public Binding merge(Binding other) {
        Binding result = new Binding();
        for (String k : this.singles.keySet()) {
            result.singles.put(k, this.singles.get(k));
        }
        for (String k : this.catchalls.keySet()) {
            result.catchalls.put(k, this.catchalls.get(k));
        }
        for (String k : this.depRels.keySet()) {
            result.depRels.put(k, this.depRels.get(k));
        }
        for (String k : other.singles.keySet()) {
            result.singles.put(k, other.singles.get(k));
        }
        for (String k : other.catchalls.keySet()) {
            result.catchalls.put(k, other.catchalls.get(k));
        }
        for (String k : other.depRels.keySet()) {
            result.depRels.put(k, other.depRels.get(k));
        }
        result.nodesAboveFrontier.addAll(this.nodesAboveFrontier);
        result.nodesAboveFrontier.addAll(other.nodesAboveFrontier);
        return result;
    }

    public Binding putSingle(String key, Node val) {
        Binding tmp = new Binding();
        tmp.singles.put(key, val);
        return this.merge(tmp);
    }

    public Binding putCatchall(String key, List<Node> val) {
        Binding tmp = new Binding();
        tmp.catchalls.put(key, val);
        return this.merge(tmp);
    }

    public Binding putDeprel(String key, String val) {
        Binding tmp = new Binding();
        tmp.depRels.put(key, val);
        return this.merge(tmp);
    }

    public List<Node> getBoundNodes() {
        List<Node> boundNodes = new ArrayList<>();
        boundNodes.addAll(singles.values());
        for (List<Node> ns : catchalls.values()) {
            boundNodes.addAll(ns);
        }
        return boundNodes;
    }
}
