package nats.drt.deprel;

import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by felix on 15/02/17.
 */
public class TreeComparator {

    private final Root r1;
    private final Root r2;
    private final List<Integer> punctuation = new ArrayList<>();
    private final List<Integer> notConverted = new ArrayList<>();
    private final List<Integer> didMatch = new ArrayList<>();
    private final List<Integer> didntMatch = new ArrayList<>();
    private final List<Integer> ignored =new ArrayList<>();


    public TreeComparator(Root r1, Root r2) {
        this.r1 = r1;
        this.r2 = r2;
    }


    private static Comparator<Node> ordComparator = new Comparator<Node>() {
        @Override
        public int compare(Node n1, Node n2) {
            if (n1 == null || n2 == null)
                throw new NullPointerException();

            if (n1.getOrd() < n2.getOrd())
                return -1;
            else if (n1.getOrd() == n2.getOrd())
                return 0;
            else
                return 1;
        }
    };

    public void compare() {
        // r1 is expected, r2 is generated
        List<Node> r1nodes = r1.getDescendants();
        Collections.sort(r1nodes, ordComparator);
        List<Node> r2nodes = r2.getDescendants();
        Collections.sort(r2nodes, ordComparator);
        if (r1nodes.size() != r2nodes.size())
            throw new IllegalStateException("Tree size does not match");
        for (int i = 0; i < r1nodes.size(); i++) {
            Node n1 = r1nodes.get(i);
            Node n2 = r2nodes.get(i);
            if (n1.getDeprel().equals("ROOT") && n2.getDeprel().equals("ROOT")) {
                this.punctuation.add(n1.getOrd());
                continue;
            }
            if (n1.getDeprel().toUpperCase().equals(n1.getDeprel())) {
                // add to list "not converted in expected, ignoring"
                this.ignored.add(n1.getOrd());
                continue;
            }
            if (n2.getDeprel().toUpperCase().equals(n2.getDeprel())) {
                // add to list "not converted"
                this.notConverted.add(n1.getOrd());
                continue;
            }
            // only nodes remain that are converted in both trees,
            if (n1.getDeprel().equals(n2.getDeprel()) && n1.getParent().get().getOrd() == n2.getParent().get().getOrd()) {
                this.didMatch.add(n1.getOrd());
                continue;
            }
            this.didntMatch.add(n1.getOrd());
        }
    }

    public List<Integer> getPunctuation() {
        return this.punctuation;
    }

    public  List<Integer> getNotConverted() {
        return this.notConverted;
    }

    public List<Integer> getDidMatch() {
        return this.didMatch;
    }

    public List<Integer> getDidntMatch() {
        return this.didntMatch;
    }

    public List<Integer> getIgnored() {
        return this.ignored;
    }

    public boolean matches() {
        return this.didntMatch.size() == 0 && this.notConverted.size() == 0 && this.ignored.size() == 0;
    }

    private static boolean isUppercase(String s) {
        return s.toUpperCase().equals(s);
    }
}
