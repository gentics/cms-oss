package com.gentics.contentnode.rest.resource.impl;

import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.RestrictiveDiffUberspector;
import com.gentics.contentnode.rest.model.request.DaisyDiffRequest;
import com.gentics.contentnode.rest.model.request.DiffRequest;
import com.gentics.contentnode.rest.model.response.DiffResponse;
import com.gentics.contentnode.rest.resource.DiffResource;
import com.gentics.contentnode.string.CNStringUtils;
import com.gentics.lib.etc.StringUtils;

/**
 * This resource implements the diff tool in the REST API, which can be used to
 * calculate the diff between html contents.
 */
@Produces({ MediaType.APPLICATION_JSON })
@Path("/diff")
public class DiffResourceImpl implements DiffResource {

	/**
	 * Shared instance of the VelocityEngine used in calls to
	 * {@link DiffResourceImpl#diffHTML(DiffRequest)} and
	 * {@link DiffResourceImpl#diffSource(DiffRequest)}.
	 */
	protected static VelocityEngine diffEngine;

	/**
	 * Get the shared instance of the VelocityEngine.
	 * @return VelocityEngine
	 * @throws Exception
	 */
	protected static synchronized VelocityEngine getDiffEngine() throws Exception {
		if (diffEngine == null) {
			// initialize a new VelocityEngine, that has a very restrictive Uberspector and uses the StringResourceLoader
			Properties vtlProps = new Properties();

			vtlProps.setProperty("runtime.introspector.uberspect", RestrictiveDiffUberspector.class.getName());
			vtlProps.setProperty("resource.loader", "string");
			vtlProps.setProperty("string.resource.loader.description", "Velocity StringResource loader");
			vtlProps.setProperty("string.resource.loader.class", "org.apache.velocity.runtime.resource.loader.StringResourceLoader");
			vtlProps.setProperty("string.resource.loader.repository.class", "org.apache.velocity.runtime.resource.loader.StringResourceRepositoryImpl");

			diffEngine = new VelocityEngine(vtlProps);
		}
		return diffEngine;
	}

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
	 * @throws ParseErrorException
	 * @throws ResourceNotFoundException
	 */
	protected String renderDiff(DiffRequest request, boolean diffSource) throws ResourceNotFoundException, ParseErrorException, Exception {
		// first generate the diff
		List diff = StringUtils.diffHTMLStrings(request.getContent1(), request.getContent2(), diffSource, request.getIgnoreRegex());

		// prepare the diff output
		StringWriter diffWriter = new StringWriter();

		// initialize the templates
		Template changeTemplate = parseTemplate(ObjectTransformer.getString(request.getChangeTemplate(), DiffRequest.DEFAULT_CHANGE_TEMPLATE));
		Template insertTemplate = parseTemplate(ObjectTransformer.getString(request.getInsertTemplate(), DiffRequest.DEFAULT_INSERT_TEMPLATE));
		Template removeTemplate = parseTemplate(ObjectTransformer.getString(request.getRemoveTemplate(), DiffRequest.DEFAULT_REMOVE_TEMPALTE));
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
				VelocityContext ctx = new VelocityContext();
				ctx.put("preDel", "");
				ctx.put("postDel", "");
				ctx.put("preIns", "");
				ctx.put("postIns", "");

				// found a difference here
				StringUtils.DiffPart diffPart = (StringUtils.DiffPart) element;
				String original = diffPart.getOriginal();
				String modified = diffPart.getModified();

				String remove = ObjectTransformer.getString(original, "");
				String insert = ObjectTransformer.getString(modified, "");

				// get the surrounding elements
				String before = getWords(diff, i - wordsBefore, i);
				String after = getWords(diff, i + 1, i + 1 + wordsAfter);

				// fix the broken tags, if those exist in the distinct change
				Pair<Optional<String>, Optional<String>> removeTags = findBrokenMarkupTags(remove);
				Pair<Optional<String>, Optional<String>> insertTags = findBrokenMarkupTags(insert);
				if (removeTags.getLeft().isPresent()) {
					ctx.put("preDel", "</" + removeTags.getLeft().get() + ">");
					remove = "<" + removeTags.getLeft().get() + ">" + remove;
				}
				if (removeTags.getRight().isPresent()) {
					ctx.put("postDel", "<" + removeTags.getRight().get() + ">");
					remove = remove + "</" + removeTags.getLeft().get() + ">";
				}
				if (insertTags.getRight().isPresent()) {
					ctx.put("postIns", "<" + insertTags.getRight().get() + ">");
					insert = insert + "</" + insertTags.getRight().get() + ">";
				}
				if (insertTags.getLeft().isPresent()) {
					ctx.put("preIns", "</" + insertTags.getLeft().get() + ">");
					insert = "<" + insertTags.getLeft().get() + ">" + insert;
				}

				// populate the context
				ctx.put("insert", insert);
				ctx.put("remove", remove);
				ctx.put("before", before);
				ctx.put("after", after);

				switch (diffPart.getDiffType()) {
				case StringUtils.DiffPart.TYPE_CHANGE:
					changeTemplate.merge(ctx, diffWriter);
					break;

				case StringUtils.DiffPart.TYPE_INSERT:
					insertTemplate.merge(ctx, diffWriter);
					break;

				case StringUtils.DiffPart.TYPE_REMOVE:
					removeTemplate.merge(ctx, diffWriter);
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
	 * Check if a change contains broken tags, and return a pair of first closing and last opening tags, if those exist.
	 * 
	 * @param change
	 * @return
	 */
	private Pair<Optional<String>, Optional<String>> findBrokenMarkupTags(String change) {
		Optional<String> maybeOpening = Optional.empty();
		Optional<String> maybeClosing = Optional.empty();

		if (StringUtils.isEmpty(change)) {
			return Pair.of(maybeClosing, maybeOpening);
		}
		int firstOpeningTagPos = change.indexOf("<");
		int firstClosingTagPos = change.indexOf("</");
		int lastOpeningTagPos = change.lastIndexOf("<");
		int lastClosingTagPos = change.lastIndexOf("</");

		if (firstOpeningTagPos > -1 && firstClosingTagPos > -1 && (firstOpeningTagPos+2) > firstClosingTagPos) {
			int endPos = change.indexOf(" ", firstClosingTagPos+2);
			if (endPos < 0) {
				endPos = change.indexOf(">", firstClosingTagPos+2);
			}
			if (endPos > firstClosingTagPos+2) {
				String tag = change.substring(firstClosingTagPos+2, endPos);
				if (!"br".equals(tag) && !"img".equals(tag)) {
					maybeClosing = Optional.of(tag);
				}
			}
		}
		if (lastClosingTagPos > -1 && lastOpeningTagPos > -1 && (lastOpeningTagPos+1) > lastClosingTagPos) {
			int endPos = change.indexOf(" ", lastOpeningTagPos+1);
			if (endPos < 0) {
				endPos = change.indexOf(">", lastOpeningTagPos+1);
			}
			if (endPos > lastOpeningTagPos+1) {
				String tag = change.substring(lastOpeningTagPos+1, endPos);
				if (!"br".equals(tag) && !"img".equals(tag)) {
					maybeOpening = Optional.of(tag);
				}
			}
		}
		return Pair.of(maybeClosing, maybeOpening);
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

	/**
	 * Parse the given source into a template
	 * 
	 * @param source
	 *            source to parse into a template
	 * @return Template
	 * @throws ResourceNotFoundException
	 * @throws ParseErrorException
	 * @throws Exception
	 */
	protected Template parseTemplate(String source) throws ResourceNotFoundException, ParseErrorException, Exception {
		VelocityEngine diffEngine = getDiffEngine();
		String templateName = "DiffResource-" + Thread.currentThread().getName();
		StringResourceRepository srr = StringResourceLoader.getRepository();

		srr.putStringResource(templateName, source);
		Template template = diffEngine.getTemplate(templateName);

		srr.removeStringResource(templateName);
		return template;
	}
}
