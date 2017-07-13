package structures;

import utilities.FileIO;
import utilities.Logger;
import utilities.StringUtil;
import utilities.Util;

import java.io.Serializable;
import java.util.*;

/**The Cardinality class encapsulates the notion
 * of mention cardinality, which must distinguish
 * between concrete values (2) and ambiguous
 * values (more than 1).
 *
 * @author ccervantes
 */
public class Cardinality implements Serializable
{
    private static Set<String> _articles;
    private static Set<String> _quantifiers;
    private static Set<String> _prps;
    private static Set<String> _nums;
    private static Set<String> _nonvisHeads;
    private static Set<String> _singArtifacts;
    private static Set<String> _masses;
    private static Set<String> _collectives;
    private static Set<String> _portions;
    private static Map<String, Integer> _collectives_kv;
    private static Map<String, Integer> _quantifiers_kv;

    private int[] _baseValues;
    private boolean[] _underdef;
    private boolean _isMass;
    private boolean _isNull;

    /**Default Cardinality constructor, where both the base
     * values and the ambiguity values are provides for sets and
     * elements
     *
     * @param baseValues
     * @param underdef
     */
    public Cardinality(int[] baseValues, boolean[] underdef)
    {
        _baseValues = baseValues;
        _underdef = underdef;
        _isMass = false;
        _isNull = false;
    }

    /**Cardinality constructor used for masses and
     * elements without cardinalities
     */
    public Cardinality(boolean isMass)
    {
        _isMass = isMass;
        _isNull = !isMass;
        _baseValues = new int[]{-1,-1};
        _underdef = new boolean[]{true, true};
    }

    /**Cardinality constructor that accepts a cardinality
     * string, in the form
     *      T|U
     * where both T and U may optionally be followed by "+" to
     * indicate ambiguity
     *
     * @param s
     */
    public Cardinality(String s) throws Exception
    {
        if(s == null){
            _isMass = false;
            _isNull = true;
            _baseValues = new int[]{-1,-1};
            _underdef = new boolean[]{true, true};
        } else if(s.equals("m")) {
            _isMass = true;
            _isNull = false;
            _baseValues = new int[]{-1,-1};
            _underdef = new boolean[]{false, false};
        } else if(s.equals("null")){
            _isMass = false;
            _isNull = true;
            _baseValues = new int[]{-1,-1};
            _underdef = new boolean[]{true, true};
        } else {
            String[] arr = s.split("\\|");
            boolean setAmbig = arr[0].contains("+");
            boolean elemAmbig = arr[1].contains("+");
            int setValue = Integer.parseInt(arr[0].replace("+", ""));
            int elemValue = Integer.parseInt(arr[1].replace("+", ""));
            _baseValues = new int[]{setValue, elemValue};
            _underdef = new boolean[]{setAmbig, elemAmbig};
            _isMass = false; _isNull = false;
        }
    }

    /**Returns this Cardinality as a string, in the form
     *  T|U
     * where both T and U may be followed by a "+", indicating
     * ambiguity
     */
    @Override
    public String toString()
    {
        if(_isMass)
            return "m";
        else if(_isNull)
            return "null";

        StringBuilder sb = new StringBuilder();
        sb.append(_baseValues[0]);
        if(_underdef[0])
            sb.append("+");
        sb.append("|");
        sb.append(_baseValues[1]);
        if(_underdef[1])
            sb.append("+");
        return sb.toString();
    }

    /**Returns whether this Cardinality refers to an underdefined
     * quantity
     *
     * @return
     */
    public boolean isUnderdef()
    {
        return _underdef[0] || _underdef[1];
    }

    /**Returns either the value of this Cardinality (if it isn't
     * ambiguous) or the value representing this Cardinality's
     * lower bound (if it is)
     *
     * @return
     */
    public int getValue()
    {
        //The value of any T sets of U elements Cardinality is
        //T * U, where T and U are incremented if ambiguous
        //ex.   "Two teams" = (2,1+) = lower bound of 4
        int T = _baseValues[0];
        if(_underdef[0])
            T++;
        int U = _baseValues[1];
        if(_underdef[1])
            U++;

        return T * U;
    }

    /**Loads the static cardinality lists; in most instances these
     * aren't needed (cards should be loaded from the DB) so an
     * on-load static call would needlessly consume memory
     */
    public static void initCardLists(String collectiveFile)
    {
        _articles = new HashSet<>(Arrays.asList(new String[]{"a", "the", "an"}));
        _quantifiers = new HashSet<>(Arrays.asList(
                new String[]{"almost", "at least", "approximately", "about"}));
        _prps = new HashSet<>(Arrays.asList(new String[]{"his", "hers", "its", "their"}));
        _nums = new HashSet<>(Arrays.asList(new String[]{"number ", "no ", "no. ", "#"}));
        _nonvisHeads = new HashSet<>(Arrays.asList(
                new String[]{"year", "inch", "hour", "meter", "kind",
                             "mile", "cent", "day", "seconds", "color", "sort", "type"}));
        _singArtifacts = new HashSet<>(Arrays.asList(
                new String[]{"jeans", "trunks", "shorts", "pants", "sunglasses",
                             "glasses", "sweatpants", "overalls", "goggles",
                             "bottoms", "scissors", "stairs", "olympics"}));
        _masses = new HashSet<>(Arrays.asList(
                new String[]{"sand", "snow", "tea", "water","beer", "coffee",
                             "dirt", "corn", "liquid", "wine"}));
        _collectives = new HashSet<>(FileIO.readFile_lineList(collectiveFile, true));
        _portions = new HashSet<>(Arrays.asList(
                new String[]{"pile", "sheet", "puddle", "mound",
                             "spray", "loaf", "cloud", "drink",
                             "sea", "handful", "bale", "line", "row"}));
        _collectives_kv = new HashMap<>();
        _collectives_kv.put("couple", 2);
        _collectives_kv.put("pair", 2);
        _collectives_kv.put("both", 2);
        _collectives_kv.put("either", 2);
        _collectives_kv.put("trio", 3);
        _collectives_kv.put("quartet", 4);
        _collectives_kv.put("dozen", 12);
        _collectives_kv.put("hundred", 100);

        _quantifiers_kv = new HashMap<>();
        _quantifiers_kv.put("several", 3);
        _quantifiers_kv.put("many", 3);
        _quantifiers_kv.put("multiple", 2);
        _quantifiers_kv.put("a few", 2);
        _quantifiers_kv.put("some", 2);
    }

    /**Parses the list of tokens and produces a Cardinality
     *
     * @param tokens
     * @return
     */
    public static Cardinality parseCardinality(List<Token> tokens)
    {
        int setVal, elemVal;
        boolean setUnderdef, elemUnderdef;

        //Read basic info from the token list
        String first = tokens.get(0).toString().toLowerCase();
        Token t_h = tokens.get(tokens.size()-1);
        String h = t_h.toString().toLowerCase();
        String h_lem = t_h.getLemma();
        String h_pos = t_h.getPosTag();

        //if we don't have a lemma or a part of speech, we don't have
        //a cardinality
        if(h_lem == null || h_pos == null)
            return new Cardinality(false);

        h_lem = h_lem.toLowerCase();
        String text = StringUtil.listToString(tokens, " ").toLowerCase().trim();
        boolean singularHead = h_pos.equals("NN") || h_pos.equals("NNP");
        boolean pluralHead = h_pos.equals("NNS") || h_pos.equals("NNPS");
        String text_lem = "";
        for(Token t : tokens)
            text_lem += t.getLemma() + " "; //We intentionally add a trailing
                                            //space, for matching

        //If this text contains "of", we recurse on each part separately
        if(text.contains(" of ")){
            List<Token> xTokens = new ArrayList<>();
            List<Token> yTokens = new ArrayList<>();
            boolean foundOf = false;
            for(int i=0; i<tokens.size(); i++){
                Token t = tokens.get(i);
                if(t.toString().equalsIgnoreCase("of") && i>0 && i<tokens.size()-1)
                    foundOf = true;
                else if(foundOf)
                    yTokens.add(t);
                else
                    xTokens.add(t);
            }
            if(xTokens.isEmpty() || yTokens.isEmpty())
                Logger.log("ERROR: empty tokens; text:%s; xTokens:%s; yTokens:%s",
                        text, StringUtil.listToString(xTokens, "|"),
                        StringUtil.listToString(yTokens, "|"));

            Cardinality xCard = parseCardinality(xTokens);
            Cardinality yCard = parseCardinality(yTokens);

            if(xCard._isNull && !yCard._isNull){
                return yCard;   //some kind of vehicle
            } else if(!xCard._isNull && yCard._isNull){
                return xCard;   //ribbons of various colors
            } else if(xCard.getValue() > 1 && yCard._baseValues[0] == 1 &&
                    yCard._baseValues[1] == 1 && !yCard._underdef[0] &&
                    yCard._underdef[1]){
                return xCard;   //dozens of sheep
            } else if(!xCard._isMass && yCard._isMass){
                //pile of sand
                setVal = xCard._baseValues[1];
                setUnderdef = xCard._underdef[1];
                elemVal = 1;
                elemUnderdef = false;
            } else if(_singArtifacts.contains(h_lem)){
                //pair of scissors
                setVal = 1; setUnderdef = false;
                elemVal = xCard._baseValues[0];
                elemUnderdef = xCard._underdef[0];
            } else {
                //two groups of people
                setVal = xCard._baseValues[0]; setUnderdef = xCard._underdef[0];
                elemVal = yCard._baseValues[1]; elemUnderdef = yCard._underdef[1];
            }
            return new Cardinality(new int[]{setVal, elemVal}, new boolean[]{setUnderdef, elemUnderdef});
        }

        //Determine all list-based facets of this token list
        boolean prefix_article = _articles.contains(first);
        boolean prefix_prp =
                StringUtil.startsWithElement(_prps, text);
        boolean prefix_quant =
                StringUtil.startsWithElement(_quantifiers, text);
        boolean nonvisHead = _nonvisHeads.contains(h_lem);
        boolean singClothingHead = _singArtifacts.contains(h);
        boolean collectiveHead = _collectives.contains(h_lem) ||
                                 _collectives_kv.containsKey(h_lem);
        boolean massHead = _masses.contains(h_lem);
        boolean portionHead = _portions.contains(h_lem);
        Integer quantVal = null;
        for(String q : _quantifiers_kv.keySet()) {
            if (text_lem.contains(q + " ")) {
                quantVal = _quantifiers_kv.get(q);
                prefix_quant = true;
            }
        }
        Integer collectiveVal = null;
        for(String c : _collectives_kv.keySet())
            if(text_lem.contains(c + " "))
                collectiveVal = _collectives_kv.get(c);

        //if the head is a number (as would be the X case in 'a number of Y')
        //then we treat it as a non-numeral collective
        boolean contains_num = false;
        if(_nums.contains(h_lem))
            collectiveHead = true;
        else if(StringUtil.containsElement(_nums, text))
            contains_num = true;

        //Compute the value of the numerals in this text
        List<Integer> valList = new ArrayList<>();
        String[] words = text.replace("-", " - ").split(" ");
        for (int i = 0; i < words.length; i++)
            valList.add(Util.parseInt(words[i]));

        int val = 0;
        for (int i = 0; i < words.length; i++) {
            //In the most naive case, we simply add the
            //values together (given how we've set this up,
            //this method accounts for "twenty two" and
            //"twenty-two")
            if (valList.get(i) != null)
                val += valList.get(i);

            //Handle cases where we have "five to six"
            //or "7-10" (but ignore "thirty-five", which
            //is already captured as 35)
            if (i > 0 && i < words.length - 1) {
                Integer v1 = valList.get(i - 1);
                Integer v2 = valList.get(i + 1);
                if (v1 != null && v2 != null) {
                    if (words[i].equals("to") ||
                       (words[i].equals("-") &&
                       !StringUtil.hasAlphaNum(words[i - 1]))) {
                        //specifying a range like this is effectively
                        //using the "about" quantifier unless we
                        //accounted for upper bounds, but we don't
                        val = v1;
                        prefix_quant = true;
                        break;
                    }
                }
            }
        }
        //Special exception: If the value is ~2000, this is a year,
        //                   not a quantity, so drop it
        if(val > 1900 && val < 2100)
            val = 0;

        //1) In all cases, if this is a nonvisual head,
        //   this has no cardinality
        if(nonvisHead)
            return new Cardinality(false);

        //2) In cases with mass nouns, we want to
        //   distinguish between "water" (mass),
        //   "a water" (single element), and
        //   "the waters" (mass)
        if(massHead)
            if(!prefix_article || pluralHead)
                return new Cardinality(true);

        //3) If we have a collective head, we handle it
        //   differently depending on whether its plural
        //   or is preceded by an article
        if(collectiveHead){
            //Typically, collective heads indicate
            //multiple elements
            setVal = 1; setUnderdef = false;
            elemVal = 1; elemUnderdef = true;

            //3a) If this collective is plural,
            //    it refers to multiple sets
            if(pluralHead){
                setVal = 1; setUnderdef = true;

                if(val > 0){
                    setVal = val; setUnderdef = false;
                }
            }

            //3c) If we have a special collective
            //    value, we set this as the number
            //    of elements ("trios", "two couples")
            if(collectiveVal != null){
                elemVal = collectiveVal;
                elemUnderdef = false;
            }
        }
        //4) If we have a non-collective head, we assume
        //   a single set of a single element
        else {
            setVal = 1; setUnderdef = false;
            elemVal = 1; elemUnderdef = false;

            //4a) We want to distinguish between
            //    "a four wheeler" (single element),
            //    "the five screens" (five elements),
            //    "number four" (no card) and "two people"
            //    (two elements)
            if(val > 0 && !contains_num) {
                if(prefix_article){
                    if(pluralHead){
                        elemVal = val; elemUnderdef = false;
                    }
                } else if(valList.get(valList.size()-1) == null){
                    elemVal = val; elemUnderdef = false;
                }
            }
            else if(quantVal != null){
                elemVal = quantVal;
                elemUnderdef = true;
            }
            else if(collectiveVal != null){
                elemVal = collectiveVal;
                elemUnderdef = false;
            }
            //4b) Various articles of clothing come in
            //    implicit pairs but are a single element;
            //    we want to distinguish these from plurals
            else if(pluralHead && !singClothingHead) {
                elemUnderdef = true;
            }
            //4c) By default, singular heads indicate
            //    one element
            else if(singularHead) {
                elemUnderdef = false;
            }

            //4d) If we have a quantifier (almost) then
            //    we want to decrement our value (if we have one)
            //    and set this as underdefined
            if(prefix_quant) {
                elemUnderdef = true;
                if(elemVal > 1)
                    elemVal--;
            }
        }

        return new Cardinality(new int[]{setVal, elemVal}, new boolean[]{setUnderdef, elemUnderdef});
    }

    /**The Cardinality unify operation, given two Cardinalities
     * c1 and c2, returns the more specific (the Cardinality
     * that refers to a smaller quantity)
     *
     * @param c1
     * @param c2
     * @return
     */
    public static Cardinality unify(Cardinality c1, Cardinality c2)
    {
        if(c1._isMass && c2._isMass)
            return c1;
        else if(c1._isMass)
            return c2;
        else if(c2._isMass)
            return c1;

        //both are ambiguous but we're unifying them anyway
        //(not recommended) take the smaller
        if(!c1.isUnderdef() && !c2.isUnderdef())
            return c1.getValue() < c2.getValue() ? c1 : c2;
        else if(!c1.isUnderdef())
            return c1;
        else if(!c2.isUnderdef())
            return c2;

        //reaching here, we know that both of these Cardinalities
        //are ambiguous. Therefore, we can simply take the larger
        //lower bound (since it refers to a smaller number of elements)
        return c1.getValue() < c2.getValue() ? c2 : c1;
    }

    /**Cardinalities c1 and c2 are said to be approximately equal
     * when
     *  - c1.v = c2.v; if neither card is underdef)
     *  - c1.v <= c2.v; if c1 is underdef and c2 is not
     *  - c2.v <= c1.v; if c2 is underdef and c1 is not
     *  - Both cardinalities are underdefined
     *
     * @param c1
     * @param c2
     * @return
     */
    public static boolean approxEqual(Cardinality c1, Cardinality c2)
    {
        if(c1._isMass || c2._isMass)
            return true;

        if(!c1.isUnderdef() && !c2.isUnderdef())
            return c1.getValue() == c2.getValue();
        else if(c1.isUnderdef() && c2.isUnderdef())
            return true;

        //Reaching here means one is ambiguous and one isn't,
        //so we just have to ensure the ambiguous one's lower bound
        //is less than the unambiguous one's value
        if(c1.isUnderdef())
            return c1.getValue() <= c2.getValue();
        else if(c2.isUnderdef())
            return c2.getValue() <= c1.getValue();

        return false;
    }

    /**Cardinality c1 is said to be less than c2 when
     * - c1.v < c2.v if neither cardinality is underdef
     * - c2 is underdefined
     *
     * @param c1
     * @param c2
     * @return
     */
    public static boolean lessThan(Cardinality c1, Cardinality c2)
    {
        return (c1.getValue() < c2.getValue() &&
               !c1.isUnderdef() && !c2.isUnderdef()) ||
               c2.isUnderdef();
    }
}