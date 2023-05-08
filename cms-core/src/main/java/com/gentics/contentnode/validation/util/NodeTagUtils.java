/*
 * @author tobiassteiner
 * @date Jan 1, 2011
 * @version $Id: NodeTagUtils.java,v 1.1.2.1 2011-02-10 13:43:40 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util;

import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Note: the set of methods that operate on strings support any valid XML name
 * for node tag attributes (with the exception of code points encoded with
 * surrogate pairs, which are not supported at the moment). The set of valid
 * XML names may not, however, be the same as the set of valid node tag
 * attributes as supported by the GCN rendering engine.
 */
public class NodeTagUtils {
    
	/**
	 * Attribute name of node tags.
	 * E.g.: <node value> will be turned into <node tag="value">
	 */
	public static final String NODE_TAG_ATTRIBUTE = "tag";
    
	/**
	 * Property that holds the name of <node> tags - usually this will be "node".
	 */
	public static final String NODE_TAG_PROPERTY = "tag_prefix";
    
	/**
	 * The <node ...> tag name. This is configurable in the node.conf, though, curiously, it's
	 * not included in the GLOBAL_CONFIG_VARS, so we'll just hard-code the value.
	 */
	public static final String NODE_TAG_NAME = "node";
    
	// Generated with dd if=/dev/random bs=1 count=16 |perl -MMIME::Base64 -ne 'print encode_base64($_), "\n"'
	public final static String RANDOM_NODE_TAG_REPLACEMENT = "xHGS9wcdwrxhQpvGk55asw==";

	/**
	 * Pattern string for a valid node tag name.
	 * Only XML is supported, so that transforming it back and forth between
	 * XML and GTX representations will not break the markup. 
	 */
	public final static String NODE_TAG_VALUE_PATTERN_STR = XMLSyntaxUtils.XML_NAME_PATTERN_STRING;
    
	protected final static String CLOSING_NODE_TAG_PATTERN_STR = "(?:</node\\s*>)?";
    
	// <node tag.name /?>
	protected final static Pattern NODE_TAG_UNGTXTIFY_PATTERN = Pattern.compile("<" + NODE_TAG_NAME + "\\s+(" + NODE_TAG_VALUE_PATTERN_STR + ")(\\s*/?)>");

	// <node tag="tag.name">
	protected final static String NODE_TAG_UNGTXTIFY_REPLACEMENT = "<" + NODE_TAG_NAME + " " // <node
			+ NODE_TAG_ATTRIBUTE + "=\"$1\"/>"; // tag="tag.name">
    
	// <node tag="tag.name" /?>
	protected final static Pattern NODE_TAG_GTXTIFY_PATTERN = Pattern.compile("<" + NODE_TAG_NAME + "\\s+" // <node
			+ NODE_TAG_ATTRIBUTE + "\\s*=\\s*" // tag=
			+ "(['\"])(" + NODE_TAG_VALUE_PATTERN_STR + ")\\1" // "tag.name"
			+ "\\s*/?>" // /?>
			+ CLOSING_NODE_TAG_PATTERN_STR); // (</node>)?
    
	// <node tag.name>
	protected final static String NODE_TAG_GTXTIFY_REPLACEMENT = "<" + NODE_TAG_NAME + " $2>";
    
	// <node tag.name /?>
	protected final static Pattern NODE_TAG_TEXTIFY_PATTERN = NODE_TAG_UNGTXTIFY_PATTERN;
    
	// (RANDOMSTR tag.name /?)
	protected final static String NODE_TAG_TEXTIFY_REPLACEMENT = "(" + RANDOM_NODE_TAG_REPLACEMENT + " $1$2)"; 

	// (RANDOMSTR tag.name /?)
	protected final static Pattern NODE_TAG_UNTEXTIFY_PATTERN = Pattern.compile("\\(" + RANDOM_NODE_TAG_REPLACEMENT // (RANDSTR
			+ "\\s+(" + NODE_TAG_VALUE_PATTERN_STR + ")(\\s*/?)\\)"// tag.name /?)
			);

	// <node tag.name /?>
	protected final static String NODE_TAG_UNTEXTIFY_REPLACEMENT = "<" + NODE_TAG_NAME + " $1$2>";

	// <node tag.name="(tag.name)?" /?>(</node>)?
	protected final static Pattern NODE_TAG_GTXTIFY_TIDIED_PATTERN = Pattern.compile("<" + NODE_TAG_NAME + "\\s+(" // <node
			+ NODE_TAG_VALUE_PATTERN_STR // tag.name
			+ ")\\s*=\\s*(['\"])\\1?\\2" // ="(tag.name)?"
			+ "\\s*/?>" // /?>
			+ CLOSING_NODE_TAG_PATTERN_STR); // (</node>)?
    
	// <node tag.name>
	protected final static String NODE_TAG_GTXTIFY_TIDIED_REPLACEMENT = "<" + NODE_TAG_NAME + " $1>";
    
	/**
	 * Turns &lt;node tag.name/?> into (RANDOMSTR#tag.name/?) 
	 * It is guaranteed that ill-formed node tags will not be modified.
	 * Preserves closing slash.
	 */
	public static String textifyNodeTags(String ml) {
		return NODE_TAG_TEXTIFY_PATTERN.matcher(ml).replaceAll(NODE_TAG_TEXTIFY_REPLACEMENT);
	}
    
	/**
	 * Turns (RANDOMSTR#tag.name/?) into &lt;node tag.name/?> 
	 * It is guaranteed that ill-formed node tags will not be modified.
	 * Preserves closing slash.
	 */
	public static String untextifyNodeTags(String ml) {
		return NODE_TAG_UNTEXTIFY_PATTERN.matcher(ml).replaceAll(NODE_TAG_UNTEXTIFY_REPLACEMENT);
	}
    
	/**
	 * Turns &lt;node tag.name/?> into &lt;node tag="tag.name"/>
	 * It is guaranteed that ill-formed node tags will not be modified.
	 * Always adds a closing slash.
	 */
	public static String ungtxtifyNodeTags(String ml) {
		return NODE_TAG_UNGTXTIFY_PATTERN.matcher(ml).replaceAll(NODE_TAG_UNGTXTIFY_REPLACEMENT);
	}
    
	/**
	 * Turns &lt;node tag="tag.name"/?>(&lt;/node>)? into &lt;node tag.name>
	 * It is guaranteed that ill-formed node tags will not be modified.
	 * Doesn't preserve closing slash.
	 */
	public static String gtxtifyNodeTags(String ml) {
		return NODE_TAG_GTXTIFY_PATTERN.matcher(ml).replaceAll(NODE_TAG_GTXTIFY_REPLACEMENT);
	}
    
	/**
	 * Turns &lt;node tag.name="(tag.name)?"/?>(&lt;/node>)? into &lt;node tag.node>
	 * It is guaranteed that ill-formed node tags will not be modified.
	 * Doesn't preserve closing slash.
	 */
	public static String gtxtifyTidiedNodeTags(String ml) {
		return NODE_TAG_GTXTIFY_TIDIED_PATTERN.matcher(ml).replaceAll(NODE_TAG_GTXTIFY_TIDIED_REPLACEMENT); 
	}
    
	/**
	 * Node tags have to be converted into a form that can be validated by AntiSamy:
	 * &lt;node tag.name=""> will become &lt;node tag="tag.name">.
	 * 
	 *   ...tag.name=""... is the parsed GTX form (the value may either be empty or
	 *   the same as the attribute name).
	 *   
	 *   ...tag="tag.name"... is the reversed XML form.
	 * 
	 * @see NodeTagAttributeReverser
	 */
	public static void reverseFirstNodeTagAttribute(Node node) {
		if (Node.ELEMENT_NODE == node.getNodeType() && NODE_TAG_NAME.equals(node.getNodeName())) {
			Element elem = (Element) node;
			Node tagAttr = elem.getAttributes().item(0);

			if (null != tagAttr) {
				String name = tagAttr.getNodeName();
				String value = tagAttr.getNodeValue();

				elem.removeAttribute(name);
				elem.setAttribute(NODE_TAG_ATTRIBUTE, value);
			}
		}
		NodeList children = node.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			reverseFirstNodeTagAttribute(children.item(i));
		}
	}
}
