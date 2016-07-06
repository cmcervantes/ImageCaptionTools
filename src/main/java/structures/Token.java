package structures;

import utilities.StringUtil;

/**The Token class allows us to unambiguously identify tokens in
 * our data structures (mentions, captions), which enables us to
 * better find and manipulate mentions in context
 *
 * @author ccervantes
 */
public class Token
{
    private String _text;
    private int _idx;
    private int _captionIdx;
    private String _chunkType;
    private String _posTag;
    private String _chainID;
    private String _lemma;

    public int chunkIdx;
    public int entityIdx;

    /**Creates a new Token object with all internal fields
     *
     * @param captionIdx
     * @param idx
     * @param text
     * @param lemma
     * @param chunkIdx
     * @param entityIdx
     * @param chunkType
     * @param posTag
     * @param chainID
     */
    public Token(int captionIdx, int idx, String text,
                 String lemma, Integer chunkIdx, Integer entityIdx,
                 String chunkType, String posTag, String chainID)
    {
        _captionIdx = captionIdx;
        _idx = idx;
        _text = text;
        _lemma = lemma;
        this.chunkIdx = chunkIdx == null ? -1 : chunkIdx;
        this.entityIdx = entityIdx == null ? -1 : entityIdx;
        _chunkType = chunkType;
        _posTag = posTag;
        _chainID = chainID;
    }

    /**Token constructor originally written with for
     * the fromEntitiesStr functionality in Caption
     *
     * @param captionIdx
     * @param idx
     * @param text
     * @param entityIdx
     * @param chainID
     */
    public Token(int captionIdx, int idx, String text,
                 Integer entityIdx, String chainID)
    {
        _captionIdx = captionIdx;
        _idx = idx;
        _text = text;
        this.entityIdx = entityIdx == null ? -1 : entityIdx;
        _chainID = chainID;
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
    public String toDebugString()
    {
        String[] keys = {"capIdx", "idx", "chunkType",
                         "pos", "chainID", "chunkIdx",
                         "entityIdx", "lemma", "text"};
        Object[] vals = {_captionIdx, _idx, _chunkType,
                        _posTag, _chainID, chunkIdx,
                        entityIdx, _lemma, _text};
        return StringUtil.toKeyValStr(keys, vals);
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
    public int getIdx(){return _idx;}
    public String getText(){return _text;}
    public String getLemma(){return _lemma;}
    public int getCaptionIdx(){return _captionIdx;}
    public String getChunkType(){return _chunkType;}
    public String getPosTag(){return _posTag;}
    public String getChainID(){return _chainID;}
}
