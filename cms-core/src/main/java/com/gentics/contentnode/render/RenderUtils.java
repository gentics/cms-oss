package com.gentics.contentnode.render;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.velocity.context.InternalContextAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.FilePublisher;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.json.JsonUtil;

/**
 * Utility class for rendering
 */
public class RenderUtils {
	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(RenderUtils.class);

	protected static ObjectMapper mapper = new ObjectMapper();

	/**
	 * Get the preview template for the given page, if the page preview must be rendered on a mesh portal, or null otherwise
	 * @param page page to preview
	 * @param renderMode render mode
	 * @return preview template or null
	 * @throws NodeException
	 */
	public static String getPreviewTemplate(Page page, int renderMode) throws NodeException {
		return getPreviewTemplate(page, renderMode, null);
	}

	/**
	 * Get the preview template for the given page, if the page preview must be rendered on a mesh portal, or null otherwise
	 * @param page page to preview
	 * @param renderMode render mode
	 * @param tag optional tagname to render
	 * @return preview template or null
	 * @throws NodeException
	 */
	public static String getPreviewTemplate(Page page, int renderMode, String tag) throws NodeException {
		String template = null;
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.MESH_CONTENTREPOSITORY)) {
			// check whether the node publishes into a Mesh CR
			Node node = page.getFolder().getNode();
			Node masterNode = node.getMaster();
			if (masterNode.doPublishContentmap()) {
				ContentRepository cr = masterNode.getContentRepository();
				String meshPreviewUrl = masterNode.getMeshPreviewUrl();

				if (cr != null && cr.getCrType() == Type.mesh && !StringUtils.isEmpty(meshPreviewUrl)) {
					String previewUrl = FilePublisher.getPath(false, true, meshPreviewUrl, node.getPublishDir(), ObjectTransformer.getString(page.getFolder().get("path"), null));

					try (
						CloseableHttpClient httpClient = getHttpClient(node.isInsecurePreviewUrl());
						RenderTypeTrx rTrx = new RenderTypeTrx(renderMode, page, false, false); MeshPublisher mp = new MeshPublisher(cr, false)
					) {
						// rendered tagmap entries should not contain aloha settings or script includes and editables shall not (yet) be replaced
						rTrx.get().setParameter(AlohaRenderer.RENDER_SETTINGS, false);
						rTrx.get().setParameter(AlohaRenderer.REPLACE_EDITABLES, false);
						if (tag != null) {
							rTrx.get().setParameter(AlohaRenderer.MARK_TAG, tag);
						}
						NodeResponse meshNode = new NodeResponse();
						String language = MeshPublisher.getMeshLanguage(page);
						meshNode.setUuid(MeshPublisher.getMeshUuid(page));
						meshNode.setLanguage(language);
						meshNode.setParentNode(new NodeReference().setUuid(MeshPublisher.getMeshUuid(page.getFolder())).setSchema(new SchemaReferenceImpl().setName(mp.getSchemaName(Folder.TYPE_FOLDER))));
						meshNode.setSchema(new SchemaReferenceImpl().setName(mp.getSchemaName(Page.TYPE_PAGE)));
						meshNode.setFields(new FieldMapImpl());
						mp.handleRenderedEntries(true, masterNode.getId(), page.getId(), page.getTType(), () -> meshNode.getFields(), mp.render(mp.getEntries(Page.TYPE_PAGE), null, language, false), null, null, null, null, null);

						HttpPost postMethod = new HttpPost(getPreviewUrl(previewUrl, getMode(renderMode), node));

						postMethod.setEntity(new StringEntity(JsonUtil.toJson(meshNode), ContentType.create("application/json", StandardCharsets.UTF_8)));

						if (logger.isInfoEnabled()) {
							logger.info(String.format("Preview Request to URL %s", postMethod.getURI().toString()));
							logger.info("Request Headers:");
							Stream.of(postMethod.getAllHeaders())
								.forEach(header -> logger.info(String.format("%s: %s", header.getName(), header.getValue())));
							if (logger.isDebugEnabled()) {
								logger.debug("Request Body:");
								logger.debug(JsonUtil.toJson(meshNode));
							}
						}

						HttpResponse response = httpClient.execute(postMethod);
						int responseCode = response.getStatusLine().getStatusCode();

						if (responseCode == 200) {
							template = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

							if (logger.isInfoEnabled()) {
								logger.info(String.format("Response code %d", responseCode));
								logger.info("Response Headers:");
								Stream.of(response.getAllHeaders())
									.forEach(header -> logger.info(String.format("%s: %s", header.getName(), header.getValue())));
								if (logger.isDebugEnabled()) {
									logger.debug("Response body:");
									logger.debug(template);
								}
							}

							if (renderMode == RenderType.EM_ALOHA || renderMode == RenderType.EM_ALOHA_READONLY) {
								template = AlohaRenderer.replaceEditables(template, TransactionManager.getCurrentTransaction().getRenderResult());
							}
						} else {
							logger.error(
								String.format("Request to render preview with portal by posting to %s returned response code %d",
									previewUrl,
									responseCode));
						}
					} catch (Exception e) {
						logger.error(String.format("Error while rendering preview with portal by posting to %s", previewUrl), e);
					}
				}
			}
		}
		return template;
	}

	/**
	 * Get parameter passed to a VTL Directive as object
	 * @param <T> object type
	 * @param clazz expected object class
	 * @param context vtl context
	 * @param node vtl node
	 * @param index index of the parameter (starting with 0)
	 * @return object instance or null, if not found
	 */
	public static <T> T getVtlDirectiveObject(Class<T> clazz, InternalContextAdapter context,
			org.apache.velocity.runtime.parser.node.Node node, int index) {
		return getVtlDirectiveObject(clazz, context, node, index, null);
	}

	/**
	 * Get parameter passed to a VTL Directive as object
	 * @param <T> object type
	 * @param clazz expected object class
	 * @param context vtl context
	 * @param node vtl node
	 * @param index index of the parameter (starting with 0)
	 * @param defaultSupplier supplier for the default value (null for no default value)
	 * @return object instance or instance supplied by defaultSupplier or null
	 */
	public static <T> T getVtlDirectiveObject(Class<T> clazz, InternalContextAdapter context,
			org.apache.velocity.runtime.parser.node.Node node, int index, Supplier<T> defaultSupplier) {
		org.apache.velocity.runtime.parser.node.Node child = null;
		if (node.jjtGetNumChildren() > index) {
			child = node.jjtGetChild(index);
		}
		if (child != null) {
			return mapper.convertValue(child.value(context), clazz);
		} else if (defaultSupplier != null) {
			return defaultSupplier.get();
		} else {
			return null;
		}
	}

	/**
	 * Get an HTTP client that will trust self signed SSL certificates and does
	 * not hostname verification.
	 *
	 * @return An HTTP client which ignores insecure SSL connections
	 * @throws NodeException The The custom SSL context can not be created
	 */
	private static CloseableHttpClient getHttpClient(boolean ignoreSslProblems) throws NodeException {
		HttpClientBuilder clientBuilder = HttpClients.custom();

		NodePreferences prefs = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
		int connectTimeout = ObjectTransformer.getInt(prefs.getProperty("mesh.client.connectTimeout"), 60) * 1000;
		int callTimeout = ObjectTransformer.getInt(prefs.getProperty("mesh.client.callTimeout"), 60) * 1000;

		RequestConfig config = RequestConfig.custom()
				  .setConnectTimeout(connectTimeout)
				  .setConnectionRequestTimeout(connectTimeout)
				  .setSocketTimeout(callTimeout).build();
		clientBuilder.setDefaultRequestConfig(config);

		if (ignoreSslProblems) {
			SSLContext sslContext;

			try {
				sslContext = SSLContextBuilder.create()
					.loadTrustMaterial(new TrustSelfSignedStrategy())
					.build();
			} catch (GeneralSecurityException e) {
				throw new NodeException("Could not create non-verifying SSL context: " + e.getMessage(), e);
			}

			clientBuilder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier()));
		}

		return clientBuilder.build();
	}

	/**
	 * Construct the preview URL including the query parameters.
	 *
	 * @param previewUrl The base preview URL
	 * @param renderMode The render mode
	 * @param node The node of the previewed page
	 * @return The URI created from {@code previewUrl} with the necessary query parameters
	 * @throws NodeException When the resulting URI would be invalid
	 */
	private static URI getPreviewUrl(String previewUrl, String renderMode, Node node) throws NodeException {
		try {
			URIBuilder builder = new URIBuilder(previewUrl);

			builder.addParameter("renderMode", renderMode);

			if (node.isChannel()) {
				builder.addParameter("channelUuid", MeshPublisher.getMeshUuid(node));
			}

			return builder.build();
		} catch (URISyntaxException e) {
			throw new NodeException(String.format("Invalid preview URL \"%s\": %s", previewUrl, e.getMessage()), e);
		}
	}

	/**
	 * Get the text representation of the given renderMode
	 * @param renderMode renderMode
	 * @return textual representation
	 */
	protected static String getMode(int renderMode) {
		switch (renderMode) {
		case RenderType.EM_PUBLISH:
			return "publish";
		case RenderType.EM_LIVEPREVIEW:
			return "live";
		case RenderType.EM_EDIT:
		case RenderType.EM_ALOHA:
			return "edit";
		case RenderType.EM_PREVIEW:
		case RenderType.EM_ALOHA_READONLY:
			return "preview";
		default:
			return "";
		}
	}
}
