package structures;

import utilities.FileIO;
import utilities.Logger;
import utilities.StringUtil;

import java.util.*;

/**Mention objects are functionally noun-phrase Chunks with additional
 * attributes of interest. However, we cannot fold this
 * functionality into Chunk nor extend Chunk because
 * XofY constructions are three chunks grouped together.
 * To preserve chunk boundaries, we must therefore
 * have Mentions, which _contain_, but are not themselves,
 * NP chunks.
 *
 * @author ccervantes
 */
public class Mention extends Annotation
{
    private static Set<String> _numerics;
    static{
        String[] numericArr = {"one", "two", "three",
                "four", "five", "six", "seven", "eight",
                "nine", "ten", "eleven", "twelve", "few",
                "group", "number", "various", "couple",
                "multiple", "several", "single", "first",
                "second", "third", "numerous", "lot", "1",
                "2", "3", "lots"};
        _numerics = new HashSet<>(Arrays.asList(numericArr));
    }

    //Lexicon dict, initialized with initializeDict
    private static Map<String, String> _flickr30kLexicon;
    private static Map<String, Set<String>> _cocoLexicon;
    private static Map<String, Set<String>> _cocoLexicon_relaxed;
    private static Map<String, String> _supercategoryDict;

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
        if(tokenList.isEmpty())
            Logger.log(new Exception("No tokens found for doc:" +
                       docID + ";cap:" + captionIdx + ";idx:" +idx));

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
        if(card == null)
            _card = Cardinality.parseCardinality(_tokenList);
        else
            _card = card;
    }

    /**Initializes this mention's lexical type, assuming the static
     * lexiconDict has been initialized already
     */
    private void initLexicalType() {
        String type = "other";

        //Handle non-pronominal mentions and pronouns
        //differently
        if(this.getPronounType() == PRONOUN_TYPE.NONE){
            //In order to distinguish between "buffalo" and "water buffalo"
            //(though, admittedly, they're the same type), we want to
            //get the lexical type for the longest string that
            //  - is in the lexicon
            //  - terminates the mention
            int matchLength = -1;
            List<String> lemmas = new ArrayList<>();
            _tokenList.stream().forEachOrdered(t -> lemmas.add(t.getLemma()));
            String lemmaStr = StringUtil.listToString(lemmas, " ");

            for (String s : _flickr30kLexicon.keySet()) {
                if (s.length() >= matchLength && lemmaStr.endsWith(s)) {
                    type = _flickr30kLexicon.get(s);
                    matchLength = s.length();
                }
            }
        } else {
            switch(this.getPronounType()){
                case SUBJECTIVE_SINGULAR:
                case SUBJECTIVE_PLURAL:
                case OBJECTIVE_SINGULAR:
                case OBJECTIVE_PLURAL:
                case REFLEXIVE_SINGULAR:
                case REFLEXIVE_PLURAL:
                case RECIPROCAL:
                    type = "people/animals";
            }
            String normText = this.toString().toLowerCase();
            if(normText.equals("who") || normText.equals("whom") ||
               normText.equals("somebody") || normText.equals("someone")){
                type = "people";
            }
        }
        _lexType = type;
    }

    /* Getters & Setters */
    public int getCaptionIdx(){return _captionIdx;}
    public Cardinality getCardinality(){return _card;}
    public Token getHead(){return _tokenList.get(_tokenList.size()-1);}
    public List<Token> getTokenList(){return _tokenList;}
    public List<Chunk> getChunkList(){return _chunkList;}
    public String getChainID(){return _chainID;}
    public String getLexicalType(){return _lexType;}
    public void setChainID(String chainID)
    {
        _chainID = chainID;
        for(Token t : _tokenList)
            t.chainID = _chainID;
    }

    /**Sets the lexical type of this mention
     * DEPRECATED: lexical types should not be set in
     * this way; this is being added to deal with an issue
     * with the MSCOCO database
     *
     * @param lexType
     */
    @Deprecated
    public void setLexicalType(String lexType)
    {
        _lexType = lexType;
    }

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
     * text; if none is found, attempts to determine
     * pronoun type based on head lemma alone
     * (NONE, if this mention is not a pronoun)
     *
     * @return
     */
    public PRONOUN_TYPE getPronounType()
    {
        PRONOUN_TYPE pronomType = PRONOUN_TYPE.parseType(toString().toLowerCase());
        if(pronomType == PRONOUN_TYPE.NONE)
            pronomType = PRONOUN_TYPE.parseType(getHead().getLemma());
        return pronomType;
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
    public String getGender(Collection<String> hypernymSet)
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
                normLemma.equals("herself") || normText.contains(" female ") ||
                normLemma.equals("lady"))
            return "female";
        else if(normLemma.equals("he") || normLemma.equals("him") ||
                normLemma.equals("boy") || normLemma.equals("man") ||
                normLemma.equals("himself") || normText.contains(" male ") ||
                normLemma.equals("guy"))
            return "male";
        else if(_tokenList.size() > 1 && normText.contains(" her "))
            return "female";
        else if(_tokenList.size() > 1 && normText.contains(" his "))
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

    /**Returns this mention's modifier strings as an
     * {numeric, other} pair. Modifiers are non-terminal
     * tokens with POS tags other than DT (determiner) or
     * IN (preposition). Numeric consist of tokens in
     * a list, corresponding to various numerals and
     * quantifiers
     *
     * @return This mentions modifiers, as a {numeric, other}
     *         pair
     */
    public String[] getModifiers()
    {
        String mod_numeric = "", mod_other = "";
        for(int i=0; i<_tokenList.size() - 1; i++){
            Token t = _tokenList.get(i);
            if(!t.getPosTag().equals("DT") && !t.getPosTag().equals("IN")){
                String mod = StringUtil.keepAlphaNum(t.toString()).toLowerCase();
                if(_numerics.contains(mod))
                    mod_numeric += mod;
                else
                    mod_other += mod;
            }
        }
        return new String[]{mod_numeric, mod_other};
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

    /**Initializes static lexicon dictionaries; intended to be
     * populated only when needed as the standard workflow
     * should not have to build mentions from scratch
     *
     * @param cocoLexFile
     * @param flickr30kLexiconDir
     */
    public static void initializeLexicons(String flickr30kLexiconDir, String cocoLexFile)
    {
        _flickr30kLexicon = new HashMap<>();
        _cocoLexicon = new HashMap<>();
        _cocoLexicon_relaxed = new HashMap<>();
        _supercategoryDict = new HashMap<>();

        //Load the flickr30k lexicon
        if(flickr30kLexiconDir != null){
            String[] types = {"animals", "bodyparts", "clothing",
                    "colors", "instruments", "people",
                    "scene", "vehicles"};
            Map<String, Set<String>> lemmaTypeSetDict = new HashMap<>();
            for(String type : types){
                List<String> lineList = FileIO.readFile_lineList(flickr30kLexiconDir + type + ".txt");
                for(String lemma : lineList){
                    if(!lemmaTypeSetDict.containsKey(lemma))
                        lemmaTypeSetDict.put(lemma, new HashSet<>());
                    lemmaTypeSetDict.get(lemma).add(type);
                }
            }
            _flickr30kLexicon = new HashMap<>();
            for(String lemma : lemmaTypeSetDict.keySet()){
                List<String> typeList = new ArrayList<>(lemmaTypeSetDict.get(lemma));
                Collections.sort(typeList);
                _flickr30kLexicon.put(lemma, StringUtil.listToString(typeList, "/"));
            }
        }

        //Load the coco lexicon file
        if(cocoLexFile != null){
            String[][] cocoLexTable = FileIO.readFile_table(cocoLexFile);
            for(String[] row : cocoLexTable){
                String cat = row[0];
                String[] heads = row[1].split("\\|");
                String[] fallbacks = row[2].split("\\|");
                String superCat = row[3];
                _cocoLexicon.put(cat, new HashSet<>(Arrays.asList(heads)));
                _cocoLexicon_relaxed.put(cat, new HashSet<>(Arrays.asList(fallbacks)));
                _supercategoryDict.put(cat, superCat);
            }
        }
    }

    /**Returns the lexical type entry for the given lemma;
     * returns less-precise results than the version
     * that takes a mention; returns 'other' if the lemma
     * is not found; returns multiple types deliniated
     * with /
     *
     * @param lemma
     * @return
     */
    public static String getLexicalEntry_flickr(String lemma)
    {
        if(_flickr30kLexicon.containsKey(lemma))
            return _flickr30kLexicon.get(lemma);
        return "other";
    }

    /**Returns the lexical type entry for the given mention;
     * returns 'other' if the head word(s) is not found;
     * returns multiple types deliniated with /
     *
     * @param m
     * @return
     */
    public static String getLexicalEntry_flickr(Mention m)
    {
        List<Token> toks = m.getTokenList();
        String head = toks.get(toks.size()-1).getLemma().toLowerCase();
        String lastTwo = "";
        if(toks.size() > 1)
            lastTwo = toks.get(toks.size()-2).getLemma() + " ";
        lastTwo += toks.get(toks.size()-1).getLemma();
        lastTwo = lastTwo.toLowerCase();

        if(_flickr30kLexicon.containsKey(lastTwo))
            return _flickr30kLexicon.get(lastTwo);
        else if(_flickr30kLexicon.containsKey(head))
            return _flickr30kLexicon.get(head);
        return "other";
    }

    /**Returns the lexical type entry for the given lemma
     * according to the MSCOCO lexicons; returns a less
     * precise results than the version that takes a mention;
     * returns null if the lemma is not found; returns multiple
     * categories deliniated with /
     *
     * @param lemma
     * @param usedRelaxed
     * @return
     */
    public static String getLexicalEntry_cocoCategory(String lemma, boolean usedRelaxed)
    {
        Set<String> categories = new HashSet<>();
        for(String category : _cocoLexicon.keySet()) {
            if (_cocoLexicon.get(category).contains(lemma))
                categories.add(category);
            else if(usedRelaxed && _cocoLexicon_relaxed.get(category).contains(lemma))
                categories.add(category);
        }

        //Treat anything in our people lexicon as a fallback person category
        if(usedRelaxed && getLexicalEntry_flickr(lemma).contains("people"))
            categories.add("person");

        //Treat person-pronouns as people
        if(PRONOUN_TYPE.getIsPerson(lemma))
            categories.add("person");

        if(categories.isEmpty())
            return null;
        List<String> categoryList = new ArrayList<>(categories);
        Collections.sort(categoryList);
        return StringUtil.listToString(categoryList, "/");
    }

    /**Returns the lexical type entry for the given mention
     * according to the mscoco lexicons; returns null if
     * the head word(s) is not found; returns multiple
     * types deliniated with /
     *
     * @param m
     * @param usedRelaxed
     * @return
     */
    public static String getLexicalEntry_cocoCategory(Mention m, boolean usedRelaxed)
    {
        List<Token> toks = m.getTokenList();
        String head = toks.get(toks.size()-1).getLemma().toLowerCase();
        String lastTwo = "";
        if(toks.size() > 1)
            lastTwo = toks.get(toks.size()-2).getLemma() + " ";
        lastTwo += toks.get(toks.size()-1).getLemma();
        lastTwo = lastTwo.toLowerCase();

        Set<String> categories = new HashSet<>();
        for(String category : _cocoLexicon.keySet()) {
            if (_cocoLexicon.get(category).contains(head) ||
                _cocoLexicon.get(category).contains(lastTwo))
                categories.add(category);
            else if(usedRelaxed && (_cocoLexicon_relaxed.get(category).contains(head) ||
                _cocoLexicon_relaxed.get(category).contains(lastTwo)))
                categories.add(category);
        }

        //Treat anything in our people lexicon as a fallback person category
        if(usedRelaxed && getLexicalEntry_flickr(m).contains("people"))
            categories.add("person");

        //Treat person-pronouns as people
        if(PRONOUN_TYPE.getIsPerson(m.toString()))
            categories.add("person");

        if(categories.isEmpty())
            return null;
        List<String> categoryList = new ArrayList<>(categories);
        Collections.sort(categoryList);
        return StringUtil.listToString(categoryList, "/");
    }

    /**Returns the coco category for a mention under the default assumption
     * that we're using the relaxed lexicon
     *
     * @param m
     * @return
     */
    public static String getLexicalEntry_cocoCategory(Mention m)
    {
        return getLexicalEntry_cocoCategory(m, true);
    }

    /**Returns the MSCOCO supercategory, given a category
     *
     * @param category
     * @return
     */
    public static String getSuperCategory(String category)
    {
        if(category == null)
            return null;

        Set<String> superCats = new HashSet<>();
        for(String cat : category.split("/"))
            superCats.add(_supercategoryDict.get(cat));
        List<String> superCatList = new ArrayList<>(superCats);
        Collections.sort(superCatList);
        return StringUtil.listToString(superCatList, "/");
    }

    /**Returns the MSCOCO cateogires
     *
     * @return
     */
    public static Set<String> getCOCOCategories() {return _cocoLexicon.keySet();}

    /**Returns the MSCOCO supercategories
     *
     * @return
     */
    public static Set<String> getCOCOSupercategories() {return new HashSet<>(_supercategoryDict.values());}

    /**PRONOUN_TYPE enumerates various pronoun types as well as provides
     * static functions for handling them;
     * NOTE: pronoun types are based on internal lists
     */
    public enum PRONOUN_TYPE
    {
        SUBJECTIVE_SINGULAR, SUBJECTIVE_PLURAL, OBJECTIVE_SINGULAR,
        OBJECTIVE_PLURAL, REFLEXIVE_SINGULAR, REFLEXIVE_PLURAL,
        RECIPROCAL, RELATIVE, DEMONSTRATIVE, INDEFINITE, DEICTIC,
        OTHER, NONE;

        private static Map<PRONOUN_TYPE, Set<String>> _typeWordSetDict;
        private static Set<String> _wordSet_sing;
        private static Set<String> _wordSet_plural;
        private static Set<String> _wordSet_people;
        static{
            _typeWordSetDict = new HashMap<>();
            _wordSet_sing = new HashSet<>();
            _wordSet_plural = new HashSet<>();
            _wordSet_people = new HashSet<>();
            for(PRONOUN_TYPE type : PRONOUN_TYPE.values())
                _typeWordSetDict.put(type, new HashSet<>());

            //subjective singular - he / she / it
            _typeWordSetDict.get(SUBJECTIVE_SINGULAR).add("he");
            _typeWordSetDict.get(SUBJECTIVE_SINGULAR).add("she");
            _typeWordSetDict.get(SUBJECTIVE_SINGULAR).add("it");
            _wordSet_sing.add("he");
            _wordSet_sing.add("she");
            _wordSet_sing.add("it");
            _wordSet_people.add("he");
            _wordSet_people.add("she");

            //subjective plural - they
            _typeWordSetDict.get(SUBJECTIVE_PLURAL).add("they");
            _wordSet_plural.add("they");
            _wordSet_people.add("they");

            //objective singular - him / her / it
            _typeWordSetDict.get(OBJECTIVE_SINGULAR).add("him");
            _typeWordSetDict.get(OBJECTIVE_SINGULAR).add("her");
            _typeWordSetDict.get(OBJECTIVE_SINGULAR).add("it");
            _wordSet_sing.add("him");
            _wordSet_sing.add("her");
            _wordSet_people.add("him");
            _wordSet_people.add("her");

            //objective plural - them
            _typeWordSetDict.get(OBJECTIVE_PLURAL).add("them");
            _wordSet_plural.add("them");
            _wordSet_people.add("them");

            //reflexive singular - himself / herself / itself / oneself
            _typeWordSetDict.get(REFLEXIVE_SINGULAR).add("himself");
            _typeWordSetDict.get(REFLEXIVE_SINGULAR).add("herself");
            _typeWordSetDict.get(REFLEXIVE_SINGULAR).add("itself");
            _typeWordSetDict.get(REFLEXIVE_SINGULAR).add("oneself");
            _wordSet_sing.add("himself");
            _wordSet_sing.add("herself");
            _wordSet_sing.add("itself");
            _wordSet_sing.add("oneself");
            _wordSet_people.add("himself");
            _wordSet_people.add("herself");

            //reflexive plural - themselves
            _typeWordSetDict.get(REFLEXIVE_PLURAL).add("themselves");
            _wordSet_plural.add("themselves");
            _wordSet_people.add("themselves");

            //reciprocal - each other / one another
            _typeWordSetDict.get(RECIPROCAL).add("each other");
            _typeWordSetDict.get(RECIPROCAL).add("each");
            _typeWordSetDict.get(RECIPROCAL).add("one another");
            _wordSet_plural.add("each other");
            _wordSet_plural.add("one another");
            _wordSet_plural.add("each");

            //relative - that / which / who / whose / whom / where / when
            _typeWordSetDict.get(RELATIVE).add("that");
            _typeWordSetDict.get(RELATIVE).add("which");
            _typeWordSetDict.get(RELATIVE).add("who");
            _typeWordSetDict.get(RELATIVE).add("whose");
            _typeWordSetDict.get(RELATIVE).add("whom");
            _typeWordSetDict.get(RELATIVE).add("where");
            _typeWordSetDict.get(RELATIVE).add("when");
            _typeWordSetDict.get(RELATIVE).add("what");
            _wordSet_sing.add("that");
            _wordSet_sing.add("which");
            _wordSet_sing.add("who");
            _wordSet_sing.add("whose");
            _wordSet_sing.add("whom");
            _wordSet_sing.add("where");
            _wordSet_sing.add("when");
            _wordSet_sing.add("what");
            _wordSet_people.add("who");
            _wordSet_people.add("whom");

            //demonstrative - this / that / these / those
            _typeWordSetDict.get(DEMONSTRATIVE).add("this");
            _typeWordSetDict.get(DEMONSTRATIVE).add("that");
            _typeWordSetDict.get(DEMONSTRATIVE).add("these");
            _typeWordSetDict.get(DEMONSTRATIVE).add("those");
            _typeWordSetDict.get(DEMONSTRATIVE).add("there");
            _wordSet_sing.add("this");
            _wordSet_sing.add("that");
            _wordSet_sing.add("there");
            _wordSet_plural.add("these");
            _wordSet_plural.add("those");


            //indefinite - anything / anybody / anyone / something /
            //             somebody / someone / nothing / nobody / one /
            //             none / no one
            _typeWordSetDict.get(INDEFINITE).add("anything");
            _typeWordSetDict.get(INDEFINITE).add("anybody");
            _typeWordSetDict.get(INDEFINITE).add("anyone");
            _typeWordSetDict.get(INDEFINITE).add("something");
            _typeWordSetDict.get(INDEFINITE).add("somebody");
            _typeWordSetDict.get(INDEFINITE).add("someone");
            _typeWordSetDict.get(INDEFINITE).add("nothing");
            _typeWordSetDict.get(INDEFINITE).add("nobody");
            _typeWordSetDict.get(INDEFINITE).add("none");
            _typeWordSetDict.get(INDEFINITE).add("no one");
            _wordSet_plural.add("anything");
            _wordSet_plural.add("anybody");
            _wordSet_plural.add("anyone");
            _wordSet_sing.add("something");
            _wordSet_sing.add("somebody");
            _wordSet_sing.add("someone");
            _wordSet_people.add("somebody");
            _wordSet_people.add("someone");

            //semi-pronouns introduce new elements to the caption
            _typeWordSetDict.get(DEICTIC).add("another");
            _typeWordSetDict.get(DEICTIC).add("other");
            _typeWordSetDict.get(DEICTIC).add("others");
            _typeWordSetDict.get(DEICTIC).add("one");
            _typeWordSetDict.get(DEICTIC).add("two");
            _typeWordSetDict.get(DEICTIC).add("three");
            _typeWordSetDict.get(DEICTIC).add("four");
            _typeWordSetDict.get(DEICTIC).add("some");
            _wordSet_sing.add("another");
            _wordSet_sing.add("other");
            _wordSet_plural.add("others");
            _wordSet_sing.add("one");
            _wordSet_plural.add("two");
            _wordSet_plural.add("three");
            _wordSet_plural.add("four");
            _wordSet_plural.add("some");

            //special pronouns are special
            _typeWordSetDict.get(OTHER).add("both");
            _typeWordSetDict.get(OTHER).add("all");
            _wordSet_plural.add("both");
            _wordSet_plural.add("all");
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
            if(_wordSet_plural.contains(s))
                return true;
            if(_wordSet_sing.contains(s))
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
            if(s != null){
                String normText = s.toLowerCase().trim();

                //since it and that don't have unique types, set them as special
                if(normText.equals("it") || normText.equals("that"))
                    return OTHER;

                for(PRONOUN_TYPE t : PRONOUN_TYPE.values())
                    if(_typeWordSetDict.get(t).contains(normText))
                        return t;
            }
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
                    this == RELATIVE || this == DEICTIC ||
                    this == OTHER;
        }

        /**Returns whether the given string is a pronoun
         * for which we have reasonable confidence refers
         * to a person (he, she, they, etc.)
         *
         * @param s
         * @return
         */
        public static boolean getIsPerson(String s)
        {
            s = s.toLowerCase().trim();
            return _wordSet_people.contains(s);
        }
    }
}
