package utilities;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.Map;

public class JsonIO
{
    /**Writes the given contents map as a JSON file at fileRoot
     *
     * @param contents
     * @param fileRoot
     */
    public static void writeFile(Map<String, Object> contents, String fileRoot)
    {
        writeFile(contents, fileRoot, false);
    }

    /**Writes the given contents map as a JSON file at fileRoot; including
     * the date if specified
     *
     * @param contents
     * @param fileRoot
     * @param includeDate
     */
    public static void writeFile(Map<String, Object> contents, String fileRoot, boolean includeDate)
    {
        JSONObject root = new JSONObject();
        for(String key : contents.keySet()){
            Object val = contents.get(key);
            if(val instanceof Collection<?>){
                JSONArray arr = new JSONArray();
                Collection<?> coll = (Collection<?>)val;
                for(Object v : coll)
                    arr.add(v);
                root.put(key, arr);
            } else if(val instanceof Object[]) {
                JSONArray arr = new JSONArray();
                Object[] coll = (Object[])val;
                for(Object v : coll)
                    arr.add(v);
                root.put(key, arr);
            } else {
                root.put(key, val);
            }
        }
        FileIO.writeFile(toPrettyStr(root.toJSONString()), fileRoot, "json", includeDate);
    }

    /**Returns a formatted json string to make it more human readable
     *
     * @param jsonStr
     * @return
     */
    private static String toPrettyStr(String jsonStr)
    {
        StringBuilder sb = new StringBuilder();
        int numTabs = 0;
        boolean inArr = false;
        for(Character c : jsonStr.toCharArray()) {
            //Ignore commas within an array (keep all that on one line)
            if(c == '[')
                inArr = true;
            else if(c == ']')
                inArr = false;

            //If we're opening or closing a JSON object,
            //put the brace on its own line and adjust
            //the tab accordingly (ignore first line)
            if(c == '{' || c == '}'){
                if(c == '}')
                    numTabs--;

                if(sb.length() > 0){
                    sb.append("\n");
                    for (int i = 0; i < numTabs; i++)
                        sb.append("  ");
                }
            }

            //add this char
            sb.append(c);

            //Put braces on their own line and -- except
            //for commas within an array -- commas indicate a break
            //(note that we don't have to add a line after a closing
            //brace because they typically appear as "},"
            if(c == '{' || c == ','){
                if(c == '{')
                    numTabs++;
                if(!inArr){
                    sb.append("\n");
                    for (int i = 0; i < numTabs; i++)
                        sb.append("  ");
                }
            }
        }
        return sb.toString();
    }
}
