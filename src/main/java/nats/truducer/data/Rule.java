package nats.truducer.data;

import cz.ufal.udapi.core.Node;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import nats.truducer.interactive.InteractiveConversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by felix on 16/01/17.
 */
public class Rule {

    private Tree matchTree;

    private ReplacementNode replacementTree;

    private Script groovyScript = null;
    private InteractiveConversion interactiveConversion = null;

    private final String origString;

    public Rule(Tree matchTree, ReplacementNode replacementTree, String groovycode, String origString) {
        this.matchTree = matchTree;
        this.replacementTree = replacementTree;
        inferMissingStructure();
        if (groovycode != null) {
            GroovyShell shell = new GroovyShell();
            groovyScript = shell.parse(groovycode);
        }
        this.origString = origString;
    }

    public void validate() {
        if (matchTree.frontierNode == null)
            throw new IllegalStateException("The matchTree needs a frontier node");
    }

    public void setInteractiveConversion(InteractiveConversion ic) {
        this.interactiveConversion = ic;
    }

    private ReplacementNode getNode(ReplacementNode root, String name) {
        if (root.getName().equals(name))
            return root;
        for (ReplacementNode child : root.getChildren()) {
            ReplacementNode result = getNode(child, name);
            if (result != null)
                return result;
        }
        return null;
    }

    /**
     * Creates a name for a node with the given string in it.
     * Used for the automated creation of matching nodes when parts of the rule are inferred.
     * @param desiredName  The name that is desired for the node.
     * @return  The desiredName or an indexed version of this name.
     */
    private String getName(String desiredName) {
        List<String> usedNames = matchTree.getUsedNames();
        if (!usedNames.contains(desiredName))
            return desiredName;
        int i = 1;
        String newName = null;
        do {
            newName = String.format("%s%d", desiredName, i);
            i++;
        } while (usedNames.contains(newName));
        return newName;
    }

    /**
     * A method to infer missing catchAll variables on nodes on the left-hand side of the rule and
     * put them in the right place on the right-hand side.
     * @param sn  a StructNode is either a MatchingNode or a FrontierNode, any kind of node from the left-hand side of the rule.
     * @param root  the replacementTree.
     */
    private void addMissingRest(StructNode sn, ReplacementNode root) {
        if (sn instanceof MatchingNode) {
            // Rests on common nodes is just catched and attached in the same place on the right-hand side.
            MatchingNode mn = (MatchingNode) sn;
            if (mn.getCatchallVar() == null) {
                String catchallName = getName("r");
                mn.setCatchallVar(catchallName);
                getNode(root, mn.getName()).addCatchAllVar(catchallName);
            }
        } else if (sn instanceof FrontierNode){
            // Rests on frontier nodes is catched and attached to the parent of the frontier node, on the right-hand side.
            // The frontier node is gone on the right-hand side.
            if (sn.getCatchallVar() == null) {
                String catchallname = getName("fr");
                sn.setCatchallVar(catchallname);
                getNode(root, ((FrontierNode) sn).getParent().getName()).addCatchAllVar(catchallname);
            }
        }
        for (StructNode child : sn.getChildren())
            addMissingRest(child, root);
    }

    private void inferMissingStructure() {
        // add rest to every node:
        // n1:a() -> n1:b() => n1:a(?r) -> n1:b(?r)

        // add frontier node and parent if it is missing:
        // n1:a(?r) -> n1:b(?r) => parent({n1:a(?r), ?frontierrest}, ?parentrest) -> parent(n1:b(?r), ?frontierrest, ?parentrest)
        // parentrest won't be added to the frontier because it was above the frontier initially
        // frontierrest will be put into a new frontier node

        if (matchTree.frontierNode == null) {
            // frontier node
            FrontierNode fn = new FrontierNode();
            String fnRestName = getName("frontierrest");
            fn.setCatchallVar(fnRestName);
            fn.addChild(matchTree.root);
            matchTree.root.setParent(fn);
            // parent node
            String parentName = getName("parent");
            MatchingNode parent = new MatchingNode(parentName, null, null, new HashMap<>());
            String parentRestName = getName("parentrest");
            parent.setCatchallVar(parentRestName);
            parent.addChild(fn);
            fn.setParent(parent);
            // replacement tree
            ReplacementNode replParent = new ReplacementNode();
            replParent.setName(parentName);
            replParent.addChild(replacementTree);
            replacementTree.setParent(replParent);
            replParent.addCatchAllVar(fnRestName);
            replParent.addCatchAllVar(parentRestName);

            matchTree.root = parent;
            matchTree.frontierNode = fn;
            replacementTree = replParent;
        }

        addMissingRest(matchTree.root, replacementTree);
    }


    /**
     * Applies this rule to a conversion state and creates a new conversion state,
     * containing the new tree after the rule was applied.
     * @param currentState  The conversion state to apply the rule to.
     * @param frontierNode  The index of the frontier node to apply the rule to.
     * @return  converted state or null if conversion not possible.
     */
    ConversionState apply(ConversionState currentState, int frontierNode) {
        // original reinbekommen
        // binding machen und gucken ob es passt
        // wenns passt, kopie machen und nochmal binding erstellen
        // setHeads, frontierAdditions
        // altes und neues binding zu einem Groovy Binding kombinieren
        // groovy code aufrufen
        // wenn er true zurück gibt dann wird die kopie zurück gegeben



        ConversionState result = null;
        DepTreeFrontierNode fn = currentState.getFrontier().get(frontierNode);
        Binding bindingCurrent = Matcher.getBinding(matchTree.frontierNode, fn);

        if (bindingCurrent != null) {
            result = currentState.deepCopy();
            result.setAppliedRule(this);
            DepTreeFrontierNode fn2 = result.getFrontier().get(frontierNode);
            Binding bindingNew = Matcher.getBinding(matchTree.frontierNode, fn2);
            assert bindingNew != null; // can't be null if bindingCurrent != null
                                       // otherwise deepCopy is broken
            // *** restructuring part ***
            // attach all the nodes in the binding to their new parents
            setHeads(bindingNew, replacementTree);
            // update frontier according to the replacementTree.
            // Also sets dependency relations!  Method should probably be renamed ...
            List<DepTreeFrontierNode> frontierAdditions = getFrontierAdditions(bindingNew);
            result.getFrontier().remove(frontierNode);
            result.getFrontier().addAll(frontierAdditions);
            // TODO merge neighboring frontier nodes
            // If two frontier nodes have the same parent, they should be merged.

            if (groovyScript != null) {
                groovy.lang.Binding scriptContext = new groovy.lang.Binding();
                groovyScript.setBinding(scriptContext);
                scriptContext.setProperty("interactive", interactiveConversion);
                addProperties(scriptContext, bindingCurrent, "");  // previous nodes without prefix
                addProperties(scriptContext, bindingNew, "_");     // new nodes prefixed with "_"
                Object scriptReturnVal = groovyScript.run();
                if (scriptReturnVal instanceof Boolean) {
                    boolean scriptConstraintSatisfied = (boolean)scriptReturnVal;
                    if (!scriptConstraintSatisfied)
                        result = null;
                }
            }
        }
        return result;
    }

    private void addProperties(groovy.lang.Binding context, Binding toAdd, String prefix) {
        for (Map.Entry<String, Node> entry : toAdd.singles.entrySet()) {
            context.setProperty(prefix + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, List<Node>> entry : toAdd.catchalls.entrySet()) {
            context.setProperty(prefix + entry.getKey(), entry.getValue());
        }
    }

    /**
     * Collects the new nodes for the frontier, also sets the depRels.
     */
    private List<DepTreeFrontierNode> getFrontierAdditions(Binding binding) {
        List<DepTreeFrontierNode> frontierAdditions = new ArrayList<>();
        collectFrontierAdditions(binding, replacementTree, frontierAdditions);
        return frontierAdditions;
    }

    /**
     * Recursive method, therefore no return
     * @param binding  the binding to use to resolve node names, depRelVars and to see which nodes were above the frontier
     *                 initially
     * @param subtree  the tree to traverse for additions
     * @param frontier  the list of nodes which need to be converted next
     */
    private void collectFrontierAdditions(Binding binding, ReplacementNode subtree, List<DepTreeFrontierNode> frontier) {
        Node currentNode = binding.singles.get(subtree.getName());
        // node has not set a deprel, therefore it still needs to be transduced.
        if (subtree.getDepRel() == null) {
            if (!binding.nodesAboveFrontier.contains(currentNode)) {
                frontier.add(new DepTreeFrontierNode(currentNode));
                return;
            }
        } else {
            // a deprel is set, check if it is a var that needs to be resolved.
            // set the deprel
            if (subtree.getDepRel().startsWith("$")) { // TODO remove this string literal
                currentNode.setDeprel(binding.depRels.get(subtree.getDepRel()));
            } else {
                currentNode.setDeprel(subtree.getDepRel());
            }
        }

        // add all the nodes from the catchalls to the frontier
        // if the nodes were above the frontier in the binding, they don't need to be added
        for (String catchall : subtree.getCatchAllVars()) {
            List<Node> additions = binding.catchalls.get(catchall).stream()
                    .filter(n -> !binding.nodesAboveFrontier.contains(n)).collect(Collectors.toList());
            if (!additions.isEmpty())
                frontier.add(new DepTreeFrontierNode(additions));
        }
        // recursion step into all the children.
        for (ReplacementNode mn : subtree.getChildren()) {
            collectFrontierAdditions(binding, mn, frontier);
        }
    }

    private void setHeads(Binding binding, ReplacementNode mn) {
        Node node = binding.singles.get(mn.getName());
        ReplacementNode parent = mn.getParent();
        if (!node.isRoot()) {
            Node parentNode = null;
            if (parent == null) {
                parentNode = binding.singles.get(null);
            } else {
                parentNode = binding.singles.get(parent.getName());
            }
            assert parentNode != null;
            node.setParent(parentNode);

            for (String var : mn.getCatchAllVars()) {
                for (Node n : binding.catchalls.get(var)) {
                    n.setParent(node);
                }
            }
        }

        for (ReplacementNode rn : mn.getChildren()) {
            setHeads(binding, rn);
        }
    }

    @Override
    public String toString() {
        return origString;
        //return matchTree.toString() + " -> " + replacementTree.toString();
    }
}
