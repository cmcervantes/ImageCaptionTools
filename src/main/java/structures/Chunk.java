package structures;

import utilities.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**Chunks serve as collections of Tokens
 * with a particular type (NP, VP, etc)
 */
public class Chunk extends Annotation
{
    private int _captionIdx;
    private String _chunkType;
    private List<Token> _tokenList;

    /**Default Chunk constructor
     *
     * @param idx
     * @param chunkType
     * @param tokenList
     */
    public Chunk(String docID, int captionIdx,
                 int idx, String chunkType,
                 List<Token> tokenList)
    {
        _docID = docID;
        _captionIdx = captionIdx;
        _idx = idx;
        _chunkType = chunkType;
        _tokenList = new ArrayList<>(tokenList);
    }

    /* Getters */
    public String getChunkType(){return _chunkType;}
    public List<Token> getTokenList(){return _tokenList;}

    /**Returns the token indices of the tokens at the
     * start and end of this chunk
     *
     * @return
     */
    public int[] getTokenRange()
    {
        return new int[]{_tokenList.get(0).getIdx(),
                _tokenList.get(_tokenList.size()-1).getIdx()};
    }

    /**The text of this chunk
     *
     * @return
     */
    @Override
    public String toString()
    {
        return StringUtil.listToString(_tokenList, " ");
    }

    /**Returns this chunk's attributes as a key:value; string
     *
     * @return
     */
    public String toDebugString()
    {
        String[] keys = {"idx", "chunkType", "text"};
        Object[] values = {_idx, _chunkType, this.toString()};
        return StringUtil.toKeyValStr(keys, values);
    }

    /**Returns a dataset-unique ID for this chunk, in the form
     * docID#capIdx;chunk:idx
     *
     * @return
     */
    public String getUniqueID()
    {
        return _docID + "#" + _captionIdx + ";chunk:" + _idx;
    }
}
