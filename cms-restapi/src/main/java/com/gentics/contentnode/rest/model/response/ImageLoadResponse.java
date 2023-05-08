/*
 * @author norbert
 * @date 28.04.2010
 * @version $Id: ImageLoadResponse.java,v 1.1 2010-04-28 15:44:31 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Image;

/**
 * Response object for responses containing an Image
 * @author norbert
 */
@XmlRootElement
public class ImageLoadResponse extends GenericResponse {

	private static final long serialVersionUID = 8841739317774825601L;

	/**
	 * Image contained in the response
	 */
	private Image image;

	/**
	 * Staging package inclusion status
	 */
	private StagingStatus stagingStatus;

	/**
	 * Constructor used by JAXB
	 */
	public ImageLoadResponse() {}

	/**
	 * Constructor with message, response info and image
	 * @param message message
	 * @param responseInfo response info
	 * @param image image
	 */
	public ImageLoadResponse(Message message, ResponseInfo responseInfo, Image image) {
		super(message, responseInfo);
		this.image = image;
	}

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

	public StagingStatus getStagingStatus() {
		return stagingStatus;
	}

	public void setStagingStatus(StagingStatus stagingStatus) {
		this.stagingStatus = stagingStatus;
	}
}
