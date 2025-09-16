package com.gentics.contentnode.string;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.outerj.daisy.diff.HtmlCleaner;
import org.outerj.daisy.diff.XslFilter;
import org.outerj.daisy.diff.html.HTMLDiffer;
import org.outerj.daisy.diff.html.HtmlSaxDiffOutput;
import org.outerj.daisy.diff.html.TextNodeComparator;
import org.outerj.daisy.diff.html.dom.DomTreeBuilder;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.gentics.contentnode.rest.resource.impl.DiffResourceImpl;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

public class CNStringUtils extends StringUtils {

	/**
	 * Group 0 will be everything from start of the matched string upto including the first body opening tag.
	 * Group 1 will be the name of the html opening tag, if the matched string includes an html opening tag.
	 * Group 2 will be the name of the body opening tag.
	 * TODO: This pattern doesn't respect CDATA sections.
	 */
	private static Pattern headerPattern = Pattern.compile("\\A.*?" + "(?:<(html)\\b)" + ".*?<(body)\\b(?:[^\"'>]*+|\"[^\"]*+\"|'[^']*+')*>",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	/**
	 * See {@link DiffResourceImpl#daisyDiff(com.gentics.contentnode.rest.model.request.DaisyDiffRequest)}.
	 * 
	 * @TODO the daisydiff algorithm introduces a lot of css classes and content elements which
	 *   require the corresponding css and javascript files to work properly.
	 * 
	 * @param older a serialized HTML document
	 * @param newer a serialized HTML document
	 * @return a serialized HTML document that contains all content of the two given HTML documents
	 *   together with annotations about the differences.
	 * @throws TransformerException 
	 * @throws SAXException 
	 * @throws IOException 
	 */
	public static String daisyDiff(String older, String newer) throws TransformerException, SAXException, IOException {

		older = replaceCdata(older);
		newer = replaceCdata(newer);
    	
		Matcher headerMatcher = headerPattern.matcher(newer);
		boolean hasBody = headerMatcher.find();
    	
		// the diff algorithm requires a root node, otherwise either content will be silently stripped
		// (HtmlCleaner below) or an error will be reported (HtmlDiffer below).
		// if there is a body tag (hasBody) then the document should have a root node, but this root
		// node will be stripped (HtmlCleaner again).
		// To solve this we strip the header ourselves and add a wrapper div.
		if (hasBody) {
			// We only strip the header and depend on the HtmlCleaner to correctly strip the </body></html> tags. 
			newer = newer.substring(headerMatcher.group().length());
			Matcher oldHeaderMatcher = headerPattern.matcher(older);

			if (oldHeaderMatcher.find()) {
				older = older.substring(oldHeaderMatcher.group().length());
			}
		}
    	
		String diffWrapperId = "gtxDiffWrapper";

		older = "<div id='" + diffWrapperId + "'>" + older + "</div>";
		newer = "<div id='" + diffWrapperId + "'>" + newer + "</div>";
    	
		SAXTransformerFactory tfFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
		TransformerHandler resultHandler = tfFactory.newTransformerHandler();
    	
		DOMResult domResult = new DOMResult();

		resultHandler.setResult(domResult);
    	
		InputSource oldSource = new InputSource(new StringReader(older));
		InputSource newSource = new InputSource(new StringReader(newer));
    	
		HtmlCleaner cleaner = new HtmlCleaner();
		DomTreeBuilder oldCleaned = new DomTreeBuilder();

		cleaner.cleanAndParse(oldSource, oldCleaned);
		DomTreeBuilder newCleaned = new DomTreeBuilder();

		cleaner.cleanAndParse(newSource, newCleaned);
		
		TextNodeComparator leftComparator = new TextNodeComparator(oldCleaned, Locale.getDefault());
		TextNodeComparator rightComparator = new TextNodeComparator(newCleaned, Locale.getDefault());
		
		XslFilter filter = new XslFilter();
		ContentHandler filterHandler = filter.xsl(resultHandler, "com/gentics/lib/etc/daisyDiffPostprocess.xsl");

		filterHandler.startDocument();
		filterHandler.startElement("", "diffreport", "diffreport", new AttributesImpl());
		filterHandler.startElement("", "diff", "diff", new AttributesImpl());
		HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(filterHandler, "gtxDiff");
		HTMLDiffer differ = new HTMLDiffer(output);

		differ.diff(leftComparator, rightComparator);
		filterHandler.endElement("", "diff", "diff");
		filterHandler.endElement("", "diffreport", "diffreport");
		filterHandler.endDocument();

		StringWriter result = new StringWriter();
		Transformer serializer = tfFactory.newTransformer();
		
		serializer.setOutputProperty(OutputKeys.METHOD, "html");
		// the xml declaration isn't being output if the output method is "html" (like above) 
		// serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "true");
		
		serializer.transform(new DOMSource(domResult.getNode()), new StreamResult(result));
		String resultString = result.toString();
		
		StringBuilder diff = new StringBuilder();

		// the cleanup procedure (HtmlCleaner above) strips the header.
		// this is sensible, since we don't want to show differences in the header (would generate invalid html).
		// if the newer document did include a header, we add it again to make sure any CSS dependencies are loaded.
		if (hasBody && !headerPattern.matcher(resultString).find()) {
			String header = headerMatcher.group(0);
			String htmlName = headerMatcher.group(1);
			String bodyName = headerMatcher.group(2);
			
			diff.append(header);
			diff.append(resultString);
			
			// attempt to append the footer corresponding to the header.
			// TODO instead, we should match the footer in the newer document and append that.
			diff.append("</" + bodyName + ">");
			if (!com.gentics.lib.etc.StringUtils.isEmpty(htmlName)) {
				diff.append("</" + htmlName + ">");
			}
		} else {
			diff.append(resultString);
		}
    	
		return diff.toString();
	}

	/**
	 * Replaces all CDATA sections in the given (presumable HTML) String.
	 * This is supposed to make up for the lack of CDATA support in the daisydiff algorithm.
	 * 
	 * @param str a String that may contain one or more CDATA sections
	 * @return the given String with all CDATA sections replaced with there equivalent non-CDATA representation (escaped).
	 */
	private static String replaceCdata(String str) {
		StringBuilder builder = new StringBuilder(str.length());
		int end = 0;
		int off;

		while (-1 != (off = str.indexOf("<![CDATA[", end))) {
			builder.append(str.substring(0, off));
			end = str.indexOf("]]>", off);
			if (-1 == end) {
				NodeLogger.getLogger(CNStringUtils.class).warn("Ignoring unbalanced CDATA section");
				end = off;
				break;
			}
			off += "<![CDATA[".length();
			String cdata = str.substring(off, end);

			end += "]]>".length();
			builder.append(org.apache.commons.lang3.StringEscapeUtils.escapeXml(cdata));
		}
		builder.append(str.substring(end));
		return builder.toString();
	}

	/**
	 * Escapes characters with special meaning in regular expressions.
	 *
	 * The following characters are escaped with an <code>\</code>:
	 * <code>.</code>, <code>?</code>, <code>*</code>, <code>+</code>,
	 * <code>|</code>, <code>^</code>, <code>$</code>, <code>(</code>,
	 * <code>)</code>, <code>{</code>, <code>}</code>, <code>[</code>,
	 * <code>]</code> and <code>\</code>.
	 *
	 * @param string The string to be escaped.
	 * @return The input string will all occurrences of special regular
	 *	expression characters escaped with a <code>\</code>.
	 */
	public static String escapeRegex(String string) {
		return string.replaceAll("([.?*+|^$(){}\\\\\\[\\]])", "\\\\$1");
	}
}
