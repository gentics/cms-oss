package com.gentics.contentnode.scheduler;

import static com.gentics.contentnode.object.Folder.FileSearch;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.logger.LogCollector;
import com.gentics.contentnode.logger.StringListAppender;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.layout.PatternLayout;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Scheduler job to convert images to WebP format.
 */
public class ConvertImagesJob {

	private static final PatternLayout PATTERN_LAYOUT = PatternLayout.newBuilder().withPattern("%d %-5p - %m%n").build();
	private static final NodeLogger logger = NodeLogger.getNodeLogger(ConvertImagesJob.class);

	/**
	 * Convert images to WebP format for all nodes which have
	 * {@link com.gentics.contentnode.etc.Feature#WEBP_CONVERSION} enabled.
	 *
	 * @param out Collection for log output.
	 * @return {@code true} if all images were successfully convert, and
	 * 		{@code false} otherwise.
	 */
	public boolean convert(List<String> out) {
		int numConverted = 0;
		FileSearch fileSearch = new FileSearch()
			.setRecursive(true)
			.setMimeType("image/webp")
			.setExcludeMimeType(true);

		out.add("Starting image conversion");
		logger.setLevel(Level.INFO);

		try (LogCollector logCollector = new LogCollector(logger.getName(), new StringListAppender(PATTERN_LAYOUT, out));
				Trx trx = new Trx();
				WastebinFilter wastebin = Wastebin.INCLUDE.set()) {
			for (Node node : trx.getTransaction().getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {

				if (!NodeConfigRuntimeConfiguration.isFeature(Feature.WEBP_CONVERSION, node)) {
					continue;
				}

				logger.info(String.format("Converting images for node {%s}", node));

				for (ImageFile image : node.getFolder().getImages(fileSearch)) {
					Trx.operate(t -> {
						t.setWastebinFilter(trx.getTransaction().getWastebinFilter());

						convert(t.getObject(image, true));
					});

					numConverted++;
				}
			}
		} catch (Exception e) {
			logger.error("Error while converting images to webp: " + e.getMessage(), e);
		}

		out.add(String.format("Image conversion finished, processed %d image%s", numConverted, numConverted == 1 ? "" : "s"));

		return true;
	}

	/**
	 * Convert the given image to WebP format.
	 *
	 * <p>
	 *     NOTE: This method requires an active transaction.
	 * </p>
	 *
	 * @param image The image to convert.
	 * @throws NodeException When the image cannot be converted or saved.
	 */
	private void convert(ImageFile image) throws NodeException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int oldFilesize = image.getFilesize();
		String oldFilename = image.getName();

		try (InputStream input = image.getFileStream()) {
			ImageIO.write(ImageIO.read(input), "webp", baos);
		} catch (IOException e) {
			throw new NodeException(e);
		}

		byte[] convertedData = baos.toByteArray();

		image.setFileStream(new ByteArrayInputStream(convertedData));
		image.setFilesize(convertedData.length);
		image.setFiletype("image/webp");
		image.setName(FilenameUtils.removeExtension(image.getName()) + ".webp");

		image.save();

		logger.info(String.format("Converted image %s (%db) -> %s (%db)", oldFilename, oldFilesize, image.getName(), image.getFilesize()));
	}
}
