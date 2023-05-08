package com.gentics.contentnode.factory.object;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.TableVersion;
import com.gentics.lib.db.TableVersionRestoreProcessor;
import com.gentics.lib.etc.StringUtils;

/**
 * Processor for restoring of page versions. This implementation will keep the data integrity intact by
 * deleting values, etc. referencing deleted contentTags
 */
public class PageVersionRestoreProcessor implements TableVersionRestoreProcessor {
	/**
	 * Content Id
	 */
	protected long contentId;

	/**
	 * Set containing the IDs of contenttags, which are removed during the restore process
	 */
	protected HashSet<Integer> removedContentTagIds;

	/**
	 * Create instance for the given contentId
	 * @param contentId contentId
	 */
	public PageVersionRestoreProcessor(long contentId) {
		this.contentId = contentId;
	}

	@Override
	public void preRestore(TableVersion tableVersion, SimpleResultProcessor data) throws NodeException {
		if ("contenttag".equals(tableVersion.getTable())) {
			// collect the current contenttag IDs (for finding out, which will be removed during the restore process)
			removedContentTagIds = new HashSet<>(tableVersion.getVersionData(new Object[] { contentId }, -1).asList().stream().map(row -> row.getInt("id"))
					.collect(Collectors.toSet()));
		}
	}

	@Override
	public void postRestore(TableVersion tableVersion, SimpleResultProcessor data) throws NodeException {
		if (!ObjectTransformer.isEmpty(removedContentTagIds)) {
			Integer[] idArray = (Integer[]) removedContentTagIds.toArray(new Integer[removedContentTagIds.size()]);
			String placeholders = StringUtils.repeat("?", idArray.length, ",");

			if ("contenttag".equals(tableVersion.getTable())) {
				// calculate the diff
				removedContentTagIds.removeAll(tableVersion.getVersionData(new Object[] { contentId }, -1).asList().stream().map(row -> row.getInt("id"))
						.collect(Collectors.toSet()));
			} else if ("value".equals(tableVersion.getTable())) {
				// get the IDs of datasources which are referenced by values with part.type_id == 32
				Set<Integer> dsIds = DBUtils.select(
						String.format(
								"SELECT datasource.id FROM value, part, datasource WHERE value.contenttag_id IN (%s) AND value.part_id = part.id AND part.type_id = 32 AND value.value_ref = datasource.id AND datasource.name IS NULL",
								placeholders), st -> {
							for (int i = 0; i < idArray.length; i++) {
								st.setInt(i + 1, idArray[i]);
							}
						}, DBUtils.IDS);

				// delete the datasources and their values
				if (!ObjectTransformer.isEmpty(dsIds)) {
					Integer[] dsIdArray = (Integer[]) dsIds.toArray(new Integer[dsIds.size()]);
					String dsPlaceholders = StringUtils.repeat("?", dsIdArray.length, ",");

					DBUtils.deleteWithPK("datasource_value", "id", String.format("datasource_id IN (%s)", dsPlaceholders), dsIdArray);
					DBUtils.executeUpdate(String.format("DELETE FROM datasource WHERE id IN (%s)", dsPlaceholders), dsIdArray);
				}

				DBUtils.deleteWithPK("value", "id", String.format("contenttag_id IN (%s)", placeholders), idArray);
			} else if ("ds".equals(tableVersion.getTable())) {
				DBUtils.deleteWithPK("ds", "id", String.format("contenttag_id IN (%s)", placeholders), idArray);
			} else if ("ds_obj".equals(tableVersion.getTable())) {
				DBUtils.deleteWithPK("ds_obj", "id", String.format("contenttag_id IN (%s)", placeholders), idArray);
			}
		}
	}

}
