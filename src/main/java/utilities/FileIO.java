package utilities;

import structures.Caption;
import structures.Mention;

import java.io.*;
import java.util.*;

/**FileIO houses static file IO functions, generally for
 * reading and writing standard text files
 *
 * @author ccervantes
 */
public class FileIO
{
    /**Writes <b>contents</b> to <b>fileRoot</b>[_date].<b>fileExt</b>
     *
     * @param contents      - The string to write as a file
     * @param fileRoot      - The name of the file
     * @param fileExt       - The file extension without "."
     *                        (txt by default)
     * @param includeDate   - Whether to include the data in
     *                        yyyyMMdd format (true by default)
     */
    public static void writeFile(String contents, String fileRoot,
                                 String fileExt, boolean includeDate)
    {
        String filename = fileRoot;
        if(includeDate)
            filename += "_" + Util.getCurrentDateTime("yyyyMMdd");
        filename += "." + fileExt;
        try {
            BufferedWriter bw = new BufferedWriter(
                    new FileWriter(filename));
            bw.write(contents);
            bw.close();
        } catch(IOException ioEx) {
            System.err.println("Could not save output file " + filename);
        }
    }

    /**Writes <b>contents</b> to <b>fileRoot</b>[_date].<b>fileExt</b>
     *
     * @param contents      - The string to write as a file
     * @param fileRoot      - The name of the file
     * @param fileExt       - The file extension without "."
     *                        (txt by default)
     */
    public static void writeFile(String contents, String fileRoot,
                                 String fileExt)
    {
        writeFile(contents, fileRoot, fileExt, true);
    }

    /**Writes <b>contents</b> to <b>fileRoot</b>[_date].<b>fileExt</b>
     *
     * @param contents      - The string to write as a file
     * @param fileRoot      - The name of the file
     */
    public static void writeFile(String contents, String fileRoot)
    {
        writeFile(contents, fileRoot, "txt", true);
    }

    /**Writes <b>lineList</b> to <b>fileRoot</b>[_date].<b>fileExt</b>,
     * where each line in the list is separated by a newline
     *
     * @param lineList      - The list of lines to write as a file
     * @param fileRoot      - The name of the file
     * @param fileExt       - The file extension without "."
     *                        (txt by default)
     * @param includeDate   - Whether to include the data in
     *                        yyyyMMdd format (true by default)
     */
    public static <T> void writeFile(Collection<T> lineList,
                                     String fileRoot, String fileExt,
                                     boolean includeDate)
    {
        writeFile(StringUtil.listToString(lineList, "\n"), fileRoot, fileExt, includeDate);
    }

    /**Writes <b>lineList</b> to <b>fileRoot</b>[_date].<b>fileExt</b>,
     * where each line in the list is separated by a newline
     *
     * @param lineList      - The list of lines to write as a file
     * @param fileRoot      - The name of the file
     * @param fileExt       - The file extension without "."
     *                        (txt by default)
     */
    public static <T> void writeFile(Collection<T> lineList,
                                     String fileRoot, String fileExt)
    {
        writeFile(lineList, fileRoot, fileExt, true);
    }

    /**Writes <b>lineList</b> to <b>fileRoot</b>[_date].<b>fileExt</b>,
     * where each line in the list is separated by a newline
     *
     * @param lineList      - The list of lines to write as a file
     * @param fileRoot      - The name of the file
     */
    public static <T> void writeFile(Collection<T> lineList,
                                     String fileRoot)
    {
        writeFile(lineList, fileRoot, "txt");
    }

    /**Writes <b>doubleDict</b> to <b>fileRoot</b>[_date].<b>fileExt</b>,
     * where each line is a key,value pair, sorted by value (desc)
     *
     * @param doubleDict    - The DoubleDict to write to file
     * @param fileRoot      - The name of the file
     * @param fileExt       - The file extension without "."
     *                        (txt by default)
     * @param includeDate   - Whether to include the data in
     *                        yyyyMMdd format (true by default)
     */
    public static <T> void writeFile(DoubleDict<T> doubleDict,
                                     String fileRoot, String fileExt,
                                     boolean includeDate)
    {
        StringBuilder sb = new StringBuilder();
        for(T key : doubleDict.getSortedByValKeys(true)) {
            if(key != null)
                sb.append(key.toString());
            sb.append(",");
            sb.append(doubleDict.get(key));
            sb.append("\n");
        }
        writeFile(sb.toString(), fileRoot, fileExt, includeDate);
    }

    /**Writes <b>doubleDict</b> to <b>fileRoot</b>[_date].<b>fileExt</b>,
     * where each line is a key,value pair, sorted by value (desc)
     *
     * @param doubleDict    - The DoubleDict to write to file
     * @param fileRoot      - The name of the file
     * @param fileExt       - The file extension without "."
     *                        (csv by default)
     */
    public static <T> void writeFile(DoubleDict<T> doubleDict,
                                     String fileRoot, String fileExt)
    {
        writeFile(doubleDict, fileRoot, fileExt, true);
    }

    /**Writes <b>doubleDict</b> to <b>fileRoot</b>[_date].<b>fileExt</b>,
     * where each line is a key,value pair, sorted by value (desc)
     *
     * @param doubleDict    - The DoubleDict to write to file
     * @param fileRoot      - The name of the file
     */
    public static <T> void writeFile(DoubleDict<T> doubleDict,
                                     String fileRoot)
    {
        writeFile(doubleDict, fileRoot, "csv");
    }

    /**Writes <b>table</b> to <b>fileRoot</b>[_date].<b>fileExt</b>,
     * where each row is a line, each cell is separated by "," and
     * each non-numeric value is enclosed by strings
     *
     * @param table         - The list of lists to write to file
     * @param fileRoot      - The name of the file
     * @param fileExt       - The file extension without "."
     *                        (csv by default)
     * @param includeDate   - Whether to include the data in
     *                        yyyyMMdd format (true by default)
     */
    public static <T> void writeFile(List<List<T>> table,
                                     String fileRoot, String fileExt,
                                     boolean includeDate)
    {
        StringBuilder sb = new StringBuilder();
        for(List<T> row : table){
            for(int i=0; i<row.size(); i++){
                T cell = row.get(i);
                String cellStr = "";
                if(cell != null){
                    cellStr = String.valueOf(cell);
                    if(cell instanceof String)
                        cellStr = "\"" + cellStr.replace("\"", "'") + "\"";
                }
                sb.append(cellStr);
                if(i < row.size() - 1)
                    sb.append(",");
            }
            sb.append("\n");
        }
        writeFile(sb.toString(), fileRoot, fileExt, includeDate);
    }

    /**Writes <b>table</b> to <b>fileRoot</b>[_date].<b>fileExt</b>,
     * where each row is a line, each cell is separated by "," and
     * each non-numeric value is enclosed by strings
     *
     * @param table         - The list of lists to write to file
     * @param fileRoot      - The name of the file
     * @param fileExt       - The file extension without "."
     *                        (csv by default)
     */
    public static <T> void writeFile(List<List<T>> table,
                                     String fileRoot, String fileExt)
    {
        writeFile(table, fileRoot, fileExt, true);
    }

    /**Writes <b>table</b> to <b>fileRoot</b>[_date].<b>fileExt</b>,
     * where each row is a line, each cell is separated by "," and
     * each non-numeric value is enclosed by strings
     *
     * @param table         - The list of lists to write to file
     * @param fileRoot      - The name of the file
     */
    public static <T> void writeFile(List<List<T>> table,
                                     String fileRoot)
    {
        writeFile(table, fileRoot, "csv");
    }

    /**Reads <b>filename</b> into a list of lines
     *
     * @param filename  - The file to read
     * @param normalize - Whether all strings should be
     *                    lowercased / trimmed (false by default)
     * @return          - The file as a list of lines
     */
    public static List<String>
        readFile_lineList(String filename, boolean normalize)
    {
        List<String> lineList = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(filename)));
            String nextLine = br.readLine();
            while(nextLine != null) {
                if(normalize)
                    nextLine = nextLine.trim().toLowerCase();
                lineList.add(nextLine);
                nextLine = br.readLine();
            }
            br.close();
        } catch(IOException ioEx) {
            Logger.log(ioEx);
        }
        return lineList;
    }

    /**Reads <b>filename</b> into a list of lines
     *
     * @param filename  - The file to read
     * @return          - The file as a list of lines
     */
    public static List<String> readFile_lineList(String filename)
    {
        return readFile_lineList(filename, false);
    }

    /**Reads <b>filename</b> into a table (2d array) where
     * each line is a row, each <b>delimiter</b>-separated
     * string a cell
     *
     * @param filename  - The file to read
     * @param delimiter - The delimiter used in the file ("," by default)
     * @return          - The file as a 2d array
     */
    public static String[][] readFile_table(String filename, String delimiter)
    {
        List<String> lineList = readFile_lineList(filename);
        String[][] table = new String[lineList.size()][];
        for(int i=0; i<lineList.size(); i++)
            table[i] = lineList.get(i).split(delimiter);
        return table;
    }

    /**Reads <b>filename</b> into a table (2d array) where
     * each line is a row, each <b>delimiter</b>-separated
     * string a cell
     *
     * @param filename  - The file to read
     * @return          - The file as a 2d array
     */
    public static String[][] readFile_table(String filename)
    {
        return readFile_table(filename, ",");
    }

    /**Writes <b>lineList</b> to an html file (<b>outRoot</b>.htm),
     * where each element occupies a row in a table
     *
     * @param lineList - The list of lines to write to the file
     * @param outRoot  - The root filename to write to
     */
    public static void writeLineList_toHtm(String[] lineList, String outRoot)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<table>");
        for(String line : lineList)
        {
            sb.append("<tr>");
            for(String cell : line.split("\\|"))
            {
                sb.append("<td>");
                sb.append(cell);
                sb.append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
        sb.append("</body></html>");

        //write the file
        writeFile(sb.toString(), outRoot + ".htm");
    }

    /**Returns a list of file names present in the directory
     * at <b>dirPath</b>
     *
     * @param dirPath - The path of the directory to look in
     * @return		  - A list of file names
     */
    public static List<String> getFileNamesFromDir(String dirPath)
    {
        ArrayList<String> filenameList = new ArrayList<>();
        File dir = new File(dirPath);
        File[] fileList = dir.listFiles();
        if(fileList != null){
            for(File f : fileList)
                if(f.isFile())
                    filenameList.add(f.getName());
        }
        return filenameList;
    }

    public static String
        getLatexColorCodedCaption(Caption c,
                                  Map<Mention, String> mentionIdDict)
    {
        //These colors are similar-not-the-same as the ones we
        //use for html. Latex enables them if we use
        // \ usepackage[usenames, dvipsnames]{xcolor}
        String[] colorArr = {"Red", "Blue", "Green",
                "DarkOrchid", "BlueGreen", "Sepia",
                "RedViolet", "Bown", "CadetBlue",
                "OliveGreen", "ForestGreen"};
        int colorIdx = 0;

        //get span tagged strings for each caption
        //NOTE: Span tags aren't exactly the most useful thing,
        //      but we're trying to piggyback on the preexisting
        //      html output code; as it happens, though, we can
        //      just replace spans wholecloth, which is nice
        String captionStr = c.toSpanString();

        //invert the mentionID dict so we can more easily color code
        Map<String, Set<Mention>> idMentionDict =
                Util.invertMap(mentionIdDict);

        //iterate through each ID, replacing spans with our formatting
        for(String ID : idMentionDict.keySet()){
            boolean boldOnly = idMentionDict.get(ID).size() == 1 || ID == null || ID.equals("-1");
            for(Mention m : idMentionDict.get(ID)) {

                String spanStr = "<span id=\"" + m.getUniqueID() + "\">";
                int idx = captionStr.indexOf(spanStr);
                if (idx > -1) {
                    String formatStr = "\\textbf{";
                    if (!boldOnly)
                        formatStr += "\\textcolor{" + colorArr[colorIdx] + "}{";
                    captionStr = captionStr.replace(spanStr, formatStr);
                }

                //get the index of the next </span> tag and replace it with
                //one or two curly braces, depending on if we added a color
                idx = captionStr.indexOf("</span>", idx);
                if (idx > -1) {
                    String closingStr = "\\textsubscript{";
                    if (ID == null)
                        closingStr += "null";
                    else
                        closingStr += ID.replace("|", "$\\vert$");
                    closingStr += "}}";
                    if (!boldOnly)
                        closingStr += "}";
                    captionStr = captionStr.substring(0, idx) +
                            closingStr + captionStr.substring(idx + 7);
                }
            }

            //if we used a color on this cluster, get a new one
            if(!boldOnly)
                colorIdx++;
        }

        //as a post-processing step, strip the span tags for mentions
        //we don't care about
        for(Mention m : c.getMentionList())
            if(!mentionIdDict.containsKey(m))
                captionStr = captionStr.replace("<span id=\"" + m.getUniqueID() + "\">", "");
        captionStr = captionStr.replace("</span>", "");

        return captionStr;
    }
}
