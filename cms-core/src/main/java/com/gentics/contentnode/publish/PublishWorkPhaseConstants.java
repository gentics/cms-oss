/*
 * @author herbert
 * @date May 30, 2007
 * @version $Id: PublishWorkPhaseConstants.java,v 1.3 2008-01-09 09:11:19 norbert Exp $
 */
package com.gentics.contentnode.publish;

/**
 * Contains constants used by the phases created for the publish run.
 */
public class PublishWorkPhaseConstants {
	private PublishWorkPhaseConstants() {}
    
	// ////////////////////////////////////////////////
	// / The name constants are not to be changed,  ///
	// / because they are used as key for i18n      ///
	// / translations on php side.                  ///

	public static final String PHASE_NAME_TIMEMANAGEMENT = "publish.phase.timemanagement";

	public static final String PHASE_NAME_INITIALIZATION = "publish.phase.initialization";

	public static final String PHASE_NAME_WAIT_FOR_DIRTEVENTS = "publish.phase.dirting";

	public static final String PHASE_NAME_CNMAP_PUBLISH = "publish.phase.cr";

	public static final String PHASE_NAME_RENDER_PAGES = "publish.phase.rendering.pages";

	public static final String PHASE_NAME_CHECK_OFFLINE_FILE_DEPENDENCIES = "publish.phase.check.offline.files";

	public static final String PHASE_NAME_CHECK_ONLINE_FILE_DEPENDENCIES = "publish.phase.check.online.files";

	public static final String PHASE_NAME_WRITEFS = "publish.phase.writefs";
    
	public static final String PHASE_NAME_WRITEFS_PAGES_FILES = "publish.phase.writefs.pages";

	public static final String PHASE_NAME_WRITEFS_IMAGE_STORE = "publish.phase.writefs.gis";

	public static final String PHASE_NAME_CNMAP_SYNC = "publish.phase.cnmap.sync";

	public static final String PHASE_NAME_CNMAP_DOPUBLISH = "publish.phase.cnmap.publish";

	public static final String PHASE_NAME_CNMAP_DELETE_OLD = "publish.phase.cnmap.delete";

	public static final String PHASE_NAME_FINALIZING = "publish.phase.finalizing";

	public static final String PHASE_NAME_MESH_PUBLISH = "publish.phase.mesh";

	public static final String PHASE_NAME_MESH_INIT = "publish.phase.mesh.init";

	public static final String PHASE_NAME_MESH_WAIT = "publish.phase.mesh.wait";

	public static final String PHASE_NAME_MESH_FOLDERS_FILES = "publish.phase.mesh.foldersandfiles";

	public static final String PHASE_NAME_MESH_POSTPONED = "publish.phase.mesh.postponed";

	public static final String PHASE_NAME_MESH_OFFLINE = "publish.phase.mesh.offline";

	public static final String PHASE_NAME_MESH_COLLECTIMAGEDATA = "publish.phase.mesh.collectimagedata";

	public static final String PHASE_NAME_MESH_IMAGEVARIANTS = "publish.phase.mesh.imagevariants";
}
