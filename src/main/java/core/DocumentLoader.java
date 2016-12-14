package core;

import nlptools.DependencyParser;
import nlptools.WordnetUtil;
import structures.*;
import utilities.*;

import javax.sql.rowset.CachedRowSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**DocumentLoader houses static functions to load Document
 * objects from various places, including .coref files,
 * Flickr30kEntities files, and databases
 */
public class DocumentLoader
{
    public static void main(String[] args)
    {
        WordnetUtil wnUtil =
                new WordnetUtil("/Users/syphonnihil/source/data/WordNet-3.0/dict");


        String[] headArr = {"sunglass", "hand", "room", "woman",
                "top", "shorts", "picture", "worker", "glass",
                "jacket", "day", "water", "grass", "stage", "baby",
                "dog", "ball", "microphone", "suit", "who", "game",
                "food", "he", "it", "pants", "that", "head", "them",
                "dress", "area", "lady", "t-shirt", "guy", "wall",
                "hat", "chair", "him", "something", "face", "guitar",
                "bench", "bicycle", "pool", "jeans", "street", "crowd",
                "side", "sign", "kid", "person", "player", "bike", "car",
                "park", "beach", "field", "man", "background", "sidewalk",
                "girl", "shirt", "camera", "coat", "one", "ground",
                "child", "there", "snow", "helmet", "tree", "people",
                "uniform", "building", "couple", "other", "air", "boat",
                "arm", "bag", "table", "boy", "horse", "road", "rock", "hair"};

        for(int i=0; i<headArr.length; i++){
            HypTree hypTree = wnUtil.getHypernymTree(headArr[i]);
            for(List<HypTree.HypNode> branch : hypTree.getRootBranches())
                System.out.println(StringUtil.listToString(branch, " | "));
            //hypTree.prettyPrint();

            System.out.println("Continue (" + i + ")?");
            char c = ' ';
            BufferedReader br =
                    new BufferedReader(new InputStreamReader(System.in));
            try{
                String line = br.readLine();
                if(line.length() == 1)
                    c = line.toCharArray()[0];
                while(c != 'y' && c != 'Y' && c != 'n' && c != 'N'){
                    System.out.print("y or n, please: ");
                    line = br.readLine();
                    if(line.length() == 1)
                        c = line.toCharArray()[0];
                }
            } catch (IOException ioEx) {
                Logger.log(ioEx);
            }
            if(c == 'n' || c == 'N')
                System.exit(0);
        }
    }

    /**Returns a set of Documents, based on a .coref file
     * and the specified lexicon and word list directories
     *
     * @param corefFile
     * @param lexiconDir
     * @param wordListDir
     * @return
     */
    public static Collection<Document> getDocumentSet(String corefFile, String lexiconDir, String wordListDir)
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
    public static Collection<Document> getDocumentSet(String flickr30kEntitiesDir, String wordListDir)
    {
        Mention.initWordLists(wordListDir);

        //get the filenames from the sentences directory (assuming the annotations are the same)
        Set<String> filenames =
                new HashSet<>(FileIO.getFileNamesFromDir(flickr30kEntitiesDir + "Sentences/"));

        Set<Document> docSet = new HashSet<>();
        for(String filename : filenames)
            docSet.add(new Document(flickr30kEntitiesDir + "Sentences/" +filename,
                                    flickr30kEntitiesDir + "Annotations/" + filename.replace("txt", "xml")));
        return docSet;
    }

    /**Returns a set of Documents constructed from the database specified in
     * the given conn, selecting all images; note that this database must be
     * of the same format as one produced by populateDocumentDB()
     *
     * @param conn
     * @return
     */
    public static Collection<Document> getDocumentSet(DBConnector conn)
    {
        return getDocumentSet(conn, -1);
    }

    /**Returns a set of Documents constructed from the database specified in
     * the given conn, selecting only those images specified by the given
     * crossVal flag; note that this database must be of the same format as
     * one produced by populateDocumentDB()
     *
     * @param conn
     * @param crossVal
     * @return
     */
    public static Collection<Document> getDocumentSet(DBConnector conn, int crossVal)
    {
        CachedRowSet rs;
        String query;
        Map<String, Document> docDict = new HashMap<>();

        try{
            Logger.log("Initializing Documents from <image>");
            if(crossVal < 0){
                query = "SELECT img_id, height, width, "+
                        "cross_val, reviewed FROM image";
            } else {
                query = "SELECT img_id, height, width, "+
                        "cross_val, reviewed FROM image "+
                        "WHERE cross_val=" +
                        crossVal;
            }
            rs = conn.query(query);
            while(rs.next()){
                Document d = new Document(rs.getString("img_id"));
                d.height = rs.getInt("height");
                d.width = rs.getInt("width");
                d.crossVal = rs.getInt("cross_val");
                d.reviewed = rs.getBoolean("reviewed");
                docDict.put(d.getID(), d);
            }

            Logger.log("Building Tokens and Captions from <token>");
            if(crossVal < 0){
                query = "SELECT img_id, caption_idx, token_idx, " +
                        "token, lemma, pos_tag FROM token";
            } else {
                query = "SELECT token.img_id, token.caption_idx, "+
                        "token.token_idx, token.token, token.lemma, " +
                        "token.pos_tag FROM token JOIN image ON "+
                        "token.img_id=image.img_id "+
                        "WHERE image.cross_val="+crossVal;
            }
            rs = conn.query(query);
            while(rs.next()){
                String imgID = rs.getString("img_id");
                int captionIdx = rs.getInt("caption_idx");
                int tokenIdx = rs.getInt("token_idx");
                String text = rs.getString("token");
                String lemma = rs.getString("lemma");
                String posTag = rs.getString("pos_tag");
                Token t = new Token(imgID, captionIdx, tokenIdx,
                        text, lemma, posTag);

                //Add a new caption to this document, if this index
                //doesn't already exist; add the Token to this caption
                Caption c = docDict.get(imgID).getCaption(captionIdx);
                if(c == null){
                    c = new Caption(imgID, captionIdx);
                    docDict.get(imgID).addCaption(c);
                }
                c.addToken(t);
            }

            Logger.log("Partitioning Tokens into Chunks with <chunk>");
            if(crossVal < 0){
                query = "SELECT img_id, caption_idx, chunk_idx, " +
                        "start_token_idx, end_token_idx, chunk_type "+
                        "FROM chunk";
            } else {
                query = "SELECT chunk.img_id, chunk.caption_idx, "+
                        "chunk.chunk_idx, chunk.start_token_idx, "+
                        "chunk.end_token_idx, chunk.chunk_type "+
                        "FROM chunk JOIN image ON chunk.img_id=image.img_id "+
                        "WHERE image.cross_val="+crossVal;
            }
            rs = conn.query(query);
            while(rs.next()){
                String imgID = rs.getString("img_id");
                int captionIdx = rs.getInt("caption_idx");
                int chunkIdx = rs.getInt("chunk_idx");
                int startTokenIdx = rs.getInt("start_token_idx");
                int endTokenIdx = rs.getInt("end_token_idx");
                String chunkType = rs.getString("chunk_type");
                docDict.get(imgID).getCaption(captionIdx).addChunk(chunkIdx,
                        chunkType, startTokenIdx, endTokenIdx);
            }

            Logger.log("Loading bounding boxes from <box>");
            if(crossVal < 0){
                query = "SELECT img_id, box_id, x_min, y_min, " +
                        "x_max, y_max FROM box";
            } else {
                query = "SELECT box.img_id, box.box_id, "+
                        "box.x_min, box.y_min, " +
                        "box.x_max, box.y_max FROM box "+
                        "JOIN image ON box.img_id=image.img_id "+
                        "WHERE image.cross_val="+crossVal;
            }
            Map<String, Map<Integer, BoundingBox>> boxDict = new HashMap<>();
            rs = conn.query(query);
            while(rs.next()){
                String imgID = rs.getString("img_id");
                int boxID = rs.getInt("box_id");
                int xMin = rs.getInt("x_min");
                int yMin = rs.getInt("y_min");
                int xMax = rs.getInt("x_max");
                int yMax = rs.getInt("y_max");
                if(!boxDict.containsKey(imgID))
                    boxDict.put(imgID, new HashMap<>());
                boxDict.get(imgID).put(boxID,
                        new BoundingBox(imgID, boxID, xMin, yMin, xMax, yMax));
            }

            Logger.log("Loading chains from <chain> and associating them with "+
                    "the bounding boxes from the previous step");
            if(crossVal < 0){
                query = "SELECT img_id, chain_id, assoc_box_ids, " +
                        "is_scene, is_orig_nobox FROM chain";
            } else {
                query = "SELECT chain.img_id, chain.chain_id, "+
                        "chain.assoc_box_ids, chain.is_scene, "+
                        "chain.is_orig_nobox "+
                        "FROM chain JOIN image ON "+
                        "chain.img_id=image.img_id "+
                        "WHERE image.cross_val="+crossVal;
            }
            rs = conn.query(query);
            while(rs.next()){
                String imgID = rs.getString("img_id");
                String chainID = rs.getString("chain_id");
                boolean isScene = rs.getBoolean("is_scene");
                boolean isOrigNobox = rs.getBoolean("is_orig_nobox");
                String assocBoxIDs = rs.getString("assoc_box_ids");
                Chain c = new Chain(imgID, chainID);
                c.isScene = isScene;
                c.isOrigNobox = isOrigNobox;
                if(assocBoxIDs != null){
                    for(String assocBox : assocBoxIDs.split("\\|")){
                        Integer boxID = Integer.parseInt(assocBox);
                        if(boxDict.get(imgID).containsKey(boxID))
                            c.addBoundingBox(boxDict.get(imgID).get(boxID));
                    }
                }
                docDict.get(imgID).addChain(c);
            }

            //add chain 0 to all documents
            for(Document d : docDict.values())
                d.addChain(new Chain(d.getID(), "0"));

            Logger.log("Partitioning Tokens into Mentions with <mention>");
            if(crossVal < 0){
                query = "SELECT img_id, caption_idx, mention_idx, " +
                        "start_token_idx, end_token_idx, card_str, " +
                        "chain_id, lexical_type FROM mention";
            } else {
                query = "SELECT mention.img_id, mention.caption_idx, "+
                        "mention.mention_idx, mention.start_token_idx, "+
                        "mention.end_token_idx, mention.card_str, " +
                        "mention.chain_id, mention.lexical_type "+
                        "FROM mention JOIN image ON "+
                        "mention.img_id=image.img_id WHERE "+
                        "image.cross_val="+crossVal;
            }
            rs = conn.query(query);
            while(rs.next()){
                String imgID = rs.getString("img_id");
                int captionIdx = rs.getInt("caption_idx");
                int mentionIdx = rs.getInt("mention_idx");
                int startTokenIdx = rs.getInt("start_token_idx");
                int endTokenIdx = rs.getInt("end_token_idx");
                String cardStr = rs.getString("card_str");
                Cardinality card = null;
                try{
                    if(cardStr != null)
                        card = new Cardinality(cardStr);
                }catch(Exception ex){
                    Logger.log(ex);
                }
                String chainID = rs.getString("chain_id");
                String lexicalType = rs.getString("lexical_type");

                Mention m = docDict.get(imgID).getCaption(captionIdx).addMention(mentionIdx,
                        lexicalType, chainID, card, startTokenIdx, endTokenIdx);
                docDict.get(imgID).addMentionToChain(m);
            }

            Logger.log("Reading dependency trees from <dependency>");
            if(crossVal < 0){
                query = "SELECT img_id, caption_idx, gov_token_idx, " +
                        "dep_token_idx, relation FROM dependency";
            } else {
                query = "SELECT dependency.img_id, dependency.caption_idx, "+
                        "dependency.gov_token_idx, dependency.dep_token_idx, "+
                        "dependency.relation FROM dependency JOIN image "+
                        "ON dependency.img_id=image.img_id WHERE cross_val="+crossVal;
            }
            Map<String, Map<Integer, Set<String>>> depDict = new HashMap<>();
            rs = conn.query(query);
            while(rs.next()){
                String imgID = rs.getString("img_id");
                int captionIdx = rs.getInt("caption_idx");
                int govTokenIdx = rs.getInt("gov_token_idx");
                int depTokenIdx = rs.getInt("dep_token_idx");
                String relation = rs.getString("relation");
                if(!depDict.containsKey(imgID))
                    depDict.put(imgID, new HashMap<>());
                if(!depDict.get(imgID).containsKey(captionIdx))
                    depDict.get(imgID).put(captionIdx, new HashSet<>());
                depDict.get(imgID).get(captionIdx).add(govTokenIdx +
                        "|" + relation + "|" + depTokenIdx);
            }
            for(String imgID : depDict.keySet())
                for(Integer capIdx : depDict.get(imgID).keySet())
                    docDict.get(imgID).getCaption(capIdx).setRootNode(depDict.get(imgID).get(capIdx));

        } catch(Exception ex) {
            Logger.log(ex);
        }
        Logger.log("Document loading complete");
        return new HashSet<>(docDict.values());
    }

    /**Populates an ostensibly empty database (specified with the conn)
     * with the Documents in the given docSet
     *
     * @param conn
     * @param docSet
     * @param batchSize
     * @param numThreads
     * @throws Exception
     */
    public static void populateDocumentDB(DBConnector conn, Collection<Document> docSet,
                      int batchSize, int numThreads) throws Exception
    {
        String query;
        Set<Object[]> paramSet;
        String insertPrefix = "INSERT";
        if(conn.getDBType() == DBConnector.DBType.SQLITE)
            insertPrefix += " OR IGNORE";
        else if(conn.getDBType() == DBConnector.DBType.MYSQL)
            insertPrefix += " IGNORE";
        insertPrefix += " INTO ";

        /* The <image> table stores basic image information,
         * like the ID, dimentions, and data split */
        Logger.log("Creating <image>");
        query = "CREATE TABLE IF NOT EXISTS image (img_id VARCHAR(15), "+
                "height INT, width INT, reviewed TINYINT(1), cross_val "+
                "TINYINT(1), anno_comments TEXT, PRIMARY KEY(img_id));";
        conn.createTable(query);
        query = insertPrefix + "image(img_id, height, width, reviewed, "+
                "cross_val, anno_comments) VALUES (?, ?, ?, ?, ?, ?);";
        paramSet = new HashSet<>();
        for(Document d : docSet) {
            paramSet.add(new Object[]{d.getID(), d.height,
                    d.width, d.reviewed, d.crossVal, d.comments});
        }
        conn.update(query, paramSet, batchSize, numThreads);

        /* While not strictly necessary, the <caption> table
         * contains the full caption string so we can easily
         * look up captions of various types */
        Logger.log("Creating <caption>");
        query = "CREATE TABLE IF NOT EXISTS caption (img_id VARCHAR(15), "+
                "caption_idx TINYINT(4), caption TEXT, " +
                "PRIMARY KEY(img_id, caption_idx));";
        conn.createTable(query);
        query = insertPrefix + "caption(img_id, caption_idx, caption) "+
                               "VALUES (?, ?, ?);";
        paramSet = new HashSet<>();
        for(Document d : docSet)
            for(Caption c : d.getCaptionList())
                paramSet.add(new Object[]{d.getID(), c.getIdx(), c.toString()});
        conn.update(query, paramSet, batchSize, numThreads);

        /* The <token> table contains the core token information,
         * including the text, lemma, and part of speech tag*/
        Logger.log("Creating <token>");
        query = "CREATE TABLE IF NOT EXISTS token (img_id VARCHAR(15), "+
                "caption_idx TINYINT(4), token_idx TINYINT(4), " +
                "token VARCHAR(50), lemma VARCHAR(50), pos_tag VARCHAR(6), "+
                "PRIMARY KEY(img_id, caption_idx, token_idx));";
        conn.createTable(query);
        query = insertPrefix + "token(img_id, caption_idx, "+
                "token_idx, token, lemma, pos_tag) "+
                "VALUES (?, ?, ?, ?, ?, ?);";
        paramSet = new HashSet<>();
        for(Document d : docSet){
            for(Caption c : d.getCaptionList()) {
                for (Token t : c.getTokenList()) {
                    paramSet.add(new Object[]{d.getID(), c.getIdx(),
                            t.getIdx(), t.toString(), t.getLemma(),
                            t.getPosTag()});
                }
            }
        }
        conn.update(query, paramSet, batchSize, numThreads);

        /* The <chunk> table does not contain the chunks themselves, but
         * enables us to organize tokens into chunks */
        Logger.log("Creating <chunk>");
        query = "CREATE TABLE IF NOT EXISTS chunk (img_id VARCHAR(15), "+
                "caption_idx TINYINT(4), chunk_idx TINYINT(4), " +
                "start_token_idx TINYINT(4), end_token_idx TINYINT(4), " +
                "chunk_type VARCHAR(10), PRIMARY KEY(img_id, "+
                "caption_idx, chunk_idx));";
        conn.createTable(query);
        query = insertPrefix + "chunk(img_id, caption_idx, "+
                "chunk_idx, start_token_idx, end_token_idx, chunk_type) "+
                "VALUES (?, ?, ?, ?, ?, ?);";
        paramSet = new HashSet<>();
        for(Document d : docSet){
            for(Caption c : d.getCaptionList()) {
                for(Chunk ch : c.getChunkList()){
                    //It's possible, in the old version of the data,
                    //for there to be empty chunks. Log these, but
                    //ignore them otherwise
                    if(!ch.toString().isEmpty()){
                        int[] tokenIndices = ch.getTokenRange();
                        paramSet.add(new Object[]{d.getID(), c.getIdx(),
                                ch.getIdx(), tokenIndices[0], tokenIndices[1],
                                ch.getChunkType()});
                    } else {
                        Logger.log("Error: missing chunk %d (doc:%s;cap:%d",
                                    ch.getIdx(), d.getID(), c.getIdx());
                    }
                }
            }
        }
        conn.update(query, paramSet, batchSize, numThreads);

        /* Like the <chunk> table, the <mention> table does not
         * contain mentions, but the indices necessary to build them
         * from tokens */
        Logger.log("Creating <mention>");
        query = "CREATE TABLE IF NOT EXISTS mention (img_id VARCHAR(15), "+
                "caption_idx TINYINT(4), mention_idx TINYINT(4), " +
                "start_token_idx TINYINT(4), end_token_idx TINYINT(4), " +
                "card_str VARCHAR(10), chain_id VARCHAR(10), "+
                "lexical_type VARCHAR(20), PRIMARY KEY(img_id, "+
                "caption_idx, mention_idx));";
        conn.createTable(query);
        query = insertPrefix + "mention(img_id, caption_idx, mention_idx, "+
                "start_token_idx, end_token_idx, card_str, chain_id, "+
                "lexical_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        paramSet = new HashSet<>();
        for(Document d : docSet){
            for(Caption c : d.getCaptionList()) {
                for(Mention m : c.getMentionList()){
                    int[] tokenIndices = m.getTokenRange();
                    String cardStr = null;
                    if(m.getCardinality() != null)
                        cardStr = m.getCardinality().toString();
                    paramSet.add(new Object[]{d.getID(), c.getIdx(),
                        m.getIdx(), tokenIndices[0], tokenIndices[1],
                        cardStr, m.getChainID(), m.getLexicalType()});
                }
            }
        }
        conn.update(query, paramSet, batchSize, numThreads);

        /* The <chain> table associates chains to boxes
         * (via a single pipe-separated string) as well as
         * specifies whether it should have the scene flag */
        Logger.log("Creating <chain>");
        query = "CREATE TABLE IF NOT EXISTS chain (img_id VARCHAR(15), "+
                "chain_id VARCHAR(10), assoc_box_ids VARCHAR(50), "+
                "is_scene TINYINT(1), is_orig_nobox TINYINT(1), "+
                "PRIMARY KEY(img_id, chain_id));";
        conn.createTable(query);
        query = insertPrefix + "chain(img_id, chain_id, assoc_box_ids, "+
                "is_scene, is_orig_nobox) VALUES (?, ?, ?, ?, ?);";
        paramSet = new HashSet<>();
        for(Document d : docSet){
            for(Chain c : d.getChainSet()){
                Set<String> boxIDs = new HashSet<>();
                c.getBoundingBoxSet().forEach(b -> boxIDs.add(""+b.getIdx()));
                String boxIdStr = null;
                if(!boxIDs.isEmpty())
                    boxIdStr = StringUtil.listToString(boxIDs,"|");
                paramSet.add(new Object[]{d.getID(), c.getID(),
                        boxIdStr, c.isScene, c.isOrigNobox});
            }
        }
        conn.update(query, paramSet, batchSize, numThreads);

        /* The <box> table contains the dataset's bounding boxes
         */
        Logger.log("Creating <box>");
        query = "CREATE TABLE IF NOT EXISTS box (img_id VARCHAR(15), "+
                "box_id INT, x_min INT, y_min INT, x_max INT, "+
                "y_max INT, PRIMARY KEY(img_id, box_id));";
        conn.createTable(query);
        query = insertPrefix + "box(img_id, box_id, x_min, y_min, " +
                "x_max, y_max) VALUES (?, ?, ?, ?, ?, ?);";
        paramSet = new HashSet<>();
        for(Document d : docSet){
            for(BoundingBox b : d.getBoundingBoxSet()){
                paramSet.add(new Object[]{d.getID(), b.getIdx(), b.getXMin(),
                        b.getYMin(), b.getXMax(), b.getYMax()});
            }
        }
        conn.update(query, paramSet, batchSize, numThreads);


        /* The <dependency> table contains arcs constructed from
         * the Stanford Dependency parser
         */
        Logger.log("Creating <dependency>");
        DependencyParser parser = new DependencyParser();
        int totalCaps = 0;
        for(Document d : docSet)
            totalCaps += d.getCaptionList().size();
        int numCaps = 0;
        for(Document d : docSet) {
            for (Caption c : d.getCaptionList()) {
                c.setRootNode(parser.predict(c));
                numCaps++;
                Logger.logStatus("Parsed %d (%.2f%%) captions",
                        numCaps, 100.0 * (double)numCaps / totalCaps);
            }
        }
        query = "CREATE TABLE IF NOT EXISTS dependency (img_id VARCHAR(15), "+
                "caption_idx TINYINT(4), gov_token_idx TINYINT(4), "+
                "dep_token_idx TINYINT(4), relation VARCHAR(10), "+
                "PRIMARY KEY(img_id, caption_idx, gov_token_idx, dep_token_idx));";
        conn.createTable(query);
        query = insertPrefix + "dependency(img_id, caption_idx, gov_token_idx, "+
                "dep_token_idx, relation) VALUES (?, ?, ?, ?, ?);";
        paramSet = new HashSet<>();
        for(Document d : docSet){
            for(Caption c : d.getCaptionList()){
                DependencyNode root = c.getRootNode();
                if(root != null) {
                    for (DependencyNode node : root.getAllNodesInTree()) {
                        int depTokenIdx = node.getToken().getIdx();
                        int govTokenIdx = -1;
                        String rel = "ROOT";
                        if (node.getGovernor() != null) {
                            govTokenIdx = node.getGovernor().getToken().getIdx();
                            rel = node.getRelationToGovernor();
                        }
                        paramSet.add(new Object[]{d.getID(), c.getIdx(),
                                govTokenIdx, depTokenIdx, rel});
                    }
                }
            }
        }
        conn.update(query, paramSet, batchSize, numThreads);
    }
}
