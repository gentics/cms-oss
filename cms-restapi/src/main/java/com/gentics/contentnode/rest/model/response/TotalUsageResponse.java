package com.gentics.contentnode.rest.model.response;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Usage response which contains the total count info for the queried element/s.
 */
@XmlRootElement
public class TotalUsageResponse extends GenericResponse {

	Map<Integer, TotalUsageInfo> infos = new HashMap<>();

	/**
	 * Return the total usage response map.
	 * 
	 * @return
	 */
	public Map<Integer, TotalUsageInfo> getInfos() {
		return infos;
	}

	/**
	 * Set the total usage response map.
	 * 
	 * @param infos
	 */
	public void setInfos(Map<Integer, TotalUsageInfo> infos) {
		this.infos = infos;
	}

	public TotalUsageResponse() {
	}

	public TotalUsageResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

}
