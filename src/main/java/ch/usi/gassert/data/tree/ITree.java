package ch.usi.gassert.data.tree;

/**
 * This interface is implemented by any type that is or can be converted to a Tree
 */
public interface ITree {
    Tree asTree();
    ITree cloneTree();
    Integer getNumberOfNodes();
    Tree.Type getType();
}
