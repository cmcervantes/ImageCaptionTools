package structures;

import utilities.FileIO;
import utilities.StringUtil;
import utilities.Util;

import java.util.*;

/**Mention objects are functionally noun-phrase Chunks with additional
 * attributes of interest. However, we cannot fold this
 * functionality into Chunk nor extend Chunk because
 * XofY constructions are three chunks grouped together.
 * To preserve chunk boundaries, we must therefore
 * have Mentions, which _contain_, but are not themselves,
 * NP chunks.
 *
 */
public class Mention extends Annotation
{
    //Word lists, initialized with initWordLists()
    private static Set<String> _massNouns;
    private static Map<String, Integer> _specialCollectiveNouns;
    private static Set<String> _collectiveNouns;

    //Lexicon dict, initialized with initLexiconDict
    private static Map<String, String> _lexiconDict;

    //member variables, set internally or during creation
    private int _captionIdx;
    private List<Token> _tokenList;
    private List<Chunk> _chunkList;
    private String _chainID;
    private Cardinality _card;
    private String _lexType;

    /**Mention constructor, used when lexical types are not set
     * and must be found in lexicons; originally implemented for
     * loading Mentions from .coref strings
     *  @param docID
     * @param captionIdx
     * @param idx
     * @param chainID
     * @param tokenList
     * @param chunkList
     */
    public Mention(String docID, int captionIdx, int idx,
                   String chainID, List<Token> tokenList,
                   List<Chunk> chunkList)
    {
        init(docID, captionIdx, idx, chainID,
             tokenList, chunkList, null, null);
    }

    /**Mention constructor, used when chunks are not available;
     * originally implemented for loading Mentions from
     * entities strings
     *  @param docID
     * @param captionIdx
     * @param idx
     * @param chainID
     * @param tokenList
     */
    public Mention(String docID, int captionIdx, int idx,
                   String chainID, List<Token> tokenList,
                   String lexicalType)
    {
        init(docID, captionIdx, idx, chainID,
             tokenList, null, lexicalType, null);
    }

    /**Constructs a Mention populating all internal fields;
     * originally written for use when loading Documents
     * from a database
     *
     * @param docID
     * @param captionIdx
     * @param idx
     * @param chainID
     * @param tokenList
     * @param chunkList
     * @param lexicalType
     * @param card
     */
    public Mention(String docID, int captionIdx, int idx,
                   String chainID, List<Token> tokenList,
                   List<Chunk> chunkList, String lexicalType,
                   Cardinality card)
    {
        init(docID, captionIdx, idx, chainID,
             tokenList, chunkList, lexicalType, card);
    }

    /**Private initializer for this mention
     *
     * @param docID
     * @param captionIdx
     * @param idx
     * @param chainID
     * @param tokenList
     * @param chunkList
     * @param lexicalType
     * @param card
     */
    private void init(String docID, int captionIdx, int idx,
                      String chainID, List<Token> tokenList,
                      List<Chunk> chunkList, String lexicalType,
                      Cardinality card)
    {
        _docID = docID;
        _captionIdx = captionIdx;
        _idx = idx;
        if(chunkList == null)
            _chunkList = new ArrayList<>();
        else
            _chunkList = new ArrayList<>(chunkList);
        _tokenList = new ArrayList<>(tokenList);
        _chainID = chainID;
        if(lexicalType == null)
            initLexicalType();
        else
            _lexType = lexicalType;
        if(card == null){
            //if we haven't initialized the collective noun
            //list (as is the case when we're loading mentions
            //from a database), set this card as null
            if(_collectiveNouns == null)
                _card = null;
            else
                initCardinality();
        } else {
            _card = card;
        }
    }

    /**Initializes the mention's Cardinality, based on the mention's
     * head and text; Cardinality is null if mass, and (1,0+) in cases
     * where the head is not a noun
     */
    private void initCardinality()
    {
        //don't bother with this if we don't have a head or
        //head pos
        Token headToken = getHead();
        if(headToken == null || headToken.getPosTag() == null)
            return;

        //if this is a pronoun, initialize its cardinality by lexicon
        Boolean isPluralPronoun = PRONOUN_TYPE.getIsPlural(this.toString().toLowerCase());
        if(isPluralPronoun != null){
            if(isPluralPronoun){
                _card = new Cardinality(new int[]{1, 0},
                        new boolean[]{false, true});
            } else {
                _card = new Cardinality(new int[]{1, 1},
                        new boolean[]{false, false});
            }
            return;
        }

        /**From a paper draft in which a mention's cardinality - η(m) - is defined
         *
         * Data:
         *      Mention m (head word: m_h; text: m_t;)
         *      Token part of speech: t_pos
         *      C: List of collective nouns
         *      S: (k, v) mapping of special collective nouns and countable values
         *      M: List of mass nouns
         * Result:
         *      η(m) as either a (T, U) tuple or ’mass’
         *      T=1;U=0+;
         *      if m_h ∈ M ∧ not t ∈ {DT,CD}, for all t ∈ m_t ; t < m_h
         *          return mass
         *      if m_h_pos ∈ {NN,NNP}
         *          U = 1;
         *      else if m_h ∈ C ∨ m_h_pos ∈ {NNS, NNPS}
         *          U = 1+;
         *      if m_h ∈ C ∧ m_h_pos ∈ {NNS,NNPS}
         *          T = 1+;
         *      if m_h ∈ S
         *          U = S(m_h);
         *      if   t_pos = CD; for all t ∈ m_t; t != m_h
         *          v = combined CD value;
         *          if m_h_pos ∈ {NN,NNP}
         *              U = v;
         *          else if m_h_pos ∈ {NNS,NNPS}
         *              T = v;
         *       return (T,U);
         */
        int setVal = 1;
        int elemVal = 0;
        boolean setAmbig = false;
        boolean elemAmbig = true;
        boolean collectiveHead = _collectiveNouns.contains(headToken.getLemma());
        boolean singularHead = headToken.getPosTag().equals("NN") ||
                headToken.getPosTag().equals("NNP");
        boolean pluralHead = headToken.getPosTag().equals("NNS") ||
                headToken.getPosTag().equals("NNPS");

        //if our head is a mass noun and isn't preceded by a determiner
        //or a count word, this is mention has no cardinality
        boolean containsPrecedingDetOrNum = false;
        for(Token t : _tokenList){
            if(t.getIdx() < headToken.getIdx()){
                String posTag = t.getPosTag();
                if(posTag.equals("DT") || posTag.equals("CD"))
                    containsPrecedingDetOrNum = true;
            }
        }
        if(_massNouns.contains(headToken.getLemma()) && !containsPrecedingDetOrNum){
            _card = new Cardinality();
            return;
        }

        //if our head is a singular noun, this mention refers to T sets of 1 element
        if(singularHead){
            elemVal = 1;
            elemAmbig = false;
        }

        //if the head is a collective noun or is a plural noun,
        //this mention refers to T sets of 1 or more elements
        if(collectiveHead || pluralHead) {
            elemVal = 1;
            elemAmbig = true;
        }

        //if the head is a collective noun _and_ is a plural noun,
        //this mention refers to 1+ sets of U elements
        if(collectiveHead && pluralHead) {
            setVal = 1;
            setAmbig = true;
        }

        //if the head is a special collective noun, get its value
        if(_specialCollectiveNouns.containsKey(headToken.getLemma())){
            elemVal = _specialCollectiveNouns.get(headToken.getLemma());
            elemAmbig = false;
        }

        //If this mention doesn't contain "number" or "#" -
        //which indicate an actual number in the image, rather
        //than a quantity - get the value for the mention's numerals,
        //if it has any
        String text = this.toString().toLowerCase().trim();
        if(!text.contains("number") && !text.contains("#")){
            Integer numeralVal = Util.getNumeralValue(text);
            if(numeralVal != null){
                //if the head is a singular noun, set U to its value
                //otherwise, set T to its value
                if(singularHead){
                    elemVal = numeralVal;
                    elemAmbig = false;
                } else if(pluralHead){
                    setVal = numeralVal;
                    setAmbig = false;
                }
            }
        }

        _card = new Cardinality(new int[]{setVal, elemVal},
                new boolean[]{setAmbig, elemAmbig});
    }

    /**Initializes this mention's lexical type, assuming the static
     * lexiconDict has been initialized already
     */
    private void initLexicalType()
    {
        //In order to distinguish between "buffalo" and "water buffalo"
        //(though, admittedly, they're the same type), we want to
        //get the lexical type for the longest string that
        //  - is in the lexicon
        //  - terminates the mention
        String type = "other";
        int matchLength = -1;
        List<String> lemmas = new ArrayList<>();
        _tokenList.forEach(t -> lemmas.add(t.getLemma()));
        String lemmaStr = StringUtil.listToString(lemmas, " ");

        for (String s : _lexiconDict.keySet()) {
            if (s.length() >= matchLength && lemmaStr.endsWith(s)) {
                type = _lexiconDict.get(s);
                matchLength = s.length();
            }
        }
        _lexType = type;
    }

    /* Getters */
    public int getCaptionIdx(){return _captionIdx;}
    public Cardinality getCardinality(){return _card;}
    public Token getHead(){return _tokenList.get(_tokenList.size()-1);}
    public List<Token> getTokenList(){return _tokenList;}
    public List<Chunk> getChunkList(){return _chunkList;}
    public String getChainID(){return _chainID;}
    public String getLexicalType(){return _lexType;}

    /**Returns the token indices of the tokens at the
     * start and end of this mention
     *
     * @return
     */
    public int[] getTokenRange()
    {
        return new int[]{_tokenList.get(0).getIdx(),
                _tokenList.get(_tokenList.size()-1).getIdx()};
    }

    /**Returns a dataset-unique ID for this mention, in the form
     * docID#capIdx;mention:idx
     *
     * @return
     */
    public String getUniqueID()
    {
        return _docID + "#" + _captionIdx + ";mention:" + _idx;
    }

    /**Returns this mention's pronoun type, based on the
     * lemma of the head word (NONE, if this mention
     * is not a pronoun)
     *
     * @return
     */
    public PRONOUN_TYPE getPronounType()
    {
        return PRONOUN_TYPE.parseType(getHead().getLemma());
    }

    /**Returns the gender of this mention, if available; originally
     * used to find the gender of pronouns and candidate antecedents.
     * Optional argument hypernymSet specifies that - in addition to
     * simple matches - male/female should be returned for any
     * mention that has a man/woman hypernym
     *
     * @param hypernymSet
     * @return  male/female/neuter
     */
    public String getGender(Set<String> hypernymSet)
    {
        String gender = getGender();
        if (gender.equals("neuter") && hypernymSet != null) {
            if (hypernymSet.contains("woman"))
                return "female";
            else if (hypernymSet.contains("man"))
                return "male";
        }
        return "neuter";
    }

    /**Returns the gender of this mention, if available; originally
     * used to find the gender of pronouns and candidate antecedents.
     *
     * @return  male/female/neuter
     */
    public String getGender()
    {
        String normLemma = getHead().getLemma().toLowerCase().trim();
        String normText = " " + toString().toLowerCase().trim() + " ";

        //search for the right hypernym, as well as handling our special cases
        if(normLemma.equals("she") || normLemma.equals("her") ||
                normLemma.equals("girl") || normLemma.equals("woman") ||
                normLemma.equals("herself") || normText.contains(" female "))
            return "female";
        else if(normLemma.equals("he") || normLemma.equals("him") ||
                normLemma.equals("boy") || normLemma.equals("man") ||
                normLemma.equals("himself") || normText.contains(" male "))
            return "male";
        return "neuter";
    }

    /**Returns this mention's text
     */
    @Override
    public String toString()
    {
        return StringUtil.listToString(_tokenList, " ");
    }

    /**Returns this mention's attributes as a key:value; string
     *
     * @return  - key-value string of mention attributes
     */
    @Override
    public String toDebugString()
    {
        String[] keys = {"captionIdx", "chainID", "cardinality",
                         "lexicalType", "numTokens", "numChunks"};
        Object[] vals = {_captionIdx, _chainID, _card.toString(),
                         _lexType, _tokenList.size(), _chunkList.size()};
        return StringUtil.toKeyValStr(keys, vals);
    }

    /**Returns whether the lexical types for m1 and m2 match;
     * 1 if exact match, 0 if no match, and 0.5 if they overlap
     * inexactly
     *
     * @param m1
     * @param m2
     * @return
     */
    public static double getLexicalTypeMatch(Mention m1, Mention m2)
    {
        if(m1._lexType.equals(m2._lexType))
            return 1.0;

        Set<String> lex_1 = new HashSet<>(Arrays.asList(m1._lexType.split("/")));
        Set<String> lex_2 = new HashSet<>(Arrays.asList(m2._lexType.split("/")));
        Set<String> intersection = new HashSet<>(lex_1);
        intersection.retainAll(lex_2);
        if(intersection.size() > 0)
            return 0.5;

        return 0.0;
    }

    /**Function to initialize static word lists;
     * Intended to be populated only when needed,
     * as the standard workflow should not have to
     * build mentions from scratch
     */
    public static void initWordLists(String dataRoot)
    {
        _massNouns =
                new HashSet<>(FileIO.readFile_lineList(dataRoot +
                        "massNouns.txt", true));
        _collectiveNouns =
                new HashSet<>(FileIO.readFile_lineList(dataRoot +
                        "collectiveNouns.txt", true));

        String[][] specialNounTable =
                FileIO.readFile_table(dataRoot +
                        "specialCollectiveNouns.csv");
        _specialCollectiveNouns = new HashMap<>();
        for(String[] row : specialNounTable)
            _specialCollectiveNouns.put(row[0], Integer.parseInt(row[1]));
    }

    /**Function to initialize static lexicon dict;
     * Intended to be populated only when needed,
     * as the standard workflow should not have to
     * build mentions from scratch
     */
    public static void initLexiconDict(String lexiconRoot)
    {
        String[] types =
                {"animals", "bodyparts", "clothing",
                        "colors", "instruments", "people",
                        "scene", "vehicles"};
        _lexiconDict = new HashMap<>();
        for(String type : types){
            List<String> lines =
                    FileIO.readFile_lineList(lexiconRoot + type + ".txt");
            for(String line : lines){
                if(_lexiconDict.containsKey(line))
                    _lexiconDict.put(line, _lexiconDict.get(line) + "/" + type);
                else
                    _lexiconDict.put(line, type);
            }
        }
    }

    /**PRONOUN_TYPE enumerates various pronoun types as well as provides
     * static functions for handling them;
     * NOTE: pronoun types are based on internal lists
     */
    public enum PRONOUN_TYPE
    {
        SUBJECTIVE_SINGULAR, SUBJECTIVE_PLURAL, OBJECTIVE_SINGULAR,
        OBJECTIVE_PLURAL, REFLEXIVE_SINGULAR, REFLEXIVE_PLURAL,
        RECIPROCAL, RELATIVE, DEMONSTRATIVE, INDEFINITE, SPECIAL, NONE;

        private static Map<PRONOUN_TYPE, Set<String>> typeWordSetDict;
        private static Set<String> wordSet_sing;
        private static Set<String> wordSet_plural;
        static{
            typeWordSetDict = new HashMap<>();
            wordSet_sing = new HashSet<>();
            wordSet_plural = new HashSet<>();
            for(PRONOUN_TYPE type : PRONOUN_TYPE.values())
                typeWordSetDict.put(type, new HashSet<>());

            //subjective singular - he / she / it
            typeWordSetDict.get(SUBJECTIVE_SINGULAR).add("he");
            typeWordSetDict.get(SUBJECTIVE_SINGULAR).add("she");
            typeWordSetDict.get(SUBJECTIVE_SINGULAR).add("it");
            wordSet_sing.add("he");
            wordSet_sing.add("she");
            wordSet_sing.add("it");

            //subjective plural - they
            typeWordSetDict.get(SUBJECTIVE_PLURAL).add("they");
            wordSet_plural.add("they");

            //objective singular - him / her / it
            typeWordSetDict.get(OBJECTIVE_SINGULAR).add("him");
            typeWordSetDict.get(OBJECTIVE_SINGULAR).add("her");
            typeWordSetDict.get(OBJECTIVE_SINGULAR).add("it");
            wordSet_sing.add("him");
            wordSet_sing.add("her");

            //objective plural - them
            typeWordSetDict.get(OBJECTIVE_PLURAL).add("them");
            wordSet_plural.add("them");

            //reflexive singular - himself / herself / itself / oneself
            typeWordSetDict.get(REFLEXIVE_SINGULAR).add("himself");
            typeWordSetDict.get(REFLEXIVE_SINGULAR).add("herself");
            typeWordSetDict.get(REFLEXIVE_SINGULAR).add("itself");
            typeWordSetDict.get(REFLEXIVE_SINGULAR).add("oneself");
            wordSet_sing.add("himself");
            wordSet_sing.add("herself");
            wordSet_sing.add("itself");
            wordSet_sing.add("oneself");

            //reflexive plural - themselves
            typeWordSetDict.get(REFLEXIVE_PLURAL).add("themselves");
            wordSet_plural.add("themselves");

            //reciprocal - each other / one another
            typeWordSetDict.get(RECIPROCAL).add("each other");
            typeWordSetDict.get(RECIPROCAL).add("one another");
            wordSet_plural.add("each other");
            wordSet_plural.add("one another");

            //relative - that / which / who / whose / whom / where / when
            typeWordSetDict.get(RELATIVE).add("that");
            typeWordSetDict.get(RELATIVE).add("which");
            typeWordSetDict.get(RELATIVE).add("who");
            typeWordSetDict.get(RELATIVE).add("whose");
            typeWordSetDict.get(RELATIVE).add("whom");
            typeWordSetDict.get(RELATIVE).add("where");
            typeWordSetDict.get(RELATIVE).add("when");
            wordSet_sing.add("that");
            wordSet_sing.add("which");
            wordSet_sing.add("who");
            wordSet_sing.add("whose");
            wordSet_sing.add("whom");
            wordSet_sing.add("where");
            wordSet_sing.add("when");

            //demonstrative - this / that / these / those
            typeWordSetDict.get(DEMONSTRATIVE).add("this");
            typeWordSetDict.get(DEMONSTRATIVE).add("that");
            typeWordSetDict.get(DEMONSTRATIVE).add("these");
            typeWordSetDict.get(DEMONSTRATIVE).add("those");
            wordSet_sing.add("this");
            wordSet_sing.add("that");
            wordSet_sing.add("these");
            wordSet_sing.add("those");

            //indefinite - anything / anybody / anyone / something /
            //             somebody / someone / nothing / nobody / one /
            //             none / no one
            typeWordSetDict.get(INDEFINITE).add("anything");
            typeWordSetDict.get(INDEFINITE).add("anybody");
            typeWordSetDict.get(INDEFINITE).add("anyone");
            typeWordSetDict.get(INDEFINITE).add("something");
            typeWordSetDict.get(INDEFINITE).add("somebody");
            typeWordSetDict.get(INDEFINITE).add("someone");
            typeWordSetDict.get(INDEFINITE).add("nothing");
            typeWordSetDict.get(INDEFINITE).add("nobody");
            typeWordSetDict.get(INDEFINITE).add("one");
            typeWordSetDict.get(INDEFINITE).add("none");
            typeWordSetDict.get(INDEFINITE).add("no one");
            wordSet_plural.add("anything");
            wordSet_plural.add("anybody");
            wordSet_plural.add("anyone");
            wordSet_sing.add("something");
            wordSet_sing.add("somebody");
            wordSet_sing.add("someone");
            wordSet_sing.add("one");
        }

        /**Returns whether given string s is a plural pronoun, false if
         * a singular pronoun, and null if s is not a pronoun
         *
         * @param s
         * @return
         */
        public static Boolean getIsPlural(String s)
        {
            s = s.toLowerCase().trim();
            if(wordSet_plural.contains(s))
                return true;
            if(wordSet_sing.contains(s))
                return false;
            return null;
        }

        /**Returns a PRONOUN_TYPE, given string s; returns type NONE if s is
         * not a known pronoun
         *
         * @param s
         * @return
         */
        public static PRONOUN_TYPE parseType(String s)
        {
            String normText = s.toLowerCase().trim();

            //since it and that don't have unique types, set them as special
            if(normText.equals("it") || normText.equals("that"))
                return SPECIAL;

            for(PRONOUN_TYPE t : PRONOUN_TYPE.values())
                if(typeWordSetDict.get(t).contains(normText))
                    return t;
            return NONE;
        }

        /**Returns true when this can refer to an animate object
         *
         * @return
         */
        public boolean isAnimate()
        {
            return this == SUBJECTIVE_SINGULAR ||
                    this == SUBJECTIVE_PLURAL ||
                    this == OBJECTIVE_SINGULAR ||
                    this == OBJECTIVE_PLURAL ||
                    this == REFLEXIVE_SINGULAR ||
                    this == REFLEXIVE_PLURAL ||
                    this == RELATIVE ||
                    this == SPECIAL;
        }
    }
}
