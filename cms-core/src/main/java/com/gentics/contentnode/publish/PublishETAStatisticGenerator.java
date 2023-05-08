/*
 * @author herbert
 * @date May 24, 2007
 * @version $Id: PublishETAStatisticGenerator.java,v 1.4 2008-02-20 12:31:49 herbert Exp $
 */
package com.gentics.contentnode.publish;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.gentics.lib.etc.IWorkPhase;
import com.gentics.lib.log.NodeLogger;

public class PublishETAStatisticGenerator extends Thread {

	private IWorkPhase rootPhase;
	private PrintStream csvStream;
    
	private List publishEtaStatistics = new ArrayList();
	private IWorkPhase initPhase;
    
	private static NodeLogger logger = NodeLogger.getNodeLogger(PublishETAStatisticGenerator.class);
    
	public PublishETAStatisticGenerator(IWorkPhase rootPhase, IWorkPhase initPhase, File csvFile) {
		this.rootPhase = rootPhase;
		this.initPhase = initPhase;
        
		try {
			csvStream = new PrintStream(new FileOutputStream(csvFile));
		} catch (IOException e) {
			throw new RuntimeException("Error while initializing publish eta statistic generator.", e);
		}
	}
    
	public void run() {
		// By default log an entry every 5 seconds.
		long intervall = 5000;
        
		long startTime = rootPhase.getStartTime();
		boolean done = false;

		try {
			while (!done && !isInterrupted()) {
				done = !rootPhase.isCurrentlyRunning();
				// time, time elapsed, %, eta, phase, phase done, phase left
				int eta = rootPhase.getETA();
				long currentTime = System.currentTimeMillis();
				long elapsed = currentTime - startTime;
				double percent = 100. / (elapsed + eta) * elapsed;
                
				IWorkPhase phase = rootPhase.getCurrentPhase();
				String phaseName = phase == null ? "None" : phase.getName();

				publishEtaStatistics.add(new PublishETADataSet(currentTime, elapsed, percent, eta, phaseName));
                
				csvStream.print(currentTime);
				csvStream.print(',');
				csvStream.print(elapsed);
				csvStream.print(',');
				csvStream.print(percent);
				csvStream.print(',');
				csvStream.print(eta);
				csvStream.print(',');
				csvStream.print(phaseName);
				if (phase != null) {
					csvStream.print(',');
					csvStream.print(phase.getDoneWork());
					csvStream.print(',');
					csvStream.print(phase.getWork());
				} else {
					csvStream.print(',');
					csvStream.print(',');
				}
				csvStream.print(',');
				csvStream.print(rootPhase.getAbsoluteProgress());
                
				csvStream.println();
                
				Thread.sleep(intervall);
			}
		} catch (InterruptedException e) {
			logger.debug("Thread was interrupted while writing statistics.", e);
		}
		csvStream.flush();
		csvStream.close();
	}
    
	public List getPublishEtaStatistics() {
		return publishEtaStatistics;
	}

	public IWorkPhase getRootPhase() {
		return rootPhase;
	}
    
	public IWorkPhase getInitPhase() {
		return initPhase;
	}
}
