package nats.truducer.startup;

import cz.ufal.udapi.core.Document;
import cz.ufal.udapi.core.Root;
import cz.ufal.udapi.core.io.impl.CoNLLUReader;
import cz.ufal.udapi.core.io.impl.CoNLLUWriter;
import nats.truducer.data.Transducer;
import nats.truducer.deprel.CoverageChecker;
import nats.truducer.deprel.TreeComparator;
import nats.truducer.gui.MainWindow;
import nats.truducer.gui.MainWindowController;
import nats.truducer.io.ruleparsing.TransducerLexer;
import nats.truducer.io.ruleparsing.TransducerParser;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.type.StringArgumentType;
import net.sourceforge.argparse4j.inf.*;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import cz.ufal.udapi.core.impl.DefaultDocument;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Created by felix on 12/01/17.
 */
public class Main {

    private static final Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("TrUDucer").defaultHelp(true)
                .description("TrUDucer - Transforming to Universal Dependencies with transducers.\n" +
                        "\n" +
                        "Tree transducer based dependency tree annotation schema conversion.\n" +
                        "A single CoNLL file or a whole directory can be converted based on given rule file; " +
                        "can compare directories, useful for testing the correctness of a rulefile with previously annotated trees." +
                        "Test the coverage of a rulefile with 'coverage' or look at a conversion process in detail with 'show'.\n\n" +
                        "For the compare and coverage subcommand it is assumed that the source dependency relations are in CAPS, " +
                        "while the UD labels are in all lowercase.  Otherwise no assumptions about the labels are made.");

        Subparsers subparsers = parser.addSubparsers().dest("subparser_name")
                .help("The various subcommands.");

        Subparser conv = subparsers.addParser("conv")
                .help("Convert a single tree given by a CoNLL file.");
        conv.addArgument("rulefile")
                .help("The file containing the transformation rules.");
        conv.addArgument("input_file")
                .help("The CoNLL file with the dependency tree to be converted.");
        conv.addArgument("output_file")
                .help("The filename of the file to be generated.");

        Subparser convall = subparsers.addParser("convall")
                .help("Convert a whole directory of CoNLL files.");
        convall.addArgument("rulefile")
                .help("The file containing the transformation rules.");
        convall.addArgument("input_dir")
                .help("The directory containing the CoNLL files to be converted.");
        convall.addArgument("output_dir")
                .help("The directory where the converted files should be generated.");

        Subparser test = subparsers.addParser("compare")
                .help("Compare two directories of CoNLL files.");
        test.addArgument("expected_dir")
                .help("The directory containing the CoNNL files with the expected tree structures.");
        test.addArgument("actual_dir")
                .help("The directory containing the CoNLL files with the actual tree structures.");

        Subparser coverage = subparsers.addParser("coverage")
                .help("Check how many dependency relations are converted; in a single directory.");
        coverage.addArgument("dir")
                .help("The directory containing CoNLL files to be checked for completeness of conversion.");
        coverage.addArgument("original_dir")
                .help("The directory containing the original conll files.");

        Subparser showTree = subparsers.addParser("show")
                .help("Show the conversion process of a single tree step by step in a GUI.");
        showTree.addArgument("input_file")
                .help("The CoNNL file containing the tree to be converted.");
        showTree.addArgument("transducer_file")
                .nargs("?")
                .help("The rule file.");

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
                case "compare":
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
        String transducerPath = ns.getString("rulefile");
        String inPath = ns.getString("input_file");
        String outPath = ns.getString("output_file");

        Transducer t = pathToTransducer(transducerPath);

        Root root = pathToTree(inPath);
        Root newRoot = t.applyTo(root);

        Document docOut = new DefaultDocument();
        docOut.createBundle().addTree(newRoot);

        new CoNLLUWriter().writeDocument(docOut, Paths.get(outPath));
    }

    private static void convertDirMain(Namespace ns) throws Exception {
        String transducerPath = ns.getString("rulefile");
        String inDir = ns.getString("input_dir");
        String outDir = ns.getString("output_dir");
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
        String compDir = ns.getString("expected_dir");
        String genDir = ns.getString("actual_dir");

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
        String genDir = ns.getString("dir");
        String origDir = ns.getString("original_dir");


        File[] files = new File(genDir).listFiles();
        Arrays.sort(files);

        CoverageChecker cc = new CoverageChecker();

        for (File file : files) {
            Root r = fileToTree(file);
            Root orig = fileToTree(new File(origDir + "/" + file.getName()));
            cc.check(orig, r);
        }

        int correctNodes = cc.getConverted().size();
        int puncuationNodes = cc.getPunctuation().size();
        int blockers = cc.getBlockers().size();
        int indirectly = cc.getIndirectlyAffected().size();

        logger.info(String.format("%d nodes converted correctly.", correctNodes));
        logger.info(String.format("%d nodes punctuation.", puncuationNodes));
        logger.info(String.format("%d nodes blockers.", blockers));
        logger.info(String.format("%d nodes not converted follow up.", indirectly));

        // Perform relevant calculations.
        int totalWithoutPunct = correctNodes + blockers + indirectly;
        double correctPercentage = (double)correctNodes / (double)totalWithoutPunct;
        double blockersPercentage = (double)blockers / (double)totalWithoutPunct;
        double indirectPercentage = (double)indirectly / (double)totalWithoutPunct;

        logger.info(String.format("%d total without punctuation", totalWithoutPunct));
        logger.info(String.format("%.5f correct", correctPercentage));
        logger.info(String.format("%.5f blockers", blockersPercentage));
        logger.info(String.format("%.5f indirect", indirectPercentage));

        logger.info("\n" + cc.getTableAsString());
        logger.info("\n" + cc.getBlockerStatsAsString());
    }

    private static void showTreeMain(Namespace ns) throws IOException {
        String conllFilePath = ns.getString("input_file");
        String transducerPath = ns.getString("transducer_file");

        Root tree = fileToTree(new File(conllFilePath));
        Transducer transducer = null;

        if (transducerPath != null) {
            transducer = pathToTransducer(transducerPath);
        }

        MainWindowController controller = new MainWindowController();
        controller.initWindow();
        controller.setTree(tree);
        controller.setTransducer(transducer);
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
