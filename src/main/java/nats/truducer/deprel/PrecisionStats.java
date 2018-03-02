package nats.truducer.deprel;

import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by felix on 14/06/17.
 */
public class PrecisionStats {

    private static class EdgeResult {

        String originalLabel;
        String expectedLabel;
        String generatedLabel;
        boolean parentCorrect;

        public EdgeResult(String oLabel, String eLabel, String gLabel, boolean parentCorrect) {
            originalLabel = oLabel;
            expectedLabel = eLabel;
            generatedLabel = gLabel;
            this.parentCorrect = parentCorrect;
        }

        public boolean isPunctuation() {
            return originalLabel.equals("ROOT");
        }

        public boolean isIgnored() {
            return originalLabel.equals(expectedLabel);
        }

        public boolean isConverted() {
            return !generatedLabel.equals(originalLabel);
        }

        public boolean isCorrect() {
            return generatedLabel.equals(expectedLabel) && parentCorrect;
        }
    }

    private static final List<EdgeResult> results = new ArrayList<>();
    private static final Set<String> inputLabels = new HashSet<>();



    public void accumulate(Root original, Root expected, Root generated) {
        List<Node> oNodes = original.getDescendants();
        Collections.sort(oNodes, new OrdComparator());
        List<Node> eNodes = expected.getDescendants();
        Collections.sort(eNodes, new OrdComparator());
        List<Node> gNodes = generated.getDescendants();
        Collections.sort(gNodes, new OrdComparator());

        for (int i = 0; i < oNodes.size(); i++) {
            inputLabels.add(oNodes.get(i).getDeprel());
            results.add(new EdgeResult(oNodes.get(i).getDeprel(),
                    eNodes.get(i).getDeprel(),
                    gNodes.get(i).getDeprel(),
                    eNodes.get(i).getParent().get().getOrd() == gNodes.get(i).getParent().get().getOrd()));
        }
    }

//    public List<EdgeResult> getConvertedResults() {
//        return results.stream()
//                .filter(er -> !er.isPunctuation())
//                .filter(er -> !er.isIgnored())
//                .filter(er -> er.isConverted())
//                .collect(Collectors.toList());
//    }


    private static void incOrInit(Map<String, Integer> map, String key) {
        if (map.containsKey(key)) {
            map.put(key, map.get(key) + 1);
        } else {
            map.put(key, 1);
        }
    }

    public String breakdownAsString() {
        StringBuilder sb = new StringBuilder();


        int punctuation = 0;
        int ignored = 0;
        int notConverted = 0;
        List<EdgeResult> converted = new ArrayList<>();
        int correct = 0;

        for (EdgeResult er : results) {
            if (er.isPunctuation())
                punctuation++;
            else if (er.isIgnored())
                ignored++;
            else if (!er.isConverted())
                notConverted++;
            else {
                converted.add(er);
                if (er.isCorrect())
                    correct++;
            }
        }


        sb.append(String.format("Total:          %d\n", results.size()));
        sb.append(String.format("Punctuation:    %d\n", punctuation));
        sb.append(String.format("Tot.-Punct.:    %d\n", results.size() - punctuation));
        sb.append(String.format("Ignored:        %d\n", ignored));
        sb.append(String.format("Not Converted:  %d\n", notConverted));
        sb.append(String.format("Converted:      %d\n", converted.size()));
        sb.append(String.format("Correct:        %d\n", correct));
        sb.append("\n");

        List<String> iLabels = new ArrayList<>(inputLabels);

        Collections.sort(iLabels);

        for (String label : iLabels) {
            List<EdgeResult> rs = converted.stream().filter(er -> er.originalLabel.equals(label)).collect(Collectors.toList());
            Map<String, Integer> expectedCount = new HashMap<>();
            Map<String, Integer> generatedCount = new HashMap<>();
            int correctTokens = 0;
            for (EdgeResult er : rs) {
                incOrInit(expectedCount, er.expectedLabel);
                incOrInit(generatedCount, er.generatedLabel);
                if (er.isCorrect())
                    correctTokens++;
            }
            sb.append(String.format("Label: %s\n", label));
            for (String eLabel : expectedCount.keySet()) {
                sb.append(String.format("  expected  %s %d times\n", eLabel, expectedCount.get(eLabel)));
            }
            sb.append("\n");
            for (String gLabel : generatedCount.keySet()) {
                sb.append(String.format("  generated %s %d times\n", gLabel, generatedCount.get(gLabel)));
            }
            sb.append(String.format("correct: %.4f\n", ((double)correctTokens)/((double)rs.size())));
        }

        return sb.toString();
    }
}
