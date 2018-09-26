package nats.truducer.startup;

import cz.ufal.udapi.core.Document;
import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;
import cz.ufal.udapi.core.io.impl.CoNLLUReader;
import cz.ufal.udapi.core.io.impl.CoNLLUWriter;
import nats.truducer.data.*;
import nats.truducer.deprel.CoverageChecker;
import nats.truducer.deprel.PrecisionStats;
import nats.truducer.deprel.TreeComparator;
import nats.truducer.deprel.TreeConversionStats;
import nats.truducer.exceptions.BlockedInteractionException;
import nats.truducer.gui.ConvGUIController;
import nats.truducer.gui.MainWindowController;
import nats.truducer.io.ruleparsing.TransducerLexer;
import nats.truducer.io.ruleparsing.TransducerParser;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.action.StoreTrueArgumentAction;
import net.sourceforge.argparse4j.inf.*;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import cz.ufal.udapi.core.impl.DefaultDocument;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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

        // ** added by Maximilian
        // to declare whether the user wants a graphical view of the tree for interactive
        // conversion or the terminal view
        // TODO implement both views and actually choose between them
        convall.addArgument("-g", "--gui")
                .setDefault("false")
                .choices("true", "false")
                .help("use graphical or terminal-based representation of graphs");

        Subparser test = subparsers.addParser("compare")
                .help("Compare two directories of CoNLL files.");
        test.addArgument("expected_dir")
                .help("The directory containing the CoNNL files with the expected tree structures.");
        test.addArgument("actual_dir")
                .help("The directory containing the CoNLL files with the actual tree structures.");
        test.addArgument("original_dir")
                .help("The directory containing the source CoNLL files.");

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

        Subparser listTrees = subparsers.addParser("list")
                .help("List trees and subsets of trees from the treebank.");
        listTrees.addArgument("src_dir")
                .help("The directory containing the original conll files.");
        listTrees.addArgument("gen_dir")
                .help("The directory containing the generated conll files.");
        listTrees.addArgument("-i", "--incomplete").action(new StoreTrueArgumentAction())
                .help("only list trees that are not fully converted");
        listTrees.addArgument("-b", "--blockers").action(new StoreTrueArgumentAction())
                .help("list the blockers in the trees");

        Subparser searchTrees = subparsers.addParser("search")
                .help("Search for trees by giving a substructure of the tree.");
        searchTrees.addArgument("dir")
                .help("Directory containing the tree files to search.");
        searchTrees.addArgument("expr")
                .help("The search expression given in the TrUDucer rule syntax.");


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
                    break;
                case "list":
                    listMain(ns);
                    break;
                case "search":
                    searchMain(ns);
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

        // ** added by Maximilian
        // creates a viewer for interactive Conversions and sets it for all the rules
        ConvGUIController interactiveWindow = new ConvGUIController();
        interactiveWindow.initWindow();

        interactiveWindow.setTransducer(t);
        t.rules.forEach(r -> r.setInteractiveConversion(interactiveWindow));

        Root root = pathToTree(inPath);
        Root newRoot = t.applyTo(root);

        interactiveWindow.close();

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

        // ** added by Maximilian
        // same as in convertMain!
        ConvGUIController interactiveWindow = new ConvGUIController();
        interactiveWindow.initWindow();

        interactiveWindow.setTransducer(t);
        t.rules.forEach(r -> r.setInteractiveConversion(interactiveWindow));

        File[] files = new File(inDir).listFiles();
        Arrays.sort(files);

        // interactive conversions block the conversion process.
        // better is be to convert in two phases, first all conversions
        // that don't need user input, and only after that convert those that
        // use interactive conversions. Then the user is free to move while
        // the machine computes for most of the time.

        // these are all the files requiring interaction, those will be
        // converted in a second pass, after all other conversions are done.
        List<File> interactiveFiles = new ArrayList<>();

        // ** changed by Maximilian
        // now first converts all non interactive files, catches those that
        // require interactions and does those in a second run
        interactiveWindow.setInteractiveAllowed(false);
        for (File file : files) {
            try {
                convertFile(t, file, new File(outDir, file.getName()));
            } catch (BlockedInteractionException e) {
                interactiveFiles.add(file);
                logger.info("conversion requires interaction, doing this in second run!");
            }
        }

        // now convert all files which require interaction with user
        interactiveWindow.setInteractiveAllowed(true);
        for (File file : interactiveFiles) {
            convertFile(t, file, new File(outDir, file.getName()));
        }

        interactiveWindow.close();
    }

    private static void convertFile(Transducer t, File inFile, File outFile) {
        logger.info(String.format("Testing file %s", inFile));
        Root orig = fileToTree(inFile);
        Root transduced = t.applyTo(orig);
        Document outDoc = new DefaultDocument();
        outDoc.createBundle().addTree(transduced);

        new CoNLLUWriter().writeDocument(outDoc, outFile.toPath());


        // the ConLLUWriter writes the tree in an unordered way, while the ConLLUWriter cannot parse
        // an unordered tree... quick fix is to order the lines in the output file afterwards...
        try {
            BufferedReader r = new BufferedReader(new FileReader(outFile));
            List<String> rows = new ArrayList<>();
            String s = r.readLine();
            while(s != null) {
                rows.add(s);
                s = r.readLine();
            }
            rows.sort((a, b) -> {
                if(a.startsWith("#") || b.length() < 1)
                    return Integer.compare(1, 0);
                if(b.startsWith("#") || a.length() < 1)
                    return Integer.compare(0, 1);
                int first = Integer.parseInt(a.split("\t")[0]);
                int second = Integer.parseInt(b.split("\t")[0]);
                return Integer.compare(first, second);
            });
            r.close();
            FileWriter w = new FileWriter(outFile);
            w.write(rows.stream().reduce("", (a, b) -> a + System.lineSeparator() + b));
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testMain(Namespace ns) throws Exception {
        logger.info("Testing");
        String compDir = ns.getString("expected_dir");
        String genDir = ns.getString("actual_dir");
        String origDir = ns.getString("original_dir");

        int punctuationNodes = 0;
        int ignoredNodes = 0;
        int notConvertedNodes = 0;
        int incorrectNodes = 0;
        int correctNodes = 0;
        int completeMatch = 0;

        FilenameFilter conllFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                if (lowercaseName.endsWith(".conll")) {
                    return true;
                } else {
                    return false;
                }
            }
        };

        File[] files = new File(genDir).listFiles(conllFilter);
        Arrays.sort(files);

        for (File file : files) {
            logger.info(String.format("Testing file %s", file.getName()));
            logger.debug("reading generated");
            Root generated = fileToTree(file);
            logger.debug("reading expected");
            Root expected = fileToTree(new File(compDir, file.getName()));
            Root original = fileToTree(new File(origDir, file.getName()));
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

        PrecisionStats ps = new PrecisionStats();
        for (File file : files) {
            Root generated = fileToTree(file);
            Root expected = fileToTree(new File(compDir, file.getName()));
            Root original = fileToTree(new File(origDir, file.getName()));
            ps.accumulate(original, expected, generated);
        }
        String x = ps.breakdownAsString();
        logger.info("\n" + x);

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
            cc.checkTree(orig, r);
        }

        int correctNodes = cc.getTotalConvertedCount();
        int puncuationNodes = cc.getTotalPunctuationCount();
        int blockers = cc.getTotalBlockerCount();
        int indirectly = cc.getTotalIndirectlyNotConvertedCount();

        logger.info(String.format("%d trees total", cc.getTotalTreeCount()));
        logger.info(String.format("%d fully converted Trees", cc.getConvertedTreeCount()));

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

    private static void listMain(Namespace ns) {
        String srcDir = ns.getString("src_dir");
        String genDir = ns.getString("gen_dir");
        boolean onlyIncomplete = ns.getBoolean("incomplete");
        boolean showBlockers = ns.getBoolean("blockers");

        File[] files = new File(genDir).listFiles();
        Arrays.sort(files);

        for (File file : files) {
            Root gen = fileToTree(file);
            Root orig = fileToTree(new File(srcDir + "/" + file.getName()));
            TreeConversionStats tcStats = new TreeConversionStats(orig, gen);
            tcStats.check();
            if (!onlyIncomplete || !tcStats.isTreeFullyConverted()) {
                StringBuilder sb = new StringBuilder();
                sb.append(file.getName());
                if (showBlockers) {
                    String blockers = tcStats.getBlockerNodes().stream().map(Node::getDeprel).collect(Collectors.joining(", "));
                    sb.append(" " + blockers);
                }
                System.out.println(sb.toString());
            }
        }
    }

    private static void searchMain(Namespace ns) {
        String dir = ns.getString("dir");
        String searchExpression = ns.getString("expr");

        // parse search expression
        ANTLRInputStream input = new ANTLRInputStream(searchExpression);
        TransducerLexer lexer = new TransducerLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TransducerParser parser = new TransducerParser(tokens);
        TransducerParser.MatchTreeContext context =  parser.matchTree(new HashMap<>());
        Tree queryTree = context.tree;
        //infer missing structure by creating a dummy rule ... really dirty hack, sorry!
        Rule dummyRule = new Rule(queryTree, generateDummyReplacementNode(queryTree), null, "");

        File[] files = new File(dir).listFiles();
        Arrays.sort(files);

        NodeClassifier defaultNodeClassifier = new NodeClassifier();

        for (File file : files) {
            Root tree = fileToTree(file);
            ConversionState convState = new ConversionState(tree, defaultNodeClassifier);

            for (DepTreeFrontierNode frontierNode : convState.getFrontier()) {
                Binding match = Matcher.getBinding(queryTree.frontierNode, frontierNode);
                if (match != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(file.getName());
                    sb.append(":");
                    for (String key : queryTree.getUsedNames()) {
                        Node n = match.singles.get(key);
                        if (n != null) {
                            sb.append("[" + key + ":" + n.getDeprel() + "]");
                        }
                    }
                    System.out.println(sb.toString());
                }
            }
        }
    }

    private static ReplacementNode generateDummyReplacementNode(Tree matchTree) {
        List<String> usedNames = matchTree.getUsedNames();
        ReplacementNode dummyParent = new ReplacementNode();
        dummyParent.setName(usedNames.stream().collect(Collectors.joining()));
        for (String name : usedNames) {
            ReplacementNode rNode = new ReplacementNode();
            rNode.setName(name);
            rNode.setParent(dummyParent);
            dummyParent.addChild(rNode);
        }
        return dummyParent;
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
        controller.setTitle(new File(conllFilePath).getCanonicalPath() + " - TrUDucer");
        controller.setTree(tree, transducer.getNodeClassifier());
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
