package nats.truducer.gui;

import nats.truducer.deprel.TreeConversionStats;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * stores relevant information about a conversion.
 * TODO store rules used in conversion somehow
 * -> if you can look through all sentences, a rule was applied to, it is easier to spot wrongly converted sentences
 */
public class ConversionResult {
    private File file;
    private File srcFile;
    private List<String> blockers;
    private boolean successfull;

    public ConversionResult(File file, File srcFile, TreeConversionStats stats) {
        this.file = file;
        this.srcFile = srcFile;
        this.blockers = new ArrayList<>();
        this.successfull = stats.isTreeFullyConverted();
        stats.getBlockerNodes().forEach(a -> blockers.add(a.getDeprel()));
    }

    @Override
    public String toString() {
        return file.getName() + ":     " + blockers.stream().collect(Collectors.joining(", "));
    }

    public File getFile() {
        return file;
    }

    public File getSrcFile() {
        return srcFile;
    }
}
