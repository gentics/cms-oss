package com.gentics.contentnode.factory.url;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.NodeObjectWithAlternateUrls;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.db.TableVersion;
import com.gentics.lib.util.FileUtil;


/**
 * Abstract base class for alternate URLs container implementations.
 * The class extends {@link TreeMap} and contains a map of alternate URL to ID of the entry in the DB (null for new entries)
 */
public abstract class AlternateUrlsContainer extends TreeMap<String, Integer> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -8796952822894751582L;

	/**
	 * Parent object
	 */
	protected NodeObjectWithAlternateUrls parent;

	/**
	 * Function that normalizes the nice URL (trim whitespace, make sure it starts with /, but does not end with /, replace with null if empty)
	 */
	public final static Function<String, String> NORMALIZER = niceUrl -> {
		// fix format of niceUrl. If not empty, it must always begin with "/" but must not end with "/"
		if (!StringUtils.isEmpty(niceUrl)) {
			niceUrl = StringUtils.trim(niceUrl);
			niceUrl = StringUtils.prependIfMissing(niceUrl, "/");
			niceUrl = StringUtils.removeEnd(niceUrl, "/");
		}
		if ("".equals(niceUrl)) {
			niceUrl = null;
		}

		if (niceUrl != null) {
			NodePreferences nodePreferences = TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences();
			Map<String, String> sanitizeCharacters = nodePreferences.getPropertyMap("sanitize_character");
			String replacementChararacter = nodePreferences.getProperty("sanitize_replacement_character");
			String[] preservedCharacters = nodePreferences.getProperties("sanitize_allowed_characters");
			niceUrl = FileUtil.sanitizeFolderPath(niceUrl, sanitizeCharacters, replacementChararacter, preservedCharacters);
		}

		return niceUrl;
	};

	/**
	 * Create instance for parent object
	 * @param parent parent object
	 * @throws NodeException
	 */
	public AlternateUrlsContainer(NodeObjectWithAlternateUrls parent) throws NodeException {
		this.parent = parent;
		load();
	}

	/**
	 * Get the name of the table holding the alternate URLs
	 * @return table name
	 */
	public abstract String getTableName();

	/**
	 * Get the name of the column referencing the record of the "main" table
	 * @return column name
	 */
	public abstract String getReferenceColumnName();

	/**
	 * Get the alternate URLs as unmodifiable set
	 * @return unmodifiable set of alternate URLs
	 * @throws NodeException
	 */
	public Set<String> getAlternateUrls() throws NodeException {
		return Collections.unmodifiableSet(keySet());
	}

	/**
	 * Save the alternate URLs (add missing, remove superfluous)
	 * @throws NodeException
	 */
	public void save() throws NodeException {
		int parentId = parent.getId();

		List<Integer> oldIds = values().stream().filter(id -> id != null).collect(Collectors.toList());
		StringBuilder deleteSql = new StringBuilder(
				"DELETE FROM " + getTableName() + " WHERE " + getReferenceColumnName() + " = ?");
		if (!oldIds.isEmpty()) {
			deleteSql.append(" AND id NOT IN (").append(StringUtils.repeat("?", ",", oldIds.size())).append(")");
		}

		Object[] params = new Object[1 + oldIds.size()];
		int index = 0;
		params[index++] = parentId;
		for (int id : oldIds) {
			params[index++] = id;
		}
		DBUtils.update(deleteSql.toString(), params);

		List<Object[]> insertData = entrySet().stream().filter(entry -> entry.getValue() == null)
				.map(entry -> new Object[] { parentId, entry.getKey() }).collect(Collectors.toList());
		if (!insertData.isEmpty()) {
			DBUtils.executeBatchInsert(
					"INSERT INTO " + getTableName() + " (" + getReferenceColumnName() + ", url) VALUES (?, ?)",
					insertData);
		}
	}

	/**
	 * Set the alternate URLs
	 * @param alternateUrls set of alternate URLs
	 * @return true when something was changed, false if not
	 * @throws NodeException
	 */
	public boolean set(Set<String> alternateUrls) throws NodeException {
		if (alternateUrls != null) {
			Set<String> temp = new HashSet<>();
			for (String url : alternateUrls) {
				String normalizedUrl = NORMALIZER.apply(url);
				if (normalizedUrl != null) {
					temp.add(normalizedUrl);
				}
			}
			alternateUrls = temp;
		}

		if (alternateUrls != null && !Objects.deepEquals(this.keySet(), alternateUrls)) {
			for (String url : alternateUrls) {
				putIfAbsent(url, null);
			}
			keySet().retainAll(alternateUrls);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Modify the URLs with the given modifier
	 * @param modifier modifier
	 * @throws NodeException
	 */
	public void modify(Function<String, String> modifier) throws NodeException {
		Set<String> current = getAlternateUrls();
		Set<String> unique = new HashSet<>();
		for (String url : current) {
			unique.add(modifier.apply(url));
		}
		set(unique);
	}

	/**
	 * Load the alternate URLs from the DB
	 * @throws NodeException
	 */
	protected void load() throws NodeException {
		if (!AbstractContentObject.isEmptyId(parent.getId())) {
			TableVersion tableVersion = null;
			if (!parent.getObjectInfo().isCurrentVersion()) {
				tableVersion = parent.getAlternateUrlTableVersion();
			}
			if (tableVersion != null) {
				SimpleResultProcessor res = tableVersion.getVersionData(new Object[] { parent.getId() },
						parent.getObjectInfo().getVersionTimestamp());
				for (SimpleResultRow row : res) {
					put(row.getString("url"), row.getInt("id"));
				}
			} else {
				DBUtils.select("SELECT url, id FROM " + getTableName() + " WHERE " + getReferenceColumnName() + " = ?",
						ps -> ps.setInt(1, parent.getId()), rs -> {
							while (rs.next()) {
								put(rs.getString("url"), rs.getInt("id"));
							}
							return true;
						});
			}
		}
	}
}
