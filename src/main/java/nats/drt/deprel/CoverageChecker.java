package nats.drt.deprel;

import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by felix on 23/02/17.
 */
public class CoverageChecker {

    private final Root root;
    private final List<Integer> converted = new ArrayList<>();
    private final List<Integer> punctuation = new ArrayList<>();
    private final List<Integer> notConverted = new ArrayList<>();


    public CoverageChecker(Root r) {
        this.root = r;
    }


    public void check() {
        List<Node> nodes = root.getDescendants();

        for (Node n : nodes) {
            if (n.getDeprel().equals("ROOT")) {
                punctuation.add(n.getOrd());
            } else if (n.getDeprel().toUpperCase().equals(n.getDeprel())) {
                notConverted.add(n.getOrd());
            } else {
                converted.add(n.getOrd());
            }
        }
    }

    public List<Integer> getConverted() {
        return converted;
    }

    public List<Integer> getNotConverted() {
        return notConverted;
    }

    public List<Integer> getPunctuation() {
        return punctuation;
    }
}
