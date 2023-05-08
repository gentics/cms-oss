package com.gentics.contentnode.tests.liveedit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static junit.framework.Assert.*;
import org.junit.Test;

import com.gentics.contentnode.etc.LiveEditorHelper;

public class MelissaPTest {

	@Test
	public void testIELiveHelper() throws Exception, Throwable {

		try {
			Method method = LiveEditorHelper.class.getDeclaredMethod("splitNodeTags", new Class[] {
				String.class, String.class });

			method.setAccessible(true);
			method.invoke(null, new Object[] { "node", "<node blah" });
		} catch (InvocationTargetException e) {
			assertNotSame(OutOfMemoryError.class, e.getCause().getClass());
			throw e.getCause();
		}

	}

}
