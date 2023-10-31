package ch.usi.gassert.data.tree.builder;

import ch.usi.gassert.data.tree.ITree;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeFactory;
import ch.usi.gassert.evolutionary.TreeBehaviourManager;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.Pair;

import java.util.stream.Stream;

public class GeneratedTreeBuilder implements ITreeBuilder<Tree> {

    public TreeBehaviourManager treeBehaviourManager;
    public TreeFactory treeFactory;
    public Tree.Type treeType;

    public GeneratedTreeBuilder(final TreeBehaviourManager treeBehaviourManager, final TreeFactory treeFactory,
                                final Tree.Type treeType) {
        this.treeBehaviourManager = treeBehaviourManager;
        this.treeFactory = treeFactory;
        this.treeType = treeType;
    }

    @Override
    public Tree.Type getTreeType() {
        return treeType;
    }

    @Override
    public Tree buildRandomTree() {
        return treeFactory.buildTree(treeType, treeBehaviourManager.GetTreeDepth.get(), treeBehaviourManager.ProbConstant);
    }

    @Override
    public Tree matchTree(Tree tree) {
        return tree;
    }

    @Override
    public Tree mutate(final ITree tree, final Individual individual) {
        return treeBehaviourManager.getMutation().mutate((Tree) tree, individual, treeFactory);
    }

    @Override
    public Pair<Tree, Tree> crossover(ITree mother, ITree father, final Individual individualMother, final Individual individualFather) {
        return treeBehaviourManager.getCrossover()
                .crossover((Tree) mother, (Tree) father, individualMother, individualFather);
    }

    @Override
    public void updateGen(int gen) {
        treeBehaviourManager.updateGen(gen);
    }

    @Override
    public Stream<Tree> minimize(final ITree iTree) {
        final Tree tree = (Tree) iTree;
        final Stream.Builder<Tree> minimized = Stream.builder();
        boolean skip = true;
        for (final Tree t : tree.getListOfNodes().getListNodes()) {
            // I skip the first one because is the same as the original tree
            if (skip) {
                skip = false;
                continue;
            }
            // Also skip subtrees of the wrong type
            if (!t.getType().equals(tree.getType())) {
                continue;
            }
            minimized.add(new Tree(t));
        }
        return minimized.build();
    }


}
