package nats.truducer.deprel;

import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;

/**
 * Created by felix on 19/03/18.
 */
public class TreeUtils {

    public static Node getById(Root tree, int id) {
        return tree.getDescendants().stream().filter(n -> n.getOrd() == id).findFirst().get();
    }
}
