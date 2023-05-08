/*
 * @author tobiassteiner
 * @date Jan 30, 2011
 * @version $Id: AntiSamy_pt.java,v 1.1.2.1 2011-02-10 13:43:38 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util.antisamy;

import java.util.Locale;

import com.gentics.contentnode.validation.util.FallbackResourceBundle;

/**
 * @see AbstractAntiSamyValidator which loads resource bundles from here.
 */
public class AntiSamy_pt extends FallbackResourceBundle {
	public AntiSamy_pt() {
		super("AntiSamy", new Locale("pt", "PT"));
	}
}
