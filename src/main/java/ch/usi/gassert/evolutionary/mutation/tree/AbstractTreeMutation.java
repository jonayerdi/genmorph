package ch.usi.gassert.evolutionary.mutation.tree;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeFactory;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evolutionary.TreeBehaviourManager;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.evolutionary.mutation.IMutation;

public abstract class AbstractTreeMutation implements ITreeMutation {

    public final TreeBehaviourManager treeBehaviourManager;

    protected AbstractTreeMutation(final TreeBehaviourManager treeBehaviourManager) {
        this.treeBehaviourManager = treeBehaviourManager;
    }

}
