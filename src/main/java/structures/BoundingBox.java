package structures;

import utilities.StringUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**Bounding boxes define image regions that belong to
 * a Document
 *
 * @author ccervantes
 */
public class BoundingBox extends Annotation
{
    private int _xMin;
    private int _yMin;
    private int _xMax;
    private int _yMax;
    private Rectangle _rec;
    private double _area;
    private String _category;
    private String _supercategory;

    /**Basic BoundingBox constructor
     *
     * @param xMin	- Minimum X coordinate
     * @param yMin	- Minimum Y coordinate
     * @param xMax	- Maximum X coordinate
     * @param yMax	- Maximum Y coordinate
     */
    public BoundingBox(String docID, int idx, int xMin,
                       int yMin, int xMax, int yMax)
    {
        _docID = docID; _idx = idx;
        _xMin = xMin; _yMin = yMin;
        _xMax = xMax; _yMax = yMax;
        _rec = new Rectangle(xMin, yMin, xMax-xMin, yMax-yMin);
        _area = _rec.getWidth() * _rec.getHeight();
        _category = null; _supercategory = null;
    }

    /**Bounding box constructor for boxes with categories
     * and supercategories (as in MSCOCO)
     *
     * @param docID
     * @param idx
     * @param xMin
     * @param yMin
     * @param xMax
     * @param yMax
     * @param category
     * @param superCategory
     */
    public BoundingBox(String docID, int idx, int xMin,
                       int yMin, int xMax, int yMax,
                       String category, String superCategory)
    {
        _docID = docID; _idx = idx;
        _xMin = xMin; _yMin = yMin;
        _xMax = xMax; _yMax = yMax;
        _rec = new Rectangle(xMin, yMin, xMax-xMin, yMax-yMin);
        _area = _rec.getWidth() * _rec.getHeight();
        _category = category; _supercategory = superCategory;
    }

    /* Getters */
    public int getXMin(){return _xMin;}
    public int getYMin(){return _yMin;}
    public int getXMax(){return _xMax;}
    public int getYMax(){return _yMax;}
    public Rectangle getRec(){return _rec;}
    public double getArea(){return _area;}
    public String getCategory(){return _category;}
    public String getSuperCategory(){return _supercategory;}

    /**Returns this bounding box's attributes as a key:value; string
     *
     * @return  - key-value string of box attributes
     */
    @Override
    public String toDebugString()
    {
        String[] keys = {"idx", "xMin", "yMin", "xMax", "yMax"};
        Object[] vals = {_idx, _xMin, _yMin, _xMax, _yMax};
        return StringUtil.toKeyValStr(keys, vals);
    }

    public String getUniqueID()
    {
        return _docID + ";box:" + _idx;
    }

    /**Returns whether this bounding box shares the same
     * coordinates as another
     *
     * @return
     */
    public boolean perfectlyOverlaps(BoundingBox b)
    {
        return _xMin == b._xMin &&
                _yMin == b._yMin &&
                _xMax == b._xMax &&
                _yMax == b._yMax;
    }

    /**Returns the intersection over union of the two bounding boxes
     *
     * @param b1
     * @param b2
     * @return
     */
    public static double IOU(BoundingBox b1, BoundingBox b2)
    {
        Rectangle intrsct = b1._rec.intersection(b2._rec);
        double intrsct_area = intrsct.getHeight() * intrsct.getWidth();
        return intrsct_area / (b1._area + b2._area - intrsct_area);
    }


    /**Returns the intersection over union of the two sets of bounding boxes, such that
     * the total area represented by the boxes in each collection is accounted for
     *
     * @param boxes_1
     * @param boxes_2
     * @return
     */
    public static double IOU(Collection<BoundingBox> boxes_1, Collection<BoundingBox> boxes_2)
    {
        //NOTE: trying to do this with built in awt objects seems like
        //      more trouble than its worth
        List<Rectangle> recList_1 = new ArrayList<>();
        boxes_1.forEach(b -> recList_1.add(b._rec));
        double area_1 = getTotalArea(recList_1);
        List<Rectangle> recList_2 = new ArrayList<>();
        boxes_2.forEach(b -> recList_2.add(b._rec));
        double area_2 = getTotalArea(recList_2);

        List<Rectangle> intrsctList = new ArrayList<>();
        for(BoundingBox b1 : boxes_1)
            for(BoundingBox b2 : boxes_2)
                intrsctList.add(b1._rec.intersection(b2._rec));
        double intrsct = getTotalArea(intrsctList);
        return intrsct / (area_1 + area_2 - intrsct);
    }

    /**Returns the size of the total area represented by the
     * given list of rectangles, where intersecting areas
     * count only once
     *
     * @param recList
     * @return
     */
    private static double getTotalArea(List<Rectangle> recList)
    {
        double area = 0.0;
        for(int i=0; i<recList.size(); i++){
            Rectangle r = recList.get(i);

            //Add area rectangle's area only once
            area += r.getHeight() * r.getWidth();

            //Remove each intersection once
            for(int j=i+1; j<recList.size(); j++){
                Rectangle intrsct = r.intersection(recList.get(j));
                area -= intrsct.getHeight() * intrsct.getWidth();
            }
        }
        return area;
    }
}
