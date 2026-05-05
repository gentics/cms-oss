package com.gentics.contentnode.render;

import java.io.IOException;
import java.io.StringWriter;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.parttype.CmsFormPartType;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.TagType;

public class FormsHandlebarsHelpers {
	/**
	 * Form helper
	 * @param context context
	 * @param options options
	 * @return rendered form
	 * @throws NodeException
	 * @throws IOException
	 */
	public static CharSequence gtx_form(Object context, Options options) throws NodeException, IOException {
		Form form = RenderUtils.getObject(context, Form.class, CmsFormPartType.class, CmsFormPartType::getTarget);
		FormRendering.Options formRenderingOptions = RenderUtils.getHandlebarsHelperObject(FormRendering.Options.class, options);

		if (options.tagType == TagType.SECTION) {
			if (FormRendering.doRender(form)) {
				StringWriter writer = new StringWriter();
				FormRendering.render(form, formRenderingOptions, writer);
				return options.fn(writer.toString());
			} else {
				return options.inverse();
			}
		} else {
			StringWriter writer = new StringWriter();
			FormRendering.render(form, formRenderingOptions, writer);
			return writer.toString();
		}
	}
}
