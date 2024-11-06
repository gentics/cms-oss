/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: ContentLanguage.java,v 1.8 2009-12-16 16:12:12 herbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.render.GCNRenderable;

/**
 * The ContentLanguages are the languages used for content within a node.
 */
@TType(ContentLanguage.TYPE_CONTENTLANGUAGE)
public interface ContentLanguage extends GCNRenderable, NodeObject, Resolvable, NamedNodeObject {
	/**
	 * Transform the node object into its rest model
	 */
	public final static Function<ContentLanguage, com.gentics.contentnode.rest.model.ContentLanguage> TRANSFORM2REST = lang -> {
		com.gentics.contentnode.rest.model.ContentLanguage model = new com.gentics.contentnode.rest.model.ContentLanguage();
		model.setId(lang.getId());
		model.setGlobalId(lang.getGlobalId().toString());
		model.setName(lang.getName());
		model.setCode(lang.getCode());
		return model;
	};

	/**
	 * Function that transforms the rest model into the given node model
	 */
	public final static BiFunction<com.gentics.contentnode.rest.model.ContentLanguage, ContentLanguage, ContentLanguage> REST2NODE = (restModel, language) -> {
		if (!StringUtils.isBlank(restModel.getName())) {
			language.setName(restModel.getName());
		}
		if (!StringUtils.isBlank(restModel.getCode())) {
			language.setCode(restModel.getCode());
		}
		return language;
	};

	public static final int TYPE_CONTENTLANGUAGE = 10023;
	public static final int TYPE_CONTENTGROUP = 10031;

	public final static Integer TYPE_CONTENTLANGUAGE_INTEGER = new Integer(TYPE_CONTENTLANGUAGE);

	/**
	 * Maximum length for names
	 */
	public final static int MAX_NAME_LENGTH = 255;

	/**
	 * Maximum length for codes
	 */
	public final static int MAX_CODE_LENGTH = 5;

	/**
	 * get the name of the language.
	 * @return the name of the language.
	 */
	@FieldGetter("name")
	String getName();

	/**
	 * Set the name of the language
	 * @param name name of the language
	 * @throws ReadOnlyException
	 */
	@FieldSetter("name")
	void setName(String name) throws ReadOnlyException;

	/**
	 * get the language-code of this language.
	 * @return the language-code.
	 */
	@FieldGetter("code")
	String getCode();

	/**
	 * Set the language code
	 * @param code language code
	 * @throws ReadOnlyException
	 */
	@FieldSetter("code")
	void setCode(String code) throws ReadOnlyException;

	/**
	 * Get the nodes to which this contentlanguage is assigned to
	 * @return list of nodes
	 * @throws NodeException
	 */
	List<Node> getNodes() throws NodeException;

	/**
	 * Get the pages using this language
	 * @return pages of this language
	 * @throws NodeException
	 */
	List<Page> getPages() throws NodeException;
}
