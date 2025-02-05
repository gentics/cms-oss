/*
 * @author norbert
 * @date 28.04.2010
 * @version $Id: ImageCreateRequest.java,v 1.1 2010-04-28 15:44:31 norbert Exp $
 */
package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Image;

@XmlRootElement
public class ImageResizeRequest {

	/**
	 * Image to be resized
	 */
	private Image image;

	private int cropHeight;

	private int cropWidth;

	private int cropStartX;

	private int cropStartY;

	private int width;

	private int height;

	private String mode;

	private String resizeMode;

	private String targetFormat;

	private Float fpX;

	private Float fpY;

	private boolean copyFile;

	private ImageRotate rotate;

	public ImageResizeRequest() {}

	/**
	 * @return the copyFile
	 */
	public boolean isCopyFile() {
		return copyFile;
	}

	/**
	 * @param copyFile the copyFile to set
	 */
	public void setCopyFile(boolean copyFile) {
		this.copyFile = copyFile;
	}

	public String getTargetFormat() {
		return targetFormat;
	}

	public void setTargetFormat(String targetFormat) {
		this.targetFormat = targetFormat;
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public int getCropHeight() {
		return cropHeight;
	}

	public void setCropHeight(int cropHeight) {
		this.cropHeight = cropHeight;
	}

	public int getCropWidth() {
		return cropWidth;
	}

	public void setCropWidth(int cropWidth) {
		this.cropWidth = cropWidth;
	}

	public int getCropStartX() {
		return cropStartX;
	}

	public void setCropStartX(int cropStartX) {
		this.cropStartX = cropStartX;
	}

	public int getCropStartY() {
		return cropStartY;
	}

	public void setCropStartY(int cropStartY) {
		this.cropStartY = cropStartY;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * Returns the mode (eg. cropandresize)
	 * @return
	 */
	public String getMode() {
		return mode;
	}

	/**
	 * Sets the mode (eg. cropandresize)
	 * @param mode
	 */
	public void setMode(String mode) {
		this.mode = mode;
	}

	/**
	 * Returns the resizemode (eg. force)
	 * @return
	 */
	public String getResizeMode() {
		return resizeMode;
	}

	/**
	 * Sets the resizemode (eg. force)
	 * @param resizeMode
	 */
	public void setResizeMode(String resizeMode) {
		this.resizeMode = resizeMode;
	}

	/**
	 * Return the focal point x factor.
	 * @return
	 */
	public Float getFpX() {
		return fpX;
	}

	/**
	 * Set the focal point x factor.
	 * @param fpX
	 */
	public void setFpX(Float fpX) {
		this.fpX = fpX;
	}

	/**
	 * Return the focal point y factor.
	 * @return
	 */
	public Float getFpY() {
		return fpY;
	}

	/**
	 * Set the focal point y factor.
	 * @param fpY
	 */
	public void setFpY(Float fpY) {
		this.fpY = fpY;
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
	 * Set rotate direction
	 * @param rotate direction
	 */
	public void setRotate(ImageRotate rotate) {
		this.rotate = rotate;
	}
}
