/*
 * @author tobiassteiner
 * @date Jan 11, 2011
 * @version $Id: XmlUriAdapter.java,v 1.1.2.1 2011-02-10 13:43:28 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util.jaxb;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * To be used with the JAXB annotation XmlJavaTypeAdapter(XmlUriAdapter.class).
 * May throw an URISyntaxException during unmarshalling.
 */
public class XmlUriAdapter extends XmlAdapter<String, URI> {

	@Override
	public String marshal(URI uri) {
		return uri.toString();
	}

	@Override
	public URI unmarshal(String uri) throws URISyntaxException {
		return new URI(uri);
	}
}
