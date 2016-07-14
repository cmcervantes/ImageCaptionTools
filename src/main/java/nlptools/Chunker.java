package nlptools;

import edu.illinois.cs.cogcomp.chunker.utils.CoNLL2000Parser;
import edu.illinois.cs.cogcomp.lbjava.nlp.Word;
import edu.illinois.cs.cogcomp.lbjava.nlp.seg.Token;
import edu.illinois.cs.cogcomp.lbjava.parse.ChildrenFromVectors;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.lbjava.parse.Parser;
import utilities.FileIO;
import utilities.StringUtil;

import java.util.ArrayList;
import java.util.List;


public class Chunker
{
    edu.illinois.cs.cogcomp.lbj.chunk.Chunker chunker;

    /**Constructs a Chunker from a pre-trained model
     * specified by modelDir
     *
     * @param modelDir
     */
    public Chunker(String modelDir)
    {
        chunker = new edu.illinois.cs.cogcomp.lbj.chunk.Chunker(
                    modelDir + "chunk.lc", modelDir + "chunk.lex");
    }

    /**Predicts BIO-encoded chunk tags for all tokens in
     * the given testData file, and outputs the resulting
     * tokens - with gold and predicted tags - to the given outFile
     *
     * @param testData
     * @param outFile
     */
    public void exportToConll(String testData, String outFile)
    {
        List<String> outList = new ArrayList<>();
        Parser parser =
                new ChildrenFromVectors(new CoNLL2000Parser(testData));
        Word w = (Word) parser.next();
        while(w != null){
            //Word objects, here, are in the format (tag POS text)
            //Since the object itself doesn't store the tag (even in the 'label' field, weirdly)
            //we have to parse the string for it
            String wordStr = w.toString();
            String gold = wordStr.substring(wordStr.indexOf('(')+1, wordStr.indexOf(' '));
            String pos = w.partOfSpeech;
            String pred = chunker.discreteValue(w);
            String text = w.form;
            String[] args = {text, pos, gold, pred};
            outList.add(StringUtil.listToString(args, " "));
            w = (Word)parser.next();
        }
        FileIO.writeFile(outList, outFile, "txt");
    }

    /**Predicts a BIO-encoded chunk tag, for the given word
     *
     * @param word
     * @return
     */
    public String predict(Token word)
    {
        return chunker.discreteValue(word);
    }

    /**Trains a new Chunker model for numIter iterations,
     * using the given trainingData, and stores the model
     * files to the given modelsDir
     *
     * @param trainingData
     * @param modelDir
     * @param numIter
     */
    public static void train(String trainingData, String modelDir, int numIter)
    {
        //train the model for numIter iterations
        edu.illinois.cs.cogcomp.lbj.chunk.Chunker chunker =
                new edu.illinois.cs.cogcomp.lbj.chunk.Chunker();
        CoNLL2000Parser parser = new CoNLL2000Parser(trainingData);
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
}
