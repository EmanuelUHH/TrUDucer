package nats.truducer.io;

import cz.ufal.udapi.core.Document;
import cz.ufal.udapi.core.Root;
import cz.ufal.udapi.core.impl.DefaultDocument;
import cz.ufal.udapi.core.io.impl.CoNLLUReader;
import cz.ufal.udapi.core.io.impl.CoNLLUWriter;
import nats.truducer.data.Transducer;
import nats.truducer.io.ruleparsing.TransducerLexer;
import nats.truducer.io.ruleparsing.TransducerParser;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TreeReaderWriter {

    public static Transducer pathToTransducer(String path) throws IOException {
        ANTLRFileStream input = new ANTLRFileStream(path);
        TransducerLexer lexer = new TransducerLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TransducerParser parser = new TransducerParser(tokens);

        TransducerParser.TransducerContext tree = parser.transducer();
        return tree.t;
    }

    public static Root fileToTree(File file) {
        Document doc = new CoNLLUReader(file).readDocument();
        // logger.debug(String.format("Bundles: %d", doc.getBundles().size()));
        // logger.debug(String.format("Trees: %d", doc.getBundles().get(0).getTrees().size()));
        return doc.getBundles().get(0).getTrees().get(0);
    }

    public static void treeToFile(File file, Root tree) {
        Document outDoc = new DefaultDocument();
        outDoc.createBundle().addTree(tree);

        writeDoc(outDoc, file);
    }

    /**
     * The CoNNLUWriter seems to not care about ordering of the words/nodes.
     * The CoNNLUReader very much does so!
     * As long as the reader is not fixed, we need to be careful about ordering
     * our output. This should help...
     *
     * @param outDoc
     * @param outFile
     */
    private static void writeDoc(Document outDoc, File outFile) {
        new CoNLLUWriter().writeDocument(outDoc, outFile.toPath());

        try {
            BufferedReader r = new BufferedReader(new FileReader(outFile));
            String s;
            List<String> strings = new ArrayList<>();
            while ((s = r.readLine()) != null) {
                strings.add(s);
            }
            r.close();
            strings.sort((a, b) -> {
                if(a.startsWith("#") || b.length() < 1)
                    return Integer.compare(1, 0);
                if(b.startsWith("#") || a.length() < 1)
                    return Integer.compare(0, 1);
                int first = Integer.parseInt(a.split("\t")[0]);
                int second = Integer.parseInt(b.split("\t")[0]);
                return Integer.compare(first, second);
            });

            FileWriter w = new FileWriter(outFile);

            // weiss nicht, ob das die schoenste Implementation in Java ist :)
            w.write(strings.stream().reduce("", (a, b) -> a + "\n" + b));
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
