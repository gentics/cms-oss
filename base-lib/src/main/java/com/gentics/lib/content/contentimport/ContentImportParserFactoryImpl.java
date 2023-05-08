/*
 * @author Stefan Hepp
 * @date 27.04.2005 16:36
 * @version $Id: ContentImportParserFactoryImpl.java,v 1.1 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.content.contentimport;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.GenticsContentObject;

/**
 * An implementation of a parser factory, which supports attributes, linked attributes,
 * foreign attribs and format options. <br>
 * The syntax for the header is <br>
 * <code>
 * <attributename>[:<foreignObjectNr>][.<referenceAttribute>][|<options>]
 * </code>
 * The reference attribute can itself be a reference or even an foreign reference.
 */
public class ContentImportParserFactoryImpl implements ContentImportParserFactory,
		ContentImportListener {

	public final char SEP_ATTRIBUTE = '.';

	public final char SEP_FORMAT = '|';

	public final char SEP_COUNT = ':';

	private Map foreignRefHelper;

	private GenticsContentImport importer;

	private ContentObjectHelper coHelper;
    
	private String fileprefix = null;

	/**
	 * create a new import factory. For each importer a new instance must be
	 * created. The factory registers itself as listener to the importer and
	 * uses its logger.
	 * 
	 * @param importer the importer for which the factory is used. 
	 * @param coHelper a contentobject helper.
	 */
	public ContentImportParserFactoryImpl(GenticsContentImport importer,
			ContentObjectHelper coHelper) {
		this.coHelper = coHelper;
		foreignRefHelper = new HashMap();
		this.importer = importer;
		importer.addListener(GenticsContentImport.EVENT_ON_ROW_FINISHED, this);
	}

	/**
	 * create a new import factory. For each importer a new instance must be
	 * created. The factory registers itself as listener to the importer and
	 * uses its logger.
	 * 
	 * @param importer the importer for which the factory is used. 
	 * @param coHelper a contentobject helper.
	 * @param fileprefix a fileprefix to set for each attribute.
	 */
	public ContentImportParserFactoryImpl(GenticsContentImport importer,
			ContentObjectHelper coHelper, String fileprefix) {
		this.coHelper = coHelper;
		foreignRefHelper = new HashMap();
		this.importer = importer;
		this.fileprefix = fileprefix;
		importer.addListener(GenticsContentImport.EVENT_ON_ROW_FINISHED, this);
	}

	public void resetImportModules() {
		for (Iterator it = foreignRefHelper.values().iterator(); it.hasNext();) {
			ForeignReferenceHelper refHelper = (ForeignReferenceHelper) it.next();

			refHelper.unregisterImport(importer);
		}
		foreignRefHelper.clear();
	}

	public ContentImportParser getImportModule(String header) {
		return getImportModule(header, coHelper);
	}

	/**
	 * get an import parser for a given header and use the given contenthelper.
	 * @param header the header containing the field configuration.
	 * @param coHelper the objecthelper to use.
	 * @return an import parser, or null if the header is invalid or null. 
	 */
	public ContentImportParser getImportModule(String header, ContentObjectHelper coHelper) {

		if (header == null || "".equals(header)) {
			return null;
		}

		// strip format of header name
		String format = "";
		int pos_format = header.indexOf(SEP_FORMAT);

		if (pos_format > -1) {
			format = header.substring(pos_format + 1);
			header = header.substring(0, pos_format);
		}

		// split header parts
		int pos = header.indexOf(SEP_ATTRIBUTE);
		int pos_cnt = header.indexOf(SEP_COUNT);

		ContentImportParser parser = null;
		ContentImportLogger logger = importer.getLogger();

		if (pos == -1) {

			if (pos_cnt > -1) {
				logger.addError(header, "An attributename must not contain any '" + SEP_COUNT + "'.");
				return null;
			}

			try {
				parser = new ContentAttributeParser(logger, coHelper, header);
			} catch (ContentImportException e) {
				logger.addError(header, e.getMessage());
				return null;
			}

		} else {

			String attrName = header.substring(0, pos);
			String refObjName = header.substring(pos + 1);

			if (pos_cnt > -1) {
				String foreignName = attrName.substring(0, pos_cnt);

				ForeignReferenceHelper refHelper = null;

				if (foreignRefHelper.containsKey(attrName)) {
					refHelper = (ForeignReferenceHelper) foreignRefHelper.get(attrName);
				} else {
					try {
						refHelper = new ForeignReferenceHelper(logger, coHelper, foreignName);
					} catch (ContentImportException e) {
						logger.addError(header, e.getMessage());
						return null;
					}
					foreignRefHelper.put(attrName, refHelper);
					refHelper.registerImport(importer);
				}

				// this is to support n:n
				ContentImportParser fRefParser = null;

				fRefParser = getImportModule(refObjName, coHelper.getInstance(refHelper.getReferenceObjectType()));

				try {
					parser = new ContentForeignReferenceParser(logger, refHelper, foreignName, fRefParser.getAttributeName());
					((ContentForeignReferenceParser) parser).setReferenceParser(fRefParser);
				} catch (ContentImportException e) {
					logger.addError(header, e.getMessage());
					return null;
				}
			} else {
				try {
					parser = new ContentReferenceParser(logger, coHelper, attrName, refObjName);
				} catch (ContentImportException e) {
					logger.addError(header, e.getMessage());
					return null;
				}
			}
		}

		if (parser != null) {
			parser.setFormat(format);
			parser.setFilePrefix(fileprefix);
		}

		return parser;
	}

	public ContentObjectHelper getObjectHelper() {
		return coHelper;
	}

	public void onEvent(int event, GenticsContentImport importer, GenticsContentObject cnObj) {

		if (event == GenticsContentImport.EVENT_ON_ROW_FINISHED) {

			Map foreignObjects = new HashMap(1);

			for (Iterator iterator = foreignRefHelper.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry entry = (Map.Entry) iterator.next();
				String key = (String) entry.getKey();

				ForeignReferenceHelper refHelper = (ForeignReferenceHelper) entry.getValue();
				String attrName = key.substring(0, key.indexOf(SEP_COUNT));

				Collection foreignObject = (Collection) foreignObjects.get(attrName);

				if (foreignObject == null) {
					foreignObject = new Vector(1);
				}

				GenticsContentObject refObj = refHelper.getForeignObject();

				if (refObj != null) {
					foreignObject.add(refObj);
				}

				foreignObjects.put(attrName, foreignObject);
			}

			ContentImportLogger logger = importer.getLogger();

			for (Iterator iterator = foreignObjects.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry entry = (Map.Entry) iterator.next();
				String attrName = (String) entry.getKey();

				try {
					String[] ids = coHelper.storeForeignObjects(cnObj, attrName, (Collection) entry.getValue());

					for (int i = 0; i < ids.length; i++) {
						String id = ids[i];

						logger.addImportId(id);
						logger.addInfo(attrName, "Added new Foreign Object '" + id + "'.");
					}

				} catch (NodeIllegalArgumentException e) {
					importer.getLogger().addError(attrName, "Could not store foreign object; " + e.getMessage());
				} catch (CMSUnavailableException e) {
					importer.getLogger().addError(attrName, "Could not store foreign object; " + e.getMessage());
				} catch (SQLException e) {
					importer.getLogger().addError(attrName, "Could not store foreign object; " + e.getMessage());
				} catch (DatasourceException e) {
					importer.getLogger().addError(attrName, "Could not store foreign object; " + e.getMessage());
				}

			}
		}
	}
}
