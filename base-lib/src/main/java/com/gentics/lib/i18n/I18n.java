package com.gentics.lib.i18n;

/**
 * created at Dec 9, 2003
 * @author Erwin Mascher (e.mascher@gentics.com) HM: For more Info see
 *         http://www.i18nfaq.com/java.html
 * @deprecated use
 * @link{I18nString} instead.
 */
public class I18n {
	public static String s(String meta) {
		return meta;
	}

	public static String s(String meta, int type, String param) {
		return s(meta, param);
	}

	public static String s(String meta, int type, String[] params) {
		return s(meta, params);
	}

	public static String s(String meta, String param) {
		if (param == null) {
			return meta;
		}
		return meta.replaceAll("\\{0\\}", param);
	}

	public static String s(String meta, String[] params) {
		for (int i = 0; i < params.length; i++) {
			String param = params[i];

			meta = meta.replaceAll("\\{" + i + "\\}", param);
		}
		return meta;
	}
}
