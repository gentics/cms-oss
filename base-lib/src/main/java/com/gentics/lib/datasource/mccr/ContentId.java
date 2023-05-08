package com.gentics.lib.datasource.mccr;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.etc.StringUtils;

/**
 * Class for content IDs. Content IDs consist of the objType and the objId. Their string representation is [objType].[objId]
 */
public class ContentId implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 9125273814852320437L;

	/**
	 * Pattern for valid contentids
	 */
	protected final static Pattern CONTENTID_PATTERN = Pattern.compile("([0-9]+)\\.([0-9]+)");

	/**
	 * Object type
	 */
	protected int objType;

	/**
	 * Object ID
	 */
	protected int objId;

	/**
	 * String representation of the content id
	 */
	protected String contentId;

	/**
	 * Create an instance with objType and objId
	 * @param objType objType
	 * @param objId objId
	 */
	public ContentId(int objType, int objId) {
		this.objType = objType;
		this.objId = objId;
		generateString();
	}

	/**
	 * Create an instance from a string
	 * @param contentId string representation of a content id
	 * @throws DatasourceException if the string representation is invalid
	 */
	public ContentId(String contentId) throws DatasourceException {
		if (contentId == null) {
			throw new DatasourceException("Invalid contentid [" + contentId + "]");
		}
		Matcher m = CONTENTID_PATTERN.matcher(contentId);

		if (!m.matches()) {
			throw new DatasourceException("Invalid contentid [" + contentId + "]");
		}
		objType = ObjectTransformer.getInt(m.group(1), 0);
		objId = ObjectTransformer.getInt(m.group(2), 0);
		this.contentId = contentId;
	}

	@Override
	public String toString() {
		return contentId;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ContentId) {
			return StringUtils.isEqual(contentId, ((ContentId) obj).contentId);
		} else {
			String compContentId = null;

			if (obj instanceof Resolvable) {
				compContentId = ObjectTransformer.getString(((Resolvable) obj).get("contentid"), null);
			} else {
				compContentId = ObjectTransformer.getString(obj, null);
			}
			return StringUtils.isEqual(contentId, compContentId);
		}
	}

	@Override
	public int hashCode() {
		return contentId.hashCode();
	}

	/**
	 * Generate the string representation
	 */
	protected void generateString() {
		StringBuffer buf = new StringBuffer();

		buf.append(objType).append(".").append(objId);
		contentId = buf.toString();
	}
}
