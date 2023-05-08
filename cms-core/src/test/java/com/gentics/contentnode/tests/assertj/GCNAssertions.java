package com.gentics.contentnode.tests.assertj;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.util.Objects;

import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.PublishableNodeObject;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.rest.model.FilePrivileges;
import com.gentics.contentnode.rest.model.PagePrivileges;
import com.gentics.contentnode.rest.model.RolePermissionsModel;
import com.gentics.contentnode.rest.model.devtools.Package;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.migration.MigrationJobEntry;
import com.gentics.contentnode.rest.model.response.migration.MigrationJobLogEntryItem;

/**
 * GCN specific assertions
 */
public class GCNAssertions extends Assertions {
	public static DependencyAssert assertThat(Dependency actual) {
		return new DependencyAssert(actual);
	}

	public static ContentMapAssert assertThat(ContentMap actual) {
		return new ContentMapAssert(actual);
	}

	public static ConstructAssert assertThat(Construct actual) {
		return new ConstructAssert(actual);
	}

	public static PartAssert assertThat(Part actual) {
		return new PartAssert(actual);
	}

	public static ValueAssert assertThat(Value actual) {
		return new ValueAssert(actual);
	}

	public static DatasourceAssert assertThat(Datasource actual) {
		return new DatasourceAssert(actual);
	}

	public static DatasourceEntryAssert assertThat(DatasourceEntry actual) {
		return new DatasourceEntryAssert(actual);
	}

	public static ObjectTagDefinitionAssert assertThat(ObjectTagDefinition actual) {
		return new ObjectTagDefinitionAssert(actual);
	}

	public static TemplateAssert assertThat(Template actual) {
		return new TemplateAssert(actual);
	}

	public static TemplateTagAssert assertThat(TemplateTag actual) {
		return new TemplateTagAssert(actual);
	}

	public static ContentTagAssert assertThat(ContentTag actual) {
		return new ContentTagAssert(actual);
	}

	public static ObjectTagAssert assertThat(ObjectTag actual) {
		return new ObjectTagAssert(actual);
	}

	public static PageAssert assertThat(Page actual) {
		return new PageAssert(actual);
	}

	public static ContentAssert assertThat(Content actual) {
		return new ContentAssert(actual);
	}

	public static ContentFileAssert assertThat(File actual) {
		return new ContentFileAssert(actual);
	}

	public static PackageAssert assertThat(Package actual) {
		return new PackageAssert(actual);
	}

	public static ConstructModelAssert assertThat(com.gentics.contentnode.rest.model.Construct actual) {
		return new ConstructModelAssert(actual);
	}

	public static MigrationJobEntryAssert assertThat(MigrationJobEntry actual) {
		return new MigrationJobEntryAssert(actual);
	}

	public static MigrationJobLogEntryItemAssert assertThat(MigrationJobLogEntryItem actual) {
		return new MigrationJobLogEntryItemAssert(actual);
	}

	public static ContentRepositoryAssert assertThat(ContentRepository actual) {
		return new ContentRepositoryAssert(actual);
	}

	public static CrFragmentAssert assertThat(CrFragment actual) {
		return new CrFragmentAssert(actual);
	}

	public static GenericResponseAssert assertThat(GenericResponse actual) {
		return new GenericResponseAssert(actual);
	}

	public static RolePermissionsModelAssert assertThat(RolePermissionsModel actual) {
		return new RolePermissionsModelAssert(actual);
	}

	public static PagePrivilegesAssert assertThat(PagePrivileges actual) {
		return new PagePrivilegesAssert(actual);
	}

	public static FilePrivilegesAssert assertThat(FilePrivileges actual) {
		return new FilePrivilegesAssert(actual);
	}

	public static FormAssert assertThat(Form actual) {
		return new FormAssert(actual);
	}

	public static PublishableNodeObjectAssert<?, ?> assertThat(PublishableNodeObject actual) {
		if (actual == null) {
			return assertThat((Page) null);
		} else if (actual instanceof Page) {
			return assertThat((Page) actual);
		} else if (actual instanceof Form) {
			return assertThat((Form) actual);
		} else {
			fail("PublishableNodeObjectAssert not implemented for object of class " + actual.getObjectInfo().getObjectClass());
			return null;
		}
	}

	public static FolderAssert assertThat(Folder actual) {
		return new FolderAssert(actual);
	}

	public static NodeAssert assertThat(Node actual) {
		return new NodeAssert(actual);
	}

	/**
	 * Create a {@link Condition} that checks whether an object has the given attribute set to the expected value.
	 * The attribute is fetched via it's bean getter. The condition can be used with {@link AbstractAssert#has(Condition)}.
	 * <br/>
	 * Example:<br/>
	 * <code>
	 *   assertThat(myObject).has(attribute("name", "Norbert"));
	 * </code>
	 * @param name attribute name
	 * @param value expected value
	 * @return condition
	 */
	public static Condition<Object> attribute(String name, Object value) {
		return new Condition<>(o -> {
			try {
				Class<?> clazz = o.getClass();
				if (o instanceof NodeObject) {
					clazz = ((NodeObject) o).getObjectInfo().getObjectClass();
				}
				for (String prefix : Arrays.asList("get", "is")) {
					String getterName = String.format("%s%s%s", prefix, name.substring(0, 1).toUpperCase(), name.substring(1));
					try {
						Method getter = clazz.getMethod(getterName, new Class[] {});
						return Objects.areEqual(getter.invoke(o, new Object[] {}), value);
					} catch (NoSuchMethodException e) {
					}
				}
				return false;
			} catch (SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
				return false;
			}
		}, String.format("value '%s' for attribute '%s'", value, name));
	}
}
