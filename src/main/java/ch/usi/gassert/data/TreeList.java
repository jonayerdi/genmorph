package ch.usi.gassert.data;

import ch.usi.gassert.data.tree.Tree;

import java.util.LinkedList;
import java.util.List;

/**
 * This class transforms a tree in a list to performs operations like get a random element
 */
public class TreeList {

    private final List<Tree> listNodes;
    private final List<Tree> listParent;

    public TreeList(final Tree root) {
        listNodes = new LinkedList<>();
        listParent = new LinkedList<>();
        dfs(null, root);
    }

    public List<Tree> getListNodes() {
        return listNodes;
    }

    public List<Tree> getListParent() {
        return listParent;
    }

    private void dfs(final Tree parent, final Tree root) {
        if (root == null) {
            return;
        }
        listNodes.add(root);
        listParent.add(parent);
        dfs(root, root.getLeft());
        dfs(root, root.getRight());
    }

    @Override
    public String toString() {
        return "TreeList{" +
                "listNodes=" + listNodes +
                ", listParent=" + listParent +
                '}';
    }
}
