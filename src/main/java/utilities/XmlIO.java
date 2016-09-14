package utilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import structures.BoundingBox;
import structures.Chain;
import structures.Mention;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;
import java.util.*;

/**XmlIO houses static xml file IO functions, originally written to
 * interface with the bounding box files
 *
 * @author ccervantes
 */
public class XmlIO
{
    /**Reads a bounding box file and loads the contents
     * directly into a Document object
     * ]
     * @param filename
     * @return
     */
    public static void
        readBoundingBoxFile(String filename, structures.Document d)
    {
        int boxCounter = 0;
        try {
            //open the file and parse it into an XML doc
            File xmlFile = new File(filename);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            //grab the height and width tags
            Node sizeNode = doc.getElementsByTagName("size").item(0);
            Element sizeElement = (Element)sizeNode;
            d.height = Integer.parseInt(sizeElement.getElementsByTagName("height").item(0).getTextContent());
            d.width = Integer.parseInt(sizeElement.getElementsByTagName("width").item(0).getTextContent());

            //iterate through the node list
            NodeList nodeList = doc.getElementsByTagName("object");
            for(int i=0; i< nodeList.getLength(); i++)
            {
                //get the relevant xml nodes from this object node
                Node n = nodeList.item(i);
                Element e = (Element)n;

                //a single object element can refer to a single
                //box but multiple chains
                NodeList nameNodeList = e.getElementsByTagName("name");
                Set<String> assocChainIDs = new HashSet<>();
                for(int j=0; j<nameNodeList.getLength(); j++){
                    String nameText = nameNodeList.item(j).getTextContent();
                    if(nameText.contains("_"))
                        assocChainIDs.add(nameText.split("_")[1]);
                    else
                        assocChainIDs.add(nameText);
                }

                int boxID = -1;
                Node node_box = e.getElementsByTagName("bndbox").item(0);
                Node node_scene = e.getElementsByTagName("scene").item(0);
                Node node_nobox = e.getElementsByTagName("nobndbox").item(0);
                if(node_box != null) {
                    boxID = boxCounter;
                    Element subElement = (Element)node_box;
                    int xMin = Integer.parseInt(subElement.getElementsByTagName("xmin").item(0).getTextContent());
                    int yMin = Integer.parseInt(subElement.getElementsByTagName("ymin").item(0).getTextContent());
                    int xMax = Integer.parseInt(subElement.getElementsByTagName("xmax").item(0).getTextContent());
                    int yMax = Integer.parseInt(subElement.getElementsByTagName("ymax").item(0).getTextContent());
                    d.addBoundingBox(new BoundingBox(d.getID(), boxID, xMin, yMin, xMax, yMax), assocChainIDs);
                }
                if(node_scene != null)
                    if(Integer.parseInt(node_scene.getTextContent()) == 1)
                        d.setSceneChains(assocChainIDs);
                if(node_nobox != null)
                    if(Integer.parseInt(node_nobox.getTextContent()) == 1)
                        d.setOrigNoboxChains(assocChainIDs);

                if(boxID > -1)
                    boxCounter++;
            }
        } catch(Exception ex) {
            Logger.log(ex);
        }
    }

    /**Returns the bounding box XML file content for the given Document
     * in PASCAL-VOC format
     *
     * @param d
     */
    public static String writeBoundingBoxFile(structures.Document d) {
        Map<Integer, Set<String>> boxChainSetDict =
                new HashMap<>();
        for(BoundingBox b : d.getBoundingBoxSet()){
            boxChainSetDict.put(b.getIdx(), new HashSet<>());
            for(Mention m : d.getMentionSetForBox(b))
                boxChainSetDict.get(b.getIdx()).add(m.getChainID());
        }

        return writeBoundingBoxFile(d.getID(), d.height, d.width,
                d.getBoundingBoxSet(), boxChainSetDict, d.getChainSet());
    }

    /**Returns the bounding box XML file content for the given set of boxes
     * in PASCAL-VOC format; originally written for use with a subset of
     * Document's boxes
     *
     * @param docID
     * @param height
     * @param width
     * @param boxSet
     * @param boxChainSetDict
     * @param chainSet
     * @return
     */
    public static String writeBoundingBoxFile(String docID,
                                              int height, int width,
                                              Collection<BoundingBox> boxSet,
                                              Map<Integer, Set<String>> boxChainSetDict,
                                              Collection<Chain> chainSet)
    {
        //first, create mappings we'll need as we create the file
        Map<String, Boolean> chainSceneDict = new HashMap<>();
        Map<String, Boolean> chainNoboxDict = new HashMap<>();
        for(Chain c : chainSet) {
            chainSceneDict.put(c.getID(), c.isScene);
            chainNoboxDict.put(c.getID(), c.isOrigNobox);
        }

        try{
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root <annotation> element
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("annotation");
            doc.appendChild(rootElement);

            //Add a standalone filename element
            Element filenameElement = doc.createElement("filename");
            filenameElement.appendChild(doc.createTextNode(docID));
            rootElement.appendChild(filenameElement);

            //add the top level size element and its members
            Element size = doc.createElement("size");
            rootElement.appendChild(size);
            Element widthElem = doc.createElement("width");
            widthElem.appendChild(doc.createTextNode(String.valueOf(width)));
            size.appendChild(widthElem);
            Element heightElem = doc.createElement("height");
            heightElem.appendChild(doc.createTextNode(String.valueOf(height)));
            size.appendChild(heightElem);
            Element depth = doc.createElement("depth");
            depth.appendChild(doc.createTextNode("3"));
            size.appendChild(depth);

            //Put each bounding box into a separate object tag
            for(BoundingBox b : boxSet) {
                Element object = doc.createElement("object");
                rootElement.appendChild(object);
                for(String chainID : boxChainSetDict.get(b.getIdx())){
                    Element name = doc.createElement("name");
                    name.appendChild(doc.createTextNode(docID + "_" + chainID));
                    object.appendChild(name);

                    //remove this accounted-for chain from the set
                    chainSceneDict.remove(chainID);
                }
                Element bndbx = doc.createElement("bndbox");
                object.appendChild(bndbx);
                Element xmin = doc.createElement("xmin");
                xmin.appendChild(doc.createTextNode(String.valueOf(b.getXMin())));
                bndbx.appendChild(xmin);
                Element xmax = doc.createElement("xmax");
                xmax.appendChild(doc.createTextNode(String.valueOf(b.getXMax())));
                bndbx.appendChild(xmax);
                Element ymin = doc.createElement("ymin");
                ymin.appendChild(doc.createTextNode(String.valueOf(b.getYMin())));
                bndbx.appendChild(ymin);
                Element ymax = doc.createElement("ymax");
                ymax.appendChild(doc.createTextNode(String.valueOf(b.getYMax())));
                bndbx.appendChild(ymax);
            }

            //for each chain we haven't yet covered, add an object tag
            for(String chainID : chainSceneDict.keySet()){
                Element object = doc.createElement("object");
                rootElement.appendChild(object);

                Element name = doc.createElement("name");
                name.appendChild(doc.createTextNode(docID + "_" + chainID));
                object.appendChild(name);

                int isScene = 0;
                if(chainSceneDict.containsKey(chainID))
                    isScene = Util.castInteger(chainSceneDict.get(chainID));
                int isNobox = 0;
                if(chainNoboxDict.containsKey(chainID))
                    isNobox = Util.castInteger(chainNoboxDict.get(chainID));

                Element scene = doc.createElement("scene");
                scene.appendChild(doc.createTextNode(""+isScene));
                object.appendChild(scene);
                Element bndbx = doc.createElement("nobndbox");
                bndbx.appendChild(doc.createTextNode(""+isNobox));
                object.appendChild(bndbx);
            }

            //write the file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);
            StringWriter sw = new StringWriter();
            transformer.transform(source, new StreamResult(sw));
            return sw.toString();
        } catch(Exception ex){
            Logger.log("Could not save bounding boxes for doc:%s",
                    docID);
            Logger.log(ex);
        }
        return null;
    }

/*
    public static void writeBoundingBoxFile(structures.Document d,
                                 Set<String> usedChainSet,
                                 Map<Integer, Set<String>> boxChainSetDict)
    {
        try{
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root <annotation> element
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("annotation");
            doc.appendChild(rootElement);

            //Add a standalone filename element
            String filename = d.getID().replace(".jpg","") + ".xml";
            Element filenameElement = doc.createElement("filename");
            filenameElement.appendChild(doc.createTextNode(d.getID()));
            rootElement.appendChild(filenameElement);

            //add the top level size element and its members
            Element size = doc.createElement("size");
            rootElement.appendChild(size);
            Element width = doc.createElement("width");
            width.appendChild(doc.createTextNode(String.valueOf(d.width)));
            size.appendChild(width);
            Element height = doc.createElement("height");
            height.appendChild(doc.createTextNode(String.valueOf(d.height)));
            size.appendChild(height);
            Element depth = doc.createElement("depth");
            depth.appendChild(doc.createTextNode("3"));
            size.appendChild(depth);

            //Put each bounding box into a separate object tag
            for(BoundingBox b : d.getBoundingBoxSet()) {
                Element object = doc.createElement("object");
                rootElement.appendChild(object);
                for(String chainID : boxChainSetDict.get(b.getID())){
                    Element name = doc.createElement("name");
                    name.appendChild(doc.createTextNode(d.getID() + "_" + chainID));
                    object.appendChild(name);

                    //remove this accounted-for chain from the set
                    usedChainSet.remove(chainID);
                }
                Element bndbx = doc.createElement("bndbox");
                object.appendChild(bndbx);
                Element xmin = doc.createElement("xmin");
                xmin.appendChild(doc.createTextNode(String.valueOf(b.getXMin())));
                bndbx.appendChild(xmin);
                Element xmax = doc.createElement("xmax");
                xmax.appendChild(doc.createTextNode(String.valueOf(b.getXMax())));
                bndbx.appendChild(xmax);
                Element ymin = doc.createElement("ymin");
                ymin.appendChild(doc.createTextNode(String.valueOf(b.getYMin())));
                bndbx.appendChild(ymin);
                Element ymax = doc.createElement("ymax");
                ymax.appendChild(doc.createTextNode(String.valueOf(b.getYMax())));
                bndbx.appendChild(ymax);
            }

            //for each chain we haven't yet covered, add an object tag
            for(Chain c : d.getChainSet()){
                Element object = doc.createElement("object");
                rootElement.appendChild(object);

                Element name = doc.createElement("name");
                name.appendChild(doc.createTextNode(d.getID() + "_" + chainID));
                object.appendChild(name);

                int isScene = 0;
                if(chainSceneDict != null)
                    if(chainSceneDict.containsKey(chainID))
                        isScene = Util.castInteger(chainSceneDict.get(chainID));

                Element scene = doc.createElement("scene");
                scene.appendChild(doc.createTextNode(""+isScene));
                object.appendChild(scene);
                Element bndbx = doc.createElement("nobndbox");
                if(noboxDict.containsKey(chainID) && noboxDict.get(chainID))
                    bndbx.appendChild(doc.createTextNode("1"));
                else
                    bndbx.appendChild(doc.createTextNode("0"));
                object.appendChild(bndbx);
            }

            //write the file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("out/Annotations/" + filename));
            transformer.transform(source, result);
        } catch(Exception ex){
            Logger.log("Could not save bounding boxes for doc:%s",
                    d.getID());
            Logger.log(ex);
        }
    }*/
}
