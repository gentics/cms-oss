package com.gentics.contentnode.etc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.FolderService;
import com.gentics.contentnode.forms.FormTransformer;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.OpResult;
import com.gentics.lib.db.SQLExecutor;

/**
 * Implementation of {@link FolderService} which prevents moving folders containing forms into other nodes,
 * which do not have all form languages activated.
 */
public class FormFolderService implements FolderService {
	@Override
	public OpResult checkMoveToOtherNode(Folder folder, Folder target, Collection<Integer> subFolderIds) throws NodeException {
		if (CollectionUtils.isEmpty(subFolderIds)) {
			return OpResult.OK;
		}
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the IDs of all forms in the moved folder structure (forms in the wastebin will also be moved)
		List<Integer> formIds = new ArrayList<>();
		DBUtils.executeMassStatement("SELECT id FROM form WHERE folder_id IN", subFolderIds, 1, new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					formIds.add(rs.getInt("id"));
				}
			}
		});
		if (formIds.isEmpty()) {
			return OpResult.OK;
		}

		List<Form> forms;
		try (WastebinFilter wb = Wastebin.INCLUDE.set()) {
			forms = t.getObjects(Form.class, formIds);
		}

		Node targetNode = target.getOwningNode();
		Set<String> targetCodes = targetNode.getLanguages().stream().map(ContentLanguage::getCode).collect(Collectors.toSet());

		// all languages by code, to output the language names
		Map<String, ContentLanguage> allLanguages = t
				.getObjects(ContentLanguage.class, DBUtils.select("SELECT id FROM contentgroup", DBUtils.IDS)).stream()
				.collect(Collectors.toMap(ContentLanguage::getCode, Function.identity(), (l1, l2) -> l1));

		List<String> problems = new ArrayList<>();
		for (Form form : forms) {
			List<String> missing = getInternalLanguageCodes(form).stream().filter(code -> !targetCodes.contains(code))
					.map(code -> allLanguages.containsKey(code) ? allLanguages.get(code).getName() : code).sorted()
					.collect(Collectors.toList());
			if (!missing.isEmpty()) {
				problems.add(String.format("%s (%s)", I18NHelper.getName(form), String.join(", ", missing)));
			}
		}

		if (problems.isEmpty()) {
			return OpResult.OK;
		}

		return OpResult.fail(Folder.class, "folder.move.form_languages_missing", I18NHelper.getName(folder),
				I18NHelper.getName(targetNode.getFolder()), String.join(", ", problems));
	}

	/**
	 * Get the language codes of the form, transformed to internal language codes (if the form type defines a language mapping)
	 * @param form form
	 * @return list of internal language codes (never null)
	 */
	protected List<String> getInternalLanguageCodes(Form form) {
		// make a copy, because the transformation will modify the list
		List<String> languages = new ArrayList<>(CollectionUtils.emptyIfNull(form.getLanguages()));
		FormTypeConfiguration config = FormConfigProvider.getConfig().get(form.getFormType());
		if (config != null) {
			languages = FormTransformer.transformToInternalLanguageCodes(config, languages);
		}
		return languages;
	}
}
