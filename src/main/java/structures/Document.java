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
    protected static final String PTRN_APPOS = "^NP , (NP (VP |ADJP |PP |and )*)+,.*$";
    protected static final String PTRN_LIST = "^NP , (NP ,?)* and NP.*$";

    protected String _ID;
    protected Map<String, Chain> _chainDict;
    protected List<Caption> _captionList;

    public int height;
    public int width;
    public int crossVal;
    public boolean reviewed;
    public String comments;
    public String imgURL;

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
    public Document(List<String> corefStrings)
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

    /**Constructs a Document from the given captions and
     * docID (which is only included for method signature reasons)
     *
     * @param docID       Document ID
     * @param captionList List of captions for this Document
     */
    public Document(String docID, List<Caption> captionList)
    {
        _captionList = new ArrayList<>(captionList);
        _ID = docID;
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

    protected Document()
    {
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
        for(String chainID : assocChainIDs)
            if(_chainDict.containsKey(chainID))
                _chainDict.get(chainID).addBoundingBox(b);
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
        //UPDATE: if for some reason we don't happen to have
        //        this chain yet, add one (this should only
        //        trigger if we have some weird DB issues that
        //        should immediately be addressed)
        if(!_chainDict.keySet().contains(m.getChainID()))
            addChain(new Chain(_ID, m.getChainID()));
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

    private List<List<Mention>> getAdjacentClusters(List<Mention> partOfCluster,
                                                    List<List<Mention>> agentClusters)
    {
        int leftIdx = partOfCluster.get(0).getIdx();
        int rightIdx = partOfCluster.get(partOfCluster.size()-1).getIdx();

        //treat all multi-element clusters as neuter
        String b_gender = "neuter";
        if(partOfCluster.size() == 1)
            b_gender = partOfCluster.get(0).getGender();

        //Iterate through each agent cluster, finding the nearest
        //on either side
        List<Mention> leftAgents = null;
        List<Mention> rightAgents = null;
        int maxIdx = Integer.MIN_VALUE;
        int minIdx = Integer.MAX_VALUE;
        for(List<Mention> agents : agentClusters){
            Mention a_last = agents.get(agents.size()-1);
            Mention a_first = agents.get(0);
            int idx_last = a_last.getIdx();
            int idx_first = a_first.getIdx();

            //treat all multi-element clusters as neuter
            String a_gender = "neuter";
            if(agents.size() == 1)
                a_gender = agents.get(0).getGender();
            boolean genderMatch = b_gender.equals("neuter") ||
                    a_gender.equals("neuter") || b_gender.equals(a_gender);

            //store this agent cluster if its the nearest
            if(idx_last < leftIdx && idx_last > maxIdx && genderMatch){
                maxIdx = idx_last;
                leftAgents = agents;
            } else if(idx_first > rightIdx && idx_first < minIdx && genderMatch){
                minIdx = idx_first;
                rightAgents = agents;
            }
        }
        List<List<Mention>> agentArr = new ArrayList<>();
        agentArr.add(leftAgents);
        agentArr.add(rightAgents);
        return agentArr;
    }

    /**Returns a set of chain pairs that are
     * in the part-of relation (part-whole)
     *
     * @return  Set of Chain pairs (as arrays)
     */
    public Set<Chain[]> getPartOfChains()
    {
        Set<Mention[]> partOfSet = new HashSet<>();

        //Storing mention types (coarse)
        Map<Mention, String> typeDict = new HashMap<>();
        for(Mention m : getMentionList()){
            String coarseType = null;
            switch(m.getLexicalType()){
                case "people":
                case "animals": coarseType = "agents";
                    break;
                case "bodyparts": coarseType = "bodyparts";
                    break;
                case "clothing":
                case "colors":
                case "clothing/colors": coarseType = "clothing";
                    break;
                default:
                    if(m.getPronounType().isAnimate())
                        coarseType = "agents";
            }
            typeDict.put(m, coarseType);
        }

        //Iterate through our captions
        for(Caption c : _captionList){
            //Create mention clusters -- that is, groups
            //of agent, clothing, or bodypart mentions
            //that are separated only by specified conjunctions
            List<List<Mention>> cluster_agent = new ArrayList<>();
            List<List<Mention>> cluster_bodyparts = new ArrayList<>();
            List<List<Mention>> cluster_clothing = new ArrayList<>();

            //Obvious list-delimiters (comma, and) should be used,
            //as well as "on and", as in "a hat on and a blue shirt"
            List<String> allowedConjList =
                    Arrays.asList(",", "and", ", and", "on and");

            List<Mention> currentCluster = new ArrayList<>();
            for(int i=0; i<c.getMentionList().size(); i++){
                Mention m_i = c.getMentionList().get(i);
                String type_i = typeDict.get(m_i);

                //If this mention isn't of a valid type, continue
                if(type_i == null)
                    continue;

                Mention m_j = null;
                String interstitialText = "";
                if(i > 0){
                    m_j = c.getMentionList().get(i-1);
                    interstitialText = StringUtil.listToString(
                            c.getInterstitialTokens(m_j, m_i), " ");
                }

                //We add this mention to the current cluster if
                // 1) there _is_ a current cluster
                // 2) the last mention in the cluster is the
                //    left-adjacent mention
                // 3) This mention has the same approved type as
                //    the rest of the cluster
                // 4) The interstitial text is one of the approved
                //    conjunctions
                boolean validClusterAddition = !currentCluster.isEmpty() &&
                        currentCluster.get(currentCluster.size()-1).equals(m_j) &&
                        type_i.equals(typeDict.get(m_j)) &&
                        allowedConjList.contains(interstitialText);

                //If this is not a valid cluster addition, close
                //out / store the cluster before adding this mention
                if(!validClusterAddition && !currentCluster.isEmpty()){
                    String prevType = typeDict.get(currentCluster.get(currentCluster.size()-1));
                    switch(prevType){
                        case "agents": cluster_agent.add(currentCluster);
                            break;
                        case "bodyparts": cluster_bodyparts.add(currentCluster);
                            break;
                        case "clothing": cluster_clothing.add(currentCluster);
                            break;
                    }
                    currentCluster = new ArrayList<>();
                }

                //Add this mention to the current cluster
                currentCluster.add(m_i);
            }

            //For each bodyparts cluster, find the nearest left/right
            //agent clusters with matching genders (if applicable)
            for(List<Mention> bodyparts : cluster_bodyparts){
                List<List<Mention>> nearestAgents =
                        getAdjacentClusters(bodyparts, cluster_agent);
                List<Mention> nearestLeft = nearestAgents.get(0);
                List<Mention> nearestRight = nearestAgents.get(1);

                List<Mention> agentCluster = null;

                //1) Associate the nearest following agent cluster
                //   if X in an XofY construction ("the arm of a man")
                if(nearestRight != null){
                    String interstitial_right =
                            StringUtil.listToString(c.getInterstitialTokens(bodyparts.get(bodyparts.size()-1),
                                    nearestRight.get(0)), " ").toLowerCase().trim();
                    if(interstitial_right.equals("of"))
                        agentCluster = nearestRight;
                }
                //2) Associate the nearest preceding agent cluster in
                //   all other cases
                if(agentCluster == null && nearestLeft != null)
                    agentCluster = nearestLeft;

                //Associate each bodypart as partOf the agent
                if (agentCluster != null) {
                    for (Mention b : bodyparts) {
                        for (Mention a : agentCluster) {
                            Mention[] arr = {b, a};
                            if (!Util.containsArr(partOfSet, arr))
                                partOfSet.add(arr);
                        }
                    }
                }
            }

            //For each clothing cluster, find the nearest left/right
            //agent clusters with matching genders if applicable
            for(List<Mention> clothing : cluster_clothing){
                List<List<Mention>> nearestAgents =
                        getAdjacentClusters(clothing, cluster_agent);
                List<Mention> nearestLeft = nearestAgents.get(0);

                //Associate the nearest preceding agent cluster
                if(nearestLeft != null){
                    for(Mention cloth : clothing){
                        for(Mention a : nearestLeft){
                            Mention[] arr = {cloth, a};
                            if(!Util.containsArr(partOfSet, arr))
                                partOfSet.add(arr);
                        }
                    }
                }
            }
        }

        //As with the coref and subset relation, part-of actually
        //operates over entities (chains), not mentions, so scale
        //out, where a coref label means mentions cannot be in a
        //part-of relation
        Set<Chain[]> partOfChains = new HashSet<>();
        for(Mention[] pair : partOfSet){
            if(!pair[0].getChainID().equals(pair[1].getChainID()) &&
               !pair[0].getChainID().equals("0") && !pair[1].getChainID().equals("0")){
                Chain[] arr = {_chainDict.get(pair[0].getChainID()),
                               _chainDict.get(pair[1].getChainID())};
                if(!Util.containsArr(partOfChains, arr))
                    partOfChains.add(arr);
            }
        }

        //We should be able to enforce transitivity in exactly
        //the same way as with subset pairs
        _enforceSubsetTransitivity(partOfChains);

        //Return the resulting chains
        return partOfChains;
    }

    /**Returns the set of part-of pairs
     * (as unique ID strings) for this document
     *
     * @return  Set of in-order mention pair strings
     */
    public Set<String> getPartOfMentions()
    {
        return _getMentionPairStrings(getPartOfChains());
    }

    /**Returns a set of chain pairs that are in
     * the subset relation
     *
     * @return  Set of Chain pairs (as arrays)
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
     * @return  Set of in-order mention pair strings
     */
    public Set<String> getSubsetMentions()
    {
        return _getMentionPairStrings(getSubsetChains());
    }

    /**Returns a set of in-order mention pair strings, given a set of
     * Chain pairs; used by getSubsetMentions and getPartOfMentions
     *
     * @param chainPairs    Set of chain pairs (as arrays)
     * @return              Set of in-order mention pair strings
     */
    private Set<String> _getMentionPairStrings(Set<Chain[]> chainPairs)
    {
        Set<String> mentionPairIDs = new HashSet<>();
        for(Chain[] chainPair : chainPairs)
            for(Mention m_i : chainPair[0].getMentionSet())
                for(Mention m_j : chainPair[1].getMentionSet())
                    mentionPairIDs.add(getMentionPairStr(m_i, m_j));
        return mentionPairIDs;
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
                Chain[] chainPair = new Chain[]{_chainDict.get(sub.getChainID()),
                    _chainDict.get(sup.getChainID())};
                if(!Util.containsArr(subsets, chainPair))
                    subsets.add(chainPair);
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
