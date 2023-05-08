/*
 * @author norbert
 * @date 13.03.2007
 * @version $Id: ContentNodeHelper.java,v 1.5.2.1 2011-02-10 13:43:42 tobiassteiner Exp $
 */
package com.gentics.contentnode.etc;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserLanguage;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.lib.etc.StringUtils;

/**
 * Static Helper class for Content.Node specific implementations
 * 
 * TODO move the browser-type checks and browser-capabilitiy checks to own java.node browser helper class.
 */
public class ContentNodeHelper {
    
	public static final int BROWSER_IE = 1;
    
	public static final int BROWSER_FIREFOX = 2;
    
	public static final int BROWSER_OPERA = 3;
    
	public static final int BROWSER_MOZILLA = 4;
    
	public static final int BROWSER_OTHER = 5;

	public static final int BROWSER_IE_TRIDENT = 6;

	/**
	 * Thread local settings
	 */
	protected final static ThreadLocal<ThreadLocalSettings> localSettings = ThreadLocal.withInitial(() -> new ThreadLocalSettings());

	// must have the same order as browserPattern
	private final static int[] browserTypes = { BROWSER_IE, BROWSER_IE_TRIDENT, BROWSER_FIREFOX, BROWSER_OPERA, BROWSER_MOZILLA };
    
	// order is important here (firefox must be before mozilla)!
	private final static Pattern[] browserPattern = { 
		Pattern.compile(".*MSIE\\s*([0-9]*)\\.([0-9]*).*"), // old IEs
		Pattern.compile(".*Trident/([0-9]*)\\.([0-9]*).*"), // IE, starting with IE11
		Pattern.compile(".*Firefox/([0-9]*)\\.([0-9]*).*"), // Firefox
		Pattern.compile(".*Opera/([0-9]*)\\.([0-9]*).*"),   // Opera
		Pattern.compile(".*Mozilla/([0-9]*)\\.([0-9]*).*")  // Mozilla
	};

	/**
	 * Create an image tag
	 * @param image url (may be null)
	 * @param border border
	 * @param alt alt text (may be null)
	 * @param name name (may be null)
	 * @param width width (may be null)
	 * @param height height (may be null)
	 * @param align align (may be null)
	 * @param style style (may be null)
	 * @param clazz class (may be null)
	 * @return the image tag
	 */
	public static String getImage(String icon, int border, String alt, String name,
			String width, String height, String align, String style, String clazz) {
		StringBuffer imageBuffer = new StringBuffer();

		if (StringUtils.isEmpty(icon)) {
			// TODO prepend the $STAG_PREFIX (wherever we get it)
			icon = "?do=11&module=system&img=null.gif";
		} else {
			icon = getIconURL(icon);
		}

		imageBuffer.append("<img src=\"").append(icon).append("\" border=\"").append(border).append("\"");
		imageBuffer.append(" alt=\"").append(alt != null ? StringUtils.escapeXML(alt) : "").append("\"");
		StringUtils.appendAttribute(imageBuffer, "title", alt);
		StringUtils.appendAttribute(imageBuffer, "name", name);
		StringUtils.appendAttribute(imageBuffer, "width", width);
		StringUtils.appendAttribute(imageBuffer, "height", height);
		StringUtils.appendAttribute(imageBuffer, "align", align);
		StringUtils.appendAttribute(imageBuffer, "style", style);
		StringUtils.appendAttribute(imageBuffer, "class", clazz);

		imageBuffer.append(">");

		return imageBuffer.toString();
	}

	public static String getImagePath() {
		// TODO prepend STAG_PREFIX
		return "?do=11&module=content&img=";
	}

	public static String getIconURL(String icon) {
		if (!StringUtils.isEmpty(icon)) {
			return getImagePath() + "constr/" + icon;
		} else {
			return getImagePath() + "tag.gif";
		}
	}

	/**
	 * Get the local settings object (create a new one if none existent)
	 * @return local settings object
	 */
	protected static ThreadLocalSettings getSettings() {
		return localSettings.get();
	}

	/**
	 * Set the current httpUserAgent (of the original request)
	 * @param httpUserAgent http useragent
	 */
	public static void setHttpUserAgent(String httpUserAgent) {
		getSettings().setHttpUserAgent(httpUserAgent);
	}

	/**
	 * Get the current httpUserAgent (of the original request)
	 * @return the user agent
	 * @throws NodeException when no user agent was set
	 */
	public static String getHttpUserAgent() throws NodeException {
		String httpUserAgent = getSettings().getHttpUserAgent();

		if (httpUserAgent == null) {
			throw new NodeException("httpUserAgent not set");
		} else {
			return httpUserAgent;
		}
	}

	/**
	 * Set language id
	 * @param languageId language id
	 */
	public static void setLanguageId(int languageId) {
		getSettings().setLanguageId(languageId);
	}

	/**
	 * Get the current language id
	 * @return current language id
	 * @throws NodeException if the language id not set
	 */
	public static int getLanguageId() throws NodeException {
		int languageId = getSettings().getLanguageId();

		if (languageId < 0) {
			throw new NodeException("languageid not set");
		} else {
			return languageId;
		}
	}

	/**
	 * Get the current language id or the default, if current is set to -1
	 * @param defaultLanguageId default language id
	 * @return current or default language id
	 */
	public static int getLanguageId(int defaultLanguageId) {
		int languageId = getSettings().getLanguageId();
		if (languageId < 0) {
			return defaultLanguageId;
		} else {
			return languageId;
		}
	}

	/**
	 * The language ID returned by {@link #getLanguageId()} isn't very useful
	 * by itself, since it is merely a reference to a {@link UserLanguage}
	 * instance.
	 * @return the {@link Locale} of the {@link UserLanguage} for the given ID.
	 */
	public static Locale getLocaleForLanguageId(int languageId, Transaction transaction) throws NodeException {
		//Fallback to standard language
		if (languageId < 1 || languageId > 2) {
			languageId = 1;
		}
		UserLanguage userLang = (UserLanguage) transaction.getObject(UserLanguage.class, languageId);

		return userLang.getLocale();
	}

	/**
	 * Check whether the browser is IE 5.5 or better
	 * @return true for IE 5.5 or better, false if not
	 * @throws NodeException
	 */
	public static boolean isIE55orBetter() {
		ThreadLocalSettings settings = getSettings();
        
		if (settings.getBrowser() != BROWSER_IE) {
			return false;
		}
        
		return settings.getBrowserMajorVersion() > 5 || (settings.getBrowserMajorVersion() == 5 && settings.getBrowserMinorVersion() >= 5);
	}

	public static boolean isCapableOfContextmenu() {
		ThreadLocalSettings settings = getSettings();
        
		switch (settings.getBrowser()) {
		case BROWSER_IE:
		case BROWSER_MOZILLA:
		case BROWSER_FIREFOX:
			return true;

		case BROWSER_OPERA:
			return false;

		default:
			return false;
		}
	}
    
	public static boolean isCapableOfContenteditable() {
		ThreadLocalSettings settings = getSettings();

		switch (settings.getBrowser()) {
		case BROWSER_IE:
			return isIE55orBetter();

		case BROWSER_FIREFOX:
			return settings.getBrowserMajorVersion() >= 3;

		case BROWSER_MOZILLA:
		case BROWSER_OPERA:
			return false;

		default:
			return false;
		}
	}
    
	/**
	 * Threadlocal settings (useragent, language, ...)
	 */
	private static class ThreadLocalSettings {

		/**
		 * user agent of the current request
		 */
		private String httpUserAgent = null;

		/**
		 * language of the current request
		 */
		private int languageId = -1;

		private int browser = -1;
        
		private int browserMajor;
        
		private int browserMinor;

		private Session session;

		/**
		 * get the user agent
		 * @return user agent
		 */
		public String getHttpUserAgent() {
			return httpUserAgent;
		}

		/**
		 * Set a new user agent
		 * @param httpUserAgent user agent
		 */
		public void setHttpUserAgent(String httpUserAgent) {
			this.browser = -1;
			this.httpUserAgent = httpUserAgent;
		}

		/**
		 * Get the current language id
		 * @return current language id
		 */
		public int getLanguageId() {
			return languageId;
		}

		/**
		 * Set a new language id
		 * @param languageId new language id
		 */
		public void setLanguageId(int languageId) {
			this.languageId = languageId;
		}

		public int getBrowser() {
			if (browser == -1) {
				initBrowser();
			}
			return browser;
		}

		public int getBrowserMajorVersion() {
			if (browser == -1) {
				initBrowser();
			}
			return browserMajor;
		}
        
		public int getBrowserMinorVersion() {
			if (browser == -1) {
				initBrowser();
			}
			return browserMinor;
		}

		public Session getSession() {
			return session;
		}

		public void setSession(Session session) {
			this.session = session;
			if (session != null) {
				this.languageId = session.getLanguageId();
			}
		}

		private void initBrowser() {
			if (httpUserAgent == null) {
				browser = BROWSER_OTHER;
				return;
			}
            
			browser = BROWSER_OTHER;
            
			Matcher m;

			for (int i = 0; i < browserTypes.length; i++) {
				m = browserPattern[i].matcher(httpUserAgent);
				if (m.matches()) {
					browser = browserTypes[i];
					browserMajor = Integer.parseInt(m.group(1));
					browserMinor = Integer.parseInt(m.group(2));

					// for some reason, IE11 has TRIDENT/7.0 (and IE10 TRIDENT/6.0 and so on)
					if (browser == BROWSER_IE_TRIDENT) {
						browser = BROWSER_IE;
						browserMajor += 4;
					}

					break;
				}
			}
            
		}
        
		/**
		 * Create an instance of the settings
		 */
		public ThreadLocalSettings() {}
	}
    
	/**
	 * Check if superedit/liveedit is enabled for a given object type. This does all the feature/browser-checks.
	 * The rendered root object type is retrieved from the rendertype of the current transaction.
	 *   
	 * @return true if superedit is enabled and supported.
	 * @throws NodeException
	 */
	public static boolean isSupereditEnabled() throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		if (renderType == null) {
			return false;
		}
		StackResolvable rootObject = renderType.getRenderedRootObject();

		if (rootObject == null) {
			return false;
		}
        
		int objType = 0;

		if (rootObject instanceof Page) {
			objType = Page.TYPE_PAGE;
		} else if (rootObject instanceof Template) {
			objType = Template.TYPE_TEMPLATE;
		}
        
		return isSupereditEnabled(objType, false);
	}
    
	/**
	 * Check if superedit/liveedit is enabled for a given object type. This does all the feature/browser-checks.
	 * @param objType type-id of the object to edit (page or template).
	 * @return true if superedit is enabled and supported.
	 * @throws NodeException
	 */
	public static boolean isSupereditEnabled(int objType) throws NodeException {
		return isSupereditEnabled(objType, false);
	}

	/**
	 * Check if superedit/liveedit is enabled for a given object type. This does all the feature/browser-checks.
	 * @param objType type-id of the object to edit (page or template).
	 * @param serverOnly check if feature is enabled on the server, do not check for client support.
	 * @return true if superedit is enabled and supported.
	 * @throws NodeException
	 */
	public static boolean isSupereditEnabled(int objType, boolean serverOnly) throws NodeException {

		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		if (renderType == null) {
			return false;
		}
        
		// first check server settings
		if (objType == Page.TYPE_PAGE) {
			if (!renderType.getPreferences().getFeature("superedit_page_193912")) {
				return false;
			}
		} else if (objType == Template.TYPE_TEMPLATE) {
			if (!renderType.getPreferences().getFeature("superedit_template_193912")) {
				return false;
			}            
		} else {
			return false;
		}
        
		if (serverOnly) {
			return true;
		}
        
		// now check client/user dependent settings
		if (!isCapableOfContenteditable()) {
			return false;
		}
        
		// check if feature is enabled for firefox
		ThreadLocalSettings settings = getSettings();
        
		if (settings.getBrowser() == BROWSER_FIREFOX) {
			if (!renderType.getPreferences().getFeature("firefox_superedit_193912")) {
				return false;
			}
		}
        
		// TODO optional personal user settings check, additional type/feature for template-source-edit
        
		// TODO cache result in threadlocal for current browser+user ?  
        
		return true;
	}

	/**
	 * Get the current session
	 * @return session
	 */
	public static Session getSession() {
		return getSettings().getSession();
	}

	/**
	 * Set the current session
	 * @param session session
	 */
	public static void setSession(Session session) {
		getSettings().setSession(session);
		if (session != null) {
			setLanguageId(session.getLanguageId());
		}
	}

	/**
	 * Create a transaction with the current session
	 * @return transaction with current session
	 * @throws NodeException
	 */
	public static Trx trx() throws NodeException {
		Session session = getSession();
		if (session != null) {
			return new Trx(session, true);
		} else {
			return new Trx();
		}
	}

	/**
	 * Create a transaction with the given session (which may be null)
	 * @param session session
	 * @return transaction with session
	 * @throws NodeException
	 */
	public static Trx trx(Session session) throws NodeException {
		if (session != null) {
			return new Trx(session, true);
		} else {
			return new Trx();
		}
	}
}
