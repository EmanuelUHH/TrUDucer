package nats.drt.startup;

import cz.ufal.udapi.core.Bundle;
import cz.ufal.udapi.core.Document;
import cz.ufal.udapi.core.Root;
import cz.ufal.udapi.core.impl.DefaultBundle;
import cz.ufal.udapi.core.io.impl.CoNLLUReader;
import cz.ufal.udapi.core.io.impl.CoNLLUWriter;
import nats.drt.data.Transducer;
import nats.drt.deprel.CoverageChecker;
import nats.drt.deprel.TreeComparator;
import nats.drt.gui.MainWindow;
import nats.drt.io.ruleparsing.TransducerLexer;
import nats.drt.io.ruleparsing.TransducerParser;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.*;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import cz.ufal.udapi.core.impl.DefaultDocument;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Created by felix on 12/01/17.
 */
public class Main {

    private static final Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("DRT").defaultHelp(true)
                .description("Dependency Tree Transducer");

        Subparsers subparsers = parser.addSubparsers().dest("subparser_name");

        Subparser conv = subparsers.addParser("conv");
        conv.addArgument("transducer");
        conv.addArgument("input_conll");
        conv.addArgument("output_path");

        Subparser convall = subparsers.addParser("convall");
        convall.addArgument("transducer");
        convall.addArgument("source");
        convall.addArgument("output");

        Subparser test = subparsers.addParser("test");
        test.addArgument("expected");
        test.addArgument("generated");

        Subparser coverage = subparsers.addParser("coverage");
        coverage.addArgument("generated");

        Subparser showTree = subparsers.addParser("show");
        showTree.addArgument("conll_tree");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);

            switch(ns.getString("subparser_name")) {
                case "conv":
                    convertMain(ns);
                    break;
                case "convall":
                    convertDirMain(ns);
                    break;
                case "test":
                    testMain(ns);
                    break;
                case "coverage":
                    checkCoverageMain(ns);
                    break;
                case "show":
                    showTreeMain(ns);
            }
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
    }

    private static void convertMain(Namespace ns) throws Exception {
        logger.info("Starting conversion");
        String transducerPath = ns.getString("transducer");
        String inPath = ns.getString("input_conll");
        String outPath = ns.getString("output_path");

        Transducer t = pathToTransducer(transducerPath);

        Root root = pathToTree(inPath);
        Root newRoot = t.applyTo(root);

        Document docOut = new DefaultDocument();
        docOut.createBundle().addTree(newRoot);

        new CoNLLUWriter().writeDocument(docOut, Paths.get(outPath));
    }

    private static void convertDirMain(Namespace ns) throws Exception {
        String transducerPath = ns.getString("transducer");
        String inDir = ns.getString("source");
        String outDir = ns.getString("output");
        if (!new File(outDir).exists())
            new File(outDir).mkdirs();

        Transducer t = pathToTransducer(transducerPath);

        File[] files = new File(inDir).listFiles();
        Arrays.sort(files);

        for (File file : files) {
            logger.info(String.format("Testing file %s", file));
            Root orig = fileToTree(file);
            Root transduced = t.applyTo(orig);
            Document outDoc = new DefaultDocument();
            outDoc.createBundle().addTree(transduced);

            new CoNLLUWriter().writeDocument(outDoc, new File(outDir, file.getName()).toPath());
        }
    }

    private static void testMain(Namespace ns) throws Exception {
        logger.info("Testing");
        String compDir = ns.getString("expected");
        String genDir = ns.getString("generated");

        int punctuationNodes = 0;
        int ignoredNodes = 0;
        int notConvertedNodes = 0;
        int incorrectNodes = 0;
        int correctNodes = 0;
        int completeMatch = 0;

        File[] files = new File(genDir).listFiles();
        Arrays.sort(files);

        for (File file : files) {
            logger.info(String.format("Testing file %s", file.getName()));
            logger.debug("reading generated");
            Root generated = fileToTree(file);
            logger.debug("reading expected");
            Root expected = fileToTree(new File(compDir, file.getName()));
            TreeComparator tc = new TreeComparator(expected, generated);
            tc.compare();
            if (tc.matches()) {
                completeMatch += 1;
                logger.info(String.format("%s equal", file.getName()));
            } else {
                logger.info(String.format("%s incorrect: %s, unconverted: %s", file.getName(), intlistToString(tc.getDidntMatch()), intlistToString(tc.getNotConverted())));
            }
            punctuationNodes += tc.getPunctuation().size();
            ignoredNodes += tc.getIgnored().size();
            notConvertedNodes += tc.getNotConverted().size();
            incorrectNodes += tc.getDidntMatch().size();
            correctNodes += tc.getDidMatch().size();
        }

        logger.info(String.format("%d nodes punctuation.", punctuationNodes));
        logger.info(String.format("%d nodes ignored (not annotated in 'expected').", ignoredNodes));
        logger.info(String.format("%d nodes not converted by transducer.", notConvertedNodes));
        logger.info(String.format("%d nodes not converted correctly.", incorrectNodes));
        logger.info(String.format("%d nodes converted correctly.", correctNodes));
        int total = punctuationNodes + ignoredNodes + notConvertedNodes + incorrectNodes + correctNodes;
        logger.info(String.format("%d nodes total", total));
        logger.info(String.format("%d/%d sentences correct", completeMatch, files.length));
    }

    private static void checkCoverageMain(Namespace ns) {
        String genDir = ns.getString("generated");

        int correctNodes = 0;
        int puncuationNodes = 0;
        int incorrectNodes = 0;

        File[] files = new File(genDir).listFiles();
        Arrays.sort(files);

        for (File file : files) {
            Root r = fileToTree(file);
            CoverageChecker cc = new CoverageChecker(r);
            cc.check();
            correctNodes += cc.getConverted().size();
            puncuationNodes += cc.getPunctuation().size();
            incorrectNodes += cc.getNotConverted().size();
        }

        logger.info(String.format("%d nodes converted correctly.", correctNodes));
        logger.info(String.format("%d nodes punctuation.", puncuationNodes));
        logger.info(String.format("%d nodes not converted.", incorrectNodes));
    }

    private static void showTreeMain(Namespace ns) {
        String conllFilePath = ns.getString("conll_tree");

        Root tree = fileToTree(new File(conllFilePath));

        MainWindow win = new MainWindow();
        SwingUtilities.invokeLater(() -> win.createAndShow(tree));
    }

    private static Transducer pathToTransducer(String path) throws IOException {
        ANTLRFileStream input = new ANTLRFileStream(path);
        TransducerLexer lexer = new TransducerLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TransducerParser parser = new TransducerParser(tokens);

        TransducerParser.TransducerContext tree = parser.transducer();
        return tree.t;
    }

    private static Root fileToTree(File file) {
        Document doc = new CoNLLUReader(file).readDocument();
        logger.debug(String.format("Bundles: %d", doc.getBundles().size()));
        logger.debug(String.format("Trees: %d", doc.getBundles().get(0).getTrees().size()));
        return doc.getBundles().get(0).getTrees().get(0);
    }

    private static Root pathToTree(String path) {
        return fileToTree(new File(path));
    }

    private static String intlistToString(List<Integer> intlist) {
        if (intlist.size() == 0) {
            return "[]";
        } else if (intlist.size() == 1) {
            return String.format("[%d]", intlist.get(0));
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(intlist.get(0));
            for (Integer i : intlist.subList(1, intlist.size())) {
                sb.append(", ");
                sb.append(i);
            }
            sb.append("]");
            return sb.toString();
        }
    }

}
