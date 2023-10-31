package ch.usi.gassert.data.tree.template;

import ch.usi.gassert.data.tree.ITree;
import ch.usi.gassert.data.tree.Tree;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This is the data structure that represents a group of Trees to instantiate a TreeTemplate.
 */
public class TreeGroup {

    public final TreeTemplate treeTemplate;
    public final Map<TreeTemplate, ITree> mappings;

    public TreeGroup(TreeTemplate treeTemplate) {
        this.treeTemplate = treeTemplate;
        final Set<TreeTemplate> generatedNodes = treeTemplate.getGeneratedNodes();
        this.mappings = new HashMap<>(generatedNodes.size());
        for (TreeTemplate t : generatedNodes) {
            this.mappings.put(t, t.getTreeBuilder().buildRandomTree());
        }
    }

    public TreeGroup(final TreeTemplate treeTemplate, final Map<TreeTemplate, ITree> mappings) {
        this.treeTemplate = treeTemplate;
        this.mappings = new HashMap<>(mappings.size());
        for (Map.Entry<TreeTemplate, ITree> m : mappings.entrySet()) {
            this.mappings.put(m.getKey(), m.getValue().cloneTree());
        }
    }

    public TreeGroup(TreeGroup treeGroup) {
        this(treeGroup.treeTemplate, treeGroup.mappings);
    }

    public Integer getTotalNumberOfNodes() {
        return this.mappings.values().stream().mapToInt(ITree::getNumberOfNodes).sum();
    }

    public Tree buildTree() {
        return treeTemplate.buildTree(mappings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TreeGroup treeGroup = (TreeGroup) o;
        return mappings.equals(treeGroup.mappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mappings);
    }
}
