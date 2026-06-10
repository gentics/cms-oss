package com.gentics.contentnode.etc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.render.FormDirective;
import com.gentics.contentnode.rest.model.formtype.BlockConfig;
import com.gentics.contentnode.rest.model.formtype.FormGenTranslations;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationModel;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;

/**
 * Implementation of {@link ConfigurationService} which parses and validates the form type configuration
 */
public final class FormConfigProvider implements ConfigurationService {
	private static final NodeLogger logger = NodeLogger.getNodeLogger(FormConfigProvider.class);

	/**
	 * Configuration path to the type configurations
	 */
	public static final String TYPES_CONFIG_PATH = "forms.types";

	/**
	 * Configuration path to the type configurations
	 */
	public static final String TRANSLATIONS_CONFIG_PATH = "forms.formGenTranslations";

	/**
	 * Configuration name of the velocity configuration
	 */
	public static final String VTL_CONFIG_NAME = "velocity";

	/**
	 * Configuration name of the velocity userdirective configuration
	 */
	public static final String VT_USERDIRECTIVE_CONFIG_NAME = "userdirective";

	/**
	 * Key ("formtype") of the stored global translations
	 */
	public static final String TRANSLATIONS_GLOBAL_KEY = "---";

	/**
	 * Object Mapper
	 */
	private static ObjectMapper mapper = new ObjectMapper();

	/**
	 * Form type configuration map
	 */
	private static FormTypeConfigurationMap configurationMap;

	/**
	 * Default formGen translations
	 */
	private static FormGenTranslations formGenTranslations = new FormGenTranslations();

	/**
	 * Transform the given entry of {@link FormTypeConfigurationMap} to an instance of {@link FormTypeConfigurationModel}
	 */
	public final static Function<Map.Entry<String, FormTypeConfiguration>, FormTypeConfigurationModel> TRANSFORM2REST = entry -> {
		String type = entry.getKey();
		FormTypeConfiguration typeConfiguration = entry.getValue();

		ObjectMapper mapper = new ObjectMapper();
		FormTypeConfigurationModel model = mapper.convertValue(typeConfiguration, FormTypeConfigurationModel.class);
		model.type = type;
		return model;
	};

	/**
	 * Resolver for entries of {@link FormTypeConfigurationMap}, which is used to filter and sort lists of {@link FormTypeConfigurationModel}
	 */
	public final static BiFunction<Map.Entry<String, FormTypeConfiguration>, String, Object> RESOLVER = (o, key) -> {
		String type = o.getKey();
		FormTypeConfiguration config = o.getValue();
		switch (key) {
		case "type":
			return type;
		case "name":
			return I18NHelper.get(config.nameI18n);
		case "description":
			return I18NHelper.get(config.descriptionI18n);
		case "external":
			return config.external;
		case "submitOnce":
			return config.submitOnce;
		default:
			return null;
		}
	};

	/**
	 * Get the current (valid) configuration map
	 * @return configuration map
	 */
	public final static FormTypeConfigurationMap getConfig() {
		return configurationMap;
	}

	/**
	 * Check whether the current configuration is valid and not empty
	 * @return true if a non-empty valid configuration was found
	 */
	public final static boolean isConfigValid() {
		if (MapUtils.isNotEmpty(configurationMap)) {
			return true;
		} else {
			logger.error("No valid form type configuration for forms found");
			return false;
		}
	}

	/**
	 * Get the default formGen translations
	 * @param overwriteWithStored true to overwrite with stored global translations
	 * @return translations
	 * @throws NodeException
	 */
	public final static FormGenTranslations getFormGenTranslations(boolean overwriteWithStored) throws NodeException {
		if (overwriteWithStored) {
			return formGenTranslations.overwrite(getStoredFormGenTranslations());
		} else {
			return formGenTranslations;
		}
	}

	/**
	 * Get the formGen translations for the given form type
	 * @param formType form type
	 * @param overwriteWithStored true to overwrite with stored global translations
	 * @return translations
	 * @throws NodeException
	 */
	public final static FormGenTranslations getFormGenTranslations(String formType, boolean overwriteWithStored) throws NodeException {
		FormGenTranslations custom = Optional.ofNullable(configurationMap).map(m -> m.get(formType))
				.map(c -> c.formGenTranslations).orElse(null);

		if (overwriteWithStored) {
			return getFormGenTranslations(overwriteWithStored).overwrite(custom)
					.overwrite(getStoredFormGenTranslations(formType));
		} else {
			return getFormGenTranslations(true).overwrite(custom);
		}
	}

	/**
	 * Get the stored global formGen translations
	 * @return translations
	 * @throws NodeException
	 */
	public final static FormGenTranslations getStoredFormGenTranslations() throws NodeException {
		return getStoredFormGenTranslations(TRANSLATIONS_GLOBAL_KEY);
	}

	/**
	 * Get the stored formGen translations for the given form type
	 * @param formType form type
	 * @return translations
	 * @throws NodeException
	 */
	public final static FormGenTranslations getStoredFormGenTranslations(String formType) throws NodeException {
		String json = DBUtils.select("SELECT translations FROM form_translations WHERE formtype = ?", pst -> {
			pst.setString(1, formType);
		}, DBUtils.firstString("translations"));

		if (StringUtils.isEmpty(json)) {
			return new FormGenTranslations();
		} else {
			try {
				return mapper.readValue(json, FormGenTranslations.class);
			} catch (JsonProcessingException e) {
				throw new NodeException("Error while reading stored global translations %s".formatted(json), e);
			}
		}
	}

	/**
	 * Store the diff for global translations
	 * @param translations translations diff
	 * @throws NodeException
	 */
	public final static void storeFormGenTranslations(FormGenTranslations translations) throws NodeException {
		storeFormGenTranslations(TRANSLATIONS_GLOBAL_KEY, translations);
	}

	/**
	 * Store the diff for fortype translations
	 * @param formType form type
	 * @param translations translations diff
	 * @throws NodeException
	 */
	public final static void storeFormGenTranslations(String formType, FormGenTranslations translations) throws NodeException {
		try {
			String json = mapper.writeValueAsString(translations);
			DBUtils.updateOrInsert("form_translations", Map.of("formtype", formType),
					Map.of("translations", json));
		} catch (JsonProcessingException e) {
			throw new NodeException("Error while saving stored global translations %s".formatted(translations), e);
		}
	}

	/**
	 * Generate the diff between the original translations and the given overwrite.
	 * @param orig original translations
	 * @param overwrite translations to overwrite
	 * @return diff
	 */
	public final static FormGenTranslations diff(FormGenTranslations orig, FormGenTranslations overwrite) {
		FormGenTranslations diff = overwrite.clone();

		// remove all keys, which do not exist in the original
		diff.keySet().retainAll(orig.keySet());

		orig.entrySet().forEach(entry -> {
			String key = entry.getKey();
			Map<String, String> translations = entry.getValue();

			if (diff.containsKey(key)) {
				Map<String, String> overwriteTranslations = diff.get(key);

				translations.entrySet().forEach(translationEntry -> {
					String language = translationEntry.getKey();
					String translation = translationEntry.getValue();

					overwriteTranslations.remove(language, translation);
				});

				if (overwriteTranslations.isEmpty()) {
					diff.remove(key);
				}
			}
		});

		return diff;
	}

	@Override
	public List<Message> init(NodePreferences preferences) {
		ensureFormDirective(preferences);

		List<Message> messages = new ArrayList<>();
		try {
			configurationMap = mapper.convertValue(
					NodeConfigRuntimeConfiguration.getPreferences().getPropertyObject(TYPES_CONFIG_PATH),
					FormTypeConfigurationMap.class);

			if (configurationMap == null) {
				configurationMap = new FormTypeConfigurationMap();
			}

			// warn about empty configurations
			configurationMap.entrySet().stream().filter(entry -> entry.getValue() == null).forEach(entry -> {
				String msg = I18NHelper.get("init.formtype.empty", entry.getKey());
				logger.warn(msg);
				messages.add(new Message(Type.WARNING, msg));
			});
			// remove empty configurations
			configurationMap.values().removeIf(c -> c == null);
		} catch (IllegalArgumentException e) {
			logger.error("Error while reading form type configuration", e.getCause());
			String msg = I18NHelper.get("init.formtype.error", e.getLocalizedMessage());
			messages.add(new Message(Type.CRITICAL, msg));
			configurationMap = null;
		}

		try {
			formGenTranslations = mapper.convertValue(
					NodeConfigRuntimeConfiguration.getPreferences().getPropertyObject(TRANSLATIONS_CONFIG_PATH),
					FormGenTranslations.class);
			if (formGenTranslations == null) {
				formGenTranslations = new FormGenTranslations();
			}
		} catch (IllegalArgumentException e) {
			logger.error("Error while reading form gen translations", e.getCause());
			String msg = I18NHelper.get("init.formgen.error", e.getLocalizedMessage());
			messages.add(new Message(Type.CRITICAL, msg));
			formGenTranslations = new FormGenTranslations();
		}

		return messages;
	}

	/**
	 * Check whether the formtypes are consistent.
	 * This will log errors, when
	 * <ul>
	 * <li>Forms exist that use unknown formtypes</li>
	 * <li>Forms exist in nodes that do not have the formtype assigned</li>
	 * <li>Form type configurations are inconsistent</li>
	 * </ul>
	 */
	public List<Message> check() {
		List<Message> messages = new ArrayList<>();
		try {
			// first check whether the migration has been done
			boolean migrationDone = Trx.supply(() -> {
				return DBUtils.tableExists("node_formtype") && DBUtils.columnExists("form", "formtype");
			});

			if (!migrationDone) {
				logger.info("Formtype consistency not checked, since migration is not done");
			} else if (configurationMap != null) {
				AtomicBoolean consistent = new AtomicBoolean(true);
				Map<Integer, Set<String>> assignedFormTypes = Trx.supply(() -> FormFactory.getFormTypeAssignment());
				Map<Integer, Set<String>> usedFormTypes = Trx.supply(() -> FormFactory.getFormTypeUsage());

				Map<Integer, String> nodeNames = new HashMap<>();
				Trx.consume(nodeIds -> {
					List<Node> nodes = TransactionManager.getCurrentTransaction().getObjects(Node.class, nodeIds);
					for (Node node : nodes) {
						nodeNames.put(node.getId(), I18NHelper.getName(node));
					}
				}, usedFormTypes.keySet());

				usedFormTypes.entrySet().stream().forEach(entry -> {
					int nodeId = entry.getKey();
					String nodeName = nodeNames.getOrDefault(nodeId, "<Unknown Node>");

					entry.getValue().stream().forEach(formType -> {
						if (!configurationMap.containsKey(formType)) {
							String msg = I18NHelper.get("init.formtype.unknown", nodeName, formType);
							logger.error(msg);
							messages.add(new Message(Type.CRITICAL, msg));
							consistent.set(false);
						} else if (!assignedFormTypes.getOrDefault(nodeId, Collections.emptySet()).contains(formType)) {
							String msg = I18NHelper.get("init.formtype.unassigned", nodeName, formType);
							logger.error(msg);
							messages.add(new Message(Type.CRITICAL, msg));
							consistent.set(false);
						}
					});
				});
				configurationMap.entrySet().stream().forEach(entry -> {
					String formType = entry.getKey();
					FormTypeConfiguration config = entry.getValue();
					Optional.ofNullable(config.blocks).ifPresent(blocks -> {
						for (Entry<String, BlockConfig> blockEntry : blocks.entrySet()) {
							String blockType = blockEntry.getKey();
							BlockConfig blockConfig = blockEntry.getValue();
							Optional.ofNullable(blockConfig.settings).ifPresent(settings -> {
								settings.stream().forEach(setting -> {
									if (setting.backend) {
										String msg = I18NHelper.get("init.formtype.block.backendsetting", formType, blockType, setting.id);
										logger.error(msg);
										messages.add(new Message(Type.CRITICAL, msg));
										consistent.set(false);
									}
								});
							});
						}
					});

					if (MapUtils.isNotEmpty(config.languageMapping)) {
						// if the size of the map changes when inverting, the mapping is not unique
						if (config.languageMapping.size() != MapUtils.invertMap(config.languageMapping).size()) {
							String msg = I18NHelper.get("init.formtype.languageMapping.error", formType,
									config.languageMapping.values().toString());
							logger.error(msg);
							messages.add(new Message(Type.CRITICAL, msg));
							consistent.set(false);
						}
					}
				});

				if (consistent.get()) {
					logger.info("Formtype configuration is consistent");
				} else {
					String msg = I18NHelper.get("init.formtype.inconsistent").toString();
					messages.add(new Message(Type.CRITICAL, msg));
					logger.error(msg);
				}
			}
		} catch (NodeException e) {
			String msg = I18NHelper.get("init.formtype.check.error", e.getLocalizedMessage());
			messages.add(new Message(Type.CRITICAL, msg));
			logger.error("Error while checking formtype consistency", e);
		}
		return messages;
	}

	/**
	 * Make sure that the FormDirective is configured for velocity
	 * @param preferences preferences
	 */
	protected void ensureFormDirective(NodePreferences preferences) {
		Optional.ofNullable(preferences.getPropertyMap(VTL_CONFIG_NAME)).ifPresent(vtlConfig -> {
			String vtlUserDirective = vtlConfig.getOrDefault(VT_USERDIRECTIVE_CONFIG_NAME, "").toString();

			List<String> directives = new ArrayList<>(Arrays.asList(StringUtils.split(vtlUserDirective, ',')));
			String formDirectiveName = FormDirective.class.getName();
			if (!directives.contains(formDirectiveName)) {
				directives.add(formDirectiveName);
				vtlUserDirective = directives.stream().collect(Collectors.joining(","));
				vtlConfig.put(VT_USERDIRECTIVE_CONFIG_NAME, vtlUserDirective);
			}
		});
	}
}
