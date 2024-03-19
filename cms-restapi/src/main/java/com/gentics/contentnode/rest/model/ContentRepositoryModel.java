package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * ContentRepository
 */
@XmlRootElement
public class ContentRepositoryModel extends AbstractModel implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -4894890997455204917L;

	protected Integer id;

	protected String name;

	protected Type crType;

	protected String dbType;

	protected String username;

	protected String usernameProperty;

	protected String password;

	protected String passwordProperty;

	protected PasswordType passwordType;

	protected Boolean http2;

	protected String url;

	protected String urlProperty;

	protected String basepath;

	protected String basepathProperty;

	protected Boolean instantPublishing;

	protected Boolean languageInformation;

	protected Boolean permissionInformation;

	protected String permissionProperty;

	protected String defaultPermission;

	protected Boolean diffDelete;

	protected CRElasticsearchModel elasticsearch;

	protected Boolean projectPerNode;

	protected String version;

	protected Integer checkDate;

	protected Status checkStatus;

	protected String checkResult;

	protected Integer statusDate;

	protected Status dataStatus;

	protected String dataCheckResult;

	protected Boolean noFoldersIndex;

	protected Boolean noFilesIndex;

	protected Boolean noPagesIndex;

	protected Boolean noFormsIndex;

	/**
	 * Create empty instance
	 */
	public ContentRepositoryModel() {
	}

	/**
	 * Internal ID
	 * @return id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the internal ID
	 * @param id id
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * Name of the ContentRepository
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * @param name name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Type of the ContentRepository
	 * @return type
	 */
	public Type getCrType() {
		return crType;
	}

	/**
	 * Set the CR type
	 * @param crType type
	 */
	public void setCrType(Type crType) {
		this.crType = crType;
	}

	/**
	 * DB Type of the ContentRepository
	 * @return db type
	 */
	public String getDbType() {
		return dbType;
	}

	/**
	 * Set the db type
	 * @param dbType db type
	 */
	public void setDbType(String dbType) {
		this.dbType = dbType;
	}

	/**
	 * Username for accessing the ContentRepository.
	 * @return username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the username
	 * @param username username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Username property for accessing the ContentRepository. This can be set to a system property or environment variable in the format ${sys:property} or ${env:variable}.
	 * @return username property
	 */
	public String getUsernameProperty() {
		return usernameProperty;
	}

	/**
	 * Set the username property
	 * @param usernameProperty username property
	 */
	public void setUsernameProperty(String usernameProperty) {
		this.usernameProperty = usernameProperty;
	}

	/**
	 * Password for accessing the ContentRepository.
	 * @return password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set the password
	 * @param password password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Property, which will resolve to the password.
	 * This can be set to a system property or environment variable in the format ${sys:property} or ${env:variable}.
	 * @return
	 */
	public String getPasswordProperty() {
		return passwordProperty;
	}

	/**
	 * Set the password property
	 * @param passwordProperty
	 */
	public void setPasswordProperty(String passwordProperty) {
		this.passwordProperty = passwordProperty;
	}

	/**
	 * True when a HTTP/2 is used
	 * @return true for HTTP/2
	 */
	public Boolean getHttp2() {
		return http2;
	}

	/**
	 * Set whether HTTP/2 version should be used
	 * @param http2 true for HTTP/2
	 */
	public void setHttp2(Boolean http2) {
		this.http2 = http2;
	}

	/**
	 * URL for accessing the ContentRepository
	 * Type of password
	 * @return password type
	 */
	public PasswordType getPasswordType() {
		return passwordType;
	}

	/**
	 * Set the type of how the password is set
	 * @param passwordType type if the password
	 */
	public void setPasswordType(PasswordType passwordType) {
		this.passwordType = passwordType;
	}

	/**
	 * URL for accessing the ContentRepository.
	 * @return url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the URL
	 * @param url url
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * URL property for accessing the ContentRepository. This can be set to a system property or environment variable in the format ${sys:property} or ${env:variable}.
	 * @return url property
	 */
	public String getUrlProperty() {
		return urlProperty;
	}

	/**
	 * Set the URL property
	 * @param urlProperty url property
	 */
	public void setUrlProperty(String urlProperty) {
		this.urlProperty = urlProperty;
	}

	/**
	 * Basepath for filesystem attributes.
	 * @return basepath
	 */
	public String getBasepath() {
		return basepath;
	}

	/**
	 * Set the basepath
	 * @param basepath basepath
	 */
	public void setBasepath(String basepath) {
		this.basepath = basepath;
	}

	/**
	 * Basepath property for filesystem attributes. This can be set to a system property or environment variable in the format ${sys:property} or ${env:variable}.
	 * @return basepath property
	 */
	public String getBasepathProperty() {
		return basepathProperty;
	}

	/**
	 * Set the basepath property
	 * @param basepathProperty basepath property
	 */
	public void setBasepathProperty(String basepathProperty) {
		this.basepathProperty = basepathProperty;
	}

	/**
	 * Flag for instant publishing
	 * @return instant publishing flag
	 */
	public Boolean getInstantPublishing() {
		return instantPublishing;
	}

	/**
	 * Set the instant publishing flag
	 * @param instantPublishing instant publishing flag
	 */
	public void setInstantPublishing(Boolean instantPublishing) {
		this.instantPublishing = instantPublishing;
	}

	/**
	 * Flag for publishing language information
	 * @return language information flag
	 */
	public Boolean getLanguageInformation() {
		return languageInformation;
	}

	/**
	 * Set the language information flag
	 * @param languageInformation language information flag
	 */
	public void setLanguageInformation(Boolean languageInformation) {
		this.languageInformation = languageInformation;
	}

	/**
	 * Flag for publishing permission information
	 * @return permission information flag
	 */
	public Boolean getPermissionInformation() {
		return permissionInformation;
	}

	/**
	 * Set the permission information flag
	 * @param permissionInformation permission information flag
	 */
	public void setPermissionInformation(Boolean permissionInformation) {
		this.permissionInformation = permissionInformation;
	}

	/**
	 * Property containing the permission (role) information for Mesh CRs
	 * @return name of the property
	 */
	public String getPermissionProperty() {
		return permissionProperty;
	}

	/**
	 * Set the permission property
	 * @param permissionProperty name of the property
	 */
	public void setPermissionProperty(String permissionProperty) {
		this.permissionProperty = permissionProperty;
	}

	/**
	 * Default permission (role) to be set on objects in Mesh CRs
	 * @return default permission
	 */
	public String getDefaultPermission() {
		return defaultPermission;
	}

	/**
	 * Set the default permission
	 * @param defaultPermission default permission
	 */
	public void setDefaultPermission(String defaultPermission) {
		this.defaultPermission = defaultPermission;
	}

	/**
	 * Flag for differential deleting of superfluous objects
	 * @return differential delete flag
	 */
	public Boolean getDiffDelete() {
		return diffDelete;
	}

	/**
	 * Set flag for differential delete
	 * @param diffDelete differential delete flag
	 */
	public void setDiffDelete(Boolean diffDelete) {
		this.diffDelete = diffDelete;
	}

	/**
	 * Get the elasticsearch specific configuration of a Mesh CR
	 * @return elasticsearch config
	 */
	public CRElasticsearchModel getElasticsearch() {
		return elasticsearch;
	}

	/**
	 * Set the elasticsearch config
	 * @param elasticsearch config
	 */
	public void setElasticsearch(CRElasticsearchModel elasticsearch) {
		this.elasticsearch = elasticsearch;
	}

	/**
	 * Flag for publishing every node into its own project for Mesh contentrepositories
	 * @return true for project per node
	 */
	public Boolean getProjectPerNode() {
		return projectPerNode;
	}

	/**
	 * Set flag for project per node
	 * @param projectPerNode true for project per node
	 */
	public void setProjectPerNode(Boolean projectPerNode) {
		this.projectPerNode = projectPerNode;
	}

	/**
	 * Implementation version of the Mesh ContentRepository
	 * @return implementation version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Set the implementation version
	 * @param version implementation version
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Date of last check of structure
	 * @return structure check date
	 */
	public Integer getCheckDate() {
		return checkDate;
	}

	/**
	 * Set structure check date
	 * @param checkDate structure check date
	 */
	public void setCheckDate(Integer checkDate) {
		this.checkDate = checkDate;
	}

	/**
	 * Status of last structure check
	 * @return structure check status
	 */
	public Status getCheckStatus() {
		return checkStatus;
	}

	/**
	 * Set structure check status
	 * @param checkStatus structure check status
	 */
	public void setCheckStatus(Status checkStatus) {
		this.checkStatus = checkStatus;
	}

	/**
	 * Result of last structure check
	 * @return structure check result
	 */
	public String getCheckResult() {
		return checkResult;
	}

	/**
	 * Set structure check result
	 * @param checkResult structure check result
	 */
	public void setCheckResult(String checkResult) {
		this.checkResult = checkResult;
	}

	/**
	 * Date of data status (last publish process)
	 * @return data status date
	 */
	public Integer getStatusDate() {
		return statusDate;
	}

	/**
	 * Set data status date
	 * @param statusDate data status date
	 */
	public void setStatusDate(Integer statusDate) {
		this.statusDate = statusDate;
	}

	/**
	 * Status of last data check
	 * @return data check status
	 */
	public Status getDataStatus() {
		return dataStatus;
	}

	/**
	 * Set data check status
	 * @param dataStatus data check status
	 */
	public void setDataStatus(Status dataStatus) {
		this.dataStatus = dataStatus;
	}

	/**
	 * Result of last data check
	 * @return data check result
	 */
	public String getDataCheckResult() {
		return dataCheckResult;
	}

	/**
	 * Set data check result
	 * @param dataCheckResult data check result
	 */
	public void setDataCheckResult(String dataCheckResult) {
		this.dataCheckResult = dataCheckResult;
	}

	/**
	 * Get 'exclude folders from indexing' flag.
	 */
	public Boolean getNoFoldersIndex() {
		return noFoldersIndex;
	}

	/**
	 * Set 'exclude folders from indexing' flag.
	 * @param noFolderIndex
	 */
	public void setNoFoldersIndex(Boolean noFolderIndex) {
		this.noFoldersIndex = noFolderIndex;
	}

	/**
	 * Get 'exclude files from indexing' flag.
	 * @return
	 */
	public Boolean getNoFilesIndex() {
		return noFilesIndex;
	}

	/**
	 * Set 'exclude files from indexing' flag.
	 * @param noFilesIndex
	 */
	public void setNoFilesIndex(Boolean noFilesIndex) {
		this.noFilesIndex = noFilesIndex;
	}

	/**
	 * Get 'exclude pages from indexing' flag.
	 * @return
	 */
	public Boolean getNoPagesIndex() {
		return noPagesIndex;
	}

	/**
	 * Set 'exclude pages from indexing' flag.
	 * @param noPagesIndex
	 */
	public void setNoPagesIndex(Boolean noPagesIndex) {
		this.noPagesIndex = noPagesIndex;
	}

	/**
	 * Get 'exclude forms from indexing' flag.
	 * @return
	 */
	public Boolean getNoFormsIndex() {
		return noFormsIndex;
	}

	/**
	 * Set 'exclude forms from indexing' flag.
	 * @param noFormsIndex
	 */
	public void setNoFormsIndex(Boolean noFormsIndex) {
		this.noFormsIndex = noFormsIndex;
	}

	/**
	 * Possible ContentRepository types
	 */
	public static enum Type {
		/**
		 * Normal CR (SQL based, no multichannelling)
		 */
		cr,

		/**
		 * Multichannelling aware CR (SQL based)
		 */
		mccr,

		/**
		 * Mesh CR
		 */
		mesh
	}

	/**
	 * Possible values for how the password is set
	 */
	public static enum PasswordType {
		/**
		 * No password is set
		 */
		none,

		/**
		 * The password is set as value
		 */
		value,

		/**
		 * The password is set as property
		 */
		property
	}

	/**
	 * Possible Check Status values
	 */
	public static enum Status {
		/**
		 * Check was never done
		 */
		unchecked(-1),

		/**
		 * Check produced an error
		 */
		error(0),

		/**
		 * Check was ok
		 */
		ok(1),

		/**
		 * Check is currently running (in background)
		 */
		running(2),

		/**
		 * Check is queued to run in background
		 */
		queued(3);

		/**
		 * Code
		 */
		private int code;

		/**
		 * Create instance with code
		 * @param code code
		 */
		private Status(int code) {
			this.code = code;
		}

		/**
		 * Get the status code
		 * @return code
		 */
		public int code() {
			return code;
		}

		/**
		 * Transform status code into Status
		 * @param code code
		 * @return Status instance
		 */
		public static Status from(int code) {
			for (Status v : values()) {
				if (v.code == code) {
					return v;
				}
			}
			return unchecked;
		}
	}
}
