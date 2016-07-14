package nlptools;

import edu.illinois.cs.cogcomp.lbj.pos.*;
import edu.illinois.cs.cogcomp.lbjava.nlp.SentenceSplitter;
import edu.illinois.cs.cogcomp.lbjava.nlp.Word;
import edu.illinois.cs.cogcomp.lbjava.nlp.WordSplitter;
import edu.illinois.cs.cogcomp.lbjava.nlp.seg.POSBracketToToken;
import edu.illinois.cs.cogcomp.lbjava.nlp.seg.PlainToTokenParser;
import edu.illinois.cs.cogcomp.lbjava.nlp.seg.Token;
import edu.illinois.cs.cogcomp.lbjava.parse.Parser;
import utilities.DoubleDict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The Tagger wraps the Illinois CogComp part-of-speech
 * tagger for use with our structures
 *
 *                  Train       | Test      | Accuracy
 *  tagger README:  00-21       | 22-24     | 96.55
 *  POSTagger    :  00-21       | 22-24     | 96.64
 *               :  00-21       | old dev   | 95.39
 *               :  00-21+train | old dev   | 97.92
 */
public class Tagger
{
    private POSTaggerKnown _posTaggerKnown;
    private POSTaggerUnknown _posTaggerUnknown;
    wordForm __wordForm;

    /**Constructs a Tagger from a pre-trained model
     * specified by modelDir
     *
     * @param modelDir
     */
    public Tagger(String modelDir)
    {
        _posTaggerKnown =
                new POSTaggerKnown(modelDir + "pos_known.lc",
                        modelDir + "pos_known.lex");
        _posTaggerUnknown =
                new POSTaggerUnknown(modelDir + "pos_unk.lc",
                        modelDir + "pos_unk.lex");
        __wordForm = new wordForm();
    }

    /**Returns an array of predicted part-of-speech tags for
     * the given tokenList
     *
     * @param tokenList
     * @return
     */
    public String[] predict(List<structures.Token> tokenList)
    {
        String[] tags = new String[tokenList.size()];
        for(int i=0; i<tokenList.size(); i++){
            structures.Token t = tokenList.get(i);
            Token illToken = new Token(new Word(t.getText()), null, null);
            tags[i] = predict(illToken);
        }
        return tags;
    }

    /**Returns an array of Illinois CogComp Tokens - with
     * predicted part-of-speech tags - given a sentence
     *
     * @param sentence
     * @return
     */
    @Deprecated
    public Token[] predict(String sentence)
    {
        List<Token> tokenList = new ArrayList<>();
        Parser parser = new PlainToTokenParser(
                new WordSplitter(new SentenceSplitter(
                        new String[]{sentence})));
        Token t = (Token)parser.next();
        while(t != null){
            t.partOfSpeech = predict(t);
            tokenList.add(t);
            t = (Token)parser.next();
        }
        Token[] tokens = new Token[tokenList.size()];
        return tokenList.toArray(tokens);
    }

    /**Predicts a part-of-speech tag for the given word,
     * which is an Illinois CogComp Token
     *
     * @param word
     * @return
     */
    private String predict(Token word)
    {
        String predTag;
        if(baselineTarget.getInstance().observed(__wordForm.discreteValue(word))){
            predTag = _posTaggerKnown.discreteValue(word);
        } else {
            predTag = _posTaggerUnknown.discreteValue(word);
        }
        return predTag;
    }

    /**Evaluates this Tagger using the given testFile
     *
     * @param testFile
     */
    public void test(String testFile)
    {
        DoubleDict<String> wordTagCountDict = new DoubleDict<>();
        DoubleDict<String> goldDict = new DoubleDict<>();
        DoubleDict<String> predDict = new DoubleDict<>();
        DoubleDict<String> correctDict = new DoubleDict<>();
        Parser parser = new POSBracketToToken(testFile);
        Token nextWord = (Token)parser.next();
        while(nextWord != null){
            String predTag = predict(nextWord);
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

    /**Trains a new Tagger model, using the given trainingData,
     * and stores the model files to the given modelsDir
     *
     * @param trainingData
     * @param modelsDir
     */
    public static void train(String trainingData, String modelsDir)
    {
        POSTrain posTrain = new POSTrain(modelsDir);
        posTrain.trainModels(trainingData);
        posTrain.writeModelsToDisk("pos_base", "pos_mik",
                                   "pos_known", "pos_unk");
    }
}


