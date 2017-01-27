package com.mediatek.sms.evdo.sanitytest;

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
    private static final String tag = "Parser";
    private Document mDoc;
    private Element mRoot;

    /**
     * M: The Constructor.
     * @param path the file path.
     */
    public XmlParser(String path) {
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
     * M: get root node.
     * @return the root node.
     */
    public Node getRootNode() {
        return mRoot;
    }

    /**
     * M: get Node by name.
     * @param parentNode the parent.
     * @param name the node name.
     * @return the node.
     */
    public Node getNodeByName(Node parentNode, String name) {
        if (parentNode == null || name == null) {
            Log.d(tag, "Invalid input parameter");
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
     * M: get node by index.
     * @param parentNode parent node.
     * @param nIndex index of node.
     * @return the node.
     */
    public Node getNodeByIndex(Node parentNode, int nIndex) {
        NodeList nodeList = parentNode.getChildNodes();
        int nTemp = nodeList.getLength();
        if (nIndex < 0 || nIndex > nTemp || nTemp <= 0) {
            Log.d(tag, "Invalid input parameter");
            return null;
        }

        return nodeList.item(nIndex);
    }

    /**
     * M: get node name.
     * @param node node.
     * @return the node name.
     */
    public String getNodeName(Node node) {
        if (null == node) {
            Log.d(tag, "Invalid input parameter");
            return null;
        }
        return node.getNodeName();
    }

    /**
     * M: get Node Value.
     * @param node the node.
     * @return the node's value.
     */
    public String getNodeValue(Node node) {
        if (null == node) {
            Log.d(tag, "Invalid input parameter");
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
     * M: get the node's attribute's value.
     * @param node the node.
     * @param attrName the attribute's name.
     * @return the attribute's value.
     */
    public String getAttrValue(Node node, String attrName) {
        if (node == null || attrName == null) {
            Log.d(tag, "Invalid input parameter");
            return null;
        }

        NamedNodeMap attributes = node.getAttributes();
        if (attributes == null) {
            return null;
        }

        return attributes.getNamedItem(attrName).getNodeValue();
    }

    /**
     * M: get the node's children`s number.
     * @param node the node.
     * @return the number.
     */
    public int getLegth(Node node) {
        if (null == node) {
            Log.d(tag, "Invalid input parameter");
            return 0;
        }

        NodeList nodeList = node.getChildNodes();
        return nodeList.getLength();
    }
}
