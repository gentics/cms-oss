package com.gentics.contentnode.rest.resource.impl.fum;

import java.io.File;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.rest.filters.AccessControl;
import com.gentics.contentnode.rest.model.fum.FUMResult;
import com.gentics.contentnode.rest.model.fum.FUMStatus;
import com.gentics.contentnode.rest.model.fum.FUMStatusResponse;
import com.gentics.contentnode.rest.resource.fum.FUMResource;
import com.gentics.contentnode.rest.util.FUMSocketOptions;
import com.gentics.contentnode.rest.util.FileUploadManipulatorFileSave;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileUtil;

@Path("/fum")
@AccessControl("FILEUPLOAD_MANIPULATOR_ACCEPT_HOST")
public class FUMResourceImpl implements FUMResource {
	private static NodeLogger logger = NodeLogger.getNodeLogger(FUMResourceImpl.class);

	@Override
	@GET
	@Path("/{filename}")
	public Response fetch(@PathParam("filename") String filename) {
		File file = new File(ConfigurationValue.TMP_PATH.get(), filename);
		if (!file.exists()) {
			return Response.status(Status.NOT_FOUND).entity("").build();
		} else {
			return Response.ok(file).build();
		}
	}

	@Override
	@POST
	@Path("/{filename}")
	public FUMStatusResponse done(@PathParam("filename") String filename, FUMResult result) {
		NodeConfigRuntimeConfiguration config = NodeConfigRuntimeConfiguration.getDefault();
		NodePreferences prefs = config.getNodeConfig().getDefaultPreferences();

		File file = new File(ConfigurationValue.TMP_PATH.get(), filename);
		File idFile = new File(ConfigurationValue.TMP_PATH.get(), filename + ".id");
		int fileId = getFileId(idFile);
		if (!file.exists() || fileId == 0) {
			return new FUMStatusResponse().setStatus(FUMStatus.ERROR).setType("invalid id");
		}

		try (Trx trx = new Trx()) {
			com.gentics.contentnode.object.File nodeFile = trx.getTransaction().getObject(com.gentics.contentnode.object.File.class, fileId, true);
			if (nodeFile == null) {
				return new FUMStatusResponse().setStatus(FUMStatus.ERROR).setType("invalid file");
			}

			switch (result.getStatus()) {
			case ACCEPTED:
				FileUploadManipulatorFileSave.handleFileAccepted(nodeFile, file, result, new FUMSocketOptions(prefs));
				ActionLogger.logCmd(ActionLogger.FUM_ACCEPTED, com.gentics.contentnode.object.File.TYPE_FILE, nodeFile.getId(), nodeFile.getFolder().getId(),
						nodeFile.getName());
				nodeFile.save();
				break;
			case DENIED:
				String msg = nodeFile.getName() + ": " + result.getMsg();
				ActionLogger.logCmd(ActionLogger.FUM_DENIED, com.gentics.contentnode.object.File.TYPE_FILE, nodeFile.getId(), nodeFile.getFolder().getId(),
						msg);
				nodeFile.delete();
				break;
			case POSTPONED:
				return new FUMStatusResponse().setStatus(FUMStatus.ERROR).setType("already postponed");
			}
			trx.success();
		} catch (Exception e) {
			return new FUMStatusResponse().setStatus(FUMStatus.ERROR).setType("general").setMsg(e.getLocalizedMessage());
		} finally {
			file.delete();
			idFile.delete();
		}

		return new FUMStatusResponse().setStatus(FUMStatus.OK);
	}

	/**
	 * Get the ID of the file
	 * @param idFile File containing the ID
	 * @return ID of the file, 0 if not found
	 */
	private int getFileId(File idFile) {
		if (!idFile.exists()) {
			return 0;
		} else {
			try {
				return Integer.parseInt(FileUtil.file2String(idFile));
			} catch (Exception e) {
				logger.warn(String.format("Error while reading ID of tmp file %s: ", idFile.getName()), e);
				return 0;
			}
		}
	}
}
