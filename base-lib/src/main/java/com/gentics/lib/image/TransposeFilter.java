/*
 * @author norbert
 * @date 11.09.2006
 * @version $Id: TransposeFilter.java,v 1.1 2006-09-27 15:16:33 norbert Exp $
 */
package com.gentics.lib.image;

import java.awt.image.renderable.ParameterBlock;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.TransposeDescriptor;
import javax.media.jai.operator.TransposeType;

import org.jmage.filter.ConfigurableImageFilter;
import org.jmage.filter.FilterException;

import com.gentics.lib.log.NodeLogger;

/**
 * The transpose filter 
 */
public class TransposeFilter extends ConfigurableImageFilter {

	/**
	 * type of the transposition
	 */
	public static final String TYPE = "TYPE";

	protected final static Map TYPES_MAP = new HashMap();

	/**
	 * transpose type
	 */
	protected TransposeType type = null;

	/**
	 * the logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(TransposeFilter.class.getName());

	static {
		TYPES_MAP.put("FLIP_VERTICAL", TransposeDescriptor.FLIP_VERTICAL);
		TYPES_MAP.put("FLIP_HORIZONTAL", TransposeDescriptor.FLIP_HORIZONTAL);
		TYPES_MAP.put("FLIP_DIAGONAL", TransposeDescriptor.FLIP_DIAGONAL);
		TYPES_MAP.put("FLIP_ANTIDIAGONAL", TransposeDescriptor.FLIP_ANTIDIAGONAL);
		TYPES_MAP.put("ROTATE_90", TransposeDescriptor.ROTATE_90);
		TYPES_MAP.put("ROTATE_180", TransposeDescriptor.ROTATE_180);
		TYPES_MAP.put("ROTATE_270", TransposeDescriptor.ROTATE_270);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmage.filter.ConfigurableImageFilter#initialize(java.util.Properties)
	 */
	public void initialize(Properties filterProperties) throws FilterException {
		try {
			String typeProp = filterProperties.getProperty(TYPE, null);

			if (logger.isDebugEnabled()) {
				logger.debug("Configured type: {" + typeProp + "}");
			}
			if (typeProp != null) {
				type = (TransposeType) TYPES_MAP.get(typeProp);
			}
		} catch (Throwable t) {
			String message = NOT_INITIALIZED + t.getMessage();

			this.filterProperties = null;
			logger.error(message);
			throw new FilterException(message);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmage.filter.ImageFilter#filter(javax.media.jai.PlanarImage)
	 */
	public PlanarImage filter(PlanarImage image) throws FilterException {
		if (type == null) {
			return image;
		} else {
			ParameterBlock pb = new ParameterBlock();

			pb.addSource(image);
			pb.add(type);
			return JAI.create("transpose", pb, null);
		}
	}
}
