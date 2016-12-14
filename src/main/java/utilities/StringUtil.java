package utilities;

import java.util.*;

/**The StringUtil class houses static functions that perform
 * various string tasks, such as determining if a string has
 * alphanumeric characters
 *
 * @author ccervantes
 */
public class StringUtil {
    /**Returns a copy of <b>s</b> with all non-alphanumeric
     * characters removed. Optional argument <b>keepWhitespace</b>
     * specifies whether to also retain whitespace characters
     *
     * @param s				 - The string to copy
     * @return				 - A copy of the alphanumeric chars in <b>s</b>
     */
    public static String keepAlphaNum(String s){return keepAlphaNum(s, false);}

    /**Returns a copy of <b>s</b> with all non-alphanumeric
     * characters removed. Optional argument <b>keepWhitespace</b>
     * specifies whether to also retain whitespace characters
     *
     * @param s				 - The string to copy
     * @param keepWhitespace - Whether to retain whitespace
     * 						   (false by default)
     * @return				 - A copy of the alphanumeric chars in <b>s</b>
     */
    public static String keepAlphaNum(String s, boolean keepWhitespace)
    {
        StringBuilder sb = new StringBuilder();
        for(char c : s.toCharArray())
        {
            if(Character.isLetterOrDigit(c))
            {
                sb.append(c);
            }
            else if(keepWhitespace && Character.isWhitespace(c))
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**Returns a copy of <b>s</b> with all non-alphabetic characters
     * removed
     *
     * @param s - The string to copy
     * @return	- A copy of the alphabetic chars in <b>s</b>
     */
    public static String keepAlpha(String s)
    {
        StringBuilder sb = new StringBuilder();
        for(char c : s.toCharArray())
            if(Character.isLetter(c))
                sb.append(c);
        return sb.toString();
    }

    /**Returns whether the specified string has alphanumeric characters
     *
     * @param s
     * @return
     */
    public static boolean hasAlphaNum(String s)
    {
        for(char c : s.toCharArray())
            if(Character.isLetterOrDigit(c))
                return true;
        return false;
    }

    /**Returns the file name portion of the given <b>path</b> string.
     *
     * @param path	  - The path from which to retrieve the file name
     * @return		  - The name of the file at <b>path</b>
     */
    public static String getFilenameFromPath(String path){return getFilenameFromPath(path, false);}

    /**Returns the file name portion of the given <b>path</b> string.
     * Optional argument <b>withExt</b> specifies whether to
     * keep the file name extension.
     *
     * @param path	  - The path from which to retrieve the file name
     * @param withExt - Whether to include the extension in the
     * 					file's name (false by default)
     * @return		  - The name of the file at <b>path</b>
     */
    public static String getFilenameFromPath(String path, boolean withExt)
    {
        //break the filename into pieces
        path = path.replace('\\', '/');
        String[] pathParts = path.split("/");
        String filename = pathParts[pathParts.length-1];
        if(!withExt)
        {
            filename = filename.split("\\.")[0];
        }
        return filename;
    }

    /**Returns <b>s1</b> and <b>s2</b>, alphabetized and
     * concatenated with a pipe.
     *
     * @param s1 - The first string
     * @param s2 - The second string
     * @return   - "<b>s1</b>|<b>s2</b>" or "<b>s2</b>|<b>s1</b>",
     * 			   depending on alphabetical order
     */
    public static String getAlphabetizedPair(String s1, String s2)
    {
        if(s1 == null && s2 == null)
            return "";
        else if(s1 == null)
            return s2;
        else if(s2 == null)
            return s1;
        else {
            if(s1.compareTo(s2) < 0)
                return s1 + "|" + s2;
            else
                return s2 + "|" + s1;
        }
    }

    /**Returns a string such that the elements of <b>coll</b> are
     * concatenated together, separated by <b>delimiter</b>
     *
     * @param coll      - The collection of strings
     * @param delimiter - String to put inbetween the collection elements
     * @return		    - The list as delimited string
     */
    public static <T> String listToString(Collection<T> coll,
                                          String delimiter)
    {
        StringBuilder sb = new StringBuilder();
        if(coll != null)
        {
            Iterator<T> iter = coll.iterator();
            if(iter.hasNext()) {
                T obj = iter.next();
                sb.append(obj == null ? "NULL" : obj.toString());
            }
            while(iter.hasNext()) {
                sb.append(delimiter);
                T obj = iter.next();
                sb.append(obj == null ? "NULL" : obj.toString());
            }
        }
        return sb.toString();
    }

    /**Returns a string such that the elements of <b>coll</b> are
     * concatenated together, separated by <b>delimiter</b>
     *
     * @param coll      - The collection of strings
     * @param delimiter - String to put inbetween the collection elements
     * @return		    - The list as delimited string
     */
    public static <T> String listToString(T[] coll, String delimiter)
    {
        return listToString(Arrays.asList(coll), delimiter);
    }

    /**Returns only the unique strings in the given array
     *
     * @param arr
     * @return
     */
    @Deprecated
    public static String[] keepUnique(String[] arr)
    {
        List<String> list = new ArrayList<>();
        for(String s : arr)
            if(s != null && !s.equalsIgnoreCase("null") &&
                    !s.isEmpty() && !list.contains(s))
                list.add(s);
        String[] arr_new = new String[list.size()];
        return list.toArray(arr_new);
    }

    /**Returns the given keys and values as a colon/semicolon
     * delimited key-value string (ie. "k:v; ... k:v;")
     *
     * @param keys
     * @param vals
     * @return
     */
    public static String toKeyValStr(String[] keys, Object[] vals){
        List<String> kvList = new ArrayList<>();
        for(int i=0; i<keys.length; i++){
            String valStr = "null";
            if(vals[i] != null)
                valStr = vals[i].toString();
            kvList.add(keys[i] + ":" + valStr);
        }
        return listToString(kvList, ";");
    }

    /**Returns the given keys and values as a colon/semicolon
     * delimited key-value string (ie. "k:v; ... k:v;")
     *
     * @param keys
     * @param vals
     * @return
     */
    public static String toKeyValStr(List<String> keys, List<Object> vals){
        String[] keyArr = new String[keys.size()];
        Object[] valArr = new Object[vals.size()];
        keyArr = keys.toArray(keyArr);
        valArr = vals.toArray(valArr);
        return toKeyValStr(keyArr, valArr);
    }

    /**Returns a mapping of keys to values, given a colon/semicolon
     * key-value string (k:v; ... k:v;)
     *
     * @param s
     * @return
     */
    public static Map<String, String> keyValStrToDict(String s){
        Map<String, String> dict = new HashMap<>();
        String[] kvPairs = s.split(";");
        for(String kvPairStr : kvPairs){
            String[] kvPair = kvPairStr.split(":");
            dict.put(kvPair[0], kvPair[1]);
        }
        return dict;
    }
}
