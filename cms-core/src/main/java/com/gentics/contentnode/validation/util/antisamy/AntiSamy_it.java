/*
 * @author tobiassteiner
 * @date Jan 30, 2011
 * @version $Id: AntiSamy_it.java,v 1.1.2.1 2011-02-10 13:43:38 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util.antisamy;

import java.util.Locale;

import com.gentics.contentnode.validation.util.FallbackResourceBundle;

/**
 * @see AbstractAntiSamyValidator which loads resource bundles from here.
 */
public class AntiSamy_it extends FallbackResourceBundle {
	public AntiSamy_it() {
		super("AntiSamy", new Locale("it", "IT"));
	}
}
