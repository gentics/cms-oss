package com.gentics.contentnode.rest.model.response.admin;

import com.gentics.contentnode.rest.model.response.GenericResponse;

/**
 * Response containing information about the current or last publish process
 */
public class PublishInfoResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	protected boolean failed;

	protected Boolean lastFailed;

	protected String status;

	protected int progress;

	protected int estimatedDuration;

	protected boolean running;

	protected int totalWork;

	protected int totalWorkDone;

	protected String phase;

	protected int phaseNumber;

	protected int phaseCount;

	protected int phaseETA;

	protected ObjectCount files;

	protected ObjectCount folders;

	protected ObjectCount pages;

	protected ObjectCount forms;

	/**
	 * Flag to mark failed publish process
	 * @documentationExample false
	 * @return true if publish process failed
	 */
	public boolean isFailed() {
		return failed;
	}

	/**
	 * Set failed flag
	 * @param failed flag
	 */
	public void setFailed(boolean failed) {
		this.failed = failed;
	}

	/**
	 * Flag to mark if the previous publish process failed.
	 * @documentationExample false
	 * @return true if the previous publish process failed, false if it was
	 * 		successful, and null if the is no previous publish process
	 */
	public Boolean getLastFailed() {
		return lastFailed;
	}

	/**
	 * Set failed flag for the previous publish process.
	 * @param lastFailed flag
	 */
	public void setLastFailed(Boolean lastFailed) {
		this.lastFailed = lastFailed;
	}

	/**
	 * Progress in percent
	 * @documentationExample 38
	 * @return progress
	 */
	public int getProgress() {
		return progress;
	}

	/**
	 * Set progress in percent
	 * @param progress progress
	 */
	public void setProgress(int progress) {
		this.progress = progress;
	}

	/**
	 * Estimated remaining duration in seconds.
	 * @documentationExample 128
	 * @return ETA in seconds
	 */
	public int getEstimatedDuration() {
		return estimatedDuration;
	}

	/**
	 * Set ETA
	 * @param estimatedDuration ETA in seconds
	 */
	public void setEstimatedDuration(int estimatedDuration) {
		this.estimatedDuration = estimatedDuration;
	}

	/**
	 * True when the publish process is currently running
	 * @documentationExample true
	 * @return true for running
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Set running flag
	 * @param running flag
	 */
	public void setRunning(boolean running) {
		this.running = running;
	}

	/**
	 * Total number of work items of the current publish process
	 * @documentationExample 1325
	 * @return total work
	 */
	public int getTotalWork() {
		return totalWork;
	}

	/**
	 * Set total work items
	 * @param totalWork items
	 */
	public void setTotalWork(int totalWork) {
		this.totalWork = totalWork;
	}

	/**
	 * Number of work items done
	 * @documentationExample 503
	 * @return work done
	 */
	public int getTotalWorkDone() {
		return totalWorkDone;
	}

	/**
	 * Get number of work items done
	 * @param totalWorkDone done
	 */
	public void setTotalWorkDone(int totalWorkDone) {
		this.totalWorkDone = totalWorkDone;
	}

	/**
	 * Name of the current publish process phase
	 * @return phase name
	 */
	public String getPhase() {
		return phase;
	}

	/**
	 * Set name of current publish process phase
	 * @documentationExample "Publish Files and Folders into Content Repository"
	 * @param phase name
	 */
	public void setPhase(String phase) {
		this.phase = phase;
	}

	/**
	 * File counts
	 * @return file counts
	 */
	public ObjectCount getFiles() {
		return files;
	}

	/**
	 * Set file counts
	 * @param files founts
	 */
	public void setFiles(ObjectCount files) {
		this.files = files;
	}

	/**
	 * Folder counts
	 * @return folder counts
	 */
	public ObjectCount getFolders() {
		return folders;
	}

	/**
	 * Set folder counts
	 * @param folders countes
	 */
	public void setFolders(ObjectCount folders) {
		this.folders = folders;
	}

	/**
	 * Page counts
	 * @return page counts
	 */
	public ObjectCount getPages() {
		return pages;
	}

	/**
	 * Page counts
	 * @param pages counts
	 */
	public void setPages(ObjectCount pages) {
		this.pages = pages;
	}

	/**
	 * Form counts
	 * @return form counts
	 */
	public ObjectCount getForms() {
		return forms;
	}

	/**
	 * Form counts
	 * @param forms counts
	 */
	public void setForms(ObjectCount forms) {
		this.forms = forms;
	}
}
