package com.gentics.contentnode.rest.model.request.page;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Target folder object for a page copy call. A folder is identified by its id
 * and the channelId. You can omit the channelId when you want to copy to master
 * folders.
 */
@XmlRootElement
public class TargetFolder {

	protected Integer id;

	protected Integer channelId;

	/**
	 * Constructor used by JAXB
	 */
	public TargetFolder() {

	}

	public TargetFolder(Integer id, Integer channelId) {
		this.id = id;
		this.channelId = channelId;
	}

	/**
	 * Get the target folder Id
	 * 
	 * @return
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the target folder Id
	 * 
	 * @param id
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * Get the target folder channel id
	 * 
	 * @return
	 */
	public Integer getChannelId() {
		return channelId;
	}

	/**
	 * Set the target folder channel id. Set 0 for master folders.
	 * 
	 * @param channelId
	 */
	public void setChannelId(Integer channelId) {
		this.channelId = channelId;
	}

}
