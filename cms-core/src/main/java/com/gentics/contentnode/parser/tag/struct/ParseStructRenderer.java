/*
 * @author Stefan Hepp
 * @date 15.12.2006
 * @version $Id: ParseStructRenderer.java,v 1.22 2010-06-15 08:40:00 norbert Exp $
 */
package com.gentics.contentnode.parser.tag.struct;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.exception.InconsistentDataException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.parser.ContentRenderer;
import com.gentics.contentnode.parser.tag.ParserTag;
import com.gentics.contentnode.parser.tag.TagParser;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.JavaParserConstants;

/**
 * This is a structure renderer which uses the ParserTags to render the code.
 */
public class ParseStructRenderer implements StructRenderer {

	private static StructRenderer renderer = new ParseStructRenderer();

	/**
	 * logger
	 */
	public final static NodeLogger logger = NodeLogger.getNodeLogger(ParseStructRenderer.class);

	public ParseStructRenderer() {}

	/**
	 * Get a static instance of this renderer.
	 * @return a reference to a static instance.
	 */
	public static StructRenderer getInstance() {
		return renderer;
	}

	/**
	 * Render the code and honor the ParserTags.
	 * The structure information of the tags is used to determine the end of the code.
	 * The tags are rendered using the ParserTags. Endcode is currently ignored, as this
	 * might lead to incoherent tag levels.
	 *
	 * @param parser a reference to the main parser.
	 * @param result the current renderResult.
	 * @param source the source where the code should be appended.
	 * @param template the template of the code.
	 * @param struct the list of codeparts.
	 * @param firstElement the first element in the struct list which should be rendered.
	 * @param splitter the splitter tags to use as closing tags, or null if not set.
	 * @param endCode the endcode for the end tag, or null if not set.
	 * @param omitTags list of tags that shall not be rendered any more
	 * @param omitTagsEdit list of tags that shall not be rendered in edit mode any more
	 * @return the status returncode.
	 */
	public RenderReturnCode renderStruct(TagParser parser, RenderResult result, StringBuffer source,
			String template, List struct, int firstElement, String[] splitter, String endCode, List omitTags, List omitTagsEdit) throws NodeException {
		boolean debugLog = logger.isDebugEnabled();

		if (debugLog) {
			logger.debug("rendering struct of " + struct.size() + " objects, starting with # " + firstElement);
		}

		// TODO: rewrite also the recursive calls to renderStruct to pass on the
		// same instance of StructRenderPosition (do not create a new object on
		// every call)
		StructRenderPosition pos = new StructRenderPosition(firstElement);

		while (pos.getPos() < struct.size()) {
			if (debugLog) {
				logger.debug("rendering part # " + pos.getPos());
			}
			CodePart part = (CodePart) struct.get(pos.getPos());

			if (part instanceof StringPart) {
				if (debugLog) {
					logger.debug("part # " + pos.getPos() + " is a static string");
				}

				source.append(template.substring(part.getStartPos(), part.getEndPos()));
				pos.increment(1);

			} else if (part instanceof TagPart) {
				if (debugLog) {
					logger.debug("part # " + pos.getPos() + " is a tag");
				}

				TagPart tagPart = (TagPart) part;

				if (tagPart.getType() == TagPart.TYPE_END) {
					if (debugLog) {
						logger.debug("found end part, closing this level");
					}

					// this closes this level, return ..
					return new RenderReturnCode(pos.getPos() + 1, RenderReturnCode.RETURN_CLOSED, null);
				} else if (tagPart.getType() == TagPart.TYPE_SPLITTER) {
					if (debugLog) {
						logger.debug("found splitter tag, closing this level");
					}
					// thou shalt be a splitter, exitus.
					String splitterKey = tagPart.matchSplitterCodes(splitter);

					return new RenderReturnCode(pos.getPos(), RenderReturnCode.RETURN_SPLITTER, splitterKey);
				} else {
					if (debugLog) {
						logger.debug("normal tag found");
					}

					RuntimeProfiler.beginMark(JavaParserConstants.PARSER_PARSESTRUCTRENDERER_GETPARSERTAG);
					ParserTag tag = tagPart.getParserTag(parser.getParserTagFactory());

					RuntimeProfiler.endMark(JavaParserConstants.PARSER_PARSESTRUCTRENDERER_GETPARSERTAG);
					if (tag == null) {
						// this tag has no parser!
						result.debug("Invalid tag",
								"Could not find tag with the specified name {" + tagPart.getCode(template) + "}: {" + tagPart.toString() + "}");

						logger.warn("Could not find tag with the specified name {" + tagPart.getCode(template) + "}: {" + tagPart.toString() + "}");

						pos.increment(1);

						continue;
					}

					if (tag.isClosingTag()) {
						if (debugLog) {
							logger.debug("Found closing tag, closing this level");
						}
						// this is a closing tag, end parsing of this level.
						return new RenderReturnCode(pos.getPos() + 1, RenderReturnCode.RETURN_CLOSED, null);
					}

					// check for splitter
					String splitterKey = tagPart.matchSplitterCodes(splitter);

					if (splitterKey != null) {
						if (debugLog) {
							logger.debug("Found splitter tag, closing this level");
						}
						// thou shalt be a splitter, exitus.
						return new RenderReturnCode(pos.getPos() + 1, RenderReturnCode.RETURN_SPLITTER, splitterKey);
					}

					// check for endTag ? quite meaningless, as this would result in invalid open/close tag-levels
					/*
					 if (endCode != null) {
					 if (tagPart.matchEndCode(endCode)) {
					 return new RenderReturnCode(pos+1, RenderReturnCode.RETURN_CLOSED, null);
					 }
					 }
					 // */

					try {
						// TODO add the name of the tag as instance key
						RuntimeProfiler.beginMark(JavaParserConstants.PARSER_PARSESTRUCTRENDERER_RENDERTAG, tag.toString());
						renderTag(parser, result, source, template, pos, struct, tagPart, tag, omitTags, omitTagsEdit);
					} catch (StructParserException e) {
						return e.getReturnCode();
					} catch (InconsistentDataException e) {
						result.info(ParseStructRenderer.class, "Inconsistent tag, skipping - " + e.getMessage());
					} catch (NodeException ne) {
						result.error(ParseStructRenderer.class, "Error while rendering tag, skipping.", ne);
					} finally {
						RuntimeProfiler.endMark(JavaParserConstants.PARSER_PARSESTRUCTRENDERER_RENDERTAG, tag.toString());
					}
				}
			} else {
				// unknown type ..
				result.warn("Invalid tag", "Unknown code-part.");
				pos.increment(1);
			}

		}

		if (debugLog) {
			logger.debug("finished rendering the structure, next pos is # " + pos.getPos());
		}

		return new RenderReturnCode(pos.getPos(), RenderReturnCode.RETURN_LAST, null);
	}

	/**
	 * Render the given tag in editmode, correctly place the edit icon
	 * @param source stringbuffer holding the rendered source
	 * @param code rendered value of the tag
	 * @param tag parser tag
	 * @param omitTags list of tags that shall not be rendered
	 * @param omitTagsEdit list of tags that shall not be rendered in edit mode
	 * @throws NodeException
	 */
	public static void renderEditableTag(StringBuffer source, String code, ParserTag tag, List omitTags, List omitTagsEdit, RenderResult result) throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		// check whether we have an open tag
		int lastTagStart = source.lastIndexOf("<");
		int lastTagEnd = source.lastIndexOf(">");

		if (lastTagStart > lastTagEnd) {
			// if (renderType.getEditMode() != RenderType.EM_ALOHA) {
			// found the tag in an open ml tag
			if (!omitTagsEdit.contains(tag)) {
				// check for aloha mode
				if (renderType.getEditMode() != RenderType.EM_ALOHA) {
					// tag in ml tag, but not inline editable
					StringBuffer replacement = new StringBuffer();

					// prefix
					replacement.append(tag.getEditPrefix());
					// editicon
					replacement.append(tag.getEditLink());
					// postfix
					replacement.append(tag.getEditPostfix());
					// place the editicon for the tag after the
					// lastTagEnd
					source.insert(lastTagEnd + 1, replacement);
				} else {
					// In Aloha Mode add an empty block before the tag
					AlohaRenderer alohaRenderer = (AlohaRenderer) RendererFactory.getRenderer(ContentRenderer.RENDERER_ALOHA);

					source.insert(lastTagEnd + 1, alohaRenderer.block("", tag, result));
				}

				// all further occurrances of the tag will not be rendered in edit mode
				omitTagsEdit.add(tag);
			}
			source.append(code);
			// }
		} else {
			// tag not in ml, not inline editable
			if (!omitTagsEdit.contains(tag)) {
				if (renderType.getEditMode() != RenderType.EM_ALOHA) {
					source.append(tag.getEditPrefix());
					source.append(tag.getEditLink());
					source.append(code);
					source.append(tag.getEditPostfix());
				} else {
					// In Aloha mode check for a direct root
					AlohaRenderer alohaRenderer = (AlohaRenderer) RendererFactory.getRenderer(ContentRenderer.RENDERER_ALOHA);

					source.append(alohaRenderer.block(code, tag, result));
				}
			} else {
				source.append(code);
			}
		}
	}

	/**
	 * Render the closed tag
	 * @param tag tag to be rendered
	 * @param source stringbuffer to accept the rendered tag
	 * @param omitTags list of tags to omit
	 * @param omitTagsEdit list of tags to omit in edit mode
	 * @param result render result
	 * @throws NodeException
	 */
	public static void renderClosedTag(ParserTag tag, StringBuffer source, List<?> omitTags, List<?> omitTagsEdit, RenderResult result) throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
		int editMode = renderType.getEditMode();
		boolean debugLog = logger.isDebugEnabled();

		if (!omitTags.contains(tag)) {

			String code = tag.render(result);

			if (debugLog) {
				logger.debug("rendering {" + tag + "} isEditable {" + tag.isEditable() + "}, editMode {" + RenderType.renderEditMode(editMode) + "}");
			}

			// when edit mode, we possibly need to reposition the rendered
			// tag
			if ((editMode == RenderType.EM_ALOHA) && tag.isEditable()) {
				if (debugLog) {
					logger.debug("rendering {" + tag + "} in editMode {" + editMode + "}");
				}
				renderEditableTag(source, code, tag, omitTags, omitTagsEdit, result);
			} else if (editMode == RenderType.EM_ALOHA_READONLY) {
				if (debugLog) {
					logger.debug("rendering {" + tag + "} in " + RenderType.renderEditMode(editMode) + " mode");
				}
				AlohaRenderer alohaRenderer = (AlohaRenderer) RendererFactory.getRenderer(ContentRenderer.RENDERER_ALOHA);

				source.append(alohaRenderer.block(code, tag, result));
			} else {
				if (debugLog) {
					logger.debug("rendering {" + tag + "} in " + RenderType.renderEditMode(editMode) + " mode");
				}
				source.append(code);
			}
		} else {
			if (debugLog) {
				logger.debug("omiting the tag");
			}
		}
	}

	/**
	 * Render a given parsertag, and parse its template and codeparts if it has splittertags.
	 *
	 * @param parser the reference to the main parser.
	 * @param result the current renderResult.
	 * @param source the source where the code should be added.
	 * @param template the template of the code.
	 * @param pos the position of the tag in the struct list, will be updated to the next unrendered element in the struct list
	 * @param struct the list of codeparts.
	 * @param tagPart the tagpart of this tag.
	 * @param tag the parsertag of this tag.
	 * @param omitTags list of tags that shall not be rendered any more
	 * @param omitTagsEdit list of tags that shall not be rendered in edit mode any more
	 * @throws StructParserException on unrecoverable structural errors.
	 */
	private void renderTag(TagParser parser, RenderResult result, StringBuffer source,
			String template, StructRenderPosition pos, List struct, TagPart tagPart, ParserTag tag, List omitTags, List omitTagsEdit) throws StructParserException, NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
		int editMode = renderType.getEditMode();
		boolean debugLog = logger.isDebugEnabled();

		if (debugLog) {
			logger.debug("Rendering tag @ position # " + pos.getPos());
		}

		boolean isClosed = (tagPart.getType() == TagPart.TYPE_CLOSED);

		if (isClosed) {
			if (debugLog) {
				logger.debug("tag is closed");
			}

			// increment the position before rendering (in case of render errors)
			pos.increment(1);

			renderClosedTag(tag, source, omitTags, omitTagsEdit, result);

		} else {
			if (debugLog) {
				logger.debug("tag is not closed");
			}

			// find end-tag, splitter,..; add code to parsertag and render it..
			String[] tagSplitter = tag.getSplitterTags();

			// if no splitter is available, assume there will also none returned, to save hashmaps
			Map codeParts = tagSplitter != null ? new HashMap(tagSplitter.length) : Collections.EMPTY_MAP;

			String code = "";
			String lastSplitter = null;

			while (true) {

				StringBuffer subCode = new StringBuffer();

				boolean preparseCode = tag.doPreParseCode(parser.doParseInputCode(), lastSplitter);

				if (debugLog) {
					logger.debug("start rendering inner tag structure @ pos #" + pos.getPos() + 1);
				}
				RenderReturnCode retCode = parser.getStructRenderer(preparseCode).renderStruct(parser, result, subCode, template, struct, pos.getPos() + 1,
						tagSplitter, tag.getTagEndCode(), omitTags, omitTagsEdit);

				if (retCode.getReason() == RenderReturnCode.RETURN_LAST) {
					// whoo, unexpected end. somewhere a tag is missing!
					source.append(subCode);
					throw new StructParserException("Unexpected end tag.", retCode);
				}

				if (lastSplitter == null) {
					code = subCode.toString();
				} else {
					// TODO check if last splitter-tag contains a parsertag too, and use it if so.
					// TODO maybe support for multiple occurrence of splitter, or at least error if already set

					codeParts.put(lastSplitter, subCode.toString());
				}

				if (debugLog) {
					logger.debug("end rendering inner tag structure, next pos is # " + retCode.getPos());
				}
				// update loop-pos
				pos.setPos(retCode.getPos());

				if (retCode.getReason() == RenderReturnCode.RETURN_SPLITTER) {
					lastSplitter = retCode.getSplitter();
				} else {
					// end tag, finished with the sub-code
					break;
				}

			}

			if (debugLog) {
				logger.debug("rendering the tag now");
			}
			// now render the thingie..
			code = tag.render(result, code, codeParts);

			if (tag.doPostParseCode(parser.doParseResultCode())) {
				code = parser.render(result, code);
			}

			// when edit mode, we eventually need to reposition the rendered tag
			source.append(code);
		}
	}

	/**
	 * Internal class for keep track of the struct render position
	 */
	protected class StructRenderPosition {

		/**
		 * The position
		 */
		protected int pos = 0;

		/**
		 * Create an instance
		 * @param pos position
		 */
		public StructRenderPosition(int pos) {
			this.pos = pos;
		}

		/**
		 * Increment by the given amount
		 * @param inc amount to increment
		 */
		public void increment(int inc) {
			this.pos += inc;
		}

		/**
		 * Get the position
		 * @return the position
		 */
		public int getPos() {
			return pos;
		}

		/**
		 * Set a new position
		 * @param pos new position
		 */
		public void setPos(int pos) {
			this.pos = pos;
		}
	}
}
