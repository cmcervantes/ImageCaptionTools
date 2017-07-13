package nlptools;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.Mention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import structures.Caption;
import structures.Cardinality;
import structures.Document;
import structures.Token;
import utilities.Logger;
import utilities.Util;

import java.util.*;

/**StanfordAnnotator wraps a variety of Stanford CoreNLP
 * functions, including tagging, parsing, and coreference
 * resolution
 *
 * Acceptable Annotators:
 * tokenize     tokenizer
 * ssplit       sentence splitter
 * pos          pos tagger
 * lemma        lemmatizer
 * parse        basic parser
 * depparse     dependency parser
 * mention      mention detection
 * coref        coreference resolution
 *
 * Full list in
 * https://stanfordnlp.github.io/CoreNLP/annotators.html
 */
public class StanfordAnnotator
{
    private StanfordCoreNLP _pipeline;


    /**The workflow for StanfordAnnotator is to create
     * objects through the static functions rather than
     * public constructors because of the various
     * context-dependent options available
     *
     * @param props
     */
    private StanfordAnnotator(Properties props)
    {
        _pipeline = new StanfordCoreNLP(props);
    }


    /**Processes the given text as a document with
     * given ID, where the contents of the resulting
     * Document's Captions' contents depend on the way
     * this annotator was set up (as a tagger, parser, or coref)
     *
     * @param docID Document ID
     * @param text  Document text
     * @return      Document with captions annotated with StanfordCoreNLP
     */
    public Document annotate(String docID, String text) {
        //Create and annotate the document
        Annotation document = new Annotation(text);
        _pipeline.annotate(document);

        //Add different aspects to the captions
        //depending on what annotators are available
        //in the pipeline
        boolean hasTagger, hasParser, hasCoref;
        String annotatorsStr =
                _pipeline.getProperties().getProperty("annotators");
        hasTagger = annotatorsStr.contains("pos");
        hasParser = annotatorsStr.contains("parse");
        hasCoref = annotatorsStr.contains("coref");

        //Don't bother continuing if we don't have a tagger, since that's
        //where we get the root tokens from
        if(!hasTagger)
            return null;

        //For each caption, iterate through its tokens and store them
        //in our structures
        int capIdx = 0;
        List<List<Token>> tokenTable = new ArrayList<>();
        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
            int tokIdx = 0;
            List<Token> tokens = new ArrayList<>();
            for (CoreLabel tok : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = tok.get(CoreAnnotations.TextAnnotation.class);
                String pos = tok.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String lemma = tok.get(CoreAnnotations.LemmaAnnotation.class);
                tokens.add(new Token(docID, capIdx, tokIdx, word, lemma, pos));
                tokIdx++;
            }
            tokenTable.add(tokens);
            capIdx++;
        }

        //TODO: Use the parser to get chunks, which at the time of this writing
        //      (20170523) is too much of a pain to bother with

        //If we have a coref annotator, create mentions and coreference chains
        List<List<int[]>> mentionIdxTable = new ArrayList<>();
        if(hasCoref){
            for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
                //Collect the mentions' start token index, end token index,
                //and chain ID
                Set<int[]> mentionIdxSet = new HashSet<>();
                for (Mention m : sentence.get(CorefCoreAnnotations.CorefMentionsAnnotation.class)) {
                    //Store the token indices (and recall that stanford's
                    //indices start at 1 as well as the chain ID (which
                    //starts at 0 and thus must be incremented to avoid conflation
                    //with our nonvisual encoding)
                    List<CoreLabel> tokens = m.originalSpan;
                    int startIdx = tokens.get(0).index()-1;
                    int endIdx = tokens.get(tokens.size()-1).index()-1;
                    mentionIdxSet.add(new int[]{startIdx, endIdx, m.corefClusterID+1});
                }

                //Stanford contains both overlapping mentions
                //and mentions that enclose one another; we want to combine
                //any overlapping mentions and drop any enclosing mentions
                //such that [1 build a [2 bear 1] workshop 1]
                //becomes [ build a bear workshop ]
                //while [1 build [2 a bear 2] workshop 1]
                //becomes [a bear]
                //in order to be more consistent with our chunking
                List<int[]> mentionIdxList = new ArrayList<>();
                while(mentionIdxSet.size() != mentionIdxList.size()){
                    mentionIdxList = new ArrayList<>(mentionIdxSet);
                    int i=0;
                    while(i<mentionIdxList.size() && mentionIdxSet.size() == mentionIdxList.size()){
                        int j=i+1;
                        while(j<mentionIdxList.size() && mentionIdxSet.size() == mentionIdxList.size()){
                            int[] indices_i = mentionIdxList.get(i);
                            int[] indices_j = mentionIdxList.get(j);

                            //NOTE: The stanford mention has an isEnclosed method (or similar), but
                            //      after a few suspicious issues I no longer trust it to do what I expect

                            //1) If j encloses i, remove j
                            if(Util.isEnclosedByRange(indices_i[0], indices_i[1], indices_j[0], indices_j[1])){
                                mentionIdxSet.remove(indices_j);
                            } //2) If i encloses j, remove i
                            else if(Util.isEnclosedByRange(indices_j[0], indices_j[1], indices_i[0], indices_i[1])){
                                mentionIdxSet.remove(indices_i);
                            } //3) If i and j overlap, remove the smaller (or the first, if tied)
                            else if(Util.isOverlappingRange(indices_i[0], indices_i[1], indices_j[0], indices_j[1])){
                                int size_i = indices_i[1] - indices_i[0];
                                int size_j = indices_j[1] - indices_j[0];
                                if(size_j > size_i)
                                    mentionIdxSet.remove(indices_i);
                                else
                                    mentionIdxSet.remove(indices_j);
                            }
                            j++;
                        }
                        i++;
                    }
                }
                //Sort the mention list by indices
                mentionIdxList = new ArrayList<>();
                for(int[] mentionIndices : mentionIdxSet){
                    int insertionIdx = 0;
                    for(int[] mentionIndicesInList : mentionIdxList)
                        if(mentionIndicesInList[0] < mentionIndices[0])
                            insertionIdx++;
                    mentionIdxList.add(insertionIdx, mentionIndices);
                }

                mentionIdxTable.add(mentionIdxList);
            }
        }

        //Actually build and return the captions, given these indices
        List<Caption> captions = new ArrayList<>();
        for(int i=0; i<tokenTable.size(); i++){
            Caption c = new Caption(docID, i, tokenTable.get(i));
            if(i < mentionIdxTable.size()){
                for(int j=0; j<mentionIdxTable.get(i).size(); j++){
                    int[] mentionIndices = mentionIdxTable.get(i).get(j);
                    c.addChunk(j, "NP", mentionIndices[0], mentionIndices[1]);
                    c.addMention(j, "", String.valueOf(mentionIndices[2]),
                            new Cardinality(false), mentionIndices[0], mentionIndices[1]);
                }
            }
            captions.add(c);
        }

        //Associate each token with the appr

        return new Document(docID, captions);
    }

    /**Creates a StanfordAnnotation object for
     * part-of-speech tagging
     *
     * @return A new StanfordAnnotator object for tagging
     */
    public static StanfordAnnotator createTagger()
    {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        return new StanfordAnnotator(props);
    }

    /**Creates a StanfordAnnotation object for
     * dependency parsing; note this also includes
     * part-of-speech tagging
     *
     * @param basicParser Whether to use the basic parser or
     *                    full dependency parser
     * @return            A new Stanfordannotator object for parsing
     */
    public static StanfordAnnotator createParser(boolean basicParser)
    {
        Properties props = new Properties();
        String annotators = "tokenize, ssplit, pos, lemma, ";
        if(basicParser)
            annotators += "parse";
        else
            annotators += "depparse";
        props.setProperty("annotators", annotators);
        return new StanfordAnnotator(props);
    }

    /**Creates a StanfordAnnotation object for
     * coreference resolution; note that this also
     * includes part-of-speech tagging and simple parsing
     *
     * @param neuralModel Whether to use the statistical or
     *                    slower (but better) neural model
     * @return            A new StanfordAnnotator object for coref
     */
    public static StanfordAnnotator createCoreference(boolean neuralModel)
    {
        if(neuralModel) {
            Logger.log("WARNING: specified neural Stanford Coref, " +
                       "but there are internal stanford issues; " +
                       "using statistical model instead");
            neuralModel = false;
        }

        Properties props = new Properties();
        String annotators = "tokenize, ssplit, pos, lemma, ner, depparse, mention, coref";
        props.setProperty("annotators", annotators);
        if(neuralModel){
            props.setProperty("coref.md.type", "dependency");
            props.setProperty("coref.algorithm", "neural");
        } else {
            props.setProperty("coref.algorithm", "statistical");
        }
        return new StanfordAnnotator(props);
    }
}
