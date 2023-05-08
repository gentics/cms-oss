package com.gentics.contentnode.update;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.version.Main;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Implementation for AutoUpdate
 */
public class AutoUpdate {
	/**
	 * Client Timeout
	 */
	protected int timeout;

	/**
	 * List of available update versions
	 */
	protected List<CMSVersion> availableVersions;

	/**
	 * Optional latest available update version
	 */
	protected Optional<CMSVersion> latestVersion;

	/**
	 * Get configured start version to consider for update
	 * @return start version
	 */
	protected static CMSVersion getStartVersion() {
		Map<String, Object> startVersionMap = NodeConfigRuntimeConfiguration.getPreferences().getPropertyMap("selfupdate.startVersion");
		if (startVersionMap != null) {
			return new CMSVersion(ObjectTransformer.getInt(startVersionMap.get("major"), 0), ObjectTransformer.getInt(startVersionMap.get("minor"), 0),
					ObjectTransformer.getInt(startVersionMap.get("patch"), 0));
		} else {
			return new CMSVersion();
		}
	}

	/**
	 * Get current version
	 * @return current version
	 */
	protected static CMSVersion getCurrentVersion() {
		return new CMSVersion(Main.getImplementationVersion());
	}

	/**
	 * Get URL of the update site
	 * @return URL
	 */
	public static String getUpdatesiteUrl() {
		Map<String, Object> updateSiteSettings = NodeConfigRuntimeConfiguration.getPreferences().getPropertyMap("selfupdate.baseUrl");
		String prefix = ObjectTransformer.getString(updateSiteSettings.get("prefix"), "");
		String base = ObjectTransformer.getString(updateSiteSettings.get("baseurl"), "");

		return String.format("%s%smaven-metadata.xml?version=%s", prefix, base, getCurrentVersion().toString());
	}

	/**
	 * Get Update versions from update site
	 * @param timeout timeout in seconds
	 * @return list of available update versions
	 * @throws NodeException
	 */
	public static List<CMSVersion> getVersionsFromUpdatesite(int timeout) throws NodeException {
		try {
			XmlMapper mapper = new XmlMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			HttpClientParams params = new HttpClientParams();
			params.setConnectionManagerTimeout(timeout * 1000L);
			params.setSoTimeout(timeout * 1000);
			HttpClient httpClient = new HttpClient(params);
			GetMethod getMethod = new GetMethod(getUpdatesiteUrl());

			int responseStatus = httpClient.executeMethod(getMethod);
			if (responseStatus == 404) {
				return Collections.emptyList();
			}
			if (responseStatus != 200) {
				throw new NodeException(String.format("Request to get mavenmetadata returned response status %d", responseStatus));
			}

			MavenMetadata mavenMetadata = mapper.readValue(getMethod.getResponseBodyAsString(), MavenMetadata.class);

			CMSVersion startVersion = getStartVersion();
			CMSVersion currentVersion = getCurrentVersion();

			// beware of NPE
			if (mavenMetadata.getVersioning() == null) {
				return Collections.emptyList();
			}
			if (mavenMetadata.getVersioning().getVersions() == null) {
				return Collections.emptyList();
			}
			Stream<CMSVersion> stream = mavenMetadata.getVersioning().getVersions().stream();
			// remove versions smaller or equal than the start version and versions smaller or equal than the current version
			stream = stream.filter(version -> version.isGreater(startVersion)).filter(version -> version.isGreater(currentVersion));

			boolean latestHotfixOnly = ObjectTransformer
					.getBoolean(NodeConfigRuntimeConfiguration.getPreferences().getProperty("selfupdate.latestHotfixOnly"), false);
			if (latestHotfixOnly) {
				// reduce to latest hotfix only
				stream = stream.collect(Collectors.groupingBy(CMSVersion::getMajorMinor)).values().stream()
						.map(list -> list.stream().max(CMSVersion::compareTo).get());
			}

			return stream.sorted().collect(Collectors.toList());
		} catch (IOException e) {
			throw new NodeException("Error while reading maven-metadata from update site", e);
		}
	}

	/**
	 * Get the latest update version from the update site
	 * @param timeout timeout in seconds
	 * @return optiona version
	 * @throws NodeException
	 */
	public static Optional<CMSVersion> getLatestUpdateFromUpdatesite(int timeout) throws NodeException {
		return getVersionsFromUpdatesite(timeout).stream().max(CMSVersion::compareTo);
	}

	/**
	 * Create instance with default timeout (60 seconds)
	 * @throws NodeException
	 */
	public AutoUpdate() throws NodeException {
		this(60);
	}

	/**
	 * Create instance with given timeout
	 * @param timeout timeout in seconds
	 * @throws NodeException
	 */
	public AutoUpdate(int timeout) throws NodeException {
		this.timeout = timeout;
		this.availableVersions = getVersionsFromUpdatesite(timeout);
		this.latestVersion = this.availableVersions.stream().max(CMSVersion::compareTo);
	}

	/**
	 * Get latest update version
	 * @return optional version
	 */
	public Optional<CMSVersion> getLatestVersion() {
		return latestVersion;
	}

	/**
	 * Get list of available update versions
	 * @return list of versions
	 */
	public List<CMSVersion> getAvailableVersions() {
		return availableVersions;
	}
}
