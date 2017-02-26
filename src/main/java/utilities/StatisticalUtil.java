package utilities;

import java.util.Collection;
import java.util.List;

/**The StatisticalUtil class houses static functions that perform
 * various statistical tasks, such as finding the mean and standard
 * deviation of a collection
 *
 * @author ccervantes
 */
public class StatisticalUtil {

    /**
     * Returns the maximum value of the given array
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

    /**
     * Returns the maximum value of the given collection
     *
     * @param coll
     * @return
     */
    public static double getMax(Collection<Double> coll) {
        Double[] arr = new Double[coll.size()];
        return getMax(coll.toArray(arr));
    }

    /**
     * Returns the minimum value of the given array
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

    /**
     * Returns the minimum value of the given collection
     *
     * @param coll
     * @return
     */
    public static double getMin(Collection<Double> coll) {
        Double[] arr = new Double[coll.size()];
        return getMin(coll.toArray(arr));
    }

    /**
     * Returns the sum of all elements in the given primitive array
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

    /**
     * Returns the sum of all elements in the given object array
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

    /**
     * Returns the sum of all elements in the given collection
     *
     * @param coll
     * @return
     */
    public static double getSum(Collection<Double> coll) {
        Double[] arr = new Double[coll.size()];
        return getSum(coll.toArray(arr));
    }

    /**
     * Returns the mean of the values in the given array
     *
     * @param arr - Array of values
     * @return - The mean of <b>arr</b>
     */
    public static double getMean(Double[] arr) {
        return getSum(arr) / arr.length;
    }

    /**
     * Returns the mean of the values in the given collection
     *
     * @param coll - Collection of values
     * @return - The mean of <b>coll</b>
     */
    public static double getMean(Collection<Double> coll) {
        Double[] arr = new Double[coll.size()];
        return getMean(coll.toArray(arr));
    }

    /**
     * Returns the standard deviation of the values in the given array
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

    /**
     * Returns the standard deviation of the values in the given collection
     *
     * @param coll - Collection of values
     * @return - The standard dev of <b>coll</b>
     */
    public static double getStdDev(Collection<Double> coll) {
        Double[] arr = new Double[coll.size()];
        return getStdDev(coll.toArray(arr));
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
    public static Double getRMSE(List<Double> pred, List<Double> gold)
    {
        Double[] predArr = new Double[pred.size()], goldArr = new Double[gold.size()];
        return getRMSE(pred.toArray(predArr), gold.toArray(goldArr));
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
    public static Double getRMSE(Double[] pred, Double[] gold)
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
}
