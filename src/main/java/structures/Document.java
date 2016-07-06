package structures;

import utilities.FileIO;
import utilities.Logger;
import utilities.StringUtil;
import utilities.XmlIO;

import java.awt.*;
import java.util.*;
import java.util.List;

/**In the image caption setting, a Document consists of
 * a set of captions and (optionally) a set of boudning
 * boxes referring to image regions
 *
 * @author ccervantes
 */
public class Document
{
    private String _ID;
    private Set<Chain> _chainSet;
    private List<Caption> _captionList;

    public int height;
    public int width;
    public int crossVal;

    /**Document constructor used for loading Documents from
     * a pair of sentence / annotation files
     *
     * @param sentenceFilename
     * @param annotationFilename
     */
    public Document(String sentenceFilename, String annotationFilename)
    {
        _ID = StringUtil.getFilenameFromPath(sentenceFilename);
        crossVal = -1;
        _captionList = new ArrayList<>();

        //Load the sentence files into Captions
        List<String> capStrList =
                FileIO.readFile_lineList(sentenceFilename);
        try{
            for(int i=0; i<capStrList.size(); i++)
                _captionList.add(Caption.fromEntitiesStr(_ID, i, capStrList.get(i)));
        }catch (Exception ex){
            Logger.log(ex);
        }

        //now that we have captions (with mentions), initialize
        //our chains
        initChains();

        //Load bounding box information from the annotation file
        XmlIO.readBoundingBoxFile(annotationFilename, this);
    }

    /**Document constructor used for loading Documents from
     * a set of coref strings
     *
     * @param corefStrings
     */
    public Document(Set<String> corefStrings)
    {
        _captionList = new ArrayList<>();
        try{
            for(String corefStr : corefStrings)
                _captionList.add(Caption.fromCorefStr(corefStr));
        } catch(Exception ex){
            Logger.log(ex);
        }
        _ID = _captionList.get(0).getDocID();

        //initialize our chains, given the captions
        //and their internal mentions
        initChains();
    }

    /**Initializes the set of chains from the mentions in
     * the caption list
     */
    private void initChains()
    {
        Map<String, Chain> chainDict = new HashMap<>();
        for(Caption c : _captionList){
            for(Mention m : c.getMentionList()){
                String chainID = m.getChainID();

                //ignore mentions that don't have chain IDs or
                //are mapped to 0 (nonvis)
                if(chainID != null && !chainID.equals("0")){
                    if(!chainDict.containsKey(chainID))
                        chainDict.put(chainID, new Chain(_ID, chainID));
                    chainDict.get(chainID).addMention(m);
                }
            }
        }
        _chainSet = new HashSet<>(chainDict.values());
    }

    /* Getters */
    public String getID(){return _ID;}
    public Set<Chain> getChainSet(){return _chainSet;}
    public List<Caption> getCaptionList(){return _captionList;}
    public boolean getIsTest(){ return crossVal == 2;}
    public boolean getIsTrain(){ return crossVal == 1;}
    public boolean getIsDev(){ return crossVal == 0;}
    public int getCrossVal(){return crossVal;}

    /**Returns the list of mentions in this Document, ordered by
     * their caption and the order within their caption
     *
     * @return
     */
    public List<Mention> getMentionList()
    {
        List<Mention> mentionList = new ArrayList<>();
        _captionList.forEach(c -> mentionList.addAll(c.getMentionList()));
        return mentionList;
    }

    /**Adds the given bounding box to all the specified chain IDs
     *
     * @param b
     * @param assocChainIDs
     */
    public void addBoundingBox(BoundingBox b, Collection<String> assocChainIDs)
    {
        for(String chainID : assocChainIDs){
            for(Chain c : _chainSet)
                if(c.getID().equals(chainID))
                    c.addBoundingBox(b);
        }
    }

    /**Sets the chain with the given chain IDs as scene chains;
     * originally written to ease interfacing with bounding box XML files
     *
     * @param chainIDs
     */
    public void setSceneChain(Collection<String> chainIDs)
    {
        for(String chainID : chainIDs){
            for(Chain c : _chainSet)
                if(c.getID().equals(chainID))
                    c.isScene = true;
        }
    }

    /**Returns the area of this image
     *
     * @return - The area of this image
     */
    public double getArea()
    {
        return width * height;
    }

    /**Returns the percentage of the image that is covered
     * by bounding boxes
     *
     * @return - The percentage (0-1) of the image
     * 			 covered by bounding boxes
     */
    public double getBoxCoveragePercentage()
    {
        //first, let's build a mapping of pixels that
        //occur in our boxes
        Set<Rectangle> recSet = new HashSet<>();
        for(Chain c : _chainSet)
            for(BoundingBox b : c.getBoundingBoxSet())
                recSet.add(b.getRec());

        //and now let's go through our image, checking
        //whether each point is covered
        Set<Point> pointSet = new HashSet<>();
        for(int x = 0; x <= width; x++) {
            for(int y = 0; y <= height; y++) {
                Point p = new Point(x,y);
                pointSet.add(p);
            }
        }

        int totalPoints = pointSet.size();
        int coveredPoints = 0;
        for(Point p : pointSet) {
            for(Rectangle r : recSet) {
                if(r.contains(p)) {
                    coveredPoints++;
                    break;
                }
            }
        }

        return (double)coveredPoints / (double)totalPoints;
    }

    /**Returns the set of bounding boxes associated with
     * the given mention; null if no boxes are associated
     *
     * @param m
     * @return
     */
    public Set<BoundingBox> getBoxSetForMention(Mention m)
    {
        for(Chain c : _chainSet)
            if(c.getMentionSet().contains(m))
                return c.getBoundingBoxSet();
        return null;
    }

    /**Returns the set of mentions associated with
     * the given bounding box; null if no mentions are associated
     *
     * @param b
     * @return
     */
    public Set<Mention> getMentionSetForBox(BoundingBox b)
    {
        Set<Mention> mentionSet = new HashSet<>();
        for(Chain c : _chainSet)
            if(c.getBoundingBoxSet().contains(b))
                return c.getMentionSet();
        return null;
    }

    /**Returns the complete set of bounding boxes for this
     * image
     *
     * @return - The set of BoundingBox objects
     */
    public Set<BoundingBox> getBoundingBoxSet()
    {
        Set<BoundingBox> boxSet = new HashSet<>();
        _chainSet.forEach(c -> boxSet.addAll(c.getBoundingBoxSet()));
        return boxSet;
    }

    /**Returns the key-value string for this mention pair,
     * where order is determined by mention precedence (cap:i;mention:j
     * always appears before cap:m;mention:n, where i<=m and j<=n)
     *
     * @param m            - Mention in pair
     * @param mPrime       - Mention in pair
     * @param includeDocID - Whether to include the document _ID in the
     *                       mention pair string
     * @return             - Mention pair keyvalue string, in the form
     *                       [doc:docID];caption_1:capIdx_1;mention_1:mIdx_1;caption_2:capIdx_2;mention_2:mIdx_2
     */
    public static String getMentionPairStr(Mention m, Mention mPrime,
                                           boolean includeDocID)
    {
        //determine mention precedence
        Mention m1, m2;
        if(m.getCaptionIdx() < mPrime.getCaptionIdx()){
            m1 = m;
            m2 = mPrime;
        } else if(mPrime.getCaptionIdx() < m.getCaptionIdx()){
            m1 = mPrime;
            m2 = m;
        } else {
            if(m.getIdx() < mPrime.getIdx()){
                m1 = m;
                m2 = mPrime;
            } else {
                m1 = mPrime;
                m2 = m;
            }
        }

        //get our keyval string for this mention pair
        List<String> keys = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        if(includeDocID){
            keys.add("doc");
            vals.add(m1.getDocID());
        }
        keys.addAll(Arrays.asList(
                new String[]{"caption_1", "mention_1",
                             "caption_2", "mention_2"}));
        vals.add(m1.getCaptionIdx());
        vals.add(m1.getIdx());
        vals.add(m2.getCaptionIdx());
        vals.add(m2.getIdx());
        return StringUtil.toKeyValStr(keys, vals);
    }
}
