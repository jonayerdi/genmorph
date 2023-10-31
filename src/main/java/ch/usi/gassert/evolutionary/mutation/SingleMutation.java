package ch.usi.gassert.evolutionary.mutation;

import ch.usi.gassert.data.tree.ITree;
import ch.usi.gassert.data.tree.builder.ITreeBuilder;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evolutionary.BehaviourManager;
import ch.usi.gassert.evolutionary.Individual;

public class SingleMutation extends AbstractMutation {

    public SingleMutation(BehaviourManager behaviourManager) {
        super(behaviourManager);
    }

    @Override
    public TreeGroup mutate(final TreeGroup treeGroup, final Individual individual) {
        // Select a random subtree and mutate it
        final TreeGroup mutatedTreeGroup = new TreeGroup(treeGroup);
        TreeTemplate subTree = behaviourManager.getTreeTemplateSelector().selectTree(treeGroup);
        ITreeBuilder<?> treeBuilder = subTree.getTreeBuilder();
        ITree mutatedTree = treeBuilder.mutate(treeGroup.mappings.get(subTree), individual);
        mutatedTreeGroup.mappings.put(subTree, mutatedTree);
        return mutatedTreeGroup;
    }

}
