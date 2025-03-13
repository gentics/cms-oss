/*
 * @author norbert
 * @date 28.04.2010
 * @version $Id: ImageSaveRequest.java,v 1.1 2010-04-28 15:44:31 norbert Exp $
 */
package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Image;

/**
 * Request to save an image
 * @author norbert
 */
@XmlRootElement
public class ImageSaveRequest {

	/**
	 * File to be saved
	 */
	private Image image;

	/**
	 * Constructor used by JAXB
	 */
	public ImageSaveRequest() {}

	/**
	 * @return the image
	 */
	public Image getImage() {
		return image;
	}

	/**
	 * @param image the image to set
	 */
	public void setImage(Image image) {
		this.image = image;
	}
}
