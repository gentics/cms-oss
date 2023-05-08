/*
 * @author herbert
 * @date 11.08.2006
 * @version $Id: VelocityToolsImp.java,v 1.1 2006-08-11 13:44:15 herbert Exp $
 */
package com.gentics.portalnode.formatter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import org.apache.velocity.tools.view.XMLToolboxManager;

import com.gentics.api.portalnode.imp.AbstractGenticsImp;
import com.gentics.api.portalnode.imp.ImpException;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * A simple Imp which wraps the functionality of the
 * Velocity Tools.
 * http://jakarta.apache.org/velocity/tools/
 *
 * @author herbert
 *
 */
public class VelocityToolsImp extends AbstractGenticsImp {
	public static final String CONFIG_PARAMETER = "configuration";
	NodeLogger logger;
	private XMLToolboxManager toolboxManager;
	private Map toolbox;

	public void init(String impId, Map parameters) throws ImpException {
		super.init(impId, parameters);
		logger = NodeLogger.getNodeLogger(this.getClass());
		String configFile = (String) parameters.get(CONFIG_PARAMETER);
		InputStream config = null;

		if (configFile != null) {
			configFile = StringUtils.resolveSystemProperties(configFile);
			try {
				config = new FileInputStream(configFile);
			} catch (FileNotFoundException e) {
				logger.error("Error while trying to load configFile from: {" + configFile + "} - trying to load from Portletapplication.", e);
			}
		}
		if (config == null) {
			config = getClass().getResourceAsStream("toolbox.xml");
		}
		this.toolboxManager = new XMLToolboxManager();
		try {
			toolboxManager.load(config);
			toolbox = toolboxManager.getToolbox(null);
		} catch (Exception e) {
			logger.error("Error while trying to load toolbox.", e);
		}
	}

	/**
	 * Returns the corresponding tool from the toolbox.
	 * 
	 * @param key name of the velocity tool as defined in toolbox.xml
	 * @return the specified Velocity Tool.
	 */
	public Object get(String key) {
		try {
			return toolbox.get(key);
		} catch (Exception e) {
			logger.error("Error while retrieving velocity tool {" + key + "}", e);
		}
		return null;
	}
}
