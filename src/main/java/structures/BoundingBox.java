package structures;

import utilities.Logger;

import java.awt.*;

/**Bounding boxes define image regions that belong to
 * a Document
 *
 * @author ccervantes
 */
public class BoundingBox
{
    private int _ID;
    private int _xMin;
    private int _yMin;
    private int _xMax;
    private int _yMax;
    private Rectangle _rec;
    private double _area;

    /**Basic BoundingBox constructor
     *
     * @param ID	- BoundingBox _ID
     * @param xMin	- Minimum X coordinate
     * @param yMin	- Minimum Y coordinate
     * @param xMax	- Maximum X coordinate
     * @param yMax	- Maximum Y coordinate
     */
    public BoundingBox(int ID, int xMin,
                       int yMin, int xMax,
                       int yMax)
    {
        _ID = ID;
        _xMin = xMin;
        _yMin = yMin;
        _xMax = xMax;
        _yMax = yMax;
        _rec = new Rectangle(xMin, yMin, xMax-xMin, yMax-yMin);
        _area = _rec.getWidth() * _rec.getHeight();
        if(_area < 0)
        {
            Logger.log("WARNING: negative area!");
        }
    }

    /* Getters */
    public int getID(){return _ID;}
    public int getXMin(){return _xMin;}
    public int getYMin(){return _yMin;}
    public int getXMax(){return _xMax;}
    public int getYMax(){return _yMax;}
    public Rectangle getRec(){return _rec;}
    public double getArea(){return _area;}

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
        Rectangle intersection = b1._rec.intersection(b2._rec);
        return intersection.getHeight() * intersection.getWidth() /
                (b1._area + b2._area);
    }
}
