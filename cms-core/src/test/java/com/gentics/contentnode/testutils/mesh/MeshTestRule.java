package com.gentics.contentnode.testutils.mesh;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * {@link TestWatcher} implementation that will reset the {@link LogBuffer} in the given mesh server instance for every starting test.
 * If a test fails, the buffered log output of the mesh server will be printed to stdout
 */
public class MeshTestRule extends TestWatcher {
	protected LogBuffer logBuffer;

	/**
	 * Create an instance
	 * @param mesh
	 */
	public MeshTestRule(MeshContext mesh) {
		this.logBuffer = mesh.getLogBuffer();
	}

	@Override
	protected void starting(Description description) {
		logBuffer.reset();
	}

	@Override
	protected void failed(Throwable e, Description description) {
		System.out.println("Mesh output for failed test " + description.getDisplayName());
		System.out.println(logBuffer.get());
	}
}
