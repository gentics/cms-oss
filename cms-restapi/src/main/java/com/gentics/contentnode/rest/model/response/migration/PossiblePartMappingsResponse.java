package com.gentics.contentnode.rest.model.response.migration;

import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Part;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response for possible part type mapping requests
 * 
 * @author johannes2
 * 
 */
@XmlRootElement
public class PossiblePartMappingsResponse extends GenericResponse {

	Map<Integer, List<Part>> possibleMappings;

	/**
	 * Constructor used by JAXB
	 */
	public PossiblePartMappingsResponse() {}

	/**
	 * 
	 * @param message
	 * @param responseInfo
	 */
	public PossiblePartMappingsResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Set the map of possible part types
	 * 
	 * @param possibleMappings
	 */
	public void setPossibleMapping(Map<Integer, List<Part>> possibleMappings) {
		this.possibleMappings = possibleMappings;
	}

	/**
	 * Returns the map of possible parts per part id
	 * 
	 * @return
	 */
	public Map<Integer, List<Part>> getPossibleMappings() {
		return possibleMappings;
	}
}
