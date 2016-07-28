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
    private Map<String, Chain> _chainDict;
    private List<Caption> _captionList;

    public int height;
    public int width;
    public int crossVal;
    public boolean reviewed;

    /**Document constructor used for loading Documents from
     * a pair of sentence / annotation files
     *
     * @param sentenceFilename
     * @param annotationFilename
     */
    public Document(String sentenceFilename, String annotationFilename)
    {
        _ID = StringUtil.getFilenameFromPath(sentenceFilename) + ".jpg";
        crossVal = -1;
        reviewed = false;
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
        reviewed = false;
        crossVal = -1;

        //initialize our chains, given the captions
        //and their internal mentions
        initChains();
    }

    /**Document constructor primarily used for loading
     * Documents from a database
     *
     * @param ID
     */
    public Document(String ID)
    {
        _ID = ID;
        _chainDict = new HashMap<>();
        _captionList = new ArrayList<>();
        reviewed = false;
        crossVal = -1;
    }

    /**Initializes the set of chains from the mentions in
     * the caption list
     */
    private void initChains()
    {
        _chainDict = new HashMap<>();
        for(Caption c : _captionList){
            for(Mention m : c.getMentionList()){
                String chainID = m.getChainID();

                //ignore mentions that don't have chain IDs or
                //are mapped to 0 (nonvis)
                if(chainID != null && !chainID.equals("0")){
                    if(!_chainDict.containsKey(chainID))
                        _chainDict.put(chainID, new Chain(_ID, chainID));
                    _chainDict.get(chainID).addMention(m);
                }
            }
        }
    }

    /* Getters */
    public String getID(){return _ID;}
    public Set<Chain> getChainSet(){return new HashSet<>(_chainDict.values());}
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

    /**Returns the Caption with the given index; null
     * if the Caption was not found
     *
     * @param idx
     * @return
     */
    public Caption getCaption(int idx)
    {
        //check the given idx, in case we can
        //directly pull the caption from the list
        if(idx < _captionList.size())
            if(_captionList.get(idx).getIdx() == idx)
                return _captionList.get(idx);

        //search the caption list for one
        //with that idx
        for(Caption c : _captionList)
            if(c.getIdx() == idx)
                return c;
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
        _chainDict.values().forEach(c -> boxSet.addAll(c.getBoundingBoxSet()));
        return boxSet;
    }

    /**Returns a list of coref strings for this document's captions,
     * given the set of predicted chains
     *
     * @param predChainSet
     * @return
     */
    public List<String> getPredictedCorefStrings(Set<Chain> predChainSet)
    {
        //create a mapping of [captionIdx -> [tokenIdx -> predChainID]]
        Map<Integer, Map<Integer, String>> capTokenChainDict = new HashMap<>();
        for(Chain c : predChainSet){
            for(Mention m : c.getMentionSet()){
                int captionIdx = m.getCaptionIdx();
                if(!capTokenChainDict.containsKey(captionIdx))
                    capTokenChainDict.put(captionIdx, new HashMap<>());

                for(Token t : m.getTokenList())
                    capTokenChainDict.get(captionIdx).put(t.getIdx(), c.getID());
            }
        }

        //get predicted coref strings for this main.java.document with this chain set
        List<String> captionCorefStrList = new ArrayList<>();
        for(Caption c : _captionList)
            captionCorefStrList.add(c.toCorefString(capTokenChainDict.get(c.getIdx())));
        return captionCorefStrList;
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
        for(Chain c : _chainDict.values())
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
        for(Chain c : _chainDict.values())
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
        for(Chain c : _chainDict.values())
            if(c.getBoundingBoxSet().contains(b))
                return c.getMentionSet();
        return null;
    }

    /**Adds the given bounding box to all the specified chain IDs
     *
     * @param b
     * @param assocChainIDs
     */
    public void addBoundingBox(BoundingBox b, Collection<String> assocChainIDs)
    {
        for(String chainID : assocChainIDs){
            for(Chain c :_chainDict.values())
                if(c.getID().equals(chainID))
                    c.addBoundingBox(b);
        }
    }

    /**Adds the given Caption to the internal list
     * in the position determined by caption index;
     * originally written for use in creating
     * Documents from a database
     *
     * @param c
     */
    public void addCaption(Caption c)
    {
        int insertionIdx = Annotation.getInsertionIdx(_captionList, c);
        _captionList.add(insertionIdx, c);
    }

    /**Adds the given Chain to the Document;
     * originally intended for use when building
     * Documents from a database
     *
     * @param c
     */
    public void addChain(Chain c)
    {
        _chainDict.put(c.getID(), c);
    }

    /**Adds the given Mention to its owning Chain
     *
     * @param m
     */
    public void addMentionToChain(Mention m)
    {
        //add the mention to its chain
        _chainDict.get(m.getChainID()).addMention(m);
    }

    /**Sets the chains with the given chain IDs as scene chains;
     * originally written to ease interfacing with bounding box XML files
     *
     * @param chainIDs
     */
    public void setSceneChains(Collection<String> chainIDs)
    {
        for(String chainID : chainIDs)
            for(Chain c : _chainDict.values())
                if(c.getID().equals(chainID))
                    c.isScene = true;
    }

    /**Sets the chains with the given chain IDs as an original nobox chains;
     * originally written to ease interfacing with bounding box XML files
     *
     * @param chainIDs
     */
    public void setOrigNoboxChains(Collection<String> chainIDs)
    {
        for(String chainID : chainIDs)
            for(Chain c : _chainDict.values())
                if(c.getID().equals(chainID))
                    c.isOrigNobox = true;
    }

    /**Merges this document with the given document, d, where
     * d contains bounding boxes assigned to chains with the
     * same IDs as this; originally implemented for merging
     * docs based on .coref and Flickr30kEntities files
     *
     * @param d
     */
    public void loadBoxesFromDocument(Document d)
    {
        //get the dimension data from this document
        height = d.height;
        width = d.width;

        //grab the boxes from d and add them to our chains
        for(Chain c : d.getChainSet()){
            for(BoundingBox b : c.getBoundingBoxSet())
                _chainDict.get(c.getID()).addBoundingBox(b);

            //Since the box annotations also contain the scene flag,
            //add that here as well
            _chainDict.get(c.getID()).isScene = c.isScene;
            _chainDict.get(c.getID()).isOrigNobox = c.isOrigNobox;
        }
    }

    /**Returns a list of strings representing this Document in the
     * CONLL 2012 format, the specifications for which can be found [here][link]
     *
     * [link]: http://conll.cemantix.org/2012/data.html
     *
     * @return
     */
    public List<String> toConll2012()
    {
        Set<Chain> chainSet = new HashSet<>(_chainDict.values());
        chainSet.remove(_chainDict.get("0"));
        return Document.toConll2012(this, chainSet);
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

    /**Returns a list of strings representing the given Document -- treating
     * the given chainSet as the source of coreferent information -- in the
     * CONLL 2012 format, the specifications for which can be found [here][link]
     *
     * [link]: http://conll.cemantix.org/2012/data.html
     *
     * @param d
     * @param chainSet
     * @return
     */
    public static List<String> toConll2012(Document d, Collection<Chain> chainSet)
    {
        //Associate mention tokens with the chains to which they belong
        Map<Token, String> tokenChainDict_start = new HashMap<>();
        Map<Token, String> tokenChainDict_end = new HashMap<>();
        for(Chain c : chainSet){
            for(Mention m : c.getMentionSet()){
                tokenChainDict_start.put(m.getTokenList().get(0), c.getID());
                tokenChainDict_end.put(m.getTokenList().get(m.getTokenList().size()-1), c.getID());
            }
        }

        //Iterate through the Document's tokens, adding lines to the set
        List<String> lineList = new ArrayList<>();
        int tokenIdx = 0;
        for(Caption c : d.getCaptionList()){
            for(Token t : c.getTokenList()){
                StringBuilder sb = new StringBuilder();
                sb.append(d.getID());   //Document ID
                sb.append("\t");
                sb.append("0");         //Part number
                sb.append("\t");
                sb.append(tokenIdx);    //Word number
                sb.append("\t");
                sb.append(t.getText()); //the word itself
                sb.append("\t");
                sb.append(t.getPosTag());   //part of speech
                sb.append("\t");
                sb.append("-");         //parse bit
                sb.append("\t");
                sb.append("-");         //predicate lemma
                sb.append("\t");
                sb.append("-");         //predicate frameset ID
                sb.append("\t");
                sb.append("-");         //word sense
                sb.append("\t");
                sb.append("-");         //speaker / author
                sb.append("\t");
                sb.append("-");         //named entities
                sb.append("\t");
                sb.append("-");         //predicate arguments
                sb.append("\t");
                //append the coref information as the final column according to
                //a) If this is a start token: (chain
                //b) If this is an end token: chain)
                //c) If this is a start _and_ end token: (chain)
                //d) else: -
                if(tokenChainDict_start.containsKey(t) && tokenChainDict_end.containsKey(t)) {
                    sb.append("(");
                    sb.append(tokenChainDict_start.get(t));
                    sb.append(")");
                }else if(tokenChainDict_start.containsKey(t)){
                    sb.append("(");
                    sb.append(tokenChainDict_start.get(t));
                } else if(tokenChainDict_end.containsKey(t)){
                    sb.append(tokenChainDict_end.get(t));
                    sb.append(")");
                } else {
                    sb.append("-");
                }
                lineList.add(sb.toString());
                tokenIdx++;
            }
        }
        return lineList;
    }
}
