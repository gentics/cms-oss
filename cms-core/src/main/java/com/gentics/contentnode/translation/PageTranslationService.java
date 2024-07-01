package com.gentics.contentnode.translation;


import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.PageFactory;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;


/**
 * Service to create language page variants
 */
public class PageTranslationService {


	/**
	 * Translate the page into the given language. When the language variant of the page exists, it
	 * is just locked and returned, otherwise the page is copied into the language variant and
	 * returned. This method fails, if the requested language is not available for the node of the
	 * page or the user has no permission to create/edit the given language variant
	 *
	 * @param pageId            id of the page to translate
	 * @param languageCode      code of the language into which the page shall be translated
	 * @param locked            true if the translation shall be locked, false if not
	 * @param channelId         for multichannelling, specify channel in which to create page (can
	 *                          be 0 or equal to node ID to be ignored)
	 * @param requirePermission if false, the permission check on the page to translate is skipped
	 * @return page load response
	 */
	public Page translate(Integer pageId, String languageCode, boolean locked, Integer channelId,
			boolean requirePermission)
			throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (channelId == null) {
			channelId = 0;
		}

		if (channelId != 0) {
			t.setChannelId(channelId);
		}

		try {
			Page page = this.getPage(pageId.toString(), requirePermission);

			Node topMasterNode = page.getFolder().getNode();
			if (topMasterNode.isChannel()) {
				topMasterNode = topMasterNode.getMaster();
			}

			var language = this.getNodeLanguageOrThrow(topMasterNode, languageCode);

			if (requirePermission && (
					!t.getPermHandler().canTranslate(page.getFolder(), Page.class, language) ||
							!t.getPermHandler().canEdit(page.getFolder(), Page.class, language))) {
				throw new InsufficientPrivilegesException(
						"You're not authorized to create and edit a translation for this language",
						page.getFolder(),
						PermType.translatepages);
			}
			Integer channelSetId = null;

			// if a channel ID was given as a query parameter, check if it is
			// part of a valid multichannelling environment
			Page languageVariant = null;
			if (channelId > 0 && (!page.isMaster() || page.isInherited())) {
				Node channel = t.getObject(Node.class, channelId);
				if (channel == null || !channel.isChannel()) {
					throw new NodeException("Error while translating page: an invalid channel ID was given");
				}
				if (!channel.getMasterNodes().contains(topMasterNode)) {
					throw new NodeException(
							"Error while translating page: node {" + channelId + "} is not a channel of node {"
									+ topMasterNode.getId() + "}");
				}

				// if the page should be translated in a channel, the page is
				// first translated in the master node
				Page master = page.getMaster();
				Page masterVariant;

				if (master.getChannel() != null) {
					t.setChannelId(master.getChannel().getId());
					try {
						masterVariant = master.getLanguageVariant(languageCode);
					} finally {
						t.resetChannel();
					}
				} else {
					masterVariant = master.getLanguageVariant(languageCode);
				}

				channelSetId = getChannelSetId(master, masterVariant, languageCode);

				// look for the language variant in the specified channel
				Page variant = page.getLanguageVariant(languageCode, channelId);
				if (variant.getChannel() != null && channelId == ObjectTransformer.getInt(
						variant.getChannel().getId(), -1)) {
					languageVariant = variant;
				}
			} else {
				languageVariant = page.getLanguageVariant(languageCode);
				this.deleteExistingLanguageVariantsWithWastebin(page, languageCode);
			}

			if (languageVariant == null) {
				languageVariant = this.createLanguageVariant(page, language,  channelId, channelSetId);

				if (!locked) {
					languageVariant.unlock();
				}
			} else {
				// language variant exists, return the page in edit mode (if not locked by another user)
				if (requirePermission && !t.getPermHandler().canEdit(page.getFolder(), Page.class, language)) {
					throw new InsufficientPrivilegesException(
							"You're not authorized to edit the translation for this language", page.getFolder(),
							PermType.update);
				}
			}
			t.commit(false);

			return languageVariant;
		} finally {
			if (setChannel) {
				t.resetChannel();
			}
		}
	}

	/**
	 *
	 * @param master the page from the master
	 * @param masterVariant the language variant of the master
	 * @param languageCode the language code for the master variant
	 * @return the channelSetId that connects all objects from channels together
	 * @throws NodeException
	 */
	private Integer getChannelSetId(Page master, Page masterVariant, String languageCode)
			throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Integer channelSetId;
		if (masterVariant == null) {
			t.setDisableMultichannellingFlag(true);
			Node masterPageChannel;
			try {
				masterPageChannel = master.getChannel();
				if (masterPageChannel == null) {
					masterPageChannel = master.getFolder().getNode();
				}
			} finally {
				t.resetDisableMultichannellingFlag();
			}

			try {
				Page translatedMaster = translate(master.getId(), languageCode, false,
						masterPageChannel.getId(), false);
				channelSetId = translatedMaster.getChannelSetId();
			}
			catch (NodeException e) {
				throw new NodeException("Error while translating page: translating the master page failed with: ", e.getMessage());
			}
		} else {
			channelSetId = masterVariant.getChannelSetId();
		}
		return channelSetId;
	}


	/**
	 * Get the page with given id, check whether the page exists. Check for given permissions for
	 * the current user.
	 *
	 * @param pageId            id of the page
	 * @param requirePermission when true a permission check is performed
	 * @return page
	 * @throws NodeException                   when loading the page fails due to underlying error
	 * @throws EntityNotFoundException         when the page was not found
	 * @throws InsufficientPrivilegesException when the user doesn't have a requested permission on
	 *                                         the page
	 */
	public Page getPage(String pageId, boolean requirePermission) throws NodeException {
		if (requirePermission) {
			return MiscUtils.load(Page.class, pageId, ObjectPermission.view);
		}

		return MiscUtils.load(Page.class, pageId);
	}

	/**
	 * Gets the suggested filename for a new page language variant
	 * @param page the source page for which the filename should be suggested
	 * @param languageVariant the page the filename should be applied
	 * @return the suggested filename
	 * @throws NodeException
	 */
	public String getSuggestedFilename(Page page, Page languageVariant) throws NodeException {
		// when the template does not enforce an extension, we will keep the original page's extension by suggesting the filename now
		if (StringUtils.isNotEmpty(languageVariant.getTemplate().getMarkupLanguage().getExtension())) {
			return null;
		}
		var originalFilename = page.getFilename();
		var extensionIndex = originalFilename.lastIndexOf(".");

		if (extensionIndex < 0) {
			return null;
		}
		var extension = originalFilename.substring(extensionIndex + 1);

		// we do not keep the "extension", when it is the language code
		if (page.getLanguage() != null && page.getLanguage().getCode().equalsIgnoreCase(extension)) {
			return null;
		}
		return PageFactory.suggestFilename(languageVariant, p -> extension);
	}


	/**
	 * Check whether the language exists for the given node
	 * @param masterNode the node for which the language is to be checked
	 * @param languageCode the language code that is checked
	 * @return the language if it exists
	 * @throws NodeException if the language does not exist on the given node
	 */
	public ContentLanguage getNodeLanguageOrThrow(Node masterNode, String languageCode)
			throws NodeException {
		List<ContentLanguage> languages = masterNode.getLanguages();

		for (ContentLanguage language : languages) {
			if (StringUtils.equals(languageCode, language.getCode())) {
				return language;
			}
		}

		throw new NodeException(
				String.format("Error while translating page: invalid language code '%s' given",
						languageCode));
	}

	private void deleteExistingLanguageVariantsWithWastebin(Page page, String languageCode) throws NodeException {
		// get the language variant (if one already exists)
		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			List<Page> languageVariants = page.getLanguageVariants(false);

			for (Page currentLanguageVariant : languageVariants) {
				ContentLanguage contentLanguage = currentLanguageVariant.getLanguage();
				if (contentLanguage != null && contentLanguage.getCode().equals(languageCode)
						&& currentLanguageVariant.isDeleted()) {
					// do this in its own transaction (as system user) to avoid permission check
					try (Trx trx = new Trx(); WastebinFilter wb = Wastebin.INCLUDE.set()) {
						if (NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING)
								&& currentLanguageVariant.isMaster()) {
							Collection<Integer> channelVariantIds = currentLanguageVariant.getChannelSet()
									.values();
							List<Page> channelVariants = trx.getTransaction()
									.getObjects(Page.class, channelVariantIds, false, false);
							for (Page channelVariant : channelVariants) {
								channelVariant.delete(true);
							}
						} else {
							currentLanguageVariant.delete(true);
						}
						trx.success();
					}
				}
			}
		}
	}


	/**
	 * Creates a new page for the given target language
	 * @param page the page for which a language variant is to be created
	 * @param targetLanguage the language of the language variant
	 * @param channelId the channel id
	 * @param channelSetId
	 * @return the page for the given target language
	 * @throws NodeException
	 */
	public Page createLanguageVariant(Page page, ContentLanguage targetLanguage, int channelId, Integer channelSetId)
			throws NodeException {
		// create the language variant
		var languageVariant = (Page) page.copy();

		// set the new language
		languageVariant.setLanguage(targetLanguage);
		var getSuggestedFilename = this.getSuggestedFilename(page, languageVariant);
		languageVariant.setFilename(getSuggestedFilename);

		// if the new page is created inside a channel, set its channel
		// information to match the translated master page
		if (channelId != 0 && channelSetId != null) {
			languageVariant.setChannelInfo(channelId, channelSetId);
		}

		// set the language variant to be sync'ed with the original page (but let it point to version 0)
		languageVariant.synchronizeWithPageVersion(page, 0);
		languageVariant.save();

		return  languageVariant;
	}

}
