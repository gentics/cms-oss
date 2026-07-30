package com.gentics.contentnode.object.parttype;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.gentics.api.portalnode.imp.GenticsImpInterface;
import com.gentics.contentnode.formatter.CNDateFormatterImp;
import com.gentics.contentnode.object.parttype.imps.CMSLoaderImp;
import com.gentics.lib.log.NodeLogger;
import com.gentics.portalnode.formatter.GenticsStringFormatter;
import com.gentics.portalnode.formatter.SortImp;
import com.gentics.portalnode.formatter.URLIncludeImp;
import com.gentics.portalnode.formatter.VelocityToolsImp;

public class ImpsResolverService implements CMSResolverService {
	protected static NodeLogger logger = NodeLogger.getNodeLogger(ImpsResolverService.class);

	@Override
	public Map<String, ProvidedResolver> getResolvers() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Imps resolver
	 */
	public static class ImpsResolver implements ProvidedResolver {

		/**
		 * velocity tools imp (when fetched from the pool)
		 */
		protected GenticsImpInterface velocityToolsImp;

		/**
		 * gentics string formatter (when fetched from the pool)
		 */
		protected GenticsImpInterface genticsStringFormatter;

		/**
		 * date formatter (when fetched from the pool)
		 */
		protected GenticsImpInterface genticsDateFormatter;

		/**
		 * sort imp (when fetched from the pool)
		 */
		protected GenticsImpInterface sortImp;
        
		/**
		 * loader imp
		 */
		protected GenticsImpInterface loaderImp;

		/**
		 * URL include imp
		 */
		protected GenticsImpInterface urlIncludeImp;

		/**
		 * constant for the velo imp
		 */
		public final static String VELOIMP = "velocitytools";

		/**
		 * constant for the string imp
		 */
		public final static String STRINGIMP = "string";

		/**
		 * constant for the date imp
		 */
		public final static String DATEIMP = "date";

		/**
		 * constant for the sorter imp
		 */
		public final static String SORTIMP = "sorter";
        
		/**
		 * constant for the loader imp
		 */
		public final static String LOADERIMP = "loader";

		/**
		 * constant for the url imp
		 */
		public final static String URLIMP = "url";

		/**
		 * Create instance of the imps resolver
		 */
		public ImpsResolver() {}

		/**
		 * Get the velocity tools imp
		 * @return velocity tools imp
		 */
		public GenticsImpInterface getVelocitytools() {
			if (velocityToolsImp == null) {
				velocityToolsImp = ImpProvider.getImp(VELOIMP);
			}
			return velocityToolsImp;
		}

		/**
		 * Get the velocity tools imp - an alias which is the same as in PN 3.3
		 * @return velocity tools imp
		 */
		public GenticsImpInterface getVelocityTools() {
			if (velocityToolsImp == null) {
				velocityToolsImp = ImpProvider.getImp(VELOIMP);
			}
			return velocityToolsImp;
		}

		/**
		 * Get the gentics string formatter imp
		 * @return gentics string formatter imp
		 */
		public GenticsImpInterface getString() {
			if (genticsStringFormatter == null) {
				genticsStringFormatter = ImpProvider.getImp(STRINGIMP);
			}
			return genticsStringFormatter;
		}

		/**
		 * Get the sorter imp
		 * @return sorter imp
		 */
		public GenticsImpInterface getSorter() {
			if (sortImp == null) {
				sortImp = ImpProvider.getImp(SORTIMP);
			}
			return sortImp;
		}

		/**
		 * Get the date formatter imp
		 * @return date formatter imp
		 */
		public GenticsImpInterface getDate() {
			if (genticsDateFormatter == null) {
				genticsDateFormatter = ImpProvider.getImp(DATEIMP);
			}
			return genticsDateFormatter;
		}
        
		public GenticsImpInterface getLoader() {
			if (loaderImp == null) {
				loaderImp = ImpProvider.getImp(LOADERIMP);
			}
			return loaderImp;
		}

		public GenticsImpInterface getUrl() {
			if (urlIncludeImp == null) {
				urlIncludeImp = ImpProvider.getImp(URLIMP);
			}
			return urlIncludeImp;
		}

		@Override
		public void clean() {
			if (genticsDateFormatter != null) {
				ImpProvider.returnImp(genticsDateFormatter, ImpsResolver.DATEIMP);
				genticsDateFormatter = null;
			}
			if (genticsStringFormatter != null) {
				ImpProvider.returnImp(genticsStringFormatter, ImpsResolver.STRINGIMP);
				genticsStringFormatter = null;
			}
			if (sortImp != null) {
				ImpProvider.returnImp(sortImp, ImpsResolver.SORTIMP);
				sortImp = null;
			}
			if (velocityToolsImp != null) {
				ImpProvider.returnImp(velocityToolsImp, ImpsResolver.VELOIMP);
				velocityToolsImp = null;
			}
			if (loaderImp != null) {
				ImpProvider.returnImp(loaderImp, ImpsResolver.LOADERIMP);
				loaderImp = null;
			}
			if (urlIncludeImp != null) {
				ImpProvider.returnImp(urlIncludeImp, ImpsResolver.URLIMP);
				urlIncludeImp = null;
			}

		}
	}

	/**
	 * Class for the singleton imp provider, that provides imps which are held
	 * in pools
	 */
	protected final static class ImpProvider {

		/**
		 * internal imp pool
		 */
		protected KeyedObjectPool<String, GenticsImpInterface> impPool = null;

		/**
		 * the singleton instance
		 */
		protected static ImpProvider instance = null;

		/**
		 * static method to get an imp from the pool
		 * @param impId id of the imp
		 * @return imp or null
		 */
		public static GenticsImpInterface getImp(String impId) {
			try {
				ImpProvider impProvider = getInstance();
				Object borrowedObject = impProvider.impPool.borrowObject(impId);

				if (logger.isInfoEnabled()) {
					logger.info(
							"borrowed imp {" + impId + "}. Active: " + impProvider.impPool.getNumActive(impId) + ", Idle: " + impProvider.impPool.getNumIdle(impId));
				}
				return (GenticsImpInterface) borrowedObject;
			} catch (Exception e) {
				logger.error("Error while fetching imp {" + impId + "}", e);
				return null;
			}
		}

		/**
		 * static method to return an imp
		 * @param imp imp to return
		 * @param impId id of the imp
		 */
		public static void returnImp(GenticsImpInterface imp, String impId) {
			try {
				ImpProvider impProvider = getInstance();

				impProvider.impPool.returnObject(impId, imp);
				if (logger.isInfoEnabled()) {
					logger.info(
							"returned imp {" + impId + "}. Active: " + impProvider.impPool.getNumActive(impId) + ", Idle: " + impProvider.impPool.getNumIdle(impId));
				}
			} catch (Exception e) {
				logger.error("Error while returning imp {" + impId + "} to pool.", e);
			}
		}

		/**
		 * Get the singleton instance of the imp provider
		 * @return imp provider
		 */
		protected static ImpProvider getInstance() {
			if (instance == null) {
				instance = new ImpProvider();
			}
			return instance;
		}

		/**
		 * private constructor for the singleton
		 */
		private ImpProvider() {
			// create the pool
			impPool = new GenericKeyedObjectPool<>(new ImpFactory(), 20, GenericObjectPool.WHEN_EXHAUSTED_GROW, -1, 5, false, false);
		}

		/**
		 * Internal imp factory
		 */
		protected class ImpFactory implements KeyedPoolableObjectFactory<String, GenticsImpInterface> {
			@Override
			public void activateObject(String key, GenticsImpInterface imp) throws Exception {}

			@Override
			public void destroyObject(String key, GenticsImpInterface imp) throws Exception {}

			@Override
			public GenticsImpInterface makeObject(String key) throws Exception {
				try {
					if (ImpsResolver.VELOIMP.equals(key)) {
						VelocityToolsImp velocityToolsImp = new VelocityToolsImp();

						velocityToolsImp.init("velocitytools", new HashMap<>());

						return velocityToolsImp;
					} else if (ImpsResolver.STRINGIMP.equals(key)) {
						GenticsStringFormatter genticsStringFormatter = new GenticsStringFormatter();

						genticsStringFormatter.init("genticsstringformatter", new HashMap<>());

						return genticsStringFormatter;
					} else if (ImpsResolver.SORTIMP.equals(key)) {
						SortImp sortImp = new SortImp();

						sortImp.init("sorter", new HashMap<>());

						return sortImp;
					} else if (ImpsResolver.LOADERIMP.equals(key)) {
						CMSLoaderImp loaderImp = new CMSLoaderImp();

						loaderImp.init("loader", new HashMap<>());

						return loaderImp;
					} else if (ImpsResolver.URLIMP.equals(key)) {
						URLIncludeImp urlIncludeImp = new URLIncludeImp();

						urlIncludeImp.init("url", new HashMap<>());

						return urlIncludeImp;
					} else if (ImpsResolver.DATEIMP.equals(key)) {
						CNDateFormatterImp dateformatterImp = new CNDateFormatterImp();
						return dateformatterImp;
					}
				} catch (Exception e) {
					logger.error("Imp {" + key + "} could not be initialized.", e);
				}
				return null;
			}

			@Override
			public void passivateObject(String key, GenticsImpInterface imp) throws Exception {}

			@Override
			public boolean validateObject(String key, GenticsImpInterface imp) {
				return true;
			}
		}
	}
}
