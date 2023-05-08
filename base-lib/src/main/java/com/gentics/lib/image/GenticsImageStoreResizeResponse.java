package com.gentics.lib.image;

import com.gentics.api.lib.upload.FileInformation;

public class GenticsImageStoreResizeResponse {

	FileInformation fileInformation;

	byte[] imageData;

	public FileInformation getFileInformation() {
		return this.fileInformation;
	}

	public byte[] getImageData() {
		return this.imageData;
	}

	/**
	 * Sets the image data for the resized image
	 */
	public void setImageData(byte[] data) {
		this.imageData = data;
	}

	/**
	 * Set the fileinformation object
	 */
	public void setFileInformation(FileInformation fileInformation) {
		this.fileInformation = fileInformation;
	}

}
