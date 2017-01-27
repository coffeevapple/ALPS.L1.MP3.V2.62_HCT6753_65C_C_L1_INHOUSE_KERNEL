/**
 * Created on 2010/11/29
 * @author MTK80939
 */

package com.mediatek.mms.evdo.sanitytest;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * M: Utils calss for parser xml.
 */
public class XmlParser {
    private static final String TAG = "xmlParser";
    private Element mRoot;

    /**
     * Constructor function.
     *
     * @param path
     *            String file path to parser.
     */
    public XmlParser(String path) {
        Document mDoc;
        try {
            DocumentBuilderFactory mDocBuilderFactory = DocumentBuilderFactory.newInstance();
            mDocBuilderFactory.setValidating(false);
            mDocBuilderFactory.setNamespaceAware(true);
            DocumentBuilder mDocBuilder = mDocBuilderFactory.newDocumentBuilder();
            mDoc = mDocBuilder.parse(new File(path));
            mRoot = mDoc.getDocumentElement();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * return root node of xml file.
     *
     * @return Node root node.
     */
    public Node getRootNode() {
        return mRoot;
    }

    /**
     * get node by name in xml file.
     *
     * @param parentNode
     *            Node.
     * @param name
     *            String node name to find.
     * @return Node the node.
     */
    public Node getNodeByName(Node parentNode, String name) {
        if (parentNode == null || name == null) {
            Log.d(TAG, "Invalid input parameter");
            return null;
        }

        Node node = parentNode.getFirstChild();
        while (node != null) {
            if (name.equals(node.getNodeName())) {
                return node;
            }
            node = node.getNextSibling();
        }
        return null;
    }

    /**
     * get node by index.
     * @param parentNode
     *            Node.
     * @param nIndex
     *            Integer node index.
     * @return Node the node.
     */
    public Node getNodeByIndex(Node parentNode, int nIndex) {
        NodeList nodeList = parentNode.getChildNodes();
        int nTemp = nodeList.getLength();
        if (nIndex < 0 || nIndex > nTemp || nTemp <= 0) {
            Log.d(TAG, "Invalid input parameter");
            return null;
        }

        return nodeList.item(nIndex);
    }

    /**
     * get node name.
     *
     * @param node
     *            Node.
     * @return String node name.
     */
    public String getNodeName(Node node) {
        if (null == node) {
            Log.d(TAG, "Invalid input parameter");
            return null;
        }
        return node.getNodeName();
    }

    /**
     * get node value.
     *
     * @param node
     *            Node.
     * @return String node value.
     */
    public String getNodeValue(Node node) {
        if (null == node) {
            Log.d(TAG, "Invalid input parameter");
            return null;
        }

        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node subnode = list.item(i);
            if (subnode.getNodeType() == Node.TEXT_NODE
                    && !subnode.getNodeValue().equals("#text")) {
                return subnode.getNodeValue();
            }
        }
        return null;
    }

    /**
     * get node value and change value to Integer.
     *
     * @param node
     *            Node.
     * @return Integer node value in Integer.
     */
    public int getNodeValueInt(Node node) {
        String str = getNodeValue(node);
        if (str == null) {
            return 0;
        } else {
            return Integer.getInteger(str);
        }
    }

    /**
     * get node value and change value to boolean.
     *
     * @param node
     *            Node.
     * @return boolean node value in boolean.
     */
    public boolean getNodeValueBool(Node node) {
        String str = getNodeValue(node);
        if (str == null) {
            return false;
        } else {
            return Boolean.getBoolean(str);
        }
    }

    /**
     * get Attributes value by attribute name.
     *
     * @param node
     *            Node.
     * @param attrName
     *            String Attributes name.
     * @return String attribute value.
     */
    public String getAttrValue(Node node, String attrName) {
        if (node == null || attrName == null) {
            Log.d(TAG, "Invalid input parameter");
            return null;
        }

        NamedNodeMap attributes = node.getAttributes();
        if (attributes == null) {
            return null;
        }

        return attributes.getNamedItem(attrName).getNodeValue();
    }

    /**
     * get Attributes value by attribute name,and change value to Integer.
     *
     * @param node
     *            Node.
     * @param attrName
     *            String.
     * @return Integer node value in Integer.
     */
    public int getAttrValueInt(Node node, String attrName) {
        String str = getAttrValue(node, attrName);
        if (str == null) {
            return 0;
        } else {
            int nTemp = Integer.parseInt(str);
            Log.d("MemTest", "test count is " + nTemp);
            return nTemp;
        }
    }

    /**
     * get Attributes value by attribute name,and change value to boolean.
     *
     * @param node
     *            Node.
     * @param attrName
     *            String.
     * @return boolean node value in boolean.
     */
    public boolean getAttrValueBool(Node node, String attrName) {
        String str = getAttrValue(node, attrName);
        if (str == null) {
            return false;
        } else {
            return Boolean.parseBoolean(str);
        }
    }

    /**
     * get node length.
     *
     * @param node
     *            Node.
     * @return Integer node length.
     */
    public int getLength(Node node) {
        if (null == node) {
            Log.d(TAG, "Invalid input parameter");
            return 0;
        }
        NodeList nodeList = node.getChildNodes();
        return nodeList.getLength();
    }

    /**
     * get node list.
     * @param parentNode parent node.
     * @param nodeName node name.
     * @return find ParentNode's all child nodes.
     */
    public NodeList getNodeList(Node parentNode, String nodeName) {
        if (parentNode == null || nodeName == null) {
            Log.d(TAG, "Invalid input parameter");
            return null;
        }
        Element element = (Element) parentNode;
        return element.getElementsByTagName(nodeName);
    }
}
