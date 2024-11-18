/*
 * @author herbert
 * @date 13.04.2007
 * @version $Id: RuntimeProfilerWriter.java,v 1.6 2007-08-17 10:37:25 norbert Exp $
 */
package com.gentics.lib.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUnderflowException;
import org.apache.commons.collections.buffer.BlockingBuffer;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;
import org.apache.commons.collections.map.HashedMap;

public class RuntimeProfilerWriter implements Runnable {
    
	private static NodeLogger logger = NodeLogger.getNodeLogger(RuntimeProfilerWriter.class);
    
	private Buffer fifoQueue;

	private ObjectOutputStream out;

	private File outfile;
    
	private boolean doMarkCount = true;
	
	private Map markCounts;

	public RuntimeProfilerWriter(File outfile) {
		try {
			logger.debug("Trying to create object output stream into {" + outfile.getAbsolutePath() + "}");
			out = new ObjectOutputStream(new FileOutputStream(outfile));
			this.outfile = outfile;
			if (doMarkCount) {
				markCounts = new HashedMap();
			}
		} catch (Exception e) {
			logger.error("Error while trying to create object output stream.", e);
		}
		fifoQueue = BlockingBuffer.decorate(new UnboundedFifoBuffer());
	}
    
	public void add(ProfilerMarkBean mark) {
		fifoQueue.add(mark);
	}

	public void run() {
		try {
			logger.info("Starting writing of profiler marks ...");
			boolean interrupted = false;
			boolean warned = false;
			int markCount = 0;

			while (!fifoQueue.isEmpty() || (!interrupted && !Thread.interrupted())) {
				try {
					ProfilerMarkBean mark = (ProfilerMarkBean) fifoQueue.remove();

					if (mark != null) {
						out.writeObject(mark);
						markCount++;
						out.reset();
						if (doMarkCount) {
							Integer count = (Integer) markCounts.get(mark.getElement());

							if (count == null) {
								count = new Integer(1);
							} else {
								count = new Integer(count.intValue() + 1);
							}
							markCounts.put(mark.getElement(), count);
						}
					}
					if (!warned) {
						if (fifoQueue.size() > 10000) {
							logger.warn("queue got bigger than 10000 items. {" + fifoQueue.size() + "}");
							warned = true;
						}
					} else {
						if (fifoQueue.size() < 9000) {
							logger.debug("queue shrank under 9000 items. {" + fifoQueue.size() + "}");
							warned = false;
						}
					}
				} catch (BufferUnderflowException e) {
					// Thrown when the remove() method is interrupted.
					logger.debug(
							"Detected buffer underflow exception (thread was probably simply interrupted.) - isempty: {" + Boolean.toString(fifoQueue.isEmpty()) + "}",
							e);
					// interrupt state of thread is probably reset, so remember that thread was interrupted.
					interrupted = true;
				}
			}
			logger.info("Stopped writing of profiler marks...");
			out.close();
			FileOutputStream stream = new FileOutputStream(new File(outfile.getAbsolutePath() + ".count"));

			stream.write(Integer.toString(markCount).getBytes());
			stream.close();
			if (doMarkCount) {
				logger.info("Profiling mark counts - Total: {" + markCount + "}");
				for (Iterator i = markCounts.entrySet().iterator(); i.hasNext();) {
					Map.Entry entry = (Entry) i.next();

					logger.info("   {" + entry.getKey() + "} - {" + entry.getValue() + "}");
				}
			}
		} catch (IOException e) {
			logger.error("Error while serializing profiler mark.", e);
		}
	}

}
