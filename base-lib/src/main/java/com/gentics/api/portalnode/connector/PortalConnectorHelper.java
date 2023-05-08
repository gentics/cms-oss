/*
 * @author herbert
 * @date 27.03.2006
 * @version $Id$
 */
package com.gentics.api.portalnode.connector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * A helper class for the Gentics Portal.Connector.<br>
 * Currently can be used to replace plinks within a page content.
 * @author herbert
 */
public final class PortalConnectorHelper {
	private static NodeLogger logger = NodeLogger.getNodeLogger(PortalConnectorHelper.class);
    
	private static final String PLINK_RESOLVE_REGEXP = "<plink id=\"([\\w\\.]+)\"((?:\\s+\\w+=\"[\\w\\.]+\")*)\\s*(?:/)?>";

	/**
	 * Don't allow any instances of this class...
	 */
	private PortalConnectorHelper() {}
    
	/**
	 * Can be used to replace the plink tags within a content of a page with
	 * URLs.<br>
	 * A plink has the syntax of: &lt;plink id="contentid" /&gt; - It may
	 * contain additional attributes after id. The use of this helper is
	 * optional, the plinks can as well be replaced by hand.
	 * @param content The content string of the page.
	 * @param replacer A resolver used to resolve contentids to links.
	 * @return the whole pagecontent, plinks replaced with urls using the given
	 *         replacer.
	 * @see PLinkReplacer
	 */
	public static String replacePLinks(String content, PLinkReplacer replacer) {
		Pattern plinkResolverPattern = Pattern.compile(PLINK_RESOLVE_REGEXP);
		Matcher matcher = plinkResolverPattern.matcher(content);
		StringBuffer buf = new StringBuffer();

		while (matcher.find()) {
			Map attributes = new HashMap();
			// Group 2 are the attributes
			String attributeString = matcher.group(2);

			if (attributeString != null) {
				String[] atts = attributeString.trim().split("[ =\"]+");

				for (int i = 0; i + 1 < atts.length;) {
					attributes.put(atts[i++], atts[i++]);
				}
			}
			PLinkInformation plink = new PLinkInformation(matcher.group(1), attributes);
			String replacement = replacer.replacePLink(plink);

			if (replacement == null) {
				logger.warn(
						"The PLinkReplacer {" + replacer.getClass().getName() + "} returned null for the PLink {" + plink.getContentId()
						+ "}. The PLink will be replaced by an empty string.");
				matcher.appendReplacement(buf, "");
			} else {
				matcher.appendReplacement(buf, replacement);
			}
            
		}
		matcher.appendTail(buf);
		return buf.toString();
	}

	/**
	 * This helper method can be used to fetch a given language variant for the
	 * given page object (which was published from Gentics Content.Node). It
	 * first tries to get the object linked by the attribute
	 * "content_[languageCode]" and if that fails, it fetches the page with same
	 * contentset_id and content_language set to the requested content_language
	 * @param page page for which the language variant shall be fetched
	 * @param languageCode language code of the language variant
	 * @param ds datasource that holds the page objects
	 * @return language variant or null if not found
	 * @throws NodeException in case of errors
	 */
	public final static Resolvable getLanguageVariant(Resolvable page, String languageCode,
			Datasource ds) throws NodeException {
		if (page == null || StringUtils.isEmpty(languageCode)) {
			return null;
		}

		// if the object is not of type page (10007), we also return null
		if (ObjectTransformer.getInt(page.get("obj_type"), -1) != 10007) {
			return null;
		}

		Resolvable languageVariant = null;

		// first try the direct link
		Object o = page.get("contentid_" + languageCode);

		if (o instanceof Resolvable) {
			languageVariant = (Resolvable) o;
		} else {
			// now let's see, whether a contentset_id is set
			int contentsetId = ObjectTransformer.getInt(page.get("contentset_id"), -1);

			if (contentsetId > 0) {
				// contentset id is set, so get the language variant from the
				// filter
				DatasourceFilter languageVariantFilter = ds.createDatasourceFilter(
						ExpressionParser.getInstance().parse(
								"object.obj_type == 10007 AND object.contentset_id == page.contentset_id AND object.content_language == data.content_language"));
				Map dataMap = new HashMap();

				dataMap.put("content_language", languageCode);
				languageVariantFilter.addBaseResolvable("page", page);
				languageVariantFilter.addBaseResolvable("data", new MapResolver(dataMap));
				Collection languageVariants = ds.getResult(languageVariantFilter, null);

				if (languageVariants.size() > 0) {
					languageVariant = (Resolvable) languageVariants.iterator().next();
				}
			}
		}

		return languageVariant;
	}

	/**
	 * Clear the object and attribute cache for all objects in the given datasource
	 * @param datasource datasource
	 */
	public static void clearCache(Datasource datasource) {
		try {
			GenticsContentFactory.clearCaches(datasource);
		} catch (PortalCacheException e) {
			logger.warn("Error while clearing cache for datasource " + datasource, e);
		}
	}

	/**
	 * Clear the object and attribute cache of the given object in the given datasource
	 * @param datasource datasource
	 * @param contentId content id
	 */
	public static void clearCache(Datasource datasource, String contentId) {
		try {
			GenticsContentFactory.uncacheObject(datasource, contentId);
		} catch (PortalCacheException e) {
			logger.warn("Error while clearing cache of " + contentId + " in datasource " + datasource, e);
		}
	}

	/**
	 * Clear the query caches for the given datasource
	 * @param datasource datasource
	 */
	public static void clearQueryCache(Datasource datasource) {
		if (datasource instanceof CNWriteableDatasource) {
			((CNWriteableDatasource) datasource).clearQueryCache();
		}
	}
}
