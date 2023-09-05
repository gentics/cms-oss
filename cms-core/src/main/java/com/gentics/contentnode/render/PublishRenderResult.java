/*
 * @author herbert
 * @date 10.04.2007
 * @version $Id: PublishRenderResult.java,v 1.6 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.contentnode.render;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.Level;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.publish.PublishInterruptedException;
import com.gentics.lib.render.exception.PublishException;
import com.gentics.lib.render.exception.RecoverableException;

/**
 * a RenderResult variation which creates a log file of the 
 * logged messages.
 * This is useful to get a logfile for a publish run .. without interfering log messages
 * from potential other renderings.
 * 
 * It is currently only ment to be used during publishing - therefore 
 * checklog will throw a {@link com.gentics.lib.render.exception.PublishException}
 * if an unrecoverable error is detected.
 * 
 * @author herbert
 */
public class PublishRenderResult extends RenderResult {

	private PrintWriter writer;
	private Level minlevel;
	protected SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private Map errorhandling;
	private PrintWriter writerVerbose;
	private Level verboseMinLevel;
    
	/**
	 * error level meaning that the error should not break the publish process,
	 * but only stop rendering of rendering the current page.
	 * (ie. the old version from publish table will be used)
	 */
	private static final String ERROR_LEVEL_SKIP_OBJECT = "skipobject";
    
	/**
	 * the publish run should fail on this error.
	 */
	private static final String ERROR_LEVEL_STOP_PUBLISHRUN = "fail";
        
	/**
	 * Completely ignore this type of error.
	 */
	private static final String ERROR_LEVEL_IGNORE = "ignore";
    
	private static final Level MIN_ERROR_LEVEL = Level.ERROR;
    
	private static final String DEFAULT_ERROR_LEVEL_KEY = "default";
    
	private static final String DEFAULT_ERROR_LEVEL_VALUE = ERROR_LEVEL_IGNORE;

	public PublishRenderResult(File filename, Level minlevel) throws NodeException {
		this(filename, minlevel, minlevel);
	}
    
	public PublishRenderResult(File filename, Level minlevel, Level verboseMinLevel) throws NodeException {
		super();
		try {
			this.writer = new PrintWriter(new FileOutputStream(filename));
		} catch (FileNotFoundException e) {
			throw new NodeException("Error while initiating file writer.", e);
		}
		this.minlevel = minlevel;
		this.verboseMinLevel = verboseMinLevel;
		this.messages = new MessageList();
	}
    
	public class MessageList extends ArrayList {
		private static final long serialVersionUID = 3050943557962163178L;

		public boolean add(Object obj) {
			if (obj instanceof NodeMessage) {
				NodeMessage msg = (NodeMessage) obj;
				StringBuffer out = new StringBuffer();

				if (msg.getLevel().isMoreSpecificThan(minlevel) || msg.getThrowable() != null) {
					out.append(dateFormat.format(new Date()));
					out.append(' ');
					out.append(msg.getLevel().toString());
					out.append(' ');
					out.append(msg.getMessage());
					if (msg.getDetails() != null && !"".equals(msg.getDetails())) {
						out.append(" - ");
						out.append(msg.getDetails());
					}
				}
				if (msg.getLevel().isMoreSpecificThan(minlevel)) {
					writer.println(out.toString());
					writer.flush();
				}
				if (writerVerbose != null && (msg.getLevel().isMoreSpecificThan(verboseMinLevel) || msg.getThrowable() != null)) {
					writerVerbose.println(out.toString());
					if (msg.getThrowable() != null) {
						writerVerbose.print(msg.getThrowable().getClass().getName());
						writerVerbose.print(": ");
						writerVerbose.println(msg.getThrowable().toString());
						msg.getThrowable().printStackTrace(writerVerbose);
					}
					writerVerbose.flush();
				}
			}
			// dont remember messages during publish, to avoid load in invokerservlet.
			return true;
		}
	}
    
	private void checklog(Level level, Class clazz, String message, Throwable t) throws NodeException {
		// Check if the given message is an error - and if it is .. check if it should be rethrown.
		if (level.isMoreSpecificThan(MIN_ERROR_LEVEL) && t instanceof NodeException && errorhandling != null) {
			String className = t.getClass().getName();
			Object res = errorhandling.get(className);

			if (res == null) {
				res = errorhandling.get(DEFAULT_ERROR_LEVEL_KEY);
				if (res == null) {
					res = DEFAULT_ERROR_LEVEL_VALUE;
				}
			}
			if (!ERROR_LEVEL_IGNORE.equals(res)) {
				if (ERROR_LEVEL_STOP_PUBLISHRUN.equals(res)) {
					throw new PublishException("Unrecoverable error detected - " + t.getLocalizedMessage(), t);
				} else if (ERROR_LEVEL_SKIP_OBJECT.equals(res)) {
					throw new RecoverableException("Detected recoverable error.", t);
				} else {
					throw new PublishException("Invalid configuration. {" + res + "}");
				}
			}
		}
		PublishRenderResult.checkInterrupted();
	}

	public void info(Class clazz, String message) throws NodeException {
		super.info(clazz, message);
		PublishRenderResult.checkInterrupted();
	}

	public void error(Class clazz, String message, Throwable e) throws NodeException {
		super.error(clazz, message, e);
		checklog(Level.ERROR, clazz, message, e);
	}

	public void fatal(Class clazz, String message, Throwable e) throws NodeException {
		super.fatal(clazz, message, e);
		checklog(Level.ERROR, clazz, message, e);
	}

	/**
	 * Close the log file.
	 */
	public void close() {
		writer.close();
		if (writerVerbose != null) {
			writerVerbose.close();
		}
	}

	/**
	 * Configures the error handling of this logger.
	 * @param errorhandling
	 */
	public void configureErrorHandling(Map errorhandling) {
		this.errorhandling = errorhandling;
	}
    
	public static void checkInterrupted() throws PublishInterruptedException {
		if (Thread.currentThread().isInterrupted()) {
			throw new PublishInterruptedException("Publish process has been interrupted (stopped ?)");
		}
	}

	/**
	 * sets the "verbose" logfile which will also contain backtraces.
	 * @param logfile_verbose
	 * @throws NodeException 
	 */
	public void setVerboseLogfile(File logfileVerbose) throws NodeException {
		try {
			this.writerVerbose = new PrintWriter(new FileOutputStream(logfileVerbose));
		} catch (FileNotFoundException e) {
			throw new NodeException("Error while initiating file writer.", e);
		}
	}

}
