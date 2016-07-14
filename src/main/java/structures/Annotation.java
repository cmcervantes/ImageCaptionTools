package structures;

import java.util.List;

/**The Annotation class abstracts the
 * common functionality for
 * Captions, Chunks, Mentions, and Tokens
 *
 */
public abstract class Annotation
{
    protected int _idx;
    protected String _docID;

    public int getIdx(){return _idx;}
    public String getDocID(){return _docID;}
    public abstract String toDebugString();
    public abstract String getUniqueID();

    /**Returns the index to insert the given item
     * in the given list, based on the Annotation idx,
     * using binary search
     *
     * @param list
     * @param item
     * @return
     */
    protected static int getInsertionIdx(List<? extends Annotation> list, Annotation item)
    {
        return getInsertionIdx(list, item, 0, list.size() - 1);
    }

    /**Returns the index to insert the given item
     * in the given list, based on the Annotation idx,
     * using binary search
     *
     * @param list
     * @param item
     * @param lo
     * @param hi
     * @return
     */
    private static int getInsertionIdx(List<? extends Annotation> list, Annotation item,
                                       int lo, int hi)
    {
        if(hi < lo){
            return lo;
        } else {
            int mid = hi - lo / 2;
            if(list.get(mid).getIdx() < item.getIdx())
                return getInsertionIdx(list, item, mid+1, hi);
            else
                return getInsertionIdx(list, item, lo, mid-1);
        }
    }
}
