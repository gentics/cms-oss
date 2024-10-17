/*
 * @author Stefan Hepp
 * @date 27.04.2005 16:21
 * @version $Id: GenticsContentTextImport.java,v 1.1 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.content.contentimport;

import java.util.ArrayList;
import java.util.List;

import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.inputsource.TextInputSource;

/**
 * Main implementation of a parser which uses a {@link TextInputSource} and 
 * can skip ranges of lines. The skip line syntax is
 * <code>
 * <linenr>|<start>-<end>|<max>+
 * </code>
 * More than one range can be given by separating the ranges by ';'.
 */
public class GenticsContentTextImport extends AbstractContentImport {

	private TextInputSource inputSource;

	private List skipLines;

	/**
	 * an interface for a skip line ranges. 
	 */
	private interface SkipLineChecker {
        
		/**
		 * check if a line should be skipped. 
		 * @param lineNr the current line number to check.
		 * @return true, if the line should be ignored.
		 */
		boolean doSkipLine(int lineNr);
	}

	/**
	 * a skip single line implemenation.
	 */
	private class SkipLine implements SkipLineChecker {
		private int lineNr;

		public SkipLine(int lineNr) {
			this.lineNr = lineNr;
		}

		public boolean doSkipLine(int lineNr) {
			return lineNr == this.lineNr;
		}
	}

	/**
	 * a skip range implementation.
	 */
	private class SkipLineRange implements SkipLineChecker {
		private int startLine;

		private int endLine;

		public SkipLineRange(int startLine, int endLine) {
			this.startLine = startLine;
			this.endLine = endLine;
		}

		public boolean doSkipLine(int lineNr) {
			return (lineNr >= startLine) && (lineNr <= endLine);
		}
	}

	/**
	 * a skip max line implementation.
	 */
	private class SkipLineLimit implements SkipLineChecker {
		private int startLine;

		public SkipLineLimit(int startLine) {
			this.startLine = startLine;
		}

		public boolean doSkipLine(int lineNr) {
			return lineNr >= startLine;
		}
	}

	/**
	 * create a new text parser.
	 * @param input the text input source to parse.
	 * @param objType the object type of the objects to import.
	 * @param skipLines the ranges of lines to skip.
	 * @throws ContentImportException
	 */
	public GenticsContentTextImport(TextInputSource input, int objType, String skipLines) throws ContentImportException {
		super(objType);
		inputSource = input;
		setSkipLines(skipLines);
	}

	/**
	 * create a new text parser.
	 * @param input the text input source to parse.
	 * @param objType the object type of the objects to import.
	 */
	public GenticsContentTextImport(TextInputSource inputSource, int objType) {
		super(objType);
		this.inputSource = inputSource;
		try {
			setSkipLines("");
		} catch (ContentImportException e) {}
	}

	/**
	 * set the line ranges to skip.
	 * @param lines the line ranges.
	 * @throws ContentImportException
	 */
	public void setSkipLines(String lines) throws ContentImportException {
		if (skipLines == null) {
			skipLines = new ArrayList();
		} else {
			skipLines.clear();
		}

		String[] parts = lines.split(";");

		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];

			if (part.length() == 0) {
				continue;
			}

			try {
				if (part.indexOf('-') > -1) {
					String[] range = part.split("-");

					if (range.length < 2) {
						continue;
					}
					skipLines.add(new SkipLineRange(Integer.parseInt(range[0]), Integer.parseInt(range[1])));
				} else if (part.charAt(part.length() - 1) == '+') {
					skipLines.add(new SkipLineLimit(Integer.parseInt(part.substring(0, part.length() - 1))));
				} else {
					skipLines.add(new SkipLine(Integer.parseInt(part)));
				}
			} catch (NumberFormatException e) {
				// error reading skip line number
				throw new ContentImportException("Could not parse skip line number '" + part + "'.");
			}
		}
	}

	public void doImport(CNWriteableDatasource ds) {

		int lineNr = 1;

		String[] line = null;

		while (doSkipLine(lineNr)) {
			line = inputSource.readLine();
			if (line == null) {
				return;
			}
			lineNr++;
		}

		line = inputSource.readLine();
		if (line == null) {
			return;
		}

		loadHeader(ds, lineNr, line);
		lineNr++;

		triggerEvent(EVENT_ON_START_IMPORT, null);

		while ((line = inputSource.readLine()) != null) {
			if (!doSkipLine(lineNr)) {
				try {
					parseLine(ds, lineNr, line);
				} catch (ContentImportException e) {
					NodeLogger.getNodeLogger(getClass()).warn("Error while importing data (line " + lineNr + ")", e);
					getLogger().addError("", "Skipping line " + lineNr + ".");
				}
			}
			lineNr++;
		}

		triggerEvent(EVENT_ON_IMPORT_FINISHED, null);
	}

	/**
	 * check if a line should be skipped.
	 * @param lineNr the line to check.
	 * @return true, if the line should be skipped.
	 */
	public boolean doSkipLine(int lineNr) {
		for (int i = 0; i < skipLines.size(); i++) {
			SkipLineChecker skipLine = (SkipLineChecker) skipLines.get(i);

			if (skipLine.doSkipLine(lineNr)) {
				return true;
			}
		}
		return false;
	}
}
