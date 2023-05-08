package com.gentics.contentnode.tests.rendering;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.resolving.StackResolvable;

/**
 * Test cases for rendertype
 */
public class RenderTypeTest extends AbstractRenderTypeTest {

	/**
	 * Test finding objects which were pushed on the stack
	 * @throws NodeException
	 */
	@Test
	public void testStackFind() throws NodeException {
		RenderType renderType = new RenderType();

		List<StackResolvable> resolvables = Arrays.asList((StackResolvable) folder, (StackResolvable) page1,
				(StackResolvable) contentTag);

		for (StackResolvable item : resolvables) {
			// object must not be found
			assertTrue(item + " must not be found on stack", renderType.find(item) == -1);

			// push it on the stack
			renderType.push(item);

			// now it must be found
			assertTrue(item + " must be found on stack", renderType.find(item) > 0);
		}

		assertFalse("Stack must not be empty now", renderType.getStack().empty());

		// now remove from stack in reverse order
		Collections.reverse(resolvables);
		for (StackResolvable item : resolvables) {
			assertTrue(item + " must have been removed from stack", renderType.pop(item));

			// object must not be found
			assertTrue(item + " must not be found on stack", renderType.find(item) == -1);
		}

		assertTrue("Stack must be empty now", renderType.getStack().empty());
	}

	/**
	 * Test pushing objects on the stack multiple times
	 * @throws NodeException
	 */
	@Test
	public void testPushTwice() throws NodeException {
		RenderType renderType = new RenderType();

		List<StackResolvable> resolvables = Arrays.asList((StackResolvable) folder, (StackResolvable) page1,
				(StackResolvable) contentTag);

		// push everything the first time
		resolvables.forEach(item -> renderType.push(item));

		// push everything the second time
		resolvables.forEach(item -> renderType.push(item));

		// find objects on the stack
		resolvables.forEach(item -> {
			assertTrue(item + " must be found on stack", renderType.find(item) > 0);
		});

		// remove objects in reverse order from stack (first time)
		Collections.reverse(resolvables);
		resolvables.forEach(item -> {
			assertTrue(item + " must have been removed from the stack", renderType.pop(item));
			assertTrue(item + " must still be found on stack", renderType.find(item) > 0);
		});

		// remove a second time
		resolvables.forEach(item -> {
			assertTrue(item + " must have been removed from the stack", renderType.pop(item));
			assertFalse(item + " must no longer be found on stack", renderType.find(item) > 0);
		});
	}

	/**
	 * Test popping elements from the rendertype stack
	 * @throws NodeException
	 */
	@Test
	public void testPop() throws NodeException {
		RenderType renderType = new RenderType();

		// push items on the stack
		List<StackResolvable> resolvables = Arrays.asList((StackResolvable) folder, (StackResolvable) page1,
				(StackResolvable) contentTag);
		resolvables.forEach(item -> renderType.push(item));

		// pop top items
		Collections.reverse(resolvables);
		resolvables.forEach(item -> {
			assertEquals("Check popped item", item, renderType.pop());
		});

		assertTrue("Stack must be empty now", renderType.getStack().empty());
	}

	/**
	 * Test that popping the wrong object from the stack does not change anything
	 * @throws NodeException
	 */
	@Test
	public void testPopOther() throws NodeException {
		RenderType renderType = new RenderType();
		renderType.push(page1);
		renderType.push(contentTag);

		// pop the page (which is not on top of the stack)
		assertFalse(page1 + " must not have been removed from the stack", renderType.pop(page1));

		assertTrue(page1 + " must be found on stack", renderType.find(page1) > 0);
		assertTrue(contentTag + " must be found on stack", renderType.find(contentTag) > 0);
	}
}
