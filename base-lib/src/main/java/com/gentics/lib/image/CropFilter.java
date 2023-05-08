/*
 * @author Christopher
 * @date 08.09.2010
 * @version $Id: CropFilter.java,v 1.2 2010-09-28 17:01:34 norbert Exp $
 */
package com.gentics.lib.image;

import java.awt.image.renderable.ParameterBlock;
import java.util.Properties;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import org.jmage.filter.ConfigurableImageFilter;
import org.jmage.filter.FilterException;

import com.gentics.lib.log.NodeLogger;

public class CropFilter extends ConfigurableImageFilter {

	public static final String TOPLEFTX = "TOPLEFTX";
	public static final String TOPLEFTY = "TOPLEFTY";
	public static final String WIDTH = "WIDTH";
	public static final String HEIGHT = "HEIGHT";

	protected float topleftx;
	protected float toplefty;
	protected float width;
	protected float height;

	protected static NodeLogger log = NodeLogger.getNodeLogger(CropFilter.class.getName());
	private static final String DEFAULT_CROP = "0";
	private static final String CROP_RANGE_ERROR = " value must be greater or equal than 0";
    
	public void initialize(Properties filterProperties) throws FilterException {
		try {
			topleftx = Float.valueOf(filterProperties.getProperty(TOPLEFTX, DEFAULT_CROP)).floatValue();
			if (topleftx < 0) {
				throw new Exception(TOPLEFTX + CROP_RANGE_ERROR);
			}
            
			toplefty = Float.valueOf(filterProperties.getProperty(TOPLEFTY, DEFAULT_CROP)).floatValue();
			if (toplefty < 0) {
				throw new Exception(TOPLEFTY + CROP_RANGE_ERROR);
			}
            
			width = Float.valueOf(filterProperties.getProperty(WIDTH, DEFAULT_CROP)).floatValue();
			if (width < 0) {
				throw new Exception(WIDTH + CROP_RANGE_ERROR);
			}
            
			height = Float.valueOf(filterProperties.getProperty(HEIGHT, DEFAULT_CROP)).floatValue();
			if (height < 0) {
				throw new Exception(HEIGHT + CROP_RANGE_ERROR);
			}

			this.filterProperties = filterProperties;
			if (log.isDebugEnabled()) {
				log.debug(INITIALIZED);
			}
		} catch (Throwable t) {
			String message = NOT_INITIALIZED + t.getMessage();

			this.filterProperties = null;
			log.error(message);
			throw new FilterException(message);
		}
	}

	public PlanarImage filter(PlanarImage image) throws FilterException {

		ParameterBlock pb = new ParameterBlock();

		pb.addSource(image);
		pb.add(topleftx);
		pb.add(toplefty);
		pb.add(width);
		pb.add(height);

		PlanarImage result = (PlanarImage) JAI.create("crop", pb);

		return result;
	}

}
