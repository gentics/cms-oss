package com.gentics.contentnode.rest.model.response;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Image;

/**
 * Response containing information about multiple images.
 */
@XmlRootElement
public class MultiImageLoadResponse extends AbstractStagingResponse<String> {

	private static final long serialVersionUID = -5704792864208230620L;
	/**
	 * The list of found images.
	 */
	private List<Image> images;

	/**
	 * Constructor used by JAXB.
	 */
	public MultiImageLoadResponse() {
	}

	/**
	 * Convenience constructor.
	 *
	 * Automatically adds a response info with a response code
	 * of <code>OK</code>.
	 *
	 * @param images The images the send with the response.
	 */
	public MultiImageLoadResponse(List<Image> images) {
		super(
			null,
			new ResponseInfo(
				ResponseCode.OK,
				(images == null ? 0 : images.size()) + " image(s) loaded"));

		this.images = images;
	}

	/**
	 * Response with an empty image list.
	 *
	 * @param message The message that should be displayed to the user.
	 * @param response ResponseInfo with the status of the response.
	 */
	public MultiImageLoadResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);

		this.images = new ArrayList<>();
	}

	/**
	 * The list of found images.
	 *
	 * @return The list of found images.
	 */
	public List<Image> getImages() {
		return images;
	}

	/**
	 * Set the list of images to send with the response.
	 *
	 * @param pages The list of images to send with the response.
	 */
	public void setImages(List<Image> images) {
		this.images = images;
	}
}
