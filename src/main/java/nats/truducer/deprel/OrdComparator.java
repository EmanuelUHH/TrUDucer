package nats.truducer.deprel;

import cz.ufal.udapi.core.Node;

import java.util.Comparator;

/**
 * Created by felix on 14/06/17.
 */
public class OrdComparator implements Comparator<Node> {
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
}
