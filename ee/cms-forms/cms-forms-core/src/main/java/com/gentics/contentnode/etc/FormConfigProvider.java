package com.gentics.contentnode.etc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationModel;
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
		case "label":
			return I18NHelper.get(config.labelI18n);
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
	public void init(NodePreferences preferences) {
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
				logger.warn("Ignoring empty form type configuration for '%s'".formatted(entry.getKey()));
			});
			// remove empty configurations
			configurationMap.values().removeIf(c -> c == null);
		} catch (IllegalArgumentException e) {
			logger.error("Error while reading form type configuration", e.getCause());
			configurationMap = null;
		}
	}

	/**
	 * Check whether the formtypes are consistent.
	 * This will log errors, when
	 * <ul>
	 * <li>Forms exist that use unknown formtypes</li>
	 * <li>Forms exist in nodes that do not have the formtype assigned</li>
	 * </ul>
	 */
	public void check() {
		try {
			// first check whether the migration has been done
			boolean migrationDone = Trx.supply(() -> {
				return DBUtils.tableExists("node_formtype") && DBUtils.columnExists("form", "formtype");
			});

			if (migrationDone) {
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
							logger.error("Node '%s' contains forms of unknown formtype '%s'".formatted(nodeName, formType));
							consistent.set(false);
						} else if (!assignedFormTypes.getOrDefault(nodeId, Collections.emptySet()).contains(formType)) {
							logger.error("Node '%s' contains forms of formtype '%s', which is not assigned to the node"
									.formatted(nodeName, formType));
							consistent.set(false);
						}
					});
				});

				if (consistent.get()) {
					logger.info("Formtype configuration is consistent");
				} else {
					logger.error("Formtype configuration is not consistent (see previous errors)");
				}
			} else {
				logger.info("Formtype consistency not checked, since migration is not done");
			}
		} catch (NodeException e) {
			logger.error("Error while checking formtype consistency", e);
		}
	}
}
