package com.gentics.contentnode.tests.rest.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.rest.model.CmpCompatibility;
import com.gentics.contentnode.rest.model.CmpProduct;
import com.gentics.contentnode.rest.util.CmpVersionUtils;
import com.gentics.contentnode.version.CmpProductVersion;
import com.gentics.contentnode.version.CmpVersionRequirement;
import com.gentics.contentnode.version.PortalVersionResponse;
import com.gentics.contentnode.version.ProductVersionRange;

@RunWith(Parameterized.class)
public class CmpVersionUtilsTest {
	
	@SuppressWarnings("serial")
	@Parameters(name = "{index} req: {0}, mesh: {1}, portal: {2}, expectedResult: {3}")
	public static Collection<Object[]> data() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return Arrays.asList(
			// Happy case, all set = Supported
			new Object[] { 
					helpSettingInitialValue(
							new CmpVersionRequirement(), 
							"setProductVersions", 
							new HashMap<CmpProduct, ProductVersionRange>() {{
									put(CmpProduct.MESH, new ProductVersionRange().setMinVersion("1.2.0").setMaxVersion("1.3.0"));
							    }}, 
							Map.class), 
					new CmpProductVersion("1.2.3"), 
					helpSettingInitialValue(
							helpSettingInitialValue(new PortalVersionResponse(), "setProductVersion", "1.2.3", String.class), 
								"setProductName", "Gentics Mesh", String.class), 
					CmpCompatibility.SUPPORTED 
				},
			// Incompatible versions, all set = Unsupported
			new Object[] { 
					helpSettingInitialValue(
							new CmpVersionRequirement(), 
							"setProductVersions", 
							new HashMap<CmpProduct, ProductVersionRange>() {{
							        put(CmpProduct.MESH, new ProductVersionRange().setMinVersion("1.0.0").setMaxVersion("1.0.9"));
							    }}, 
							Map.class), 
					new CmpProductVersion("1.1"), 
					helpSettingInitialValue(
							helpSettingInitialValue(new PortalVersionResponse(), "setProductVersion", "1.0.9", String.class), 
								"setProductName", "Gentics Mesh", String.class), 
					CmpCompatibility.NOT_SUPPORTED 
				},
			// Happy Case + No version requirement = Unknown
			new Object[] { 
					null, 
					new CmpProductVersion("1.1"), 
					helpSettingInitialValue(
							helpSettingInitialValue(new PortalVersionResponse(), "setProductVersion", "1.0.9", String.class), 
								"setProductName", "Gentics Mesh", String.class), 
					CmpCompatibility.UNKNOWN 
				},
			// Happy Case + No Product version = Unknown
			new Object[] { 
					helpSettingInitialValue(
							new CmpVersionRequirement(), 
							"setProductVersions", 
							new HashMap<CmpProduct, ProductVersionRange>() {{
							        put(CmpProduct.MESH, new ProductVersionRange().setMinVersion("1.0.0").setMaxVersion("1.0.9"));
							    }}, 
							Map.class), 
					null, 
					helpSettingInitialValue(
							helpSettingInitialValue(new PortalVersionResponse(), "setProductVersion", "1.0.9", String.class), 
								"setProductName", "Gentics Mesh", String.class), 
					CmpCompatibility.UNKNOWN 
				},
			// Happy Case + No Portal set = Supported (SUP-10096#2)
			new Object[] { 
					helpSettingInitialValue(
							new CmpVersionRequirement(), 
							"setProductVersions", 
							new HashMap<CmpProduct, ProductVersionRange>() {{
							        put(CmpProduct.MESH, new ProductVersionRange().setMinVersion("1.0.0").setMaxVersion("1.0.9"));
							    }}, 
							Map.class), 
					new CmpProductVersion("1.0.5"), 
					null, 
					CmpCompatibility.SUPPORTED 
				}
		);
	}
	
	@Parameter(0)
	public CmpVersionRequirement req;
	
	@Parameter(1)
	public CmpProductVersion meshVersion;
	
	@Parameter(2)
	public PortalVersionResponse portalVersionResponse;
	
	@Parameter(3)
	public CmpCompatibility expectedResult;

	@Test
	public void testCreateVersionInfo() {
		assertThat(CmpVersionUtils.createVersionInfo(req, meshVersion, portalVersionResponse).getCompatibility())
			.as("Compare value")
			.isEqualTo(expectedResult);
	}
	
	@SuppressWarnings("unchecked")
	private static final <T, V> T helpSettingInitialValue(T target, String setter, V value, Class<?> classOfV) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<T> cls = (Class<T>) target.getClass();
		Method setterMethod = cls.getMethod(setter, classOfV);
		
		setterMethod.invoke(target, value);		
		return target;
	}
}
