package com.gentics.node.tests.lib.io;

import java.io.IOException;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.lib.io.CycleStreamWrapper;
import org.junit.experimental.categories.Category;

/**
 * This test tests the cycles stream wrapper. In our usecase the output stream
 * tries to write data. This write calls are redirected and only accepted as a
 * thread tries to read from our inputstream.
 */
@Category(BaseLibTest.class)
public class CycleStreamWrapperTest {

	@Test
	public void testCycleStreamWrapper() throws Exception {

		final CycleStreamWrapper csw = new CycleStreamWrapper();

		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						System.out.println("Writing hello to outputstream.");
						// Fill the buffer.
						csw.getOutputStream().write(new byte[] { 'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd', '!', '\n' });
					} catch (IOException e) {
						e.printStackTrace();
					}

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				// csw.getOutputStream().close();
			}

		}).start();

		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {

						System.out.println(csw.getAvailable());
						// Empty the buffer.
						int c;

						while ((c = csw.getInputStream().read()) != -1) {
							System.out.print((char) c);
						}
						System.out.println(csw.getAvailable());

					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();

	}
}
