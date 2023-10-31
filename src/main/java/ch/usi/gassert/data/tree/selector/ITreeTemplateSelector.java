package ch.usi.gassert.data.tree.selector;

import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.template.TreeTemplate;

import java.util.Collection;

public interface ITreeTemplateSelector {

    TreeTemplate selectTree(final Collection<TreeTemplate> generatedNodes);

    default TreeTemplate selectTree(final TreeTemplate treeTemplate) {
        return this.selectTree(treeTemplate.getGeneratedNodes());
    }

    default TreeTemplate selectTree(final TreeGroup treeGroup) {
        return this.selectTree(treeGroup.mappings.keySet());
    }

}
