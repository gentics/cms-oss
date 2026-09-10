package com.gentics.contentnode.rest.resource.impl;

import java.io.StringWriter;
import java.util.List;
import java.util.regex.Pattern;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.model.request.DaisyDiffRequest;
import com.gentics.contentnode.rest.model.request.DiffRequest;
import com.gentics.contentnode.rest.model.response.DiffResponse;
import com.gentics.contentnode.rest.resource.DiffResource;
import com.gentics.contentnode.string.CNStringUtils;
import com.gentics.lib.etc.StringUtils;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.StringTemplateSource;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * This resource implements the diff tool in the REST API, which can be used to
 * calculate the diff between html contents.
 */
@Produces({ MediaType.APPLICATION_JSON })
@Path("/diff")
public class DiffResourceImpl implements DiffResource {

	/**
	 * Shared instance of Handlebars used in calls to
	 * {@link DiffResourceImpl#diffHTML(DiffRequest)} and
	 * {@link DiffResourceImpl#diffSource(DiffRequest)}.
	 */
	protected static Handlebars diffEngine = new Handlebars();

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.DiffResource#daisyDiff(com.gentics.contentnode.rest.model.request.DaisyDiffRequest)
	 */
	@POST
	@Path("/daisyDiff")
	public DiffResponse daisyDiff(DaisyDiffRequest request) {
		DiffResponse response = new DiffResponse();

		try {
			String older = request.getOlder();
			String newer = request.getNewer();

			if (null != request.getIgnoreRegex()) {
				Pattern ignorePattern = Pattern.compile(request.getIgnoreRegex());

				older = ignorePattern.matcher(older).replaceAll("");
				newer = ignorePattern.matcher(newer).replaceAll("");
			}
			response.setDiff(CNStringUtils.daisyDiff(older, newer));
		} catch (Exception e) {
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while rendering DaisyDiff").build());
		}
		return response;
	}
    
	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.DiffResource#diffHTML(com.gentics.contentnode.rest.model.request.DiffRequest)
	 */
	@POST
	@Path("/html")
	public DiffResponse diffHTML(DiffRequest request) {
		try {
			DiffResponse response = new DiffResponse();

			response.setDiff(renderDiff(request, false));
			return response;
		} catch (Exception e) {
			throw new WebApplicationException(e, Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while rendering html diff").build());
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.DiffResource#diffSource(com.gentics.contentnode.rest.model.request.DiffRequest)
	 */
	@POST
	@Path("/source")
	public DiffResponse diffSource(DiffRequest request) {
		try {
			DiffResponse response = new DiffResponse();

			response.setDiff(renderDiff(request, true));
			return response;
		} catch (Exception e) {
			throw new WebApplicationException(e, Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while rendering source diff").build());
		}
	}

	/**
	 * Render the diff from the contents of the given request
	 * 
	 * @param request
	 *            diff request
	 * @param diffSource
	 *            true when the diff shall be shown in source code, false for
	 *            html
	 * @return rendered diff
	 * @throws Exception
	 */
	protected String renderDiff(DiffRequest request, boolean diffSource) throws Exception {
		// first generate the diff
		List diff = StringUtils.diffHTMLStrings(request.getContent1(), request.getContent2(), diffSource, request.getIgnoreRegex());

		// prepare the diff output
		StringWriter diffWriter = new StringWriter();

		// initialize the templates
		Template changeTemplate = diffEngine.compile(new StringTemplateSource("change",
				ObjectTransformer.getString(request.getChangeTemplate(), DiffRequest.DEFAULT_CHANGE_TEMPLATE)));
		Template insertTemplate = diffEngine.compile(new StringTemplateSource("insert",
				ObjectTransformer.getString(request.getInsertTemplate(), DiffRequest.DEFAULT_INSERT_TEMPLATE)));
		Template removeTemplate = diffEngine.compile(new StringTemplateSource("remove",
				ObjectTransformer.getString(request.getRemoveTemplate(), DiffRequest.DEFAULT_REMOVE_TEMPALTE)));
		int wordsBefore = ObjectTransformer.getInt(request.getWordsBefore(), DiffRequest.DEFAULT_WORDS_BEFORE);
		int wordsAfter = ObjectTransformer.getInt(request.getWordsAfter(), DiffRequest.DEFAULT_WORDS_AFTER);

		// iterate over the diff
		int i = 0;

		for (Object element : diff) {
			if (element instanceof String) {
				// constant parts are just added to the output (no change
				// found here)
				diffWriter.write(element.toString());
			} else if (element instanceof StringUtils.DiffPart) {
				// found a difference here
				StringUtils.DiffPart diffPart = (StringUtils.DiffPart) element;
				String original = diffPart.getOriginal();
				String modified = diffPart.getModified();

				String remove = ObjectTransformer.getString(original, "");

				String insert = ObjectTransformer.getString(modified, "");

				// get the surrounding elements
				String before = getWords(diff, i - wordsBefore, i);
				String after = getWords(diff, i + 1, i + 1 + wordsAfter);

				// populate the context
				Context ctx = Context.newBuilder(null)
					.combine("insert", insert)
					.combine("remove", remove)
					.combine("before", before)
					.combine("after", after)
					.build();

				switch (diffPart.getDiffType()) {
				case StringUtils.DiffPart.TYPE_CHANGE:
					changeTemplate.apply(ctx, diffWriter);
					break;

				case StringUtils.DiffPart.TYPE_INSERT:
					insertTemplate.apply(ctx, diffWriter);
					break;

				case StringUtils.DiffPart.TYPE_REMOVE:
					removeTemplate.apply(ctx, diffWriter);
					break;

				default:
					break;
				}
			}

			i++;
		}

		return diffWriter.toString();
	}

	/**
	 * Helper Method to get a specific portion of the diff
	 * @param diffList whole diff
	 * @param start start index in the diff list
	 * @param end end index in the diff list
	 * @return the portion of the diff between start and end
	 */
	private String getWords(List diffList, int start, int end) {
		StringBuffer out = new StringBuffer();

		for (int i = Math.max(0, start); i < Math.min(diffList.size(), end); i++) {
			Object element = diffList.get(i);

			if (element instanceof String) {
				out.append(element);
			} else if (element instanceof StringUtils.DiffPart) {
				String modified = ((StringUtils.DiffPart) element).getModified();

				if (!StringUtils.isEmpty(modified)) {
					out.append(modified);
				}
			}
		}

		return out.toString();
	}
}
