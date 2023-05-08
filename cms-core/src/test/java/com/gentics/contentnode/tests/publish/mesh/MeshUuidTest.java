package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.publish.mesh.MeshPublisher;

/**
 * Test cases for transforming global IDs into Mesh UUIDs
 */
@RunWith(value = Parameterized.class)
public class MeshUuidTest {
	@Parameters(name = "{index}: {0} -> {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {"3D6C.0fcd3497-962f-11e8-918d-00155df038f9", "0fcd3497962f11e8918d00155df038f9"});
		data.add(new Object[] {"A547.69433", "00000000000000000000000a54769433"});
		return data;
	}

	@Parameter(0)
	public String globalId;

	@Parameter(1)
	public String meshUuid;

	@Test
	public void test() {
		assertThat(MeshPublisher.toMeshUuid(globalId)).isEqualTo(meshUuid);
	}
}
