package nats.truducer.data;

import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;
import nats.truducer.io.ruleparsing.TransducerParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nats.truducer.TestUtils.stringToParser;
import static nats.truducer.TestUtils.stringToTree;

/**
 * Created by felix on 25/01/17.
 */
public class MatcherTest {

    // # sent_id = 2
    // # text = I have no clue.
    // 1   I       I       PRON    PRP   Case=Nom|Number=Sing|Person=1     2   nsubj   _   _
    // 2   have    have    VERB    VBP   Number=Sing|Person=1|Tense=Pres   0   root    _   _
    // 3   no      no      DET     DT    PronType=Neg                      4   det     _   _
    // 4   clue    clue    NOUN    NN    Number=Sing                       2   obj     _   SpaceAfter=No
    // 5   .       .       PUNCT   .     _                                 2   punct   _   _

    static final String s1 =
                    "# sent_id = 1\n" +
                    "1\tI\tI\tPRON\tPRP\tCase=Nom|Number=Sing|Person=1\t2\tnsubj\t_\t_\n" +
                    "2\thave\thave\tVERB\tVBP\tNumber=Sing|Person=1|Tense=Pres\t0\troot\t_\t_\n" +
                    "3\tno\tno\tDET\tDT\tPronType=Neg\t4\tdet\t_\t_\n" +
                    "4\tclue\tclue\tNOUN\tNN\tNumber=Sing\t2\tobj\t_\tSpaceAfter=No\n" +
                    "5\t.\t.\tPUNCT\t.\t_\t2\tpunct\t_\t_\n";
    static final Root sentence1 = stringToTree(s1);


    private DepTreeFrontierNode treeToFrontierNode(Root r) {
        return new DepTreeFrontierNode(r.getNode().getChildren().get(0));
    }

    @Test
    public void testMatcher1() throws IOException {
        TransducerParser parser = stringToParser("{n2()}"); // TODO needs to be frontiernode

        Binding b = Matcher.getBinding(parser.matchTree(new HashMap<>()).tree.frontierNode, treeToFrontierNode(sentence1));

        Assert.assertNotNull(b);
        Assert.assertTrue(b.singles.containsKey("n2"));
        Assert.assertEquals(sentence1.getNode().getChildren().get(0), b.singles.get("n2"));
    }

    @Test
    public void testMatcher2() {
        TransducerParser parser = stringToParser("{verb(subj:nsubj())}");

        Binding b = Matcher.getBinding(parser.matchTree(new HashMap<>()).tree.frontierNode, treeToFrontierNode(sentence1));

        Assert.assertNotNull("No binding created", b);
        Assert.assertEquals("Root didn't match", sentence1.getNode().getChildren().get(0), b.singles.get("verb"));
        Assert.assertTrue(b.singles.get("subj").getDeprel().equals("nsubj"));
    }

    @Test
    public void testMatcherCatchall() {
        TransducerParser parser = stringToParser("{n1(n2:nsubj(), ?rest)}");

        Binding b = Matcher.getBinding(parser.matchTree(new HashMap<>()).tree.frontierNode, treeToFrontierNode(sentence1));

        Assert.assertNotNull(b);
        Assert.assertTrue(b.catchalls.containsKey("rest"));
        Assert.assertEquals(2, b.catchalls.get("rest").size());
    }

    @Test
    public void testParent() {
        // actually more of a test for the Parser, but relies on the binding
        TransducerParser parser = stringToParser("{n1(n2:nsubj(), ?rest)}");

        Binding b = Matcher.getBinding(parser.matchTree(new HashMap<>()).tree.frontierNode, treeToFrontierNode(sentence1));

        Assert.assertNotNull(b);
        Assert.assertTrue(b.singles.get("n2").getParent().get() == b.singles.get("n1"));
        List<Node> rest = b.catchalls.get("rest");
        for (Node n : rest) {
            Assert.assertTrue(n.getParent().get() == b.singles.get("n1"));
        }
    }

    @Test
    public void testUpwardsMatching() {
        Node root = sentence1.getNode().getChildren().get(0);
        Node nsubj = root.getChildren().stream().filter(n -> n.getOrd() == 1).findFirst().get();
        TransducerParser parser = stringToParser("parent({n:nsubj()})");

        Binding b = Matcher.getBinding(parser.matchTree(new HashMap<>()).tree.frontierNode, new DepTreeFrontierNode(nsubj));

        Assert.assertTrue(b.singles.get("parent") == root);
        Assert.assertTrue(b.singles.get("n") == nsubj);
    }

    @Test
    public void testListExpansion() {
        Node root = sentence1.getNode().getChildren().get(0);
        Node nsubj = root.getChildren().stream().filter(n -> n.getOrd() == 1).findFirst().get();

        List<String> subjList = new ArrayList<>();
        subjList.add("nsubj");
        subjList.add("csubj");

        Map<String, List<String>> expansions = new HashMap<>();
        expansions.put("SUBJ", subjList);

        TransducerParser parser = stringToParser("parent({n:SUBJ()})");

        Binding b = Matcher.getBinding(parser.matchTree(expansions).tree.frontierNode, new DepTreeFrontierNode(nsubj));

        Assert.assertTrue(b.singles.get("parent") == root);
        Assert.assertTrue(b.singles.get("n") == nsubj);
    }

}
