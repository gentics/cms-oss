/*
 * @author tobiassteiner
 * @date Jan 22, 2011
 * @version $Id: MiscUtils.java,v 1.1.2.1 2011-02-10 13:43:36 tobiassteiner Exp $
 */
package com.gentics.contentnode.rest.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.fum.FUMRequest;
import com.gentics.contentnode.rest.model.fum.FUMResponseStatus;
import com.gentics.contentnode.rest.model.fum.FUMResult;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.util.FileUtil;

public class FileUploadManipulatorFileSave {
	
	/**
	 * Key to fetch the cn_local_server string from the configuration.
	 */
	private static final String CN_LOCAL_SERVER_KEY = "cn_local_server";
	
	/**
	 * Key to fetch the portletapp_prefix string from the configuration.
	 */
	private static final String PORTLETAPP_PREFIX_INTERNAL = "portletapp_prefix_internal";
	
	/**
	 * Parameter Key, that is used to fetch the FILEUPLOADMANIPULATOR URL.
	 */
	private static final String FILEUPLOADMANIPULATOR_CONFIGURATION_URL_PARAMETER = "fileupload_manipulator_url";
    
	/**
	 * Parameter Key, that is used to fetch the FILEUPLOADMANIPULATOR OPTIONS.
	 */
	private static final String FILEUPLOADMANIPULATOR_CONFIGURATION_OPTIONS_PARAMETER = "fileupload_manipulator_options";

	/**
	 * The prefix of the tempfile
	 */
	private static final String FUM_TEMPFILE_PREFIX = "fileupload";

	/**
	 * The suffix of the tempfile
	 */
	private static final String FUM_TEMPFILE_SUFFIX = "fum";
	
	/**
	 * The default timeout when tempfiles are deleted
	 */
	protected static final int FUM_TEMPFILE_TIMEOUT = 86400000; //1 day

	/**
	 * This method will call the FileUploadManipulator (FUM) to perform its actions on the file.
	 * 
	 * The FUM will only be called if the FILEUPLOAD_MANIPULATOR_URL is configured.
	 * 
	 * The Stream of the ContentNodeFile Object will be either set to the source stream (when the FUM is not called), or to the temp file (if the file has been accepted as is),
	 * or to the manipulated response of the FUM.
	 * 
	 * @param t Current Transaction.
	 * @param prefs Current NodePreferences
	 * @param fileUploadManipulatorURL URL where the FUM can be reached
	 * @param fileUploadManipulatorOptions Configured options for the FUM
	 * @param file ContentNodeFile Object
	 * @param languageInfo Language information
	 * @param fileStream The stream containing the binary data
	 * @return String returns a string with the temp filename of the file that the id of the object should be written to. This should be used in handleObejctCreated
	 * @throws NodeException A NodeException is being thrown in case of an error. An error message will be attached.
	 */
	public static Message handleFileUploadManipulator(Transaction t, NodePreferences prefs, com.gentics.contentnode.object.File file, InputStream fileStream) throws NodeException {
		Message message = null;
		String languageInfo = ContentNodeHelper.getLocaleForLanguageId(t.getSession().getLanguageId(), t).getLanguage();
		String fileUploadManipulatorURL = prefs.getProperty(FILEUPLOADMANIPULATOR_CONFIGURATION_URL_PARAMETER);
		Map<?, ?> fileUploadManipulatorOptions = prefs.getPropertyMap(FILEUPLOADMANIPULATOR_CONFIGURATION_OPTIONS_PARAMETER);
		String tmpPath = ConfigurationValue.TMP_PATH.get();
		File tempFile = null;
		boolean postponed = false;

		if (!ObjectTransformer.isEmpty(fileUploadManipulatorURL)) {

			try {
				ObjectMapper mapper = new ObjectMapper();
				tempFile = createTempFile(tmpPath, fileStream);

				// Prepare the file for saving
				FileInputStream fileInputStream = null;

				try {
					fileInputStream = new FileInputStream(tempFile);
					file.setFileStream(fileInputStream);
					// Save the file, so we can pass a fileId to the FUM script
					file.save();
				} finally {
					fileInputStream.close();
				}

				TransactionManager.execute(() -> ActionLogger.logCmd(ActionLogger.FUM_START, com.gentics.contentnode.object.File.TYPE_FILE, file.getId(),
						file.getFolder().getId(), file.getName()));
				FUMSocketOptions socketOptions = new FUMSocketOptions(prefs);

				HttpClient client = getHttpClient(socketOptions.getSocketTimeout(), socketOptions.getConnectionTimeout(), socketOptions.getConnectionRetry());

				PostMethod post = new PostMethod(fileUploadManipulatorURL);

				FUMRequest request = createFUMRequestObject(prefs, file, languageInfo, fileUploadManipulatorOptions, tempFile);

				post.setRequestEntity(new StringRequestEntity(mapper.writeValueAsString(request), "application/json", "UTF-8"));

				client.executeMethod(post);

				int statusCode = post.getStatusCode();

				if (statusCode != HttpStatus.SC_OK) {
					String msg = "Request to the File Upload Manipulator was not successful." + " Got HTTP status code: " + statusCode
							+ ". Check your configuration!";
					I18nString i18nMessage = new CNI18nString("rest.file.upload.fum_failure");

					TransactionManager.execute(() -> ActionLogger.logCmd(ActionLogger.FUM_ERROR, com.gentics.contentnode.object.File.TYPE_FILE, file.getId(),
							file.getFolder().getId(), file.getName() + ": HTTP Status Code " + statusCode));
					throw new NodeException(i18nMessage.toString(), new Exception(msg)); 
				}

				FUMResult responseObject = mapper.readValue(post.getResponseBodyAsString(), FUMResult.class);

				switch (responseObject.getStatus()) {
				case ACCEPTED:
				{
					message = handleFileAccepted(file, tempFile, responseObject, socketOptions);
					ActionLogger.logCmd(ActionLogger.FUM_ACCEPTED, com.gentics.contentnode.object.File.TYPE_FILE, file.getId(), file.getFolder().getId(), file.getName());
					file.save();
					break;
				}
				case POSTPONED:
				{
					ActionLogger.logCmd(ActionLogger.FUM_POSTPONED, com.gentics.contentnode.object.File.TYPE_FILE, file.getId(), file.getFolder().getId(), file.getName());
					I18nString  i18nMessage = new CNI18nString("rest.file.upload.fum_postponed");

					i18nMessage.setParameter("0", ObjectTransformer.getString(file.getId(), null));
					message = new Message(Message.Type.SUCCESS, i18nMessage.toString());

					postponed = true;

					FileInputStream tempFileInputStream = null;
					try {
						tempFileInputStream = new FileInputStream(tempFile);
						file.setFileStream(tempFileInputStream);
						file.save();
					} finally {
						tempFileInputStream.close();
					}
					break;
				}
				case DENIED:
				default:
				{
					String msg = "The file you tried to upload could not successfully be validated in the installed checks (FileUploadManipulator).";
					I18nString i18nMessage = new CNI18nString("rest.file.upload.fum_failure");

					if (responseObject.getStatus() == FUMResponseStatus.DENIED) {
						msg = responseObject.getMsg();
						i18nMessage = new CNI18nString("error while invoking file upload manipulator");
					}

					final String finalMsg = file.getName() + ": " + msg;

					// Just delete the created file again
					file.delete();

					TransactionManager.execute(() -> ActionLogger.logCmd(ActionLogger.FUM_DENIED, com.gentics.contentnode.object.File.TYPE_FILE, file.getId(),
							file.getFolder().getId(), finalMsg));
					throw new NodeException(msg, new Exception(i18nMessage.toString() + ": " + msg));
				}
				}

				if (postponed) {
					handlePostponedId(tempFile.getCanonicalPath() + ".id", file.getId());
				}
			} catch (IOException e) {
				TransactionManager.execute(() -> ActionLogger.logCmd(ActionLogger.FUM_ERROR, com.gentics.contentnode.object.File.TYPE_FILE, file.getId(),
						file.getFolder().getId(), file.getName() + ":" + e.getLocalizedMessage()));
				String msg = "The file you tried to upload could not successfully be validated in the installed checks (FileUploadManipulator).";
				I18nString i18nMessage = new CNI18nString("rest.file.upload.fum_failure");

				throw new NodeException(i18nMessage.toString(), new Exception(msg + ": " + e.getLocalizedMessage(), e));
			} finally {
				if (!postponed) {
					if (tempFile != null) {
						tempFile.delete();
					}
				}
				deleteOldTempFiles(tmpPath);
			}
		} else {
			file.setFileStream(fileStream);
			file.save();
		}
		return message;
	}

	/**
	 * This method deletes all temporary files that are older than the
	 * tempFileTimeout
	 *
	 * @param tmpPath the path to look for temporary files
	 * @throws NodeException a NodeException is thrown when access is denied to the tmpFolder or when a temporary file could not be deleted
	 */
	private static void deleteOldTempFiles(String tmpPath) throws NodeException {

		File tmpFolder = new File(tmpPath);
		try {
			if (tmpFolder.isDirectory()) {
				for (File file : tmpFolder.listFiles()) {
					String fileName = file.getName();
					if (fileName.startsWith(FUM_TEMPFILE_PREFIX) && fileName.endsWith(FUM_TEMPFILE_SUFFIX)) {

						// delete file if it is older than the timeout
						if ((System.currentTimeMillis() - file.lastModified()) > FUM_TEMPFILE_TIMEOUT) {
							try {
								file.delete();
							} catch (SecurityException e) {
								I18nString i18nMessage = new CNI18nString("rest.file.upload.fum_failure");
								String msg = "Unsufficient permissions deleting temporary file " + file.getAbsolutePath();
								throw new NodeException(i18nMessage.toString(), new Exception(msg + ": " + e.getLocalizedMessage(), e));
							}
						}
					}
				}

			}
		} catch (SecurityException e) {
			I18nString i18nMessage = new CNI18nString("rest.file.upload.fum_failure");
			String msg = "Error deleting temporary files. The temporary Folder " + tmpPath + " is not accessible";
			throw new NodeException(i18nMessage.toString(), new Exception(msg + ": " + e.getLocalizedMessage(), e));
		}
	}

	/**
	 * Method to handle the accepted FUM Response.
	 * @param file
	 * @param tempFile
	 * @param responseObject
	 * @param socketOptions socket options
	 * @return
	 * @throws JSONException
	 * @throws NodeException
	 * @throws ReadOnlyException
	 * @throws IOException 
	 * @throws HttpException 
	 */
	public static Message handleFileAccepted(
			com.gentics.contentnode.object.File file,
			File tempFile,
			FUMResult responseObject,
			FUMSocketOptions socketOptions) throws NodeException,
				ReadOnlyException, HttpException, IOException {
		Message message = null;

		if (!StringUtils.isEmpty(responseObject.getUrl())) {
			// File has been accepted and modified.
			HttpClient client = getHttpClient(socketOptions.getSocketTimeout(), socketOptions.getConnectionTimeout(), socketOptions.getConnectionRetry());
			GetMethod get = new GetMethod(responseObject.getUrl());

			client.executeMethod(get);
			file.setFileStream(get.getResponseBodyAsStream());
		} else {
			// File has been accepted as is.
			file.setFileStream(new FileInputStream(tempFile));
		}

		if (!StringUtils.isEmpty(responseObject.getFilename())) {
			file.setName(responseObject.getFilename());
		}
		if (!StringUtils.isEmpty(responseObject.getMimetype())) {
			file.setFiletype(responseObject.getMimetype());
		}
		if (!StringUtils.isEmpty(responseObject.getMsg())) {
			I18nString  i18nMessage = new CNI18nString(responseObject.getMsg());

			i18nMessage.setParameter("0", ObjectTransformer.getString(file.getId(), null));
			message = new Message(Message.Type.SUCCESS, i18nMessage.toString());
		}
		return message;
	}

	/**
	 * Creates the JSON Object for the Request to the FUM
	 * @param prefs
	 * @param file
	 * @param languageInfo
	 * @param fileUploadManipulatorOptions
	 * @param tempFile
	 * @return
	 * @throws IOException
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	private static FUMRequest createFUMRequestObject(NodePreferences prefs,
			com.gentics.contentnode.object.File file, String languageInfo,
			Map<?, ?> fileUploadManipulatorOptions, File tempFile) throws IOException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		FUMRequest request = new FUMRequest();
		request.setOptions((Map<String, Object>) fileUploadManipulatorOptions);
		request.setId(tempFile.getCanonicalPath());
		request.setFileid(file.getId());
		request.setFilename(file.getName());
		request.setMimetype(file.getFiletype());
		request.setUrl(prefs.getProperty(CN_LOCAL_SERVER_KEY) + prefs.getProperty(PORTLETAPP_PREFIX_INTERNAL) + "rest/fum/" + tempFile.getName());
		request.setPostponeurl(prefs.getProperty(CN_LOCAL_SERVER_KEY) + prefs.getProperty(PORTLETAPP_PREFIX_INTERNAL) + "rest/fum/" + tempFile.getName());
		request.setLang(languageInfo);

		com.gentics.contentnode.object.Folder folder = file.getFolder();
		Map<String, Object> folderMap = new HashMap<>();
		folderMap.put("id", folder.getId());
		folderMap.put("name", folder.getName());
		folderMap.put("nodeid", folder.getNode().getId());
		request.setFolder(folderMap);

		request.setUser(ModelBuilder.getUser(t.getObject(SystemUser.class, t.getUserId()), Reference.DESCRIPTION));

		return request;
	}

	/**
	 * Writes the id of the object to the destined file in order to enable the FUM to find the file.
	 * @param fileName name of the temporary id file
	 * @param id id of the saved object
	 * @throws IOException 
	 */
	public static void handlePostponedId(String fileName, Object id) throws IOException {
		FileWriter tempIdFileWriter = new FileWriter(fileName, false);

		tempIdFileWriter.write(ObjectTransformer.getString(id, ""));
		tempIdFileWriter.close();
	}
	
	/**
	 * Create a temp File from the inputStream.
	 * @param tmpPath path where the temporary file should be stored in
	 * @param stream
	 * @return temp file containing binary
	 * @throws IOException
	 */
	private static File createTempFile(String tmpPath, InputStream stream) throws IOException {
		File tempFile = File.createTempFile(FUM_TEMPFILE_PREFIX , FUM_TEMPFILE_SUFFIX, new File(tmpPath));
		
		FileOutputStream out = new FileOutputStream(tempFile);

		FileUtil.inputStreamToOutputStream(stream, out);
		out.flush();
		out.close();
		return tempFile;
	}
	
	/**
	 * Get an instance of the {@link HttpClient} to be used for authentication and logging out.
	 * 
	 * @param socketTimeout Socket timeout for the HttpClient
	 * @param connectionTimeout Connection timeout for the HttpClient
	 * @param connectionRetry Max number of connection restries for the HttpClient
	 * @return http client instance
	 */
	private static HttpClient getHttpClient(int socketTimeout, long connectionTimeout, int connectionRetry) {
		HttpClient client = new HttpClient();

		client.setHttpConnectionManager(new MultiThreadedHttpConnectionManager());
		if (connectionTimeout >= 0) {
			client.getParams().setConnectionManagerTimeout(connectionTimeout);
			client.getParams().setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, (int) connectionTimeout);
		}
		if (socketTimeout >= 0) {
			client.getParams().setSoTimeout(socketTimeout);
		}
		client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(connectionRetry, false));
		return client;
	}
}
