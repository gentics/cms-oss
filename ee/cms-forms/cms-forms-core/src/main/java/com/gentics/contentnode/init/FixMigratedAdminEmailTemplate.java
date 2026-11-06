package com.gentics.contentnode.init;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Form;

/**
 * Init Job that fixes incorrectly migrated adminEmailTemplate attributes of forms.
 */
@InitJobName("fixmigratedadminemailtemplate")
public class FixMigratedAdminEmailTemplate extends InitJob {
	/**
	 * Pattern to find incorrectly migrated adminMailTemplate attributes (migrated as objects, but should be strings)
	 */
	protected final static String INCORRECT_ADMINMAIL_TEMPLATE_PATTERN = "\"adminEmailTemplate\":\\s*\\{";

	/**
	 * SQL Statement to get up to 100 incorrectly migrated forms
	 */
	protected final static String SQL_INCORRECTLY_MIGRATED_FORMS = "SELECT id, data FROM form WHERE data REGEXP ? LIMIT 100";

	/**
	 * SQL Statement to update form data
	 */
	protected final static String SQL_UPDATE_FORM_DATA = "UPDATE form SET data = ? WHERE id = ?";

	/**
	 * SQL Statement to get up to 100 incorrectly migrated form versions
	 */
	protected final static String SQL_INCORRECTLY_MIGRATED_FORM_VERSIONS = "SELECT auto_id, id, data FROM form_nodeversion WHERE data REGEXP ? LIMIT 100";

	/**
	 * SQL Statement to update form version data
	 */
	protected final static String SQL_UPDATE_FORM_VERSION_DATA = "UPDATE form_nodeversion SET data = ? WHERE auto_id = ?";

	/**
	 * Object mapper to use
	 */
	private ObjectMapper mapper = new ObjectMapper();

	@Override
	public void execute() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Set<Integer> correctedFormIds = new HashSet<>();

		// migrate the forms in batches of max 100 forms
		while (true) {
			List<Pair<Integer, String>> formIdAndData = DBUtils.select(SQL_INCORRECTLY_MIGRATED_FORMS, pst -> {
				pst.setString(1, INCORRECT_ADMINMAIL_TEMPLATE_PATTERN);
			}, rs -> {
				List<Pair<Integer, String>> tmp = new ArrayList<>();
				while (rs.next()) {
					tmp.add(Pair.of(rs.getInt("id"), rs.getString("data")));
				}
				return tmp;
			});

			if (formIdAndData.isEmpty()) {
				break;
			}

			for (Pair<Integer, String> pair : formIdAndData) {
				int formId = pair.getLeft();
				String data = pair.getRight();

				try {
					String correctedData = correctAdminEmailTemplate(data);
					DBUtils.update(SQL_UPDATE_FORM_DATA, correctedData, formId);
					correctedFormIds.add(formId);
				} catch (JsonProcessingException e) {
					throw new NodeException("Error while correcting adminEmailTemplate for form %d".formatted(formId),
							e);
				}
			}

			t.commit(false);
		}

		// migrate the form_nodeversion data in batches of max 100 entries
		while (true) {
			List<Triple<Integer, Integer, String>> autoIdFormIdData = DBUtils.select(SQL_INCORRECTLY_MIGRATED_FORM_VERSIONS, pst -> {
				pst.setString(1, INCORRECT_ADMINMAIL_TEMPLATE_PATTERN);
			}, rs -> {
				List<Triple<Integer, Integer, String>> tmp = new ArrayList<>();
				while (rs.next()) {
					tmp.add(Triple.of(rs.getInt("auto_id"), rs.getInt("id"), rs.getString("data")));
				}
				return tmp;
			});

			if (autoIdFormIdData.isEmpty()) {
				break;
			}

			for (Triple<Integer, Integer, String> triple : autoIdFormIdData) {
				int autoId = triple.getLeft();
				int formId = triple.getMiddle();
				String data = triple.getRight();

				try {
					String correctedData = correctAdminEmailTemplate(data);
					DBUtils.update(SQL_UPDATE_FORM_VERSION_DATA, correctedData, autoId);
					correctedFormIds.add(formId);
				} catch (JsonProcessingException e) {
					throw new NodeException("Error while correcting adminEmailTemplate for form %d".formatted(formId),
							e);
				}
			}

			t.commit(false);
		}

		// trigger update on all forms, so they will be republished
		if (!correctedFormIds.isEmpty()) {
			for (int formId : correctedFormIds) {
				t.addTransactional(new TransactionalTriggerEvent(Form.class, formId, null, Events.UPDATE));
			}
		}
	}

	/**
	 * Correct the attribute "adminEmailTemplate"
	 * @param data old data
	 * @return corrected data
	 * @throws JsonProcessingException
	 */
	protected String correctAdminEmailTemplate(String data) throws JsonProcessingException {
		JsonNode dataNode = mapper.readTree(data);
		if (dataNode instanceof ObjectNode dataNodeObject) {
			dataNodeObject.put("adminEmailTemplate",
					MigrateForms.getStringFromI18n(dataNodeObject, "adminEmailTemplate").orElse(""));
			return dataNodeObject.toString();
		} else {
			return dataNode.toString();
		}
	}
}
