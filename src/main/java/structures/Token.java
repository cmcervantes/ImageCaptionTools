package structures;

import utilities.StringUtil;

/**The Token class allows us to unambiguously identify tokens in
 * our data structures (mentions, captions), which enables us to
 * better find and manipulate mentions in context
 *
 * @author ccervantes
 */
public class Token extends Annotation
{
    private String _text;
    private int _captionIdx;
    private String _posTag;
    private String _lemma;

    public int chunkIdx;
    public int mentionIdx;
    public String chainID;
    public String chunkType;

    /**Creates a new Token object with all internal fields
     *
     * @param docID
     * @param captionIdx
     * @param idx
     * @param text
     * @param lemma
     * @param chunkIdx
     * @param mentionIdx
     * @param chunkType
     * @param posTag
     * @param chainID
     */
    public Token(String docID, int captionIdx, int idx, String text,
                 String lemma, Integer chunkIdx, Integer mentionIdx,
                 String chunkType, String posTag, String chainID)
    {
        _docID = docID;
        _captionIdx = captionIdx;
        _idx = idx;
        _text = text;
        _lemma = lemma;
        _posTag = posTag;
        this.chunkIdx = chunkIdx == null ? -1 : chunkIdx;
        this.mentionIdx = mentionIdx == null ? -1 : mentionIdx;
        this.chunkType = chunkType;
        this.chainID = chainID;
    }

    /**Token constructor originally written with for
     * the fromEntitiesStr functionality in Caption
     *
     * @param docID
     * @param captionIdx
     * @param idx
     * @param text
     * @param mentionIdx
     * @param chainID
     */
    public Token(String docID, int captionIdx, int idx,
                 String text, Integer mentionIdx, String chainID)
    {
        _docID = docID;
        _captionIdx = captionIdx;
        _idx = idx;
        _text = text;
        this.mentionIdx = mentionIdx == null ? -1 : mentionIdx;
        this.chainID = chainID;
        chunkIdx = -1;
        chunkType = null;
    }

    /**Token constructor specifying token-specific
     * fields only; originally written for use in
     * creating Tokens from a database
     *
     * @param docID
     * @param captionIdx
     * @param idx
     * @param text
     * @param lemma
     * @param posTag
     */
    public Token(String docID, int captionIdx, int idx,
                 String text, String lemma, String posTag)
    {
        _docID = docID;
        _captionIdx = captionIdx;
        _idx = idx;
        _text = text;
        _lemma = lemma;
        _posTag = posTag;
        chunkIdx = -1;
        mentionIdx = -1;
        chunkType = null;
        chainID = null;
    }

    /**Returns the original text of this token
     *
     * @return - Token text
     */
    @Override
    public String toString()
    {
        return _text;
    }

    /**Returns this token's attributes as a key:value; string
     *
     * @return  - key-value string of token attributes
     */
    @Override
    public String toDebugString()
    {
        String[] keys = {"docID", "capIdx", "idx", "chunkType",
                         "pos", "chainID", "chunkIdx",
                         "mentionIdx", "lemma", "text"};
        Object[] vals = {_docID, _captionIdx, _idx, chunkType,
                        _posTag, chainID, chunkIdx,
                mentionIdx, _lemma, _text};
        return StringUtil.toKeyValStr(keys, vals);
    }

    /**Returns a dataset-unique ID for this token, in the form
     * docID#capIdx;token:idx
     *
     * @return
     */
    public String getUniqueID()
    {
        return _docID + "#" + _captionIdx + ";token:" + _idx;
    }

    /**Returns a POS string, originally implemented for
     * training / evaluating part-of-speech taggers
     *
     * @return - This token, as a "(POS txt)" string
     */
    public String toPosString()
    {
        return "(" + _posTag + " " + _text + ")";
    }

    /* Getters */
    public String getLemma(){return _lemma;}
    public int getCaptionIdx(){return _captionIdx;}
    public String getPosTag(){return _posTag;}
}
