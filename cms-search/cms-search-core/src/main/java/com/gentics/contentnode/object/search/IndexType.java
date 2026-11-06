package com.gentics.contentnode.object.search;

import static com.gentics.contentnode.db.DBUtils.IDS;
import static com.gentics.contentnode.db.DBUtils.firstInt;
import static com.gentics.contentnode.db.DBUtils.select;
import static com.gentics.contentnode.factory.Trx.supply;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.IndexableFile;
import com.gentics.contentnode.object.IndexableFolder;
import com.gentics.contentnode.object.IndexableForm;
import com.gentics.contentnode.object.IndexableImageFile;
import com.gentics.contentnode.object.IndexablePage;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;

import io.reactivex.Flowable;

/**
 * Type of indexed objects
 */
public enum IndexType {
	file(File.class, IndexableFile.wrap, ContentFile.TYPE_FILE, false, false, "FROM contentfile WHERE filetype NOT LIKE 'image/%'", "AND folder_id = ?", "SELECT contentfile.id FROM contentfile, folder WHERE contentfile.folder_id = folder.id AND contentfile.filetype NOT LIKE 'image/%' AND folder.node_id = ?"),
	image(ImageFile.class, IndexableImageFile.wrap, ContentFile.TYPE_IMAGE, false, false, "FROM contentfile WHERE filetype LIKE 'image/%'", "AND folder_id = ?", "SELECT contentfile.id FROM contentfile, folder WHERE contentfile.folder_id = folder.id AND contentfile.filetype LIKE 'image/%' AND folder.node_id = ?"),
	folder(Folder.class, IndexableFolder.wrap, Folder.TYPE_FOLDER, false, false, "FROM folder WHERE mother != 0", "AND mother = ?", "SELECT id FROM folder WHERE mother != 0 AND node_id = ?"),
	page(Page.class, IndexablePage.wrap, Page.TYPE_PAGE, true, true, "FROM page WHERE contentgroup_id = ?", "AND folder_id = ?", "SELECT page.id FROM page, folder WHERE page.folder_id = folder.id AND page.contentgroup_id = ? AND folder.node_id = ?"),
	form(Form.class, IndexableForm.wrap, Form.TYPE_FORM, true, false, "FROM form", "WHERE folder_id = ?", "SELECT form.id FROM form, folder WHERE form.folder_id = folder.id AND folder.node_id = ?");

	/**
	 * Object Type
	 */
	private int objectType;

	/**
	 * Object class
	 */
	private Class<? extends NodeObject> clazz;

	/**
	 * Wrapper function to wrap a {@link NodeObject} into the corresponding {@link IndexableNodeObject}
	 */
	private Function<NodeObject, IndexableNodeObject<?>> wrapper;

	/**
	 * True, when type supports languages
	 */
	private boolean multiLanguage;

	/**
	 * True, when {@link #multiLanguage} is true and the language is defined per instance
	 */
	private boolean languagePerInstance;

	/**
	 * SQL to select objects for the index type
	 */
	private String selectSql;

	/**
	 * SQL to count objects for the index type
	 */
	private String countSql;

	/**
	 * SQL to select objects for the index type in a folder
	 */
	private String inFolderSelectSql;

	/**
	 * SQL to count objects for the index type in a folder
	 */
	private String inFolderCountSql;

	/**
	 * SQL to select objects for the index type in a node
	 */
	private String inNodeSelectSql;

	/**
	 * Get the optional {@link IndexType} for the given {@link NodeObject}
	 * @param object object
	 * @return optional {@link IndexType}
	 */
	public static Optional<IndexType> get(NodeObject object) {
		return get(object.getObjectInfo().getObjectClass());
	}

	/**
	 * Get the optional {@link IndexType} for the given {@link NodeObject} class
	 * @param clazz class
	 * @return optional {@link IndexType}
	 */
	public static Optional<IndexType> get(Class<? extends NodeObject> clazz) {
		for (IndexType type : values()) {
			if (type.clazz.equals(clazz)) {
				return Optional.of(type);
			}
		}
		return Optional.empty();
	}

	/**
	 * Optionally wrap the given {@link NodeObject} into an {@link IndexableNodeObject}
	 * @param object object to wrap
	 * @return optional wrapper
	 * @throws NodeException
	 */
	public static Optional<IndexableNodeObject<?>> wrap(NodeObject object) throws NodeException {
		Optional<IndexType> optType = get(object);
		if (optType.isPresent()) {
			return Optional.of(optType.get().wrapper.apply(object));
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Create instance
	 * @param clazz object class
	 * @param nodeObjectClazz node object class
	 * @param objectType object type
	 * @param multiLanguage true for multilanguage
	 * @param languagePerInstance true if the language is set per instance
	 * @param sql FROM part of the SQL statements
	 * @param inFolderClause additional clause to restrict to children of a folder
	 * @param inNodeSelect complete SQL statement to select items in a node
	 */
	<T extends NodeObject> IndexType(Class<T> clazz, Function<NodeObject, IndexableNodeObject<?>> wrapper, int objectType, boolean multiLanguage, boolean languagePerInstance, String sql, String inFolderClause, String inNodeSelectSql) {
		this.clazz = clazz;
		this.wrapper = wrapper;
		this.objectType = objectType;
		this.multiLanguage = multiLanguage;
		this.languagePerInstance = languagePerInstance;
		this.selectSql = String.format("SELECT id %s", sql);
		this.countSql = String.format("SELECT count(id) c %s", sql);
		this.inFolderSelectSql = String.format("SELECT id %s %s", sql, inFolderClause);
		this.inFolderCountSql = String.format("SELECT count(id) c %s %s", sql, inFolderClause);
		this.inNodeSelectSql = inNodeSelectSql;
	}

	/**
	 * Get the objects with given IDs wrapped into {@link IndexableNodeObject}
	 * @param ids object IDs
	 * @return list of wrappers
	 * @throws NodeException
	 */
	public List<IndexableNodeObject<? extends NodeObject>> getObjects(Collection<Integer> ids) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<IndexableNodeObject<? extends NodeObject>> result = new ArrayList<>();
		for (NodeObject nodeObject : t.getObjects(clazz, ids)) {
			result.add(wrapper.apply(nodeObject));
		}
		return result;
	}

	/**
	 * Get object type
	 * @return object type
	 */
	public int getObjectType() {
		return objectType;
	}

	/**
	 * Return true, if the indexed object type supports languages
	 * @return true for multilanguage
	 */
	public boolean isMultiLanguage() {
		return multiLanguage;
	}

	/**
	 * Return true, if the language of the indexed object is defined per instance, false if the object contains all supported languages
	 * @return true for language per instance
	 */
	public boolean isLanguagePerInstance() {
		return languagePerInstance;
	}

	/**
	 * Get function that returns the object IDs for the given language
	 * @return function
	 */
	public Function<String, Set<Integer>> ids() {
		return language -> {
			return supply(t -> {
				if (multiLanguage && languagePerInstance) {
					int contentGroupId = language != null
							? select("SELECT id FROM contentgroup WHERE code = ?", ps -> ps.setString(1, language), firstInt("id"))
							: 0;
					return select(selectSql, ps -> ps.setInt(1, contentGroupId), IDS);
				} else if (multiLanguage && !languagePerInstance) {
					try (WastebinFilter wbf = Wastebin.INCLUDE.set()) {
						// if the objects "contain" the language variants, we need to load all objects and check directly
						List<IndexableNodeObject<? extends NodeObject>> objects = getObjects(select(selectSql, IDS));
						return new HashSet<>(Flowable.fromIterable(objects).filter(o -> {
							Set<String> languages = o.getIndexLanguages();
							return language == null ? languages.isEmpty() : languages.contains(language);
						}).map(o -> o.getId()).toList().blockingGet());
					}
				} else {
					return select(selectSql, IDS);
				}
			});
		};
	}

	/**
	 * Get function that returns the object IDs for the given language and parent folder ID
	 * @return bifunction
	 */
	public BiFunction<String, Integer, Set<Integer>> childIds() {
		return (language, folderId) -> {
			return supply(t -> {
				if (multiLanguage && languagePerInstance) {
					int contentGroupId = language != null
							? select("SELECT id FROM contentgroup WHERE code = ?", ps -> ps.setString(1, language), firstInt("id"))
							: 0;
					return select(inFolderSelectSql, ps -> {
						ps.setInt(1, contentGroupId);
						ps.setInt(2, folderId);
					}, IDS);
				} else if (multiLanguage && !languagePerInstance) {
					try (WastebinFilter wbf = Wastebin.INCLUDE.set()) {
						// if the objects "contain" the language variants, we need to load all objects and check directly
						List<IndexableNodeObject<? extends NodeObject>> objects = getObjects(select(inFolderSelectSql, ps -> {
							ps.setInt(1, folderId);
						}, IDS));
						return new HashSet<>(Flowable.fromIterable(objects).filter(o -> {
							Set<String> languages = o.getIndexLanguages();
							return language == null ? languages.isEmpty() : languages.contains(language);
						}).map(o -> o.getId()).toList().blockingGet());
					}
				} else {
					return select(inFolderSelectSql, ps -> {
						ps.setInt(1, folderId);
					}, IDS);
				}
			});
		};
	}

	/**
	 * Get function that returns the object IDs of objects in the given node ID and language
	 * @return bifunction
	 */
	public BiFunction<String, Integer, Set<Integer>> inNodeIds() {
		return (language, nodeId) -> {
			return supply(t -> {
				if (multiLanguage && languagePerInstance) {
					int contentGroupId = language != null
							? select("SELECT id FROM contentgroup WHERE code = ?", ps -> ps.setString(1, language), firstInt("id"))
							: 0;
					return select(inNodeSelectSql, ps -> {
						ps.setInt(1, contentGroupId);
						ps.setInt(2, nodeId);
					}, IDS);
				} else if (multiLanguage && !languagePerInstance) {
					try (WastebinFilter wbf = Wastebin.INCLUDE.set()) {
						// if the objects "contain" the language variants, we need to load all objects and check directly
						List<IndexableNodeObject<? extends NodeObject>> objects = getObjects(
								select(inNodeSelectSql, ps -> {
									ps.setInt(1, nodeId);
								}, IDS));
						return new HashSet<>(Flowable.fromIterable(objects).filter(o -> {
							Set<String> languages = o.getIndexLanguages();
							return language == null ? languages.isEmpty() : languages.contains(language);
						}).map(o -> o.getId()).toList().blockingGet());
					}
				} else {
					return select(inNodeSelectSql, ps -> {
						ps.setInt(1, nodeId);
					}, IDS);
				}
			});
		};
	}

	/**
	 * Get function that returns the object count for the given language
	 * @return function
	 */
	public Function<String, Integer> count() {
		return language -> {
			return supply(t -> {
				if (multiLanguage && languagePerInstance) {
					int contentGroupId = language != null
							? select("SELECT id FROM contentgroup WHERE code = ?", ps -> ps.setString(1, language), firstInt("id"))
							: 0;
					return select(countSql, ps -> ps.setInt(1, contentGroupId), firstInt("c"));
				} else if (multiLanguage && !languagePerInstance) {
					try (WastebinFilter wbf = Wastebin.INCLUDE.set()) {
						// if the objects "contain" the language variants, we need to load all objects and check directly
						List<IndexableNodeObject<? extends NodeObject>> objects = getObjects(select(selectSql, IDS));
						return Flowable.fromIterable(objects).filter(o -> {
							Set<String> languages = o.getIndexLanguages();
							return language == null ? languages.isEmpty() : languages.contains(language);
						}).count().blockingGet().intValue();
					}
				} else {
					return select(countSql, firstInt("c"));
				}
			});
		};
	}

	/**
	 * Load the object, which is indexed in this index
	 * @param id object ID
	 * @return object or null, if not found
	 * @throws NodeException
	 */
	public IndexableNodeObject<? extends NodeObject> load(int id) throws NodeException {
		try (Trx trx = new Trx(); WastebinFilter wbf = Wastebin.INCLUDE.set()) {
			NodeObject nodeObject = trx.getTransaction().getObject(clazz, id, -1, false);
			IndexableNodeObject<? extends NodeObject> obj = wrapper.apply(nodeObject);
			trx.success();
			return obj;
		}
	}

	/**
	 * Get function that returns the object count for the given language and parent folder ID
	 * @return bifunction
	 */
	public BiFunction<String, Integer, Integer> childCount() {
		return (language, folderId) -> {
			return supply(t -> {
				if (multiLanguage && languagePerInstance) {
					int contentGroupId = language != null
							? select("SELECT id FROM contentgroup WHERE code = ?", ps -> ps.setString(1, language), firstInt("id"))
							: 0;
					return select(inFolderCountSql, ps -> {
						ps.setInt(1, contentGroupId);
						ps.setInt(2, folderId);
					}, firstInt("c"));
				} else if (multiLanguage && !languagePerInstance) {
					try (WastebinFilter wbf = Wastebin.INCLUDE.set()) {
						// if the objects "contain" the language variants, we need to load all objects and check directly
						List<IndexableNodeObject<? extends NodeObject>> objects = getObjects(select(inFolderCountSql, ps -> {
							ps.setInt(1, folderId);
						}, IDS));
						return Flowable.fromIterable(objects).filter(o -> {
							Set<String> languages = o.getIndexLanguages();
							return language == null ? languages.isEmpty() : languages.contains(language);
						}).count().blockingGet().intValue();
					}
				} else {
					return select(inFolderCountSql, ps -> {
						ps.setInt(1, folderId);
					}, firstInt("c"));
				}
			});
		};
	}
}
