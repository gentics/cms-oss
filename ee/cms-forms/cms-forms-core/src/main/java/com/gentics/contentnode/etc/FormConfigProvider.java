package com.gentics.contentnode.etc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections.MapUtils;

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
import com.gentics.contentnode.rest.model.formtype.BlockConfig;
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
	 * Form type configuration map
	 */
	private static FormTypeConfigurationMap configurationMap;

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
		if (!MapUtils.isEmpty(configurationMap)) {
			return true;
		} else {
			logger.error("No valid form type configuration for forms found");
			return false;
		}
	}

	@Override
	public List<Message> init(NodePreferences preferences) {
		List<Message> messages = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			configurationMap = mapper.convertValue(
					NodeConfigRuntimeConfiguration.getPreferences().getPropertyObject(TYPES_CONFIG_PATH),
					FormTypeConfigurationMap.class);

			if (configurationMap == null) {
				configurationMap = new FormTypeConfigurationMap();
			}

			// warn about empty configurations
			configurationMap.entrySet().stream().filter(entry -> entry.getValue() == null).forEach(entry -> {
				String msg = "Ignoring empty form type configuration for '%s'".formatted(entry.getKey());
				logger.warn(msg);
				messages.add(new Message(Type.WARNING, msg));
			});
			// remove empty configurations
			configurationMap.values().removeIf(c -> c == null);
		} catch (IllegalArgumentException e) {
			logger.error("Error while reading form type configuration", e.getCause());
			messages.add(new Message(Type.CRITICAL,
					"Error while reading form type configuration: %s".formatted(e.getLocalizedMessage())));
			configurationMap = null;
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
				messages.add(new Message(Type.INFO, "Formtype consistency not checked, since migration is not done"));
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
							String msg = "Node '%s' contains forms of unknown formtype '%s'".formatted(nodeName, formType);
							logger.error(msg);
							messages.add(new Message(Type.CRITICAL, msg));
							consistent.set(false);
						} else if (!assignedFormTypes.getOrDefault(nodeId, Collections.emptySet()).contains(formType)) {
							String msg = "Node '%s' contains forms of formtype '%s', which is not assigned to the node"
									.formatted(nodeName, formType);
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
										String msg = "Formtype configuration '%s' contains block configuration '%s' with setting '%s' having backend=true, which is not allowed."
												.formatted(formType, blockType, setting.id);
										logger.error(msg);
										messages.add(new Message(Type.CRITICAL, msg));
										consistent.set(false);
									}
								});
							});
						}
					});
				});

				if (consistent.get()) {
					String msg = "Formtype configuration is consistent";
					messages.add(new Message(Type.INFO, msg));
					logger.info(msg);
				} else {
					String msg = "Formtype configuration is not consistent (see previous errors)";
					messages.add(new Message(Type.CRITICAL, msg));
					logger.error(msg);
				}
			}
		} catch (NodeException e) {
			messages.add(new Message(Type.CRITICAL, "Error while checking formtype consistency: %s".formatted(e.getLocalizedMessage())));
			logger.error("Error while checking formtype consistency", e);
		}
		return messages;
	}
}
