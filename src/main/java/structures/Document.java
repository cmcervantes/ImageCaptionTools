package structures;

import utilities.FileIO;
import utilities.Logger;
import utilities.StringUtil;
import utilities.XmlIO;

import java.awt.*;
import java.awt.geom.Area;
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
    private static Set<String> _collectives = new HashSet<>(FileIO.readFile_lineList(
            "/shared/projects/Flickr30kEntities_v2/resources/collectiveNouns.txt"));
    private static final String PTRN_APPOS = "^NP , (NP (VP |ADJP |PP |and )*)+,.*$";
    private static final String PTRN_LIST = "^NP , (NP ,?)* and NP.*$";

    private String _ID;
    private Map<String, Chain> _chainDict;
    private List<Caption> _captionList;

    public int height;
    public int width;
    public int crossVal;
    public boolean reviewed;
    public String comments;

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
        int lastCapIdx = -1;
        try{
            for(int i=0; i<capStrList.size(); i++){
                lastCapIdx = i;
                _captionList.add(Caption.fromEntitiesStr(_ID, i, capStrList.get(i)));
            }
        }catch (Exception ex){
            Logger.log("Encountered exception: " + _ID + "#" + lastCapIdx);
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
        _captionList.stream().forEachOrdered(c -> mentionList.addAll(c.getMentionList()));
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

    /**Returns whether -- according to the bounding box data --
     * m1 is a subset of m2
     *
     * @param m1
     * @param m2
     * @return
     */
    public boolean getBoxesAreSubset(Mention m1, Mention m2)
    {
        Set<BoundingBox> boxes1 = getBoxSetForMention(m1);
        Set<BoundingBox> boxes2 = getBoxSetForMention(m2);
        Set<BoundingBox> intersect = new HashSet<>(boxes1);
        intersect.retainAll(boxes2);
        if(boxes1.size() == intersect.size() &&
           boxes2.size() > intersect.size() &&
           !intersect.isEmpty()){
            return true;
        }
        return false;
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
            //It's possible - in the old data - for chains in
            //coref files to not appear in the entities strings;
            //drop them
            if(_chainDict.containsKey(c.getID())){
                for(BoundingBox b : c.getBoundingBoxSet())
                    _chainDict.get(c.getID()).addBoundingBox(b);

                //Since the box annotations also contain the scene flag,
                //add that here as well
                _chainDict.get(c.getID()).isScene = c.isScene;
                _chainDict.get(c.getID()).isOrigNobox = c.isOrigNobox;
            }
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

    /**Returns the set of subset pairs (as unique ID strings),
     * for this Document; superset pairs can be inferred by
     * reversing each of the pairs
     *
     * @return
     */
    public Set<String> getSubsetMentions()
    {
        Set<String> subsets = new HashSet<>();
        List<Mention> mentionList = getMentionList();

        Set<Mention[]> subsets_box = new HashSet<>();
        Set<Mention[]> subsets_heur = new HashSet<>();

        //Get all mention pairs with boxes in the subset relation
        for(int i=0; i<mentionList.size(); i++){
            Mention m_i = mentionList.get(i);
            if(m_i.getChainID().equals("0"))
                continue;

            for(int j=i+1; j<mentionList.size(); j++){
                Mention m_j = mentionList.get(j);

                if(m_j.getChainID().equals("0"))
                    continue;

                if(getBoxesAreSubset(m_i, m_j))
                    subsets_box.add(new Mention[]{m_i, m_j});
                else if(getBoxesAreSubset(m_j, m_i))
                    subsets_box.add(new Mention[]{m_j, m_i});
            }
        }

        //Get all mention pairs in our syntactic structures
        for(Caption c : _captionList){
            Mention xInRelevantXofY = null;
            if(!c.getMentionList().isEmpty()){
                Mention m0 = c.getMentionList().get(0);
                for(int i=2; i<c.getMentionList().size(); i++){
                    Mention m = c.getMentionList().get(i-1);
                    Mention mPrime = c.getMentionList().get(i);
                    if(m0.getChainID().equals(mPrime.getChainID())){
                        List<Token> inters = c.getInterstitialTokens(m, mPrime);
                        if(inters.size() == 1 && inters.get(0).toString().equals("of"))
                            xInRelevantXofY = m;
                    }
                }
            }

            //grab the first mention
            Mention m0 = null;
            if(!c.getMentionList().isEmpty())
                m0 = c.getMentionList().get(0);

            if(c.toChunkTypeString(true).matches(PTRN_APPOS) && !c.toChunkTypeString(true).matches(PTRN_LIST)){
                Chunk firstNP = m0.getChunkList().get(m0.getChunkList().size() - 1);

                //Get everything between the first mention
                //and the first verb chunk
                Chunk firstVP = null;
                for(Chunk ch : c.getChunkList()){
                    if(ch.getChunkType().equals("VP")){
                        firstVP = ch;
                        break;
                    }
                }

                //Get interstitial chunks
                if(firstVP != null){
                    List<Token> interstitial = c.getInterstitialTokens(firstNP, firstVP);
                    //If this span is enclosed by commas, strip them
                    if(interstitial.get(0).toString().equals(","))
                        interstitial.remove(0);
                    int intrst_len = interstitial.size();
                    if(interstitial.get(intrst_len - 1).toString().equals(","))
                        interstitial.remove(intrst_len-1);

                    //split the token list into sublists on commas or 'and'
                    List<List<Token>> phraseList = new ArrayList<>();
                    List<Token> currentPhrase = new ArrayList<>();
                    for(Token t : interstitial){
                        if(t.toString().equals("and") || t.toString().equals(",")){
                            if(!currentPhrase.isEmpty())
                                phraseList.add(currentPhrase);
                            currentPhrase = new ArrayList<>();
                        } else {
                            currentPhrase.add(t);
                        }
                    }
                    if(!currentPhrase.isEmpty())
                        phraseList.add(currentPhrase);

                    //for each of these phrases, get the mention corresponding to the
                    //first token
                    Set<Mention> subsetMentions = new HashSet<>();
                    for(List<Token> phrase : phraseList){
                        int mIdx = phrase.get(0).mentionIdx;
                        if(mIdx > -1){
                            Mention m = c.getMentionList().get(mIdx);

                            //Mentions can only be in a subset relation if they
                            //  a) have matching lexical types
                            //  b) the latter mention is a pronoun
                            //  c) the latter mention is a numeral
                            //  d) the latter is some variant of 'other'
                            boolean mIsNum = false;
                            try{
                                Integer.parseInt(m.toString());
                                mIsNum = true;
                            } catch(Exception ex){/*Do nothing*/}
                            if(Mention.getLexicalTypeMatch(m0, m) > 0 ||
                                    m0.getPronounType() != Mention.PRONOUN_TYPE.NONE ||
                                    m.getPronounType() != Mention.PRONOUN_TYPE.NONE ||
                                    mIsNum || m.getHead().getLemma().equals("other")){
                                subsetMentions.add(c.getMentionList().get(mIdx));
                            }
                        }
                    }
                    if(!subsetMentions.isEmpty()){
                        for(Mention m : subsetMentions)
                            subsets_heur.add(new Mention[]{m, m0});
                    }
                }
            } else if(xInRelevantXofY != null){
                //assign subset to mention 0
                subsets_heur.add(new Mention[]{xInRelevantXofY, m0});
            }
        }

        //if we've identified m \subset m' according to the heuristics, then
        //m \subset m'', where m' is coref with m''; similarly
        //mm \subset m' where mm is coref with m
        List<Mention[]> subsets_heur_list = new ArrayList<>(subsets_heur);
        for(Mention[] pair : subsets_heur_list){
            Mention sub = pair[0], sup = pair[1];
            List<Mention> subMentions = new ArrayList<>();
            List<Mention> supMentions = new ArrayList<>();
            for(Mention m : mentionList){
                if(sub.getChainID().equals(m.getChainID()))
                    subMentions.add(m);
                else if(sup.getChainID().equals(m.getChainID()))
                    supMentions.add(m);
            }
            for(Mention subM : subMentions)
                for(Mention supM : supMentions)
                    subsets_heur.add(new Mention[]{subM, supM});
        }

        //Finally, reduce both subset sets to unique IDs, to throw away dups
        for(Mention[] pair : subsets_box)
            subsets.add(Document.getMentionPairStr(pair[0], pair[1], true, true));
        for(Mention[] pair : subsets_heur)
            subsets.add(Document.getMentionPairStr(pair[0], pair[1], true, true));
        return subsets;
    }

    /**Returns the key-value string for this mention pair,
     * where order is determined by mention precedence (cap:i;mention:j
     * always appears before cap:m;mention:n, where i<=m and j<=n)
     *
     * @param m            - Mention in pair
     * @param mPrime       - Mention in pair
     * @param includeDocID - Whether to include the document _ID in the
     *                       mention pair string
     * @param enforceOrder - Returns the IDs in the order they were given, rather
     *                       than by their idx (default)
     * @return             - Mention pair keyvalue string, in the form
     *                       [doc:docID];caption_1:capIdx_1;mention_1:mIdx_1;caption_2:capIdx_2;mention_2:mIdx_2
     */
    public static String getMentionPairStr(Mention m, Mention mPrime,
                                           boolean includeDocID, boolean enforceOrder)
    {
        //determine mention precedence
        Mention m1, m2;
        if(enforceOrder){
            m1 = m;
            m2 = mPrime;
        } else {
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
        }

        //get our keyval string for this mention pair
        List<String> keys = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        if(includeDocID){
            keys.add("doc");
            vals.add(m1.getDocID());
        }
        keys.addAll(Arrays.asList("caption_1", "mention_1",
                "caption_2", "mention_2"));
        vals.add(m1.getCaptionIdx());
        vals.add(m1.getIdx());
        vals.add(m2.getCaptionIdx());
        vals.add(m2.getIdx());
        return StringUtil.toKeyValStr(keys, vals);
    }

    /**Returns whether m1 is a subset of m2, according to
     * the bounding box data and our heuristics; returns
     * 0: mentions are not in a subset rel
     * 1: b1 subset b2
     * 2: b1 = b2 and card(m1) < card(m2)
     * 3: m1 is a collection, m2 isn't, each box in m1 is
     *    covered by area in boxes in m2
     *
     * @param m1
     * @param m2
     * @return
     */
    @Deprecated
    public int getSubsetCase(Mention m1, Mention m2)
    {
        //There is no subset link between coreferent mentions nor between nonvisual mentions
        if(m1.getChainID().equals(m2.getChainID()))
            return 0;
        if(m1.getChainID().equals("0") && m2.getChainID().equals("0"))
            return 0;

        //We cannot know the subset relation between mentions that have no boxes
        Set<BoundingBox> boxes_1 = getBoxSetForMention(m1);
        Set<BoundingBox> boxes_2 = getBoxSetForMention(m2);
        if(boxes_1.isEmpty() || boxes_2.isEmpty())
            return 0;

        //If these boxes are in a proper subset relationship, don't bother
        //checking the other heuristics
        if(getBoxesAreSubset(m1, m2))
            return 1;

        //determine if these mentions are collective
        boolean m1_coll = false;
        String m1_norm = m1.toString().toLowerCase().trim();
        for(String coll : _collectives){
            if(m1_norm.endsWith(coll) || m1_norm.contains(coll + " of ")){
                if(m1.getLexicalType().contains("people") || m1.getLexicalType().contains("animals")){
                    m1_coll = true;
                    break;
                }
            }
        }
        boolean m2_coll = false;
        String m2_norm = m2.toString().toLowerCase().trim();
        for(String coll : _collectives){
            if(m2_norm.endsWith(coll) || m2_norm.contains(coll + " of ")){
                if(m2.getLexicalType().contains("people") || m2.getLexicalType().contains("animals")){
                    m2_coll = true;
                    break;
                }
            }
        }

        //only consider pairs between matching types (or if one is a pronoun
        if(Mention.getLexicalTypeMatch(m1, m2) > 0) {
            /*
            // || m1.getPronounType() != Mention.PRONOUN_TYPE.NONE || m2.getPronounType() != Mention.PRONOUN_TYPE.NONE
            boolean m2_subj_m1_obj = false;
            Caption c1 = getCaption(m1.getCaptionIdx());
            Caption c2 = getCaption(m2.getCaptionIdx());

            //We want to drop candidates for which m2 is the subject of
            //a verb that m1 is the object of, taking coreference into account
            //such that if m2_verb_mX and m1 is coref with mX, the relation holds;
            //Consider
            //  [Two people] look at [a child]
            //  [Folks] with [a kid]
            //'a child' cannot be a subset of 'Two people'; neither can 'a kid'
            if(c1.equals(c2)){
                Chunk subj2 = c1.getSubjectOf(m2);
                if(subj2 != null && subj2.equals(c1.getObjectOf(m1)))
                    m2_subj_m1_obj = true;
            } else {
                Chain ch1 = null, ch2 = null;
                for(Chain ch : getChainSet()){
                    if(ch.getMentionSet().contains(m1))
                        ch1 = ch;
                    else if(ch.getMentionSet().contains(m2))
                        ch2 = ch;
                }
                if(ch1 != null && ch2 != null){
                    Chunk subj2 = c2.getSubjectOf(m2);
                    if(subj2 != null){
                        for(Mention mPrime : ch2.getMentionSet()){
                            if(mPrime.getCaptionIdx() == c2.getIdx())
                                if(subj2.equals(c2.getObjectOf(mPrime)))
                                    m2_subj_m1_obj = true;
                        }
                    }
                }
            }
            */


            //For each box in boxes_1, if we can find a box in
            //boxes_2 for which the IOU exceeds 0.75, this is a subset
            //relation
            /*
            if(boxes_1.size() < boxes_2.size()){
                boolean missingBox = false;
                for(BoundingBox b1 : boxes_1){
                    boolean foundBox = false;
                    for(BoundingBox b2 : boxes_2){
                        if(BoundingBox.IOU(b1, b2) >= 0.75)
                            foundBox = true;
                    }
                    if(!foundBox)
                        missingBox = true;
                }
                if(!missingBox)
                    return 2;
            }*/

            //if(m2_coll && !m1_coll && !m2_subj_m1_obj){
            if(m2_coll && !m1_coll){
                //only consider subsets between m1 and m2 where
                //  a) m2 is collective
                //  b) m1 is _not_ collective
                //  c) the [m2 verb m1] relationship does not hold
                Area a2 = new Area();
                boxes_2.forEach(b -> a2.add(new Area(b.getRec())));
                boolean fullCoverage = true;
                for(BoundingBox b1 : boxes_1)
                    if(!a2.contains(b1.getRec()))
                        fullCoverage = false;
                if(fullCoverage)
                    return 3;
            }
        }

        return 0;
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
        return getMentionPairStr(m, mPrime, includeDocID, false);
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
                sb.append(t.toString()); //the word itself
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
