package utilities;

import java.util.*;

/**The DoubleDict class simplifies the
 * process of creating a HashMap of
 * [K -> V], allowing for straightforward
 * incrementing and decrementing
 *
 * @param <K> - The Class of the keys
 *
 * @author ccervantes
 */
public class DoubleDict<K>
{
    protected HashMap<K, Double> dict;

    /**Basic constructor initializes the
     * internal HashMap
     */
    public DoubleDict()
    {
        dict = new HashMap<>();
    }

    /**Returns the number of keys
     * in this DoubleDict
     *
     * @return - The size of this DoubleDict
     */
    public int size()
    {
        return dict.size();
    }

    /**Returns the value to which
     * <b>key</b> is mapped
     *
     * @param key - The key in the dict
     * @return	  - The value in the dict
     * 				(0 if key isn't found)
     */
    public double get(K key)
    {
        if(dict.containsKey(key))
            return dict.get(key);
        return 0;
    }

    /**Sets the <b>key</b> to <b>value</b>,
     * overwriting any previous entries
     *
     * @param key   - Key to overwrite
     * @param value - Value to overwrite with
     */
    protected void set(K key, double value)
    {
        if(dict.containsKey(key))
            dict.remove(key);
        increment(key, value);
    }

    /**Returns the value mapped to <b>key</b>,
     * divided by <b>val</b>
     *
     * @param key - The key of the value to find
     * @param val - The divisor for the division operation
     */
    public void divide(K key, double val)
    {
        if(dict.containsKey(key))
        {
            dict.put(key, dict.get(key)/val);
        }
    }

    /**Increments the value mapped
     * to the <b>key</b> by one or
     * by <b>value</b>, if specified.
     * If <b>key</b> isn't found, it
     * is inserted into the dict with 0
     * and then incremented.
     *
     * @param key   - The key in the dict
     */
    public void increment(K key)
    {
        if(!dict.containsKey(key))
            dict.put(key, 0.0);
        double val = dict.get(key);
        dict.put(key, val+1);
    }

    /**Increments the value mapped
     * to the <b>key</b> by one or
     * by <b>value</b>, if specified.
     * If <b>key</b> isn't found, it
     * is inserted into the dict with 0
     * and then incremented.
     *
     * @param key   - The key in the dict
     * @param value - The number to increment
     * 				  the dict value by (1 by
     * 				  default)
     */
    public void increment(K key, double value)
    {
        if(!dict.containsKey(key))
        {
            dict.put(key, 0.0);
        }
        double val = dict.get(key);
        dict.put(key, val + value);
    }

    /**Decrements the value mapped to the
     * <b>key</b> by one or by <b>value</b>,
     * if specified. If <b>key</b> isn't found,
     * it is inserted into the dict with 0
     * and then decremented. If optional
     * argument <b>zeroFloor</b> is used,
     * the value cannot be decremented below 0.
     *
     * @param key       - The key in the dict
     */
    public void decrement(K key)
    {
        decrement(key, 1, true);
    }

    /**Decrements the value mapped to the
     * <b>key</b> by one or by <b>value</b>,
     * if specified. If <b>key</b> isn't found,
     * it is inserted into the dict with 0
     * and then decremented. If optional
     * argument <b>zeroFloor</b> is used,
     * the value cannot be decremented below 0.
     *
     * @param key       - The key in the dict
     * @param value     - The number to decrement
     * 				      the dict value by (1 by
     * 				      default)
     */
    public void decrement(K key, double value)
    {
        decrement(key, value, true);
    }

    /**Decrements the value mapped to the
     * <b>key</b> by one or by <b>value</b>,
     * if specified. If <b>key</b> isn't found,
     * it is inserted into the dict with 0
     * and then decremented. If optional
     * argument <b>zeroFloor</b> is used,
     * the value cannot be decremented below 0.
     *
     * @param key       - The key in the dict
     * @param value     - The number to decrement
     * 				      the dict value by (1 by
     * 				      default)
     * @param zeroFloor - Whether 0 should be treated
     * 					  as the floor (true by default)
     */
    public void decrement(K key, double value, boolean zeroFloor)
    {
        if(!dict.containsKey(key))
            dict.put(key, 0.0);
        double val = dict.get(key);
        if(val > 0 || !zeroFloor)
            dict.put(key, val-value);
    }

    /**Returns the set of dictionary keys
     *
     * @return - The set of keys in the dict
     */
    public Set<K> keySet()
    {
        return dict.keySet();
    }

    /**Returns a list of keys to this DoubleDict, where
     * the list is sorted in ascending order by the value
     * to which the key is mapped. Optional argument
     * <b>reverse</b> switches to descending order.
     *
     * @return		  - The list of keys, sorted by the value
     * 					to which they are mapped
     */
    public boolean containsKey(K key)
    {
        return dict.containsKey(key);
    }

    /**Returns a list of keys to this DoubleDict, where
     * the list is sorted in ascending order by the value
     * to which the key is mapped. Optional argument
     * <b>reverse</b> switches to descending order.
     *
     * @return		  - The list of keys, sorted by the value
     * 					to which they are mapped
     */
    public List<K> getSortedByValKeys()
    {
        return getSortedByValKeys(false);
    }

    /**Returns a list of keys to this DoubleDict, where
     * the list is sorted in ascending order by the value
     * to which the key is mapped. Optional argument
     * <b>descendingOrder</b> switches to descending order.
     *
     * @param descendingOrder - Whether to return the list in
     * 					desceninding order (false by default)
     * @return		  - The list of keys, sorted by the value
     * 					to which they are mapped
     */
    public List<K> getSortedByValKeys(boolean descendingOrder)
    {
        LinkedHashMap<K, Double> sortedMap = Util.sortHashMap(dict, descendingOrder);
        return new ArrayList<>(sortedMap.keySet());
    }

    /**Returns the mean of the values in this
     * DoubleDict
     *
     * @return	- The mean of this DoubleDict
     */
    public double getMean()
    {
        return StatisticalUtil.getMean(dict.values());
    }

    /**Returns the standard deviation of the
     * values in this DoubleDict
     *
     * @return	- The standard dev of this DoubleDict
     */
    public double getStdDev()
    {
        return StatisticalUtil.getStdDev(dict.values());
    }

    public double getSum()
    {
        return StatisticalUtil.getSum(dict.values());
    }

    /**Returns the max value stored in
     * this PrecisionCountDict
     *
     * @return - The maximum value
     */
    public double getMax()
    {
        if(dict.isEmpty())
            return Double.NEGATIVE_INFINITY;
        else
            return dict.get(getMaxKey());
    }

    /**Returns the key that is mapped
     * to the maximum value in the PrecisionCountDict
     *
     * @return - The key to the maximum value
     * 			 in this PrecisionCountDict
     */
    public K getMaxKey()
    {
        double maxVal = Double.NEGATIVE_INFINITY;
        K maxKey = null;
        for(K key : dict.keySet()) {
            if(dict.get(key) > maxVal) {
                maxVal = dict.get(key);
                maxKey = key;
            }
        }
        return maxKey;
    }

    /**Returns the min value stored in
     * this PrecisionCountDict
     *
     * @return - The minimum value
     */
    public double getMin()
    {
        if(dict.isEmpty())
            return Double.POSITIVE_INFINITY;
        else
            return dict.get(getMinKey());
    }

    /**Returns the key that is mapped
     * to the minimum value in the
     * PrecisionCountDict
     *
     * @return - The key to the minimum value
     * 			 in this PrecisionCountDict
     */
    public K getMinKey()
    {
        double minVal = Double.POSITIVE_INFINITY;
        K minKey = null;
        for(K key : dict.keySet())
        {
            if(dict.get(key) < minVal)
            {
                minVal = dict.get(key);
                minKey = key;
            }
        }
        return minKey;
    }

    public void remove(K key)
    {
        if(dict.containsKey(key))
            dict.remove(key);
    }

    public void removeKeysWithVal(Double val)
    {
        Set<K> keySet = new HashSet<>();
        for(K key : dict.keySet())
            if(dict.get(key).equals(val))
                keySet.add(key);
        for(K key : keySet)
            dict.remove(key);
    }

    /**Returns a string representation of this
     * PrecisionCountDict in the form
     * k,v
     * k,v
     * ...
     * k,v
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for(K key : getSortedByValKeys(true)) {
            sb.append(key.toString());
            sb.append(",");
            Double val = dict.get(key);
            if(val.intValue() == val)
                sb.append(String.valueOf(val.intValue()));
            else
                sb.append(val.toString());

            sb.append("\n");
        }
        return sb.toString();
    }

    public void printPercentageDict(K totalKey)
    {
        StringBuilder sb = new StringBuilder();
        for(K key : getSortedByValKeys(true)){
            if(totalKey == key)
                continue;
            System.out.printf("%s: %.2f%%\n",
                    key.toString(), 100.0 * dict.get(key) /
                    dict.get(totalKey));
        }
    }

    /**Returns a <i>DoubleDict</i> where the keys are the
     * values of the given <b>countDict</b>, and the values are
     * the number of keys with which they were associated.
     *
     * ex.
     * countDict: {a=1, b=2, c=1, d=3}
     * histDict: {1=2, 2=1, 3=1}
     *
     * @param countDict
     * @param <K>
     * @return
     */
    public static <K> DoubleDict<Double>
    getHistogramDict(DoubleDict<K> countDict)
    {
        DoubleDict<Double> histDict = new DoubleDict<>();
        for(K key : countDict.keySet())
            histDict.increment(countDict.get(key));
        return histDict;
    }

    /**Returns a <i>DoubleDict</i> where the keys are the
     * values in the given <b>dict</b> and the values are
     * the number of keys in the dict with which the value appears
     *
     * ex.
     * dict: {x=a, y=a, z=b}
     * histDict: {a=2, b=1}
     *
     * @param dict
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K,V> DoubleDict<V> getHistogramDict(Map<K,V> dict)
    {
        DoubleDict<V> histDict = new DoubleDict<>();
        for(K key : dict.keySet())
            histDict.increment(dict.get(key));
        return histDict;
    }
}
