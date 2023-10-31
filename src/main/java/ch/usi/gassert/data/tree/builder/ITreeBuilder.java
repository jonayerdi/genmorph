package ch.usi.gassert.data.tree.builder;

import ch.usi.gassert.data.tree.ITree;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.Pair;

import java.util.stream.Stream;

/**
 * Interface for classes that handle generating and transforming Trees
 */
public interface ITreeBuilder<T extends ITree> {

    // Get info on the generated Trees
    Tree.Type getTreeType();

    // Build random expression from scratch
    T buildRandomTree();

    // Convert the given Tree to whatever ITree representation this ITreeBuilder uses
    T matchTree(final Tree tree);

    // Evolutionary operators
    T mutate(final ITree tree, final Individual individual);
    Pair<T, T> crossover(ITree mother, ITree father, final Individual individualMother, final Individual individualFather);

    // Interface for the Evolutionary algorithm to notify the current generation, in case behaviour changes
    void updateGen(int gen);

    // Minimized expressions
    Stream<T> minimize(final ITree tree);

}
