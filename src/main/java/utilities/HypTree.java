package utilities;

import edu.mit.jwi.item.ISynset;
import java.util.*;

/**Creates a tree such that the data for each
 * element must be unique within the tree, enabling
 * constant time retrieval and O(n) traversal
 *
 * @param <T>
 */
public class HypTree {
    private List<HypNode> _rootBranches;
    private String _rootLemma;

    /**Creates a new tree
     *
     * @param rootLemma - The lemma this hyptree is
     *                    based on
     */
    public HypTree(String rootLemma)
    {
        _rootLemma = rootLemma;
        _rootBranches = new ArrayList<>();
    }

    /**Adds a child to the tree, given
     * a parent and a tag count; if parentData
     * is null, adds child to root
     *
     * @param data       - The child to add
     * @param parentData - The parent of this child
     * @param tagCount   - The child tag count
     * @return           - The HypNode that was added
     */
    public HypNode addChild(ISynset data, ISynset parentData, int tagCount)
    {
        HypNode child;
        if(parentData == null){
            //if parent is null, add this new node to the root
            child = new HypNode(data, null, tagCount);
            _rootBranches.add(child);
        } else {
            //else, add this new node to a parent
            HypNode parent = getNode(parentData);
            child = new HypNode(data, parent, tagCount);
            parent._children.add(child);
        }
        return child;
    }

    /**Returns the list of root branches; each 'root branch'
     * corresponds to a list of hypernyms for this tree's
     * senses
     * Optional argument stopAtSplits specifies whether
     * the branch should terminate whenever a node has multiple children
     *
     * @return
     */
    public List<List<HypNode>> getRootBranches()
    {
        return getRootBranches(false);
    }

    /**Returns the list of root branches; each 'root branch'
     * corresponds to a list of hypernyms for this tree's
     * senses
     * Optional argument stopAtSplits specifies whether
     * the branch should terminate whenever a node has multiple children
     *
     * @param stopAtSplits
     * @return
     */
    public List<List<HypNode>> getRootBranches(boolean stopAtSplits)
    {
        List<List<HypNode>> branches = new ArrayList<>();
        for(HypNode branchRoot : _rootBranches){
            List<HypNode> branch = new ArrayList<>();
            buildBranch(branchRoot, branch, stopAtSplits);
            branches.add(branch);
        }
        return branches;
    }

    /**Recursively builds a branch (list of nodes, starting at
     * the root's senses), adding the current node and children,
     * depending on how many there are and whether we
     * stop at splits further down the branch
     *
     * @param node
     * @param branch
     * @param stopAtSplits
     */
    private void buildBranch(HypNode node, List<HypNode> branch, boolean stopAtSplits)
    {
        //no matter what, reaching here indicates we should add
        //this node to the branch
        branch.add(node);

        if(node._children.size() == 1) {
            //if this node has one child, always recurse
            buildBranch(node._children.get(0), branch, stopAtSplits);
        } else if(node._children.size() > 1 && !stopAtSplits){
            //if this node has more than one child, only recurse if
            //we're not stopping at splits
            for(HypNode child : node._children)
                buildBranch(child, branch, stopAtSplits);
        }
    }

    /**Returns the node with the given data
     *
     * @param data
     * @return
     */
    public HypNode getNode(ISynset data)
    {
        HypNode foundNode = null;
        int i = 0;
        while(i < _rootBranches.size() && foundNode == null){
            foundNode = getNode(_rootBranches.get(i), data);
            i++;
        }
        return foundNode;
    }

    /**Returns the node with the given data
     * WARNING: searching for nodes by strings
     * will overgenerate; only the first node
     * is returned
     *
     * @param data
     * @return
     */
    public HypNode getNode(String data)
    {
        HypNode foundNode = null;
        int i = 0;
        while(i < _rootBranches.size() && foundNode == null){
            foundNode = getNode(_rootBranches.get(i), data);
            i++;
        }
        return foundNode;
    }

    /**Returns the node idenfied by the given data,
     * recursively searching (DFS) starting at node
     *
     * @param node - The starting point of the search
     * @param data - The data to find (synset)
     * @return     - The node with the specified data
     */
    private HypNode getNode(HypNode node, ISynset data)
    {
        if(node._data.equals(data))
            return node;
        int childIdx = 0;
        HypNode foundNode = null;
        while(childIdx < node._children.size() && foundNode == null) {
            foundNode = getNode(node._children.get(childIdx), data);
            childIdx++;
        }
        return foundNode;
    }

    /**Returns the node idenfied by the given data,
     * recursively searching (DFS) starting at node
     *
     * @param node - The starting point of the search
     * @param data - The data to find (string)
     * @return     - The node with the specified data
     */
    private HypNode getNode(HypNode node, String data)
    {
        if(node.toString().equals(data))
            return node;
        int childIdx = 0;
        HypNode foundNode = null;
        while(childIdx < node._children.size() && foundNode == null) {
            foundNode = getNode(node._children.get(childIdx), data);
            childIdx++;
        }
        return foundNode;
    }

    /**Returns whether this HypTree contains the given data
     *
     * @param data - The data to search for
     * @return     - Whether data is one of the nodes
     */
    public boolean contains(ISynset data)
    {
        boolean foundNode = false;
        int i = 0;
        while(i < _rootBranches.size() && !foundNode){
            foundNode = contains(_rootBranches.get(i), data);
            i++;
        }
        return foundNode;
    }

    /**Returns whether this HypTree contains the given data
     * WARNING: multiple synsets may have the same string
     *          representation; this contains may overgenerate
     *
     * @param data - The data to search for
     * @return     - Whether data is one of the nodes
     */
    public boolean contains(String data)
    {
        boolean foundNode = false;
        int i = 0;
        while(i < _rootBranches.size() && !foundNode){
            foundNode = contains(_rootBranches.get(i), data);
            i++;
        }
        return foundNode;
    }

    /**Performs depth-first-search on the tree to find the
     * specified data
     *
     * @param node - The current node
     * @param data - The data to find (synset)
     * @return
     */
    private boolean contains(HypNode node, ISynset data)
    {
        //Base case: this node is the data, return true
        if(node._data.equals(data))
            return true;

        //iterate through the children via DFS, stop
        //if we've found the data
        int childIdx = 0;
        boolean foundData = false;
        while(childIdx < node._children.size() && !foundData) {
            foundData = contains(node._children.get(childIdx), data);
            childIdx++;
        }
        return foundData;
    }

    /**Performs depth-first-search on the tree to find the
     * specified data
     *
     * @param node - The current node
     * @param data - The data to find (string)
     * @return
     */
    private boolean contains(HypNode node, String data)
    {
        //Base case: this node is the data, return true
        if(node.toString().equals(data))
            return true;

        //iterate through the children via DFS, stop
        //if we've found the data
        int childIdx = 0;
        boolean foundData = false;
        while(childIdx < node._children.size() && !foundData) {
            foundData = contains(node._children.get(childIdx), data);
            childIdx++;
        }
        return foundData;
    }

    /**Prints this hypernym tree with appropriate indentation
     * and frequency counts
     */
    public void prettyPrint()
    {
        System.out.println(_rootLemma);
        for(HypNode rootBranch : _rootBranches)
            prettyPrint(rootBranch, 1);
    }

    /**Prints this hypernym tree recursively, padding
     * a node's text with 2 * depth spaces on the left
     *
     * @param node  - The node to print at this level
     * @param depth - The measure of indentation to use
     */
    private void prettyPrint(HypNode node, int depth)
    {
        for(int i=0; i<depth; i++)
            System.out.print("  ");
        System.out.println(node.toString() + " (" + node._tagCount + ")");
        for(HypNode child : node._children)
            prettyPrint(child, depth+1);
    }

    /**Hypernym Nodes wrap synset data
     * with tag counts, as well as
     * storing tree traversal info (depth,
     * parent, children)
     */
    public static class HypNode
    {
        private ISynset _data;
        private int _depth;
        private HypNode _parent;
        private List<HypNode> _children;
        private int _tagCount;

        /**Builds a HypNode from the given data,
         * with the given parent and tag count
         *
         * @param data
         * @param parent
         * @param tagCount
         */
        public HypNode(ISynset data, HypNode parent, int tagCount)
        {
            _data = data;
            _parent = parent;
            if(_parent == null)
                _depth = 0;
            else
                _depth = _parent._depth + 1;
            _children = new ArrayList<>();
            _tagCount = tagCount;
        }

        /**Returns the tag count for this node
         *
         * @return
         */
        public int getTagCount(){return _tagCount;}

        /**Returns the first word associated with this synset
         *
         * @return
         */
        @Override
        public String toString()
        {
            return _data.getWords().get(0).getLemma().toLowerCase();
        }
    }
}