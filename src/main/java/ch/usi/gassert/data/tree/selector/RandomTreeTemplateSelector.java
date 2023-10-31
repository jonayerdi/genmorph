package ch.usi.gassert.data.tree.selector;

import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.util.random.MyRandom;

import java.util.Collection;

public class RandomTreeTemplateSelector implements ITreeTemplateSelector {

    @Override
    public TreeTemplate selectTree(Collection<TreeTemplate> generatedNodes) {
        return MyRandom.getInstance().getRandomElementCollection(generatedNodes);
    }

}
