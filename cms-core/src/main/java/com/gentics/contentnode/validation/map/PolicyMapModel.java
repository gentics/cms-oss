/*
 * @author tobiassteiner
 * @date Jan 18, 2011
 * @version $Id: PolicyMapModel.java,v 1.1.2.2 2011-03-07 18:42:00 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.gentics.contentnode.validation.util.jaxb.XmlRef;
import com.gentics.contentnode.validation.util.jaxb.XmlUriAdapter;
import com.gentics.contentnode.validation.validator.impl.AntiSamyPolicy;
import com.gentics.contentnode.validation.validator.impl.AttributePolicy;
import com.gentics.contentnode.validation.validator.impl.PassThroughPolicy;

/**
 * If any JAXB annotations are modified, the schema file should be regenerated.
 * See dev.doc/build_policy_map_schema.xml.
 */ 
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "policyMap")
@XmlType(propOrder = { "inputChannels", "policyGroups", "policies" })
public class PolicyMapModel {
	@XmlElement(required = true)
	protected InputChannels inputChannels;
	@XmlElementWrapper(name = "policyGroups", required = true)
	@XmlElement(name = "policyGroup")
	protected List<PolicyGroup> policyGroups = new ArrayList<PolicyGroup>();
	@XmlElementWrapper(name = "policies", required = true)
	@XmlElements({ @XmlElement(name = "antiSamy", type = AntiSamyPolicy.class),
		@XmlElement(name = "attribute", type = AttributePolicy.class),
		@XmlElement(name = "passThrough", type = PassThroughPolicy.class) })
	protected List<Policy> policies = new ArrayList<Policy>();    
    
	@XmlAccessorType(XmlAccessType.NONE)
	public static class PolicyModel {
		@XmlAttribute
		protected Boolean convertNodeTags = true;
		@XmlAttribute
		protected String displayName;
		@XmlID
		@XmlAttribute(required = true)
		protected String id;
		@XmlJavaTypeAdapter(XmlUriAdapter.class)
		@XmlAttribute(required = true)
		protected URI uri;
	}

	@XmlAccessorType(XmlAccessType.NONE)
	@XmlType(propOrder = {})// mixed order
	public static class NodeModel {        
		@XmlElement(name = "default")
		protected PolicyRef _default; 
		@XmlElement
		protected PolicyRef fileDescription;
		@XmlElement
		protected PolicyRef fileName;
		@XmlElement
		protected PolicyRef folderDescription;
		@XmlElement
		protected PolicyRef folderName;
		@XmlElement
		protected PolicyRef mimeType;
		@XmlElement
		protected PolicyRef pageDescription;
		@XmlElement
		protected PolicyRef pageLanguage;
		@XmlElement
		protected PolicyRef pageName;
	}
    
	@XmlAccessorType(XmlAccessType.NONE)
	public static class PartTypeModel extends PolicyGroupRef {
		@XmlAttribute(required = true)
		protected int typeId;
	}
    
	@XmlAccessorType(XmlAccessType.NONE)
	@XmlType(propOrder = { "policyRefs" })
	public static class PolicyGroupModel {
		@XmlElements({ @XmlElement(name = "policy"),
			@XmlElement(name = "default", type = DefaultPolicyRef.class) })
		protected List<PolicyRef> policyRefs = new ArrayList<PolicyRef>();
		@XmlID
		@XmlAttribute(required = true)
		protected String id;
	}

	// this is used to tag a PolicyRef specified using the <default> element,
	// so that the PolicyRef specified using that particular element name
	// can be recovered.
	protected static class DefaultPolicyRef extends PolicyRef {}

	@XmlAccessorType(XmlAccessType.NONE)
	@XmlType(propOrder = { "contentAdminWrapper", "nodesWrapper", "_default" })
	protected static class InputChannels {
		@XmlElement(name = "contentAdmin", required = true)
		protected ContentAdminWrapper contentAdminWrapper;
		@XmlElement(name = "nodes", required = true)
		protected NodeWrapper nodesWrapper;
		@XmlElement(name = "default")
		protected PolicyRef _default;
	}
    
	@XmlAccessorType(XmlAccessType.NONE)
	@XmlType(propOrder = {})// any order
	protected static class ContentAdminWrapper {
		@XmlElement
		protected PolicyRef fsPath;
		@XmlElement
		protected PolicyRef groupDescription;
		@XmlElement
		protected PolicyRef groupName;
		@XmlElement
		protected PolicyRef hostName;
		@XmlElement
		protected PolicyRef nodeDescription;
		@XmlElement
		protected PolicyRef nodeName;
		@XmlElement(name = "partTypes", required = true)
		protected PartTypesWrapper partTypesWrapper;
		@XmlElement
		protected PolicyRef roleDescription;
		@XmlElement
		protected PolicyRef roleName;
		@XmlElement
		protected PolicyRef userDescription;
		@XmlElement
		protected PolicyRef userEmail;
		@XmlElement
		protected PolicyRef userFirstLastName;
		@XmlElement
		protected PolicyRef userMessage;
		@XmlElement
		protected PolicyRef userName;
	}
    
	@XmlAccessorType(XmlAccessType.NONE)
	@XmlType(propOrder = { "nodes", "_default" })
	protected static class NodeWrapper {
		@XmlElement(name = "node")
		protected List<IdentifiedNode> nodes = new ArrayList<IdentifiedNode>();
		@XmlElement(name = "default", required = true)
		protected PolicyMapImpl.NodeImpl _default;
	}
        
	@XmlAccessorType(XmlAccessType.NONE)
	protected static class IdentifiedNode extends PolicyMapImpl.NodeImpl {
		@XmlAttribute(required = true)
		protected int localId;
	}
    
	@XmlAccessorType(XmlAccessType.NONE)
	@XmlType(propOrder = { "parts", "_default" })
	protected static class PartTypesWrapper {
		@XmlElement(name = "partType")
		protected List<PolicyMap.PartType> parts = new ArrayList<PolicyMap.PartType>();
		@XmlElement(name = "default")
		protected PolicyGroupRef _default;
	}
    
	@XmlAccessorType(XmlAccessType.NONE)
	protected static class PolicyRef extends XmlRef<Policy> {
		@Override
		public Class<Policy> getRefClass() {
			return Policy.class;
		}
	}
    
	@XmlAccessorType(XmlAccessType.NONE)
	protected static class PolicyGroupRef extends XmlRef<PolicyGroup> {
		@Override
		public Class<PolicyGroup> getRefClass() {
			return PolicyGroup.class;
		}
	}
}
