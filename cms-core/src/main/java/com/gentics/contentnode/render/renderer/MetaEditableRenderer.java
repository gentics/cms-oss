/*
 * @author Clemens
 * @date Aug 30, 2010
 * @version $Id: MetaEditableRenderer.java,v 1.2 2010-09-16 15:09:58 clemens Exp $
 */
package com.gentics.contentnode.render.renderer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.TemplateRenderer;

/**
 * the editable renderer will match <node obj.property:editable> tags and prepare them to become Aloha editables
 * @author Clemens
 *
 */
public class MetaEditableRenderer implements TemplateRenderer {
	// TODO this should be limited to match the actual functional range        
	protected static Pattern pMetaeditable = Pattern.compile("(<node (\\w+\\.\\w+):editable>)", Pattern.MULTILINE);
    
	// match readonly attributes
	protected static Pattern pReadonly = Pattern.compile("<node (\\w+):readonly>", Pattern.MULTILINE);

	/**
	 * key to identify metaeditables in the render result
	 */
	public static final String METAEDITABLES_KEY = "metaeditables";
    
	/**
	 * key to identify metaeditables in the render result
	 */
	public static final String READONLIES_KEY = "readonlyeditables";
    
	/**
	 * prefix for metaeditable ids
	 */
	public static final String EDITABLE_PREFIX = "GENTICS_METAEDITABLE_";
    
	/**
	 * search for the :editable modifier and mark those tags as editable
	 * add key "metaeditables" to the result, which contains a Collection of all meta-editables found  
	 */
	@SuppressWarnings("unchecked")
	public String render(RenderResult renderResult, String template) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		/*
		 * We don't want to replace meta editables in publish mode
		 */
		int editMode = t.getRenderType().getEditMode();

		if (editMode != RenderType.EM_ALOHA && editMode != RenderType.EM_ALOHA_READONLY) {
			return template;
		}
		// meta editables
		Matcher m = pMetaeditable.matcher(template);
		StringBuffer out = new StringBuffer();
        
		Vector matches = new Vector();
		String id; // id composed out of matches
		MatchResult res; // matcher result

		while (m.find()) {
			res = m.toMatchResult();
			// note: replacing to page.name will not work as jQuery will
			// interprete this as a css class name when applying .aloha()
			// therefore an "_" ist used instead of a "."
			id = res.group(2).replace(".", "_");
			m.appendReplacement(out, "<gtxEditable " + id + ">$1</gtxEditable " + id + ">");
			matches.add(res.group(2));
		}
        
		m.appendTail(out);
		if (matches.size() > 0) {
			renderResult.addParameters(METAEDITABLES_KEY, matches);
		}
        
		// readonly editables
		m = pReadonly.matcher(out);
		Vector readonlies = new Vector();

		while (m.find()) {
			res = m.toMatchResult();
			readonlies.add(res.group(1));
		}
        
		if (readonlies.size() > 0) {
			renderResult.addParameters(READONLIES_KEY, readonlies);
		}
        
		return out.toString();
	}

}
