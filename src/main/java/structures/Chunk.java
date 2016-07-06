package structures;

import utilities.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Chunk
{
    private String _chunkType;
    private int _idx;
    private List<Token> _tokenList;

    /**Default Chunk constructor
     *
     * @param idx
     * @param chunkType
     * @param tokenList
     */
    public Chunk(int idx, String chunkType, List<Token> tokenList)
    {
        _idx = idx;
        _chunkType = chunkType;
        _tokenList = new ArrayList<>(tokenList);
    }

    /* Getters */
    public int getIdx(){return _idx;}
    public String getChunkType(){return _chunkType;}
    public List<Token> getTokenList(){return _tokenList;}

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
}
