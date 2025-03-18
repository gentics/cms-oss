package com.gentics.contentnode.rest.model.request;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Image;

/**
 * Request to rotate an image
 */
@XmlRootElement
public class ImageRotateRequest implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2809852370605434613L;

	private Image image;

	private boolean copyFile;

	private ImageRotate rotate;

	private String targetFormat;

	/**
	 * Image to rotate
	 * @return image
	 */
	public Image getImage() {
		return image;
	}

	/**
	 * Set the image
	 * @param image image
	 * @return fluent API
	 */
	public ImageRotateRequest setImage(Image image) {
		this.image = image;
		return this;
	}

	/**
	 * Flag for copying the file
	 * @return flag
	 */
	public boolean isCopyFile() {
		return copyFile;
	}

	/**
	 * Set the copy file flag
	 * @param copyFile flag
	 * @return fluent API
	 */
	public ImageRotateRequest setCopyFile(boolean copyFile) {
		this.copyFile = copyFile;
		return this;
	}

	/**
	 * Direction for rotating the image. cw for clockwise, ccw for counter-clockwise.
	 * The image will first be rotated and then (optionally) cropped and resized
	 * @return rotate direction
	 */
	public ImageRotate getRotate() {
		return rotate;
	}

	/**
	 * Set rotation direction
	 * @param rotate direction
	 * @return fluent API
	 */
	public ImageRotateRequest setRotate(ImageRotate rotate) {
		this.rotate = rotate;
		return this;
	}

	/**
	 * Target format (defaults to "png")
	 * @return target format
	 */
	public String getTargetFormat() {
		return targetFormat;
	}

	/**
	 * Set the target format
	 * @param targetFormat format
	 * @return fluent API
	 */
	public ImageRotateRequest setTargetFormat(String targetFormat) {
		this.targetFormat = targetFormat;
		return this;
	}
}
