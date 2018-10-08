package nats.truducer.io;

import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;
import nats.truducer.data.*;
import nats.truducer.io.ruleparsing.TransducerLexer;
import nats.truducer.io.ruleparsing.TransducerParser;
import nats.truducer.startup.Main;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static nats.truducer.io.TreeReaderWriter.fileToTree;

public class CoNLLMatcher {
    public static boolean checkForMatching(String filterText, File srcFile) {
        Root root = fileToTree(srcFile);

        // parse search expression
        ANTLRInputStream input = new ANTLRInputStream(filterText);
        TransducerLexer lexer = new TransducerLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TransducerParser parser = new TransducerParser(tokens);
        TransducerParser.MatchTreeContext context =  parser.matchTree(new HashMap<>());
        Tree queryTree = context.tree;
        //infer missing structure by creating a dummy rule ... really dirty hack, sorry!
        Rule dummyRule = new Rule(queryTree, Main.generateDummyReplacementNode(queryTree), null, "");

        for (DepTreeFrontierNode frontierNode : treeToFrontierList(new ArrayList<>(), root.getNode())) {
            Binding match = Matcher.getBinding(queryTree.frontierNode, frontierNode);
            if (match != null) {
                return true;
            }
        }
        return false;
    }

    private static List<DepTreeFrontierNode> treeToFrontierList(List<DepTreeFrontierNode> result, Node tree) {
        List<Node> children = tree.getChildren();

        if(!children.isEmpty()) {
            DepTreeFrontierNode node = new DepTreeFrontierNode(children);
            result.add(node);
            for(Node child: children) {
                treeToFrontierList(result, child);
            }
        }

        return result;
    }
}
