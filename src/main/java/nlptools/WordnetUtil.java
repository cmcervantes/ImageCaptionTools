package nlptools;

import edu.mit.jwi.IRAMDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.WordnetStemmer;
import utilities.HypTree;
import utilities.Logger;

import java.io.File;
import java.util.*;

/**A WordnetUtil object contains
 * a Wordnet dictionary and methods
 * for interacting with it
 *
 * @author ccervantes
 */
public class WordnetUtil
{
    private static Set<String> _rootHypernyms;
    static {
        _rootHypernyms = new HashSet<>();
        String[] hypArr = {"concept","weather","clothing",
                "group","person","equipment","location",
                "animal","body_covering","plant", "substance", "wheeled_vehicle",
                "abstraction","vehicle","natural_object","artifact",
                "device","structure","furniture","physical_entity",
                "body_part","matter", "measure",
                "cognition", "event", "natural_phenomenon"};
        _rootHypernyms.addAll(Arrays.asList(hypArr));
    }

    private WordnetStemmer wnStemmer;
    private IRAMDictionary wordnetDict;


    /**Constructor that creates the WordNet dictionary
     * using a local wordnet directory.
     *
     * @author ccervantes
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

    /**Returns whether any of the senses in the hypernym
     * tree of lemma_i is a meronym of any of the senses in
     * the hypernym tree of lemma_j
     *
     * @param lemma_i
     * @param lemma_j
     * @return
     */
    public boolean isMeronym(String lemma_i, String lemma_j)
    {
        HypTree hyps_i = getHypernymTree(lemma_i);
        HypTree hyps_j = getHypernymTree(lemma_j);
        Set<ISynset> senses_i = hyps_i.getAllSenses();
        Set<ISynset> senses_j = hyps_j.getAllSenses();

        for(ISynset sense_i : senses_i){
            //List<String> meroIDs = new HashSet<>();
            //for(ISynsetID meroID : sense_i.getRelatedSynsets(Pointer.MERONYM_PART))

            List<ISynsetID> meroIDs =
                    new ArrayList<>(sense_i.getRelatedSynsets(Pointer.MERONYM_PART));
            meroIDs.addAll(new ArrayList<>(sense_i.getRelatedSynsets(Pointer.MERONYM_MEMBER)));
            for(ISynset sense_j : senses_j)
                for(ISynsetID meroID : meroIDs)
                    if(meroID.equals(sense_j.getID()))
                        return true;
        }

        return false;
    }


    /**Returns the hypernym tree for this lemma, where a sense
     * is retained if its frequency count is greater than 0 or
     * -- if no such sense is present -- the first sense is taken;
     * returns a HypTree
     *
     * @param lemma - The lemma for which hypernyms are returned
     * @return      - A HypTree
     */
    public HypTree getHypernymTree(String lemma)
    {
        HypTree tree = new HypTree(lemma);

        //iterate over all the stems of this lemma
        for(String stem : wnStemmer.findStems(lemma, POS.NOUN)) {
            IIndexWord idxWord = wordnetDict.getIndexWord(stem, POS.NOUN);

            if (idxWord != null) {
                //We keep only those senses that have frequency counts > 0;
                //If no frequency counts are greater than 0; take only the
                //first sense
                List<IWordID> wordIDs = new ArrayList<>();
                for (IWordID wordID : idxWord.getWordIDs()) {
                    IWord word = wordnetDict.getWord(wordID);
                    if (wordnetDict.getSenseEntry(word.getSenseKey()).getTagCount() > 0)
                        wordIDs.add(wordID);
                }
                if (wordIDs.isEmpty())
                    wordIDs.add(idxWord.getWordIDs().get(0));
                wordIDs = wordIDs.subList(0, Math.min(3, wordIDs.size()));
                for (IWordID wordID : wordIDs) {
                    IWord word = wordnetDict.getWord(wordID);
                    int tagCount = wordnetDict.getSenseEntry(word.getSenseKey()).getTagCount();
                    ISynset synset = word.getSynset();
                    HypTree.HypNode node = tree.addChild(synset, null, tagCount);
                    buildHypernymTree(synset, node, tree);
                }
            }
        }

        return tree;
    }

    /**At each step, this method iterates through the hypernyms of the given
     * synset, adding those hypernyms as children to the given lemma in the
     * given tree, and recurses on each hypernym
     *
     * @param synset - The current synset (for which hypernyms are produced)
     * @param tree   - The tree to build
     */
    private void buildHypernymTree(ISynset synset, HypTree.HypNode lastNode, HypTree tree)
    {
        //Add the hypernyms of this synset to the tree and recurse
        for(ISynsetID hypID : synset.getRelatedSynsets(Pointer.HYPERNYM)){
            ISynset hypSyn = wordnetDict.getSynset(hypID);
            IWord hypWord = hypSyn.getWords().get(0);
            int tagCount = wordnetDict.getSenseEntry(hypWord.getSenseKey()).getTagCount();
            HypTree.HypNode node = tree.addChild(hypSyn, lastNode, tagCount);
            buildHypernymTree(hypSyn, node, tree);
        }
    }

    /**Recursive tree-building for hypernym synset trees
     *
     * @param synset
     * @param lastNode
     * @param tree
     */
    private void _buildHyponymTree(ISynset synset, HypTree.HypNode lastNode, HypTree tree)
    {
        //Add the hypernyms of this synset to the tree and recurse
        for(ISynsetID hypID : synset.getRelatedSynsets(Pointer.HYPONYM)){
            ISynset hypSyn = wordnetDict.getSynset(hypID);
            IWord hypWord = hypSyn.getWords().get(0);
            int tagCount = wordnetDict.getSenseEntry(hypWord.getSenseKey()).getTagCount();
            HypTree.HypNode node = tree.addChild(hypSyn, lastNode, tagCount);
            buildHypernymTree(hypSyn, node, tree);
        }
    }

    /**Returns the bag-of-hypernyms representation of the lemma,
     * according to the static set of root hypernyms
     *
     * @param lemma
     * @return
     */
    public Set<String> getBagOfHypernyms(String lemma)
    {
        Set<String> boh = new HashSet<>();
        HypTree hypTree = getHypernymTree(lemma);
        for(List<HypTree.HypNode> branch : hypTree.getRootBranches(true)){
            String leaf = null;
            for(HypTree.HypNode h : branch){
                String hStr = h.toString();
                if(_rootHypernyms.contains(hStr)){
                    leaf = hStr; break;
                }
            }
            if(leaf != null)
                boh.add(leaf);
        }
        return boh;
    }
}
