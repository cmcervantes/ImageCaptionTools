package nlptools;


import edu.illinois.cs.cogcomp.chunker.utils.CoNLL2000Parser;
import edu.illinois.cs.cogcomp.lbj.chunk.Chunker;
import edu.illinois.cs.cogcomp.lbj.pos.*;
import edu.illinois.cs.cogcomp.lbjava.nlp.Sentence;
import edu.illinois.cs.cogcomp.lbjava.nlp.SentenceSplitter;
import edu.illinois.cs.cogcomp.lbjava.nlp.Word;
import edu.illinois.cs.cogcomp.lbjava.nlp.WordSplitter;
import edu.illinois.cs.cogcomp.lbjava.nlp.seg.POSBracketToToken;
import edu.illinois.cs.cogcomp.lbjava.nlp.seg.PlainToTokenParser;
import edu.illinois.cs.cogcomp.lbjava.nlp.seg.Token;
import edu.illinois.cs.cogcomp.lbjava.parse.ChildrenFromVectors;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.lbjava.parse.Parser;
import edu.illinois.cs.cogcomp.nlp.lemmatizer.IllinoisLemmatizer;
import structures.Caption;
import structures.Cardinality;
import structures.Mention;
import utilities.DoubleDict;
import utilities.FileIO;
import utilities.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**IllinoisAnnotators wraps tagging and chunking
 * functions from Illinois CogComp
 */
public class IllinoisAnnotator
{
    private IllinoisLemmatizer _lemmatizer;
    private POSTaggerKnown _posTaggerKnown;
    private POSTaggerUnknown _posTaggerUnknown;
    private wordForm __wordForm;
    private Chunker _chunker;


    /**The workflow for IllinoisAnnotator is to create
     * objects through the static functions rather than
     * public constructors because of the various
     * context-dependent options available
     */
    public IllinoisAnnotator()
    {

    }

    /**Initializes the fields necessary for
     * part-of-speech tagging
     *
     * @param modelDir The directory from which to load the model
     */
    private void initTagger(String modelDir)
    {
        _posTaggerKnown =
                new POSTaggerKnown(modelDir + "pos_known.lc",
                        modelDir + "pos_known.lex");
        _posTaggerUnknown =
                new POSTaggerUnknown(modelDir + "pos_unk.lc",
                        modelDir + "pos_unk.lex");
        __wordForm = new wordForm();
        _lemmatizer = new IllinoisLemmatizer();
    }

    /**Initializes the fields necessary for chunking
     *
     * @param modelDir The directory from which to load the model
     */
    private void initChunker(String modelDir)
    {
        _chunker = new Chunker(modelDir + "chunk.lc",
                modelDir + "chunk.lex");
    }

    /**Predicts part-of-speech tags for the given tokens,
     * returning an array of tags as strings
     *
     * @param tokens List of ImageCaptionTools tokens
     * @return       Array of POS tags
     */
    public String[] predictPosTag(List<structures.Token> tokens)
    {
        String[] tags = new String[tokens.size()];
        for(int i=0; i<tokens.size(); i++){
            structures.Token t = tokens.get(i);
            Token illToken = new Token(new Word(t.toString()), null, null);
            tags[i] = predictPosTag(illToken);
        }
        return tags;
    }

    /**Predicts part-of-speech tags for the given
     * sentence, returning an array of Illinois
     * CogComp tokens
     *
     * @param sentence Untokenized sentence
     * @return         Array of tokens with tags
     */
    public Token[] predictPosTag(String sentence)
    {
        List<Token> tokenList = new ArrayList<>();
        Parser parser = new PlainToTokenParser(
                new WordSplitter(new SentenceSplitter(
                        new String[]{sentence})));
        Token t = (Token)parser.next();
        while(t != null){
            t.partOfSpeech = predictPosTag(t);
            t.lemma = _lemmatizer.getLemma(t.form, t.partOfSpeech);
            tokenList.add(t);
            t = (Token)parser.next();
        }
        Token[] tokens = new Token[tokenList.size()];
        return tokenList.toArray(tokens);
    }

    /**Predicts a part-of-speech tag for the given
     * word
     *
     * @param word The Illinois CogComp token to predict the word of
     * @return     The predicted POS tag
     */
    private String predictPosTag(Token word)
    {
        String predTag;
        if(baselineTarget.getInstance().observed(__wordForm.discreteValue(word)))
            predTag = _posTaggerKnown.discreteValue(word);
        else
            predTag = _posTaggerUnknown.discreteValue(word);
        return predTag;
    }

    /**Splits the given block of text into sentences
     *
     * @param text
     * @return
     */
    public String[] splitSentences(String text)
    {
        SentenceSplitter splitter = new SentenceSplitter(new String[]{text});
        Sentence[] sentences = splitter.splitAll();
        String[] sentenceStrings = new String[sentences.length];
        for(int i=0; i<sentences.length; i++)
            sentenceStrings[i] = sentences[i].text;
        return sentenceStrings;
    }

    /**Predicts a BIO-encoding chunk tag for the
     * given word
     *
     * @param word An Illinois CogComp Word object
     * @return     A BIO tag for this word
     */
    private String predictChunkBIO(Word word)
    {
        return _chunker.discreteValue(word);
    }

    /**Predicts a BIO-encoding chunk tag for the
     * given word
     *
     * @param word An Illinois CogComp token object
     * @return     A BIO tag for this word
     */
    public String predictChunkBIO(Token word)
    {
        return _chunker.discreteValue(word);
    }

    /**Predicts the part-of-speech tags and chunk boundaries
     * for the given untokenized sentence, returning an
     * ImageCaptionTools Caption object
     *
     * @param docID    The source Document ID
     * @param capIdx   The sentence's caption index
     * @param sentence A raw text sentence
     * @return         A Caption object
     */
    public Caption predictCaption(String docID, int capIdx, String sentence)
    {
        //Predict the tokens of the sentence
        Token[] tokens = predictPosTag(sentence);

        //Set up the Caption
        Caption c = new Caption(docID, capIdx);
        for(int i=0; i<tokens.length; i++){
            Token t = tokens[i];
            c.addToken(new structures.Token(docID, capIdx, i,
                    t.form, t.lemma, t.partOfSpeech));
        }

        //Predict chunk indices
        int startIdx = -1, chunkIdx = 0, mentionIdx = 0;
        String previousChunkType = "";
        for(int i=0; i<=tokens.length; i++){
            boolean chunkStart = false, chunkEnd = false;
            String chunkType = "";

            //If this is the last token, it's the end of the chunk
            if(i == tokens.length) {
                chunkEnd = true;
            } else {
                //predict this BIO tag
                String predBIO = predictChunkBIO(tokens[i]);
                String[] bioParts = predBIO.split("-");
                if(!bioParts[0].equals("I")){
                    chunkEnd = true;
                    if(bioParts[0].equals("B"))
                        chunkStart = true;
                }
                if(bioParts.length > 1)
                    chunkType = bioParts[1];
            }

            //If this is the end of the chunk for which
            //there's a start index, add it to the caption
            if(chunkEnd && startIdx > -1){
                c.addChunk(chunkIdx, previousChunkType, startIdx, i-1);
                if(previousChunkType.equals("NP")){
                    String headLemma = tokens[i-1].lemma;
                    c.addMention(mentionIdx, Mention.getLexicalEntry_flickr(headLemma),
                            "-1", new Cardinality(false), startIdx, i-1);
                    mentionIdx++;
                }
                startIdx = -1; previousChunkType = "";
                chunkIdx++;
            }

            //if this is the start of a chunk, add this idx
            //as the beginning
            if(chunkStart){
                previousChunkType = chunkType;
                startIdx = i;
            }
        }
        return c;
    }

    /**Tests the IllinoisTagger using the contents of
     * the given testFile and prints the results
     *
     * @param testFile The test data file
     */
    public void testTagger(String testFile)
    {
        DoubleDict<String> wordTagCountDict = new DoubleDict<>();
        DoubleDict<String> goldDict = new DoubleDict<>();
        DoubleDict<String> predDict = new DoubleDict<>();
        DoubleDict<String> correctDict = new DoubleDict<>();
        Parser parser = new POSBracketToToken(testFile);
        Token nextWord = (Token)parser.next();
        while(nextWord != null){
            String predTag = predictPosTag(nextWord);
            String goldTag = nextWord.label;

            if(!predTag.equals(goldTag))
                wordTagCountDict.increment(nextWord.form.toLowerCase().trim() + "_" + goldTag + "|" + predTag);
            goldDict.increment(goldTag);
            predDict.increment(predTag);
            if(goldTag.equals(predTag))
                correctDict.increment(goldTag);
            nextWord = (Token)parser.next();
        }
        List<String> labelList = new ArrayList<>(goldDict.keySet());
        Collections.sort(labelList);
        for(String label : labelList){
            double p = correctDict.get(label) / predDict.get(label);
            double r = correctDict.get(label) / goldDict.get(label);
            double f = 2 * p * r / (p + r);
            System.out.printf("%5s | P:%3.2f%% | R:%3.2f%% | F1:%3.2f%%\n",
                    label, 100.0*p, 100.0*r, 100.0*f);
        }
        System.out.println("----------");
        System.out.printf("Overall accuracy: %.2f\n", 100.0*correctDict.getSum() / goldDict.getSum());
        System.out.println("Printing error dict");
        List<String> keyList = new ArrayList<>(wordTagCountDict.keySet());
        Collections.sort(keyList);
        for(String key : keyList)
            if(wordTagCountDict.get(key)>10)
                System.out.printf("%20s: %d\n", key, (int)wordTagCountDict.get(key));
        System.out.println("total: " + wordTagCountDict.getSum());
    }

    /**As a pre-processing step for chunking evaluation
     * (since chunking eval is done with the CONLL perl
     * scripts), this function predicts BIO-encoded
     * chunk tags for all tokens in the given testFile
     * and outputs the resulting tokens -- with gold
     * and predicted tags -- to the given outFile
     *
     * @param testFile The test data file to read
     * @param outFile  The evaluation file to which to write
     */
    public void testChunker_exportToConll(String testFile, String outFile)
    {
        List<String> outList = new ArrayList<>();
        Parser parser = new ChildrenFromVectors(new CoNLL2000Parser(testFile));
        Word w = (Word) parser.next();
        while(w != null){
            //Word objects, here, are in the format (tag POS text)
            //Since the object itself doesn't store the tag (even in the 'label' field, weirdly)
            //we have to parse the string for it
            String wordStr = w.toString();
            String gold = wordStr.substring(wordStr.indexOf('(')+1, wordStr.indexOf(' '));
            String pos = w.partOfSpeech;
            String pred = predictChunkBIO(w);
            String text = w.form;

            String[] args = {text, pos, gold, pred};
            outList.add(StringUtil.listToString(args, " "));
            w = (Word)parser.next();
        }
        FileIO.writeFile(outList, outFile, "txt");
    }

    /**Trains a new Illinois CogComp tagger using the given
     * trainFile and saves the model to the modelDir
     *
     * @param trainFile  The training data file
     * @param modelDir   The directory to save the model in
     */
    public static void trainTagger(String trainFile, String modelDir)
    {
        POSTrain posTrain = new POSTrain(modelDir);
        posTrain.trainModels(trainFile);
        posTrain.writeModelsToDisk("pos_base", "pos_mik", "pos_known", "pos_unk");
    }

    /**Trains a new Illinois CogComp chunker using the
     * given trainFile using numIter iterations and saves
     * the model to the modelDir
     *
     * @param trainFile The training data file
     * @param numIter   The number of iterations to train with
     * @param modelDir  The directory to save the model in
     */
    public static void trainChunker(String trainFile, int numIter, String modelDir)
    {
        //train the model for numIter iterations
        edu.illinois.cs.cogcomp.lbj.chunk.Chunker chunker =
                new edu.illinois.cs.cogcomp.lbj.chunk.Chunker();
        CoNLL2000Parser parser = new CoNLL2000Parser(trainFile);
        for (int i = 0; i < numIter; i++) {
            LinkedVector ex;
            while ((ex = (LinkedVector)parser.next()) != null) {
                for (int j = 0; j < ex.size(); j++) {
                    chunker.learn(ex.get(j));
                }
            }
        }

        //save the model at the model path
        chunker.write(modelDir + "chunk.lc", modelDir + "chunk.lex");
    }

    /**Creates an IllinoisAnnotator object for part-of-speech tagging
     *
     * @param modelDir The directory to load the model from
     * @return         A new IllinoisAnnotator object for tagging
     */
    public static IllinoisAnnotator createTagger(String modelDir)
    {
        IllinoisAnnotator annotator = new IllinoisAnnotator();
        annotator.initTagger(modelDir);
        return annotator;
    }

    /**Creates an IllinoisAnnoator object for chunking
     *
     * @param modelDir The directory from which to load the model
     * @return         A new IllinoisAnnotator object for chunking
     */
    public static IllinoisAnnotator createChunker(String modelDir)
    {
        IllinoisAnnotator annotator = new IllinoisAnnotator();
        annotator.initChunker(modelDir);
        return annotator;
    }

    /**Creates an IllinoisAnnotator object for tagging and chunking
     *
     * @param tagModelDir   The pos tagging model directory
     * @param chunkModelDir The chunking model directory
     * @return              A new IllinoisAnnotator object for tagging
     *                      and chunking
     */
    public static IllinoisAnnotator createChunker(String tagModelDir, String chunkModelDir)
    {
        IllinoisAnnotator annotator = new IllinoisAnnotator();
        annotator.initTagger(tagModelDir);
        annotator.initChunker(chunkModelDir);
        return annotator;
    }
}
