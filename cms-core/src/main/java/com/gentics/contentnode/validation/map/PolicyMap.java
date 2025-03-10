/*
 * @author tobiassteiner
 * @date Jan 17, 2011
 * @version $Id: PolicyMap.java,v 1.1.2.2 2011-03-07 18:42:00 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import com.gentics.contentnode.validation.util.jaxb.XmlFinishedUnmarshalPropagator;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * Provides an abstract interface for the policy map and static helper
 * methods to load associated resources.
 * 
 * @see PolicyMap.Node for the rationale about why this class is abstract.
 */
public abstract class PolicyMap extends PolicyMapModel {    
	public enum POLICIES {

		/**
		 * The built-in policy for element content
		 * (see policy-map.default.xml).
		 */
		PARANOID_POLICY("http://www.gentics.com/validation/policy/paranoid"), /**
		 * The built-in policy for attributes containing URIs
		 * (see policy-map.default.xml). 
		 */ ANY_URI_POLICY("http://www.gentics.com/validation/policy/anyUri"), /**
		 * A pass-through policy that successfully validates any content unchanged
		 * (see policy-map.default.xml).
		 */ ANY_CONTENT_POLICY("http://www.gentics.com/validation/policy/anyContent");

		private final URI uri;

		private POLICIES(String uri) {
			try {
				this.uri = new URI(uri);
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}

		public URI getURI() {
			return uri;
		}
	}
    
	/**
	 * Configuration for node-bound input channels.
	 * This is abstract so we can implement a composite node.
	 * 
	 * This class is abstract, so that we can implement the
	 * {@link CompositePolicyMap.CompositeNode} - it lets us override only
	 * specific input in the user policy map, without overriding the defaults
	 * in the developer-supplied policy map - essentially it means that
	 * if the user has overridden a node in the user policy map, be it the
	 * default node or a node with a specific id, in order to change e.g. only
	 * the policy for folder names, the developer can still change the policy
	 * for all other inputs (e.g. the folder description).
	 * 
	 * Without the {@link CompositePolicyMap.CompositeNode}, if the user
	 * overrides a node in the user policy map, the entire node configuration
	 * will be overridden (and possibly the global default policy will be used
	 * for non-specified inputs).
	 *  
	 * Consider this logic when adding other inputs, or when extending the
	 * {@link PartType} interface.
	 */
	public abstract static class Node extends NodeModel {        

		/**
		 * @return the Policy that was specified as the &lt;default>
		 *   for this Node, or null if none was specified.
		 */
		public abstract Policy getDefaultPolicy();

		/**
		 * @return the Policy to use for folder names, or null if none
		 *   was specified.
		 */
		public abstract Policy getFolderNamePolicy();
        
		/**
		 * @return the Policy to use for folder descriptions, or null
		 *   if none was specified.
		 */
		public abstract Policy getFolderDescriptionPolicy();

		/**
		 * @return the Policy to use for page names, or null if none
		 *   was specified.
		 */
		public abstract Policy getPageNamePolicy();
        
		/**
		 * @return the Policy to use for page languages, or null if none
		 *   was specified.
		 */
		public abstract Policy getPageLanguagePolicy();
        
		/**
		 * @return the Policy to use for page descriptions, or null if none
		 *   was specified.
		 */
		public abstract Policy getPageDescriptionPolicy();
        
		/**
		 * @return the Policy to use for file descriptions, or null if none
		 *   was specified.
		 */
		public abstract Policy getFileDescriptionPolicy();
        
		/**
		 * @return the Policy to use for file names, or null if none
		 *   was specified.
		 */
		public abstract Policy getFileNamePolicy();

		/**
		 * @return the Poliocy to use for mime-types, or null if none
		 *   was specified.
		 */
		public abstract Policy getMimeTypePolicy();
	}
    
	/**
	 * The {@link PolicyGroup} reference for a part-type. 
	 * An abstract class is not needed (contrary to {@link Node}), since we
	 * only provide a single getter that will never be null. 
	 * 
	 * @see Node for the rationale about the abstract interface.
	 */
	public static class PartType extends PartTypeModel {

		/**
		 * @return the Policy group referred to by this PartType.
		 */
		public PolicyGroup getPolicyGroup() {
			return getRef();
		}
	}
    
	/**
	 * The default AntiSamy policy file resource.
	 */
	protected final static String DEFAULT_ANTI_SAMY_POLICY_RESOURCE = "antisamy-paranoid.xml";
    
	/**
	 * The default policy map resource bundled with the application.
	 */
	protected final static String DEFAULT_POLICY_MAP_RESOURCE = "policy-map.default.xml";
    
	/**
	 * The XML Schema resource for the PolicyMap.
	 */
	protected final static String POLICY_MAP_SCHEMA_RESOURCE = "policy-map.xsd";
    
	/**
	 * Searches for a Policy with the given URI in this PolicyMap.
	 * @return the Policy with the given URI, or null, of no such Policy exists.
	 */
	public abstract Policy getPolicyByURI(URI policyURI);
    
	/**
	 * @return the global default Policy - the ultimative fallback, so to speak, if
	 * no other defaults are appropritate, or null if no such policy exists.
	 */
	public abstract Policy getDefaultPolicy();
    
	/**
	 * @return the Node specified in the &lt;nodes> section
	 *   that has the given local id, or null if no such Node exists.
	 */
	public abstract PolicyMap.Node getNodeById(int localId);
    
	/**
	 * @return the node specified as the &lt;default> in the &lt;nodes>
	 *   section of the policy map, or null if no default node exists.
	 */
	public abstract PolicyMap.Node getDefaultNode();
    
	/**
	 * @return the PartType in the &lt;partTypes> section
	 *   that has the given type id, or null, if no such Part exists.
	 */
	public abstract PolicyMap.PartType getPartTypeById(int typeId);
    
	/**
	 * @return the PolicyGroup that was specified as the &lt;default> in the
	 *   &lt;parts> child of this Node, or null, if none was specified.
	 */
	public abstract PolicyGroup getDefaultPolicyGroup();
    
	/**
	 * Loads a resource referred to inside the policy map. What this resource
	 * is (an actual class resource or a file located by an URL) is up to the
	 * implementation.
	 * @param location an arbitrary string - what it means is up to the impl.
	 * @return an InputStream for the resource, should be closed by the caller
	 *   when not needed any longer.
	 */
	public abstract InputStream getLocationAsStream(String location) throws IOException;
    
	/**
	 * @return a Collection of all policies defined in this PolicyMap instance.
	 */
	public abstract Collection<Policy> getPolicies();

	/**
	 * @return the Policy to use for the login-name of a
	 *   user, or null if none was specified.
	 */
	public abstract Policy getUserNamePolicy();
    
	/**
	 * @return the Policy to use for the first and last name of a
	 *   user, or null if none was specified.
	 */
	public abstract Policy getUserFirstLastNamePolicy();
        
	/**
	 * @return the Policy to use for the email of a
	 *   user, or null if none was specified.
	 */
	public abstract Policy getUserEmailPolicy();
    
	/**
	 * @return the Policy to use for the description of a user, or
	 *   null if none was specified.
	 */
	public abstract Policy getUserDescriptionPolicy();

	/**
	 * @return the Policy to use for the description of a node, or
	 *   null if none was specified.
	 */
	public abstract Policy getNodeDescriptionPolicy();
    
	/**
	 * @return the Policy to use for the name of a node, or
	 *   null if none was specified.
	 */
	public abstract Policy getNodeNamePolicy();

	/**
	 * @return the Policy to use for host names (or domain names), or
	 *   null if none was specified.
	 */
	public abstract Policy getHostNamePolicy();

	/**
	 * @return the Policy to use for a file-system path, or
	 *   null if none was specified.
	 */
	public abstract Policy getFsPathPolicy();
    
	/**
	 * @return the Policy to use for inbox messages, or
	 *   null if none was specified.
	 */
	public abstract Policy getUserMessagePolicy();
    
	/**
	 * @return the Policy to use for group names, or
	 *   null if none was specified.
	 */
	public abstract Policy getGroupNamePolicy();
    
	/**
	 * @return the Policy to use for group descriptions, or
	 *   null if none was specified.
	 */
	public abstract Policy getGroupDescriptionPolicy();
    
	/**
	 * @return the Policy to use for role names, or
	 *   null if none was specified.
	 */
	public abstract Policy getRoleNamePolicy();
    
	/**
	 * @return the Policy to use for role descriptions, or
	 *   null if none was specified.
	 */
	public abstract Policy getRoleDescriptionPolicy();

	/**
	 * This method should be used to load the policy-map. This will automatically set
	 * the URI, against which relative policy URIs are resolved, to the one in the given
	 * Source.
	 */
	public static PolicyMapImpl load(URI uri) throws SAXException, JAXBException, MalformedURLException, IOException {
		PolicyMapImpl map = load(new StreamSource(uri.toURL().toString()));

		map.resolveURI = uri;
		return map;
	}

	/**
	 * This method may be used to load the policy-map without setting the URI against
	 * which other URIs, that occur in the document, are resolved. This means that all
	 * URIs, that occur in the document, must be absolute - otherwise an exception
	 * may occur when loading resources referred to by this document.
	 */
	public static PolicyMapImpl load(Source source) throws SAXException, JAXBException, IOException {
		InputStream schemaStream = PolicyMapImpl.class.getResourceAsStream(PolicyMap.POLICY_MAP_SCHEMA_RESOURCE);

		try {
			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = sf.newSchema(new StreamSource(schemaStream));

			JAXBContext ctx = JAXBContext.newInstance(PolicyMapImpl.class);
			Unmarshaller unmarshaller = ctx.createUnmarshaller();

			unmarshaller.setSchema(schema);

			return unmarshalChecked(unmarshaller, source);
		} finally {
			schemaStream.close();
		}
	}
    
	/**
	 * This method will unmarshal a policy map with the given unmarshaller from the given source 
	 * and check the consistency of the resulting policy map beyond what the schema can do.
	 */
	public static PolicyMapImpl unmarshalChecked(Unmarshaller unmarshaller, Source source) throws JAXBException {
		XmlFinishedUnmarshalPropagator propagator = new XmlFinishedUnmarshalPropagator();

		unmarshaller.setListener(propagator);
		JAXBElement<PolicyMapImpl> elem = unmarshaller.unmarshal(source, PolicyMapImpl.class);

		propagator.finishedUnmarshal();
		return elem.getValue();
	}
    
	/**
	 * Loads the policy-map bundled with the application.
	 */
	public static PolicyMapImpl loadDefault() throws JAXBException, SAXException, URISyntaxException, IOException {
		InputStream defaultPolicyMap = PolicyMapImpl.class.getResourceAsStream(PolicyMap.DEFAULT_POLICY_MAP_RESOURCE);

		try {
			PolicyMapImpl map = PolicyMap.load(new StreamSource(defaultPolicyMap));

			map.loadAsResource = true;
			return map;
		} finally {
			defaultPolicyMap.close();
		}
	}
    
	/**
	 * Returns the default AntiSamy Policy file resource as an InputStream.
	 * The input stream should be closed when the caller finished reading from it.
	 */
	public static InputStream getDefaultAntiSamyPolicyAsInputStream() {
		return PolicyMapImpl.class.getResourceAsStream(PolicyMap.DEFAULT_ANTI_SAMY_POLICY_RESOURCE);
	}
}
