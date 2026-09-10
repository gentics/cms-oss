package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;

/**
 * Interface for services which render text which is returned by {@link TextPartType#render(com.gentics.contentnode.render.RenderResult, String)}
 */
public interface TextRenderService {
	/**
	 * Render the given text for the value
	 * @param value value which returns the text
	 * @param text text
	 * @return possibly modified text
	 * @throws NodeException
	 */
	String render(Value value, String text) throws NodeException;
}
