package utilities;

import structures.Caption;
import structures.Chain;
import structures.Document;
import structures.Mention;

import java.util.*;

/**HtmlIO houses static html file IO functions, generally for
 * writing Documents as web pages
 *
 * @author ccervantes
 */
public class HtmlIO
{
    private static final String _imgSrcRoot =
            "http://shannon.cs.illinois.edu/DenotationGraph/graph/flickr30k-images/";
    private static final String[] _colors = {"red", "blue", "green",
            "darkorchid", "teal", "saddlebrown", "mediumvioletred",
            "darkkhaki", "darkslategray", "darkolivegreen", "olivedrab",
            "chocolate"};


    public static String getImgHtm(Document d)
    {
        //get the image source
        String imgSrc = _imgSrcRoot + d.getID();

        //build the row, enclosing it in a table
        //so as not to throw off any formatting
        StringBuilder sb = new StringBuilder();
        sb.append("<td><table><tr><td>");
        sb.append(String.format("<img src=\"%s\" height=\"%d\" width=\"%d\"/><br/>",
                imgSrc, d.height, d.width));
        sb.append("Image: <em>");
        sb.append(d.getID());
        sb.append("</em>");
        sb.append("</td>");
        sb.append("<td>");
        sb.append(_getColorCodedCaptions(d, _getChainColors(d.getChainSet())));
        sb.append("</td></tr></table></td>");
        return sb.toString();
    }

    public static String getImgHtm(Document d, Set<Chain> predChainSet)
    {
        return getImgHtm(d, predChainSet, null);
    }

    public static String getImgHtm(Document d, Set<Chain> predChainSet, Set<Chain[]> subsetPairs)
    {
        //get the image source
        String imgSrc = _imgSrcRoot + d.getID();

        //build the row, enclosing it in a table
        //so as not to throw off any formatting
        StringBuilder sb = new StringBuilder();
        sb.append("<td><table><tr><td>");
        sb.append(String.format("<img src=\"%s\" height=\"%d\" width=\"%d\"/><br/>",
                imgSrc, d.height, d.width));
        sb.append("Image: <em>");
        sb.append(d.getID());
        sb.append("</em>");
        sb.append("</td>");
        sb.append("<td>");
        sb.append("<p>");
        sb.append("<h4>Gold</h4>");
        Map<Chain, String> chainColors_gold = _getChainColors(d.getChainSet());
        sb.append(_getColorCodedCaptions(d, chainColors_gold));
        sb.append("</p>");
        sb.append("<p>");
        sb.append("<h4>Predicted</h4>");
        Map<Chain, String> chainColors_pred = _getChainColors(predChainSet);
        sb.append(_getColorCodedCaptions(d, chainColors_pred));
        sb.append("</p>");
        sb.append("</td></tr>");
        if(subsetPairs != null){
            sb.append("<tr><td>");
            sb.append("<h4>Gold</h4>");
            sb.append(_getColorCodedSubsetList(d.getSubsetChains(), chainColors_gold));
            sb.append("<h4>Predicted</h4>");
            sb.append(_getColorCodedSubsetList(subsetPairs, chainColors_pred));
            sb.append("</td></tr>");
        }
        sb.append("</table></td>");
        return sb.toString();
    }



    public static String getMultiImgPage(Collection<Document> docList)
    {
        StringBuilder pageBuilder = new StringBuilder();
        pageBuilder.append("<html><body><table>");
        for(Document d : docList)
        {
            pageBuilder.append("<tr>");
            pageBuilder.append(getImgHtm(d));
            pageBuilder.append("</tr>");
        }
        pageBuilder.append("</table></body></html>");
        return pageBuilder.toString();
    }

    private static StringBuilder
    getLinkMismatchTableRows(Set<String[]> mentionPairSet)
    {
        StringBuilder sb = new StringBuilder();
        for(String[] mentionPairArr : mentionPairSet){
            sb.append("<tr>");
            sb.append("<td style=\"text-align:right\">");
            sb.append(mentionPairArr[0]);
            sb.append("</td>");
            sb.append("<td style=\"text-align:center\"> --- </td>");
            sb.append("<td style=\"text-align:left\">");
            sb.append(mentionPairArr[1]);
            sb.append("</td>");
            sb.append("</tr>");
        }
        return sb;
    }

    /**Returns the string for an html unordered list of subset pairs,
     * where each list item is
     * {m, m, m} sub {m, m, m}
     * corresponding to the given subset pairs
     *
     * @param subsetPairs
     * @param chainColors
     * @return
     */
    private static String _getColorCodedSubsetList(Set<Chain[]> subsetPairs, Map<Chain, String> chainColors)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<ul>");
        for(Chain[] subsetPair : subsetPairs) {
            Chain sub = subsetPair[0], sup = subsetPair[1];
            sb.append("<li>");

            //Sub mentions
            sb.append("<span style=\"color:");
            sb.append(chainColors.get(sub));
            sb.append(";\">");
            sb.append("&#123;");
            List<Mention> subMentions = new ArrayList<>(sub.getMentionSet());
            for(int i=0; i<subMentions.size(); i++){
                sb.append("[");
                sb.append(StringUtil.toWebSafeStr(subMentions.get(i).toString()));
                sb.append("]");
                if(i < subMentions.size()-1)
                    sb.append(", ");
            }
            sb.append("&#125;");
            sb.append("<sub>");
            String subID = sub.getID().substring(Math.max(0,
                    sub.getID().length() - 5), sub.getID().length());
            sb.append(subID);
            sb.append("</sub>");
            sb.append("</span>");

            //Subset
            sb.append(" &#8834; ");

            //Sup mentions
            sb.append("<span style=\"color:");
            sb.append(chainColors.get(sup));
            sb.append(";\">");
            sb.append("&#123;");
            List<Mention> supMentions = new ArrayList<>(sup.getMentionSet());
            for(int i=0; i<supMentions.size(); i++){
                sb.append("[");
                sb.append(StringUtil.toWebSafeStr(supMentions.get(i).toString()));
                sb.append("]");
                if(i < supMentions.size()-1)
                    sb.append(", ");
            }
            sb.append("&#125;");
            sb.append("<sub>");
            String supID = sup.getID().substring(Math.max(0,
                    sup.getID().length() - 5), sup.getID().length());
            sb.append(supID);
            sb.append("</sub>");
            sb.append("</span>");


            sb.append("</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    /**Returns a mapping of the given chains to
     * colors, such that chain 0 is always black,
     * chains up to the twelfth are according to
     * our set colors, and each chain beyond this
     * is assigned a random color
     *
     * @param chainSet
     * @return
     */
    private static Map<Chain, String> _getChainColors(Set<Chain> chainSet)
    {
        Map<Chain, String> colorDict = new HashMap<>();
        int colorIdx = 0;
        for(Chain c : chainSet){
            String color = "black";
            if(!c.getID().equals("0")) {
                color = "#" + Integer.toHexString((int)(Math.random()*16777215));
                if(colorIdx < _colors.length)
                    color = _colors[colorIdx++];
            }
            colorDict.put(c, color);
        }
        return colorDict;
    }

    /**Given a Document, <b>d</b>, returns the captions
     * as an html block, color coded to correspond with
     * the given chain assignments
     *
     * @param d
     * @param chainColorDict
     * @return 			- The HTML for the color-coded captions
     */
    private static String _getColorCodedCaptions(Document d, Map<Chain, String> chainColorDict)
    {
        //get span tagged strings for each caption
        StringBuilder captionBuilder = new StringBuilder();
        captionBuilder.append("<ul>");
        for(Caption c : d.getCaptionList())
        {
            captionBuilder.append("<li>");
            captionBuilder.append(c.toSpanString());
            captionBuilder.append("</li>");
        }
        captionBuilder.append("</ul>");

        //iterate through the chains, replacing mentions' IDs with
        //the ID+formatting
        for(Chain c : chainColorDict.keySet()) {
            for(Mention m : c.getMentionSet()) {
                int index = captionBuilder.indexOf(m.getUniqueID());
                if(index > -1) {
                    //find the end of this mention's span tag
                    index = captionBuilder.indexOf(">", index);

                    //insert some formatting. Bold for all mentions,
                    //a color for non-singleton chains
                    String spanFormat = " style=\"font-weight:bold;";
                    spanFormat += "color:" + chainColorDict.get(c) + ";";
                    spanFormat += "\"";
                    captionBuilder.insert(index, spanFormat);

                    //find the end of this span tag, and add
                    //the last four chars of the chain ID
                    index = captionBuilder.indexOf("<", index);
                    String chainID = c.getID();
                    if(chainID != null && chainID.length() > 6)
                        chainID = chainID.substring(chainID.length()-5, chainID.length()-1);
                    captionBuilder.insert(index, "<sub>"+chainID+"</sub>");
                }
            }
        }

        return captionBuilder.toString();
    }

    public static String getColorCodedCaption(Caption c, Map<Mention, String> mentionIdDict)
    {
        //we'll need a list of colors so we can color code
        //our chains
        String[] colorArr = {"red", "blue", "green", "darkorchid",
                "teal", "saddlebrown",
                "mediumvioletred", "darkkhaki",
                "darkslategray", "darkolivegreen",
                "olivedrab", "chocolate"};
        int colorIndex = 0;

        //get span tagged strings for each caption
        StringBuilder captionBuilder = new StringBuilder();
        captionBuilder.append(c.toSpanString());

        //invert the mentionID dict so we can more easily color code
        Map<String, Set<Mention>> idMentionDict =
                Util.invertMap(mentionIdDict);

        //iterate through each ID, replacing spans with our formatting
        for(String ID : idMentionDict.keySet()){
            boolean isSingleton = idMentionDict.get(ID).size() == 1;
            for(Mention m : idMentionDict.get(ID)){
                int index = captionBuilder.indexOf(m.getUniqueID());
                if(index > -1) {
                    //find the end of this mention's span tag
                    index = captionBuilder.indexOf(">", index);

                    //insert some formatting. Bold for all mentions,
                    //a color for non-singleton chains
                    String spanFormat = " style=\"font-weight:bold;";
                    if(!isSingleton)
                        spanFormat += "color:" + colorArr[colorIndex] + ";";
                    spanFormat += "\"";
                    captionBuilder.insert(index, spanFormat);

                    //find the end of this span tag, and add
                    //the last four chars of the chain ID
                    index = captionBuilder.indexOf("<", index);

                    if(ID != null && ID.length() > 6)
                        ID = ID.substring(ID.length()-5, ID.length()-1);
                    captionBuilder.insert(index, "<sub>"+ID+"</sub>");
                }
            }

            //if this was a multi-phrase chain, increment the color
            if(!isSingleton)
                colorIndex++;
        }

        return captionBuilder.toString();
    }
}
