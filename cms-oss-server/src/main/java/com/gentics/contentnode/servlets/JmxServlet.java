package com.gentics.contentnode.servlets;

import com.gentics.contentnode.security.AccessControlService;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.function.Supplier;
import jakarta.inject.Inject;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;
import org.jvnet.hk2.annotations.Service;

/**
 * Servlet, that exposes configured JMX Beans in JSON format
 */
@Service
public class JmxServlet extends HttpServlet {

	/**
	 * Logger
	 */
	private final NodeLogger logger = NodeLogger.getNodeLogger(JmxServlet.class);

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1353475253742768499L;

	/**
	 * Access control
	 */
	@Inject
	private AccessControlService accessControlService;

	private final ObjectMapper mapper = new ObjectMapper();

	private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

	private Supplier<NodePreferences> configSupplier;


	@Override
	public void init() {
		if (configSupplier == null) {
			accessControlService = new AccessControlService("jmx");
		} else {
			accessControlService = new AccessControlService("jmx", configSupplier);
		}
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.disable(MapperFeature.USE_GETTERS_AS_SETTERS);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		if (!accessControlService.verifyAccess(req, res)) {
			return;
		}

		NodePreferences nodePreferences = getNodePreferences();
		Map<?, ?> propertyMap = nodePreferences.getPropertyMap("jmx");

		ObjectNode output = mapper.createObjectNode();

		for (Entry<?, ?> entry : propertyMap.entrySet()) {
			String name = ObjectTransformer.getString(entry.getKey(), "");

			try {
				ObjectName objectName = new ObjectName(name);
				Collection<String> attributes = getEntryAttributes(nodePreferences, entry, name);

				ObjectNode dataObject = mapper.createObjectNode();
				for (Object attr : attributes) {
					Object value;
					try {
						value = mBeanServer.getAttribute(objectName, attr.toString());
						addAttributeValue(dataObject, attr.toString(), value);
					} catch (Exception e) {
						logger.error(
								String.format("Error while getting attribute {%s} for bean {%s}", attr, name), e);
					}
				}
				output.set(name, dataObject);
			} catch (Exception exception) {
				logger.error(String.format("Error while getting attributes for bean {%s}", name),
						exception);
			}
		}

		mapper.writeValue(res.getWriter(), output);
	}

	private NodePreferences getNodePreferences() {
		if (configSupplier == null) {
			return NodeConfigRuntimeConfiguration
					.getPreferences();
		}
		return configSupplier.get();
	}

	/**
	 * Setting AccesControl service triggers reloading the configuration
	 */
	public void setAccessControlService(AccessControlService accessControlService) {
		this.accessControlService = accessControlService;
	}

	public void setConfigurationSupplier(Supplier<NodePreferences> configSupplier) {
		this.configSupplier = configSupplier;
	}

	private <T, V> Collection<String> getEntryAttributes(NodePreferences nodePreferences,
			Entry<T, V> entry, String name)
			throws Exception {
		Collection<String> attributes;
		String attribute = ObjectTransformer.getString(entry.getValue(), "");

		if ("*".equals(attribute)) {
			attributes = new ArrayList<>();
			MBeanAttributeInfo[] attrInfos = mBeanServer.getMBeanInfo(new ObjectName(name))
					.getAttributes();

			for (MBeanAttributeInfo attrInfo : attrInfos) {
				if (attrInfo.isReadable()) {
					attributes.add(attrInfo.getName());
				}
			}
		} else {
			var value = entry.getValue();

			if (value instanceof String) {
				attributes = Collections.singleton(attribute);
			} else {
				attributes = (List<String>) value;
			}
		}

		return attributes;
	}

	/**
	 * Transform a value from a JMX Bean into an attribute of the Json Object
	 *
	 * @param object    Json Object
	 * @param attribute attribute name
	 * @param value     value to transform
	 * @throws JSONException
	 */
	protected void addAttributeValue(ObjectNode object, String attribute, Object value)
			throws JSONException {
		if (value instanceof CompositeDataSupport) {
			object.set(attribute, transform((CompositeDataSupport) value));
		} else if (value instanceof Map<?, ?>) {
			object.set(attribute, transform((Map<?, ?>) value));
		} else if (value instanceof Double) {
			object.put(attribute, (Double) value);
		} else if (value instanceof Float) {
			object.put(attribute, (Float) value);
		} else if (value instanceof Long) {
			object.put(attribute, (Long) value);
		} else if (value instanceof Integer) {
			object.put(attribute, (Integer) value);
		} else if (value instanceof Short) {
			object.put(attribute, (Short) value);
		} else if (value instanceof Boolean) {
			object.put(attribute, (Boolean) value);
		} else if (value instanceof BigDecimal) {
			object.put(attribute, (BigDecimal) value);
		} else if (value instanceof char[]) {
			ArrayNode arrayNode = mapper.createArrayNode();
			char[] valueArray = (char[]) value;
			for (char c : valueArray) {
				arrayNode.add(c);
			}
			object.set(attribute, arrayNode);
		} else if (value instanceof byte[]) {
			ArrayNode arrayNode = mapper.createArrayNode();
			byte[] valueArray = (byte[]) value;
			for (byte b : valueArray) {
				arrayNode.add(b);
			}
			object.set(attribute, arrayNode);
		} else if (value instanceof int[]) {
			ArrayNode arrayNode = mapper.createArrayNode();
			int[] valueArray = (int[]) value;
			for (int j : valueArray) {
				arrayNode.add(j);
			}
			object.set(attribute, arrayNode);
		} else if (value instanceof long[]) {
			ArrayNode arrayNode = mapper.createArrayNode();
			long[] valueArray = (long[]) value;
			for (long l : valueArray) {
				arrayNode.add(l);
			}
			object.set(attribute, arrayNode);
		} else if (value instanceof float[]) {
			ArrayNode arrayNode = mapper.createArrayNode();
			float[] valueArray = (float[]) value;
			for (float v : valueArray) {
				arrayNode.add(v);
			}
			object.set(attribute, arrayNode);
		} else if (value instanceof double[]) {
			ArrayNode arrayNode = mapper.createArrayNode();
			double[] valueArray = (double[]) value;
			for (double v : valueArray) {
				arrayNode.add(v);
			}
			object.set(attribute, arrayNode);
		} else if (value instanceof Object[]) {
			object.set(attribute, transform((Object[]) value));
		} else {
			object.put(attribute, ObjectTransformer.getString(value, null));
		}
	}

	/**
	 * Transform into an ObjectNode
	 *
	 * @param data data to transform
	 * @return transformed data
	 * @throws JSONException
	 */
	protected ObjectNode transform(CompositeDataSupport data) throws JSONException {
		ObjectNode objectNode = mapper.createObjectNode();
		for (String key : data.getCompositeType().keySet()) {
			addAttributeValue(objectNode, key, data.get(key));
		}
		return objectNode;
	}

	/**
	 * Transform into an ObjectNode
	 *
	 * @param data data to transform
	 * @return transformed data
	 * @throws JSONException
	 */
	protected ObjectNode transform(Map<?, ?> data) throws JSONException {
		ObjectNode objectNode = mapper.createObjectNode();
		for (Map.Entry<?, ?> entry : data.entrySet()) {
			String key = ObjectTransformer.getString(entry.getKey(), null);
			if (!ObjectTransformer.isEmpty(key)) {
				addAttributeValue(objectNode, key, entry.getValue());
			}
		}
		return objectNode;
	}

	/**
	 * Transform into an ArrayNode
	 *
	 * @param data data to transform
	 * @return transformed data
	 * @throws JSONException
	 */
	protected ArrayNode transform(Object[] data) throws JSONException {
		ArrayNode arrayNode = mapper.createArrayNode();
		for (Object value : data) {
			if (value instanceof CompositeDataSupport) {
				arrayNode.add(transform((CompositeDataSupport) value));
			} else if (value instanceof Map<?, ?>) {
				arrayNode.add(transform((Map<?, ?>) value));
			} else if (value instanceof Double) {
				arrayNode.add((Double) value);
			} else if (value instanceof Float) {
				arrayNode.add((Float) value);
			} else if (value instanceof Long) {
				arrayNode.add((Long) value);
			} else if (value instanceof Integer) {
				arrayNode.add((Integer) value);
			} else if (value instanceof Short) {
				arrayNode.add((Short) value);
			} else if (value instanceof Boolean) {
				arrayNode.add((Boolean) value);
			} else if (value instanceof BigDecimal) {
				arrayNode.add((BigDecimal) value);
			} else if (value instanceof Object[]) {
				arrayNode.add(transform((Object[]) value));
			} else {
				arrayNode.add(ObjectTransformer.getString(value, null));
			}
		}
		return arrayNode;
	}
}
