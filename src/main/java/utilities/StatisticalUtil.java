package utilities;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**The StatisticalUtil class houses static functions that perform
 * various statistical tasks, such as finding the mean and standard
 * deviation of a collection
 *
 * @author ccervantes
 */
public class StatisticalUtil {

    /* Simple arithmatic functions (gets) */

    /**Returns the maximum value of the given array
     *
     * @param arr
     * @return
     */
    public static double getMax(Double[] arr) {
        double max = Double.NEGATIVE_INFINITY;
        for (double d : arr)
            if (d > max)
                max = d;
        return max;
    }

    /**Returns the maximum value of the given collection
     *
     * @param coll
     * @return
     */
    public static double getMax(Collection<Double> coll) {
        Double[] arr = new Double[coll.size()];
        return getMax(coll.toArray(arr));
    }

    /**Returns the minimum value of the given array
     *
     * @param arr
     * @return
     */
    public static double getMin(Double[] arr) {
        double min = Double.POSITIVE_INFINITY;
        for (double d : arr)
            if (d < min)
                min = d;
        return min;
    }

    /**Returns the minimum value of the given collection
     *
     * @param coll
     * @return
     */
    public static double getMin(Collection<Double> coll) {
        Double[] arr = new Double[coll.size()];
        return getMin(coll.toArray(arr));
    }

    /**Returns the sum of all elements in the given primitive array
     *
     * @param arr
     * @return
     */
    public static double getSum(double[] arr) {
        double sum = 0;
        for (int i = 0; i < arr.length; i++)
            sum += arr[i];
        return sum;
    }

    /**Returns the sum of all elements in the given object array
     *
     * @param arr
     * @return
     */
    public static double getSum(Double[] arr) {
        double sum = 0;
        for (int i = 0; i < arr.length; i++)
            if (arr[i] != null)
                sum += arr[i];
        return sum;
    }

    /**Returns the sum of all elements in the given collection
     *
     * @param coll
     * @return
     */
    public static double getSum(Collection<Double> coll) {
        Double[] arr = new Double[coll.size()];
        return getSum(coll.toArray(arr));
    }

    /**Returns the mean of the values in the given array
     *
     * @param arr - Array of values
     * @return - The mean of <b>arr</b>
     */
    public static double getMean(Double[] arr) {
        return getSum(arr) / arr.length;
    }

    /**Returns the mean of the values in the given collection
     *
     * @param coll - Collection of values
     * @return - The mean of <b>coll</b>
     */
    public static double getMean(Collection<Double> coll) {
        Double[] arr = new Double[coll.size()];
        return getMean(coll.toArray(arr));
    }

    /**Returns the standard deviation of the values in the given array
     *
     * @param arr - Array of values
     * @return - The standard dev of <b>arr</b>
     */
    public static double getStdDev(Double[] arr) {
        double mu = getMean(arr);
        int n = arr.length;
        double total = 0;
        for (int i = 0; i < n; i++) {
            total += Math.pow(arr[i] - mu, 2);
        }
        return Math.sqrt(total / n);
    }

    /**Returns the standard deviation of the values in the given collection
     *
     * @param coll - Collection of values
     * @return - The standard dev of <b>coll</b>
     */
    public static double getStdDev(Collection<Double> coll) {
        Double[] arr = new Double[coll.size()];
        return getStdDev(coll.toArray(arr));
    }

    /* Complex Functions (computes) */

    /**Returns the pointwise mutual information,
     * specifying whether to normalize the score
     * to the [-1,1] space
     *
     * @param probX
     * @param probY
     * @param probXY
     * @return
     */
    public static double computePMI(double probX, double probY, double probXY)
    {
        return computePMI(probX, probY, probXY, true);
    }

    /**We compute normalized PMI as in 'Normalized
     * (Pointwise) Mutual Information in Collocation
     * Extraction', where
     * pmi = ln( P(x,y) / [P(x)P(y)] ) / -ln(P(x,y))
     *
     * @param probX
     * @param probY
     * @param probXY
     * @param normalize
     * @return
     */
    public static double computePMI(double probX, double probY, double probXY, boolean normalize)
    {
        double pmi = Math.log(probXY) - (Math.log(probX) + Math.log(probY));
        if(normalize)
            pmi /= (-1 * Math.log(probXY));
        return pmi;
    }

    /**Returns the root mean squared error of the two lists, where
     * the items are assumed to be in the same order (so the
     * distance is (pred[i] - gold[i])^2)
     *
     * @param pred
     * @param gold
     * @return      - The root mean squared error or null if the
     *                lists are of different lengths
     */
    public static Double computeRMSE(List<Double> pred, List<Double> gold)
    {
        Double[] predArr = new Double[pred.size()], goldArr = new Double[gold.size()];
        return computeRMSE(pred.toArray(predArr), gold.toArray(goldArr));
    }

    /**Returns the root mean squared error of the two arrays, where
     * the items are assumed to be in the same order (so the
     * distance is (pred[i] - gold[i])^2)
     *
     * @param pred
     * @param gold
     * @return      - The root mean squared error or null if the
     *                arrays are of different lengths
     */
    public static Double computeRMSE(Double[] pred, Double[] gold)
    {
        if(pred.length != gold.length)
            return null;

        //Compute the rmse as per usual
        double rmse = 0.0, n = pred.length;
        for(int i=0; i<n; i++)
            rmse += Math.pow(pred[i] - gold[i], 2);
        rmse = Math.sqrt(rmse / n);

        //normalize by the mean
        //rmse /= getMean(gold);

        return rmse;
    }

    /**Computes the KL-Divergence from the given Q distribution to the
     * P distribution; this asymmetric measure produces 0 when the
     * two distributions are expected to produce the same reasults and
     * produces 1 when the distributions are completely different
     *
     * @param qDistro
     * @param pDistro
     * @param <T>
     * @return
     */
    public static <T> Double computeKLDivergence(DoubleDict<T> qDistro, DoubleDict<T> pDistro)
    {
        //Return null if there's a pDistro key that doesn't appear in q
        Set<T> pKeys = pDistro.keySet();
        for(T key : pKeys)
            if(!qDistro.keySet().contains(key))
                return null;

        //Now that we know each of the pDistro keys, compute KL divergence
        double klDiv = 0;
        for(T key : pKeys)
            klDiv += pDistro.get(key) *
                    (Math.log(pDistro.get(key)) - Math.log(qDistro.get(key)));
        return klDiv;
    }
}
