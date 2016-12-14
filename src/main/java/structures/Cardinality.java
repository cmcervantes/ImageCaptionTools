package structures;

/**The Cardinality class encapsulates the notion
 * of mention cardinality, which must distinguish
 * between concrete values (2) and ambiguous
 * values (more than 1).
 *
 * @author ccervantes
 */
public class Cardinality
{
    private int[] _baseValues;
    private boolean[] _underdef;
    private boolean _isMass;

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
    }

    /**Cardinality constructor used for mass mentions
     */
    public Cardinality()
    {
        _isMass = true;
        _baseValues = new int[]{-1,-1};
        _underdef = new boolean[]{false, false};
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
        if(s.equals("m")) {
            _isMass = true;
            _baseValues = new int[]{-1,-1};
            _underdef = new boolean[]{false, false};
        } else {
            String[] arr = s.split("\\|");
            boolean setAmbig = arr[0].contains("+");
            boolean elemAmbig = arr[1].contains("+");
            int setValue = Integer.parseInt(arr[0].replace("+", ""));
            int elemValue = Integer.parseInt(arr[1].replace("+", ""));
            _baseValues = new int[]{setValue, elemValue};
            _underdef = new boolean[]{setAmbig, elemAmbig};
            _isMass = false;
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