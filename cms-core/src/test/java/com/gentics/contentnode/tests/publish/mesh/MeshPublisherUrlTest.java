package com.gentics.contentnode.tests.publish.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;

@RunWith(value = Parameterized.class)
public class MeshPublisherUrlTest {
	@Parameter(0)
	public String url;

	@Parameter(1)
	public String hostname;

	@Parameter(2)
	public int port;

	@Parameter(3)
	public String project;

	@Parameter(4)
	public boolean ssl;

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {"hostname/project", "hostname", 80, "project", false});
		data.add(new Object[] {"hostname:8000/project", "hostname", 8000, "project", false});
		data.add(new Object[] {"//hostname/project", "hostname", 80, "project", false});
		data.add(new Object[] {"//hostname:4711/project", "hostname", 4711, "project", false});
		data.add(new Object[] {"http://hostname/project", "hostname", 80, "project", false});
		data.add(new Object[] {"https://hostname/project", "hostname", 443, "project", true});
		data.add(new Object[] {"https://hostname:9999/project", "hostname", 9999, "project", true});
		return data;
	}

	@Before
	public void setup() {
		TransactionManager.setCurrentTransaction(mock(Transaction.class));
	}

	@After
	public void tearDown() {
		TransactionManager.setCurrentTransaction(null);
	}

	@Test
	public void test() throws NodeException, MalformedURLException {
		ContentRepository cr = mock(ContentRepository.class);
		when(cr.getCrType()).thenReturn(ContentRepositoryModel.Type.mesh);
		when(cr.getUrl()).thenReturn(url);
		when(cr.getEffectiveUrl()).thenReturn(url);

		try (MeshPublisher publisher = new MeshPublisher(cr, false)) {
			assertThat(publisher.getHost()).as("Hostname").isEqualTo(hostname);
			assertThat(publisher.getPort()).as("Port").isEqualTo(port);
			assertThat(publisher.isSsl()).as("SSL").isEqualTo(ssl);
			assertThat(publisher.getSchemaPrefix()).as("Schema prefix").isEqualTo(project);
		}
	}
}
