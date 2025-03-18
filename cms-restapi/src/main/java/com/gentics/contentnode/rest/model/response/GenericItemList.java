package com.gentics.contentnode.rest.model.response;

import java.io.Serial;
import jakarta.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class GenericItemList<T> extends AbstractListResponse<T> {

	@Serial
	private static final long serialVersionUID = -5713150794537109477L;
}
