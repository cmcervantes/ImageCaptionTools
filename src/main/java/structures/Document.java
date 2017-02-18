package structures;

import utilities.*;

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
        String chainID = m.getChainID();
        if(_chainDict.containsKey(chainID))
            return _chainDict.get(chainID).getBoundingBoxSet();
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

    private boolean getBoxesAreSubset(Chain c1, Chain c2)
    {
        Set<BoundingBox> boxes_1 = c1.getBoundingBoxSet();
        Set<BoundingBox> boxes_2 = c2.getBoundingBoxSet();
        Set<BoundingBox> intersect = new HashSet<>(boxes_1);
        intersect.retainAll(boxes_2);
        return !intersect.isEmpty() &&
               boxes_1.size() == intersect.size() &&
               boxes_2.size() > intersect.size();
    }

    /**Returns the set of mentions associated with
     * the given bounding box; null if no mentions are associated
     *
     * @param b
     * @return
     */
    public Set<Mention> getMentionSetForBox(BoundingBox b)
    {
        Set<Mention> mentions = new HashSet<>();
        for(Chain c : _chainDict.values())
            if(c.getBoundingBoxSet().contains(b))
                mentions.addAll(c.getMentionSet());
        return mentions;
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

    /**Returns a set of chain pairs that are in
     * the subset relation
     *
     * @return
     */
    public Set<Chain[]> getSubsetChains()
    {
        Set<Chain[]> subsets = new HashSet<>();
        subsets.addAll(_getSubsetMentions_boxes());

        //where there are conflicts, we defer to the box labeling over the heuristic
        for(Chain[] pair :_getSubsetMentions_heuristic())
            if(!Util.containsArr(subsets, pair) &&
                    !Util.containsArr(subsets, new Chain[]{pair[1], pair[0]}))
                subsets.add(pair);

        //enforce transitivity in our decisions
        _enforceSubsetTransitivity(subsets);

        return subsets;
    }

    /**Returns the set of subset pairs (as unique ID strings),
     * for this Document; superset pairs can be inferred by
     * reversing each of the pairs
     *
     * @return
     */
    public Set<String> getSubsetMentions()
    {
        Set<Chain[]> subsetChains = getSubsetChains();

        Set<String> subsetPairIDs = new HashSet<>();
        for(Chain[] pair : subsetChains)
            for(Mention m_sub : pair[0].getMentionSet())
                for(Mention m_sup : pair[1].getMentionSet())
                    subsetPairIDs.add(getMentionPairStr(m_sub, m_sup));
        return subsetPairIDs;
    }

    /**Returns pairs of subset mentions (ordered as sub,sup),
     * excluding nonvisual and coreferent mentions
     *
     * @return
     */
    private Set<Chain[]> _getSubsetMentions_heuristic()
    {
        Set<Chain[]> subsets = new HashSet<>();
        for(Caption c : _captionList){
            Set<Mention[]> subsetsToAdd = new HashSet<>();

            //if for some reason this caption has no mentions, just continue
            if(c.getMentionList().isEmpty())
                continue;

            //grab the first mention
            Mention m0 = c.getMentionList().get(0);

            //Find all mentions in this caption such that
            // a) the mention is X in an XofY
            // b) the Y is coreferent with the first mention
            //Typically there will only be one of these per caption,
            //but this is not guaranteed
            for(int i=2; i<c.getMentionList().size(); i++){
                Mention m = c.getMentionList().get(i-1);
                Mention mPrime = c.getMentionList().get(i);
                List<Token> inters = c.getInterstitialTokens(m, mPrime);

                //if this is an XofY and Y is coreferent with the first mention,
                //add it to the set
                if(inters.size() == 1 && inters.get(0).toString().equals("of") &&
                   mPrime.getChainID().equals(m0.getChainID())){
                    subsetsToAdd.add(new Mention[]{m, mPrime});
                    subsetsToAdd.add(new Mention[]{m, m0});
                }
            }

            //Determine if this caption's chunk string (including extra-chunk tokens)
            //matches the appositive but _not_ the list pattern (since they overlap)
            if(c.toChunkTypeString(true).matches(PTRN_APPOS) &&
               !c.toChunkTypeString(true).matches(PTRN_LIST)){
                //If we have a match, grab the first NP and VP
                Chunk firstNP = m0.getChunkList().get(m0.getChunkList().size() - 1);
                Chunk firstVP = null;
                for(Chunk ch : c.getChunkList()){
                    if(ch.getChunkType().equals("VP")){
                        firstVP = ch;
                        break;
                    }
                }

                //If we have an NP/VP, get the interstitial chunks
                if(firstNP != null && firstVP != null){
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
                            //  a) have matching lexical types or
                            //  b) either mention is pronominal
                            if(Mention.getLexicalTypeMatch(m0, m) > 0 ||
                               m0.getPronounType() != Mention.PRONOUN_TYPE.NONE ||
                               m.getPronounType() != Mention.PRONOUN_TYPE.NONE) {
                                subsetMentions.add(c.getMentionList().get(mIdx));
                            }
                        }
                    }
                    if(!subsetMentions.isEmpty()){
                        for(Mention m : subsetMentions)
                            subsetsToAdd.add(new Mention[]{m, m0});
                    }
                }
            }

            //We do not add subset links where either mention is in our
            //set of nonvisuals or if the link is in our set of coref links
            for(Mention[] pair : subsetsToAdd){
                Mention sub = pair[0], sup = pair[1];
                if(sub.getChainID().equals("0") || sup.getChainID().equals("0"))
                    continue;
                if(sub.getChainID().equals(sup.getChainID()))
                    continue;
                subsets.add(new Chain[]{_chainDict.get(sub.getChainID()),
                        _chainDict.get(sup.getChainID())});
            }
        }
        return subsets;
    }

    /**Returns pairs of subset mentions (ordered as sub,sup),
     * excluding gold nonvis and gold coref; intended to
     * retrieve gold subset links
     *
     * @return
     */
    private Set<Chain[]> _getSubsetMentions_boxes()
    {
        Set<Chain[]> subsetChains = new HashSet<>();
        List<Chain> chainList = new ArrayList<>(_chainDict.values());
        for(int i=0; i<chainList.size(); i++){
            Chain chain_i = chainList.get(i);
            for(int j=i+1; j<chainList.size(); j++){
                Chain chain_j = chainList.get(j);

                if(getBoxesAreSubset(chain_i, chain_j))
                    subsetChains.add(new Chain[]{chain_i, chain_j});
                else if(getBoxesAreSubset(chain_j, chain_i))
                    subsetChains.add(new Chain[]{chain_j, chain_i});
                else {
                    boolean containsPeople_i = false, containsPeople_j = false;
                    for(Mention m : chain_i.getMentionSet())
                        containsPeople_i |= m.getLexicalType().contains("people");
                    for(Mention m : chain_j.getMentionSet())
                        containsPeople_j |= m.getLexicalType().contains("people");

                    if(!containsPeople_i || !containsPeople_j)
                        continue;

                    Set<BoundingBox> boxes_i = chain_i.getBoundingBoxSet();
                    Set<BoundingBox> boxes_j = chain_j.getBoundingBoxSet();
                    if(!boxes_i.isEmpty() && !boxes_j.isEmpty()){
                        boolean ij_ordering = boxes_i.size() < boxes_j.size();

                        Set<BoundingBox> boxes_1, boxes_2;
                        if(ij_ordering){
                            boxes_1 = boxes_i; boxes_2 = boxes_j;
                        } else {
                            boxes_1 = boxes_j; boxes_2 = boxes_i;
                        }

                        boolean coveredallBoxes = true;
                        for(BoundingBox b_1 : boxes_1) {
                            if(!boxes_2.contains(b_1)){
                                boolean foundMatch = false;
                                for(BoundingBox b_2 : boxes_2)
                                    if(BoundingBox.IOU(b_1, b_2) > 0.9)
                                        foundMatch = true;
                                if(!foundMatch)
                                    coveredallBoxes = false;
                            }
                        }

                        if(coveredallBoxes) {
                            if(ij_ordering) {
                                subsetChains.add(new Chain[]{chain_i, chain_j});
                            } else {
                                subsetChains.add(new Chain[]{chain_j, chain_i});
                            }
                        }
                    }
                }
            }
        }
        return subsetChains;


        /*
        Set<Mention[]> subsets = new HashSet<>();
        List<Mention> mentionList = getMentionList();
        for(int i=0; i<mentionList.size(); i++){
            Mention m_i = mentionList.get(i);

            //Skip non-pronominal nonvisuals in train and
            //nonvis everywhere else
            if(m_i.getChainID().equals("0"))
                continue;

            for(int j=i+1; j<mentionList.size(); j++){
                Mention m_j = mentionList.get(j);
                if(m_j.getChainID().equals("0"))
                    continue;

                //skip coreferent pairs
                if(m_i.getChainID().equals(m_j.getChainID()))
                    continue;

                //skip non-pronominal heterogeneously typed pairs
                if(m_i.getPronounType() == Mention.PRONOUN_TYPE.NONE &&
                   m_j.getPronounType() == Mention.PRONOUN_TYPE.NONE &&
                   Mention.getLexicalTypeMatch(m_i, m_j) == 0)
                    continue;


                //If our boxes are in a proper subset relationship,
                //add them to the set
                if(getBoxesAreSubset(m_i, m_j))
                    subsets.add(new Mention[]{m_i, m_j});
                else if(getBoxesAreSubset(m_j, m_i))
                    subsets.add(new Mention[]{m_j, m_i});
                else {
                    //If these mentions aren't in a subset relation,
                    //check if IOU accounts for the mismatch (people only)
                    if(m_i.getLexicalType().contains("people") &&
                       m_j.getLexicalType().contains("people")){
                        Set<BoundingBox> boxes_i = getBoxSetForMention(m_i);
                        Set<BoundingBox> boxes_j = getBoxSetForMention(m_j);
                        if(!boxes_i.isEmpty() && !boxes_j.isEmpty()){
                            boolean ij_ordering = boxes_i.size() < boxes_j.size();

                            Set<BoundingBox> boxes_1, boxes_2;
                            if(ij_ordering){
                                boxes_1 = boxes_i; boxes_2 = boxes_j;
                            } else {
                                boxes_1 = boxes_j; boxes_2 = boxes_i;
                            }

                            boolean coveredallBoxes = true;
                            for(BoundingBox b_1 : boxes_1) {
                                if(!boxes_2.contains(b_1)){
                                    boolean foundMatch = false;
                                    for(BoundingBox b_2 : boxes_2)
                                        if(BoundingBox.IOU(b_1, b_2) > 0.9)
                                            foundMatch = true;
                                    if(!foundMatch)
                                        coveredallBoxes = false;
                                }
                            }

                            if(coveredallBoxes) {
                                if(ij_ordering)
                                    subsets.add(new Mention[]{m_i, m_j});
                                else
                                    subsets.add(new Mention[]{m_j, m_i});
                            }
                        }
                    }
                }
            }
        }
        return subsets;*/
    }

    /**Cascades the given subset links, enforcing subset transitivity, such that
     * 1) if a sub b and b sub c -> a sub c
     * 2) if a sub b, c sub d, and b coref c -> a sub d
     *
     * @param subsets
     * @return
     */
    private void _enforceSubsetTransitivity(Set<Chain[]> subsets)
    {
        //Enforce transitivity, such that if a sub b and b sub c, then a sub c
        List<Chain[]> subsetList;
        do{
            subsetList = new ArrayList<>(subsets);
            for(int i=0; i<subsetList.size(); i++){
                Chain sub_i = subsetList.get(i)[0];
                Chain sup_i = subsetList.get(i)[1];

                for(int j=i+1; j<subsetList.size(); j++){
                    Chain sub_j = subsetList.get(j)[0];
                    Chain sup_j = subsetList.get(j)[1];

                    Chain[] pairToAdd = null;
                    if(sup_i.equals(sub_j))
                        pairToAdd = new Chain[]{sub_i, sup_j};
                    else if(sup_j.equals(sub_i))
                        pairToAdd = new Chain[]{sub_j, sup_i};

                    if(pairToAdd != null && !Util.containsArr(subsets, pairToAdd))
                        subsets.add(pairToAdd);
                }
            }
        } while(subsets.size() != subsetList.size());

        /*
        List<Mention[]> subsetList;
        do{
            subsetList = new ArrayList<>(subsets);
            for(int i=0; i<subsetList.size(); i++){
                Mention sub_i = subsetList.get(i)[0];
                Mention sup_i = subsetList.get(i)[1];

                for(int j=i+1; j<subsetList.size(); j++){
                    Mention sub_j = subsetList.get(j)[0];
                    Mention sup_j = subsetList.get(j)[1];

                    //In practice we're really either adding a
                    //sub_i -> sup_j link or a sub_j -> sup_i link,
                    //so we can combine appropriately
                    Mention[] pairToAdd = null;
                    if(sup_i.equals(sub_j) || //ij ordering
                       sup_i.getChainID().equals(sub_j.getChainID())) {
                        pairToAdd = new Mention[]{sub_i, sup_j};
                    } else if(sup_j.equals(sub_i) || //ji ordering
                              sub_i.getChainID().equals(sup_j.getChainID())) {
                        pairToAdd = new Mention[]{sub_j, sup_i};
                    }

                    //Only add pairs that aren't already in the set _and_ that do not
                    //appear in the inverse
                    if(pairToAdd != null){
                        if(!Util.containsArr(subsets, pairToAdd) &&
                           !Util.containsArr(subsets, new Mention[]{pairToAdd[1], pairToAdd[0]})){
                            subsets.add(pairToAdd);
                        }
                    }
                }
            }
        } while(subsetList.size() != subsets.size());
        return subsets;*/
    }

    /**Returns the pair of mention objects specified in the
     * given mention pair string
     *
     * @param mentionPairStr
     * @return
     */
    public Mention[] getMentionPairFromStr(String mentionPairStr)
    {
        Map<String, String> idDict = StringUtil.keyValStrToDict(mentionPairStr);
        int capIdx_1 = Integer.parseInt(idDict.get("caption_1"));
        int capIdx_2 = Integer.parseInt(idDict.get("caption_2"));
        int mentionIdx_1 = Integer.parseInt(idDict.get("mention_1"));
        int mentionIdx_2 = Integer.parseInt(idDict.get("mention_2"));
        Mention m1 = _captionList.get(capIdx_1).getMentionList().get(mentionIdx_1);
        Mention m2 = _captionList.get(capIdx_2).getMentionList().get(mentionIdx_2);
        return new Mention[]{m1,m2};
    }

    /**Returns the key-value string for this mention pair,
     * where order is enforced and the document ID is always
     * included (to ensure uniqueness)
     *
     * @param m1            - Mention in pair
     * @param m2            - Mention in pair
     * @return             - Mention pair keyvalue string, in the form
     *                       [doc:docID];caption_1:capIdx_1;mention_1:mIdx_1;caption_2:capIdx_2;mention_2:mIdx_2
     */
    public static String getMentionPairStr(Mention m1, Mention m2)
    {
        List<String> keys = new ArrayList<>();
        List<Object> vals = new ArrayList<>();

        //Add the doc ID
        keys.add("doc");
        vals.add(m1.getDocID());

        //Add the mention indices
        keys.addAll(Arrays.asList("caption_1", "mention_1", "caption_2", "mention_2"));
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
