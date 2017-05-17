package structures;

import nlptools.IllinoisChunker;
import nlptools.IllinoisTagger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import utilities.Logger;
import utilities.StringUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SNLIPair extends Document
{
    private String origDocID;
    private String origCapID;
    private EntailmentLabel goldLabel;
    private List<EntailmentLabel> annoLabels;

    /**Constructor used for parsing SNLI pairs from a JSON line; tags and chunks the
     * caption pairs using the given tagger and chunker
     *
     * @param jsonLine
     * @param tagger
     * @param chunker
     */
    public SNLIPair(String jsonLine, IllinoisTagger tagger, IllinoisChunker chunker)
    {
        String premiseStr = "", hypothesisStr = "";
        try{
            //Parse the json line
            JSONParser jParse = new JSONParser();
            JSONObject jObj = (JSONObject)jParse.parse(jsonLine);

            //caption ID / label / captions
            origCapID = (String)jObj.get("captionID");
            origDocID = origCapID.split("#")[0];
            _ID = (String)jObj.get("pairID");
            goldLabel = EntailmentLabel.parseLabel((String)jObj.get("gold_label"));
            premiseStr = (String)jObj.get("sentence1");
            hypothesisStr = (String)jObj.get("sentence2");

            //Load the annotator label array
            annoLabels = new ArrayList<>();
            JSONArray annoLabelArr = (JSONArray)jObj.get("annotator_labels");
            Iterator<String> iterator = annoLabelArr.iterator();
            while (iterator.hasNext())
                annoLabels.add(EntailmentLabel.parseLabel(iterator.next()));
        } catch(Exception ex){
            Logger.log(ex);
        }

        //Parse the two captions
        _captionList.add(chunker.predictCaptionChunks(tagger.predict(premiseStr), _ID, 0));
        _captionList.add(chunker.predictCaptionChunks(tagger.predict(hypothesisStr), _ID, 1));
    }

    /**Constructor used for combining the SNLI pair data from JSCON line and
     * the given premise and hypothesis coref-formatted lines (assumes the caller
     * will have found the appropriate lines from a .coref file)
     *
     * @param jsonLine
     * @param premiseCoref
     * @param hypCoref
     */
    public SNLIPair(String jsonLine, String premiseCoref, String hypCoref)
    {
        try{
            //Parse the json line
            JSONParser jParse = new JSONParser();
            JSONObject jObj = (JSONObject)jParse.parse(jsonLine);

            //caption ID / label / captions
            origCapID = (String)jObj.get("captionID");
            origDocID = origCapID.split("#")[0];
            _ID = (String)jObj.get("pairID");
            goldLabel = EntailmentLabel.parseLabel((String)jObj.get("gold_label"));

            //Load the annotator label array
            annoLabels = new ArrayList<>();
            JSONArray annoLabelArr = (JSONArray)jObj.get("annotator_labels");
            Iterator<String> iterator = annoLabelArr.iterator();
            while (iterator.hasNext())
                annoLabels.add(EntailmentLabel.parseLabel(iterator.next()));

            //load the two captions
            _captionList.add(Caption.fromCorefStr(premiseCoref));
            _captionList.add(Caption.fromCorefStr(hypCoref));
        } catch(Exception ex){
            Logger.log(ex);
        }
    }

    public Caption getPremise(){return _captionList.get(0);}

    public Caption getHypothesis(){return _captionList.get(1);}

    public String toDebugString()
    {
        List<String> keys = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        keys.add("orig_caption_id"); vals.add(origCapID);
        keys.add("orig_doc_id"); vals.add(origDocID);
        keys.add("pair_id"); vals.add(_ID);
        keys.add("gold_label"); vals.add(goldLabel.toString());
        keys.add("anno_labels"); vals.add(StringUtil.listToString(annoLabels, "|"));
        return StringUtil.toKeyValStr(keys, vals);
    }

    public String getOriginalCaptionID(){return origCapID;}
    public String getOriginalDocumentID(){return origDocID;}
}
