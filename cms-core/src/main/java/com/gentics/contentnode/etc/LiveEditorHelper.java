/*
 * @author norbert
 * @date 19.03.2007
 * @version $Id: LiveEditorHelper.java,v 1.3 2010-09-28 17:01:28 norbert Exp $
 */
package com.gentics.contentnode.etc;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.render.RenderType;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * TODO comment this
 * @author norbert
 *
 */
public class LiveEditorHelper {
	protected static List splitNodeTags(String tagPrefix, String code) throws NodeException {
		String searchFor = "<" + tagPrefix + " ";

		// be aware, this does not check for tags within <node> tags!
		List parts = new Vector();
		int pos1 = 0;
		int pos2 = 0;
		int lastpos = 0;

		while ((pos1 = code.indexOf(searchFor, lastpos)) >= 0) {
			pos2 = code.indexOf('>', pos1);
			if (pos2 < 0) {
				// will prevent endless loops in case the node tag is not closed like:
				// <node kaputt     (should be <node kaputt>)
				NodeLogger.getLogger(LiveEditorHelper.class.getName()).error("could not find closing '>' for node tag in code {" + code + "}");
				break;
			}
            
			if (lastpos < pos1) {
				parts.add(new Part(false, code.substring(lastpos, pos1)));
				// $parts[] = array("code"=>substr($code, $lastpos, $pos1 - $lastpos), "istag"=>false);
			}
			parts.add(new Part(true, code.substring(pos1, pos2 + 1)));
			// $parts[] = array("code"=>substr($code, $pos1, $pos2 - $pos1 + 1), "istag"=>true);

			lastpos = pos2 + 1;
		}

		if (lastpos < code.length()) {
			parts.add(new Part(false, code.substring(lastpos)));
			// $parts[] = array("code"=>substr($code, $lastpos), "istag"=>false);
		}
		return parts;
	}

	protected static List prepareIEParts(List parts) throws NodeException {
		boolean lastIsTag = false;
		NodePreferences prefs = TransactionManager.getCurrentTransaction().getRenderType().getPreferences();
		boolean replaceSpace = prefs.getFeature("replace_live_spaces");

		for (Iterator iter = parts.iterator(); iter.hasNext();) {
			Part part = (Part) iter.next();

			if (part.isTag()) {
				lastIsTag = true;
			} else {
				// replace space with &nbsp;
				if (lastIsTag && replaceSpace && " ".equals(part.getCode().substring(0, 1))) {
					part.code = "&nbsp;" + part.getCode().substring(1);
				}
			}
		}

		return parts;
	}

	protected static String mergeParts(List parts) {
		StringBuffer code = new StringBuffer();

		for (Iterator iter = parts.iterator(); iter.hasNext();) {
			Part part = (Part) iter.next();

			code.append(part.getCode());
		}
		return code.toString();
	}

	public static String prepareIECode(String code) throws NodeException {
		NodePreferences prefs = TransactionManager.getCurrentTransaction().getRenderType().getPreferences();
		String tagPrefix = prefs.getProperty("tagprefix");

		List parts = splitNodeTags(tagPrefix, code);

		prepareIEParts(parts);
		return mergeParts(parts);
	}

	protected static class Part {
		protected boolean tag = false;
		protected String code = null;

		/**
		 * @param tag
		 * @param code
		 */
		public Part(boolean tag, String code) {
			this.tag = tag;
			this.code = code;
		}

		public String getCode() {
			return code;
		}

		public boolean isTag() {
			return tag;
		}
	}

	/**
	 * retrieve tag name for the liveedit wrapper tag, which may be div or span
	 * @param renderedTag the currently rendered tag
	 * @return div or span, depending on feature liveedit_spans
	 * @throws TransactionException 
	 */
	public static String getLiveeditWrapperTagName(Tag renderedTag) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
		String defaultValue = renderType.getPreferences().getFeature("liveedit_spans") ? "span" : "div";

		// when the feature 'liveedit_tag_perconstruct' is set, the construct might have an individual setting
		if (renderType.getPreferences().getFeature("liveedit_tag_perconstruct") && renderedTag != null) {
			Construct construct = renderedTag.getConstruct();
			String constructSetting = construct.getLiveEditorTagName();

			if (!StringUtils.isEmpty(constructSetting)) {
				defaultValue = constructSetting;
			}
		}

		return defaultValue;
	}
    
}
