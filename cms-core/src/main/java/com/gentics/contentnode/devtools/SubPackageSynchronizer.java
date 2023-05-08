package com.gentics.contentnode.devtools;

import java.nio.file.Path;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.rest.model.devtools.Package;

/**
 * Subpackage synchronization implementation
 */
public class SubPackageSynchronizer extends PackageSynchronizer {
	/**
	 * Lambda that generates the rest model for a package
	 */
	public final static Function<SubPackageSynchronizer, Package> TRANSFORM2REST = synchronizer -> {
		Package restModel = new Package(synchronizer.getName());
		restModel.setConstructs(synchronizer.getObjects(Construct.class).size());
		restModel.setDatasources(synchronizer.getObjects(Datasource.class).size());
		restModel.setObjectProperties(synchronizer.getObjects(ObjectTagDefinition.class).size());
		restModel.setTemplates(synchronizer.getObjects(Template.class).size());
		restModel.setCrFragments(synchronizer.getObjects(CrFragment.class, false).size());
		restModel.setContentRepositories(synchronizer.getObjects(ContentRepository.class, false).size());
		restModel.setDescription(synchronizer.getDescription());
		return restModel;
	};

	/**
	 * Create an instance
	 * @param packagePath package path
	 * @throws NodeException
	 */
	public SubPackageSynchronizer(Path packagePath) throws NodeException {
		super(packagePath);
	}
}
