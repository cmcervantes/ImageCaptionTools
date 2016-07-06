package core;

import structures.Caption;
import structures.Document;
import structures.Mention;
import utilities.FileIO;

import java.util.*;

/**DocumentLoader houses static functions to load Document
 * objects from various places, including .coref files,
 * Flickr30kEntities files, and databases
 */
public class DocumentLoader
{



    public static void main(String[] args)
    {
        //Initialize lexicons, word lists, and lemmatizers for
        //loading Documents from a coref file
        /*
        Set<Document> docSet =
            getDocumentSet("/Users/syphonnihil/source/PanOpt/flickr30kEntities_20160628.coref",
            "/Users/syphonnihil/source/data/lexicons/", "/Users/syphonnihil/source/data/");
        Caption.initLemmatizer();

        Map<String, Set<String>> corefDict = new HashMap<>();
        for(String corefStr : corefList){
            String docID = corefStr.split("#")[0];
            if(!corefDict.containsKey(docID))
                corefDict.put(docID, new HashSet<>());
            corefDict.get(docID).add(corefStr);
        }

        for(Set<String> corefStrSet : corefDict.values()){
            Document d = new Document(corefStrSet);
            int x = 0;
            System.exit(0);
        }
        */

        /*
        Mention.initWordLists("/Users/syphonnihil/source/data/");
        String sentenceFile = "/Users/syphonnihil/source/data/Flickr30kEntities/Sentences/3293018193.txt";
        String annotationFile = "/Users/syphonnihil/source/data/Flickr30kEntities/Annotations/3293018193.xml";
        Document d = new Document(sentenceFile, annotationFile);
        int x = 0;*/
    }

    /**Returns a set of Documents, based on a .coref file
     * and the specified lexicon and word list directories
     *
     * @param corefFile
     * @param lexiconDir
     * @param wordListDir
     * @return
     */
    public static Set<Document> getDocumentSet(String corefFile, String lexiconDir, String wordListDir)
    {
        List<String> corefList = FileIO.readFile_lineList(corefFile);
        Mention.initLexiconDict(lexiconDir);
        Mention.initWordLists(wordListDir);
        Caption.initLemmatizer();

        Map<String, Set<String>> corefDict = new HashMap<>();
        for(String corefStr : corefList){
            String docID = corefStr.split("#")[0];
            if(!corefDict.containsKey(docID))
                corefDict.put(docID, new HashSet<>());
            corefDict.get(docID).add(corefStr);
        }

        Set<Document> docSet = new HashSet<>();
        for(Set<String> corefStrSet : corefDict.values())
            docSet.add(new Document(corefStrSet));
        return docSet;
    }

    /**Returns a set of Documents, based on a Flickr30kEntities directory
     * (which contains Sentences/ and Annotations/ directories, each of which has
     * a file used in Document construction)
     *
     * @param flickr30kEntitiesDir
     * @param wordListDir
     * @return
     */
    public static Set<Document> getDocumentSet(String flickr30kEntitiesDir, String wordListDir)
    {
        Mention.initWordLists(wordListDir);

        //get the filenames from the sentences directory (assuming the annotations are the same)
        Set<String> filenames =
                new HashSet<>(FileIO.getFileNamesFromDir(flickr30kEntitiesDir + "Sentences/"));

        Set<Document> docSet = new HashSet<>();
        for(String filename : filenames)
            docSet.add(new Document(flickr30kEntitiesDir + "Sentences/" +filename + ".txt",
                                    flickr30kEntitiesDir + "Annotations/" + filename + ".xml"));
        return docSet;
    }

}
