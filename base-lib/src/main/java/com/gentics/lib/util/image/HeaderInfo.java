package com.gentics.lib.util.image;

/**
 * Container for image header information.
 */
public class HeaderInfo {

	private int colorType;
	private boolean hasAdobeMarker;

	public HeaderInfo(int colorType, boolean hasAdobeMarker) {
		this.colorType = colorType;
		this.hasAdobeMarker = hasAdobeMarker;
	}

	/**
	 * Return the color type which was identified by inspecting the header
	 * 
	 * @return
	 */
	public int getColorType() {
		return colorType;
	}

	/**
	 * Flag which indicates whether the adobe format image marker was found in
	 * the image header.
	 * 
	 * @return
	 */
	public boolean hasAdobeMarker() {
		return hasAdobeMarker;
	}
}
