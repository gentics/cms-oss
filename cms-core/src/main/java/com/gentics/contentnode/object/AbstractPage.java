package com.gentics.contentnode.object;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;

import com.gentics.contentnode.rest.util.MiscUtils;
import org.apache.commons.collections4.SetUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.resolving.ResolvableBean;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.NoObjectTagSync;
import com.gentics.contentnode.factory.PublishedNodeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionStatistics;
import com.gentics.contentnode.factory.object.ExtensiblePublishableObject;
import com.gentics.contentnode.factory.object.PageFactory;
import com.gentics.contentnode.factory.object.TableVersion;
import com.gentics.contentnode.factory.object.TableVersion.Diff;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.publish.CnMapPublisher;
import com.gentics.contentnode.publish.cr.TagmapEntryRenderer;
import com.gentics.contentnode.publish.wrapper.PublishablePage;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.render.TemplateRenderer;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.JavaParserConstants;
import com.gentics.lib.resolving.ResolvableMapWrappable;

public abstract class AbstractPage extends AbstractContentObject implements Page, ExtensiblePublishableObject<Page> {

	private static final long serialVersionUID = 3295096410764805012L;

	/**
	 * static map of resolvable properties
	 */
	protected static Map<String, Property> resolvableProperties;

	protected final static Set<String> resolvableKeys;

	/**
	 * languageset of this page (provides access to all language variants of this page)
	 */
	protected LanguageSet languageSet;

	protected NodeObjectVersion[] pageVersions;

	static {
		resolvableProperties = new HashMap<String, Property>();
		Property page = new Property(null) {
			public Object get(AbstractPage page, String key) {
				return page;
			}
		};

		resolvableProperties.put("seite", page);
		resolvableProperties.put("page", page);        
		resolvableProperties.put("ispage", new Property(null) {
			public Object get(AbstractPage page, String key) {
				return true;
			}
		});
		resolvableProperties.put("url", new Property(null) {
			public Object get(AbstractPage page, String key) {
				try {
					RenderUrl renderUrl;

					renderUrl = TransactionManager.getCurrentTransaction().getRenderType().getRenderUrl(Page.class, page.getId());
					renderUrl.setMode(RenderUrl.MODE_LINK);
					return renderUrl.toString();
				} catch (NodeException e) {
					page.logger.error("could not generate page url", e);
					return null;
				}
			}
		});
		resolvableProperties.put("template", new Property(new String[] { "template_id"}) {
			public Object get(AbstractPage page, String key) {
				try {
					return page.new PageTemplate(page.getTemplate());
				} catch (NodeException e) {
					page.logger.error("Could not retrieve template for page", e);
					return null;
				}
			}
		});
		resolvableProperties.put("template_id", new Property(new String[] { "template_id"}) {
			public Object get(AbstractPage page, String key) {
				try {
					return page.getTemplate().getId();
				} catch (NodeException e) {
					page.logger.error("Could not retrieve template_id for page", e);
					return null;
				}
			}
		});
		resolvableProperties.put("ml_id", new Property(new String[] { "template_id"}) {
			public Object get(AbstractPage page, String key) {
				try {
					return page.getTemplate().getMlId();
				} catch (NodeException e) {
					page.logger.error("Could not retrieve template_id for page", e);
					return null;
				}
			}
		});
		resolvableProperties.put("name", new Property(new String[] { "name"}) {
			public Object get(AbstractPage page, String key) {
				return page.getName();
			}
		});
		resolvableProperties.put("nice_url", new Property(new String[] { "nice_url"}) {
			public Object get(AbstractPage page, String key) {
				return page.getNiceUrl();
			}
		});
		resolvableProperties.put("alternate_urls", new Property(new String[] {"alternate_urls"}) {
			public Object get(AbstractPage page, String key) {
				try {
					return page.getAlternateUrls();
				} catch (NodeException e) {
					page.logger.error("Error while retrieving alternate URLs", e);
					return null;
				}
			}
		});
		resolvableProperties.put("filename", new Property(new String[] { "filename"}) {
			public Object get(AbstractPage page, String key) {
				return page.getFilename();
			}
		});
		Property descriptionProp = new Property(new String[] { "description"}) {
			public Object get(AbstractPage page, String key) {
				return page.getDescription();
			}
		};

		resolvableProperties.put("description", descriptionProp);
		resolvableProperties.put("beschreibung", descriptionProp);
		resolvableProperties.put("priority", new Property(new String[] { "priority"}) {
			public Object get(AbstractPage page, String key) {
				return page.getPriority();
			}
		});
		resolvableProperties.put("folder_id", new Property(new String[] { "folder_id"}) {
			public Object get(AbstractPage page, String key) {
				try {
					return page.getFolder().getId();
				} catch (NodeException e) {
					page.logger.error("could not retrieve folder for page", e);
					return null;
				}
			}
		});
		resolvableProperties.put("node_id", new Property(new String[] { "folder_id"}) {
			public Object get(AbstractPage page, String key) {
				try {
					// add additional dependencies here
					RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

					if (renderType.doHandleDependencies()) {
						renderType.addDependency(page.getFolder(), "node_id");
					}

					return page.getFolder().getNode().getId();
				} catch (NodeException e) {
					page.logger.error("could not retrieve folder/node for page", e);
					return null;
				}
			}
		});
		resolvableProperties.put("tags", new Property(new String[] { "tags"}) {
			public Object get(AbstractPage page, String key) {
				return page.getTagResolver();
			}
		});
		Property publishtimestamp = new Property(new String[] { "pdate"}) {
			public Object get(AbstractPage page, String key) {
				return page.getPDate().getTimestamp();
			}
		};

		resolvableProperties.put("publishtimestamp", publishtimestamp);
		resolvableProperties.put("veroeffentlichungstimestamp", publishtimestamp);
		Property publishdate = new Property(new String[] { "pdate"}) {
			public Object get(AbstractPage page, String key) {
				return page.getPDate();
			}
		};

		resolvableProperties.put("publishdate", publishdate);
		resolvableProperties.put("veroeffentlichungsdatum", publishdate);
		Property creationtimestamp = new Property(new String[] { "cdate", "custom_cdate" }) {
			public Object get(AbstractPage page, String key) {
				return page.getCustomOrDefaultCDate().getTimestamp();
			}
		};

		resolvableProperties.put("creationtimestamp", creationtimestamp);
		resolvableProperties.put("erstellungstimestamp", creationtimestamp);
        
		Property creationdate = new Property(new String[] { "cdate", "custom_cdate" }) {
			public Object get(AbstractPage page, String key) {
				return page.getCustomOrDefaultCDate();
			}
		};

		resolvableProperties.put("creationdate", creationdate);
		resolvableProperties.put("erstellungsdatum", creationdate);
		Property edittimestamp = new Property(new String[] { "edate", "custom_edate" }) {
			public Object get(AbstractPage page, String key) {
				return page.getCustomOrDefaultEDate().getTimestamp();
			}
		};

		resolvableProperties.put("edittimestamp", edittimestamp);
		resolvableProperties.put("bearbeitungstimestamp", edittimestamp);
		Property editdate = new Property(new String[] { "edate", "custom_edate" }) {
			public Object get(AbstractPage page, String key) {
				return page.getCustomOrDefaultEDate();
			}
		};

		resolvableProperties.put("editdate", editdate);
		resolvableProperties.put("bearbeitungsdatum", editdate);
		resolvableProperties.put("expiredate", new Property(new String[] { "time_off"}) {
			public Object get(AbstractPage page, String key) {
				return page.getTimeOff();
			}
		});
		resolvableProperties.put("expiretimestamp", new Property(new String[] { "time_off"}) {
			public Object get(AbstractPage page, String key) {
				return page.getTimeOff().getTimestamp();
			}
		});
		Property keywordProp = new Property(new String[] { "folder_id"}) {
			public Object get(AbstractPage page, String key) {
				try {
					// dependency is already added by caller.
					return page.getKeywordResolvableWithoutDependencies(key);
				} catch (Exception e) {
					page.logger.error("Unable to retrieve KeywordResolvable for key {" + key + "}", e);
					return null;
				}
			}
		};

		resolvableProperties.put("ordner", keywordProp);
		resolvableProperties.put("folder", keywordProp);
		resolvableProperties.put("node", keywordProp);

		// TODO page languages dependencies have to be revised! just setting
		// contentgroup won't work since cg does not change if new pages are
		// added to it
		Property languageSetProp = new Property(new String[] {}) {
			public Object get(AbstractPage page, String key) {
				if (page.languageSet == null) {
					page.languageSet = page.new LanguageSet();
				}

				return page.languageSet;
			}
		};

		resolvableProperties.put("languageset", languageSetProp);

		Property creatorProp = new Property(new String[] { "creator"}) {
			public Object get(AbstractPage page, String key) {
				try {
					return page.getCreator();
				} catch (NodeException e) {
					page.logger.error("could not retrieve creator", e);
					return null;
				}
			}
		};

		resolvableProperties.put("ersteller", creatorProp);
		resolvableProperties.put("creator", creatorProp);
		Property editorProp = new Property(new String[] { "editor"}) {
			public Object get(AbstractPage page, String key) {
				try {
					return page.getEditor();
				} catch (NodeException e) {
					page.logger.error("could not retrieve editor", e);
					return null;
				}
			}
		};

		resolvableProperties.put("bearbeiter", editorProp);
		resolvableProperties.put("editor", editorProp);
		Property publisherProp = new Property(new String[] { "publisher"}) {
			public Object get(AbstractPage page, String key) {
				try {
					return page.getPublisher();
				} catch (NodeException e) {
					page.logger.error("could not retrieve publisher", e);
					return null;
				}
			}
		};

		resolvableProperties.put("veroeffentlicher", publisherProp);
		resolvableProperties.put("publisher", publisherProp);
		Property languageid = new Property(new String[] {}) {
			public Object get(AbstractPage page, String key) {
				Object languageId = page.getLanguageId();

				// this is for compatibility with GCN 3.6 (when no language is set -> return empty string)
				return ObjectTransformer.getInt(languageId, 0) <= 0 ? "" : languageId;
			}
		};

		resolvableProperties.put("sprach_id", languageid);
		resolvableProperties.put("language_id", languageid);
		Property language = new Property(new String[] { "contentset_id"}) {
			public Object get(AbstractPage page, String key) {
				try {
					ContentLanguage lang = page.getLanguage();

					if (null != lang) {
						return lang;
					} else {
						return "";
					}
				} catch (Exception e) {
					page.logger.error("could not retrieve language for page", e);
					return null;
				}
			}
		};

		resolvableProperties.put("language", language);
		resolvableProperties.put("sprache", language);
		Property languagecode = new Property(new String[] { "contentset_id"}) {
			public Object get(AbstractPage page, String key) {
				try {
					ContentLanguage lang = page.getLanguage();

					// if a language was set return it
					if (null != lang) {
						// add additional dependencies here
						RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

						if (renderType.doHandleDependencies()) {
							renderType.addDependency(lang, "code");
						}
						return lang.getCode();
					} else {
						return null;
					}
				} catch (NodeException e) {
					page.logger.error("could not retrieve ContentLanguage for page", e);
					return null;
				}
			}
		};

		resolvableProperties.put("languagecode", languagecode);
		resolvableProperties.put("sprach_code", languagecode);
		// the language variants property is made dependent on "languageset"
		Property languagevariants = new Property(new String[] { "languageset"}) {
			public Object get(AbstractPage page, String key) {
				return page.getLanguageVariantsPHPSerialized();
			}
		};

		resolvableProperties.put("languagevariants", languagevariants);
		resolvableProperties.put("sprachvarianten", languagevariants);
		resolvableProperties.put("object", new Property(new String[] {}) {
			public Object get(AbstractPage page, String key) {
				return new ObjectTagResolvable(page);
			}
		});
        
		resolvableProperties.put("versions", new Property(new String[] {}) {
			public Object get(AbstractPage page, String key) {
				try {
					return page.getVersions();
				} catch (NodeException e) {
					page.logger.error("Error while getting versions of page {" + page.getId() + "}", e);
					return null;
				}
			}
		});
		resolvableProperties.put("version", new Property(new String[] {}) {
			public Object get(AbstractPage page, String key) {
				try {
					return page.getVersion();
				} catch (NodeException e) {
					page.logger.error("Error while getting page version of page {" + page.getId() + "}", e);
					return null;
				}
			}
		});
		resolvableProperties.put("pagevariants", new Property(new String[] { "pagevariants"}) {
			public Object get(AbstractPage page, String key) {
				try {
					return page.getPageVariants();
				} catch (NodeException e) {
					page.logger.error("could not retrieve pagevariants of page {" + page.getId() + "}", e);
					return null;
				}
			} 
		});
		resolvableProperties.put("ismaster", new Property(null) {
			@Override
			public Object get(AbstractPage page, String key) {
				try {
					return page.isMaster();
				} catch (NodeException e) {
					page.logger.error("Error while checking property ismaster of page {" + page.getId() + "}", e);
					return null;
				}
			}
		});
		resolvableProperties.put("inherited", new Property(null) {
			@Override
			public Object get(AbstractPage page, String key) {
				try {
					return page.isInherited();
				} catch (NodeException e) {
					page.logger.error("Error while checking property inherited of page {" + page.getId() + "}", e);
					return null;
				}
			}
		});
		resolvableProperties.put("edittime", new Property(new String[] { "edate", "custom_edate" }) {
			public Object get(AbstractPage page, String key) {
				return page.getCustomOrDefaultEDate().getFullFormat();
			}
		});
		resolvableProperties.put("createtime", new Property(new String[] { "cdate", "custom_cdate" }) {
			public Object get(AbstractPage page, String key) {
				return page.getCustomOrDefaultCDate().getFullFormat();
			}
		});
		resolvableProperties.put("createtimestamp", new Property(new String[] { "cdate", "custom_cdate" }) {
			public Object get(AbstractPage page, String key) {
				return page.getCustomOrDefaultCDate().getTimestamp();
			}
		});
		resolvableProperties.put("timepub", new Property(new String[] { "timepub"}) {
			public Object get(AbstractPage page, String key) {
				return page.getTimePub().getFullFormat();
			}
		});
		resolvableProperties.put("id", new Property(new String[] { "id" }) {
			public Object get(AbstractPage page, String key) {
				try {
					Transaction t = TransactionManager.getCurrentTransaction();
					RenderType renderType = t.getRenderType();
					if (renderType != null && renderType.doHandleDependencies()) {
						renderType.addDependency(page, "id");
					}
				} catch (NodeException e) {
				}
				return page.getId();
			}
		});
		resolvableProperties.put("content_id", new Property(new String[] {}) {
			public Object get(AbstractPage page, String key) {
				try {
					return page.getContent().getId();
				} catch (NodeException e) {
					page.logger.error("could not retrieve content_id of " + page);
					return null;
				}
			}
		});
		resolvableProperties.put("code", languagecode);
		resolvableProperties.put("online", new Property(new String[] { "status"}) {
			public Object get(AbstractPage page, String key) {
				try {
					return page.isOnline();
				} catch (NodeException e) {
					page.logger.error("problem when checking online status of page {" + page + "}");
					return null;
				}
			}
		});
        
		resolvableProperties.put("sprachen", new Property(new String[] { "languageset"}) {
			public Object get(AbstractPage page, String key) {
				return page.getLanguageVariantResolver();
			}
		});

		resolvableProperties.put("contentset_id", new Property(new String[] { "contentset_id"}) {
			public Object get(AbstractPage page, String key) {
				return page.getContentsetId();
			}
		});
		// end of non documented properties

		resolvableKeys = SetUtils.union(AbstractContentObject.resolvableKeys, resolvableProperties.keySet());
	}

	protected AbstractPage(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	@Override
	public Set<String> getResolvableKeys() {
		return resolvableKeys;
	}

	@Override
	public Resolvable getKeywordResolvable(String keyword) throws NodeException {
		Resolvable r = getKeywordResolvableWithoutDependencies(keyword);

		addDependency(keyword, r);
		return r;
	}

	@Override
	public void deleteAllLanguages() throws InsufficientPrivilegesException, NodeException {
		Collection<Page> languages = getLanguageVariants(true);

		// Delete all languages
		for (Page page : languages) {
			page.delete();
		}

		// Make sure that the page is deleted if no language variants are returned
		if (!languages.contains(this)) {
			this.delete();
		}
	}

	/**
	 * Deletes this page in the current language.<br />
	 * Note: the entry in "contentset" database table will remain even if the last language of this page is removed.
	 * @param force true to delete the page, even if wastebin is activated
	 */
	public void delete(boolean force) throws NodeException, InsufficientPrivilegesException {
		// check permissions
		Transaction t = TransactionManager.getCurrentTransaction();
		PermHandler permHandler = t.getPermHandler();

		if (!permHandler.canDelete(this)) {
			throw new InsufficientPrivilegesException("You are not allowed to delete pages from the folder " + this.getFolder(), "no_perm_del_page", this
					.getFolder().getName(), this, PermType.delete);
		}

		// put page into wastebin
		if (!force && t.getNodeConfig().getDefaultPreferences().isFeature(Feature.WASTEBIN, getOwningNode())) {
			putIntoWastebin();

			onDelete(this, true, t.getUserId());

			return;
		}

		// delete ObjectTags
		Collection<ObjectTag> objectTags = this.getObjectTags().values();

		// deleting pages should never cause deletion of objtags in other pages due to synchronization
		try (NoObjectTagSync noSync = new NoObjectTagSync()) {
			for (ObjectTag tag : objectTags) {
				tag.delete();
			}
		}

		// delete the content
		if (canDeleteContent()) {
			getContent().delete();
		}
		performDelete();

		onDelete(this, false, t.getUserId());
	}

	/**
	 * Returns true if the content can be removed.
	 * This means it is not referenced from any other site that won't be deleted.
	 */
	protected abstract boolean canDeleteContent() throws NodeException;

	/**
	 * Performs the delete of the Page
	 * @throws NodeException
	 */
	protected abstract void performDelete() throws NodeException;

	/**
	 * Put the page into the wastebin
	 * @throws NodeException
	 */
	protected abstract void putIntoWastebin() throws NodeException;

	public Resolvable getShortcutResolvable() throws NodeException {
		return getTagResolver();
	}

	public String getStackHashKey() {
		return "page:" + getHashKey();
	}

	@Override
	public String render(String template, RenderResult renderResult, Map<TagmapEntryRenderer, Object> tagmapEntries, Set<String> attributes,
			BiFunction<TagmapEntryRenderer, Object, Object> linkTransformer, long[] times) throws NodeException {
		RuntimeProfiler.beginMark(JavaParserConstants.PAGE_RENDER);
		boolean channelIdSet = false;
		Transaction t = null;

		try {
			t = TransactionManager.getCurrentTransaction();
			TransactionStatistics stats = t.getStatistics();
			RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
			boolean multichannelling = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING);
			boolean checkInherited = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.PUBLISH_INHERITED_SOURCE);
			boolean publish = renderType.getEditMode() == RenderType.EM_PUBLISH;

			if (renderType.find(this) > -1) {
				// TODO loop found! errorhandling
				return "";
			}

			// when multichannelling is used and no channel has been set, set the page's node as channel
			if (multichannelling && t.getChannel() == null) {
				t.setChannelId(getOwningNode().getId());
				channelIdSet = true;
			}

			try (PublishedNodeTrx pnTrx = t.initPublishedNode(t.getChannel())) {
				if (renderType.doHandleDependencies()) {
					// push the dependent object to the stack
					DependencyObject depObject = new DependencyObject(this, (NodeObject) null);
					renderType.pushDependentObject(depObject);
				}
	    
				renderType.push(this);
	    
				// set the templates markup language (if given)
				MarkupLanguage ml = getTemplate().getMarkupLanguage();
	
				if (ml != null) {
					renderType.setMarkupLanguage(ml.getExtension());
				}
	    
				try {
					ContentLanguage lang = getLanguage();
	
					if (lang != null) {
						renderType.setLanguage(lang);
					}
	
					String source = null;
					if (!ObjectTransformer.isEmpty(attributes) && !attributes.contains("content")) {
						renderType.preserveDependencies("content");
					} else {
						// we are rendering the content now
						renderType.setRenderedProperty("content");
	
						// if multichannelling is used and the page is inherited, we get the dependencies for "content" of the page the super node
						if (publish && multichannelling && checkInherited && isInherited()) {
							source = MultichannellingFactory.getInheritedPageSource(this, renderResult);
						}
	
						if (source == null) {
							long startRenderContent = System.currentTimeMillis();
							if (stats != null) {
								stats.get(TransactionStatistics.Item.RENDER_PAGE_CONTENT).start();
							}

							if (template != null) {
								source = template;
							} else {
								// TODO this is just dead wrong here - find a better place to enlist the renderer
								// apply metaeditable renderer first
								TemplateRenderer eRenderer = RendererFactory.getRenderer(RendererFactory.RENDERER_METAEDITABLE);

								source = eRenderer.render(renderResult, getTemplate().getSource());
							}
	
							TemplateRenderer renderer = RendererFactory.getRenderer(renderType.getDefaultRenderer());
	
							// add dependency on the templates source
							if (renderType.doHandleDependencies()) {
								DependencyObject depObject = new DependencyObject(getTemplate(), (NodeObject) null);
								renderType.addDependency(depObject, "source");
							}
							source = renderer.render(renderResult, source);
	
							if (stats != null) {
								stats.get(TransactionStatistics.Item.RENDER_PAGE_CONTENT).stop();
							}
							if (times != null && times.length >= 1) {
								times[0] = System.currentTimeMillis() - startRenderContent;
							}
						}
					}
	
					// render the attributes (if given)
					if (tagmapEntries != null) {
						long startRenderAttributes = System.currentTimeMillis();
						if (stats != null) {
							stats.get(TransactionStatistics.Item.RENDER_PAGE_ATTS).start();
						}
	
						for (Iterator<TagmapEntryRenderer> i = tagmapEntries.keySet().iterator(); i.hasNext();) {
							TagmapEntryRenderer entry = i.next();
	
							if (entry.skip(attributes)) {
								renderType.preserveDependencies(entry.getMapname());
								i.remove();
							} else if (CnMapPublisher.isPageContent(entry)) {
								tagmapEntries.put(entry, source);
							} else {
								// set the rendered property
								renderType.setRenderedProperty(entry.getMapname());
								tagmapEntries.put(entry, entry.getRenderedTransformedValue(renderType, new RenderResult(), linkTransformer));
							}
						}
						if (stats != null) {
							stats.get(TransactionStatistics.Item.RENDER_PAGE_ATTS).stop();
						}
						if (times != null && times.length >= 2) {
							times[1] = System.currentTimeMillis() - startRenderAttributes;
						}
					}
	
					return source;
				} finally {
					// reset the rendered property
					renderType.setRenderedProperty(null);
	
					renderType.pop();
					if (renderType.doHandleDependencies()) {
						renderType.popDependentObject();
						renderType.storeDependencies();
						DependencyManager.removePreparedDependencies(this);
					}
	    
					// clear the level2 cache after page was rendered
					TransactionManager.getCurrentTransaction().clearLevel2Cache();
				}
			}
		} finally {
			if (channelIdSet) {
				t.resetChannel();
			}
			RuntimeProfiler.endMark(JavaParserConstants.PAGE_RENDER);
		}
	}

	@Override
	public Integer setTemplateId(Integer templateId, boolean syncTags) throws ReadOnlyException, NodeException {
		failReadOnly();
		return null;
	}

	@Override
	public String getQualifiedName() throws NodeException {
		StringBuffer qname = new StringBuffer();

		qname.append(getFolder().getQualifiedName());
		qname.append("/");
		qname.append(getName());
		return qname.toString();
	}

	@Override
	public void setFolderId(Integer folderId) throws NodeException, ReadOnlyException {
		failReadOnly();
	}

	@Override
	public Collection<Page> getLanguages() throws NodeException {
		List<Page> langVariants = getLanguageVariants(true);
		Map<String, Page> languages = new HashMap<String, Page>(langVariants.size());
		boolean isPublishing = false;
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
		if (renderType != null) {
			isPublishing = renderType.getEditMode() == RenderType.EM_PUBLISH;
		}
		ContentLanguage cl;

		for (Page page : langVariants) {
			if (isPublishing && !page.isOnline()) {
				continue;
			}
			cl = page.getLanguage();
			if (null != cl) {
				languages.put(cl.getCode(), page);
			}
		}
		return new PageLanguages(languages);
	}

	public Object get(String key) {
		Property prop = (Property) resolvableProperties.get(key);

		if (prop != null) {
			Object value = prop.get(this, key);

			addDependency(key, value);
			return value;
		} else {
			return super.get(key);
		}
	}

	protected String getDate(int timestamp) {
		long microtime = ObjectTransformer.getLong(Integer.valueOf(timestamp), 0L);
		SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");

		return df.format(new Date(microtime));
	}

	@Override
	public ObjectTag getObjectTag(String name, boolean fallback) throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		ObjectTag tag = getObjectTag(name);

		// add dependency on the page object property (although it was not
		// resolved)
		if (renderType.doHandleDependencies()) {
			if (tag == null) {
				renderType.addDependency(new DependencyObject(this, (NodeObject) null), "object." + name);
			} else {
				renderType.addDependency(new DependencyObject(this, tag), null);
			}
		}

		if ((tag == null || !tag.isEnabled()) && fallback) {
			tag = (ObjectTag) getTemplate().getObjectTags().get(name);
			// add dependency on the template object property (although it was not resolved)
			if (renderType.doHandleDependencies()) {
				if (tag == null) {
					renderType.addDependency(new DependencyObject(getTemplate(), (NodeObject) null), "object." + name);
				} else {
					renderType.addDependency(new DependencyObject(getTemplate(), tag), null);
				}
			}

			if ((tag == null || !tag.isEnabled()) && fallback) {
				tag = getFolder().getObjectTag(name, true);

				// add dependency on the folder object property (altough it was not resolved)
				if (renderType.doHandleDependencies()) {
					if (tag == null) {
						renderType.addDependency(new DependencyObject(getFolder(), (NodeObject) null), "object." + name);
					} else {
						renderType.addDependency(new DependencyObject(getFolder(), tag), null);
					}
				}
			}
		}
		return tag;
	}

	@Override
	public ObjectTag getObjectTag(String name) throws NodeException {
		return (ObjectTag) getObjectTags().get(name);
	}

	@Override
	public Set<String> getObjectTagNames(boolean fallback) throws NodeException {
		Set<String> names = new HashSet<>();
		names.addAll(getObjectTags().keySet());
		if (fallback) {
			names.addAll(getTemplate().getObjectTags().keySet());
			names.addAll(getFolder().getObjectTags().keySet());
		}
		return names;
	}

	@Override
	public ContentTag getContentTag(String name) throws NodeException {
		return getContentTags().get(name);
	}

	@Override
	public ContentTag getContentTag(Integer id) throws NodeException {
		return getContentTags().values().stream().filter(tag -> Objects.equals(tag.getId(), id)).findFirst().orElse(null);
	}

	/**
	 * get tag by name:
	 * contenttag if templatetag is editable (public),
	 * templatetag if it is private,
	 * contenttag if templatetag is null,
	 */
	public Tag getTag(String name) throws NodeException {
		// get the contenttag
		Tag tag = getContent().getTag(name);
		// get the templatetag
		TemplateTag templateTag = getTemplate().getTemplateTag(name);

		// Is a pure content tag or is in page editable (public)
		if (templateTag == null || templateTag.isPublic()) {
			return tag;
		} else {
			// not editable.
			return templateTag;
		}
	}

	public Map<String, ContentTag> getContentTags() throws NodeException {
		var content = getContent();
		var contentTags = content.getContentTags();

		if (content.isPartiallyLocalized()) {
			var masterTags = MultichannellingFactory.getNextHigherObject(this).getContentTags();

			addInheritedContentTags(masterTags, contentTags);
		}

		return contentTags;
	}

	public Map<String, Tag> getTags() throws NodeException {
		Map<String, Tag> mergedTags = new HashMap<String, Tag>(getTemplate().getPrivateTemplateTags());

		// before overwriting a templatetag with a contenttag of same name, we
		// check whether the templatetag is editable in page
		Map<String, ContentTag> contentTags = getContentTags();

		for (Map.Entry<String, ContentTag> entry : contentTags.entrySet()) {
			// get the contenttag's name
			String tagName = entry.getKey();

			if (!mergedTags.containsKey(tagName)) {
				// put the contenttag to the map of tags only if editable (public)
				mergedTags.put(entry.getKey(), entry.getValue());
			}
		}

		return mergedTags;
	}

	private void addInheritedContentTags(Map<String, ContentTag> masterContentTags, Map<String, ContentTag> contentTags) throws NodeException {

		var inheritedTags = new HashMap<String, ContentTag>();

		for (var masterTagEntry : masterContentTags.entrySet()) {
			var masterTagName = masterTagEntry.getKey();
			var masterTag = masterTagEntry.getValue();

			if (!contentTags.containsKey(masterTagName)) {
				masterTag.setInherited(true);

				if (masterTag.comesFromTemplate()) {
					inheritedTags.put(masterTagName, masterTag);

					for (var embeddedTagName: MiscUtils.getEmbeddedTagNames(masterContentTags, masterTagName)) {
						var embeddedTag = masterContentTags.get(embeddedTagName);

						embeddedTag.setInherited(true);
						inheritedTags.put(embeddedTagName, embeddedTag);
					}
				}

			}
		}

		contentTags.putAll(inheritedTags);
	}

	private Resolvable getTagResolver() {
		return new TagResolvable(this);
	}
    
	private Resolvable getLanguageVariantResolver() {

		/**
		 * anonymous inner class which will resolve languagecodes via getLanguageVariant
		 */
		return new Resolvable() {
			public Object getProperty(String key) {
				return get(key);
			}

			public Object get(String key) {
				try {
					return getLanguageVariant(key);
				} catch (NodeException e) {
					logger.error("could not retrieve language variant {" + key + "}", e);
				}
				return null;
			}

			public boolean canResolve() {
				return true;
			}
		};
	}

	/**
	 * handle special events for pages which have to be taken offline
	 * @param object
	 * @param property 
	 * @param depth
	 * @throws NodeException
	 */
	protected void handlePageOffline(DependencyObject object, String[] property, int depth) throws NodeException {
		// check whether the page was online
		if (property != null && Arrays.asList(property).contains("online")) {
			// property "pages" for the Folder changes
			DependencyObject folderDep = new DependencyObject(getFolder());

			triggeLocalizeableNodeObjectChangeInFolder(depth + 1);

			// trigger "language variants" changed for other pages in the same languageset
			List<Page> languageVariants = getLanguageVariants(false);

			for (Page variant : languageVariants) {
				String[] modProps = getModifiedPropertiesArray(new String[] { "languageset"});

				if (!this.equals(variant)) {
					DependencyObject variantDep = new DependencyObject(variant);

					variant.triggerEvent(variantDep, modProps, Events.UPDATE, depth + 1, 0);
				}
			}
			// trigger "pagevariants" changed for other page variants
			List<Page> pageVariants = getPageVariants();

			for (Page variant : pageVariants) {
				String[] modProps = getModifiedPropertiesArray(new String[] { "pagevariants"});

				if (!this.equals(variant)) {
					DependencyObject variantDep = new DependencyObject(variant);

					variant.triggerEvent(variantDep, modProps, Events.UPDATE, depth + 1, 0);
				}
			}

			// the page changes its status
			List<String> modifiedProperties = new Vector<String>(getModifiedProperties(new String[] { "status"}));

			triggerEvent(object, (String[]) modifiedProperties.toArray(new String[modifiedProperties.size()]), Events.UPDATE, depth + 1, 0);
		}

		// if the page taken offline is a localized copy, we need to hide former inherited pages in subchannels
		// this might seem wrong (since the page is taken offline), but we also want offline pages to hide inherited pages
		// and the hiding needs to be done here, when an inherited, published page is localized and then taken offline in the channel.
		if (!isMaster()) {
			PageFactory.hideFormerInheritedObjects(Page.TYPE_PAGE, getId(), getChannel(), getChannelSet());
		}
	}

	/**
	 * handle special events for pages which have to be published.
	 * @param object
	 * @param property
	 * @param depth
	 * @throws NodeException
	 */
	protected void handlePagePublish(DependencyObject object, String[] property, int depth) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		int lastPublishedVersion = getLastPublishedVersion(property);

		if (DependencyManager.getLogger().isDebugEnabled()) {
			if (lastPublishedVersion == 0) {
				DependencyManager.getLogger().debug("page was published for the first time");
			} else {
				Date date = new Date((long) lastPublishedVersion * (long) 1000);

				DependencyManager.getLogger().debug(this + " was last published @ " + date);
			}
		}

		List<String> modifiedProperties = collectModifiedProperties(t, property, lastPublishedVersion);

		// we have to trigger the new-language-variant-event not only when a page
		// is published for the first time, but also when the language of the page
		// was set to something else (that is, if "contentgroup_id" changed).
		if (0 == lastPublishedVersion || modifiedProperties.contains("contentgroup_id")) {
			triggerNewLanguageVariant(depth);
		}
		if (0 == lastPublishedVersion) {
			triggeLocalizeableNodeObjectChangeInFolder(depth);
			triggerNewPageVariant(depth);
			modifiedProperties.addAll(getModifiedProperties(new String[] { "status"}));
		}

		// if properties were changed, trigger the event
		if (modifiedProperties.size() > 0) {
			triggerEvent(object, (String[]) modifiedProperties.toArray(new String[modifiedProperties.size()]), Events.UPDATE, depth + 1, 0);
		}

		Set<Integer> contentTagIds = getModifiedContenttags(lastPublishedVersion, -1);

		for (Integer contentTagId : contentTagIds) {
			NodeObject obj = t.getObject(ContentTag.class, contentTagId);

			if (obj != null) {
				DependencyObject depObj = new DependencyObject(obj, (NodeObject) null);

				obj.triggerEvent(depObj, null, Events.UPDATE, depth + 1, 0);
			} else {// TODO obj not found!
			}
		}

		// if the published page is a localized copy, we need to hide former inherited pages in subchannels
		if (!isMaster()) {
			PageFactory.hideFormerInheritedObjects(Page.TYPE_PAGE, getId(), getChannel(), getChannelSet());
		}

		// dirt all page variants
		List<Page> pageVariants = getPageVariants();

		for (Page page : pageVariants) {
			if (!page.equals(this)) {
				page.dirtPage(0);
			}
		}
	}

	private List<String> collectModifiedProperties(Transaction t, String[] property, int lastPublishedVersion) throws NodeException {
		List<String> modifiedProperties = new ArrayList<String>();

		// determine the modified parts and trigger specific mod events
		TableVersion pageVersion = new TableVersion();

		pageVersion.setTable("page");
		pageVersion.setWherePart("gentics_main.id = ?");

		List<Diff> pageDiff = pageVersion.getDiff(new Object[] { getId()}, lastPublishedVersion, -1);

		if (pageDiff.size() > 0) {
			// get the modified columns, transform into modified
			// render-properties and trigger the UPDATE event
			modifiedProperties.addAll(getModifiedProperties((pageDiff.get(0)).getModColumns()));
		}
		// always update the pdate
		modifiedProperties.addAll(getModifiedProperties(new String[] { "pdate"}));

		// and the property
		modifiedProperties.addAll(getModifiedProperties(property));

		return modifiedProperties;
	}

	/**
	 * trigger "new language variant" for other pages in the same languageset
	 */
	private void triggerNewLanguageVariant(int depth) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Node channel = getChannel();
		int channelId = 0;
		if (channel != null) {
			channelId = ObjectTransformer.getInt(channel.getId(), 0);
		}

		t.setChannelId(channelId);
		List<Page> languageVariants = null;
		try {
			languageVariants = getLanguageVariants(false);
		} finally {
			t.resetChannel();
		}

		for (Page variant : languageVariants) {
			String[] modProps = getModifiedPropertiesArray(new String[] { "languageset"});

			if (!this.equals(variant)) {
				DependencyObject variantDep = new DependencyObject(variant);

				List<Integer> channelIds = new ArrayList<Integer>();
				if (channel == null) {
					channelIds.add(0);
				} else {
					channelIds.addAll(MultichannellingFactory.getNodeIds(this, true));
				}
				for (Integer cId : channelIds) {
					variant.triggerEvent(variantDep, modProps, Events.UPDATE, depth + 1, cId);
				}
			}
		}
	}

	/**
	 *  trigger "new page variant" for other page variants
	 */
	private void triggerNewPageVariant(int depth) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Node channel = getChannel();
		int channelId = 0;
		if (channel != null) {
			channelId = ObjectTransformer.getInt(channel.getId(), 0);
		}

		t.setChannelId(channelId);
		List<Page> pageVariants = null;
		try {
			pageVariants = getPageVariants();
		} finally {
			t.resetChannel();
		}

		for (Page variant : pageVariants) {
			String[] modProps = getModifiedPropertiesArray(new String[] { "pagevariants"});

			if (!this.equals(variant)) {
				DependencyObject variantDep = new DependencyObject(variant);

				List<Integer> channelIds = new ArrayList<Integer>();
				if (channel == null) {
					channelIds.add(0);
				} else {
					channelIds.addAll(MultichannellingFactory.getNodeIds(this, true));
				}
				for (Integer cId : channelIds) {
					variant.triggerEvent(variantDep, modProps, Events.UPDATE, depth + 1, cId);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#triggerEvent(com.gentics.contentnode.events.DependencyObject,
	 *      java.lang.String[], int, int)
	 */
	public void triggerEvent(DependencyObject object, String[] property, int eventMask, int depth, int channelId) throws NodeException {
        
		// trigger general dependencies
		super.triggerEvent(object, property, eventMask, depth, channelId);

		
		if (Events.isEvent(eventMask, Events.REVEAL)) {
			triggerNewPageVariant(depth);
		}

		// handle special status changes
		if (Events.isEvent(eventMask, Events.EVENT_CN_PAGESTATUS)) {
			if (isOnline()) {
				handlePagePublish(object, property, depth);
			} else {
				handlePageOffline(object, property, depth);
			}
		}

		if (Events.isEvent(eventMask, Events.UPDATE)) {
			NodeLogger depLogger = DependencyManager.getLogger();

			// update event triggered on the whole page (without properties) -> dirt the page
			if (object.getElementClass() == null && property != null && property.length == 0) {
				if (dirtPage(channelId)) {
					if (depLogger.isInfoEnabled()) {
						depLogger.info((depLogger.isDebugEnabled() ? StringUtils.repeat("  ", depth) : "") + "DIRT {" + this + "}");
					}
				} else {
					if (depLogger.isDebugEnabled()) {
						depLogger.debug(StringUtils.repeat("  ", depth) + "not dirting {" + this + "})");
					}
				}
			}

			// when a property was modified, we notify the folder, this is necessary
			// because of overviews that show pages in folders
			if (!ObjectTransformer.isEmpty(property)) {
				getFolder().triggerEvent(new DependencyObject(getFolder(), Page.class), property, eventMask, depth + 1, 0);
			}
		}

		// the page was moved
		if (Events.isEvent(eventMask, Events.MOVE)) {
			// always dirt the page
			dirtPage(channelId);

			// the property "folder_id" changed
			List<String> modProps = getModifiedProperties(new String[] { "folder_id"});

			triggerEvent(object, modProps.toArray(new String[modProps.size()]), Events.UPDATE, depth + 1, channelId);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.AbstractContentObject#getModifiedProperties(java.lang.String[])
	 */
	protected List<String> getModifiedProperties(String[] modifiedDataProperties) {
		List<String> modifiedProperties = super.getModifiedProperties(modifiedDataProperties);

		return getModifiedProperties(resolvableProperties, modifiedDataProperties, modifiedProperties);
	}

	@Override
	public void setGlobalContentsetId(GlobalId globalId) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public void setContentsetId(Integer id) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public void resetContentsetId() throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public NodeObjectVersion[] getVersions() throws NodeException {
		// if this is not the current version of the page, we get hte
		// pageversions from the current version, because page versions might
		// change over time and old versions of the page will not be dirted in
		// the cache
		if (!getObjectInfo().isCurrentVersion()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			// hack: we want to get the current version, even if currently
			// publishing, so for getting the object here, we temporarily set
			// the rendertyp to preview here
			RenderType renderType = t.getRenderType();
			int oldEditMode = RenderType.EM_PREVIEW;

			if (renderType != null) {
				oldEditMode = renderType.getEditMode();
				renderType.setEditMode(RenderType.EM_PREVIEW);
			}
			Page currentVersion = (Page) t.getObject(Page.class, getId());

			if (renderType != null) {
				// don't forget to restore the original editmode
				renderType.setEditMode(oldEditMode);
			}
			// check if the current version exists
			if (currentVersion != null) {
				// current version exists, so get the page versions
				return currentVersion.getVersions();
			} else {
				// current version does not exist, so tha page does not have any page versions
				// (actually the page does not exist, but somehow this version was still present)
				return new NodeObjectVersion[0];
			}
		} else {
			if (pageVersions == null) {
				pageVersions = loadVersions();
			}

			// clone to make sure that nobody can tamper with the order of the versions
			return pageVersions.clone();
		}
	}

	@Override
	public void unlock() throws NodeException {
		// unlocking the page is done by unlocking the content
		getContent().unlock();
	}

	@Override
	public void restoreTagVersion(ContentTag tag, int versionTimestamp) throws NodeException {
		failReadOnly();
	}

	@Override
	public Page getPublishedObject() throws NodeException {
		// get the published version of the page
		Transaction t = TransactionManager.getCurrentTransaction();
		if (t.isPublishCacheEnabled()) {
			return PublishablePage.getInstance(ObjectTransformer.getInt(getId(), 0));
		} else {
			NodeObjectVersion publishedVersion = getPublishedVersion();

			// if no published version could be detected, return the current object
			if (publishedVersion == null) {
				return this;
			}

			// check whether this is the published version
			if (publishedVersion.getDate().getIntTimestamp() == getObjectInfo().getVersionTimestamp()) {
				return this;
			}

			return t.getObject(Page.class, getId(), publishedVersion.getDate().getIntTimestamp(), false);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#setChannelInfo(java.lang.Integer, java.lang.Integer, boolean)
	 */
	public void setChannelInfo(Integer channelId, Integer channelSetId, boolean allowChange) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	/**
	 * Modify the channel id of an existing master page to a higher master
	 * @param channelId id of the higher channel (may be 0)
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public void modifyChannelId(Integer channelId) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public Node getChannelMasterNode() throws NodeException {
		Node masterNode = getMaster().getChannel();

		if (masterNode != null) {
			return masterNode;
		}

		Node node = getFolder().getNode();
		List<Node> masterNodes = node.getMasterNodes();

		if (masterNodes.size() > 0) {
			return masterNodes.get(masterNodes.size() - 1);
		} else {
			return node;
		}
	}

	@Override
	public Page pushToMaster(Node master) throws ReadOnlyException, NodeException {
		boolean onlineAndNotModified = isOnline() && !isModified();
		boolean offline = !isOnline();

		Page masterPage = MultichannellingFactory.pushToMaster(this, master).getObject();
		Transaction t = TransactionManager.getCurrentTransaction();

		boolean publishPerm = t.canPublish(masterPage);

		if (onlineAndNotModified && publishPerm) {
			masterPage.publish(getTimePub().getIntTimestamp(), null);
		}
		if (offline && publishPerm) {
			masterPage.takeOffline();
		}

		return masterPage;
	}

	@Override
	public void publish(int timestamp, NodeObjectVersion version, boolean updatePublisher) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public void queuePublish(SystemUser user, int timestamp, NodeObjectVersion version) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public void takeOffline(int timestamp) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public void queueOffline(SystemUser user, int timestamp) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public void setContent(Content content) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public void setContentId(Integer contentId) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setCustomCDate(int timestamp) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setCustomEDate(int timestamp) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public boolean save(boolean createVersion, boolean updateEditor) throws InsufficientPrivilegesException, NodeException {
		failReadOnly();
		return false;
	}

	@Override
	public void migrateContenttags(Template template, List<String> tagnames, boolean force) throws NodeException {
		assertEditable();
	}

	@Override
	public NodeObject getParentObject() throws NodeException {
		// parent of a page is the folder
		return getFolder();
	}

	@Override
	public int getEffectiveUdate() throws NodeException {
		// get the page's udate
		int udate = getUdate();

		// check the content
		udate = Math.max(udate, getContent().getEffectiveUdate());
		// check the objtags
		Map<String, ObjectTag> tags = getObjectTags();

		for (Tag tag : tags.values()) {
			udate = Math.max(udate, tag.getEffectiveUdate());
		}
		return udate;
	}

	@Override
	public boolean isPage() {
		return true;
	}

	@Override
	public boolean isRecyclable() {
		return true;
	}

	/**
	 * Inner property class
	 */
	private abstract static class Property extends AbstractProperty {

		/**
		 * Create instance of the property
		 * @param dependsOn
		 */
		public Property(String[] dependsOn) {
			super(dependsOn);
		}

		/**
		 * Get the property value for the given object
		 * @param object object
		 * @param key property key
		 * @return property value
		 */
		public abstract Object get(AbstractPage object, String key);
	}

	/**
	 * creates a resolvable collection of pages
	 * @author clemens
	 */
	class PageLanguages implements Collection<Page>, Resolvable, ResolvableMapWrappable {
		private Map<String, Page> pages;

		/**
		 * create a resolveable collection of pages, populated with provided map of pages 
		 * @param pages used for the new collection
		 */
		public PageLanguages(Map<String, Page> pages) {
			this.pages = pages; 
		}

		@Override
		public Set<String> getResolvableKeys() {
			return pages.keySet();
		}

		public int size() {
			return pages.size();
		}

		public void clear() {
			throw new UnsupportedOperationException();
		}

		public boolean isEmpty() {
			return pages.isEmpty();
		}

		public Object[] toArray() {
			return pages.values().toArray();
		}

		public boolean add(Page o) {
			throw new UnsupportedOperationException();
		}

		public boolean contains(Object o) {
			return pages.containsKey(o);
		}

		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		public boolean addAll(Collection<? extends Page> c) {
			throw new UnsupportedOperationException();
		}

		public boolean containsAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		public Iterator<Page> iterator() {
			return pages.values().iterator();
		}

		public Object[] toArray(Object[] a) {
			return pages.values().toArray();
		}

		public Object getProperty(String key) {
			return get(key);
		}

		public Object get(String key) {
			return pages.get(key);
		}

		public boolean canResolve() {
			return true;
		}

		public String toString() {
			String obj;

			try {
				obj = pages.values().iterator().next().toString();
				return obj;
			} catch (Exception e) {
				logger.error("could not turn first page of contentset into a string", e);
			}
			return super.toString();
		}
	}
    
	/**
	 * this inner class is used to detain page.template from being rendered as
	 * the template itself when used inside a page
	 */
	protected class PageTemplate implements Resolvable, ResolvableMapWrappable {
		private Template template;

		/**
		 * create a new PageTemplate
		 * @param template to be created from
		 */
		public PageTemplate(Template template) {
			this.template = template;
		}

		@Override
		public Set<String> getResolvableKeys() {
			return template.getResolvableKeys();
		}

		public Object getProperty(String key) {
			return get(key);
		}

		public Object get(String key) {
			return template.get(key);
		}

		public boolean canResolve() {
			return true;
		}

		public String toString() {
			return template.toString();
		}
	}

	/**
	 * Inner class representing the languageset of a page (the languageset
	 * contains all language variants of the page)
	 */
	public class LanguageSet extends ResolvableBean implements ResolvableMapWrappable {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 3151476465698652085L;

		protected final static Set<String> resolvableKeys = SetUtils.hashSet("id", "pages");

		@Override
		public Set<String> getResolvableKeys() {
			return resolvableKeys;
		}

		/**
		 * Get the id of the languageset (= contentset_id)
		 * @return id of the languageset
		 */
		public Object getId() {
			return getContentsetId();
		}

		/**
		 * Get the language variants of this page
		 * @return language variants
		 * @throws NodeException
		 */
		public Map<String, Page> getPages() throws NodeException {
			List<Page> langVariants = getLanguageVariants(true);
			Map<String, Page> languages = new LinkedHashMap<String, Page>(langVariants.size());
			ContentLanguage cl;

			boolean isPublishing = TransactionManager.getCurrentTransaction().getRenderType().getEditMode() == RenderType.EM_PUBLISH;
			for (Page page : langVariants) {
				if (isPublishing && !page.isOnline()) {
					continue;
				}
				cl = page.getLanguage();
				if (null != cl) {
					languages.put(cl.getCode(), page);
				}
			}
			return languages;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "LanguageSet of " + AbstractPage.this;
		}
	}
}
