package com.gentics.contentnode.tests.edit;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;

/**
 * Abstract base class for some editing tests that test dirting with the help of pages containing vtl tags.
 * Since vtl does not exist any more, those pages are migrated to tags containing hbs via the method {@link AbstractEditSandboxTest#migrateVtlPagesToHbsPages()}.
 */
public abstract class AbstractEditSandboxTest {
	protected static Pattern FILE_LOADER = Pattern.compile("\\$cms\\.imps\\.loader\\.getFile\\((\\d+)\\)\\.(.*)");

	protected static Pattern FOLDER_LOADER = Pattern.compile("\\$cms\\.imps\\.loader\\.getFolder\\((\\d+)\\)\\.(.*)");

	public final static int NODE_ID = 1;

	public final static int VTL_CONSTRUCT_ID = 2;

	/**
	 * Migrate the pages to containing hbs tags
	 * @throws NodeException
	 */
	protected void migrateVtlPagesToHbsPages() throws NodeException {
		Node node = supply(t -> t.getObject(Node.class, NODE_ID));
		int hbsConstructId = supply(() -> createConstruct(node, HandlebarsPartType.class, "hbs", "hbs"));

		List<Integer> pageIds = supply(() -> DBUtils.select("SELECT id FROM page WHERE folder_id IN (?, ?, ?) AND deleted = 0", pst -> {
			pst.setInt(1, 14);
			pst.setInt(2, 15);
			pst.setInt(3, 17);
		}, DBUtils.IDLIST));

		// modify the target pages to replace the vtl tag by a hbs tag
		for (int pageId : pageIds) {
			Page page = supply(t -> t.getObject(Page.class, pageId));
			update(page, upd -> {
				String hbsTemplate = null;
				String tagName = null;

				for (ContentTag tag : upd.getContentTags().values()) {
					if (Objects.equals(tag.getConstructId(), VTL_CONSTRUCT_ID)) {
						String template = getPartType(LongHTMLPartType.class, tag, "template").getText();

						Matcher fileLoaderMatcher = FILE_LOADER.matcher(template);
						Matcher folderLoaderMatcher = FOLDER_LOADER.matcher(template);
						if (fileLoaderMatcher.matches()) {
							int fileId = Integer.parseInt(fileLoaderMatcher.group(1));
							String prop = fileLoaderMatcher.group(2);
							hbsTemplate = "{{#with (gtx_test_file %d)}}{{gtx_render %s}}{{/with}}".formatted(fileId, prop);
							tagName = tag.getName();
							break;
						} else if (folderLoaderMatcher.matches()) {
							int folderId = Integer.parseInt(folderLoaderMatcher.group(1));
							String prop = folderLoaderMatcher.group(2);
							hbsTemplate = "{{#with (gtx_test_folder %d)}}{{gtx_render %s}}{{/with}}".formatted(folderId, prop);
							tagName = tag.getName();
							break;
						}
					}
				}

				if (StringUtils.isNoneBlank(hbsTemplate, tagName)) {
					// remove the old vtl tag
					upd.getContentTags().remove(tagName);

					// create a new hbs tag instead
					ContentTag hbsTag = upd.getContent().addContentTag(hbsConstructId);
					hbsTag.setName(tagName);
					getPartType(HandlebarsPartType.class, hbsTag, "hbs").setText(hbsTemplate);
				}

			}).publish().build();
		}
	}
}
