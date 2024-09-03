package com.gentics.contentnode.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public record PublishLogDto(
		int objId,
		String type,
		String state,
		int user,
		String date
) {

}
