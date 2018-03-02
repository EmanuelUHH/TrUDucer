package nats.truducer.data;

import cz.ufal.udapi.core.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Creates a binding between the left-hand side tree of a rule and a part of a dependency tree.
 */
public class Matcher {

    private static class PartialBinding {
        private MatchingNode mn;
        private Node node;
        private Binding currentBinding;

        PartialBinding(MatchingNode mn, Node node, Binding currentBinding) {
            this.mn = mn;
            this.node = node;
            this.currentBinding = currentBinding;
        }

        /**
         * @return  A binding that was created by applying this partial binding.  This includes a subbinding
         * for the subtree below this node, that was also checked. And includes the binding that was given
         * when this PartialBinding was created.
         * Returns null if this partial Steps doesn't work.
         */
        Binding works() {
            Binding b = mn.matches(node, currentBinding);
            if (b != null) {
                List<MatchingNode> children = new ArrayList<>();
                for (StructNode sn : mn.getChildren()) {
                    if (sn instanceof FrontierNode)
                        throw new IllegalStateException("Nested Frontier nodes detected");
                    else
                        children.add((MatchingNode)sn);
                }
                Binding subbinding = getBinding(children, node.getChildren(), mn.getCatchallVar());
                if (subbinding != null) {
                    return b.merge(subbinding);
                }
            }
            return null;
        }
    }

    private static class SearchState2 {
        Binding binding;
        List<MatchingNode> matchNodes;
        List<Node> nodes;
        Stack<PartialBinding> steps = new Stack<>();

        SearchState2(List<MatchingNode> mns, List<Node> nodes) {
            this(mns, nodes, new Binding());
        }

        SearchState2(List<MatchingNode> mns, List<Node> nodes, Binding existingBindings) {
            this.matchNodes = mns;
            this.nodes = nodes;
            this.binding = existingBindings;
            if (!this.matchNodes.isEmpty()) {
                MatchingNode nextToMatch = this.matchNodes.get(0);
                for (Node n : this.nodes) {
                    this.steps.push(new PartialBinding(nextToMatch, n, this.binding));
                }
            }
        }

        public boolean isComplete() {
            return this.matchNodes.isEmpty();
        }

        SearchState2 generateNext(Binding newBindings) {
            List<MatchingNode> remaining = this.matchNodes.stream()
                    .filter(mn -> !newBindings.singles.containsKey(mn.getName()))
                    .collect(Collectors.toList());
            List<Node> remainingNodes = this.nodes.stream()
                    .filter(n -> !newBindings.singles.containsValue(n))
                    .collect(Collectors.toList());
            return new SearchState2(remaining, remainingNodes, newBindings);
        }
    }

    private static Binding getBinding(List<MatchingNode> mns, List<Node> nodes, String catchallVar) {
        SearchState2 result = null;

        SearchState2 initialState = new SearchState2(mns, nodes);
        if (initialState.isComplete())
            result = initialState;

        Stack<SearchState2> states = new Stack<>();
        states.push(initialState);
        Outer:
        while (result == null && !states.isEmpty()) {
            SearchState2 currentState = states.peek();
            while (!currentState.steps.isEmpty()) {
                PartialBinding step = currentState.steps.pop();
                Binding progression = step.works();
                if (progression != null) {
                    SearchState2 newState = currentState.generateNext(progression);
                    if (newState.isComplete()) {
                        result = newState;
                        break Outer;
                    }
                    else {
                        states.push(newState);
                        break;
                    }
                }
            }
            if (currentState.steps.isEmpty())
                states.pop(); // discard current state, no partial steps left
        }

        // catchall stuff
        Binding resultBinding = null;
        if (result != null) {
            resultBinding = result.binding;
            if (catchallVar != null)
                resultBinding = resultBinding.putCatchall(catchallVar, result.nodes);
        }
        return resultBinding;
    }


    private static Binding getBindingUpwards(FrontierNode fn, DepTreeFrontierNode n, Binding currentBinding) { // WTF I don't get this code anymore
        Binding b = currentBinding;
        MatchingNode parent = fn.getParent();
        Optional<Node> parentNode = Optional.of(n.getParent());
        Optional<Node> subtreeHead = parentNode;
        while (parent != null) {
            if (parent.getChildren().size() != 1)
                throw new IllegalArgumentException("The parent should not have additional subtrees");

            if (!parentNode.isPresent())
                return null; // cannot match further

            subtreeHead = parentNode.get().getParent();

            Binding match = parent.matches(parentNode.get(), b);
            if (match == null)
                return null; // nodes didn't match

            b = match;
            b.nodesAboveFrontier.add(parentNode.get());

            if (parent.getCatchallVar() != null) {
                List<Node> rest = new ArrayList<>(parentNode.get().getChildren());
                rest.removeAll(b.getBoundNodes());
                b = b.putCatchall(parent.getCatchallVar(), rest);
                b.nodesAboveFrontier.addAll(rest);
            }

            parent = (MatchingNode)parent.getParent(); // cast is valid, the parent cannot be a frontier node again.
            parentNode = parentNode.get().getParent();
        }

        b = b.putSingle(null, subtreeHead.orElse(null));  // special case

        return b;
    }


    public static Binding getBinding(FrontierNode fn, DepTreeFrontierNode dtfn) { // TODO here a list of Nodes needs to be given
        List<MatchingNode> mns = new ArrayList<>();
        mns.addAll(fn.getChildrenAsMn());
        Binding b = getBinding(mns, dtfn.getChildren(), fn.getCatchallVar());
        if (b != null) {
            b = getBindingUpwards(fn, dtfn, b);  // TODO here the common parent of the nodes in the list should be given
        }
        return b;
    }

    // Backtracking
    // State is the binding
    // partial Solution is a binding for one var
    // only relevant for one layer in the tree, the subbinding is a new backtracking problem
    // stack of SearchStates, where a state consists of the current binding and the remaining partial bindings to be checked
    // if a partial binding works, a new SearchState is created with the modified binding
    // there, next steps need to be generated again.  If a partial binding is revoked, the remaining alternatives to be checked are still there
    //
    // create SearchState with mns and nodes, and already bound vars
    // -> automatically creates next partial bindings in a list inside the state
    // while there are still searchstate on the stack:
    //     while there are still alternatives in the current search state:
    //         take the first alternative
    //         if it works:
    //             generate new search state
    //             if the new search state is complete:
    //                 return it!
    //             else:
    //                 push it to the stack.  If there are no alternatives in this state, it will be popped immediately
    //     no alternatives left -> discard current search state
    // no search states left, return null

    // fundamental question: is there only one possible conversion result?
    // -> depending on the order in which rules are checked, there might be multiple different results

    // that there is a varying number of children for each node is a bigger problem than I thought.

    // if there are multiple matching alternatives can be checked if I collect all possible bindings instead of taking the first matching one
}
