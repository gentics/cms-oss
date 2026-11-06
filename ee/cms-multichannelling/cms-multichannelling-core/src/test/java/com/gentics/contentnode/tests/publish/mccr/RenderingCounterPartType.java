package com.gentics.contentnode.tests.publish.mccr;

import com.gentics.api.contentnode.parttype.AbstractExtensiblePartType;
import com.gentics.api.lib.exception.NodeException;

/**
 * PartType that's used for counting the renderings of an object.
 * 
 * @author Antoniy Chonkov <antoniy@web-teh.net>
 */
public class RenderingCounterPartType extends AbstractExtensiblePartType {

	private static int RENDERING_COUNTER = 0;
	
	public static int getRenderingCounter() {
		return RENDERING_COUNTER;
	}
	
	public static void resetRenderingCounter() {
		RENDERING_COUNTER = 0;
	}

	public String render() throws NodeException {
		RenderingCounterPartType.RENDERING_COUNTER++;
		return RenderingCounterPartType.class.getSimpleName();
	}
}
