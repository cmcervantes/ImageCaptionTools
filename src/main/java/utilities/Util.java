package utilities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**The Util class houses static functions that perform
 * various tasks that may, someday, be folded into
 * Java proper, including sorting maps by value,
 * getting a deliniator separated string for a list,
 * or getting a random sample of elements from a collection.
 * NOTE: Given that this project has migrated from Java 6 to 7 to 8,
 * it's possible these things _already_ have been folded into Java proper
 *
 * @author ccervantes
 */
public class Util
{
    private static HashMap<String, Integer> numeralDict;
    static
    {
        numeralDict = new HashMap<>();
        numeralDict.put("one", 1);
        numeralDict.put("two", 2);
        numeralDict.put("three", 3);
        numeralDict.put("four", 4);
        numeralDict.put("five", 5);
        numeralDict.put("six", 6);
        numeralDict.put("seven", 7);
        numeralDict.put("eight", 8);
        numeralDict.put("nine", 9);
        numeralDict.put("ten", 10);
        numeralDict.put("eleven", 11);
        numeralDict.put("twelve", 12);
        numeralDict.put("thirteen", 13);
        numeralDict.put("fourteen", 14);
        numeralDict.put("fifteen", 15);
        numeralDict.put("sixteen", 16);
        numeralDict.put("seventeen", 17);
        numeralDict.put("eighteen", 18);
        numeralDict.put("nineteen", 19);
        numeralDict.put("twenty", 20);
        numeralDict.put("thirty", 30);
        numeralDict.put("forty", 40);
        numeralDict.put("fifty", 50);
        numeralDict.put("sixty", 60);
        numeralDict.put("seventy", 70);
        numeralDict.put("eighty", 80);
        numeralDict.put("ninety", 90);
    }

    /**This function takes a mapping [K -> V] (<b>map</b>)
     * and returns the inverse [V -> list(K)]
     *
     * @param map - A mapping of keys to values
     * @return	  - A mapping of values to lists of keys
     */
    public static <K,V> Map<V, Set<K>> invertMap(Map<K, V> map)
    {
        Map<V, Set<K>> invertedMap = new HashMap<>();

        for(K key : map.keySet())
        {
            V val = map.get(key);
            if(!invertedMap.containsKey(val))
                invertedMap.put(val, new HashSet<>());
            invertedMap.get(val).add(key);
        }
        return invertedMap;
    }

    /**Returns the cosine similarity between <b>vec1</b> and <b>vec2</b>,
     * computed as A*B/(sqrt(A*A)*sqrt(B*B))
     *
     * @param vec1  - Vector 1
     * @param vec2  - Vector 2
     * @return      - The cosine similarity between the vectors
     */
    public static double
        cosineSimilarity(List<Double> vec1, List<Double> vec2)
    {
        if(vec1 == null || vec2 == null || vec1.size() != vec2.size())
            return 0;

        //get the vector norms
        double vecNorm_1 = 0.0;
        for(double d : vec1)
            vecNorm_1 += getDoubleProd(d, d);
        vecNorm_1 = Math.sqrt(vecNorm_1);

        double vecNorm_2 = 0.0;
        for(double d : vec2)
            vecNorm_2 += getDoubleProd(d, d);
        vecNorm_2 = Math.sqrt(vecNorm_2);

        //get the cross product between vectors 1 and 2
        double crossProd = 0;
        for(int i=0; i<vec1.size(); i++)
            crossProd += getDoubleProd(vec1.get(i), vec2.get(i));

        return crossProd / (vecNorm_1 * vecNorm_2);
    }

    /**Returns the product of a and b, computed in log space
     * to prevent underflow
     *
     * @param a
     * @param b
     * @return  Product of a and b
     */
    public static double getDoubleProd(double a, double b)
    {
        //if one of these is a 0, return 0
        if(a == 0 || b == 0)
            return 0;

        //Keep a sign bit to return the right signed value, since
        //the sign doesn't matter, as such, until the end, and we're
        //only converting things to log space for underflow reasons
        double signBit = 1;
        if(a < 0){
            a *= -1;
            signBit *= -1;
        }
        if(b < 0){
            b *= -1;
            signBit *= -1;
        }

        //convert both into logs, and then exponentiate their sum
        return signBit * Math.exp(Math.log(a) + Math.log(b));
    }

    /**Returns the current date and time, in the given <b>format</b>
     *
     * @param format In the SimpleDateFormat, as in "yyyy-MM-dd HH:mm:ss"
     * @return  The current date and time
     */
    public static String getCurrentDateTime(String format)
    {
        DateFormat dateFormat = new SimpleDateFormat(format);
        Date date = new Date();
        return dateFormat.format(date);
    }

    /**Returns the date and time, shifted by <b>shiftNumDays</b>, in
     * the format specified by <b>format</b>
     *
     * @param format       - In the SimpleDateFormat, as in "yyyy-MM-dd HH:mm:ss"
     * @param shiftNumDays - The number of days by which to shift the returned date
     *                       (positive for future, negative for past)
     * @return  The specified date and time
     */
    public static String getDateTime(String format, int shiftNumDays)
    {
        DateFormat dateFormat = new SimpleDateFormat(format);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, shiftNumDays);
        return dateFormat.format(calendar.getTime());
    }

    /**Tries to parse a boolean from <b>s</b>, extending
     * the Boolean.parseBoolean() functionality to
     * recognize 0 and 1 as booleans
     *
     * @param s - string to parse
     * @return  - boolean for <b>s</b> (null if
     * 			  <b>s</b> isn't a boolean)
     */
    public static Boolean parseBoolean(String s)
    {
        Boolean b = null;
        if(s != null)
        {
            String normS = s.trim().toLowerCase();
            if(normS.equals("1")) {
                b = true;
            } else if(normS.equals("0")) {
                b = false;
            } else {
                try {
                    b = Boolean.parseBoolean(normS);
                } catch(Exception ex){/*do nothing and fall through */}
            }
        }
        return b;
    }

    /**Tries to parse an int from <b>s</b>, extending the
     * Integer.parseInt() functionality to recognize
     * numerals (ie. "two") as integers
     *
     * @param s - string to parse
     * @return  - int for <b>s</b> (null if <b>s</b> isn't a number)
     */
    public static Integer parseInt(String s)
    {
        Integer i = null;
        if(s != null && !s.isEmpty()) {
            s = s.toLowerCase().trim();
            try {//parse digits
                i = Integer.parseInt(s);
            } catch(Exception ex) {
                //parsing digits failed, so try finding the numeral
                //in the dict (returning null if it isn't present)
                i = numeralDict.get(s);
            }
        }
        return i;
    }

    /**Casts given object as a string
     *
     * @param o
     * @return
     */
    @Deprecated
    public static String castString(Object o)
    {
        String s;
        if(o == null)
            s = null;
        else
            s = o.toString();
        return s;
    }

    /**Casts given object as a boolean, where
     * true Booleans, numbers equal to 1, and the string
     * "true" return true
     *
     * @param o
     * @return
     */
    public static Boolean castBoolean(Object o)
    {
        Boolean b = null;
        if(o != null) {
            if(o instanceof Boolean)
                b = (Boolean)o;
            else if(o instanceof Integer)
                b = ((Integer)o) == 1;
            else if(o instanceof Double)
                b = ((Double)o) == 1.0;
            else if(o instanceof String)
                b = ((String)o).trim().equalsIgnoreCase("true");
        }
        return b;
    }

    /**Casts the given object as an integer,
     * where booleans are treated as 1 and 0
     *
     * @param o
     * @return
     */
    public static Integer castInteger(Object o)
    {
        Integer i = null;
        if(o != null){
            if(o instanceof Integer)
                i = (Integer)o;
            else if(o instanceof Boolean)
                i = (Boolean)o ? 1 : 0;
            else if(o instanceof String)
                try{
                    i = Integer.parseInt((String)o);
                } catch (Exception ex) {}
        }
        return i;
    }

    /**Returns a list containing <b>numElements</b> elements, randomly
     * selected from <b>coll</b>.
     *
     * @param coll		  - The original collection of elements
     * @param numElements - The number of random elements to choose
     * @return			  - A sub-list of random elements form <b>coll</b>
     */
    public static <T> List<T> getRandomElements(Collection<T> coll, int numElements)
    {
        //put everything in a list, so we have an order
        //to randomize
        ArrayList<T> list = new ArrayList<>();
        for(T t : coll)
            list.add(t);

        //shuffle the list
        Collections.shuffle(list);

        //return the first <numElements> items
        if(numElements > list.size())
        {
            numElements = list.size();
        }
        return list.subList(0, numElements);
    }

    /**Returns <b>map</b> as a LinkedHashMap, sorted in ascending
     * order based on values. Optional param <b>reverse</b>
     * specifies whether the sort should be reserved.
     *
     * @param map     - The HashMap to sort on values
     * @return	      - A LinkedHashMap copy of <b>map</b>,
     * 					sorted by values
     */
    public static <K,V> LinkedHashMap<K,V> sortHashMap(Map<K, V> map)
    {
        return sortHashMap(map, false);
    }

    /**Returns <b>map</b> as a LinkedHashMap, sorted in ascending
     * order based on values. Optional param <b>reverse</b>
     * specifies whether the sort should be reserved.
     *
     * @param map     - The HashMap to sort on values
     * @param reverse - Whether to reverse the sort and switch to
     * 					descending order (false by default)
     * @return	      - A LinkedHashMap copy of <b>map</b>,
     * 					sorted by values
     */
    public static <K,V> LinkedHashMap<K,V> sortHashMap(
            Map<K, V> map, boolean reverse)
    {
        ArrayList<Object[]> sortedKeyValList =
                new ArrayList<>();
        for(K key : map.keySet())
        {
            Comparable value = (Comparable)map.get(key);
            int currentIndex = 0;
            int insertIndex = -1;
            while(currentIndex < sortedKeyValList.size() &&
                    insertIndex < 0)
            {
                //our list is in ascending order, so when
                //we traverse from the beginning, we've found
                //our spot if we're less than or equal to the
                //current element
                Comparable valAtIndex = (Comparable)sortedKeyValList.get(currentIndex)[1];

                if(value.compareTo(valAtIndex) <= 0)
                {
                    insertIndex = currentIndex;
                }
                currentIndex++;
            }
            Object[] keyVal = {key, value};
            if(insertIndex < 0)
            {
                sortedKeyValList.add(keyVal);
            }
            else
            {
                sortedKeyValList.add(insertIndex, keyVal);
            }
        }

        //if we were told to reverse the list, do so
        if(reverse)
        {
            Collections.reverse(sortedKeyValList);
        }

        //add the keys and values to the map in order (so, sorted)
        LinkedHashMap<K, V> sortedMap =
                new LinkedHashMap<K, V>();
        for(Object[] keyVal : sortedKeyValList)
        {
            sortedMap.put((K)keyVal[0], (V)keyVal[1]);
        }
        return sortedMap;
    }

    /**Returns the relative compliment of <b>A</b> in <b>B</b>, or the
     * elements in <b>union</b> that are not in <b>A</b>
     *
     * @param A     - A set of elements to exclude
     * @param B     - An (empty) set of elements that will be populated and
     *                returned (needed for generic return)
     * @param union - The set of all elements
     * @param <T>   - The type of collection
     * @param <U>   - The type of elements in the collection
     * @return      - The populated <b>B</b>, or the set of elements
     *                in <b>union</b> that are not in <b>A</b>
     */
    public static <T extends Collection<U>, U> T
        getRelativeCompliment(T A, T B, T  union)
    {
        for(U u : union)
            if(!A.contains(u))
                B.add(u);
        return B;
    }

    /**Returns whether <b>arr1</b> and <b>arr2</b> intersect
     * (share any common elements)
     *
     * @param arr1 - The first array
     * @param arr2 - The second array
     * @return     - Whether, for any pair of elements <i>a</i>
     * 				 and <i>a'</b>, a.equals(a')
     */
    @Deprecated
    public static <T> boolean hasIntersection(T[] arr1, T[] arr2)
    {
        //TODO: Replace with set intersection?
        for(T a1 : arr1)
            for(T a2 : arr2)
                if(a1.equals(a2))
                    return true;
        return false;
    }

    /**Returns 0 if <b>d</b> is closer to 0, 1 if closer to 1
     *
     * @param d - A double from 0-1
     * @return
     */
    public static int shiftedSignum(double d)
    {
        int sign = (int)Math.signum(d-.5);
        if(sign < 1)
            return 0;
        else
            return 1;
    }

    /**Adds the given vectorList together, element by element
     *
     * @param vectorList
     * @return
     */
    public static List<Double> vectorAdd(List<List<Double>> vectorList)
    {
        List<Double> result = new ArrayList<>(vectorList.get(0));
        for(int i=1; i<vectorList.size(); i++){
            List<Double> vec = vectorList.get(i);
            for(int j=0; j<vec.size(); j++)
                result.set(j, result.get(j) + vec.get(j));
        }
        return result;
    }

    /**Returns the mean of the given vectorList, element by element
     *
     * @param vectorList
     * @return
     */
    public static List<Double> vectorMean(List<List<Double>> vectorList)
    {
        List<Double> result = vectorAdd(vectorList);
        for(int i=0; i<result.size(); i++)
            result.set(i, result.get(i) / vectorList.size());
        return result;
    }

    /**Returns the pointwise multiplication of the given vectorList
     *
     * @param vectorList
     * @return
     */
    public static List<Double> vectorPointwiseMult(List<List<Double>> vectorList)
    {
        List<Double> result = new ArrayList<>(vectorList.get(0));
        for(int i=1; i<vectorList.size(); i++){
            List<Double> vec = vectorList.get(i);
            for(int j=0; j<vec.size(); j++)
                result.set(j, result.get(j) * vec.get(j));
        }
        return result;
    }

    /**Searches the given collection of arrays for a given array,
     * where the elements of the arrays are compared rather than the
     * default contains operation, which only compares the array pointers
     * themselves
     *
     * @param coll
     * @param arr
     * @param <T>
     * @return
     */
    public static <T> boolean containsArr(Collection<T[]> coll, T[] arr)
    {
        for(T[] elem : coll){
            if(arr.length != elem.length)
                continue;
            boolean foundAllElem = true;
            for(int i=0; i<elem.length; i++)
                if(!arr[i].equals(elem[i]))
                    foundAllElem = false;
            if(foundAllElem)
                return true;
        }
        return false;
    }

    /**Returns the index with the maximum value in the array;
     * the first incidence of this value, if there are dups
     */
    public static <T extends Number> int getMaxIdx(T[] arr)
    {
        if(arr.length == 0)
            return -1;

        T maxVal = arr[0]; int maxIdx = 0;
        for(int i=1; i<arr.length; i++){
            if(arr[i].doubleValue() > maxVal.doubleValue()){
                maxVal = arr[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }
}























