package com.gentics.contentnode.rest.model.response;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Response containing the list of subselected folders
 */
@XmlRootElement
public class ExportSelectionResponse extends GenericResponse {

	/**
	 * List of subselected folders
	 */
	protected List<Integer> folders;

	/**
	 * Map of subselected inherited folders
	 */
	protected Map<Integer, List<Integer>> inheritedFolders;

	/**
	 * Empty constructor
	 */
	public ExportSelectionResponse() {}

	/**
	 * Create a response with given message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public ExportSelectionResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Get the subselected folders
	 * @return the folders
	 */
	public List<Integer> getFolders() {
		return folders;
	}

	/**
	 * Set the subselected folders
	 * @param folders the folders to set
	 */
	public void setFolders(List<Integer> folders) {
		this.folders = folders;
	}

	/**
	 * Get the subselected inherited folders
	 * @return the inheritedFolders
	 */
	public Map<Integer, List<Integer>> getInheritedFolders() {
		return inheritedFolders;
	}

	/**
	 * Set the subselected inherited folders
	 * @param inheritedFolders the inheritedFolders to set
	 */
	public void setInheritedFolders(Map<Integer, List<Integer>> inheritedFolders) {
		this.inheritedFolders = inheritedFolders;
	}
}
