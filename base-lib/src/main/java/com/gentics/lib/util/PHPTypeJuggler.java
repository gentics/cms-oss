package com.gentics.lib.util;

/**
 * Created by IntelliJ IDEA. User: erwin Date: 03.06.2004 Time: 13:48:49 To
 * change this template use File | Settings | File Templates.
 */
public class PHPTypeJuggler {
	public static String string(Object o) {
		if (o instanceof String) {
			if (o.equals("0")) {
				return "";
			}
			return (String) o;
		}
		if (o instanceof Integer) {
			int i = ((Integer) o).intValue();

			if (i != 0) {
				return Integer.toString(i);
			} else {
				return "";
			}
		}
		if (o instanceof Long) {
			long i = ((Long) o).longValue();

			if (i != 0) {
				return Long.toString(i);
			} else {
				return "";
			}
		}

		return o.toString();
	}
}
