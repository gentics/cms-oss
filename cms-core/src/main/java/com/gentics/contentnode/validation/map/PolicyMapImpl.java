package com.gentics.contentnode.validation.map;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import javax.xml.bind.annotation.XmlTransient;

/**
 * Default implementation for the policy map.
 * @see CompositePolicyMap
 */
public class PolicyMapImpl extends PolicyMap {
	@XmlTransient
	protected URI resolveURI;
	@XmlTransient
	protected boolean loadAsResource = false;
    
	protected PolicyMapImpl() {
		try {
			// default value used for resolving. relative paths will
			// be returned unchanged if resolved against the empty string.
			resolveURI = new URI("");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e); // should never happen
		}
	}

	@Override
	public Policy getPolicyByURI(URI policyURI) {
		for (Policy policy : policies) {
			if (policy.getURI().equals(policyURI)) {
				return policy;
			}
		}
		return null;
	}

	@Override
	public Policy getDefaultPolicy() {
		if (null != inputChannels._default) {
			return inputChannels._default.getRef();
		}
		return null;
	}

	@Override
	public PolicyMap.Node getNodeById(int localId) {
		for (IdentifiedNode node : inputChannels.nodesWrapper.nodes) {
			if (localId == node.localId) {
				return node;
			}
		}
		return null;
	}
    
	@Override
	public PolicyMap.Node getDefaultNode() {
		return inputChannels.nodesWrapper._default;
	}
    
	@Override
	public PolicyMap.PartType getPartTypeById(int typeId) {
		for (PolicyMap.PartType part : inputChannels.contentAdminWrapper.partTypesWrapper.parts) {
			if (typeId == part.typeId) {
				return part;
			}
		}
		return null;
	}

	@Override
	public PolicyGroup getDefaultPolicyGroup() {
		if (null != inputChannels.contentAdminWrapper.partTypesWrapper._default) {
			return inputChannels.contentAdminWrapper.partTypesWrapper._default.getRef();
		}
		return null;
	}

	@Override
	public Collection<Policy> getPolicies() {
		return policies;
	}

	@Override
	public Policy getUserNamePolicy() {
		if (null != inputChannels.contentAdminWrapper.userName) {
			return inputChannels.contentAdminWrapper.userName.getRef();
		}
		return null;
	}
    
	@Override
	public Policy getUserFirstLastNamePolicy() {
		if (null != inputChannels.contentAdminWrapper.userFirstLastName) {
			return inputChannels.contentAdminWrapper.userFirstLastName.getRef();
		}
		return null;
	}
    
	@Override
	public Policy getUserEmailPolicy() {
		if (null != inputChannels.contentAdminWrapper.userEmail) {
			return inputChannels.contentAdminWrapper.userEmail.getRef();
		}
		return null;
	}
    
	@Override
	public Policy getUserDescriptionPolicy() {
		if (null != inputChannels.contentAdminWrapper.userDescription) {
			return inputChannels.contentAdminWrapper.userDescription.getRef();
		}
		return null;
	}

	@Override
	public Policy getNodeDescriptionPolicy() {
		if (null != inputChannels.contentAdminWrapper.nodeDescription) {
			return inputChannels.contentAdminWrapper.nodeDescription.getRef();
		}
		return null;
	}
    
	@Override
	public Policy getNodeNamePolicy() {
		if (null != inputChannels.contentAdminWrapper.nodeName) {
			return inputChannels.contentAdminWrapper.nodeName.getRef();
		}
		return null;
	}

	@Override
	public Policy getHostNamePolicy() {
		if (null != inputChannels.contentAdminWrapper.hostName) {
			return inputChannels.contentAdminWrapper.hostName.getRef();
		}
		return null;
	}
    
	@Override
	public Policy getFsPathPolicy() {
		if (null != inputChannels.contentAdminWrapper.fsPath) {
			return inputChannels.contentAdminWrapper.fsPath.getRef();
		}
		return null;
	}
    
	@Override
	public Policy getUserMessagePolicy() {
		if (null != inputChannels.contentAdminWrapper.userMessage) {
			return inputChannels.contentAdminWrapper.userMessage.getRef();
		}
		return null;
	}
    
	@Override
	public Policy getGroupNamePolicy() {
		if (null != inputChannels.contentAdminWrapper.groupName) {
			return inputChannels.contentAdminWrapper.groupName.getRef();
		}
		return null;
	}

	@Override
	public Policy getGroupDescriptionPolicy() {
		if (null != inputChannels.contentAdminWrapper.groupDescription) {
			return inputChannels.contentAdminWrapper.groupDescription.getRef();
		}
		return null;
	}

	@Override
	public Policy getRoleNamePolicy() {
		if (null != inputChannels.contentAdminWrapper.roleName) {
			return inputChannels.contentAdminWrapper.roleName.getRef();
		}
		return null;
	}

	@Override
	public Policy getRoleDescriptionPolicy() {
		if (null != inputChannels.contentAdminWrapper.roleDescription) {
			return inputChannels.contentAdminWrapper.roleDescription.getRef();
		}
		return null;
	}

	/**
	 * If the {@link #loadAsResource} flag is true, such as when {@link PolicyMap#loadDefault()} is used,
	 * all locations must be given as names of resources. Otherwise, if this PolicyMap was loaded
	 * from a URI, the location may be a relative URI that will be resolved against the URI the
	 * this instance was loaded from. Otherwise, the location must be an absolute URL.
	 * This method is used by policies that refer to external files.
	 */
	@Override
	public InputStream getLocationAsStream(String location) throws IOException {
		if (loadAsResource) {
			InputStream stream = PolicyMapImpl.class.getResourceAsStream(location);

			if (null == stream) {
				throw new IOException("resource not found: `" + location + "'");
			}
			return stream;
		} else {
			return resolveURI.resolve(location).toURL().openStream();
		}
	}
    
	public static class NodeImpl extends Node {        
        
		@Override
		public Policy getDefaultPolicy() {
			if (null != _default) {
				return _default.getRef();
			}
			return null;
		}
        
		@Override
		public Policy getFolderNamePolicy() {
			if (null != folderName) {
				return folderName.getRef();
			}
			return null;
		}
        
		@Override
		public Policy getFolderDescriptionPolicy() {
			if (null != folderDescription) {
				return folderDescription.getRef();
			}
			return null;
		}
        
		@Override
		public Policy getPageNamePolicy() {
			if (null != pageName) {
				return pageName.getRef();
			}
			return null;
		}
        
		@Override
		public Policy getPageLanguagePolicy() {
			if (null != pageLanguage) {
				return pageLanguage.getRef();
			}
			return null;
		}
        
		@Override
		public Policy getPageDescriptionPolicy() {
			if (null != pageDescription) {
				return pageDescription.getRef();
			}
			return null;
		}
        
		@Override
		public Policy getFileDescriptionPolicy() {
			if (null != fileDescription) {
				return fileDescription.getRef();
			}
			return null;
		}
        
		@Override
		public Policy getFileNamePolicy() {
			if (null != fileName) {
				return fileName.getRef();
			}
			return null;
		}

		@Override
		public Policy getMimeTypePolicy() {
			if (null != mimeType) {
				return mimeType.getRef();
			}
			return null;
		}
	}
}
