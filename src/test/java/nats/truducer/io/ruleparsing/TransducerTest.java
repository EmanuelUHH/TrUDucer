package nats.truducer.io.ruleparsing;

import nats.truducer.data.MatchingNode;
import nats.truducer.data.Tree;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

import static nats.truducer.TestUtils.stringToParser;

/**
 * Created by felix on 19/01/17.
 */
public class TransducerTest {

    @Test
    public void testNodeParsing() {
        TransducerParser parser = stringToParser("n1.POS:DEP()");

        TransducerParser.MatchTreeContext tree = parser.matchTree(new HashMap<>());
        MatchingNode mn = (MatchingNode) tree.tree.root;
        Assert.assertEquals(mn.getName(), "n1");
        Assert.assertEquals(mn.getPosTag(), "POS");
        Assert.assertEquals(mn.getDepRel(), "DEP");
    }

	@Test
    public void testExtendedLabels() {
        TransducerParser parser = stringToParser("n1.POS:DEP:EXTENSION()");

        TransducerParser.MatchTreeContext tree = parser.matchTree(new HashMap<>());
        MatchingNode mn = (MatchingNode) tree.tree.root;
        Assert.assertEquals("n1", mn.getName());
        Assert.assertEquals("POS", mn.getPosTag());
        Assert.assertEquals("DEP:EXTENSION",mn.getDepRel());
    }

    @Test
    public void testTreeParsing() {
        TransducerParser parser = stringToParser("n1(n2(), n3())");

        TransducerParser.MatchTreeContext tree = parser.matchTree(new HashMap<>());
        Tree t = tree.tree;
        Assert.assertEquals(2, t.root.getChildren().size());
    }

    @Test
    public void testCatchallVar() {
        TransducerParser parser = stringToParser("n1(n2(), n3(), ?rest)");

        Assert.assertEquals("rest", parser.matchTree(new HashMap<>()).tree.root.getCatchallVar());
    }
}
