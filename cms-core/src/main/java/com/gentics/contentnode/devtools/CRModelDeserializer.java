package com.gentics.contentnode.devtools;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.gentics.contentnode.devtools.model.AbstractCRModel;
import com.gentics.contentnode.devtools.model.MeshCRModel;
import com.gentics.contentnode.devtools.model.NonMeshCRModel;

/**
 * Custom deserializer that decides by crType, which Model to use
 */
public class CRModelDeserializer extends StdDeserializer<AbstractCRModel> {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 258788138259039462L;

	/**
	 * Create instance
	 */
	public CRModelDeserializer() {
		this(null);
	}

	/**
	 * Create instance
	 * @param vc Value Class
	 */
	protected CRModelDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public AbstractCRModel deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = p.getCodec();

		JsonNode node = oc.readTree(p);

		if (node.has("crType") && "mesh".equals(node.get("crType").asText())) {
			return oc.treeToValue(node, MeshCRModel.class);
		} else {
			return oc.treeToValue(node, NonMeshCRModel.class);
		}
	}
}
