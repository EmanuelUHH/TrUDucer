package nats.truducer.deprel;

import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;

import java.util.*;

import static nats.truducer.deprel.TreeUtils.getById;

/**
 * Aggregates statistics about tree converted nodes.
 */
public class CoverageChecker {

    private final Map<String, Integer> blockersIndividual = new HashMap<>();
    private final Map<String, Integer> indirectlyIndividual = new HashMap<>();
    private final Map<String, Integer> convertedIndividual = new HashMap<>();

    private int punctuationNodes = 0;
    private int blockerNodes = 0;
    private int indirectlyAffectedNodes = 0;
    private int convertedNodes = 0;

    private int convertedTreeCount = 0;
    private int totalTreeCount = 0;


    public CoverageChecker() {

    }


    public void checkTree(Root original, Root generated) {
        TreeConversionStats tStats = new TreeConversionStats(original, generated);
        tStats.check();
        punctuationNodes += tStats.getPunctuationNodes().size();
        blockerNodes += tStats.getBlockerNodes().size();
        indirectlyAffectedNodes += tStats.getIndirectlyNotConvertedNodes().size();
        convertedNodes += tStats.getConvertedNodes().size();

        for (Node blockerNode : tStats.getBlockerNodes()) {
            incOrInit(blockersIndividual, getById(original, blockerNode.getOrd()).getDeprel());
        }
        for (Node iaNode : tStats.getIndirectlyNotConvertedNodes()) {
            incOrInit(indirectlyIndividual, getById(original, iaNode.getOrd()).getDeprel());
        }
        for (Node convNode : tStats.getConvertedNodes()) {
            incOrInit(convertedIndividual, getById(original, convNode.getOrd()).getDeprel());
        }

        totalTreeCount++;
        if (tStats.isTreeFullyConverted())
            convertedTreeCount++;
    }

    private static void incOrInit(Map<String, Integer> map, String key) {
        if (map.containsKey(key)) {
            map.put(key, map.get(key) + 1);
        } else {
            map.put(key, 1);
        }
    }

    public int getTotalPunctuationCount() {
        return punctuationNodes;
    }

    public int getTotalBlockerCount() {
        return blockerNodes;
    }

    public int getTotalIndirectlyNotConvertedCount() {
        return indirectlyAffectedNodes;
    }

    public int getTotalConvertedCount() {
        return convertedNodes;
    }

    public int getTotalTreeCount() {
        return totalTreeCount;
    }

    public int getConvertedTreeCount() {
        return convertedTreeCount;
    }

    public Map<String, Integer> getBlockersIndividual() {
        return blockersIndividual;
    }

    public Map<String, Integer> getIndirectlyIndividual() {
        return indirectlyIndividual;
    }

    public Map<String, Integer> getConvertedIndividual() {
        return convertedIndividual;
    }

    /**
     * Returns a sorted list of potential keys for the maps
     */
    public List<String> overallKeyset() {
        Set<String> keyset = new HashSet<>();
        keyset.addAll(blockersIndividual.keySet());
        keyset.addAll(indirectlyIndividual.keySet());
        keyset.addAll(convertedIndividual.keySet());
        List<String> sorted = new ArrayList<>(keyset);
        Collections.sort(sorted);
        return sorted;
    }

    public String getTableAsString() {
        int totalTokens = 0;
        totalTokens += getTotalConvertedCount();
        totalTokens += getTotalBlockerCount();
        totalTokens += getTotalIndirectlyNotConvertedCount();

        StringBuilder sb = new StringBuilder();
        sb.append("DepRel    total  conv block  rest\n");

        for (String depRel : overallKeyset()) {
            double convertedCount = convertedIndividual.getOrDefault(depRel, 0);
            double blockerCount = blockersIndividual.getOrDefault(depRel, 0);
            double indirectlyCount = indirectlyIndividual.getOrDefault(depRel, 0);
            double totalCount = convertedCount + blockerCount + indirectlyCount;

            String line = String.format("%1$9s %2$7.4f %3$7.4f %4$7.4f %5$7.4f",
                    depRel, totalCount/totalTokens, convertedCount/totalCount,
                    blockerCount/totalCount, indirectlyCount/totalCount);
            sb.append(line + "\n");

        }

        return sb.toString();
    }

    /**
     * Returns a string containing percentage values for the blocker deprels.
     * For each deprel that causes a conversion block, the share is given that it has
     * on the total conversion blocks, i.e. APP 0.59, meaning that in 59% of the
     * blocks, the APP relation is the cause.
     */
    public String getBlockerStatsAsString() {
        class Tuple {
            String depRel;
            int count;
            public Tuple(String d, int c) {
                depRel = d;
                count = c;
            }
        }

        List<Tuple> vals = new ArrayList<Tuple>();
        for (String key : blockersIndividual.keySet()) {
            vals.add(new Tuple(key, blockersIndividual.get(key)));
        }

        Collections.sort(vals, new Comparator<Tuple>() {
            @Override
            public int compare(Tuple t1, Tuple t2) {
                int r = Integer.compare(t1.count, t2.count);
                if (r == 0)
                    r = t1.depRel.compareTo(t2.depRel);
                return r;
            }
        });

        StringBuilder sb = new StringBuilder();
        for (Tuple t : vals) {
            sb.append(String.format("%1$9s %2$6.4f\n", t.depRel, ((double)t.count)/((double)getTotalBlockerCount())));
        }

        return sb.toString();
    }
}
