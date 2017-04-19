package nats.drt.gui;

import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;
import io.gitlab.nats.deptreeviz.ParseInterface;
import io.gitlab.nats.deptreeviz.SimpleParse;
import io.gitlab.nats.deptreeviz.SimpleWord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by felix on 22/02/17.
 */
public class UdapiToDeptreeviz {

    public static SimpleParse getParse(Root tree) {
        List<Integer> heads = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<SimpleWord> words = new ArrayList<>();
        List<Node> nodes = tree.getDescendants();
        Collections.sort(nodes, (n1, n2) -> Integer.compare(n1.getOrd(), n2.getOrd()));
        for (Node n : nodes) {
            heads.add(n.getParent().get().getOrd() - 1);
            labels.add(n.getDeprel());
            words.add(new SimpleWord(n.getForm(), n.getXpos()));
        }
        return new SimpleParse(words, heads, labels);
    }
}
