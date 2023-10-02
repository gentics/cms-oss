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

	public static final String PHASE_NAME_TIMEMANAGEMENT = "timemanagement";

	public static final String PHASE_NAME_INITIALIZATION = "Initialization";

	public static final String PHASE_NAME_WAIT_FOR_DIRTEVENTS = "Waiting for dirt events";

	public static final String PHASE_NAME_CNMAP_PUBLISH = "Content Repository publishing";

	public static final String PHASE_NAME_RENDER_PAGES = "Rendering Pages";

	public static final String PHASE_NAME_CHECK_OFFLINE_FILE_DEPENDENCIES = "Checking dependencies for offline files";

	public static final String PHASE_NAME_CHECK_ONLINE_FILE_DEPENDENCIES = "Checking dependencies for online files";

	public static final String PHASE_NAME_WRITEFS = "Writing Files";
    
	public static final String PHASE_NAME_WRITEFS_PAGES_FILES = "Writing pages and Files into Filesystem";

	public static final String PHASE_NAME_WRITEFS_IMAGE_STORE = "Invoking Gentics Image Store";

	public static final String PHASE_NAME_CNMAP_SYNC = "Synchronize Object Types";

	public static final String PHASE_NAME_CNMAP_DOPUBLISH = "Publish Files and Folders into Content Repository";

	public static final String PHASE_NAME_CNMAP_DELETE_OLD = "Delete old Objects from Content Repository";

	public static final String PHASE_NAME_FINALIZING = "Finalizing";

	public static final String PHASE_NAME_MESH_PUBLISH = "publish.phase.mesh";

	public static final String PHASE_NAME_MESH_INIT = "publish.phase.mesh.init";

	public static final String PHASE_NAME_MESH_WAIT = "publish.phase.mesh.wait";

	public static final String PHASE_NAME_MESH_FOLDERS_FILES = "publish.phase.mesh.foldersandfiles";

	public static final String PHASE_NAME_MESH_POSTPONED = "publish.phase.mesh.postponed";

	public static final String PHASE_NAME_MESH_OFFLINE = "publish.phase.mesh.offline";

	public static final String PHASE_NAME_MESH_COLLECTIMAGEDATA = "publish.phase.mesh.collectimagedata";

	public static final String PHASE_NAME_MESH_IMAGEVARIANTS = "publish.phase.mesh.imagevariants";
}
