package com.gentics.contentnode.init;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Init Job that migrates forms. The "type" attribute of the form "data" is transformed into the "formtype" (lowercase).
 * Form type configurations for existing (not deleted) forms are required to already exist in the configuration (if not, the migration job will fail).
 * Form type configurations will automatically be assigned to nodes, that contain forms with the types.
 * The form "data" is migrated to the new structure
 */
public class MigrateForms extends InitJob {
	/**
	 * Configuration path to the type configurations
	 */
	private static final String TYPES_CONFIG_PATH = "forms.types";

	@Override
	public void execute() throws NodeException {
		Map<String, Object> formTypeConfigurations = NodeConfigRuntimeConfiguration.getPreferences()
				.getPropertyMap(TYPES_CONFIG_PATH);

		ObjectMapper mapper = new ObjectMapper();
		Map<Integer, Set<String>> formTypesPerNode = new HashMap<>();

		while (true) {
			Map<Integer, Pair<Integer, JsonNode>> toMigrate = DBUtils.select(
					"SELECT form.id, folder.node_id, form.data FROM form LEFT JOIN folder ON form.folder_id = folder.id WHERE form.formtype = ? LIMIT 100",
					pst -> {
						pst.setString(1, FormFactory.UNMIGRATED_FORMS);
					}, rs -> {
						Map<Integer, Pair<Integer, JsonNode>> result = new HashMap<>();

						while (rs.next()) {
							int formId = rs.getInt("id");
							int nodeId = rs.getInt("node_id");
							JsonNode data = mapper.convertValue(rs.getString("data"), JsonNode.class);
							result.put(formId, Pair.of(nodeId, data));
						}
						return result;
					});

			if (toMigrate.isEmpty()) {
				break;
			}

			for (Map.Entry<Integer, Pair<Integer, JsonNode>> entry : toMigrate.entrySet()) {
				int formId = entry.getKey();
				int nodeId = entry.getValue().getLeft();
				JsonNode data = entry.getValue().getRight();

				String formType = StringUtils.toRootLowerCase(data.get("type").asText("generic"));

				if (!formTypeConfigurations.containsKey(formType)) {
					throw new NodeException(
							"Migration of forms not possible. Missing required form type configuration '%s'."
									.formatted(formType));
				}

				formTypesPerNode.computeIfAbsent(nodeId, key -> new HashSet<>()).add(formType);

				JsonNode migratedData = migrateFormData(data);

				DBUtils.update("UPDATE form SET data = ?, formtype = ? WHERE id = ?", migratedData.toString(), formType, formId);

				// TODO migrate all form_nodeversion data
			}

			TransactionManager.getCurrentTransaction().commit(false);
		}

		for (Entry<Integer, Set<String>> entry : formTypesPerNode.entrySet()) {
			int nodeId = entry.getKey();
			Set<String> formTypes = entry.getValue();

			for (String formType : formTypes) {
				DBUtils.update("INSERT IGNORE INTO node_formtype (node_id, formtype) VALUES (?, ?)", nodeId, formType);
			}
		}
	}

	/**
	 * Migrate the form data
	 * @param data original data
	 * @return migrated form data
	 */
	protected JsonNode migrateFormData(JsonNode data) {
		// TODO migrate the data
		return data;
	}
}
