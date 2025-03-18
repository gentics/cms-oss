package com.gentics.contentnode.rest.model.response;

import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * REST Model for localization info
 */
@XmlRootElement
public class LocalizationInfo extends GenericResponse implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 7961730624835802361L;

	/**
	 * Localization info for channels
	 */
	private List<ChannelLocalizationInfo> channels;

	/**
	 * Create empty instance
	 */
	public LocalizationInfo() {}

	/**
	 * Create instance with given message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public LocalizationInfo(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * @return the channels
	 */
	public List<ChannelLocalizationInfo> getChannels() {
		return channels;
	}

	/**
	 * @param channels the channels to set
	 */
	public void setChannels(List<ChannelLocalizationInfo> channels) {
		this.channels = channels;
	}
}
