package nlptools;

import edu.mit.jwi.IRAMDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.WordnetStemmer;
import structures.Document;
import structures.Mention;
import utilities.Logger;

import java.io.File;
import java.util.*;

/**The WordnetUtil - unlike everything else
 * currently in the legacy package - is actually
 * meant to be instantiated! The resulting
 * object will contain a Wordnet dictionary
 * and methods for interacting with it
 *
 * @author ccervantes
 */
public class WordnetUtil
{
    private WordnetStemmer wnStemmer;
    private IRAMDictionary wordnetDict;

    /**Constructor that creates the WordNet dictionary
     * using a local wordnet directory.
     */
    public WordnetUtil(String wordnetDirPath)
    {
        File wordnetDir = new File(wordnetDirPath);
        wordnetDict = new RAMDictionary(wordnetDir,
                ILoadPolicy.NO_LOAD);
        try {
            wordnetDict.open();
            wordnetDict.load(true);
        } catch(Exception ex) {
            Logger.log(ex);
        }
        wnStemmer = new WordnetStemmer(wordnetDict);
    }

    /**Returns a mapping of lemmas and their hypernyms, for all documents'
     * mentions' head words
     *
     * @param docSet
     * @return
     */
    public Map<String, String> getHypernymDict(Collection<Document> docSet)
    {
        Map<String, String> hypDict = new HashMap<>();
        for (Document d : docSet){
            for (Mention m : d.getMentionList()) {
                String lemma = m.getHead().getLemma().toLowerCase();
                if (!hypDict.containsKey(lemma))
                    buildHypernymDict(lemma, hypDict);
            }
        }
        return hypDict;
    }

    /**Populates the given hypDict with hypernym links, or a mapping of
     * lemma - hypernym lemma pairs for all direct ancestors of
     * the given lemma
     *
     * @param lemma
     * @param hypDict
     */
    private void buildHypernymDict(String lemma, Map<String, String> hypDict)
    {
        //get all stems for the lemma
        for(String stem : wnStemmer.findStems(lemma, POS.NOUN)){
            IIndexWord idxWord = wordnetDict.getIndexWord(stem, POS.NOUN);
            if(idxWord != null){
                //take only the first sense; we're assuming it's
                //the most common usage
                List<IWordID> wordIDList = idxWord.getWordIDs();
                if(wordIDList.size() > 0) {
                    IWordID wordID = wordIDList.get(0);
                    IWord wordInDict = wordnetDict.getWord(wordID);
                    ISynset synset = wordInDict.getSynset();

                    //take the first hypernym in the set
                    List<ISynsetID> hypSet = synset.getRelatedSynsets(Pointer.HYPERNYM);
                    if(hypSet.size() > 0) {
                        ISynsetID synID = hypSet.get(0);
                        ISynset hypSynset = wordnetDict.getSynset(synID);
                        //store the relationship between this lemma and its hypernym,
                        //and recurse up the hyp tree
                        List<IWord> wordList = hypSynset.getWords();
                        if(!wordList.isEmpty()){
                            String hypLemma = wordList.get(0).getLemma().toLowerCase();
                            hypDict.put(lemma, hypLemma);
                            if(!hypDict.containsKey(hypLemma))
                                buildHypernymDict(hypLemma, hypDict);
                        }
                    }
                }
            }
        }
    }
}
