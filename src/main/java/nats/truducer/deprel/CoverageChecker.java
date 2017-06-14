package nats.truducer.deprel;

import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;

import java.util.*;

/**
 * Created by felix on 23/02/17.
 */
public class CoverageChecker {

    private final List<Node> punct = new ArrayList<>();
    private final List<Node> blockers = new ArrayList<>();
    private final Map<String, Integer> blockersIndividual = new HashMap<>();
    private final List<Node> indirectlyAffected = new ArrayList<>();
    private final Map<String, Integer> indirectlyIndividual = new HashMap<>();
    private final List<Node> converted = new ArrayList<>();
    private final Map<String, Integer> convertedIndividual = new HashMap<>();


    public CoverageChecker() {

    }


    public void check(Root original, Root generated) {
        for (Node n : generated.getNode().getChildren()) {
            checkNode(n, original);
        }
    }

    private static void incOrInit(Map<String, Integer> map, String key) {
        if (map.containsKey(key)) {
            map.put(key, map.get(key) + 1);
        } else {
            map.put(key, 0);
        }
    }

    private static Node getById(Root tree, int id) {
        return tree.getDescendants().stream().filter(n -> n.getOrd() == id).findFirst().get();
    }

    private void checkNode(Node node, Root original) {
        if (node.getDeprel().equals("ROOT")) {
            punct.add(node);
        } else if (node.getDeprel().equals(getById(original, node.getOrd()).getDeprel())) {
            blockers.add(node);
            incOrInit(blockersIndividual, getById(original, node.getOrd()).getDeprel());
            for (Node n : node.getDescendants()) {
                indirectlyAffected.add(n);
                incOrInit(indirectlyIndividual, getById(original, n.getOrd()).getDeprel());
            }
        } else {
            converted.add(node);
            incOrInit(convertedIndividual, getById(original, node.getOrd()).getDeprel());
            for (Node n : node.getChildren()) {
                checkNode(n, original);
            }
        }
    }


    public List<Node> getPunctuation() {
        return punct;
    }

    public List<Node> getBlockers() {
        return blockers;
    }

    public Map<String, Integer> getBlockersIndividual() {
        return blockersIndividual;
    }

    public List<Node> getIndirectlyAffected() {
        return indirectlyAffected;
    }

    public Map<String, Integer> getIndirectlyIndividual() {
        return indirectlyIndividual;
    }

    public List<Node> getConverted() {
        return converted;
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
        totalTokens += getConverted().size();
        totalTokens += getBlockers().size();
        totalTokens += getIndirectlyAffected().size();

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

    public String getBlockerStatsAsString() {
        Map<String, Integer> blockerDeprels = new HashMap<>();
        for (Node n : blockers) {
            incOrInit(blockerDeprels, n.getDeprel());
        }

        class Tuple {
            String depRel;
            int count;
            public Tuple(String d, int c) {
                depRel = d;
                count = c;
            }
        }

        List<Tuple> vals = new ArrayList<Tuple>();
        for (String key : blockerDeprels.keySet()) {
            vals.add(new Tuple(key, blockerDeprels.get(key)));
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
            sb.append(String.format("%1$9s %2$6.4f\n", t.depRel, ((double)t.count)/((double)blockers.size())));
        }

        return sb.toString();
    }
}
