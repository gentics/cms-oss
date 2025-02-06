package com.gentics.contentnode.rest.model.response;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * REST Model class for localization info of a channel
 */
@XmlRootElement
public class ChannelLocalizationInfo implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -7011302592562459653L;

	/**
	 * Channel ID
	 */
	private int channelId;

	/**
	 * Localization info for folders
	 */
	private LocalizationInfoItem folder;

	/**
	 * Localization info for pages
	 */
	private LocalizationInfoItem page;

	/**
	 * Localization info for images
	 */
	private LocalizationInfoItem image;

	/**
	 * Localization info for files
	 */
	private LocalizationInfoItem file;

	/**
	 * Localization info for templates
	 */
	private LocalizationInfoItem template;

	/**
	 * Number of inherited objects
	 */
	private int inherited = 0;

	/**
	 * Number of localized objects
	 */
	private int localized = 0;

	/**
	 * Number of local objects
	 */
	private int local = 0;

	/**
	 * Create empty instance
	 */
	public ChannelLocalizationInfo() {}

	/**
	 * Update the total sums
	 */
	protected void updateTotalSums() {
		inherited = (folder != null ? folder.getInherited() : 0) + (page != null ? page.getInherited() : 0) + (image != null ? image.getInherited() : 0)
				+ (file != null ? file.getInherited() : 0) + (template != null ? template.getInherited() : 0);
		localized = (folder != null ? folder.getLocalized() : 0) + (page != null ? page.getLocalized() : 0) + (image != null ? image.getLocalized() : 0)
				+ (file != null ? file.getLocalized() : 0) + (template != null ? template.getLocalized() : 0);
		local = (folder != null ? folder.getLocal() : 0) + (page != null ? page.getLocal() : 0) + (image != null ? image.getLocal() : 0)
				+ (file != null ? file.getLocal() : 0) + (template != null ? template.getLocal() : 0);
	}

	/**
	 * @return the channelId
	 */
	public int getChannelId() {
		return channelId;
	}

	/**
	 * @param channelId the channelId to set
	 */
	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}

	/**
	 * @return the folder
	 */
	public LocalizationInfoItem getFolder() {
		return folder;
	}

	/**
	 * @param folder the folder to set
	 */
	public void setFolder(LocalizationInfoItem folder) {
		this.folder = folder;
		updateTotalSums();
	}

	/**
	 * @return the page
	 */
	public LocalizationInfoItem getPage() {
		return page;
	}

	/**
	 * @param page the page to set
	 */
	public void setPage(LocalizationInfoItem page) {
		this.page = page;
		updateTotalSums();
	}

	/**
	 * @return the image
	 */
	public LocalizationInfoItem getImage() {
		return image;
	}

	/**
	 * @param image the image to set
	 */
	public void setImage(LocalizationInfoItem image) {
		this.image = image;
		updateTotalSums();
	}

	/**
	 * @return the file
	 */
	public LocalizationInfoItem getFile() {
		return file;
	}

	/**
	 * @param file the file to set
	 */
	public void setFile(LocalizationInfoItem file) {
		this.file = file;
		updateTotalSums();
	}

	/**
	 * @return the template
	 */
	public LocalizationInfoItem getTemplate() {
		return template;
	}

	/**
	 * @param template the template to set
	 */
	public void setTemplate(LocalizationInfoItem template) {
		this.template = template;
		updateTotalSums();
	}

	/**
	 * @return the inherited
	 */
	public int getInherited() {
		return inherited;
	}

	/**
	 * @param inherited the inherited to set
	 */
	public void setInherited(int inherited) {
		this.inherited = inherited;
	}

	/**
	 * @return the localized
	 */
	public int getLocalized() {
		return localized;
	}

	/**
	 * @param localized the localized to set
	 */
	public void setLocalized(int localized) {
		this.localized = localized;
	}

	/**
	 * @return the local
	 */
	public int getLocal() {
		return local;
	}

	/**
	 * @param local the local to set
	 */
	public void setLocal(int local) {
		this.local = local;
	}

	/**
	 * Merge this info with the given info
	 * @param info info
	 */
	public void mergeWith(ChannelLocalizationInfo info) {
		if (info.getFolder() != null) {
			if (folder == null) {
				folder = new LocalizationInfoItem(info.getFolder());
			} else {
				folder.mergeWith(info.getFolder());
			}
		}
		if (info.getPage() != null) {
			if (page == null) {
				page = new LocalizationInfoItem(info.getPage());
			} else {
				page.mergeWith(info.getPage());
			}
		}
		if (info.getImage() != null) {
			if (image == null) {
				image = new LocalizationInfoItem(info.getImage());
			} else {
				image.mergeWith(info.getImage());
			}
		}
		if (info.getFile() != null) {
			if (file == null) {
				file = new LocalizationInfoItem(info.getFile());
			} else {
				file.mergeWith(info.getFile());
			}
		}
		if (info.getTemplate() != null) {
			if (template == null) {
				template = new LocalizationInfoItem(info.getTemplate());
			} else {
				template.mergeWith(info.getTemplate());
			}
		}

		updateTotalSums();
	}
}
