package com.gentics.contentnode.object;

import java.io.Serializable;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.lib.etc.StringUtils;

/**
 * Created by IntelliJ IDEA. <p/>User: wpsadmin <p/>Date: 16.06.2003 <p/> Time:
 * 11:01:06 <p/>To change this template use Options | File Templates. TODO
 * complete rewrite
 */
public class Icon implements Serializable {

	/**
	 * generated serial version UID
	 */
	private static final long serialVersionUID = 804169944244044990L;

	private String name;

	private String title;

	private String module;

	public final static String STAG_PREFIX_PARAM = "stag_prefix";

	public Icon(String module, String name, String title) {
		this.module = module;
		this.name = name;
		this.title = title;
	}

	public String getURL() throws NodeException {
		StringBuffer url = new StringBuffer(100);

		// prepend STAG_PREFIX
		String stagPrefix = TransactionManager.getCurrentTransaction().getRenderType().getPreferences().getProperty(STAG_PREFIX_PARAM);

		url.append(stagPrefix);
		url.append("?do=11&module=");
		if (StringUtils.isEmpty(name)) {
			url.append("system");
		} else {
			url.append(module);
		}
		url.append("&img=");
		if (!StringUtils.isEmpty(name)) {
			url.append(this.name);
		} else {
			url.append("null.gif");
		}
		return url.toString();
	}

	public String getHTML(int border, String title, String name, String width, String height, String align,
			String style, String clazz) throws NodeException {
		StringBuffer ret = new StringBuffer(100);

		if (title == null) {
			title = this.title;
		}
		ret.append("<img src=\"").append(getURL()).append("\" border=\"").append(border).append("\"");
		ret.append(" alt=\"").append(title != null ? StringUtils.escapeXML(title) : "").append("\"");
		StringUtils.appendAttribute(ret, "title", title);
		StringUtils.appendAttribute(ret, "name", name);
		StringUtils.appendAttribute(ret, "width", width);
		StringUtils.appendAttribute(ret, "height", height);
		StringUtils.appendAttribute(ret, "align", align);
		StringUtils.appendAttribute(ret, "style", style);
		StringUtils.appendAttribute(ret, "class", clazz);

		ret.append(">");

		return ret.toString();
	}
}
