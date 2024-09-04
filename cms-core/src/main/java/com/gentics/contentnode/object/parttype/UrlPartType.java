/*
 * @author Stefan Hepp
 * @date 06.12.2005
 * @version $Id: UrlPartType.java,v 1.21 2010-09-28 17:01:29 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.lib.log.NodeLogger;

/**
 * The url parttype renders urls to a given target.
 */
public abstract class UrlPartType extends AbstractPartType implements PartType {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2392676776361147075L;

	public static final int TARGET_PAGE = 1;
	public static final int TARGET_IMAGE = 2;
	public static final int TARGET_FILE = 3;
	public static final int TARGET_FOLDER = 4;

	private final static Set<String> resolvableKeys = SetUtils.unmodifiableSet("internal", "externalurl", "target", "url", "node", "nodeId");

	private int target;
	private boolean encoded;
	private Class<? extends NodeObject> targetClass;

	/**
	 * Create a new url parttype.
	 * @param value the value which should be rendered.
	 * @param target the target objecttype, one of the above constants.
	 * @param encoded true, if the url should be encoded. 
	 */
	public UrlPartType(Value value, int target, boolean encoded) throws NodeException {
		super(value);
		this.encoded = encoded;
		setTarget(target);
	}

	public UrlPartType(Value value, int target) throws NodeException {
		super(value);
		this.encoded = false;
		setTarget(target);
	}

	@Override
	public Set<String> getResolvableKeys() {
		return resolvableKeys;
	}

	public boolean hasTemplate() throws NodeException {
		return false;
	}

	public void setTarget(int target) {
		this.target = target;

		if (target == TARGET_PAGE) {
			targetClass = Page.class;
		} else if (target == TARGET_FILE || target == TARGET_IMAGE) {
			targetClass = File.class;
		} else if (target == TARGET_FOLDER) {
			targetClass = Folder.class;
		} else {
			NodeLogger.getLogger(getClass()).error("Invalid url-target [" + target + "].");
		}
	}

	/**
	 * Checks if the URL Parttype is mandatory and not empty
	 */
	public boolean isMandatoryAndNotFilledIn() throws NodeException {
		if (!isRequired()) {
			return false;
		}

		if (getInternal() == 0) {
			String url = getValueObject().getValueText();

			if (url == null) {
				return true;
			} else {
				return "".equals(url);
			}
		} else {
			if (getValueObject().getValueRef() <= 0) {
				return true;
			}
		}

		return false;
	}

	public String render(RenderResult result, String template) throws NodeException {
		super.render(result, template);
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		String url = "";

		if (getInternal() == 0) {

			// external link
			url = getValueObject().getValueText();

		} else if (getValueObject().getValueRef() > 0) {

			// internal link
			RenderUrl renderUrl = renderType.getRenderUrl(targetClass, new Integer(getValueObject().getValueRef()));

			renderUrl.setMode(RenderUrl.MODE_LINK);

			url = renderUrl.toString();
		}

		if (encoded) {
			url = encode(url);
		}

		return url;
	}

	/**
	 * encode a url as done in php parser. some extra magic is done since all %
	 * will be replaced by § - no idea why
	 * @param url to be encoded
	 * @return encoded url
	 */
	protected String encode(String url) {
		try {
			url = URLEncoder.encode(url, "utf8");
		} catch (UnsupportedEncodingException e) {
			logger.error("could not encode url - encoding is bogus", e);
		}
		url = url.replaceAll("%", "§");
		return url;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.AbstractPartType#get(java.lang.String)
	 */
	public Object get(String key) {
		// TODO: move this to javabean getters

		if ("isinternal".equals(key)) {
			return getInternal();
		}
		if ("target".equals(key)) {
			try {
				return getValueObject().getValueRef() > 0
						? TransactionManager.getCurrentTransaction().getObject(targetClass, new Integer(getValueObject().getValueRef()))
						: null;
			} catch (NodeException e) {
				logger.error("Error while resolving target of " + this, e);
			}
		}

		if ("internal".equals(key)) {
			if (getInternal() == 0) {
				return Boolean.FALSE;
			} else {
				return Boolean.TRUE;
			}
		}

		if ("externalurl".equals(key)) {
			return getInternal()  == 1 ? null : getValueObject().getValueText();
		}

		if ("nodeId".equals(key)) {
			return getNodeId();
		}

		if ("node".equals(key)) {
			try {
				return getNode();
			} catch (NodeException e) {
				logger.error("Error while getting node for " + this, e);
			}
		}

		// TODO: how to handle 'encoded' and obj-props for file?

		// compatiblity syntax hack
		Resolvable prop;

		try {
			prop = getPropResolver();
		} catch (NodeException e) {
			logger.error("Error while getting property resolver", e);
			return null;
		}
		if (prop != null) {
			return prop.get(key);
		}

		return null;
	}

	/**
	 * Get 1 if the url is internal, 0 if not
	 * @return 1 or 0
	 * @deprecated
	 * @see #getInternal()
	 */
	public int getIsinternal() {
		return getInternal();
	}

	/**
	 * Get 1 if the url is internal, 0 if not
	 * @return 1 or 0
	 */
	public int getInternal() {
		return getValueObject().getInfo() == 1 ? 1 : 0;
	}

	/**
	 * Get the target
	 * @return the target or null
	 */
	public NodeObject getTarget() {
		try {
			return getValueObject().getValueRef() > 0
					? TransactionManager.getCurrentTransaction().getObject(targetClass, new Integer(getValueObject().getValueRef()))
					: null;
		} catch (NodeException e) {
			logger.error("Error while getting target", e);
			return null;
		}
	}

	private Resolvable getPropResolver() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (getValueObject().getValueRef() == 0) {
			return null;
		}

		if (target == TARGET_PAGE && getValueObject().getInfo() == 1) {
			Page page = null;

			page = t.getObject(Page.class, new Integer(getValueObject().getValueRef()));

			// avoid nasty NPE
			if (page == null) {
				return null;
			}

			// check whether page is online, if not, return null (we will
			// not resolve properties from an offline page)
			if (!ObjectTransformer.getBoolean(page.get("online"), false)) {
				return null;
			}

			return page;
		}

		if (target == TARGET_FILE || target == TARGET_IMAGE) {
			File file = null;

			file = t.getObject(File.class, new Integer(getValueObject().getValueRef()));
			return file;
		}

		if (target == TARGET_FOLDER) {
			Folder folder = null;

			folder = t.getObject(Folder.class, new Integer(getValueObject().getValueRef()));
			return folder;
		}

		return null;
	}

	/**
	 * Get the target channel of the URL (if internal)
	 * @return target channel or null
	 * @throws NodeException
	 */
	public Node getNode() throws NodeException {
		if (getInternal() == 1) {
			Transaction t = TransactionManager.getCurrentTransaction();
			return t.getObject(Node.class, ObjectTransformer.getInt(getValueObject().getValueText(), 0), -1, false);
		} else {
			return null;
		}
	}

	/**
	 * Set the target channel for internal URLs
	 * @param node target channel (may be null)
	 * @throws NodeException
	 */
	public void setNode(Node node) throws NodeException {
		if (getInternal() == 1) {
			String channelId = "";
			if (node != null) {
				channelId = Integer.toString(node.getId());
			}
			getValueObject().setValueText(channelId);
		}
	}

	/**
	 * Get the node ID for internal URLs or 0
	 * @return node ID or 0
	 */
	public int getNodeId() {
		if (getInternal() == 1) {
			try {
				Node node = getNode();
				return node != null ? node.getId() : 0;
			} catch (NodeException e) {
				return 0;
			}
		} else {
			return 0;
		}
	}

	@Override
	public boolean hasSameContent(PartType other) {
		if (other instanceof UrlPartType) {
			UrlPartType urlPartType = (UrlPartType) other;
			return Objects.equals(getInternal(), urlPartType.getInternal())
					&& Objects.equals(getNodeId(), urlPartType.getNodeId())
					&& Objects.equals(getTarget(), urlPartType.getTarget())
					&& Objects.equals(getValueObject().getValueText(), other.getValueObject().getValueText());
		} else {
			return false;
		}
	}
}
