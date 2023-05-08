package com.gentics.contentnode.rest.model.response;

import com.gentics.contentnode.rest.model.CmpVersionInfo;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

/**
 * Response for a request asking for the version of the REST API on the server.
 */
@XmlRootElement
public class VersionResponse extends GenericResponse {
	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The current server version
	 */
	private String version;

	/**
	 * The respective CMP version for this CMS version.
	 */
	private String cmpVersion;

	/**
	 * Per node information about CMP component versions.
	 */
	private Map<String, CmpVersionInfo> nodeInfo;

	/**
	 * Empty constructor needed by JAXB
	 */
	public VersionResponse() {}

	/**
	 * Creates a VersionResponse with the provided single message, the ResponseInfo and the version as String
	 *
	 * @param message
	 *            The messages that should be displayed to the user
	 * @param responseInfo
	 *            ResponseInfo with the status of the response
	 */
	public VersionResponse(Message message, ResponseInfo responseInfo, String version) {
		super(message, responseInfo);
		this.version = version;
	}

	/**
	 * Current server version
	 * @documentationExample 5.38.0
	 * @return The current server version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param version The current server version
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Respective CMP version for this CMS version
	 * @documentationExample 7.9
	 * @return The respective CMP version for this CMS version
	 */
	public String getCmpVersion() {
		return cmpVersion;
	}

	/**
	 * @param cmpVersion The respective CMP version for this CMS version
	 */
	public void setCmpVersion(String cmpVersion) {
		this.cmpVersion = cmpVersion;
	}

	/**
	 * Get the map containing CMP component version information for each node.
	 *
	 * @return The map containing CMP component version information for each node
	 */
	public Map<String, CmpVersionInfo> getNodeInfo() {
		return nodeInfo;
	}

	/**
	 * Set the per node CMP component version information.
	 *
	 * @param nodeInfo The CMP component version information
	 */
	public void setNodeInfo(Map<String, CmpVersionInfo> nodeInfo) {
		this.nodeInfo = nodeInfo;
	}
}
