/*
 * @author Stefan Hepp
 * @date 27.04.2005 16:21
 * @version $Id: CsvInputSource.java,v 1.5 2006-08-10 09:58:43 stefan Exp $
 */
package com.gentics.lib.util.inputsource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

/**
 * A simple Csv-Reader implementation, implements TextInputSource.
 */
public class CsvInputSource implements TextInputSource {

	private char delim;

	private char splitter;

	private int cols;

	private BufferedReader csvInput;

	/**
	 * Default Constructor, create a new csv-reader. '\n' introduces a new line.
	 * TODO if \n appears between delimiter, merge next line into recordset.
	 * @param delim delimiter character of string-fields, like '"'.
	 * @param splitter splitter char between two fields, like ';' or '\t'.
	 */
	public CsvInputSource(char delim, char splitter) {
		super();
		this.delim = delim;
		this.splitter = splitter;
	}

	public void startInput(Reader file) {
		csvInput = new BufferedReader(file);
		cols = 0;
	}

	public String[] readLine() {

		String[] data = null;

		if (csvInput == null) {
			return null;
		}

		try {
			String line = csvInput.readLine();

			if (line == null) {
				csvInput.close();
			} else {
				// read the line
				Vector row = new Vector(cols);

				if (parseLine(row, line)) {
					data = new String[row.size()];
					row.toArray(data);
					cols = Math.max(cols, row.size());
				} else {
					data = new String[0];
				}
			}

		} catch (IOException e) {
			data = null;
		}

		return data;
	}

	private boolean parseLine(Vector row, String line) {

		int pos = 0;
		int startPos = 0;
		int endPos = 0;
		int enclosed = 0;
		char c;

		while (pos < line.length()) {
			c = line.charAt(pos);
			if (c == '\\') {
				// skip next char
				pos++;
			} else if (c == splitter && enclosed == 0) {
				row.add(line.substring(startPos, endPos).trim());
				startPos = pos + 1;
				endPos = startPos;
			} else if (c == delim) {
				if (enclosed == 0) {
					// skip opening "
					if (startPos == endPos) {
						startPos = pos + 1;
						endPos = startPos;
					}
					enclosed = 1;
				} else {
					enclosed = 0;
				}
			} else if (c == Character.LINE_SEPARATOR) {
				// do not add to result, skip rest (if any)
				break;
			} else {
				endPos = pos + 1;
			}
			pos++;
		}

		// add last line
		if (startPos != endPos) {
			row.add(line.substring(startPos, endPos));
		}

		return true;
	}
}
