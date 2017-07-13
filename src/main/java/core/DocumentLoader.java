package core;

import nlptools.StanfordParser;
import structures.*;
import utilities.*;

import javax.sql.rowset.CachedRowSet;
import java.util.*;

/**DocumentLoader houses static functions to load Document
 * objects from various places, including .coref files,
 * Flickr30kEntities files, and databases
 */
public class DocumentLoader
{

    public static void main(String[] args)
    {
        String[] mscoco_mysqlParams = {"ccervan2.web.engr.illinois.edu",
                "ccervan2_root", "thenIdefyheaven!", "ccervan2_coco"};
        DBConnector conn = new DBConnector(mscoco_mysqlParams[0], mscoco_mysqlParams[1],
                mscoco_mysqlParams[2], mscoco_mysqlParams[3]);
        getDocumentSet(conn, 0, true, 100);
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
        Mention.initializeLexicons(lexiconDir, null);
        Cardinality.initCardLists(wordListDir + "/collectiveNouns.txt");
        Caption.initLemmatizer();

        Map<String, List<String>> corefDict = new HashMap<>();
        for(String corefStr : corefList){
            String idStr = corefStr.split("\t")[0];
            String[] idParts = idStr.split("#");
            if(!corefDict.containsKey(idParts[0]))
                corefDict.put(idParts[0], new ArrayList<>());
            int insertionIdx = 0, captionIdx = Integer.parseInt(idParts[1]);
            for(String corefLine : corefDict.get(idParts[0])){
                if(captionIdx > Integer.parseInt(corefLine.split("\t")[0].split("#")[1]))
                    insertionIdx++;
            }
            corefDict.get(idParts[0]).add(insertionIdx, corefStr);
        }

        Set<Document> docSet = new HashSet<>();
        for(List<String> corefStrSet : corefDict.values())
            docSet.add(new Document(corefStrSet));

        return docSet;
    }

    /**Returns a set of Documents, based on a coref file, bounding box file, and image info file
     * (written for loading MSCOCO images)
     *
     * @param corefFile
     * @param bboxFile
     * @param imgFile
     * @return
     */
    public static Collection<Document> getDocumentSet(String corefFile, String bboxFile,
                                                      String imgFile, String lexiconDir,
                                                      String wordListDir)
    {
        Mention.initializeLexicons(lexiconDir, null);
        Cardinality.initCardLists(wordListDir + "/collectiveNouns.txt");
        Caption.initLemmatizer();

        List<String> corefList = FileIO.readFile_lineList(corefFile);

        //Load .coref files and build the documents
        Map<String, List<String>> corefDict = new HashMap<>();
        for(String corefStr : corefList){
            String idStr = corefStr.split("\t")[0];
            String[] idParts = idStr.split("#");
            if(!corefDict.containsKey(idParts[0]))
                corefDict.put(idParts[0], new ArrayList<>());
            int insertionIdx = 0, captionIdx = Integer.parseInt(idParts[1]);
            for(String corefLine : corefDict.get(idParts[0])){
                if(captionIdx > Integer.parseInt(corefLine.split("\t")[0].split("#")[1]))
                    insertionIdx++;
            }
            corefDict.get(idParts[0]).add(insertionIdx, corefStr);
        }

        Map<String, Document> docDict = new HashMap<>();
        for(List<String> corefStrSet : corefDict.values()){
            Document d = new Document(corefStrSet);
            docDict.put(d.getID(), d);
        }

        //Load the img file and augment the documents
        String[][] imgTable = FileIO.readFile_table(imgFile);
        for(String row[] : imgTable){
            //Assume a format of
            //      id, url, cross_val, height, width
            String ID = row[0];
            docDict.get(ID).imgURL = row[1];
            docDict.get(ID).crossVal = Integer.parseInt(row[2]);
            docDict.get(ID).reviewed = Integer.parseInt(row[3]) == 1;
            docDict.get(ID).height = Integer.parseInt(row[4]);
            docDict.get(ID).width = Integer.parseInt(row[5]);
        }

        //load bounding box file and add these bounding boxes
        //to the documents
        for(String row[] : FileIO.readFile_table(bboxFile)){
            //Assume format
            //      img_id, box_idx, box_id, category, supercateogry, x, y, width, height
            String docID = row[0];
            int boxIdx = Integer.parseInt(row[1]);
            String cat = row[3]; String superCat = row[4];
            int xMin = (int)Double.parseDouble(row[5]);
            int yMin = (int)Double.parseDouble(row[6]);
            int xMax = xMin + (int)Double.parseDouble(row[7]);
            int yMax = yMin + (int)Double.parseDouble(row[8]);
            Set<String> assocChains = new HashSet<>();
            assocChains.add("-1");

            //if there isn't a -1 chain, add one
            Set<String> chainIDs = new HashSet<>();
            for(Chain c : docDict.get(docID).getChainSet())
                chainIDs.add(c.getID());
            if(!chainIDs.contains("-1"))
                docDict.get(docID).addChain(new Chain(docID, "-1"));

            docDict.get(docID).addBoundingBox(new BoundingBox(docID,
                    boxIdx, xMin, yMin, xMax, yMax, cat, superCat), assocChains);
        }

        return docDict.values();
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
        Cardinality.initCardLists(wordListDir + "/collectiveNouns.txt");

        //get the filenames from the sentences directory (assuming the annotations are the same)
        Set<String> filenames =
                new HashSet<>(FileIO.getFileNamesFromDir(flickr30kEntitiesDir + "Sentences/"));

        Set<Document> docSet = new HashSet<>();
        for(String filename : filenames)
            docSet.add(new Document(flickr30kEntitiesDir + "Sentences/" +filename,
                                    flickr30kEntitiesDir + "Annotations/" + filename.replace("txt", "xml")));
        return docSet;
    }

    /**Returns a collection of documents from the database specified by
     * the conn
     *
     * @param conn          Database connector
     * @return              Collection of documents
     */
    public static Collection<Document> getDocumentSet(DBConnector conn)
    {
        return getDocumentSet(conn, -1, false, -1);
    }

    /**Returns a collection of documents from the database specified by
     * the conn
     *
     * @param conn          Database connector
     * @param crossVal      The cross validation flag (-1: all; 0: dev; 1: train; 2: test)
     * @return              Collection of documents
     */
    public static Collection<Document> getDocumentSet(DBConnector conn, int crossVal)
    {
        return getDocumentSet(conn, crossVal, false, -1);
    }

    /**Returns a collection of documents from the database specified by
     * the conn
     *
     * @param conn          Database connector
     * @param crossVal      The cross validation flag (-1: all; 0: dev; 1: train; 2: test)
     * @param reviewedOnly  Whether to retrieve only reviewed document
     * @return              Collection of documents
     */
    public static Collection<Document> getDocumentSet(DBConnector conn, int crossVal, boolean reviewedOnly)
    {
        return getDocumentSet(conn, crossVal, reviewedOnly, -1);
    }

    /**Returns a collection of documents from the database specified by
     * the conn
     *
     * @param conn          Database connector
     * @param crossVal      The cross validation flag (-1: all; 0: dev; 1: train; 2: test)
     * @param reviewedOnly  Whether to retrieve only reviewed document
     * @param numDocs       The number of random docs to retrieve (&leq;0: all; &geq;1: random documents)
     * @return              Collection of documents
     */
    public static Collection<Document> getDocumentSet(DBConnector conn, int crossVal,
                                                      boolean reviewedOnly, int numDocs)
    {
        CachedRowSet rs;
        String query;

        //Get the valid documents
        Set<String> imgIDs = new HashSet<>();
        try{
            query = "SELECT img_id FROM image";
            if(crossVal >= 0 || reviewedOnly)
                query += " WHERE ";
            if(crossVal >= 0)
                query += "cross_val="+crossVal;
            if(reviewedOnly){
                if(crossVal >= 0)
                    query += " AND ";
                query += "reviewed=1";
            }
            if(numDocs > 0){
                query += " ORDER BY ";
                if(conn.getDBType() == DBConnector.DBType.SQLITE)
                    query += "RANDOM() ";
                else if(conn.getDBType() == DBConnector.DBType.MYSQL)
                    query += "RAND() ";
                query += "LIMIT " + numDocs;
            }
            query += ";";

            rs = conn.query(query);
            while(rs.next())
                imgIDs.add(rs.getString("img_id"));
        } catch(Exception ex) {
            Logger.log(ex);
        }

        //return a document collection based on these random IDs
        return getDocumentSet(conn, imgIDs);
    }

    /**Returns a collection of documents from the database specified by
     * the conn
     *
     * @param conn      Database connector
     * @param docIDs    Document IDs
     * @return          Collection of documents
     */
    public static Collection<Document> getDocumentSet(DBConnector conn, Collection<String> docIDs)
    {
        CachedRowSet rs;
        String query;
        Map<String, Document> docDict = new HashMap<>();

        //Convert the given docIDs to a query string component
        Set<String> docIDs_enclosed = new HashSet<>();
        docIDs.forEach(id -> docIDs_enclosed.add("'" + id + "'"));
        String docIdStr = "(" + StringUtil.listToString(docIDs_enclosed, ",") + ")";

        try{
            Logger.log("Initializing Documents from <image>");
            query = "SELECT img_id, height, width, "+
                    "cross_val, reviewed, img_url FROM image "+
                    "WHERE img_id IN " + docIdStr;
            rs = conn.query(query);
            while(rs.next()){
                Document d = new Document(rs.getString("img_id"));
                d.height = rs.getInt("height");
                d.width = rs.getInt("width");
                d.crossVal = Util.castInteger(rs.getObject("cross_val"));
                d.reviewed = rs.getBoolean("reviewed");
                d.imgURL = rs.getString("img_url");
                docDict.put(d.getID(), d);
            }

            Logger.log("Building Tokens and Captions from <token>");
            query = "SELECT img_id, caption_idx, token_idx, " +
                    "token, lemma, pos_tag FROM token "+
                    "WHERE img_id IN " + docIdStr;
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
            query = "SELECT img_id, caption_idx, chunk_idx, " +
                    "start_token_idx, end_token_idx, chunk_type "+
                    "FROM chunk WHERE img_id IN " + docIdStr;
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
            query = "SELECT img_id, box_id, x_min, y_min, " +
                    "x_max, y_max, category, super_category FROM box " +
                    "WHERE img_id IN " + docIdStr;
            Map<String, Map<Integer, BoundingBox>> boxDict = new HashMap<>();
            rs = conn.query(query);
            while(rs.next()){
                String imgID = rs.getString("img_id");
                int boxID = rs.getInt("box_id");
                int xMin = rs.getInt("x_min");
                int yMin = rs.getInt("y_min");
                int xMax = rs.getInt("x_max");
                int yMax = rs.getInt("y_max");
                String category = rs.getString("category");
                String superCat = rs.getString("super_category");
                if(!boxDict.containsKey(imgID))
                    boxDict.put(imgID, new HashMap<>());
                boxDict.get(imgID).put(boxID,
                    new BoundingBox(imgID, boxID, xMin,
                    yMin, xMax, yMax, category, superCat));
            }

            Logger.log("Loading chains from <chain> and associating them with "+
                    "the bounding boxes from the previous step");
            query = "SELECT img_id, chain_id, assoc_box_ids, " +
                    "is_scene, is_orig_nobox FROM chain "+
                    "WHERE img_id IN " + docIdStr;
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
                if(assocBoxIDs != null && !assocBoxIDs.trim().isEmpty()){
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
            query = "SELECT img_id, caption_idx, mention_idx, " +
                    "start_token_idx, end_token_idx, card_str, " +
                    "chain_id, lexical_type FROM mention " +
                    "WHERE img_id IN " + docIdStr;
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
            query = "SELECT img_id, caption_idx, gov_token_idx, " +
                    "dep_token_idx, relation FROM dependency " +
                    "WHERE img_id IN " + docIdStr;
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
        query = "CREATE TABLE IF NOT EXISTS image (img_id VARCHAR(20), "+
                "height INT, width INT, reviewed TINYINT(1), cross_val "+
                "TINYINT(1), anno_comments TEXT, img_url TEXT, " +
                "PRIMARY KEY(img_id));";
        conn.createTable(query);
        query = insertPrefix + "image(img_id, height, width, reviewed, "+
                "cross_val, anno_comments, img_url) "+
                "VALUES (?, ?, ?, ?, ?, ?, ?);";
        paramSet = new HashSet<>();
        for(Document d : docSet) {
            paramSet.add(new Object[]{d.getID(), d.height,
                    d.width, d.reviewed, d.crossVal,
                    d.comments, d.imgURL});
        }
        conn.update(query, paramSet, batchSize, numThreads);

        /* While not strictly necessary, the <caption> table
         * contains the full caption string so we can easily
         * look up captions of various types */
        Logger.log("Creating <caption>");
        query = "CREATE TABLE IF NOT EXISTS caption (img_id VARCHAR(20), "+
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
        query = "CREATE TABLE IF NOT EXISTS token (img_id VARCHAR(20), "+
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
        query = "CREATE TABLE IF NOT EXISTS chunk (img_id VARCHAR(20), "+
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
        query = "CREATE TABLE IF NOT EXISTS mention (img_id VARCHAR(20), "+
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
        query = "CREATE TABLE IF NOT EXISTS chain (img_id VARCHAR(20), "+
                "chain_id VARCHAR(10), assoc_box_ids VARCHAR(250), "+
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
        query = "CREATE TABLE IF NOT EXISTS box (img_id VARCHAR(20), "+
                "box_id INT, x_min INT, y_min INT, x_max INT, "+
                "y_max INT, category TEXT, super_category TEXT, "+
                "PRIMARY KEY(img_id, box_id));";
        conn.createTable(query);
        query = insertPrefix + "box(img_id, box_id, x_min, y_min, " +
                "x_max, y_max, category, super_category) "+
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        paramSet = new HashSet<>();
        for(Document d : docSet){
            for(BoundingBox b : d.getBoundingBoxSet()){
                paramSet.add(new Object[]{d.getID(), b.getIdx(), b.getXMin(),
                        b.getYMin(), b.getXMax(), b.getYMax(),
                        b.getCategory(), b.getSuperCategory()});
            }
        }
        conn.update(query, paramSet, batchSize, numThreads);


        /* The <dependency> table contains arcs constructed from
         * the Stanford Dependency parser
         */
        Logger.log("Creating <dependency>");

        StanfordParser parser = new StanfordParser();
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
        query = "CREATE TABLE IF NOT EXISTS dependency (img_id VARCHAR(20), "+
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

    /**
     *
     * @param docSet
     * @param outRoot
     */
    public static void exportCOCOFiles(Collection<Document> docSet, String outRoot)
    {
        List<String> ll_img = new ArrayList<>();
        List<String> ll_bbox = new ArrayList<>();
        List<String> ll_coref = new ArrayList<>();

        for(Document d : docSet){
            ll_img.add(d.getID() + "," + d.imgURL + "," +
                    d.crossVal + "," + d.reviewed + "," +
                    d.height + "," + d.width);

            for(BoundingBox b : d.getBoundingBoxSet()){
                String bboxLine = d.getID() + "," + b.getIdx() + "," +
                        b.getCategory() + "," + b.getSuperCategory() + "," +
                        b.getXMin() + "," + b.getYMin() + "," +
                        b.getXMax() + "," + b.getYMax() + ",";
                Set<String> assocChains = new HashSet<>();
                for(Mention m : d.getMentionSetForBox(b))
                    assocChains.add(m.getChainID());
                if(!assocChains.isEmpty()){
                    List<String> assocChains_list = new ArrayList<>(assocChains);
                    Collections.sort(assocChains_list);
                    bboxLine += StringUtil.listToString(assocChains_list, "|");
                }
                ll_bbox.add(bboxLine);
            }

            for(Caption c : d.getCaptionList())
                ll_coref.add(c.toCorefString(true));
        }
        FileIO.writeFile(ll_img, outRoot + "_img", "csv", false);
        FileIO.writeFile(ll_bbox, outRoot + "_bbox", "csv", false);
        FileIO.writeFile(ll_coref, outRoot + "_caps", "coref", false);
    }

    /**Exports a collection of documents to a release directory (which assumes
     * subdirectories Sentences/ and Annotations/
     *
     * @param docSet
     * @param releaseDir
     */
    public static void exportDocumentToReleaseDir(Collection<Document> docSet, String releaseDir)
    {
        List<String> ll_subsets = new ArrayList<>();

        for(Document d : docSet){
            String docID = d.getID().replace(".jpg", "");

            //Export an entities-formatted caption file
            List<String> captionList = new ArrayList<>();
            for(Caption c : d.getCaptionList())
                captionList.add(c.toEntitiesString());
            FileIO.writeFile(captionList, releaseDir + "Sentences/" +
                    docID, "txt", false);

            //Export a bounding box file
            String boxXML = XmlIO.createdBoundingBoxXML(d);
            if(boxXML != null)
                FileIO.writeFile(boxXML, releaseDir +
                        "Annotations/" + docID, "txt", false);
            else
                Logger.log("ERROR: failed to create box XML for " + d.getID());

            //Add the subsets to the list
            for(Chain[] subsetPair : d.getSubsetChains())
                ll_subsets.add(d.getID() + "," + subsetPair[0].getID() + "," + subsetPair[1].getID());
        }
        FileIO.writeFile(ll_subsets, releaseDir + "subsets", "csv", false);
    }
}
