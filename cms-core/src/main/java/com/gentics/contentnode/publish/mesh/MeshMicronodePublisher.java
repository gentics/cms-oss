package com.gentics.contentnode.publish.mesh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TagContainer;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.ChangeableListPartType;
import com.gentics.contentnode.object.parttype.CheckboxPartType;
import com.gentics.contentnode.object.parttype.DatasourcePartType;
import com.gentics.contentnode.object.parttype.ListPartType;
import com.gentics.contentnode.object.parttype.NodePartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PageTagPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.SelectPartType;
import com.gentics.contentnode.object.parttype.TemplateTagPartType;
import com.gentics.contentnode.object.parttype.UrlPartType;
import com.gentics.contentnode.publish.mesh.MeshPublisher.MeshProject;
import com.gentics.contentnode.rest.util.AbstractNodeObjectFilter;
import com.gentics.contentnode.rest.util.NodeObjectFilter;
import com.gentics.contentnode.rest.util.OrFilter;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.info.BranchInfoMicroschemaList;
import com.gentics.mesh.core.rest.branch.info.BranchMicroschemaInfo;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.client.SchemaUpdateParametersImpl;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Predicate;

/**
 * Mesh Publisher for microschemas
 */
public class MeshMicronodePublisher {
	/**
	 * Filter that accepts all Constructs/Tags
	 */
	protected final static NodeObjectFilter ACCEPT_ALL = new AbstractNodeObjectFilter() {
		@Override
		public boolean matches(NodeObject object) throws NodeException {
			return object instanceof Construct || object instanceof Tag;
		}
	};

	/**
	 * Filter for parts that are transformed to microschema fields
	 */
	protected final static Predicate<? super Part> PART_FILTER = part -> {
		// only accept parts that are editable, contain a value, have a non-empty keyword and are not of the (deprecated) type "Table ext"
		return part.isEditable() && !part.isValueless() && !StringUtils.isEmpty(part.getKeyname()) && part.getPartTypeId() != 23;
	};

	/**
	 * Mesh publisher instance
	 */
	protected MeshPublisher publisher;

	/**
	 * Map of microschemas. Keys are the construct uuid's
	 */
	protected Map<String, MicroschemaResponse> microschemaMap;

	/**
	 * Map of micronode filters per map name
	 */
	protected Map<String, NodeObjectFilter> filters = new HashMap<>();

	/**
	 * Combined filter
	 */
	protected OrFilter combinedFilter = new OrFilter();

	/**
	 * Current transaction
	 */
	protected Transaction t;

	/**
	 * Parse the given micronode filter string
	 * @param filter filter string
	 * @return filter
	 */
	public final static NodeObjectFilter createFilter(String filter) {
		if (StringUtils.isEmpty(filter)) {
			return ACCEPT_ALL;
		}
		String[] keywords = parseFilter(filter);

		if (keywords.length == 0) {
			return ACCEPT_ALL;
		}

		if (keywords[0].startsWith("-")) {
			return new MeshMicronodeBlacklistFilter(keywords);
		} else {
			return new MeshMicronodeWhitelistFilter(keywords);
		}
	}

	/**
	 * Parse the filter into an array of keywords
	 * @param filter filter
	 * @return array of keywords
	 */
	public final static String[] parseFilter(String filter) {
		if (StringUtils.isEmpty(filter)) {
			return new String[0];
		} else {
			return filter.replaceAll("\\,", " ").replaceAll("\\|", " ").trim().split("[\\s]+");
		}
	}

	/**
	 * Create instance
	 * @param publisher Mesh publisher
	 */
	public MeshMicronodePublisher(MeshPublisher publisher) {
		this.publisher = publisher;
	}

	/**
	 * Initialize microschemas for preview. This will just generate the microschemas transformed from constructs
	 * @throws NodeException
	 */
	protected void initForPreview() throws NodeException {
		t = TransactionManager.getCurrentTransaction();

		// prepare micronode filters
		for (TagmapEntry entry : publisher.cr.getAllEntries()) {
			if (entry.getAttributetype() != AttributeType.micronode) {
				continue;
			}
			NodeObjectFilter filter = createFilter(entry.getMicronodeFilter());
			combinedFilter.addFilter(filter);
			filters.put(entry.getMapname(), filter);
		}

		microschemaMap = Observable.fromIterable(publisher.cr.getAllEntries()).filter(entry -> {
			return entry.getAttributetype() == AttributeType.micronode;
		}).firstElement().flatMapSingleElement(entry -> {
			return constructs().flatMapSingle(construct -> getMicroschema(construct, new MicroschemaResponse())).toMap(MicroschemaModel::getDescription);
		}).blockingGet(Collections.emptyMap());
	}

	/**
	 * Initialize microschemas. This will load existing microschemas from Mesh and compare with microschemas transformed from constructs
	 * @param check true to check validity of microschemas
	 * @param repair true to repair invalid (and create missing), false to only check
	 * @param success success flag, is set to false, if microschemas are invalid or missing and repair is false
	 * @throws NodeException
	 */
	protected void init(boolean check, boolean repair, AtomicBoolean success) throws NodeException {
		t = TransactionManager.getCurrentTransaction();

		// prepare micronode filters
		for (TagmapEntry entry : publisher.cr.getAllEntries()) {
			if (entry.getAttributetype() != AttributeType.micronode) {
				continue;
			}
			NodeObjectFilter filter = createFilter(entry.getMicronodeFilter());
			combinedFilter.addFilter(filter);
			filters.put(entry.getMapname(), filter);
		}

		microschemaMap = Observable.fromIterable(publisher.cr.getAllEntries()).filter(entry -> {
			return entry.getAttributetype() == AttributeType.micronode;
		}).firstElement().flatMapSingleElement(entry -> checkMicroschemas(check, repair, success)).blockingGet(Collections.emptyMap());
	}

	/**
	 * Check microschemas (existence and structure)
	 * @param check true to check validity of microschemas
	 * @param repair true to repair invalid (and create missing), false to only check
	 * @param success success flag, is set to false, if microschemas are invalid or missing and repair is false
	 * @return single map of microschemas (keys are the construct uuid's)
	 * @throws NodeException
	 */
	protected Single<Map<String, MicroschemaResponse>> checkMicroschemas(boolean check, boolean repair, AtomicBoolean success) throws NodeException {
		return processConstructs(constructs(), microschemas(), check, repair, success)
			.doOnSubscribe(sub -> publisher.info("Check microschemas"))
			.doOnComplete(() -> publisher.info("Done checking microschemas"))
			.toMap(MicroschemaResponse::getDescription);
	}

	/**
	 * Check and optionally repair microschema assignment to project
	 * @param projectName project name
	 * @param repair true to repair missing assignment
	 * @param success success flag
	 * @return completable
	 * @throws NodeException
	 */
	protected Completable checkMicroschemaAssignment(String projectName, boolean repair, AtomicBoolean success) throws NodeException {
		return processAssignment(projectName, Observable.fromIterable(microschemaMap.values()), microschemas(projectName), repair, success);
	}

	/**
	 * Check and optionally repair microschema version assignment to branch
	 * @param projectName project name
	 * @param branchUuid branch uuid
	 * @param branchName branch name
	 * @param repair true to repair incorrect assignment
	 * @param success success flag
	 * @return completable
	 * @throws NodeException
	 */
	protected Completable checkMicroschemaBranchVersions(String projectName, String branchUuid, String branchName, boolean repair, AtomicBoolean success)
			throws NodeException {
		return processVersions(projectName, branchUuid, branchName, Observable.fromIterable(microschemaMap.values()), microschemas(projectName, branchUuid),
				repair, success);
	}

	/**
	 * Get constructs for which microschemas need to be created.
	 * This includes all constructs, that are assigned to at least one node, that is published into the CR, and does not have publishing into CR disabled.
	 * Constructs, that contain no single editable part are also filtered out.
	 * 
	 * @return observable that will emit the constructs
	 * @throws NodeException
	 */
	protected Observable<Construct> constructs() throws NodeException {
		return Observable.fromIterable(publisher.cr.getNodes()).filter(node -> {
			return !node.isPublishDisabled() && node.doPublishContentmap() && !node.isChannel();
		}).flatMap(node -> {
			return Observable.fromIterable(node.getConstructs());
		}).distinct().filter(construct -> combinedFilter.matches(construct)).filter(construct -> {
			return Observable.fromIterable(construct.getParts()).filter(partFilter()).firstElement().blockingGet() != null;
		}).doOnSubscribe(sub -> publisher.debug("Fetching constructs"));
	}

	/**
	 * Get map of existing microschemas (keys are the descriptions, which are supposed to be the uuids of the constructs)
	 * @return observable emitting the map
	 */
	protected Observable<Map<String, MicroschemaResponse>> microschemas() {
		return publisher.client.findMicroschemas().toSingle().flatMapObservable(response -> {
			return Observable.fromIterable(response.getData()).filter(microschema -> !StringUtils.isEmpty(microschema.getDescription()))
					.toMap(MicroschemaResponse::getDescription).toObservable();
		}).doOnSubscribe(sub -> publisher.debug("Loading microschemas"));
	}

	/**
	 * Get map of microschemas assigned to the project (keys are the uuids)
	 * @param projectName project name
	 * @return observable emitting the single map
	 */
	protected Observable<Map<String, MicroschemaResponse>> microschemas(String projectName) {
		return publisher.client.findMicroschemas(projectName).toSingle().retry(MeshPublisher.RETRY_HANDLER).flatMapObservable(response -> {
			return Observable.fromIterable(response.getData()).filter(microschema -> microschemaMap.containsKey(microschema.getDescription()))
					.toMap(MicroschemaResponse::getUuid).toObservable();
		});
	}

	/**
	 * Get map of microschema infos of a branch
	 * @param projectName project name
	 * @param branchUuid branch uuid
	 * @return observable emitting the single map
	 */
	protected Observable<Map<String, BranchMicroschemaInfo>> microschemas(String projectName, String branchUuid) {
		return publisher.client.getBranchMicroschemaVersions(projectName, branchUuid).toSingle().flatMapObservable(response -> {
			return Observable.fromIterable(response.getMicroschemas()).toMap(BranchMicroschemaInfo::getUuid).toObservable();
		});
	}

	/**
	 * Process the constructs. For each construct, check whether the microschema map contains the matching microschema.
	 * @param constructs observable emitting the constructs
	 * @param microschemaMap observable emitting the map of microschemas
	 * @param check true to check validity of microschemas
	 * @param repair true to repair/create invalid/missing microschemas, false to only check
	 * @param success success flag
	 * @return observable emitting all microschemas
	 */
	protected Observable<MicroschemaResponse> processConstructs(Observable<Construct> constructs,
			Observable<Map<String, MicroschemaResponse>> microschemaMap, boolean check, boolean repair, AtomicBoolean success) {
		return Observable.zip(constructs, microschemaMap.cache().repeat(), (construct, map) -> {
			try (Trx trx = new Trx(); LangTrx lTrx = new LangTrx("en")) {
				publisher.debug("Check existence of microschema for %s", construct);
				String uuid = MeshPublisher.getMeshUuid(construct);

				if (map.containsKey(uuid)) {
					if (check) {
						if (repair) {
							return diffAndUpdate(map.get(uuid), construct).toObservable();
						} else {
							return diff(map.get(uuid), construct, success).toObservable();
						}
					} else {
						return Observable.just(map.get(uuid));
					}
				} else {
					publisher.info("Did not find microschema for %s", construct);
					if (check) {
						if (repair) {
							return create(construct).toObservable();
						} else {
							success.set(false);
							Observable<MicroschemaResponse> empty = Observable.empty();
							return empty;
						}
					} else {
						Observable<MicroschemaResponse> empty = Observable.empty();
						return empty;
					}
				}
			}
		}).flatMap(x -> x);
	}

	/**
	 * Process assignment of microschemas to project
	 * @param projectName project name
	 * @param expected observable emitting all microschemas, which are expected to be assigned to the project
	 * @param assigned observable emitting the map of current assignment
	 * @param repair true to repair
	 * @param success success flag
	 * @return completable
	 */
	protected Completable processAssignment(String projectName, Observable<MicroschemaResponse> expected, Observable<Map<String, MicroschemaResponse>> assigned,
			boolean repair, AtomicBoolean success) {
		Observable<MicroschemaResponse> obs = Observable.zip(expected, assigned.cache().repeat(), (microschema, map) -> {
			if (map.containsKey(microschema.getUuid())) {
				// assigned
				return Observable.just(microschema);
			} else {
				// not assigned
				publisher.info("Microschema %s not assigned to project", microschema.getName());
				if (repair) {
					return publisher.client.assignMicroschemaToProject(projectName, microschema.getUuid()).toObservable().retry(MeshPublisher.RETRY_HANDLER);
				} else {
					success.set(false);
					Observable<MicroschemaResponse> empty = Observable.empty();
					return empty;
				}
			}
		}).flatMap(x -> x);
		return Completable.fromObservable(obs);
	}

	/**
	 * Process version assignment to branch
	 * @param projectName project name
	 * @param branchUuid branch uuid
	 * @param branchName branch name
	 * @param expected observable emitting microschema (versions) expected to be assigned
	 * @param assigned observable emitting the map of current version assignment
	 * @param repair true to repair
	 * @param success success flag
	 * @return completable
	 */
	protected Completable processVersions(String projectName, String branchUuid, String branchName, Observable<MicroschemaResponse> expected,
			Observable<Map<String, BranchMicroschemaInfo>> assigned, boolean repair, AtomicBoolean success) {

		return Observable.zip(expected, assigned.cache().repeat(), (microschema, map) -> {
			if (map.containsKey(microschema.getUuid())) {
				BranchMicroschemaInfo info = map.get(microschema.getUuid());
				if (StringUtils.equals(info.getVersion(), microschema.getVersion())) {
					Observable<BranchMicroschemaInfo> empty = Observable.empty();
					return empty;
				} else {
					publisher.info("Branch %s has version %s of microschema %s assigned, but latest schema version is %s", branchName, info.getVersion(),
							microschema.getName(), microschema.getVersion());
					if (repair) {
						MicroschemaReference ref = new MicroschemaReferenceImpl().setName(microschema.getName()).setUuid(microschema.getUuid())
								.setVersion(microschema.getVersion());

						return Observable.just(new BranchMicroschemaInfo(ref));
					} else {
						success.set(false);
						Observable<BranchMicroschemaInfo> empty = Observable.empty();
						return empty;
					}
				}
			} else {
				Observable<BranchMicroschemaInfo> empty = Observable.empty();
				return empty;
			}
		}).flatMap(x -> x).toList().flatMapCompletable(list -> {
			if (list.isEmpty()) {
				return Completable.complete();
			} else {
				BranchInfoMicroschemaList infoList = new BranchInfoMicroschemaList();
				infoList.getMicroschemas().addAll(list);
				return publisher.client.assignBranchMicroschemaVersions(projectName, branchUuid, infoList).toCompletable();
			}
		});
	}

	/**
	 * Determine diff between existing and expected microschema, if diff found, set success flag to false
	 * @param microschema existing
	 * @param construct construct
	 * @param success success flag
	 * @return maybe microschema
	 * @throws NodeException
	 */
	protected Maybe<MicroschemaResponse> diff(MicroschemaResponse microschema, Construct construct, AtomicBoolean success) throws NodeException {
		return getMicroschema(construct, new MicroschemaUpdateRequest()).flatMap(update -> {
			return publisher.client.diffMicroschema(microschema.getUuid(), update).toSingle();
		}).flatMapMaybe(diffResponse -> {
			if (diffResponse.getChanges().isEmpty()) {
				// nothing to do
				return Maybe.just(microschema).doOnSuccess(resp -> {
					try (Trx trx = new Trx(); LangTrx lTrx = new LangTrx("en")) {
						publisher.info("Microschema for %s is valid", construct);
					}
				});
			} else {
				try (Trx trx = new Trx(); LangTrx lTrx = new LangTrx("en")) {
					success.set(false);
					publisher.info("Microschema for %s is not valid", construct);
					diffResponse.getChanges().forEach(change -> MeshPublisher.logger.warn(MeshPublisher.getReadableInfo(change)));
					return Maybe.empty();
				}
			}
		});
	}

	/**
	 * Determine diff between existing and expected microschema, if diff found, update the microschema
	 * @param microschema existing
	 * @param construct construct
	 * @return maybe (possibly updated) microschema
	 * @throws NodeException
	 */
	protected Single<MicroschemaResponse> diffAndUpdate(MicroschemaResponse microschema, Construct construct) throws NodeException {
		return getMicroschema(construct, new MicroschemaUpdateRequest()).flatMap(update -> {
			// return publisher.client.diffMicroschema(microschema.getUuid(), update).toSingle();
			return publisher.client.diffMicroschema(microschema.getUuid(), update).toSingle().flatMap(diffResponse -> {
				if (diffResponse.getChanges().isEmpty()) {
					// nothing to do
					return Single.just(microschema).doOnSuccess(resp -> {
						try (Trx trx = new Trx(); LangTrx lTrx = new LangTrx("en")) {
							publisher.info("Microschema for %s is valid", construct);
						}
					});
				} else {
					return publisher.client.updateMicroschema(microschema.getUuid(), update, new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false))
							.toSingle().flatMap(resp -> publisher.client.findMicroschemaByUuid(microschema.getUuid()).toSingle()).doOnSuccess(resp -> {
								try (Trx trx = new Trx(); LangTrx lTrx = new LangTrx("en")) {
									publisher.info("Updated microschema for %s", construct);
								}
							});
				}
			});
		});
	}

	/**
	 * Create microschema for construct
	 * @param construct construct
	 * @return single of created microschema
	 * @throws NodeException
	 */
	protected Single<MicroschemaResponse> create(Construct construct) throws NodeException {
		return getMicroschema(construct, new MicroschemaCreateRequest()).flatMap(create -> {
			return publisher.client.createMicroschema(create).toSingle();
		}).doOnSuccess(resp -> {
			try (Trx trx = new Trx(); LangTrx lTrx = new LangTrx("en")) {
				publisher.info("Created microschema for %s", construct);
			}
		});
	}

	/**
	 * Generate the microschema for the given construct
	 * @param construct construct
	 * @param microschema microschema instance that is updated
	 * @return single of updated microschema
	 * @throws NodeException
	 */
	protected <T extends MicroschemaModel> Single<T> getMicroschema(Construct construct, T microschema) throws NodeException {
		microschema.setName(getMicroschemaName(construct));
		microschema.setDescription(MeshPublisher.getMeshUuid(construct));
		return Observable.fromIterable(construct.getParts()).filter(partFilter()).flatMap(this::toFieldSchema).toList().map(fields -> {
			microschema.setFields(fields);
			return microschema;
		}).doOnSubscribe(sub -> {
			try (Trx trx = new Trx(); LangTrx lTrx = new LangTrx("en")) {
				publisher.debug("Transforming %s into microschema", construct);
			}
		});
	}

	/**
	 * Transform the part into a field schema (or multiple field schemas in special cases)
	 * @param part part
	 * @return maybe field
	 * @throws NodeException
	 */
	protected Observable<FieldSchema> toFieldSchema(Part part) throws NodeException {
		List<FieldSchema> fieldSchemas = new ArrayList<>();

		switch (part.getPartTypeId()) {
			case 1: // Text
			case 9: // Text (short)
			case 37: // Text (custom form)
			case 2: // Text/HTML
			case 3: // HTML
			case 10: // Text/HTML (long)
			case 21: // HTML (long)
			case 26: // Java Editor
			case 27: // DHTML Editor
			case 36: // HTML (custom form)
			case 18: // Select (image-height)
			case 19: // Select (image-width)
			case 24: // Select (class)
			case 22: // File (localpath)
				fieldSchemas.add(new StringFieldSchemaImpl());
				break;
			case 4: // URL (page)
				FieldSchema internal = new NodeFieldSchemaImpl().setAllowedSchemas(publisher.getSchemaName(Page.TYPE_PAGE));
				internal.setName(String.format("%s_internal", part.getKeyname()));
				fieldSchemas.add(internal);

				FieldSchema external = new StringFieldSchemaImpl();
				external.setName(String.format("%s_external", part.getKeyname()));
				fieldSchemas.add(external);
				break;

			case 6: // URL (image)
			case 8: // URL (file)
			case 38: // URL (file)
				fieldSchemas.add(new NodeFieldSchemaImpl().setAllowedSchemas(publisher.getSchemaName(File.TYPE_FILE)));
				break;
			case 11: // Tag (page)
				FieldSchema pageField = new NodeFieldSchemaImpl().setAllowedSchemas(publisher.getSchemaName(Page.TYPE_PAGE));
				pageField.setName(String.format("%s_page", part.getKeyname()));
				fieldSchemas.add(pageField);

				FieldSchema tagField = new StringFieldSchemaImpl();
				tagField.setName(String.format("%s_tag", part.getKeyname()));
				fieldSchemas.add(tagField);
				break;

			case 13: // Overview
				new MeshOverviewPublisher(publisher, part.getKeyname()).addFieldSchemas(fieldSchemas);
				break;

			case 15: // List
				fieldSchemas.add(new BooleanFieldSchemaImpl().setName(String.format("%s_ordered", part.getKeyname())));
			case 16: // List (unordered)
			case 17: // List (ordered)
				fieldSchemas.add(new ListFieldSchemaImpl().setListType(FieldTypes.STRING.toString()));
				break;

			case 29: // Select (single)
				fieldSchemas.add(new StringFieldSchemaImpl());
				break;
			case 30: // Select (multiple)
				fieldSchemas.add(new ListFieldSchemaImpl().setListType(FieldTypes.STRING.toString()));
				break;

			case 20: // Tag (template)
				FieldSchema templateField = new NumberFieldSchemaImpl();
				templateField.setName(String.format("%s_template", part.getKeyname()));
				fieldSchemas.add(templateField);

				FieldSchema templateTagField = new StringFieldSchemaImpl();
				templateTagField.setName(String.format("%s_tag", part.getKeyname()));
				fieldSchemas.add(templateTagField);
				break;

			case 25: // URL (folder)
			case 39: // URL (folder)
				fieldSchemas.add(new NodeFieldSchemaImpl().setAllowedSchemas(publisher.getSchemaName(Folder.TYPE_FOLDER)));
				break;

			case 31: // Checkbox
				fieldSchemas.add(new BooleanFieldSchemaImpl());
				break;

			case 32: // Datasource
				fieldSchemas.add(new ListFieldSchemaImpl().setListType(FieldTypes.STRING.toString()));
				break;

			case 33: // Velocity
				break;

			case 34: // Breadcrumb
				break;

			case 35: // Navigation
				break;

			case 23: // Table
				break;

			case 40: // Node
				fieldSchemas.add(new NumberFieldSchemaImpl());
				break;
		}

		if (fieldSchemas.isEmpty()) {
			return Observable.empty();
		} else {
			for (FieldSchema fieldSchema : fieldSchemas) {
				if (StringUtils.isEmpty(fieldSchema.getName())) {
					fieldSchema.setName(part.getKeyname());
				}
				if (StringUtils.isEmpty(fieldSchema.getLabel())) {
					try (Trx trx = new Trx(); LangTrx lTrx = new LangTrx("en")) {
						fieldSchema.setLabel(part.getName().toString());
					}
				}
			}

			return Observable.fromIterable(fieldSchemas);
		}
	}

	/**
	 * Transform the given value into field(s)
	 * @param nodeId node ID
	 * @param value value
	 * @param postponeHandler handler for postponing updates (will be called, when Node references can not yet be set, because target Node is not present in Mesh)
	 * @return observable emitting pairs of fieldname and field
	 * @throws NodeException
	 */
	protected Observable<Pair<String, Field>> toField(int nodeId, Value value, Operator postponeHandler) throws NodeException {
		List<Pair<String, Field>> fieldList = new ArrayList<>();

		switch (value.getPart().getPartTypeId()) {
		case 1: // Text
		case 9: // Text (short)
		case 37: // Text (custom form)
		case 2: // Text/HTML
		case 3: // HTML
		case 10: // Text/HTML (long)
		case 21: // HTML (long)
		case 26: // Java Editor
		case 27: // DHTML Editor
		case 36: // HTML (custom form)
		case 18: // Select (image-height)
		case 19: // Select (image-width)
		case 24: // Select (class)
		case 22: // File (localpath)
			fieldList.add(ImmutablePair.of(value.getPart().getKeyname(), new StringFieldImpl().setString(value.getValueText())));
			break;
		case 4: // URL (page)
			PageURLPartType pageUrlPartType = value.getPartType();
			if (pageUrlPartType.getInternal() == 1) {
				Page pageTarget = pageUrlPartType.getTargetPage();
				if (pageTarget != null && pageTarget.isOnline()) {
					String targetMeshUuid = MeshPublisher.getMeshUuid(pageTarget);
					Optional<MeshProject> optionalProject = publisher.getProject(pageTarget);

					if (optionalProject.isPresent() && publisher.existsInMesh(nodeId, optionalProject.get(), pageTarget)) {
						fieldList.add(ImmutablePair.of(String.format("%s_internal", value.getPart().getKeyname()),
								new NodeFieldImpl().setUuid(targetMeshUuid)));
					} else {
						postponeHandler.operate();
					}
				}
			} else {
				fieldList.add(ImmutablePair.of(String.format("%s_external", value.getPart().getKeyname()),
						new StringFieldImpl().setString(pageUrlPartType.getExternalTarget())));
			}
			break;

		case 6: // URL (image)
		case 8: // URL (file)
		case 38: // URL (file)
		case 25: // URL (folder)
		case 39: // URL (folder)
			UrlPartType urlPartType = value.getPartType();
			NodeObject targetObject = urlPartType.getTarget();
			if (targetObject != null) {
				String targetMeshUuid = MeshPublisher.getMeshUuid(targetObject);
				Optional<MeshProject> optionalProject = publisher.getProject(targetObject);

				if (optionalProject.isPresent() && publisher.existsInMesh(nodeId, optionalProject.get(), targetObject)) {
					fieldList.add(ImmutablePair.of(value.getPart().getKeyname(), new NodeFieldImpl().setUuid(targetMeshUuid)));
				} else {
					postponeHandler.operate();
				}
			}
			break;
		case 11: // Tag (page)
		{
			PageTagPartType pageTagPartType = value.getPartType();
			TagContainer tagContainer = pageTagPartType.getTagContainer();
			Tag linkedTag = pageTagPartType.getLinkedTag();
			if (linkedTag != null && tagContainer != null) {
				NodeObject containerObject = (NodeObject) tagContainer;
				String containerMeshUuid = MeshPublisher.getMeshUuid(containerObject);
				Optional<MeshProject> optionalProject = publisher.getProject(containerObject);

				if (optionalProject.isPresent() && publisher.existsInMesh(nodeId, optionalProject.get(), containerObject)) {
					fieldList.add(ImmutablePair.of(String.format("%s_page", value.getPart().getKeyname()),
							new NodeFieldImpl().setUuid(containerMeshUuid)));
				} else {
					postponeHandler.operate();
				}
				fieldList.add(ImmutablePair.of(String.format("%s_tag", value.getPart().getKeyname()), new StringFieldImpl().setString(linkedTag.getName())));
			}
			break;
		}

		case 13: // Overview
			OverviewPartType overviewPartType = value.getPartType();
			if (overviewPartType.getOverview() != null) {
				new MeshOverviewPublisher(publisher, value.getPart().getKeyname()).addFields(nodeId, fieldList, overviewPartType.getOverview(), postponeHandler);
			}
			break;

		case 15: // List
			ChangeableListPartType changeableListPartType = value.getPartType();
			fieldList.add(ImmutablePair.of(String.format("%s_ordered", value.getPart().getKeyname()),
					new BooleanFieldImpl().setValue(changeableListPartType.isOrdered())));
			fieldList.add(ImmutablePair.of(value.getPart().getKeyname(), new StringFieldListImpl().setItems(Arrays.asList(changeableListPartType.getLines()))));
			break;
		case 16: // List (unordered)
		case 17: // List (ordered)
			ListPartType listPartType = value.getPartType();
			fieldList.add(ImmutablePair.of(value.getPart().getKeyname(), new StringFieldListImpl().setItems(Arrays.asList(listPartType.getLines()))));
			break;

		case 29: // Select (single)
		case 30: // Select (multiple)
			SelectPartType selectPartType = value.getPartType();
			List<DatasourceEntry> selection = selectPartType.getSelection();

			if (!ObjectTransformer.isEmpty(selection)) {
				if (value.getPart().getPartTypeId() == 29) {
					fieldList.add(ImmutablePair.of(value.getPart().getKeyname(), new StringFieldImpl().setString(selection.iterator().next().getKey())));
				} else {
					List<String> stringSelection = selection.stream().map(entry -> entry.getKey()).collect(Collectors.toList());
					fieldList.add(ImmutablePair.of(value.getPart().getKeyname(), new StringFieldListImpl().setItems(stringSelection)));
				}
			}
			break;

		case 20: // Tag (template)
		{
			TemplateTagPartType templateTagPartType = value.getPartType();
			TagContainer tagContainer = templateTagPartType.getTagContainer();
			Tag linkedTag = templateTagPartType.getLinkedTag();
			if (linkedTag != null && tagContainer != null) {
				Template template = (Template)tagContainer;
				fieldList.add(ImmutablePair.of(String.format("%s_template", value.getPart().getKeyname()), new NumberFieldImpl().setNumber(template.getId())));
				fieldList.add(ImmutablePair.of(String.format("%s_tag", value.getPart().getKeyname()), new StringFieldImpl().setString(linkedTag.getName())));
			}
			break;
		}

		case 31: // Checkbox
			CheckboxPartType checkboxPartType = value.getPartType();
			fieldList.add(ImmutablePair.of(value.getPart().getKeyname(), new BooleanFieldImpl().setValue(checkboxPartType.isChecked())));
			break;

		case 32: // Datasource
			DatasourcePartType datasourcePartType = value.getPartType();
			fieldList.add(ImmutablePair.of(value.getPart().getKeyname(), new StringFieldListImpl().setItems(datasourcePartType.getKeys())));
			break;

		case 33: // Velocity
			break;

		case 34: // Breadcrumb
			break;

		case 35: // Navigation
			break;

		case 23: // Table
			break;

		case 40: // Node
			NodePartType nodePartType = value.getPartType();
			Node node = nodePartType.getNode();
			if (node != null) {
				fieldList.add(ImmutablePair.of(value.getPart().getKeyname(), new NumberFieldImpl().setNumber(node.getId())));
			}
			break;
		}

		if (fieldList.isEmpty()) {
			return Observable.empty();
		} else {
			return Observable.fromIterable(fieldList);
		}
	}

	/**
	 * Transform the tag into a micronode
	 * @param mapName map name of the tagmap entry
	 * @param nodeId node ID
	 * @param tag tag to transform
	 * @param postponeHandler handler for postponed updates
	 * @return maybe micronode
	 * @throws NodeException
	 */
	protected Maybe<MicronodeResponse> toMicronode(String mapName, int nodeId, Tag tag, Operator postponeHandler) throws NodeException {
		Construct construct = tag.getConstruct();

		if (!filters.getOrDefault(mapName, ACCEPT_ALL).matches(construct)) {
			return Maybe.empty();
		}

		String uuid = MeshPublisher.getMeshUuid(construct);

		if (microschemaMap.containsKey(uuid)) {
			MicroschemaResponse microschema = microschemaMap.get(uuid);

			return Observable.fromIterable(tag.getValues()).flatMap(value -> toField(nodeId, value, postponeHandler)).toMap(Pair::getKey, Pair::getValue).map(fields -> {
				MicronodeResponse micronode = new MicronodeResponse();
				micronode.setMicroschema(new MicroschemaReferenceImpl().setUuid(microschema.getUuid()).setName(microschema.getName()).setVersion(microschema.getVersion()));
				micronode.getFields().putAll(fields);
				return micronode;
			}).toMaybe();
		} else {
			return Maybe.empty();
		}
	}

	/**
	 * Get the microschema name for the construct
	 * @param construct construct
	 * @return microschema name
	 */
	protected String getMicroschemaName(Construct construct) {
		return String.format("%s_%s", publisher.schemaPrefix, construct.getKeyword());
	}

	/**
	 * Return a filter instance that runs with the transaction
	 * @return filter
	 */
	protected Predicate<? super Part> partFilter() {
		return part -> {
			Transaction previous = TransactionManager.getCurrentTransactionOrNull();
			if (previous != t) {
				TransactionManager.setCurrentTransaction(t);
			}
			try {
				return PART_FILTER.test(part);
			} finally {
				if (previous != t) {
					TransactionManager.setCurrentTransaction(previous);
				}
			}
		};
	}

	/**
	 * Check microschema migrations of the branch
	 * @param project project
	 * @param branch branch
	 * @param unmigratedSchemas map of unmigrated schemas
	 * @return true if all migrations have been done
	 * @throws NodeException
	 */
	protected boolean checkMicroschemaMigrations(MeshProject project, BranchResponse branch, Map<String, Long> unmigratedSchemas) throws NodeException {
		boolean allDone = true;

		Set<String> knownMicroschemaUuids = microschemaMap.values().stream().map(MicroschemaResponse::getUuid)
				.collect(Collectors.toSet());

		BranchInfoMicroschemaList schemaStatusList = publisher.client.getBranchMicroschemaVersions(project.name, branch.getUuid()).blockingGet();
		for (BranchMicroschemaInfo info : schemaStatusList.getMicroschemas()) {
			// ignore unknown microschemas
			if (!knownMicroschemaUuids.contains(info.getUuid())) {
				continue;
			}

			switch (info.getMigrationStatus()) {
			case COMPLETED:
			case UNKNOWN:
				Long startTimestamp = unmigratedSchemas.get(info.getName());
				if (startTimestamp != null) {
					long duration = System.currentTimeMillis() - startTimestamp;
					publisher.info(String.format("Branch %s: Migration status for microschema %s, version %s changed to %s after waiting %d seconds", branch.getName(),
							info.getName(), info.getVersion(), info.getMigrationStatus().name(),
							TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS)));
					unmigratedSchemas.remove(info.getName());
				}
				break;
			case FAILED:
				throw new NodeException(String.format("Branch %s: Migration status for microschema %s, version %s is %s", branch.getName(), info.getName(),
						info.getVersion(), info.getMigrationStatus().name()));
			default:
				unmigratedSchemas.computeIfAbsent(info.getName(), schemaName -> {
					publisher.info(String.format("Branch %s: Migration status for microschema %s, version %s is %s", branch.getName(), info.getName(), info.getVersion(),
							info.getMigrationStatus().name()));
					return System.currentTimeMillis();
				});
				allDone = false;
				break;
			}
		}

		return allDone;
	}
}
