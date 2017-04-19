package nats.drt;

import cz.ufal.udapi.core.Document;
import cz.ufal.udapi.core.Root;
import cz.ufal.udapi.core.io.impl.CoNLLUReader;
import nats.drt.io.ruleparsing.TransducerLexer;
import nats.drt.io.ruleparsing.TransducerParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.StringReader;

/**
 * Created by felix on 25/01/17.
 */
public class TestUtils {

    public static TransducerParser stringToParser(String s) {
        ANTLRInputStream input = new ANTLRInputStream(s);
        TransducerLexer lexer = new TransducerLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TransducerParser parser = new TransducerParser(tokens);
        return parser;
    }

    public static Root stringToTree(String s) {
        Document d = new CoNLLUReader(new StringReader(s)).readDocument();
        return d.getBundles().get(0).getTrees().get(0);
    }
}
