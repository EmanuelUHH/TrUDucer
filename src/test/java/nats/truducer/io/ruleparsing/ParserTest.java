package nats.truducer.io.ruleparsing;

import nats.truducer.data.Tree;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

import static nats.truducer.TestUtils.stringToParser;

/**
 * Created by felix on 06/02/17.
 */
public class ParserTest {

    @Test
    public void testRootNodeFrontier() {
        TransducerParser parser = stringToParser("n1(n2(), n3());");
        Tree t = parser.matchTree(new HashMap<>()).tree;

        Assert.assertTrue(t.frontierNode == null);
    }

    @Test
    public void testRootNodeFrontier2() {
        TransducerParser parser = stringToParser("n1(n2(), n3()) -> n1(n2(), n3());");
        Tree t = parser.convrule(new HashMap<>()).mt.tree;

        // there is an extra generated root and frontier node
        Assert.assertTrue(t.root.getChildren().get(0) == t.frontierNode);
    }

    @Test
    public void testFrontierNode() {
        TransducerParser parser = stringToParser("parent({n2(n3(), n4())});");
        Tree t = parser.matchTree(new HashMap<>()).tree;

        Assert.assertTrue(t.root.getChildren().get(0) == t.frontierNode);
    }

    @Test
    public void testFrontierNode2() {
        TransducerParser parser = stringToParser("parent:$x({n:AUX(?auxr)}, ?r);");
        Tree t = parser.matchTree(new HashMap<>()).tree;

        Assert.assertTrue(t.root.getChildren().get(0) == t.frontierNode);
    }
}
