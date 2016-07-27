package nlptools;

import utilities.Logger;
import utilities.Util;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**Word2VecUtil wraps the process of reading word embeddings
 * from a gzipped word2vec file; to avoid either loading all
 * vectors into memory or opening and re-opening the file,
 * these objects require the set of needed words to be
 * provided up-front.
 *
 * @author ccervantes
 */
public class Word2VecUtil
{
    private Map<String, List<Double>> vectorDict;
    private static List<Double> emptyVector;
    static {
        emptyVector = new ArrayList<>();
        for(int i=0; i<300; i++)
            emptyVector.add(0.0);
    }

    /**Constructs a Word2VecUtil object, using the vectors at
     * the given w2vPath, and reads vectors for the given
     * wordSet
     *
     * @param w2vPath
     * @param wordSet
     */
    public Word2VecUtil(String w2vPath, Collection<String> wordSet)
    {
        vectorDict = new HashMap<>();
        try{
            InputStream gzipStream =
                    new GZIPInputStream(new FileInputStream(w2vPath));
            BufferedReader br =
                    new BufferedReader(new InputStreamReader(gzipStream, "UTF-8"));

            String nextLine = br.readLine();
            while(nextLine != null) {
                String[] lineParts = nextLine.split("\t");
                String word = lineParts[0];
                if(wordSet.contains(word)){
                    List<Double> vector = new ArrayList<>();
                    for(int i=1; i<lineParts.length; i++)
                        vector.add(Double.parseDouble(lineParts[i]));
                    vectorDict.put(word, vector);
                }
                nextLine = br.readLine();
            }
            br.close();
        } catch (IOException ioEx) {
            Logger.log(ioEx);
        }
    }

    /**Returns the cosine similarity between the vectors for
     * the given words
     *
     * @param word1
     * @param word2
     * @return
     */
    public double getWord2VecSim(String word1, String word2)
    {
        List<Double> vec1 = vectorDict.get(word1);
        List<Double> vec2 = vectorDict.get(word2);
        return Util.cosineSimilarity(vec1, vec2);
    }

    /**Returns the word embedding for the given word
     *
     * @param word
     * @return
     */
    public List<Double> getVector(String word){return vectorDict.get(word);}

    /**Returns the vectors for the given words concatenated together,
     * where the words are in alphabetical order
     *
     * @param word1
     * @param word2
     * @return
     */
    public List<Double> getConcatVector(String word1, String word2)
    {
        String s1, s2;
        if(word1 == null)
            word1 = "";
        if(word2 == null)
            word2 = "";

        if(word1.compareToIgnoreCase(word2) < 0) {
            s1 = word1;
            s2 = word2;
        } else {
            s1 = word2;
            s2 = word1;
        }

        List<Double> a = vectorDict.get(s1);
        List<Double> b = vectorDict.get(s2);

        List<Double> c = new ArrayList<>();
        if(a == null)
            a = emptyVector;
        if(b == null)
            b = emptyVector;
        c.addAll(a);
        c.addAll(b);
        return c;
    }
}
