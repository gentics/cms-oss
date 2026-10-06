package com.gentics.contentnode.mcp.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FileFactory;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.FileInfo;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.ImageLoadResponse;
import com.gentics.contentnode.rest.resource.FileResource;
import com.gentics.contentnode.rest.resource.ImageResource;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Stores a base64 encoded file in a folder. Delegates to {@link FileResourceImpl#createSimple}, which reads the file
 * from the request body, so it gets a request that only serves the decoded bytes. The permission to create (or, when
 * overwriting, to edit) files is checked before, because the delegate reports a missing one as a plain failure.
 */
public class UploadFileTool extends AbstractMcpTool {
	static final String NAME = "upload_file";

	static final String ARG_FOLDER_ID = "folderId";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_NAME = "name";

	static final String ARG_MEDIA_TYPE = "mediaType";

	static final String ARG_DESCRIPTION = "description";

	static final String ARG_SOURCE = "source";

	static final String ARG_OVERWRITE = "overwrite";

	/**
	 * Maximum size of the decoded file
	 */
	static final int MAX_BYTES = 5 * 1024 * 1024;

	/**
	 * Maximum length of the base64 data
	 */
	static final int MAX_BASE64_LENGTH = 7_000_000;

	/**
	 * File extensions of common media types, for a name without extension. The CMS derives the media type from the
	 * extension (and the content), not from a given media type.
	 */
	static final Map<String, String> EXTENSIONS = Map.of("image/jpeg", "jpg", "image/png", "png", "image/gif", "gif",
			"image/webp", "webp", "image/svg+xml", "svg", "application/pdf", "pdf", "text/plain", "txt", "text/csv",
			"csv", "application/json", "json", "application/zip", "zip");

	/**
	 * The uploaded file
	 * @param info file metadata
	 * @param isImage whether the CMS stored it as an image
	 */
	public record UploadedFile(@JsonUnwrapped FileInfo info, boolean isImage) {
	}

	/**
	 * Result of the tool
	 * @param file the uploaded file
	 * @param created false if an existing file of that name was overwritten
	 */
	public record Result(UploadedFile file, boolean created) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> base64 = new LinkedHashMap<>();
		base64.put("kind", Map.of("const", "base64"));
		base64.put("data", schema("string", "Base64 payload, at most 5 MB decoded.", "contentEncoding", "base64",
				"maxLength", MAX_BASE64_LENGTH));
		Map<String, Object> url = new LinkedHashMap<>();
		url.put("kind", Map.of("const", "url"));
		url.put("url", schema("string", "Not supported yet: calls with a URL source are rejected.", "maxLength", 2000));
		url.put("expiresAt", schema("integer", "Unix timestamp in seconds."));

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_FOLDER_ID, schema("integer", "ID of the folder.", "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer", "Not supported yet: calls that set it are rejected.", "minimum",
				1));
		properties.put(ARG_NAME, schema("string", "Filename, with extension.", "minLength", 1, "maxLength", 255));
		properties.put(ARG_MEDIA_TYPE, schema("string",
				"IANA media type, e.g. image/jpeg. Only used to add an extension to a name without one.", "maxLength",
				150));
		properties.put(ARG_DESCRIPTION, schema("string", "Description.", "maxLength", 4000));
		properties.put(ARG_SOURCE, Map.of("oneOf", List.of(
				schema("object", null, "properties", base64, "required", List.of("kind", "data"),
						"additionalProperties", false),
				schema("object", null, "properties", url, "required", List.of("kind", "url"), "additionalProperties",
						false))));
		properties.put(ARG_OVERWRITE, schema("boolean", "Overwrite a file of the same name in the folder.", "default",
				false));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		return Tool.builder().name(NAME).title("Upload file")
				.description("Puts a binary into a CMS folder so a page can reference it. Pass the content as source "
						+ "{kind: base64, data}, at most 5 MB decoded; URL sources are not supported yet. Use the "
						+ "node's default file or image folder from list_nodes unless the user names a folder. A file "
						+ "of the same name in the folder is refused unless overwrite is true. The CMS detects images "
						+ "by extension and content and may convert them (e.g. to WebP), so read fileName from the "
						+ "result.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_FOLDER_ID, ARG_NAME, ARG_SOURCE)).additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Writes.rejectChannel(NAME, arguments);
		int folderId = Args.id(arguments, ARG_FOLDER_ID);
		String name = stringArg(arguments, ARG_NAME, 1, 255);
		if (name == null) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(ARG_NAME));
		}
		String fileName = fileName(name, stringArg(arguments, ARG_MEDIA_TYPE, 0, 150));
		String description = stringArg(arguments, ARG_DESCRIPTION, 0, 4000);
		boolean overwrite = booleanArg(arguments, ARG_OVERWRITE, false);
		byte[] data = decode(arguments.get(ARG_SOURCE));
		Writes.logIdempotencyKey(NAME, folderId, arguments);

		boolean exists = checkFolder(folderId, fileName, overwrite);

		FileUploadResponse response = RestPermissions.guard(FileResource.class, new FileResourceImpl())
				.createSimple(request(data), folderId, 0, null, fileName, description, overwrite);
		requireOk(response, "The file was not uploaded to folder %d".formatted(folderId));

		com.gentics.contentnode.rest.model.File file = response.getFile();
		boolean isImage = file.getFileType() != null && file.getFileType().startsWith("image/");
		if (isImage) {
			// the upload response has no image dimensions
			ImageLoadResponse image = RestPermissions.guard(ImageResource.class, new ImageResourceImpl())
					.load(Integer.toString(file.getId()), false, false, null, null);
			requireOk(image, "The image %d could not be loaded".formatted(file.getId()));
			file = image.getImage();
		}
		return new Result(new UploadedFile(FileInfo.of(file, null), isImage), !exists);
	}

	/**
	 * Add the extension of the media type to a name without extension
	 * @param name name
	 * @param mediaType media type, may be null
	 * @return filename
	 */
	static String fileName(String name, String mediaType) {
		if (name.lastIndexOf('.') > 0 || mediaType == null) {
			return name;
		}
		String extension = EXTENSIONS.get(mediaType.split(";")[0].trim().toLowerCase(Locale.ROOT));
		return extension != null ? name + "." + extension : name;
	}

	/**
	 * Decode the source
	 * @param source source argument
	 * @return file content
	 * @throws IllegalArgumentException if the source is not valid base64 of at most {@link #MAX_BYTES}
	 */
	static byte[] decode(Object source) {
		if (!(source instanceof Map<?, ?> map)) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(ARG_SOURCE));
		}
		if ("url".equals(map.get("kind"))) {
			throw new IllegalArgumentException("URL sources are not supported yet, pass the file as {kind: base64}");
		}
		if (!"base64".equals(map.get("kind")) || !(map.get("data") instanceof String data)) {
			throw new IllegalArgumentException("'%s' must be {kind: base64, data}".formatted(ARG_SOURCE));
		}
		if (data.length() > MAX_BASE64_LENGTH) {
			throw new IllegalArgumentException("The base64 data is longer than %d characters".formatted(
					MAX_BASE64_LENGTH));
		}
		byte[] bytes;
		try {
			// strict, apart from line breaks: the MIME decoder would silently skip invalid characters
			bytes = Base64.getDecoder().decode(data.replaceAll("\\s", ""));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("'%s.data' is not valid base64: %s".formatted(ARG_SOURCE,
					e.getMessage()));
		}
		if (bytes.length == 0 || bytes.length > MAX_BYTES) {
			throw new IllegalArgumentException("The file has %d bytes, it must have 1 to %d".formatted(bytes.length,
					MAX_BYTES));
		}
		return bytes;
	}

	/**
	 * Build the request for {@link FileResourceImpl#createSimple}, which only reads its body. Any other method throws,
	 * so a CMS change that needs more fails clearly.
	 * @param data request body
	 * @return request
	 */
	static HttpServletRequest request(byte[] data) {
		ByteArrayInputStream bytes = new ByteArrayInputStream(data);
		ServletInputStream input = new ServletInputStream() {
			@Override
			public int read() throws IOException {
				return bytes.read();
			}

			@Override
			public int read(byte[] b, int off, int len) {
				return bytes.read(b, off, len);
			}

			@Override
			public boolean isFinished() {
				return bytes.available() == 0;
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setReadListener(ReadListener readListener) {
				throw new UnsupportedOperationException();
			}
		};
		return (HttpServletRequest) Proxy.newProxyInstance(UploadFileTool.class.getClassLoader(),
				new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> {
					if ("getInputStream".equals(method.getName()) && method.getParameterCount() == 0) {
						return input;
					}
					throw new UnsupportedOperationException(
							"The upload request only provides getInputStream(), not %s()".formatted(method.getName()));
				});
	}

	/**
	 * Check the folder and the permission, and whether a file of the name exists in the folder (as the delegate does,
	 * with the sanitized name)
	 * @param folderId folder ID
	 * @param fileName filename
	 * @param overwrite whether an existing file is overwritten
	 * @return true if a file of the name exists
	 * @throws Exception
	 * @throws EntityNotFoundException if the folder does not exist
	 * @throws InsufficientPrivilegesException if the caller may not create files in the folder, or not edit the file
	 *         to overwrite
	 * @throws IllegalArgumentException if the file exists and is not to be overwritten
	 */
	private static boolean checkFolder(int folderId, String fileName, boolean overwrite) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			Folder folder = t.getObject(Folder.class, folderId);
			if (folder == null || !t.canView(folder)) {
				throw new EntityNotFoundException("Folder %d does not exist or is not visible to you".formatted(
						folderId));
			}
			if (!t.canCreate(folder, File.class, null)) {
				throw new InsufficientPrivilegesException("You may not create files in folder %d".formatted(folderId),
						folder, PermType.createitems);
			}
			String sanitized = FileFactory.sanitizeName(fileName);
			File existing = null;
			for (File file : folder.getFilesAndImages()) {
				if (file.getName().equals(sanitized)) {
					existing = file;
				}
			}
			if (existing != null && !overwrite) {
				// the CMS would store the file under a made-up name instead
				throw new IllegalArgumentException("Folder %d already has a file named '%s'. Pass overwrite=true to "
						.formatted(folderId, sanitized) + "replace it, or choose another name.");
			}
			if (existing != null && !t.canEdit(existing)) {
				throw new InsufficientPrivilegesException("You may not overwrite the file %d".formatted(
						existing.getId()), existing, PermType.updateitems);
			}
			trx.success();
			return existing != null;
		}
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> outputSchema() {
		Map<String, Object> file = FileInfo.jsonSchema("The uploaded file.");
		((Map<String, Object>) file.get("properties")).put("isImage", schema("boolean",
				"Whether the CMS stored it as an image."));
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("file", file);
		properties.put("created", schema("boolean", "False if an existing file of the name was overwritten."));
		return schema("object", null, "properties", properties, "required", List.of("file", "created"));
	}
}
