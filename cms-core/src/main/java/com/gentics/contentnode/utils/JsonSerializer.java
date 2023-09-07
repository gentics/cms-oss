package com.gentics.contentnode.utils;

import static com.gentics.contentnode.devtools.Synchronizer.mapper;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.lib.util.FileUtil;
import fi.iki.santtu.md5.MD5;
import fi.iki.santtu.md5.MD5InputStream;
import fi.iki.santtu.md5.MD5OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.output.NullOutputStream;

public class JsonSerializer {

	/**
	 * Prettily write the given object as JSON into the given file
	 * @param object object
	 * @param file JSON file
	 * @throws NodeException
	 */
	public static void jsonToFile(Object object, File file) throws NodeException {
		byte[] json = null;
		String jsonMD5 = null;

		// serialize object into json (and generate MD5)
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				MD5OutputStream md5 = new MD5OutputStream(out);
				JsonGenerator jg = new JsonFactory().createGenerator(md5, JsonEncoding.UTF8)) {
			DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n"));
			jg.setPrettyPrinter(prettyPrinter);
			mapper().writeValue(jg, object);

			json = out.toByteArray();
			jsonMD5 = MD5.asHex(md5.hash()).toLowerCase();
		} catch (IOException e) {
			throw new NodeException("Unable to synchronize " + object + " to fs", e);
		}

		// if file exists, check whether MD5 sums are identical
		if (file.exists()) {
			String fileMD5 = getMD5(file);

			if (ObjectTransformer.equals(jsonMD5, fileMD5)) {
				Synchronizer.logger.debug(file + " is already up to date, not sync'ing");
				return;
			}
		}

		// write json into file
		try (InputStream in = new ByteArrayInputStream(json); OutputStream out = new FileOutputStream(file)) {
			Synchronizer.logger.debug("Updating " + file);
			FileUtil.pooledBufferInToOut(in, out);
		} catch (IOException e) {
			throw new NodeException("Unable to synchronize " + object + " to fs", e);
		}
	}


	/**
	 * Determine the md5 sum of the contents of the given file
	 * @param file file
	 * @return md5
	 * @throws NodeException
	 */
	public static String getMD5(File file) throws NodeException {
		try (FileInputStream in = new FileInputStream(file); MD5InputStream md5 = new MD5InputStream(in); OutputStream out = new NullOutputStream()) {
			FileUtil.pooledBufferInToOut(md5, out);
			return MD5.asHex(md5.hash()).toLowerCase();
		} catch (IOException e) {
			throw new NodeException("Error while calculating MD5 of " + file, e);
		}
	}


}
