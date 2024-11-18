package com.gentics.contentnode.publish.mesh;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.time.Instant;
import java.util.Set;

public class LicenseInfoModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("The encoded license key.")
	private String licenseKey;

	@JsonProperty(required = true)
	@JsonPropertyDescription("License status.")
	private String status;

	@JsonProperty(required = false)
	@JsonPropertyDescription("License features.")
	private Set<String> features;

	@JsonProperty(required = false)
	@JsonPropertyDescription("License issued at.")
	private String issuedAt;

	@JsonProperty(required = false)
	@JsonPropertyDescription("License expires at.")
	private String expiresAt;

	@JsonProperty(required = false)
	@JsonPropertyDescription("License issuer.")
	private String issuer;

	@JsonProperty(required = false)
	@JsonPropertyDescription("License subject.")
	private String subject;

	public LicenseInfoModel setLicenseKey(String key) {
		this.licenseKey = key;
		return this;
	}

	public LicenseInfoModel setStatus(String name) {
		this.status = name;
		return this;
	}

	public LicenseInfoModel setFeatures(Set<String> features) {
		this.features = features;
		return this;
	}

	public String getLicenseKey() {
		return licenseKey;
	}

	public String getStatus() {
		return status;
	}

	public Set<String> getFeatures() {
		return features;
	}

	public String getIssuedAt() {
		return issuedAt;
	}

	public LicenseInfoModel setIssuedAt(String issuedAt) {
		this.issuedAt = issuedAt;
		return this;
	}

	public String getExpiresAt() {
		return expiresAt;
	}

	public LicenseInfoModel setExpiresAt(String expiresAt) {
		this.expiresAt = expiresAt;
		return this;
	}

	public String getIssuer() {
		return issuer;
	}

	public LicenseInfoModel setIssuer(String issuer) {
		this.issuer = issuer;
		return this;
	}

	public String getSubject() {
		return subject;
	}

	public LicenseInfoModel setSubject(String subject) {
		this.subject = subject;
		return this;
	}
}

