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

    /**Returns the given list of lists as a formatted table
     * string, where each column is 1 space larger than the largest
     * cell's string (for that column). Tables are formatted as
     *
     *     | col  col  col
     * ---------------------
     * row | cell cell cell
     * row | cell cell cell
     *
     * @param table
     * @param includesHeaders
     * @return
     */
    public static String toTableStr(List<List<String>> table, boolean includesHeaders)
    {
        String[][] tableArr = new String[table.size()][];
        for(int i=0; i<table.size(); i++)
            tableArr[i] = table.get(i).toArray(new String[]{});
        return toTableStr(tableArr, includesHeaders);
    }

    /**Returns the given 2d array as a formatted table string,
     * where each column is 1 space larger than the largest
     * cell's string (for that column). Tables are formatted as
     *
     *     | col  col  col
     * ---------------------
     * row | cell cell cell
     * row | cell cell cell
     *
     * @param table
     * @param includesHeaders
     * @return
     */
    public static String toTableStr(String[][] table, boolean includesHeaders)
    {
        //Set up our formatting string using an
        //array of column widths, such that the
        //column width for column j will be one
        //greater than the longest ij string,
        //for all i
        int numRows = table.length;
        int numCols = 0;
        for(int i=0; i<numRows; i++)
            if(table[i].length > numCols)
                numCols = table[i].length;
        int[] colWidths = new int[numCols];
        for(int i=0; i<numRows; i++)
            for(int j=0; j<numCols; j++)
                if(j < table[i].length)
                    if(table[i][j].length() > colWidths[j])
                        colWidths[j] = table[i][j].length();
        String formatStr = "";
        int startIdx = 0;
        if(includesHeaders){
            formatStr = "%-" + (colWidths[0]+1) + "s | ";
            startIdx = 1;
        }
        for(int i=startIdx; i<numCols; i++)
            formatStr += "%-" + (colWidths[i]+1) + "s ";
        formatStr += "\n";

        //Create the table string and return
        StringBuilder sb = new StringBuilder();
        String[] colHeaders = new String[numCols];
        for(int col=0; col<numCols; col++){
            if(col < table[0].length)
                colHeaders[col] = table[0][col];
            else
                colHeaders[col] = "";
        }
        sb.append(String.format(formatStr, (Object[])colHeaders));

        //Add that separating row of dashes
        if(includesHeaders){
            for(int i=0; i<numCols; i++){
                for(int j=0; j < colWidths[i] + 2; j++)
                    sb.append("-");
                if(i == 0) //first column has pipe and extra space
                    sb.append("|-");
            }
            sb.append("\n");
        }

        //Add the rows
        for(int r=1; r<numRows; r++){
            String[] row = new String[numCols];
            for(int c=0; c<numCols; c++){
                if(c < table[r].length)
                    row[c] = table[r][c];
                else
                    row[c] = "";
            }
            sb.append(String.format(formatStr, (Object[])row));
        }
        return sb.toString();
    }

    /**Converts the given string to a websafe version,
     * replacing special characters with their escape codes
     *
     * @param s
     * @return
     */
    public static String toWebSafeStr(String s)
    {
        //Strip out the special chars
        String websafe = s.replace("\"", "[QUOTE]");
        websafe = websafe.replace("'", "[APPOS]");
        websafe = websafe.replace("&", "[AMPRS]");
        websafe = websafe.replace("<", "[LESS]");
        websafe = websafe.replace(">", "[GRTR]");
        websafe = websafe.replace(";", "[DEICTIC]");
        websafe = websafe.replace(":", "[COLON]");
        websafe = websafe.replace("#", "[HASH]");

        //Replace them with their web equivalents
        websafe = websafe.replace("[QUOTE]", "&#34;");
        websafe = websafe.replace("[APPOS]", "&#39;");
        websafe = websafe.replace("[AMPRS]", "&#38;");
        websafe = websafe.replace("[LESS]", "&#60;");
        websafe = websafe.replace("[GRTR]", "&#62;");
        websafe = websafe.replace("[DEICTIC]", "&#59;");
        websafe = websafe.replace("[COLON]", "&#58;");
        websafe = websafe.replace("[HASH]", "&#35;");

        return websafe;
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

    /**Returns whether the specified string starts with an element
     * from the specified collection
     *
     * @param coll
     * @param s
     * @return
     */
    public static boolean startsWithElement(Collection<String> coll, String s)
    {
        for(String item : coll)
            if(s.startsWith(item))
                return true;
        return false;
    }

    /**Return whether the specified string contains one of the
     * elements from the specified collection
     *
     * @param coll
     * @param s
     * @return
     */
    public static boolean containsElement(Collection<String> coll, String s)
    {
        for(String item : coll)
            if(s.contains(item))
                return true;
        return false;
    }
}
