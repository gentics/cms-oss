/*
 * @author herbert
 * @date May 25, 2007
 * @version $Id: PublishETADataSet.java,v 1.2 2007-08-17 10:37:12 norbert Exp $
 */
package com.gentics.contentnode.publish;

class PublishETADataSet {
	long time;
	long elapsed;
	double percent;
	long eta;
	String phase;
    
	public PublishETADataSet(long time, long elapsed, double percent, long eta, String phase) {
		this.time = time;
		this.elapsed = elapsed;
		this.percent = percent;
		this.eta = eta;
		this.phase = phase;
	}
    
	public PublishETADataSet(String line) {
		String[] v = line.split(",");
        
		time = Long.parseLong(v[0]);
		elapsed = Long.parseLong(v[1]);
		percent = Double.parseDouble(v[2]);
		eta = Long.parseLong(v[3]);
		phase = v[4];
	}
}
