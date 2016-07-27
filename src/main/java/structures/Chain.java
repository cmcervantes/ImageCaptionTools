package structures;

import utilities.StringUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**Chains are collections of coreferent Mentions and
 * are associated with BoundingBoxes
 *
 * @author ccervantes
 */
public class Chain extends Annotation
{
    private String _ID;
    private Set<Mention> _mentionSet;
    private Set<BoundingBox> _boxSet;
    public boolean isScene;
    public boolean isOrigNobox;

    /**Default Chain constructor assumes mentions and boxes
     * will be added to the sets after initialization
     *
     * @param docID
     * @param ID
     */
    public Chain(String docID, String ID)
    {
        _docID = docID;
        _ID = ID;
        _mentionSet = new HashSet<>();
        _boxSet = new HashSet<>();
        isScene = false;
        isOrigNobox = false;
    }

    /**Adds the given Mention to the chain's Mention set
     *
     * @param m
     */
    public void addMention(Mention m)
    {
        _mentionSet.add(m);
    }

    /**Adds the given BoundingBox to this chain's set
     *
     * @param b - The BoundingBox to add
     */
    public void addBoundingBox(BoundingBox b)
    {
        _boxSet.add(b);
    }

    /**Getters*/
    public String getID(){return _ID;}
    public Set<Mention> getMentionSet(){return _mentionSet;}
    public Set<BoundingBox> getBoundingBoxSet(){return _boxSet;}
    public boolean hasBoundingBoxes(){return !_boxSet.isEmpty();}

    /**Returns the unified Cardinality of the mentions
     * in this chain
     *
     * @return
     */
    public Cardinality getUnifiedCardinality()
    {
        List<Mention> mentionList = new ArrayList<>(_mentionSet);
        Cardinality card = mentionList.get(0).getCardinality();
        for(int i=1; i<mentionList.size(); i++){
            Cardinality c = mentionList.get(i).getCardinality();
            card = Cardinality.unify(card, c);
        }
        return card;
    }

    /**Returns whether all the mentions in this chain
     * have approximately equal Cardinalities (the mentions
     * in this chain can refer to the same number of elements)
     *
     * @return
     */
    public boolean hasUnifiableCardinalities()
    {
        boolean canUnify = true;
        List<Mention> mentionList = new ArrayList<>(_mentionSet);
        for(int i=0; i<mentionList.size(); i++){
            for(int j=i+1; j<mentionList.size(); j++){
                canUnify &= Cardinality.approxEqual(mentionList.get(i).getCardinality(), mentionList.get(j).getCardinality());
            }
        }
        return canUnify;
    }

    /**Returns this chain's attributes as a key:value; string
     *
     * @return  - key-value string of chain attributes
     */
    @Override
    public String toDebugString()
    {
        String[] keys = {"docID", "ID", "numMentions",
                         "numBoxes", "isScene", "isOrigNobox"};
        Object[] vals = {_docID, _ID, _mentionSet.size(),
                         _boxSet.size(), isScene, isOrigNobox};
        return StringUtil.toKeyValStr(keys, vals);
    }

    /**Returns a dataset-unique ID for this chain, in the form
     * docID;chain:ID
     *
     * @return
     */
    public String getUniqueID()
    {
        return _docID + ";chain:" + _ID;
    }
}
