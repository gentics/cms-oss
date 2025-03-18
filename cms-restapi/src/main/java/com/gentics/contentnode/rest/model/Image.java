/*
 * @author norbert
 * @date 26.04.2010
 * @version $Id: Image.java,v 1.3 2010-09-27 08:21:00 johannes2 Exp $
 */
package com.gentics.contentnode.rest.model;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Image object, represents an Image in GCN
 * @author norbert
 */
@XmlRootElement
public class Image extends File {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -7055484237715128279L;

	/**
	 * Ttype of file
	 */
	private final Integer typeId = 10011;

	/**
	 * Width of the Image
	 */
	private Integer sizeX;

	/**
	 * Height of the Image
	 */
	private Integer sizeY;

	/**
	 * DPI of the image along x-axis
	 */
	private Integer dpiX;

	/**
	 * DPI of the image along y-axis
	 */
	private Integer dpiY;

	/**
	 * Focal point factor for x-axis.
	 */
	private Float fpX;

	/**
	 * Focal point factor for y-axis.
	 */
	private Float fpY;

	/**
	 * Whether the image is resizable by Gentics Image Store
	 */
	private Boolean gisResizable;

	/**
	 * Constructor used by JAXB
	 */
	public Image() {
		setType(ItemType.image);
	}

	/**
	 * Define attribute to select the appropriate class
	 * @return
	 */
	public String getIconCls() {
		return "gtx_image";
	}
    
	/**
	 * Image size (x-Dimension)
	 * @return the sizeX
	 */
	public Integer getSizeX() {
		return sizeX;
	}

	/**
	 * Image size (y-Dimension)
	 * @return the sizeY
	 */
	public Integer getSizeY() {
		return sizeY;
	}

	/**
	 * Type ID
	 * @return the typeId
	 */
	public Integer getTypeId() {
		return this.typeId;
	}
    
	/**
	 * DPI (x-Dimension)
	 * @return the dpiX
	 */
	public Integer getDpiX() {
		return dpiX;
	}

	/**
	 * DPI (y-Dimension)
	 * @return the dpiY
	 */
	public Integer getDpiY() {
		return dpiY;
	}

	/**
	 * Whether the image is resizable by Gentics Image Store
	 * @return
	 */
	public Boolean isGisResizable() {
		return gisResizable;
	}

	/**
	 * @param sizeX the sizeX to set
	 */
	public void setSizeX(Integer sizeX) {
		this.sizeX = sizeX;
	}

	/**
	 * @param sizeY the sizeY to set
	 */
	public void setSizeY(Integer sizeY) {
		this.sizeY = sizeY;
	}

	/**
	 * @param dpiX the dpiX to set
	 */
	public void setDpiX(Integer dpiX) {
		this.dpiX = dpiX;
	}

	/**
	 * @param dpiY the dpiY to set
	 */
	public void setDpiY(Integer dpiY) {
		this.dpiY = dpiY;
	}

	/**
	 * Return the focal point x-axis factor.
	 * @return
	 */
	public Float getFpX() {
		return fpX;
	}

	/**
	 * Set the Focal point x-axis factor.
	 * @param fpX
	 */
	public void setFpX(Float fpX) {
		this.fpX = fpX;
	}

	/**
	 * Return the focal point y-axis factor.
	 * @return
	 */
	public Float getFpY() {
		return fpY;
	}

	/**
	 * Set the focal point y-axis factor.
	 * @param fpY
	 */
	public void setFpY(Float fpY) {
		this.fpY = fpY;
	}

	/**
	 * Sets whether the image is resizable by Gentics Image Store
	 * @param gisResizable True if resizable
	 */
	public void setGisResizable(Boolean gisResizable) {
		this.gisResizable = gisResizable;
	}
}
