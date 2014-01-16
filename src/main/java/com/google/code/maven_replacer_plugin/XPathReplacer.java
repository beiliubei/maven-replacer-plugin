package com.google.code.maven_replacer_plugin;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XPathReplacer implements Replacer {

	private final TokenReplacer tokenReplacer;
	private final DocumentBuilder docBuilder;
	private final XPath xpath;
	private final Transformer transformer;

	public XPathReplacer(TokenReplacer tokenReplacer) {
		try {
			if (tokenReplacer == null) {
				throw new IllegalArgumentException("Must supply a tokenReplacer to change the node's content.");
			}
			
			this.tokenReplacer = tokenReplacer;
			this.xpath = XPathFactory.newInstance().newXPath();
			this.docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			this.transformer = TransformerFactory.newInstance().newTransformer();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to initialise XML processing: " + e.getMessage(), e);
		}
	}

	public String replace(String content, Replacement replacement, boolean regex, int regexFlags) {
		try {
			Document doc = parseXml(content);
			NodeList replacementTargets = findReplacementNodes(doc, replacement.getXpath());
			replaceContent(replacementTargets, replacement, regex, regexFlags);
			return writeXml(doc);
		} catch (Exception e) {
			String cause = e.getMessage() != null ? e.getMessage() : e.getCause().getMessage();
			throw new RuntimeException("Error during XML replacement: " + cause, e);
		}
	}

	private void replaceContent(NodeList replacementNodes, Replacement replacement, boolean regex, int regexFlags) throws Exception {
		for (int i=0; i < replacementNodes.getLength(); i++) {
			Node replacementNode = replacementNodes.item(i);

			switch (replacementNode.getNodeType()) {
			case Node.ATTRIBUTE_NODE: case Node.TEXT_NODE:
				String replacedValue = tokenReplacer.replace(replacementNode.getTextContent(), replacement, regex, regexFlags);
				replacementNode.setNodeValue(replacedValue);
				break;
			default:
				String replacementNodeStr = convertNodeToString(replacementNode);
				String replacedNodeStr = tokenReplacer.replace(replacementNodeStr, replacement, regex, regexFlags);

				Node parent = replacementNode.getParentNode();
				if (parent.getOwnerDocument() == null) {
					throw new UnsupportedOperationException("Cannot replace a node's content not part of a parent node.");
				}
				Node replacedNode = convertXmlToNode(replacedNodeStr);
				Node newNode = parent.getOwnerDocument().importNode(replacedNode, true);
				parent.replaceChild(newNode, replacementNode);
			}
		}
	}

	private Document parseXml(String content) throws Exception {
		return docBuilder.parse(new InputSource(new StringReader(content)));
	}

	private NodeList findReplacementNodes(Document doc, String xpathString) throws Exception {
		XPathExpression xpathExpr = xpath.compile(xpathString);
		return (NodeList) xpathExpr.evaluate(doc, XPathConstants.NODESET);
	}

	private String convertNodeToString(Node replacementTarget) throws TransformerException {
		DOMSource targetSource = new DOMSource(replacementTarget);
		StringWriter stringWriter = new StringWriter();
		Result stringResult = new StreamResult(stringWriter);
		transformer.transform(targetSource, stringResult);
		return stringWriter.toString();
	}

	private Node convertXmlToNode(String xml) throws Exception {
		InputSource docSource = new InputSource(new StringReader(xml));
		Document doc = docBuilder.parse(docSource);
		return doc.getFirstChild();
	}

	private String writeXml(Document doc) throws Exception {
		OutputFormat of = new OutputFormat(doc);
		of.setPreserveSpace(true);
		of.setEncoding(doc.getXmlEncoding());

		StringWriter sw = new StringWriter();
		XMLSerializer serializer = new XMLSerializer(sw, of);
		serializer.serialize(doc);
		return sw.toString();
	}
}
