package com.gentics.lib.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.log.NodeLogger;

/**
 * @author haymo Date: 07.09.2004
 * @version $Id: GenticsContentHelper.java,v 1.1.1.1 2005/01/25 08:54:09 norbert
 *          Exp $
 */
public class GenticsContentHelper {

	public static String getString(GenticsContentObject object, String attributeName) {
		if (object == null) {
			return null;
		}
		if (attributeName == null) {
			return null;
		}
		try {
			GenticsContentAttribute attribute = object.getAttribute(attributeName);

			if (attribute != null) {
				return attribute.toString();
			} else {
				return null;
			}
		} catch (CMSUnavailableException e) {
			NodeLogger.getLogger(GenticsContentHelper.class).error(e.getClass().getName() + ": " + e.getMessage());
		} catch (NodeIllegalArgumentException e) {
			NodeLogger.getLogger(GenticsContentHelper.class).error(e.getClass().getName() + ": " + e.getMessage());
		}

		return null;
	}

	public static int getInt(GenticsContentObject object, String attributeName) {
		if (object == null) {
			return 0;
		}
		try {
			GenticsContentAttribute attribute = object.getAttribute(attributeName);

			if (attribute != null) {
				return ObjectTransformer.getInt(attribute.getNextValue(), 0);
			} else {
				return 0;
			}
		} catch (CMSUnavailableException e) {
			e.printStackTrace();
		} catch (NodeIllegalArgumentException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public static double getDouble(GenticsContentObject object, String attributeName) {
		if (object == null) {
			return 0;
		}
		try {
			GenticsContentAttribute attribute = object.getAttribute(attributeName);

			if (attribute != null) {
				return ObjectTransformer.getDouble(attribute.getNextValue(), 0);
			} else {
				return 0;
			}
		} catch (CMSUnavailableException e) {
			e.printStackTrace();
		} catch (NodeIllegalArgumentException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public static long getLong(GenticsContentObject object, String attributeName) {
		if (object == null) {
			return 0;
		}
		try {
			GenticsContentAttribute attribute = object.getAttribute(attributeName);

			if (attribute != null) {
				return ObjectTransformer.getLong(attribute.getNextValue(), 0);
			} else {
				return 0;
			}
		} catch (CMSUnavailableException e) {
			e.printStackTrace();
		} catch (NodeIllegalArgumentException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public static boolean getBoolean(GenticsContentObject object, String attributeName) {
		if (object == null) {
			return false;
		}
		try {
			GenticsContentAttribute attribute = object.getAttribute(attributeName);

			if (attribute != null) {
				return ObjectTransformer.getBoolean(attribute.getNextValue(), false);
			} else {
				return false;
			}
		} catch (CMSUnavailableException e) {
			e.printStackTrace();
		} catch (NodeIllegalArgumentException e) {
			e.printStackTrace();
		}

		return false;
	}

	public static Object getObject(GenticsContentObject object, String attributeName) {
		if (object == null) {
			return null;
		}
		try {
			return object.getAttribute(attributeName);
		} catch (CMSUnavailableException e) {
			e.printStackTrace();
		} catch (NodeIllegalArgumentException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Collection toStringCollection(GenticsContentObject object,
			String attributeName) {
		try {
			GenticsContentAttribute attrib = object.getAttribute(attributeName);
			ArrayList list;

			if (attrib != null) {
				Iterator it = attrib.valueIterator();

				list = new ArrayList(attrib.countValues());
				while (it.hasNext()) {
					String str = (String) it.next();

					list.add(str);
				}
				return list;
			}
		} catch (CMSUnavailableException e) {
			e.printStackTrace();
		} catch (NodeIllegalArgumentException e) {
			e.printStackTrace();
		}
		return new ArrayList();
	}
}
