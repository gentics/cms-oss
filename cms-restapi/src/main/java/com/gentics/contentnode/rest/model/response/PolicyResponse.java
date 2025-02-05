/*
 * @author tobiassteiner
 * @date Jan 20, 2011
 * @version $Id: PolicyResponse.java,v 1.1.2.3 2011-02-10 13:43:41 tobiassteiner Exp $
 */
package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "policy")
public class PolicyResponse {
	@XmlAttribute
	public String name;
	@XmlAttribute
	public String uri;

	public PolicyResponse() {}
}
