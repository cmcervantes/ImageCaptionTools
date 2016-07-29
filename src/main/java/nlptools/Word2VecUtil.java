package nlptools;

import utilities.DoubleDict;
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
    private int _maxVectors;
    private Map<String, List<Double>> _vectorDict;
    private DoubleDict<String> _wordFreqDict;
    private String _w2vPath;
    private Set<String> _unknownWords;
    private static List<Double> _emptyVector;
    static {
        _emptyVector = new ArrayList<>();
        for(int i=0; i<300; i++)
            _emptyVector.add(0.0);
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
        _vectorDict = new HashMap<>();
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
                    _vectorDict.put(word, vector);
                }
                nextLine = br.readLine();
            }
            br.close();
        } catch (IOException ioEx) {
            Logger.log(ioEx);
        }
        _w2vPath = null;
        _maxVectors = -1;
        _wordFreqDict = null;
        _unknownWords = null;
    }

    /**Constructs an empty Word2VecUtil object, which
     * opens the file each time a new word is queried for;
     * the given maxVectors specifies how many vectors to
     * store in memory, when at its limit, low frequency
     * words are replaced with the new requests
     *
     * @param w2vPath
     * @param maxVectors
     */
    public Word2VecUtil(String w2vPath, int maxVectors)
    {
        _w2vPath = w2vPath;
        _maxVectors = maxVectors;
        _vectorDict = new HashMap<>();
        _wordFreqDict = new DoubleDict<>();
        _unknownWords = new HashSet<>();
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
        List<Double> vec1 = _vectorDict.get(word1);
        List<Double> vec2 = _vectorDict.get(word2);
        return Util.cosineSimilarity(vec1, vec2);
    }

    /**Returns the word embedding for the given word
     *
     * @param word
     * @return
     */
    public List<Double> getVector(String word)
    {
        List<Double> vectorToReturn = _emptyVector;
        if(_maxVectors < 0){
            //if this is a preloaded word2vecUtil, just return the vector
            if(_vectorDict.containsKey(word))
                vectorToReturn = _vectorDict.get(word);
        } else {
            //if this is an as-needed word2vec util,
            //first determine if we've already seen this word
            if(_vectorDict.containsKey(word)) {
                vectorToReturn = _vectorDict.get(word);
                _wordFreqDict.increment(word);
            } else if(!_unknownWords.contains(word)) {
                //if this word isn't in our dict nor in our
                //unknown set, read the file
                try{
                    InputStream gzipStream =
                            new GZIPInputStream(new FileInputStream(_w2vPath));
                    BufferedReader br =
                            new BufferedReader(new InputStreamReader(gzipStream, "UTF-8"));
                    String prefix = word + "\t";
                    String nextLine = br.readLine();
                    while(nextLine != null) {
                        if(nextLine.startsWith(prefix)){
                            String[] lineParts = nextLine.split("\t");
                            vectorToReturn = new ArrayList<>();
                            for(int i=1; i<lineParts.length; i++)
                                vectorToReturn.add(Double.parseDouble(lineParts[i]));
                            break;
                        }
                        nextLine = br.readLine();
                    }
                    br.close();
                } catch (IOException ioEx) {
                    Logger.log(ioEx);
                }

                //if this word wasn't found in the file, this is
                //unknown and we shouldn't look for it again
                if(vectorToReturn.equals(_emptyVector)){
                    _unknownWords.add(word);
                } else {
                    //if we're at our max stored words,
                    //remove the least frequent
                    if(_vectorDict.size() >= _maxVectors){
                        String leastSeenWord = _wordFreqDict.getSortedByValKeys(false).get(0);
                        _vectorDict.remove(leastSeenWord);
                        _wordFreqDict.remove(leastSeenWord);
                    }

                    //add this new word to the dict
                    _vectorDict.put(word, vectorToReturn);
                    _wordFreqDict.increment(word);
                }
            }
        }

        return vectorToReturn;
    }

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

        List<Double> a = _vectorDict.get(s1);
        List<Double> b = _vectorDict.get(s2);

        List<Double> c = new ArrayList<>();
        if(a == null)
            a = _emptyVector;
        if(b == null)
            b = _emptyVector;
        c.addAll(a);
        c.addAll(b);
        return c;
    }
}
