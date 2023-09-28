package com.gentics.lib.util;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import jakarta.activation.MimetypesFileTypeMap;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

/**
 * Utilities Class for File handling
 */
public class FileUtil {

	private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

	/**
	 * Max length for a filename.
	 */
	public final static int MAX_FILE_NAME_LENGTH = 64;

	/**
	 * Max path length
	 */
	public final static int MAX_PATH_LENGTH = 255;

	/**
	 * Regular expression for allowed characters.
	 */
	private final static String ALLOWED_CHARACTERS_REG_EXP = "[^\\w\\.\\-\\(\\)\\[\\]{}\\$#insert#]+";
	
	/**
	 * Regular expression for allowed characters in folder directories.
	 */
	private final static String ALLOWED_CHARACTERS_FOLDER_DIR_REG_EXP = "[^\\w\\.\\-\\(\\)\\[\\]{}\\$/#insert#]+";

	/**
	 * Name of the system property to configure the buffersize
	 */
	public final static String BUFFERSIZE_PARAM = "com.gentics.util.buffersize";

	/**
	 * Name of the system property to configure the maximum number of idle buffers in the pool
	 */
	public final static String BUFFERIDLE_PARAM = "com.gentics.util.buffer.maxIdle";

	/**
	 * Default buffer size (1 MB)
	 */
	public final static int DEFAULT_BUFFERSIZE = 1024 * 1024;

	/**
	 * Default value for maxIdle (10)
	 */
	public final static int DEFAULT_MAXIDLE = 10;

	/**
	* Default value for the replacement character for sanitizing filenames
	*/
	public final static String DEFAULT_REPLACEMENT_CHARACTER = "_";

	/**
	 * Pool for buffers
	 */
	protected final static GenericObjectPool<byte[]> BUFFER_POOL = new GenericObjectPool<byte[]>(new BufferFactory());

	static {
		// allow indefinite number of buffers
		BUFFER_POOL.setMaxActive(-1);
		BUFFER_POOL.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_GROW);
		// restrict the number of idle buffers
		BUFFER_POOL.setMaxIdle(ObjectTransformer.getInt(System.getProperty(BUFFERIDLE_PARAM), DEFAULT_MAXIDLE));
	}
	
	public static boolean verifyPath(String path, boolean create) {
		File f = new File(path);

		if (f.exists()) {
			if (f.isDirectory()) {
				return true;
			} else {
				return false;
			}
		}
		if (create) {
			return f.mkdirs();
		} else {
			return false;
		}
	}

	/**
	 * Read the contents of the given stream into a string. The stream is NOT
	 * closed after reading
	 * @param stream stream to be read
	 * @param charsetName name of the character set to be used for reading the
	 *        stream
	 * @return contents of the stream as string
	 * @throws IOException
	 */
	public static String stream2String(InputStream stream, String charsetName) throws IOException {
		char[] cbuf = new char[4096];
		InputStreamReader sr = new InputStreamReader(stream, charsetName);
		int read = 0;
		StringBuffer buffer = new StringBuffer();

		while ((read = sr.read(cbuf)) > 0) {
			buffer.append(cbuf, 0, read);
		}
		return buffer.toString();
	}
    
	/**
	 * Writes the inputStream to the outputStream. Uses a default buffer size of 4kB
	 * @param in inputStream
	 * @param out outputStream
	 * @throws IOException 
	 */
	public static void inputStreamToOutputStream(InputStream in, OutputStream out) throws IOException {
		inputStreamToOutputStream(in, out, 4096);
	}

	/**
	 * Writes the inputStream to the outputStream.
	 * @param in inputStream
	 * @param out outputStream
	 * @param bufferSize buffer size
	 * @throws IOException 
	 */
	public static void inputStreamToOutputStream(InputStream in, OutputStream out, int bufferSize) throws IOException {
		inputStreamToOutputStream(in, out, new byte[bufferSize]);
	}

	/**
	 * Writes the inputStream to the outputStream, using the given buffer
	 * @param in inputStream
	 * @param out outputStream
	 * @param buffer buffer to use
	 * @throws IOException
	 */
	public static void inputStreamToOutputStream(InputStream in, OutputStream out, byte[] buffer) throws IOException {
		int read = 0;

		while ((read = in.read(buffer)) >= 0) {
			out.write(buffer, 0, read);
		}
	}

	/**
	 * Writes the inputStream to the outputStream, using a buffer from the internal buffer pool
	 * @param in inputStream
	 * @param out outputStream
	 * @throws IOException
	 */
	public static void pooledBufferInToOut(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = null;
		boolean pooled = false;

		try {
			buffer = (byte[]) BUFFER_POOL.borrowObject();
			pooled = true;
		} catch (Exception e) {
			// failed to get a buffer from the pool. so create a temporary one
			buffer = new byte[ObjectTransformer.getInt(System.getProperty(BUFFERSIZE_PARAM), DEFAULT_BUFFERSIZE)];
		}

		try {
			inputStreamToOutputStream(in, out, buffer);
		} finally {
			// when the buffer was fetched from the pool, return it.
			if (pooled) {
				try {
					BUFFER_POOL.returnObject(buffer);
				} catch (Exception e) {// ignored
				}
			}
		}
	}

	public static String file2String(File file) throws IOException, FileNotFoundException {
		long n = file.length();
		char[] cbuf = new char[(int) n];
		FileReader fr = new FileReader(file);

		fr.read(cbuf);
		fr.close();
		return (new String(cbuf));
	}

	public static String[] file2StringArr(File file) throws IOException, FileNotFoundException {
		Vector<String> lines = new Vector<String>();
		FileInputStream fin = new FileInputStream(file);
		BufferedReader myInput = new BufferedReader(new InputStreamReader(fin));

		try {
			String thisLine;

			while ((thisLine = myInput.readLine()) != null) {
				lines.add(thisLine);
			}

			String[] ret = new String[lines.size()];

			return (String[]) lines.toArray(ret);
		} finally {
			try {
				fin.close();
				myInput.close();
			} catch (IOException e) {
				NodeLogger.getNodeLogger(FileUtil.class).warn("Error while closing stream", e);
			}
		}
	}

	/**
	 * Recursively remove all files and directories that are children of the
	 * given directory, but do not remove the directory itself.
	 * @param directory directory to clean
	 * @return true when the directory was cleaned, false if not
	 */
	public static boolean cleanDirectory(File directory) {
		if (directory == null || !directory.exists() || !directory.isDirectory()) {
			return false;
		}
		File[] files = directory.listFiles();

		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				if (!deleteDirectory(files[i])) {
					return false;
				}
			} else {
				if (!files[i].delete()) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Recursively remove the given directory
	 * @param directory directory to remove
	 * @return true when the directory was successfully removed, false if not
	 */
	public static boolean deleteDirectory(File directory) {
		if (!cleanDirectory(directory)) {
			return false;
		}

		return directory.delete();
	}

	/**
	 * Recursively remove all empty subdirectories (directories that contain nothing but empty directories). The given directory will not be removed.
	 * @param directory directory
	 * @return true if the directory is completely empty after cleaning, false if not
	 */
	public static boolean removeEmptySubDirectories(File directory) {
		if (directory == null || !directory.exists() || !directory.isDirectory()) {
			return false;
		}

		boolean isEmpty = true;
		// get all files/directories in the dir
		File[] files = directory.listFiles();

		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				// recurse into directories
				if (removeEmptySubDirectories(files[i])) {
					// if the directory is empty, remove it
					files[i].delete();
				}
			} else {
				// found a file, so the dir is not empty
				isEmpty = false;
			}
		}

		return isEmpty;
	}

	/**
	 * Load the properties from the given input stream in UTF-8 encoding. The
	 * input stream will not be closed.
	 * @param inputStream input stream of utf-8 encoded properties
	 * @return loaded properties
	 * @throws IOException
	 */
	public static Properties loadProperties(InputStream inputStream) throws IOException {
		return loadProperties(inputStream, new Properties(), "UTF-8");
	}

	/**
	 * Load the properties from the given input stream in the given encoding.
	 * The input stream will not be closed.
	 * @param inputStream input stream of encoded properties
	 * @param encoding name of the encoding to use
	 * @return loaded properties
	 * @throws IOException
	 */
	public static Properties loadProperties(InputStream inputStream, String encoding) throws IOException {
		return loadProperties(inputStream, new Properties(), encoding);
	}

	/**
	 * Load the properties from the given input stream in UTF-8 encoding into
	 * the given properties object. The input stream will not be closed.
	 * @param inputStream input stream of encoded properties
	 * @param properties where to load the properties
	 * @return loaded properties
	 * @throws IOException
	 */
	public static Properties loadProperties(InputStream inputStream, Properties properties) throws IOException {
		return loadProperties(inputStream, properties, "UTF-8");
	}

	/**
	 * This method is used to copy a file to a destination
	 * 
	 * @param source
	 * @param dest
	 * @throws IOException
	 */
	public static void copy(File source, File dest) throws IOException {
		byte[] buffer = new byte[4 * 4096];
		int read = 0;

		FileInputStream fIn = null;
		FileOutputStream fOut = null;

		try {
			fIn = new FileInputStream(source);
			fOut = new FileOutputStream(dest);

			while ((read = fIn.read(buffer)) > 0) {
				fOut.write(buffer, 0, read);
			}
		} finally {
			if (fIn != null) {
				try {
					fIn.close();
				} catch (IOException ignored) {}
			}
			if (fOut != null) {
				try {
					fOut.close();
				} catch (IOException ignored) {}
			}
		}
	}
    
	/**
	 * Load the properties from the given input stream in the given encoding
	 * into the given properties object. The input stream will not be closed.
	 * @param inputStream input stream of encoded properties
	 * @param properties where to load the properties
	 * @param encoding name of the encoding to use
	 * @return loaded properties
	 * @throws IOException
	 */
	public static Properties loadProperties(InputStream inputStream, Properties properties,
			String encoding) throws IOException {
		StringBuffer fileContent = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, encoding));
		String readLine = null;

		while ((readLine = reader.readLine()) != null) {
			fileContent.append(readLine).append("\n");
		}

		// now output the content into a byte array (use encoding
		// iso-8859-1)
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Writer outWriter = new OutputStreamWriter(out, "ISO-8859-1");

		outWriter.write(StringUtils.encodeWithUnicode(fileContent.toString()));
		outWriter.flush();
		byte[] binData = out.toByteArray();

		properties.load(new ByteArrayInputStream(binData));
		return properties;
	}
	
	/**
	 * Sanitizes path name.
	 * Keeps sure that the path length does not exceed the length of 255 characters
	 * @param path the folder path
	 * @param preservedCharacters characters that should not be sanitized
	 * @param sanitizeCharacterMap the map of character replacements
	 * @param replacementCharacter the replacement character for not allowed characters
	 * @return the sanitized path name
	 * @throws NodeException
	 */
	public static String sanitizeFolderPath(String path, Map<String, String> sanitizeCharacterMap, String replacementCharacter, String[] preservedCharacters)
			throws NodeException {
		return sanitizeName(path, MAX_PATH_LENGTH, ALLOWED_CHARACTERS_FOLDER_DIR_REG_EXP, null, sanitizeCharacterMap, replacementCharacter, preservedCharacters, true);
	}
	
	/**
	 * Sanitizes the filename of uploaded files.
	 * Keeps sure that the filename length does not exceed the length of 64 characters
	 * and that only allowed characters are in the filename.
	 * The extension, that is preserved for the max length cut, will be calculated using 
	 * org.apache.commons.FileNameUtils.getExtension(filename) after the sanitizing.
	 * @param filename name of the uploaded file
	 * @param preservedCharacters characters that should not be sanitized
	 * @param sanitizeCharacterMap the map of character replacements
	 * @param replacementCharacter the replacement character for not allowed characters
	 * @return the sanitized filename
	 * @throws NodeException 
	 */
	public static String sanitizeName(String filename, Map<String, String> sanitizeCharacterMap, String replacementCharacter, String[] preservedCharacters)
			throws NodeException {
		return sanitizeName(filename, MAX_FILE_NAME_LENGTH, ALLOWED_CHARACTERS_REG_EXP, null, sanitizeCharacterMap, replacementCharacter, preservedCharacters, false);
	}

	/**
	 * Sanitizes the filename of uploaded files and preserves the extension if the filename
	 * ends with the given extension.
	 * Keeps sure that the filename length does not exceed the length of 64 characters
	 * and that only allowed characters are in the filename.
	 * If the extension is empty or null, the extension that will be preserved for the max length cut 
	 * will be calculated using org.apache.commons.FileNameUtils.getExtension(filename) after the sanitizing.
	 * @param filename name of the uploaded file
	 * @param extension the extension to preserve
	 * @param preservedCharacters characters that should not be sanitized
	 * @param sanitizeCharacterMap the map of character replacements
	 * @param replacementCharacter the replacement character for not allowed characters
	 * @return the sanitized filename
	 * @throws NodeException 
	 */
	public static String sanitizeName(String filename, String extension, Map<String, String> sanitizeCharacterMap, String replacementCharacter, String[] preservedCharacters)
			throws NodeException {
		return sanitizeName(filename, MAX_FILE_NAME_LENGTH, ALLOWED_CHARACTERS_REG_EXP, extension, sanitizeCharacterMap, replacementCharacter, preservedCharacters, false);
	}

	/**
	 * @param filename the filename to sanitize
	 * @param maxLength the max length of the filename
	 * @param allowedCharactersRegularExpression a regular expression for characters that are allowed
	 * @param extension the file name extension that will not be sanitized
	 * @param preservedCharacters characters that should not be sanitized
	 * @param isFolderPath set to true if it is a folder path. the path separator will be preserved
	 * @param sanitizeCharacterMap the map of character replacements
	 * @param replacementCharacter the replacement character for not allowed characters
	 * @return the sanitized filename
	 * @throws NodeException 
	 */
	private static String sanitizeName(String filename, int maxLength, String allowedCharactersRegularExpression, String extension, final Map<String, String> sanitizeCharacterMap, String replacementCharacter, String[] preservedCharacters, boolean isFolderPath)
			throws NodeException {
		if (filename == null) {
			throw new NodeException("filename cannot be null");
		} else if ("".equals(filename) || "".equals(filename.trim())) {
			return ObjectTransformer.isEmpty(extension) ? "1" : "1" + extension;
		}

		String allowedCharactersRegEx = null;
		if (!ObjectTransformer.isEmpty(preservedCharacters)) {
			String preservedCharactersRegex = Pattern.quote(org.apache.commons.lang.StringUtils.join(preservedCharacters));
			allowedCharactersRegEx = allowedCharactersRegularExpression.replace("#insert#", preservedCharactersRegex);
		} else {
			allowedCharactersRegEx = allowedCharactersRegularExpression.replace("#insert#", "");
		}
		String newFileName = filename.trim();

		// check if there is an extension to preserve
		String extensionToPreserve = "";
		if (!isFolderPath && StringUtils.isEmpty(extension)) {
			extension = FilenameUtils.getExtension(newFileName);
		}
		if (!StringUtils.isEmpty(extension)) {
			// remove trailing whitespace before checking .endsWith because we
			// trimmed before
			extension = extension.replaceAll("\\s+$", "");

			// make sure, the extension starts with "."
			if (!extension.startsWith(".")) {
				extension = "." + extension;
			}

			if (newFileName.endsWith(extension)) {
				extensionToPreserve = extension;
				newFileName = newFileName.substring(0, newFileName.lastIndexOf(extension));
				if (extension.startsWith(".")) {
					if (extension.length() > 1) {
						extensionToPreserve = extension.substring(1, extension.length());
					} else {
						extensionToPreserve = "";
					}

				} else {
					if (newFileName.endsWith(".")) {
						if (newFileName.length() > 1) {
							newFileName = newFileName.substring(0, newFileName.length() - 1);
						} else {
							newFileName = "";
						}
					}
				}
			}
		}

		// do character replacement
		// replace characters from sanitizeCharacterMap

		//split the extension at the dots and sanitize the parts
		String[] extensionParts = null;
		if (!ObjectTransformer.isEmpty(extensionToPreserve)) {
			if (extensionToPreserve.contains(".")) {
				extensionParts = extensionToPreserve.split("\\.");
			}
		}

		if (!ObjectTransformer.isEmpty(sanitizeCharacterMap)) {
		for (Entry<String, String> entry : sanitizeCharacterMap.entrySet()) {
				if (!isFolderPath || !"/".equals(entry.getKey())) {
				newFileName = newFileName.replace(entry.getKey(), entry.getValue());
					if (extensionParts != null) {
						for (int i = 0; i < extensionParts.length; i++) {
							extensionParts[i] = extensionParts[i].replace(entry.getKey(), entry.getValue());
						}
					} else {
						if (!ObjectTransformer.isEmpty(extensionToPreserve)) {
							extensionToPreserve = extensionToPreserve.replace(entry.getKey(), entry.getValue());
						}
					}
			}
		}
		}

		// replace characters with allowed characters regex but preserve
		// otherwise specified ones
		if (replacementCharacter == null) {
			replacementCharacter = DEFAULT_REPLACEMENT_CHARACTER;
		}
		newFileName = newFileName.replaceAll(allowedCharactersRegEx, replacementCharacter);
		if (extensionParts != null) {
			for (int i = 0; i < extensionParts.length; i++) {
				extensionParts[i] = extensionParts[i].replaceAll(allowedCharactersRegEx, replacementCharacter);
			}
			extensionToPreserve = org.apache.commons.lang.StringUtils.join(extensionParts, ".");
		} else {
			if (!ObjectTransformer.isEmpty(extensionToPreserve)) {
				extensionToPreserve = extensionToPreserve.replaceAll(allowedCharactersRegEx, replacementCharacter);
			}
		}

		// save file extension (if any) for max length cut
		if (!isFolderPath && "".equals(extensionToPreserve)) {
			String filenameExtension = FilenameUtils.getExtension(newFileName);
			if (!"".equals(filenameExtension)) {
				extensionToPreserve = filenameExtension;
				newFileName = newFileName.substring(0, newFileName.lastIndexOf(extensionToPreserve));
				if (newFileName.endsWith(".")) {
					if (newFileName.length() > 1) {
						newFileName = newFileName.substring(0, newFileName.lastIndexOf('.'));
					}
				}
			}
		}

		// Check for the filename length and cut it automatically
		// If the file it has an extension, subtract 1 from maxLenght for the
		// filename separator '.'
		if (extensionToPreserve != null && extensionToPreserve.length() > 1 && !extensionToPreserve.startsWith(".")) {
			maxLength--;
		}
		if (newFileName.length() + extensionToPreserve.length() > maxLength) {

			int newFileNameLength = maxLength - extensionToPreserve.length();

			if (newFileNameLength > 0) {
				newFileName = newFileName.substring(0, newFileNameLength);
			} else {
				// extension exceeds max length, assuming that the extension is wrong
				// we just cut the filename with the extension
				newFileName = (newFileName + "." + extensionToPreserve).substring(0, maxLength + 1);
				extensionToPreserve = "";
				}
			}
		if (filename.endsWith(".") && ".".equals(extension)) {
			newFileName = newFileName + extension;
		} else if (".".equals(newFileName) && !extensionToPreserve.startsWith(".")) {
			newFileName = newFileName + extensionToPreserve;
		} else {
			newFileName = newFileName + ("".equals(extensionToPreserve) ? "" : "." + extensionToPreserve);
		}
		// ensure that filename is not hidden.
		if (newFileName.startsWith(".")) {
			newFileName = "1" + newFileName;
		}

		return newFileName;
	}

	/**
	 * Determines a files mimetype by inputStream and fileName.
	 * This first calls getMimeTypeByContent() and tries to determine the mimetype
	 * by the file content. If that fails it tries to determine the mimetype by the
	 * files extension.
	 * This does not work with for .svg images at the moment.
	 *
	 * @param inputStream  InputStream of the file
	 * @param fileName     The filename including the extension
	 * @return The mimetype in the format "type/subtype"
	 */
	public static String getMimeType(InputStream inputStream, String fileName) {
		String mimeType = getMimeTypeByContent(inputStream, fileName);

		// If we got application/octet-stream, try to identify the mimetype
		// by the file extension as last attempt.
		// If we got something starting with application/x-tika-, do the same
		// - this means Tika could not parse the file contents
		if (DEFAULT_MIME_TYPE.equals(mimeType) 
				|| (org.apache.commons.lang.StringUtils.isNotBlank(mimeType) 
						&& mimeType.startsWith("application/x-tika-"))) {
			mimeType = FileUtil.getMimeTypeByExtension(fileName);
		}

		return mimeType;
	}

	/**
	 * Determines a files mimetype by inputStream and fileName.
	 * The passed InputStream will be read in order to identify the mimetype.
	 * This does not work for all .svg images at the moment.
	 *
	 * @param inputStream  InputStream of the file
	 * @param fileName     The filename including the extension
	 * @return The mimetype in the format "type/subtype"
	 */
	public static String getMimeTypeByContent(InputStream inputStream, String fileName) {
		TikaConfig config = TikaConfig.getDefaultConfig();
		Detector detector = config.getDetector();
		
		try(TikaInputStream tikaInputStream = TikaInputStream.get(inputStream)) {
			Metadata metadata = new Metadata();
			metadata.add(Metadata.RESOURCE_NAME_KEY, fileName);
			String mimeType = detector.detect(tikaInputStream, metadata).toString();

			return mimeType;
		} catch (Exception e1) {
			return DEFAULT_MIME_TYPE;
		}
	}

	/**
	 * Determines the filenames mimetype.
	 * This uses the mime.types files for lookup, which comes with node-lib.
	 * 
	 * @param fileName The filename including the extension
	 * @return The mimetype in the format "type/subtype"
	 */
	public static String getMimeTypeByExtension(String fileName) {
		return MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fileName);
	}

	/**
	 * Determines the extension for a given mimetype
	 * 
	 * @param mimeTypeString The mimetype string including the extension
	 * @return The mimetype in the format "type/subtype"
	 */
	public static String getExtensionByMimeType(String mimeTypeString) {
		String extension = null;

		try {
			TikaConfig config = TikaConfig.getDefaultConfig();
			MimeTypes mimeTypes = config.getMimeRepository();
			MimeType mimeType = mimeTypes.forName(mimeTypeString);
			extension = mimeType.getExtension();
		} catch (MimeTypeException e) {
		}

		if (extension != null && extension.isEmpty()) {
			extension = null;
		}

		return extension;
	}

	/**
	 * Silently close the given input stream, if its not null.
	 * Do nothing when the stream is null, catch and ignore any thrown exception
	 * @param in input stream
	 */
	public static void close(InputStream in) {
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {// ignored
			}
		}
	}

	/**
	 * Silently close the given output stream, if its not null.
	 * Do nothing when the stream is null, catch and ignore any thrown exception
	 * @param out output stream
	 */
	public static void close(OutputStream out) {
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {// ignored
			}
		}
	}

	/**
	 * BufferFactory, that creates buffers of the configured size
	 */
	protected static class BufferFactory extends BasePoolableObjectFactory<byte[]> {

		/* (non-Javadoc)
		 * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
		 */
		public byte[] makeObject() throws Exception {
			return new byte[ObjectTransformer.getInt(System.getProperty(BUFFERSIZE_PARAM), DEFAULT_BUFFERSIZE)];
		}
	}
}
