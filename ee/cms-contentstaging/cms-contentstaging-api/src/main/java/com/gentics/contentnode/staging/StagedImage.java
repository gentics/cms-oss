package com.gentics.contentnode.staging;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * REST entity for the image, contained in the Content Staging package.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedImage extends StagedFile {

	public static final String TYPE_NAME = "image";

	private static final long serialVersionUID = -1836617632473566482L;

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
	 * DPI (x-Dimension)
	 * @return the dpiX
	 */
	public Integer getDpiX() {
		return dpiX;
	}

	/**
	 * Clone the origin into this entity.
	 * 
	 * @param origin
	 */
	@XmlTransient
	@JsonIgnore
    public void clone(StagedImage origin) {
		super.clone(origin);
		this.sizeX = origin.sizeX;
		this.sizeY = origin.sizeY;
		this.dpiX = origin.dpiX;
		this.dpiY = origin.dpiY;
		this.fpX = origin.fpX;
		this.fpY = origin.fpY;
		this.gisResizable = origin.gisResizable;
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

	@Override
	@XmlTransient
	@JsonIgnore
	public String getTypeName() {
		return TYPE_NAME;
	}
}
