package utilities;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**Chart utilities for automatic chart generation
 *
 */
public class ChartIO {

    /**Saves a (jpg) chart for the given data to filename, where
     * the keys are 2-dim string arrays for (row,col), where columns
     * are values in the x-dimension, and rows are different bars
     * within a value
     *
     * @param data
     * @param filename
     */
    public static void saveBarChart(Map<Comparable[], Double> data, String filename)
    {
        saveBarChart(data, null, null, null, filename);
    }

    /**Saves a (jpg) chart for the given data to filename, where
     * the keys are 2-dim string arrays for (row,col), where columns
     * are values in the x-dimension, and rows are different bars
     * within a value
     * Optional arguments for chart title, xAxis, and yAxis labels are
     * also available
     *
     * @param data
     * @param title
     * @param xAxis
     * @param yAxis
     * @param filename
     */
    public static void saveBarChart(Map<Comparable[], Double> data, String title,
                                    String xAxis, String yAxis, String filename)
    {
        //We assume the data we've been given is a table, we want to sort
        //the contents consistently
        Set<Comparable> keys_row = new HashSet<>(), keys_col = new HashSet<>();
        Map<Comparable, Map<Comparable, Double>> dataDict = new HashMap<>();
        for(Comparable[] key : data.keySet()) {
            Comparable row = key[0];
            Comparable col = key[1];
            keys_row.add(row);
            keys_col.add(col);
            if(!dataDict.containsKey(row))
                dataDict.put(row, new HashMap<>());
            dataDict.get(row).put(col, data.get(key));
        }
        List<Comparable> keyList_row = new ArrayList<>(keys_row);
        List<Comparable> keyList_col = new ArrayList<>(keys_col);
        Collections.sort(keyList_row); Collections.sort(keyList_col);

        //Add the data in order to the chart
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for(Comparable row : keyList_row)
            for(Comparable col : keyList_col)
                if(dataDict.containsKey(row) && dataDict.get(row).containsKey(col))
                    dataset.addValue(dataDict.get(row).get(col), row, col);

        //save the chart to file
        boolean printLegend = keys_row.size() > 1;
        JFreeChart barChart = ChartFactory.createBarChart(title, xAxis, yAxis,
                dataset, PlotOrientation.VERTICAL, printLegend, true, false);
        int width = 640, height = 480;
        File chartFile = new File(filename);
        try{
            ChartUtilities.saveChartAsJPEG(chartFile, barChart, width, height);
        } catch(IOException ioEx) {
            Logger.log(ioEx);
        }
    }

}
