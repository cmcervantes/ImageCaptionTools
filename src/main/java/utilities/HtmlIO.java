package utilities;

import structures.Caption;
import structures.Chain;
import structures.Document;
import structures.Mention;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**HtmlIO houses static html file IO functions, generally for
 * writing Documents as web pages
 *
 * @author ccervantes
 */
public class HtmlIO
{
    private static final String imgSrcRoot =
            "http://shannon.cs.illinois.edu/DenotationGraph/graph/flickr30k-images/";

    public static String getImgHtm(Document d)
    {
        //get the image source
        String imgSrc = imgSrcRoot + d.getID();

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
        sb.append(getColorCodedCaptions(d, d.getChainSet()));
        sb.append("</td></tr></table></td>");
        return sb.toString();
    }

    public static String getImgHtm(Document d, Set<Chain> predChainSet)
    {
        //get the image source
        String imgSrc = imgSrcRoot + d.getID() + ".jpg";

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
        sb.append(getColorCodedCaptions(d, d.getChainSet()));
        sb.append("</p>");
        sb.append("<p>");
        sb.append("<h4>Predicted</h4>");
        sb.append(getColorCodedCaptions(d, predChainSet));
        sb.append("</p>");
        sb.append("</td></tr></table></td>");
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

    /*
    public static String getCorefImgPage(Document d, Set<Chain> predChainSet,
                                         List<String> debugLineList, Score score,
                                         String imgID_prev, String imgID_next)
    {
        //put everything together, in the form
        //  ----------------
        //  |        img   |    gold captions
        //  |              |    pred captions
        //  ----------------
        //  [prevBtn] [nextBtn]
        //  ________________________________________
        //  |   Stats            | Mismatched Links |
        //  |____________________|__________________|
        //  | CoreferenceUtil Log     | Singleton Debug  |
        //  |____________________|__________________|
        //
        //  (table boundaries shown for clarity)
        StringBuilder pageBuilder = new StringBuilder();
        pageBuilder.append("<html><body><table>");

        //The image row contains the image and gold/predicted chains
        pageBuilder.append("<tr>");
        pageBuilder.append(getImgHtm(d, predChainSet));
        pageBuilder.append("</tr>");

        //The button row contains the buttons, off to the left hand side
        pageBuilder.append("<tr>");
        pageBuilder.append("<td><table><tr><td width=50%>");
        pageBuilder.append("<form style=\"display:inline;width=50px;height=50px\"");
        pageBuilder.append("action=\"http://web.engr.illinois.edu/~ccervan2/coref/chain/");
        pageBuilder.append(imgID_prev);
        pageBuilder.append(".htm\"><input type=\"submit\" value=\"Previous\"></form>");
        pageBuilder.append("<form style=\"display:inline;width=50px;height=50px\"");
        pageBuilder.append("action=\"http://web.engr.illinois.edu/~ccervan2/coref/chain/");
        pageBuilder.append(imgID_next);
        pageBuilder.append(".htm\"><input type=\"submit\" value=\"Next\"></form>");
        pageBuilder.append("</td><td width=50%></td></tr></table></td>");
        pageBuilder.append("</tr>");

        //the next row will contain our score table. The exact information
        //included in which will differ based on the score type
        pageBuilder.append("<tr><td>");
        pageBuilder.append(getScoreTable(score));
        pageBuilder.append("</td></tr>");

        //The next row shows our debug output, split into
        //classifier logs (on the left) and singleton debug logs (on
        //the right). The split allows us to more directly view why
        //the singleton chains weren't put into larger chains
        //put our debug output into a single string
        StringBuilder debugBuilder_1 = new StringBuilder();
        StringBuilder debugBuilder_2 = new StringBuilder();
        debugBuilder_1.append("<h4>CoreferenceUtil Log</h4>");
        debugBuilder_2.append("<h4>Singleton Debug</h4>");
        boolean switchDebug = false;
        if(debugLineList != null){
            for(String line : debugLineList) {
                if(line.contains("Confidence with which"))
                    switchDebug = true;
                if(switchDebug) {
                    debugBuilder_2.append(line);
                    debugBuilder_2.append("<br/>");
                } else {
                    debugBuilder_1.append(line);
                    debugBuilder_1.append("<br/>");
                }
            }
        }
        pageBuilder.append("<tr><td><table><tr>");
        pageBuilder.append("<td width=50% valign=\"top\">");
        pageBuilder.append(debugBuilder_1);
        pageBuilder.append("</td>");
        pageBuilder.append("<td width=50% valign=\"top\">");
        pageBuilder.append(debugBuilder_2);
        pageBuilder.append("</td>");
        pageBuilder.append("</tr></table></td></tr>");

        pageBuilder.append("</table></body></html>");
        return pageBuilder.toString();
    }*/

    /*
    private static StringBuilder getScoreTable(Score score)
    {
        StringBuilder tableBuilder = new StringBuilder();
        tableBuilder.append("<table><tr>");
        if(score.getType() == Score.ScoreType.BCUBED){
            BCubed bcubScore = (BCubed)score;
            tableBuilder.append("<td>");
            tableBuilder.append(String.format("B<sup>3</sup> Accuracy<br/>"+
                            "<div style=\"padding-left:2em\">%s</div>",
                    bcubScore.getScoreString()));
            tableBuilder.append("</td>");
        } else if(score.getType() == Score.ScoreType.BLANC){
            Blanc blancScore = (Blanc)score;

            //this column displays the overal classifier stats
            tableBuilder.append("<td width=50% valign=\"top\">");
            tableBuilder.append(String.format("Total links: %d<br/>"+
                            "<div style=\"padding-left:2em\">%d positive</div>"+
                            "<div style=\"padding-left:2em\">%d negative</div>",
                    blancScore.getNumLinks(), blancScore.getNumPosLinks(),
                    blancScore.getNumNegLinks()));
            tableBuilder.append(String.format("BLANC Accuracy<br/>"+
                            "<div style=\"padding-left:2em\">Pos: %s</div>"+
                            "<div style=\"padding-left:2em\">Neg: %s</div>"+
                            "<div style=\"padding-left:2em\">Ttl: %s</div>",
                    blancScore.getScoreString_pos(), blancScore.getScoreString_neg(),
                    blancScore.getScoreString_total()));
            tableBuilder.append("<br/><br/><br/>");
            tableBuilder.append("<em>Added Links: negative in gold, positive in predicted</em><br/>");
            tableBuilder.append("<em>Removed Links: positive in gold, negative in predicted</em><br/>");
            tableBuilder.append("<em>Totals based on merged gold</em>");
            tableBuilder.append("</td>");
        }
        tableBuilder.append("</tr></table>");
        return tableBuilder;
    }
    */


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

    /**Given a Document, <b>d</b>, returns the captions
     * as an html block, color coded to correspond with
     * the chain assignments.
     * NOTE: also requires a chain dict, since we want both
     *       the captions (which don't vary based on our assignments)
     *       and the colors (which do
     *
     * @return 			- The HTML for the color-coded captions
     */
    private static String getColorCodedCaptions(Document d, Set<Chain> chainSet)
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
        for(Chain c : chainSet) {
            boolean isSingletonChain = c.getMentionSet().size() == 1;
            for(Mention m : c.getMentionSet()) {
                int index = captionBuilder.indexOf(m.getUniqueID());
                if(index > -1) {
                    //find the end of this mention's span tag
                    index = captionBuilder.indexOf(">", index);

                    //insert some formatting. Bold for all mentions,
                    //a color for non-singleton chains
                    String spanFormat = " style=\"font-weight:bold;";
                    if(!isSingletonChain)
                        spanFormat += "color:" + colorArr[colorIndex] + ";";
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

            //if this was a multi-phrase chain, increment the color
            if(!isSingletonChain)
                colorIndex++;
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
