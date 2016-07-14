package structures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**The DependencyNode class functions as a wrapper for
 * the dependency tree returned by a stanford dependency parser;
 * each node is associated with a token, has a single
 * governing node - with a relation - and has 0 or more
 * dependent nodes
 *
 * Various searching functions assume the use of the root node,
 * as searches proceed through dependents only
 */
public class DependencyNode
{
    private Token _token;
    private List<DependencyNode> _dependents;
    private String _relationToGovernor;
    private DependencyNode _governor;
    private int _depth;

    /**Constructor used for non-root nodes in the tree
     *
     * @param t
     * @param governor
     * @param relationToGov
     * @param depth
     */
    public DependencyNode(Token t, DependencyNode governor,
                          String relationToGov, int depth)
    {
        _token = t;
        _governor = governor;
        _relationToGovernor = relationToGov;
        _depth = depth;
        _dependents = new ArrayList<>();
    }

    /**Constructor for the root node in the tree
     *
     * @param t
     */
    public DependencyNode(Token t)
    {
        _token = t;
        _governor = null;
        _relationToGovernor = "ROOT";
        _dependents = new ArrayList<>();
        _depth = 0;
    }

    /**Adds the given token as a new dependent node (with the given
     * relation) of this node; the position in the
     * dependent node list is defined by the index of t
     *
     * @param t
     * @param relation
     */
    public void addDependent(Token t, String relation)
    {
        DependencyNode dep =
                new DependencyNode(t, this, relation, _depth+1);

        //Add this node in the list based on the idx
        //TODO: See if we can easily fold this into the Annotation binary search
        int idxToAdd = 0;
        for(DependencyNode node : _dependents)
            if(node._token.getIdx() < dep._token.getIdx())
                idxToAdd++;
        _dependents.add(idxToAdd, dep);
    }

    /* Getters */
    public Token getToken(){return _token;}
    public String getRelationToGovernor(){return _relationToGovernor;}
    public List<DependencyNode> getDependents(){return _dependents;}
    public boolean isLeaf(){return _dependents.isEmpty();}
    public int getDepth(){return _depth;}
    public DependencyNode getGovernor(){return _governor;}

    /**Finds the DependencyNode associated with the given token using
     * DFS; returns null if t is not associated with any dependent
     *
      * @param t
     * @return
     */
    public DependencyNode findDependent(Token t)
    {
        int idxToFind = t.getIdx();
        if(_token.getIdx() == idxToFind)
            return this;

        DependencyNode node = null;
        for(DependencyNode child : _dependents){
            if(child._token.getIdx() == idxToFind){
                node = child;
            } else {
                node = child.findDependent(t);
            }
            if(node != null)
                break;
        }
        return node;
    }

    /**Returns a list of nodes associated with the
     * given mention from among the dependents
     * of this node (intended for use at root)
     *
     * @param m
     * @return
     */
    public List<DependencyNode> getNodes(Mention m)
    {
        List<DependencyNode> nodeList = new ArrayList<>();
        for(Token t : m.getTokenList()) {
            DependencyNode n = findDependent(t);
            if(n != null)
                nodeList.add(n);
        }
        return nodeList;
    }

    /**Returns a bottom-up, no-duplicate list of Chunk indices
     * corresponding to the Tokens belonging to the
     * ancestor nodes of this; list begins with
     * immediate governor's corresponding Chunk idx.
     *
     * @return
     */
    public List<Integer> getGoverningChunkIndices()
    {
        List<Integer> chunkIdxList = new ArrayList<>();
        getGoverningChunkIndices(chunkIdxList);
        return chunkIdxList;
    }

    /**Recursively populates the given list with Token's chunk indices;
     * internal helper method for public getGoverningChunkIndices
     *
     * @param idxList
     */
    private void getGoverningChunkIndices(List<Integer> idxList)
    {
        if(_governor != null){
            int chunkIdx = _governor.getToken().chunkIdx;
            if(!idxList.contains(chunkIdx))
                idxList.add(chunkIdx);
            _governor.getGoverningChunkIndices(idxList);
        }
    }

    /**Returns whether the given node is found in
     * the tree for which this node is the root
     *
     * @param node
     * @return
     */
    public boolean hasNodeInTree(DependencyNode node)
    {
        if(this.equals(node))
            return true;

        for(DependencyNode dep : _dependents){
            if(dep.hasNodeInTree(node))
                return true;
        }
        return false;
    }

    /**Returns the set of all nodes in the tree
     * for which this node is a root
     *
     * @return
     */
    public Set<DependencyNode> getAllNodesInTree()
    {
        Set<DependencyNode> depSet = new HashSet<>();
        depSet.add(this);
        for(DependencyNode dep : _dependents)
            depSet.addAll(dep.getAllNodesInTree());
        return depSet;
    }

    /**Returns a set of relations for which tokens belonging
     * to the given mention are the governor and the dependent
     * is a token not belonging to this mention (or vice versa);
     * intended for use on the root node only
     *
     * @param m
     * @return
     */
    public Set<String> getOutRelations(Mention m)
    {
        Set<String> outRelations = new HashSet<>();
        List<DependencyNode> nodeList = getNodes(m);
        for(DependencyNode n : nodeList){
            DependencyNode gov = n.getGovernor();
            if(gov != null && gov.getToken().mentionIdx != m.getIdx())
                outRelations.add(n.getRelationToGovernor());
            for(DependencyNode dep : n.getDependents())
                if(dep.getToken().mentionIdx != m.getIdx())
                    outRelations.add(dep.getRelationToGovernor());
        }
        return outRelations;
    }

    /**Returns a right to left, BFS list of
     * preceding nodes
     *
     * @return
     */
    public List<DependencyNode> getPrecedingNodes()
    {
        List<DependencyNode> precList = new ArrayList<>();
        if(_governor != null)
            precList.addAll(_governor.getPrecedingNodes(this));
        return precList;
    }

    /**Returns a right to left, BFS list of
     * preceding nodes
     *
     * @param currentNode
     * @return
     */
    private List<DependencyNode> getPrecedingNodes(DependencyNode currentNode)
    {
        //get all left siblings to the current node
        List<DependencyNode> precList = new ArrayList<>();
        int currentNodeIdx = _dependents.indexOf(currentNode);
        for(int i=currentNodeIdx-1; i>=0; i--)
            precList.add(_dependents.get(i));

        //go up to the parent level and do the same
        if(currentNode != null){
            precList.add(_governor);
            precList.addAll(_governor.getPrecedingNodes(this));
        }

        //return the list
        return precList;
    }

    /**Prints the tree in a tabbed format
     *
     * FORMAT:
     *    node
     *      |
     *      |--[relation]-> node
     *
     */
    public void prettyPrint()
    {
        this.prettyPrint(0);
    }

    /**Prints the tree in a tabbed format
     *
     * FORMAT:
     *    node
     *      |
     *      |--[relation]-> node
     *
     * @param offset
     */
    private void prettyPrint(int offset)
    {
        if(offset > 0){
            for(int i=0; i<offset; i++)
                System.out.print("  ");
            System.out.print("|--[");
            System.out.print(_relationToGovernor);
            System.out.print("]->");
        }
        System.out.println(_token.toString());
        for(DependencyNode child : _dependents){
            child.prettyPrint(offset+1);
        }
    }
}