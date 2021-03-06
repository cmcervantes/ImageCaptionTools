package structures;

import edu.illinois.cs.cogcomp.nlp.lemmatizer.IllinoisLemmatizer;
import utilities.StringUtil;

import java.util.*;

/**The Caption class represents sentences in image caption datasets,
 * and is composed of tokens, chunks, and mentions.
 *
 * @author ccervantes
 */
public class Caption extends Annotation {
    private static IllinoisLemmatizer lemmatizer;

    private List<Token> _tokenList;
    private List<Chunk> _chunkList;
    private List<Mention> _mentionList;
    private DependencyNode _rootNode;

    /**Default Caption constructor
     *
     * @param docID
     * @param idx
     * @param tokenList
     */
    public Caption(String docID, int idx, List<Token> tokenList) {
        _docID = docID;
        _idx = idx;
        _tokenList = new ArrayList<>(tokenList);
        initChunkList();
        initMentionList();
    }

    /**Constructs a Caption without tokens, which
     * should be added with addToken()
     *
     * @param docID
     * @param idx
     */
    public Caption(String docID, int idx) {
        _docID = docID;
        _idx = idx;
        _tokenList = new ArrayList<>();
        _chunkList = new ArrayList<>();
        _mentionList = new ArrayList<>();
    }

    /**Empty Caption constructor for use during static
     * Caption construction
     */
    private Caption() {
        _tokenList = new ArrayList<>();
        _chunkList = new ArrayList<>();
        _mentionList = new ArrayList<>();
    }

    /**Adds the given Token to the internal list
     * in the position determined by token index
     *
     * @param t
     */
    public void addToken(Token t) {
        int insertionIdx =
                Annotation.getInsertionIdx(_tokenList, t);
        _tokenList.add(insertionIdx, t);
    }

    /**Creates a new Chunk with the tokens at the given
     * ranges and type, adding it into the internal chunk
     * list based on the given index
     *
     * @param chunkIdx
     * @param chunkType
     * @param startTokenIdx
     * @param endTokenIdx
     */
    public void addChunk(int chunkIdx, String chunkType,
                         int startTokenIdx, int endTokenIdx) {
        Chunk ch = new Chunk(_docID, _idx, chunkIdx, chunkType,
                _tokenList.subList(startTokenIdx, endTokenIdx + 1));
        int insertionIdx = Annotation.getInsertionIdx(_chunkList, ch);
        _chunkList.add(insertionIdx, ch);

        //associates this chunk's tokens with this index
        for (int i = startTokenIdx; i <= endTokenIdx; i++) {
            _tokenList.get(i).chunkIdx = chunkIdx;
            _tokenList.get(i).chunkType = chunkType;
        }
    }

    /**Adds a new Mention with the given attributes and
     * returns it
     *
     * @param idx
     * @param lexicalType
     * @param chainID
     * @param card
     * @param startTokenIdx
     * @param endTokenIdx
     * @return
     */
    public Mention addMention(int idx, String lexicalType, String chainID,
                              Cardinality card, int startTokenIdx,
                              int endTokenIdx) {
        List<Token> mentionTokenList =
                _tokenList.subList(startTokenIdx, endTokenIdx + 1);
        List<Chunk> mentionChunkList = new ArrayList<>();
        int startChunkIdx = mentionTokenList.get(0).chunkIdx;
        int endChunkIdx = mentionTokenList.get(mentionTokenList.size() - 1).chunkIdx;
        for (int i = startChunkIdx; i <= endChunkIdx; i++)
            if (i > -1 && i < _chunkList.size())
                mentionChunkList.add(_chunkList.get(i));

        Mention m = new Mention(_docID, _idx, idx, chainID,
                mentionTokenList, mentionChunkList,
                lexicalType, card);
        int insertionIdx =
                Annotation.getInsertionIdx(_mentionList, m);
        _mentionList.add(insertionIdx, m);

        //associates this mention's tokens with this index
        for (int i = startTokenIdx; i <= endTokenIdx; i++) {
            _tokenList.get(i).mentionIdx = idx;
            _tokenList.get(i).chainID = chainID;
        }

        return m;
    }

    /**Constructs the dependency tree (and sets the internal root node)
     * based on the given dependency strings, which must be in the
     * format gov_token_idx|relation|dep_token_idx
     *
     * @param dependencyStrings
     */
    public void setRootNode(Collection<String> dependencyStrings) {
        List<String> depStrings = new ArrayList<>(dependencyStrings);
        boolean[] usedDepBits = new boolean[depStrings.size()];
        Arrays.fill(usedDepBits, false);
        _rootNode = null;

        //construct our dependency tree, given the strings
        boolean addedNode;
        do {
            addedNode = false;
            for (int i = 0; i < depStrings.size(); i++) {
                if (!usedDepBits[i]) {
                    String[] depArr = depStrings.get(i).split("\\|");
                    int govTokenIdx = Integer.parseInt(depArr[0]);
                    int depTokenIdx = Integer.parseInt(depArr[2]);

                    if (govTokenIdx < 0) {
                        //if we already have a root node, bomb; I don't yet have a way to handle
                        //multi-rooted captions
                        //TODO: enable captions with multiple dependency roots
                        if (_rootNode != null) {
                            _rootNode = null;
                            return;
                        }
                        _rootNode = new DependencyNode(_tokenList.get(depTokenIdx));
                        usedDepBits[i] = true;
                        addedNode = true;
                    } else if (_rootNode != null) {
                        //get the tokens specified by the indices
                        DependencyNode govNode = _rootNode.findDependent(_tokenList.get(govTokenIdx));
                        if (govNode != null) {
                            govNode.addDependent(_tokenList.get(depTokenIdx), depArr[1]);
                            usedDepBits[i] = true;
                            addedNode = true;
                        }
                    }
                }
            }
        } while (addedNode);
    }

    /**Returns the Chunk immediately left-adjacent to ch;
     * null if ch is the first chunk
     *
     * @param ch
     * @return
     */
    public Chunk getLeftNeighbor(Chunk ch) {
        if (ch.getIdx() > 0)
            return _chunkList.get(ch.getIdx() - 1);
        return null;
    }

    /**Returns the Chunk immediately right-adjacent to ch;
     * null if ch is the last chunk
     *
     * @param ch
     * @return
     */
    public Chunk getRightNeighbor(Chunk ch) {
        if (ch.getIdx() < _chunkList.size() - 1)
            return _chunkList.get(ch.getIdx() + 1);
        return null;
    }

    /* Getters */
    public List<Mention> getMentionList() {
        return _mentionList;
    }

    public List<Token> getTokenList() {
        return _tokenList;
    }

    public List<Chunk> getChunkList() {
        return _chunkList;
    }

    public DependencyNode getRootNode() {
        return _rootNode;
    }

    /**Returns a dataset-unique ID for this caption, in the form
     * docID#capIdx
     *
     * @return
     */
    public String getUniqueID() {
        return _docID + "#" + _idx;
    }

    /**Returns a list of interstitial chunks between chunks c1 and c2;
     * empty list if c1 is adjacent to c2
     *
     * @param c1
     * @param c2
     * @return
     */
    public List<Chunk> getInterstitialChunks(Chunk c1, Chunk c2) {
        int startIdx = c1.getIdx() + 1;
        int endIdx = c2.getIdx();

        //sublist is [inclusive,exclusive)
        if (startIdx < endIdx)
            return new ArrayList<>(_chunkList.subList(startIdx, endIdx));
        return new ArrayList<>();
    }

    /**Returns a list of interstitial chunks between mentions m1 and m2;
     * empty list if m1 is adjacent to m2
     *
     * @param m1
     * @param m2
     * @return
     */
    public List<Chunk> getInterstitialChunks(Mention m1, Mention m2)
    {
        if(m1.getChunkList() == null || m2.getChunkList() == null ||
           m1.getChunkList().isEmpty() || m2.getChunkList().isEmpty())
            return new ArrayList<>();
        Chunk lastChunk_1 = m1.getChunkList().get(m1.getChunkList().size() - 1);
        Chunk firstChunk_2 = m2.getChunkList().get(0);
        return getInterstitialChunks(lastChunk_1, firstChunk_2);
    }

    /**Returns a list of interstitial tokens between chunks c1 and c2;
     * empty list if c1 is adjacent to c2
     *
     * @param c1
     * @param c2
     * @return
     */
    public List<Token> getInterstitialTokens(Chunk c1, Chunk c2) {
        int startIdx = c1.getTokenRange()[1] + 1;
        int endIdx = c2.getTokenRange()[0];

        //sublist is [inclusive,exclusive)
        if (startIdx < endIdx)
            return new ArrayList<>(_tokenList.subList(startIdx, endIdx));
        return new ArrayList<>();
    }

    /**Returns a list of interstitial tokens between mentions m1 and m2;
     * empty list if m1 is adjacent to m2
     *
     * @param m1
     * @param m2
     * @return
     */
    public List<Token> getInterstitialTokens(Mention m1, Mention m2) {
        if (m1.getChunkList() != null && !m1.getChunkList().isEmpty() &&
                m2.getChunkList() != null && !m2.getChunkList().isEmpty()) {
            Chunk lastChunk_1 = m1.getChunkList().get(m1.getChunkList().size() - 1);
            Chunk firstChunk_2 = m2.getChunkList().get(0);
            return getInterstitialTokens(lastChunk_1, firstChunk_2);
        }
        return new ArrayList<>();
    }

    /**Returns the text of this caption
     *
     * @return
     */
    @Override
    public String toString() {
        return StringUtil.listToString(_tokenList, " ");
    }

    /**Returns this caption's attributes as a key:value; string
     *
     * @return - key-value string of caption attributes
     */
    @Override
    public String toDebugString() {
        String[] keys = {"numTokens", "numChunks", "numMentions"};
        Object[] vals = {_tokenList.size(), _chunkList.size(), _mentionList.size()};
        return StringUtil.toKeyValStr(keys, vals);
    }

    /**Returns this caption as a latex string, where each mention is
     * bracketed, with mentionAssigs assignments as superscripts
     *
     * @param mentionAssigs
     * @return
     */
    public String toLatexString(Map<String, String> mentionAssigs)
    {
        return toLatexString(mentionAssigs, null, false, false);
    }

    /**Returns this caption as a latex string, where each mention
     * is bolded or bracketed (depeding on useBold) with assignments
     * from mentionAssigs as superscripts or subscripts
     * (depending on subscript), where --if assigColors is not null --
     * mentions have their assignment's color
     *
     * @param mentionAssigs
     * @param assigColors
     * @param subscript
     * @return
     */
    public String toLatexString(Map<String, String> mentionAssigs,
                                Map<String, String> assigColors,
                                boolean useBold, boolean subscript)
    {
        //create a mapping from token indices to mention IDs
        //(but only for those tokens that are in mentions)
        Map<Integer, String> tokenIdxMentionIdDict = new HashMap<>();
        for(Mention m : _mentionList)
            for(Token t : m.getTokenList())
                tokenIdxMentionIdDict.put(t.getIdx(), m.getUniqueID());

        StringBuilder sb = new StringBuilder();
        String lastMentionID = null;
        boolean buildingMention = false;
        for(Token t : _tokenList)
        {
            String currentMentionID = tokenIdxMentionIdDict.get(t.getIdx());

            //Switch on the last/current equality. Essentially
            //1) If equal (or both null): add t without markup
            //2) If last is null, current is not: start markup, add t
            //3) If last is mention, current is null: end markup, add t
            //4) If inequal, neither is null: end markup, start markup, add t
            if((lastMentionID != null && !lastMentionID.equals(currentMentionID)) ||
                (currentMentionID != null && !currentMentionID.equals(lastMentionID)))
            {
                //by reaching here, we know we have inequal
                //mentionIDs. So, using that switch logic comment
                //above, we know to add end markup if we see
                //a non-null lastMention
                if(lastMentionID != null)
                {
                    sb.deleteCharAt(sb.length()-1);

                    if(useBold)
                        sb.append("}");
                    else
                        sb.append("]");
                    if(subscript)
                        sb.append("\\textsubscript{");
                    else
                        sb.append("\\textsuperscript{");
                    sb.append(mentionAssigs.get(lastMentionID));
                    sb.append("}");

                    if(assigColors != null)
                        sb.append("}");

                    sb.append(" ");
                    buildingMention = false;
                }

                //we also know to add start mention markup
                //if we see a non-null currentMentionID
                if(currentMentionID != null)
                {
                    if(assigColors != null){
                        String color = assigColors.get(mentionAssigs.get(currentMentionID));
                        if(color == null)
                            color = "black";
                        sb.append("\\textcolor{");
                        sb.append(color);
                        sb.append("}{");
                    }

                    if(useBold)
                        sb.append("\\textbf{");
                    else
                        sb.append("[");

                    buildingMention = true;
                }
            }
            lastMentionID = currentMentionID;

            //regardless of how we switched, above, we still want
            //to add this token. So add it.
            sb.append(t.toString());
            sb.append(" ");
        }
        //if we're still building a mention, close it
        if(buildingMention) {
            if(useBold)
                sb.append("}");
            else
                sb.append("]");
            if(subscript)
                sb.append("\\textsubscript{");
            else
                sb.append("\\textsuperscript{");
            sb.append(mentionAssigs.get(lastMentionID));
            sb.append("}");

            if(assigColors != null)
                sb.append("}");
        }
        return sb.toString();
    }

    /**Returns this caption as an html string where
     * mentions are enclosed in span tags
     *
     * @return
     */
    public String toSpanString()
    {
        //create a mapping from token indices to mention IDs
        //(but only for those tokens that are in mentions)
        HashMap<Integer, String> tokenIdxMentionIdDict =
                new HashMap<>();
        for(Mention m : _mentionList) {
            for(Token t : m.getTokenList()) {
                tokenIdxMentionIdDict.put(t.getIdx(), m.getUniqueID());
            }
        }

        //build a string from all of this caption's tokens,
        //span tagging all the mention tokens
        StringBuilder sb = new StringBuilder();
        String lastMentionID = null;
        boolean buildingMention = false;
        for(Token t : _tokenList)
        {
            String currentMentionID =
                    tokenIdxMentionIdDict.get(t.getIdx());

            //Switch on the last/current equality. Essentially
            //1) If equal (or both null): add t without markup
            //2) If last is null, current is not: start markup, add t
            //3) If last is mention, current is null: end markup, add t
            //4) If inequal, neither is null: end markup, start markup, add t
            if((lastMentionID != null && !lastMentionID.equals(currentMentionID)) ||
                    (currentMentionID != null && !currentMentionID.equals(lastMentionID)))
            {
                //by reaching here, we know we have inequal
                //mentionIDs. So, using that switch logic comment
                //above, we know to add end markup if we see
                //a non-null lastMention
                if(lastMentionID != null)
                {
                    sb.deleteCharAt(sb.length()-1);
                    sb.append("</span> ");
                    buildingMention = false;
                }

                //we also know to add start mention markup
                //if we see a non-null currentMentionID
                if(currentMentionID != null)
                {
                    sb.append("<span id=\"");
                    sb.append(currentMentionID);
                    sb.append("\">");
                    buildingMention = true;
                }
            }
            lastMentionID = currentMentionID;

            //regardless of how we switched, above, we still want
            //to add this token. So add it.
            sb.append(t.toString());
            sb.append(" ");
        }
        //if we're still building a mention, close it
        if(buildingMention)
            sb.append("</span>");
        return sb.toString();
    }

    /**Returns this caption as a series of chunk types (ie. "NP VP NP").
     *
     * @return  A chunk type string
     */
    public String toChunkTypeString(){return toChunkTypeString(true);}

    /**Returns this caption as a series of chunk types (ie. "NP VP NP").
     * Optional argument includeChunklessTokens directly copies
     * the text of tokens that aren't associated with chunks
     * (ie. "NP and NP VP")
     *
     * @param includeChunklessTokens Whether to include the text of tokens without
     *                               chunks (true by default)
     * @return  A chunk type string
     */
    public String toChunkTypeString(boolean includeChunklessTokens)
    {
        StringBuilder sb = new StringBuilder();
        int prevChunkIdx = -1;
        for(Token t : _tokenList){
            String chunkType = t.chunkType;
            int chunkIdx = t.chunkIdx;
            if(chunkType == null && includeChunklessTokens){
                sb.append(t.toString());
                sb.append(" ");
            } else if(chunkType != null && chunkIdx != prevChunkIdx){
                sb.append(t.chunkType);
                sb.append(" ");
            }
            prevChunkIdx = t.chunkIdx;
        }
        return sb.toString().trim();
    }

    /**Returns this caption as a coref string (ostensibly for inclusion in a
     * .coref file); Optional argument tokenChainDict allows mentions to be
     * re-mapped to chainIDs (necessary during dataset revisions) and optional
     * argument includeID specifies whether the returned string should be
     * prefixed with the caption's uniqueID
     *
     * @return
     */
    public String toCorefString()
    {
        return toCorefString(true);
    }

    /**Returns this caption as a coref string (ostensibly for inclusion in a
     * .coref file); Optional argument tokenChainDict allows mentions to be
     * re-mapped to chainIDs (necessary during dataset revisions) and optional
     * argument includeID specifies whether the returned string should be
     * prefixed with the caption's uniqueID
     *
     * @param includeID      - Whether to prefix the returned string with uniqueID
     *                         (true by default)
     * @return
     */
    public String toCorefString(boolean includeID)
    {
        Map<Integer, String> tokenChainDict = new HashMap<>();
        for(Token t : _tokenList)
            tokenChainDict.put(t.getIdx(), t.chainID);
        return toCorefString(tokenChainDict, includeID);
    }

    /**Returns this caption as a coref string (ostensibly for inclusion in a
     * .coref file); Optional argument tokenChainDict allows mentions to be
     * re-mapped to chainIDs (necessary during dataset revisions) and optional
     * argument includeID specifies whether the returned string should be
     * prefixed with the caption's uniqueID
     *
     * FORMAT:  [EN/chainID [ChunkType token/POS ] ] [ChunkType token/POS ] ...
     * Ex.      [PP On/IN ] [EN/0 [NP a/DT bright/JJ sunny/JJ day/NN ] ] ...
     *
     * @param tokenChainDict - Token chain ID assignments (if unspecified, uses
     *                         the token's internal chain assignments)
     * @return
     */
    public String toCorefString(Map<Integer, String> tokenChainDict)
    {
        return toCorefString(tokenChainDict, true);
    }

    /**Returns this caption as a coref string (ostensibly for inclusion in a
     * .coref file); Optional argument tokenChainDict allows mentions to be
     * re-mapped to chainIDs (necessary during dataset revisions) and optional
     * argument includeID specifies whether the returned string should be
     * prefixed with the caption's uniqueID
     *
     * FORMAT:  [EN/chainID [ChunkType token/POS ] ] [ChunkType token/POS ] ...
     * Ex.      [PP On/IN ] [EN/0 [NP a/DT bright/JJ sunny/JJ day/NN ] ] ...
     *
     * @param tokenChainDict - Token chain ID assignments (if unspecified, uses
     *                         the token's internal chain assignments)
     * @param includeID      - Whether to prefix the returned string with uniqueID
     *                         (true by default)
     * @return
     */
    public String toCorefString(Map<Integer, String> tokenChainDict, boolean includeID)
    {
        StringBuilder sb = new StringBuilder();
        int prevChunkIdx = -1;
        int currentChunkIdx = -1;
        int prevEntityIdx = -1;
        int currentEntityIdx = -1;
        boolean buildingChunk = false;
        boolean buildingEntity = false;
        for(Token t : _tokenList){
            boolean closeChunk = false;
            boolean openChunk = false;
            boolean closeEntity = false;
            boolean openEntity = false;

            currentChunkIdx = t.chunkIdx;
            //there are cases where the chunkIdx is valid but
            //the type has been stripped. In these cases we treat the
            //chunk as though it's invalid (to prevent it from
            //being bracketed
            if(t.chunkType == null || t.chunkType.equalsIgnoreCase("null"))
                currentChunkIdx = -1;

            currentEntityIdx = t.mentionIdx;

            //We want to handle three cases
            //a) We weren't building an entity, but now we are
            //b) We were building an entity, and now we're building a new one
            //c) We were building an entity, and now we're not
            if(currentEntityIdx != prevEntityIdx){
                if(prevEntityIdx > -1) {  // cases b and c
                    closeEntity = true;
                    buildingEntity = false;
                }
                if(currentEntityIdx > -1) { //cases a and b
                    openEntity = true;
                    buildingEntity = true;
                }
            }
            //same deal with chunks
            if(currentChunkIdx != prevChunkIdx){
                if(prevChunkIdx > -1) {
                    closeChunk = true;
                    buildingChunk = false;
                }
                if(currentChunkIdx > -1) {
                    openChunk = true;
                    buildingChunk = true;
                }
            }

            //handle these open / close entity / chunk blocks
            //in order; close prev chunk, prev entity, start new
            //entity, new chunk, add text
            if(closeChunk)
                sb.append("] ");
            if(closeEntity)
                sb.append("] ");
            if(openEntity){
                sb.append("[EN/");
                String chainID = tokenChainDict.get(t.getIdx());
                if(chainID == null)
                    chainID = "0";
                sb.append(chainID);
                sb.append(" ");
            }
            if(openChunk){
                sb.append("[");
                sb.append(t.chunkType);
                sb.append(" ");
            }

            /*
            boolean internalOf = false;
            if(t.chunkType != null && t.chunkType.equals("NP") && t.toString().equals("of")){
                sb.append("] ");
                sb.append("[PP ");
                internalOf = true;
            }*/

            //regardless of what / where we are, add the token
            sb.append(t.toString());
            if(!t.toString().equals("/")){
                sb.append("/");
                sb.append(t.getPosTag());
            }
            sb.append(" ");

            /*
            if(internalOf) {
                sb.append("] ");
                sb.append("[NP ");
            }*/

            //set the previous
            prevChunkIdx = currentChunkIdx;
            prevEntityIdx = currentEntityIdx;
        }
        if(buildingChunk)
            sb.append("] ");
        if(buildingEntity)
            sb.append("] ");

        String corefStr = sb.toString().trim();
        if(includeID)
            return getUniqueID() + "\t" + corefStr;
        return corefStr;
    }

    /**Returns this caption as a pos string (ostensibly for inclusion in a
     * .pos file); Optional argument includeID specifies whether the
     * returned string should be prefixed with the caption's uniqueID
     *
     * FORMAT:  [ChunkType token/POS ] ] [ChunkType token/POS ] ...
     * Ex.      [PP On/IN ] [NP a/DT bright/JJ sunny/JJ day/NN ] ...
     *
     * @param includeID      - Whether to prefix the returned string with uniqueID
     *                         (true by default)
     * @return
     */
    public String toCorefString_pos(boolean includeID)
    {
        StringBuilder sb = new StringBuilder();
        int prevChunkIdx = -1;
        int currentChunkIdx = -1;
        boolean buildingChunk = false;
        for(Token t : _tokenList){
            boolean closeChunk = false;
            boolean openChunk = false;

            currentChunkIdx = t.chunkIdx;
            //there are cases where the chunkIdx is valid but
            //the type has been stripped. In these cases we treat the
            //chunk as though it's invalid (to prevent it from
            //being bracketed
            if(t.chunkType == null || t.chunkType.equalsIgnoreCase("null"))
                currentChunkIdx = -1;

            //We want to handle three cases
            //a) We weren't building an chunk, but now we are
            //b) We were building an chunk, and now we're building a new one
            //c) We were building an chunk, and now we're not
            if(currentChunkIdx != prevChunkIdx){
                if(prevChunkIdx > -1) {
                    closeChunk = true;
                    buildingChunk = false;
                }
                if(currentChunkIdx > -1) {
                    openChunk = true;
                    buildingChunk = true;
                }
            }

            //handle these open / close chunk blocks
            //in order; close prev chunk, start new
            //new chunk, add text
            if(closeChunk)
                sb.append("] ");
            if(openChunk){
                sb.append("[");
                sb.append(t.chunkType);
                sb.append(" ");
            }

            boolean internalOf = false;
            if(t.chunkType != null && t.chunkType.equals("NP") && t.toString().equals("of")){
                sb.append("] ");
                sb.append("[PP ");
                internalOf = true;
            }

            //regardless of what / where we are, add the token
            sb.append(t.toString());
            sb.append("/");
            sb.append(t.getPosTag());
            sb.append(" ");

            if(internalOf) {
                sb.append("] ");
                sb.append("[NP ");
            }

            //set the previous
            prevChunkIdx = currentChunkIdx;
        }
        if(buildingChunk)
            sb.append("] ");

        String posStr = sb.toString().trim();
        if(includeID)
            return getUniqueID() + "\t" + posStr;
        return posStr;
    }

    /**Returns this caption as an entities string (ostensibly for inclusion in a .txt
     * file included with the Flickr30kEntities dataset)
     *
     * FORMAT:  [/EN#chainID/lexicalType text ] text ...
     * EX:      [/EN#5/people Two teams ] compete ...
     *
     * @return
     */
    public String toEntitiesString()
    {
        Map<Integer, String> tokenChainDict = new HashMap<>();
        for(Token t : _tokenList)
            tokenChainDict.put(t.getIdx(), t.chainID);
        return toEntitiesString(tokenChainDict);
    }

    /**Returns this caption as an entities string (ostensibly for inclusion in a .txt
     * file included with the Flickr30kEntities dataset);
     * Optional argument tokenChainDict allows mentions to be re-mapped to new chainIDs
     * (necessary during dataset revisions); uses token chainIDs by default
     *
     * FORMAT:  [/EN#chainID/lexicalType text ] text ...
     * EX:      [/EN#5/people Two teams ] compete ...
     *
     * @param tokenChainDict
     * @return
     */
    public String toEntitiesString(Map<Integer, String> tokenChainDict)
    {
        Map<Integer, String> entityTypeDict = new HashMap<>();
        for(Mention m : _mentionList)
            entityTypeDict.put(m.getIdx(), m.getLexicalType());

        StringBuilder sb = new StringBuilder();
        int prevEntityIdx = -1;
        int currentEntityIdx = -1;
        boolean buildingEntity = false;
        for(Token t : _tokenList){
            boolean closeEntity = false;
            boolean openEntity = false;

            currentEntityIdx = t.mentionIdx;

            //We want to handle three cases
            //a) We weren't building an entity, but now we are
            //b) We were building an entity, and now we're building a new one
            //c) We were building an entity, and now we're not
            if(currentEntityIdx != prevEntityIdx){
                if(prevEntityIdx > -1) {  // cases b and c
                    closeEntity = true;
                    buildingEntity = false;
                }
                if(currentEntityIdx > -1) { //cases a and b
                    openEntity = true;
                    buildingEntity = true;
                }
            }

            //close the entity if we were building one, and open
            //a new one if we're doing that.
            if(closeEntity){
                //for backwards compatibility purposes
                //we can't put the closing bracket as its own
                //token
                sb.insert(sb.length()-1, "]");
            }
            if(openEntity){
                sb.append("[/EN#");
                String chainID = tokenChainDict.get(t.getIdx());
                if(chainID == null)
                    chainID = "0";
                sb.append(chainID);
                sb.append("/");
                sb.append(entityTypeDict.get(t.mentionIdx));
                sb.append(" ");
            }

            //regardless of what / where we are, add the token
            sb.append(t.toString());
            sb.append(" ");

            //set the previous
            prevEntityIdx = currentEntityIdx;
        }
        if(buildingEntity){
            //for backwards compatibility purposes
            //we can't put the closing bracket as its own
            //token
            sb.insert(sb.length()-1, "]");
        }

        return sb.toString().trim();
    }

    /**Returns this caption as a POS string, used during part-of-speech tagger training
     * and evaluation
     *
     * FORMAT:  (POS text) ...
     * EX:      (DT A) (NN person) ...
     *
     * @return
     */
    public String toPosString()
    {
        List<String> tokenStrList = new ArrayList<>();
        _tokenList.stream().forEachOrdered(t -> tokenStrList.add(t.toPosString()));
        return StringUtil.listToString(tokenStrList, " ");
    }

    /**Returns this caption as a list of CONLL-2000 chunk strings.
     *
     * FORMAT:  token POS goldTag predTag
     * EX:      can VB B-VP I-NP
     *
     * @return
     */
    public List<String> toConllStrings()
    {
        return toConllStrings(null);
    }

    /**Returns this caption as a list of CONLL-2000 chunk strings.
     * Optional argument predTokens specifies to add the predicted tokens
     * (if omitted, no predicted tokens are returned)
     *
     * FORMAT:  token POS goldTag predTag
     * EX:      can VB B-VP I-NP
     *
     * @param predTokens
     * @return
     */
    public List<String> toConllStrings(List<Token> predTokens)
    {
        List<String> conllStrings = new ArrayList<>();
        int prevChunkIdx = -1;
        int prevChunkIdxPrime = -1;
        for(int tokenIdx=0; tokenIdx < _tokenList.size(); tokenIdx++){
            //get the gold token data
            String label_gold = "O";
            Token t = _tokenList.get(tokenIdx);
            String cType = t.chunkType;
            int cIdx = t.chunkIdx;
            if(cType != null && !cType.isEmpty())
                label_gold = cType;
            //if this is a new chunk, it's a B;
            //else its an I (if it has a type)
            if(!label_gold.equals("O")){
                if(cIdx != prevChunkIdx)
                    label_gold = "B-" + label_gold;
                else
                    label_gold = "I-" + label_gold;
            }

            //get the predicted data, if we were given any
            String label_pred = "";
            if(predTokens != null){
                Token tPrime = predTokens.get(tokenIdx);
                String cTypePrime = tPrime.chunkType;
                int cIdxPrime = tPrime.chunkIdx;

                //if this chunk type is null or empty, the str is O
                label_pred = "O";
                if(cTypePrime != null && !cTypePrime.isEmpty())
                    label_pred = cTypePrime;

                if(!label_pred.equals("O")){
                    if(cIdxPrime != prevChunkIdxPrime)
                        label_pred = "B-" + label_pred;
                    else
                        label_pred = "I-" + label_pred;
                }
                prevChunkIdxPrime = cIdxPrime;
            }

            //add this line to the string list
            String[] lineArgs = {t.toString(), t.getPosTag(), label_gold, label_pred};
            conllStrings.add(StringUtil.listToString(lineArgs, " "));

            //store the current indices for the next iteration
            prevChunkIdx = cIdx;
        }
        return conllStrings;
    }

    /**Searches the dependency tree to find the VP chunk for which
     * the given mention is the subject; null if m is not a subject
     * or if this caption has no clean parse
     *
     * @param m
     * @return
     */
    public Chunk getSubjectOf(Mention m)
    {
        Chunk subjOf = null;
        if(_rootNode != null){
            List<DependencyNode> nodeList = _rootNode.getNodes(m);
            for(DependencyNode n : nodeList) {
                String relation = n.getRelationToGovernor();
                if(relation != null && relation.contains("subj")){
                    int chunkIdx = n.getGovernor().getToken().chunkIdx;
                    if(chunkIdx >= 0 && chunkIdx < _chunkList.size()){
                        Chunk ch = _chunkList.get(chunkIdx);
                        if (ch.getChunkType().equals("VP"))
                            subjOf = ch;
                    }
                }
            }
        }
        return subjOf;
    }

    /**Searches the dependency tree to find the VP chunk for which
     * the given mention is an object; null if m is not an object
     * or if this caption has no clean parse
     *
     * @param m
     * @return
     */
    public Chunk getObjectOf(Mention m)
    {
        Chunk objOf = null;
        if(_rootNode != null){
            List<DependencyNode> nodeList = _rootNode.getNodes(m);
            for(DependencyNode n : nodeList) {
                String relation = n.getRelationToGovernor();
                if(relation != null && relation.contains("obj")){
                    int chunkIdx = n.getGovernor().getToken().chunkIdx;
                    if(chunkIdx >= 0 && chunkIdx < _chunkList.size()){
                        Chunk ch = _chunkList.get(chunkIdx);
                        if (ch.getChunkType().equals("VP"))
                            objOf = ch;
                    }
                }
            }
        }
        return objOf;
    }

    /**Initializes the internal chunk list from the token list
     */
    private void initChunkList()
    {
        _chunkList = new ArrayList<>();
        //Though this isn't the most efficient way
        //to do this, it's the safest to avoid
        //weird off-by-one issues; store the min/max
        //indices for each chunk index along with
        //chunk type
        Map<Integer, Integer[]> chunkTokenDict = new HashMap<>();
        Map<Integer, String> chunkTypeDict = new HashMap<>();
        for(Token t : _tokenList){
            int chunkIdx = t.chunkIdx;
            String chunkType = t.chunkType;
            if(chunkIdx != -1){
                if(!chunkTokenDict.containsKey(chunkIdx)){
                    chunkTokenDict.put(chunkIdx, new Integer[]{Integer.MAX_VALUE, Integer.MIN_VALUE});
                    chunkTypeDict.put(chunkIdx, chunkType);
                }
                Integer[] indices = chunkTokenDict.get(chunkIdx);
                if(t.getIdx() < indices[0])
                    indices[0] = t.getIdx();
                if(t.getIdx() > indices[1])
                    indices[1] = t.getIdx();
            }
        }
        List<Integer> chunkIndices = new ArrayList<>(chunkTokenDict.keySet());
        Collections.sort(chunkIndices);
        for(int chunkIdx : chunkIndices)
            this.addChunk(chunkIdx, chunkTypeDict.get(chunkIdx),
                    chunkTokenDict.get(chunkIdx)[0], chunkTokenDict.get(chunkIdx)[1]);
    }

    /**Initializes the internal mention list from the token and chunk lists
     */
    private void initMentionList()
    {
        _mentionList = new ArrayList<>();
        Map<Integer, Integer[]> mentionTokenDict = new HashMap<>();
        Map<Integer, String> mentionChainDict = new HashMap<>();
        for(Token t : _tokenList){
            int mentionIdx = t.mentionIdx;
            String chainID = t.chainID;
            if(mentionIdx != -1){
                if(!mentionTokenDict.containsKey(mentionIdx)){
                    mentionTokenDict.put(mentionIdx, new Integer[]{Integer.MAX_VALUE, Integer.MIN_VALUE});
                    mentionChainDict.put(mentionIdx, chainID);
                }
                Integer[] indices = mentionTokenDict.get(mentionIdx);
                if(t.getIdx() < indices[0])
                    indices[0] = t.getIdx();
                if(t.getIdx() > indices[1])
                    indices[1] = t.getIdx();
            }
        }
        List<Integer> mentionIndices = new ArrayList<>(mentionTokenDict.keySet());
        Collections.sort(mentionIndices);
        for(int mentionIdx : mentionIndices)
            this.addMention(mentionIdx, null, mentionChainDict.get(mentionIdx),
                    null, mentionTokenDict.get(mentionIdx)[0],
                    mentionTokenDict.get(mentionIdx)[1]);
    }

    /**Loads a Caption from a coref string, where the caption
     * ID is tab-prefixed in the given string
     *
     * @param corefStr
     * @return
     * @throws Exception
     */
    public static Caption fromCorefStr(String corefStr) throws Exception
    {
        //get the id from the left side of the coref string
        String[] corefStrArr = corefStr.split("\t");
        String[] idArr = corefStrArr[0].split("#");
        return fromCorefStr(corefStrArr[1], idArr[0], Integer.parseInt(idArr[1]));
    }

    /**Loads a Caption from a coref string; this function assumes
     * the coref string will NOT have the tab-prefixed ID, requiring
     * explicit docID and capIdx values
     *
     * @param corefStr
     * @param docID
     * @param capIdx
     * @return
     */
    public static Caption fromCorefStr(String corefStr, String docID, int capIdx) throws Exception
    {
        Caption c = new Caption();
        c._docID = docID;
        c._idx = capIdx;

        //split the right side of the coref string by spaces
        String[] captionArr = corefStr.split(" ");
        int entityCounter = 0;
        int chunkCounter = 0;
        int entityIdx = -1;
        int chunkIdx = -1;
        String chainID = null;
        String chunkType = null;
        int tokenIdx_chunkStart = -1;
        int tokenIdx_mentionStart = -1;
        int chunkIdx_start = -1;
        boolean expectingClosingPPBracket = false;
        for(String s : captionArr) {
            s = s.trim();
            if (s.startsWith("[EN")) {
                //if we're building either a chunk or a mention,
                //we shouldn't be here
                if (entityIdx > -1 || chunkIdx > -1)
                    throw new Exception("Unexpected new entity bracket (" +
                            docID + "#" + capIdx + ")\n" + "Tokens thusfar:\n" +
                            StringUtil.listToString(c._tokenList, " "));
                entityIdx = entityCounter;
                chainID = s.split("/")[1];
                tokenIdx_mentionStart = c._tokenList.size();
                chunkIdx_start = c._chunkList.size();
            } else if (s.startsWith("[")) {
                if (chunkIdx > -1 && s.equals("[PP")){
                    System.err.println("WARNING: Ignoring internal PP bracket for v1 annotations for " +
                            docID + "#" + capIdx);
                    expectingClosingPPBracket = true;
                } else if(chunkIdx > -1){
                    throw new Exception("Unexpected new chunk bracket (" +
                            docID + "#" + capIdx + ")\n" + "Tokens thusfar:\n" +
                            StringUtil.listToString(c._tokenList, " "));
                } else {
                    //Drop the special chunk types from the DenotationGraph
                    //pipeline
                    chunkType = s.replace("[", "");
                    if(chunkType.contains("/"))
                        chunkType = chunkType.split("/")[0];

                    chunkIdx = chunkCounter;
                    tokenIdx_chunkStart = c._tokenList.size();
                }
            } else if (s.equals("]")) {
                if(expectingClosingPPBracket){
                    // Just ignore this closing bracket
                    expectingClosingPPBracket = false;
                } else if (chunkIdx > -1) {
                    Chunk ch = new Chunk(c._docID, c._idx, chunkIdx, chunkType,
                            c._tokenList.subList(tokenIdx_chunkStart,
                                    c._tokenList.size()));
                    c._chunkList.add(ch);
                    chunkIdx = -1;
                    chunkType = null;
                    chunkCounter++;
                } else if (entityIdx > -1) {
                    List<Token> tokenSubList = c._tokenList.subList(tokenIdx_mentionStart, c._tokenList.size());
                    if(!tokenSubList.isEmpty()){
                        Mention m = new Mention(c._docID, c._idx, entityIdx,
                                chainID, tokenSubList, c._chunkList.subList(chunkIdx_start,
                                c._chunkList.size()));
                        c._mentionList.add(m);
                        chainID = null;
                        entityIdx = -1;
                        entityCounter++;
                    }
                } else {
                    //for some reason we're closing a bracket
                    //we never started
                    throw new Exception("Found unopened closing bracket (" +
                            docID + "#" + capIdx + ")\n" + "Tokens thusfar:\n" +
                            StringUtil.listToString(c._tokenList, " "));
                }
            } else if(!s.isEmpty()) {
                //if we've reached here, this is a token/POS combo
                //UPDATE: in a small number of MSCOCO cases, there are additional spaces
                //that must be removed; skip these
                String text, pos, lemma;
                if(s.equals("/")){
                    text = pos = lemma = s;
                } else {
                    String[] tokenArr = s.split("/");
                    if(tokenArr.length < 2)
                        throw new Exception("Found token without POS (" +
                                docID + "#" + capIdx + ")\n" + "Token: " + s);
                    text = tokenArr[0].trim();
                    pos = tokenArr[1].trim();
                    lemma = lemmatizer.getLemma(text, pos).trim();
                }
                Token t = new Token(c._docID, c._idx, c._tokenList.size(),
                        text, lemma, chunkIdx, entityIdx, chunkType, pos,
                        chainID);
                c._tokenList.add(t);
            }
        }
        return c;
    }

    /**Loads a Caption from an entities string
     *
     * @param entitiesStr
     * @return
     * @throws Exception
     */
    public static Caption
        fromEntitiesStr(String docID, int idx,
                        String entitiesStr) throws Exception
    {
        Caption c = new Caption();
        c._docID = docID;
        c._idx = idx;

        String[] entitiesWords = entitiesStr.split(" ");
        String currentMentionType = null;
        String currentChainID = null;
        int tokenIdx = 0;
        int mentionIdx = 0;
        int tokenIdx_startMention = -1;
        for(String word : entitiesWords){
            //openening brackets indicate metadata
            if(word.startsWith("[")){
                //split the metadata by slashes
                String[] metadataArr = word.split("/");

                //the second element (first is blank) is EN#chainID
                currentChainID = metadataArr[1].replace("EN#","");

                //store the rest as the mention type
                List<String> typeList = new ArrayList<>();
                for(int i=2; i<metadataArr.length; i++)
                    typeList.add(metadataArr[i]);
                currentMentionType = StringUtil.listToString(typeList,"/");

                //store the next token as the start mention idx
                tokenIdx_startMention = c._tokenList.size();
            } else {
                String tokenText;
                if(word.endsWith("]"))
                    tokenText = word.replace("]", "");
                else
                    tokenText = word;
                Token t = new Token(docID, idx, tokenIdx, tokenText,
                                    mentionIdx, currentChainID);
                c._tokenList.add(t);
                tokenIdx++;

                if(word.endsWith("]")){
                    List<Token> mentionTokenList =
                            c._tokenList.subList(tokenIdx_startMention,
                            c._tokenList.size());
                    Mention m = new Mention(docID, idx, mentionIdx,
                            currentChainID, mentionTokenList,
                            currentMentionType);
                    c._mentionList.add(m);
                    mentionIdx++;
                    tokenIdx_startMention = -1;
                    currentChainID = null;
                    currentMentionType = null;
                }
            }
        }

        return c;
    }

    /**Function to initialize static lemmatizer;
     * Intended to be populated only when needed,
     * as the standard workflow should not have to
     * find lemmas from scratch
     */
    public static void initLemmatizer()
    {
        lemmatizer = new IllinoisLemmatizer();
    }
}
