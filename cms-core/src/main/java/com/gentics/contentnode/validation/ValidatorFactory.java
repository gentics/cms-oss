/*
 * @author tobiassteiner
 * @date Dec 6, 2010
 * @version $Id: ValidatorFactory.java,v 1.1.2.1 2011-02-10 13:43:41 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.owasp.validator.html.PolicyException;
import org.xml.sax.SAXException;

import com.gentics.api.lib.i18n.Language;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyMap;
import com.gentics.contentnode.validation.map.inputchannels.GenericConfigurableInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.InputChannel;
import com.gentics.contentnode.validation.util.StringViewMap;
import com.gentics.contentnode.validation.validator.ValidationMessage;
import com.gentics.contentnode.validation.validator.Validator;
import com.gentics.contentnode.validation.validator.ValidatorInstantiationException;
import com.gentics.contentnode.validation.validator.impl.AbstractAntiSamyValidator;
import com.gentics.contentnode.validation.validator.impl.AntiSamyPolicy;
import com.gentics.contentnode.validation.validator.impl.AntiSamyValidator;
import com.gentics.contentnode.validation.validator.impl.AttributePolicy;
import com.gentics.contentnode.validation.validator.impl.AttributeValidator;
import com.gentics.contentnode.validation.validator.impl.PassThroughPolicy;
import com.gentics.contentnode.validation.validator.impl.PassThroughValidator;
import com.gentics.lib.i18n.LanguageProvider;
import com.gentics.lib.log.NodeLogger;

public class ValidatorFactory {

	protected final static NodeLogger logger = NodeLogger.getNodeLogger(ValidatorFactory.class);

	/**
	 * Prefix for validation properties.
	 */
	protected final static String PROP_PREFIX = "validation";

	/**
	 * Property name (relative to PROP_PREFIX) for the policy map location. The
	 * property value must be an absolute URI.
	 */
	protected final static String POLICY_MAP_PROP = "policyMap";

	/**
	 * Semantics of java guarantee lazy and thread safe loading of the singleton. 
	 * Lazy loading is useful for unit testing, so the default config need not be loaded.
	 */
	private static class LazyLoader {
        
		protected final static ValidatorFactory singleton;
        
		static {
			try {
				singleton = new ValidatorFactory();
				// Loading an AntiSamy policy file is an extreme effort, which should
				// be done as early as possible (ideally at startup), and not when the
				// user first visits a page. Also, this way, validator instantiation
				// errors are visible as early as possible.
				singleton.preloadValidators();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * AntiSamy Policy instances mapped to the location they were loaded from.
	 * Must be final and a ConcurrentHashMap or synchronized to guarantee thread-safety.
	 */
	protected final static Map<String, org.owasp.validator.html.Policy> antiSamyPolicies = new ConcurrentHashMap<String, org.owasp.validator.html.Policy>();

	/**
	 * Must be final and immutable (including contained objects)
	 * to guarantee thread-safety.
	 */
	protected final PolicyMap policyMap; 

	/**
	 * The locale for AntiSamy error messages.
	 */
	protected final Locale locale;

	/**
	 * The default AntiSamy policy if not configured.
	 */
	protected final org.owasp.validator.html.Policy defaultAntiSamyPolicy;
    
	/**
	 * We try to avoid loading the configuration multiple times, so we use a singleton
	 * to store the default configuration and refer to it as required.
	 * This is not a problem, since all methods in this class are thread safe and no state 
	 * is kept except for the AntiSamy policy cache, which is transparent to users of
	 * this class - users will never see the difference between new instances and a
	 * single instance.
	 * 
	 * Validators constructed by this Factory should not be considered
	 * thread safe unless stated otherwise. The construction of the validators
	 * itself should be thread safe to the best of my knowledge.
	 * 
	 * This method is thread-safe.
	 */
	public static ValidatorFactory newInstance(Locale locale) {
		ValidatorFactory singleton = LazyLoader.singleton;

		return new ValidatorFactory(singleton.policyMap, locale, singleton.defaultAntiSamyPolicy);
	}
    
	/**
	 * Will create a new factory with the locale of the current transaction, or
	 * the system default locale if no current transaction is available.
	 * @see #newInstance(Locale)
	 */
	public static ValidatorFactory newInstance() {
		return newInstance(getCurrentLocale());
	}

	/**
	 * Gets the {@link PolicyMap} used by this ValidatorFactory.
	 */
	public PolicyMap getPolicyMap() {
		return policyMap;
	}
    
	/**
	 * Returns a new Validator that can validate input according to the policy
	 * configured for the given inputChannel. Both, the policy map provided by
	 * the user and the default policy-map provided by the developer are
	 * checked for an appropriate policy to use - the developer policy-map
	 * only if the user policy-map has no policy configured for the given
	 * inputChannel.
	 * This method is thread-safe to the best of my knowledge.
	 */
	public Validator newValidatorForInputChannel(InputChannel inputChannel) throws ValidatorInstantiationException {
		Policy p = inputChannel.getPolicy(policyMap);

		if (null == p) {
			// this shouldn't happen since the default policy
			// map, at least, should have a global default policy.
			throw new IllegalStateException(
					"The default policy map should have a global default policy and the" + " input channel must return it if no more-appropriate policy exists: "
					+ inputChannel);
		}
		return p.newValidator(this);
	}
    
	/**
	 * Like {@link #newValidatorForInputChannel(InputChannel)} but creates the
	 * validator for the given policy URI.
	 */
	public Validator newValidatorForPolicyURI(final URI policyURI) throws ValidatorInstantiationException {
		return newValidatorForInputChannel(new GenericConfigurableInputChannel(policyURI));
	}

	/**
	 * This method is thread-safe.
	 */
	public AbstractAntiSamyValidator newAntiSamyValidator(AntiSamyPolicy policy) throws ValidatorInstantiationException {
		try {
			return new AntiSamyValidator(policy, getPolicyFile(policy), locale);
		} catch (Exception e) {
			throw new ValidatorInstantiationException(e);
		}
	}
    
	/**
	 * This method is thread-safe.
	 */
	public AttributeValidator newAttributeValidator(AttributePolicy policy) throws ValidatorInstantiationException {
		try {
			return new AttributeValidator(policy, getPolicyFile(policy), locale);
		} catch (Exception e) {
			throw new ValidatorInstantiationException(e);
		}
	}

	/**
	 * This method is thread-safe.
	 */
	public PassThroughValidator newPassThroughValidator(PassThroughPolicy policy) throws ValidatorInstantiationException {
		return new PassThroughValidator(policy);
	}

	/**
	 * @param policyMap the policy map that will be used to find
	 *   the correct {@link Validator} for an {@link InputChannel}.
	 *   {@see #newValidatorForInputChannel(InputChannel)}
	 * @param locale will be used for localized {@link ValidationMessage}s.
	 * @param defaultAntiSamyPolicy a default policy instance that will
	 *   be used if an {@link AntiSamyPolicy} doesn't have a policyFile
	 *   configured.
	 */
	public ValidatorFactory(
			PolicyMap policyMap,
			Locale locale,
			org.owasp.validator.html.Policy defaultAntiSamyPolicy) {
		this.policyMap = policyMap;
		this.locale = locale;
		this.defaultAntiSamyPolicy = defaultAntiSamyPolicy;
	}

	/**
	 * Constructs a new instance with default settings.
	 * @see #ValidatorFactory(PolicyMap, Properties, Locale, org.owasp.validator.html.Policy)
	 */
	public ValidatorFactory() throws URISyntaxException, MalformedURLException, JAXBException,
				SAXException, PolicyException, IOException {
		this(loadPolicyMap(getDefaultProperties()), getCurrentLocale(), loadDefaultAntiSamyPolicy());
	}

	/**
	 * @return a new instance of the AntiSamy policy bundled with the
	 *   application.
	 */
	protected static org.owasp.validator.html.Policy loadDefaultAntiSamyPolicy() throws PolicyException, IOException {
		InputStream stream = PolicyMap.getDefaultAntiSamyPolicyAsInputStream();

		try {
			return org.owasp.validator.html.Policy.getInstance(stream);
		} finally {
			stream.close();
		}
	}

	/**
	 * @return the locale for the current transaction, or the System default
	 *   locale if no current transaction exists.
	 */
	protected static Locale getCurrentLocale() {
		LanguageProvider langpro = TransactionManager.getCurrentTransactionOrNull();

		if (null != langpro) {
			Language lang = langpro.getLanguage();

			if (lang != null) {
				return lang.getLocale();
			}
		}
		return Locale.getDefault();
	}

	/**
	 * Loads a configured policy or default policy map.
	 * 
	 * @param props
	 * 		User configurable validation properties.
	 *  	If the map contains a {@link #POLICY_MAP_PROP} property it will be loaded and returned.
	 * @return
	 * 		Either a user configured PolicyMap instance or a default policy map instance,
	 *      depending on the given property map.
	 */
	protected static PolicyMap loadPolicyMap(Map<String, String> props) throws JAXBException, SAXException, URISyntaxException, MalformedURLException, IOException {
		PolicyMap userMap = loadUserPolicyMap(props);

		if (null != userMap) {
			return userMap;
		}
		return PolicyMap.loadDefault();
	}

	protected static PolicyMap loadUserPolicyMap(Map<String, String> props) throws JAXBException, SAXException, MalformedURLException, URISyntaxException, IOException {
		String policyMap = props.get(POLICY_MAP_PROP);

		if (StringUtils.isEmpty(policyMap)) {
			return null;
		}
		logger.warn("User policy map configured: " + policyMap);
		return PolicyMap.load(new URI(policyMap));
	}
    
	/**
	 * @return the validation properties as configured under
	 *   {@link #PROP_PREFIX} in the node.conf.
	 */
	protected static Map<String, String> getDefaultProperties() {
		NodeConfigRuntimeConfiguration runtimeConfiguration = NodeConfigRuntimeConfiguration.getDefault();
		NodePreferences prefs = runtimeConfiguration.getNodeConfig().getDefaultPreferences();
        
		Map<?, ?> props = prefs.getPropertyMap(PROP_PREFIX);

		if (null == props) {
			logger.warn(PROP_PREFIX + " is unconfigured");
			return new HashMap<String, String>();
		}
        
		// we use a string view so that we don't need to cast,
		// I think the values should all be strings.
		return new StringViewMap(props);
	}

	/**
	 * Loads and caches an AntiSamy {@link org.owasp.validator.html.Policy}.
	 * @see #loadPolicyFile(AntiSamyPolicy)
	 */
	protected org.owasp.validator.html.Policy getPolicyFile(AntiSamyPolicy policy) throws ValidatorInstantiationException {
		String location = policy.getPolicyFile();

		if (null == location) {
			return defaultAntiSamyPolicy;
		}
		org.owasp.validator.html.Policy policyFile = antiSamyPolicies.get(location);

		// double checked locking idiom. depends on antiSamyPolicies being a
		// Map that correctly handles reads/writes from multiple threads.
		if (null == policyFile) {
			// this synchronized block is optional - to avoid the same policy
			// file being loaded more than once if two threads load it at the
			// same time. can be removed without any adverse side-effects.
			synchronized (this) {
				policyFile = antiSamyPolicies.get(location);
				if (null == policyFile) {
					policyFile = loadPolicyFile(policy);
					antiSamyPolicies.put(location, policyFile);
				}
			}
		}
		return policyFile;
	}

	/**
	 * Loads an AntiSamy {@link org.owasp.validator.html.Policy}. 
	 */
	protected org.owasp.validator.html.Policy loadPolicyFile(AntiSamyPolicy policy) throws ValidatorInstantiationException { 
		try {
			InputStream stream = policy.getPolicyFileAsInputStream();

			try {
				return org.owasp.validator.html.Policy.getInstance(stream);
			} finally {
				stream.close();
			}
		} catch (Exception e) {
			throw new ValidatorInstantiationException(e);
		}
	}
    
	/**
	 * Preloads all AntiSamy policies in the policy map by
	 * creating a Validator instance for each configured policy.
	 * This is useful to catch configuration errors at startup-time.
	 */
	protected void preloadValidators() throws ValidatorInstantiationException {
		for (Policy policy : policyMap.getPolicies()) {
			policy.newValidator(this);
		}
	}
    
	/**
	 * Preload the default configuration for new {@link ValidatorFactory}
	 * instances.
	 * @see #newInstance()
	 * @see #newInstance(Locale)
	 */
	public static void preload() {
		// will load a new instance with the default configuration which is
		// then used henceforth as a "blueprint" for other instances
		newInstance();
	}
}
