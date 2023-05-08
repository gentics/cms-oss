package com.gentics.contentnode.rest.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.rest.model.CmpCompatibility;
import com.gentics.contentnode.rest.model.CmpProduct;
import com.gentics.contentnode.rest.model.CmpVersionInfo;
import com.gentics.contentnode.version.CmpProductVersion;
import com.gentics.contentnode.version.CmpVersionRequirement;
import com.gentics.contentnode.version.ProductVersionRange;
import com.gentics.contentnode.version.PortalVersionResponse;
import com.gentics.lib.log.NodeLogger;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Map;

/**
 * Utility methods for determining CMP component versions.
 */
public class CmpVersionUtils {

	private static final NodeLogger logger = NodeLogger.getNodeLogger(CmpVersionUtils.class);

	/**
	 * Get the product version from the specified URL.
	 *
	 * @param versionUrl The URL to retrieve version information from
	 * @return The version information returned from the given URL
	 */
	public static PortalVersionResponse getPortalVersion(String versionUrl) {
		HttpClient client = new HttpClient();
		ObjectMapper mapper = new ObjectMapper();
		GetMethod getVersionRequest = new GetMethod(versionUrl);

		try {
			int status = client.executeMethod(getVersionRequest);

			if (status == HttpResponseStatus.OK.code()) {
				return mapper.readValue(getVersionRequest.getResponseBodyAsStream(), PortalVersionResponse.class);
			}
		} catch (Exception e) {
			// We log the problem as a warning, but don't rethrow the exception
			// because one unreachable Mesh or portal instance should not cancel
			// the complete request to /rest/admin/version.
			logger.warn("Could not get version information from " + versionUrl, e);
		}

		return null;
	}

	/**
	 * Create an instance of {@link CmpVersionInfo} from the given Mesh and
	 * portal version.
	 *
	 * <p>
	 *     All information available in {@code meshVersion} and {@code portalVersion}
	 *     will be added to the {@code CmpVersionInfo}.
	 * </p>
	 *
	 * <p>
	 *     If any of the parameters is {@code null} the CMP compatibility will
	 *     be set to {@code UNKNOWN}.
	 * </p>
	 *
	 * <p>
	 *     When the CMP version requiremens as well as the Mesh version and the
	 *     portal version are available, the CMP compatibility will be set
	 *     accordingly after a check if the versions match the required
	 *     version ranges.
	 * </p>
	 *
	 * @param req The version requirements for the CMP version
	 * @param meshVersion The version response from Mesh
	 * @param portalVersionResponse The version response from the portal
	 * @return An instance of {@code CmpVersionInfo} with all the given
	 * 		information, and an indication whether this combination is a
	 * 		supported CMP version
	 */
	public static CmpVersionInfo createVersionInfo(CmpVersionRequirement req, CmpProductVersion meshVersion, PortalVersionResponse portalVersionResponse) {
		CmpVersionInfo versionInfo = new CmpVersionInfo();

		if (meshVersion != null) {
			versionInfo.setMeshVersion(meshVersion.toString());
		}

		if (portalVersionResponse != null) {
			versionInfo.setPortalType(portalVersionResponse.getProductName())
				.setPortalVersion(portalVersionResponse.getProductVersion());
		}

		// 1. No request = no compatibility
		if (req == null || req.getProductVersions() == null) {
			return versionInfo.setCompatibility(CmpCompatibility.UNKNOWN);
		}

		Map<CmpProduct, ProductVersionRange> productVersions = req.getProductVersions();
		boolean compatible = true;

		// 2. Check Mesh existence & compatibility (mandatory)
		if (meshVersion != null) {
			compatible &= meshVersion.inRange(productVersions.get(CmpProduct.MESH));
		} else {
			return versionInfo.setCompatibility(CmpCompatibility.UNKNOWN);
		}

		// 3. Check Portal existence & compatibility (optional)
		if (compatible && portalVersionResponse != null) {
			CmpProductVersion portalVersion = new CmpProductVersion(portalVersionResponse.getProductVersion());
			compatible &= portalVersion.inRange(productVersions.get(CmpProduct.forName(portalVersionResponse.getProductName())));
		}

		return versionInfo.setCompatibility(compatible ? CmpCompatibility.SUPPORTED : CmpCompatibility.NOT_SUPPORTED);
	}
}
