package com.gentics.contentnode.render;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.ImageFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.invocation.InvocationOnMock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test cases for the {@link GisDirective#calculateFpCropInfo(ImageFile, GisDirective.ResizeInfo)} method,
 * which handles CropInfo calculation for the "fpsmart" resize mode of the #gtx_gis directive.
 */
@RunWith(Parameterized.class)
public class GisDirectiveCalculateFpCropInfoTest {
	
	/**
	 * Since GisDirective.CropInfo is not publicly accessible and not
	 * easily constructible without a HashMap, we use this class.
	 */
	protected static class CropRectangle {
		int x;
		int y;
		int width;
		int height;
		
		public CropRectangle(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
		
		@Override
		public String toString() {
			return "(x: " + x + " y: " + y + " width: " + width + " height: " + height + ")";
		}
	}
	
	/**
	 * Provides access to the protected {@link GisDirective#calculateFpCropInfo(ImageFile, ResizeInfo)} method.
	 */
	protected static class GisDirectiveAdapter extends GisDirective {
		
		/**
		 * Calls the {@link GisDirective#calculateFpCropInfo(ImageFile, ResizeInfo)} method and returns
		 * the result as a CropRectangle.
		 */
		public CropRectangle calculateFpCropInfo(ImageFile image, int resizeWidth, int resizeHeight) throws NodeException {
			Map<Object, Object> resizeInfoMap = new HashMap<>();
			resizeInfoMap.put(WIDTH_ARG, resizeWidth);
			resizeInfoMap.put(HEIGHT_ARG, resizeHeight);
			resizeInfoMap.put(MODE_ARG, Mode.fpsmart.toString());
			resizeInfoMap.put(TYPE_ARG, Type.url.toString());
			ResizeInfo resizeInfo = new ResizeInfo(resizeInfoMap);
			
			CropInfo cropInfo = calculateFpCropInfo(image, resizeInfo);
			return new CropRectangle(cropInfo.x, cropInfo.y, cropInfo.width, cropInfo.height);
		}
	
	}
	
	/**
	 * Provides a toString() for mocked images.
	 */
	protected static String imageFileToString(InvocationOnMock invocation) {
		ImageFile image = (ImageFile) invocation.getMock();
		return "(width: " + image.getSizeX() + " height: " + image.getSizeY() + " fpX: " + image.getFpX() + " fpY: " + image.getFpY() + ")";
	}

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: image: {0}, resizeWidth: {1}, resizeHeight: {2}, expectedCropRect: {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		
		ImageFile image;
		int resizeWidth, resizeHeight;
		CropRectangle expectedCropRect;

		// Shrink 4:3 image to 16:9 with focal point at center.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(4000);
		when(image.getSizeY()).thenReturn(3000);
		when(image.getFpX()).thenReturn(0.5f);
		when(image.getFpY()).thenReturn(0.5f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 1600;
		resizeHeight = 900;
		expectedCropRect = new CropRectangle(0, 375, 4000, 2250);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Shrink 4:3 image to 9:16 with focal point at center.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(4000);
		when(image.getSizeY()).thenReturn(3000);
		when(image.getFpX()).thenReturn(0.5f);
		when(image.getFpY()).thenReturn(0.5f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 900;
		resizeHeight = 1600;
		expectedCropRect = new CropRectangle(1157, 0, 1687, 3000);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Shrink 4:3 image to 1:1 with focal point at center.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(4000);
		when(image.getSizeY()).thenReturn(3000);
		when(image.getFpX()).thenReturn(0.5f);
		when(image.getFpY()).thenReturn(0.5f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 1000;
		resizeHeight = 1000;
		expectedCropRect = new CropRectangle(500, 0, 3000, 3000);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Enlarge 4:3 image to 16:9 with focal point at center.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(400);
		when(image.getSizeY()).thenReturn(300);
		when(image.getFpX()).thenReturn(0.5f);
		when(image.getFpY()).thenReturn(0.5f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 1600;
		resizeHeight = 900;
		expectedCropRect = new CropRectangle(0, 38, 400, 225);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Enlarge 4:3 image to 9:16 with focal point at center.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(400);
		when(image.getSizeY()).thenReturn(300);
		when(image.getFpX()).thenReturn(0.5f);
		when(image.getFpY()).thenReturn(0.5f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 900;
		resizeHeight = 1600;
		expectedCropRect = new CropRectangle(116, 0, 168, 300);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Enlarge 4:3 image to 1:1 with focal point at center.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(400);
		when(image.getSizeY()).thenReturn(300);
		when(image.getFpX()).thenReturn(0.5f);
		when(image.getFpY()).thenReturn(0.5f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 1000;
		resizeHeight = 1000;
		expectedCropRect = new CropRectangle(50, 0, 300, 300);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Shrink 16:9 image to 4:3 with focal point at top left.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(1600);
		when(image.getSizeY()).thenReturn(900);
		when(image.getFpX()).thenReturn(0.1f);
		when(image.getFpY()).thenReturn(0.1f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 400;
		resizeHeight = 300;
		expectedCropRect = new CropRectangle(0, 0, 1200, 900);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Shrink 16:9 image to 4:3 with focal point at top right.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(1600);
		when(image.getSizeY()).thenReturn(900);
		when(image.getFpX()).thenReturn(0.9f);
		when(image.getFpY()).thenReturn(0.1f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 400;
		resizeHeight = 300;
		expectedCropRect = new CropRectangle(400, 0, 1200, 900);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Shrink 16:9 image to 4:3 with focal point at bottom right.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(1600);
		when(image.getSizeY()).thenReturn(900);
		when(image.getFpX()).thenReturn(0.9f);
		when(image.getFpY()).thenReturn(0.9f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 400;
		resizeHeight = 300;
		expectedCropRect = new CropRectangle(400, 0, 1200, 900);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Shrink 16:9 image to 4:3 with focal point at bottom left.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(1600);
		when(image.getSizeY()).thenReturn(900);
		when(image.getFpX()).thenReturn(0.1f);
		when(image.getFpY()).thenReturn(0.9f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 400;
		resizeHeight = 300;
		expectedCropRect = new CropRectangle(0, 0, 1200, 900);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Shrink 4:3 image to 16:9 with focal point at top left.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(4000);
		when(image.getSizeY()).thenReturn(3000);
		when(image.getFpX()).thenReturn(0.1f);
		when(image.getFpY()).thenReturn(0.1f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 1600;
		resizeHeight = 900;
		expectedCropRect = new CropRectangle(0, 0, 4000, 2250);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Shrink 4:3 image to 16:9 with focal point at top right.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(4000);
		when(image.getSizeY()).thenReturn(3000);
		when(image.getFpX()).thenReturn(0.9f);
		when(image.getFpY()).thenReturn(0.1f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 1600;
		resizeHeight = 900;
		expectedCropRect = new CropRectangle(0, 0, 4000, 2250);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Shrink 4:3 image to 16:9 with focal point at bottom right.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(4000);
		when(image.getSizeY()).thenReturn(3000);
		when(image.getFpX()).thenReturn(0.9f);
		when(image.getFpY()).thenReturn(0.9f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 1600;
		resizeHeight = 900;
		expectedCropRect = new CropRectangle(0, 750, 4000, 2250);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Shrink 4:3 image to 16:9 with focal point at bottom left.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(4000);
		when(image.getSizeY()).thenReturn(3000);
		when(image.getFpX()).thenReturn(0.1f);
		when(image.getFpY()).thenReturn(0.9f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 1600;
		resizeHeight = 900;
		expectedCropRect = new CropRectangle(0, 750, 4000, 2250);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Enlarge 4:3 image to 1:1 with focal point at top left.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(400);
		when(image.getSizeY()).thenReturn(300);
		when(image.getFpX()).thenReturn(0.0f);
		when(image.getFpY()).thenReturn(0.0f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 1000;
		resizeHeight = 1000;
		expectedCropRect = new CropRectangle(0, 0, 300, 300);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Enlarge 4:3 image to 1:1 with focal point at top right.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(400);
		when(image.getSizeY()).thenReturn(300);
		when(image.getFpX()).thenReturn(1.0f);
		when(image.getFpY()).thenReturn(0.0f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 1000;
		resizeHeight = 1000;
		expectedCropRect = new CropRectangle(100, 0, 300, 300);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Enlarge 4:3 image to 1:1 with focal point at bottom right.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(400);
		when(image.getSizeY()).thenReturn(300);
		when(image.getFpX()).thenReturn(1.0f);
		when(image.getFpY()).thenReturn(1.0f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 1000;
		resizeHeight = 1000;
		expectedCropRect = new CropRectangle(100, 0, 300, 300);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });
		
		// Enlarge 4:3 image to 1:1 with focal point at bottom left.
		image = mock(ImageFile.class);
		when(image.getSizeX()).thenReturn(400);
		when(image.getSizeY()).thenReturn(300);
		when(image.getFpX()).thenReturn(0.0f);
		when(image.getFpY()).thenReturn(1.0f);
		when(image.toString()).then(GisDirectiveCalculateFpCropInfoTest::imageFileToString);
		resizeWidth = 1000;
		resizeHeight = 1000;
		expectedCropRect = new CropRectangle(0, 0, 300, 300);
		data.add(new Object[] { image, resizeWidth, resizeHeight, expectedCropRect });

		return data;
	}

	
	/**
	 * Create a test instance
	 * @param image the image, on which the cropping calculation should be tested
	 * @param resizeWidth the width, to which the image should be resized
	 * @param resizeHeight the height, to which the image should be resized
	 * @param expectedCropRect the expected result
	 */
	public GisDirectiveCalculateFpCropInfoTest(ImageFile image, int resizeWidth, int resizeHeight, CropRectangle expectedCropRect) {
		this.image = image;
		this.resizeWidth = resizeWidth;
		this.resizeHeight = resizeHeight;
		this.expectedCropRect = expectedCropRect;
	}
	
	/**
	 * the image, on which the cropping calculation should be tested
	 */
	protected ImageFile image;
	
	/**
	 * the width, to which the image should be resized
	 */
	protected int resizeWidth;
	
	/**
	 * the height, to which the image should be resized
	 */
	protected int resizeHeight;
	
	/**
	 * the expected result
	 */
	protected CropRectangle expectedCropRect;
	

	/**
	 * Test calculation of CropInfo with a focal point.
	 * @throws Exception
	 */
	@Test
	public void testCalculateFpCropInfo() throws Exception {
		GisDirectiveAdapter gisDirective = new GisDirectiveAdapter();
		CropRectangle actualCropRect = gisDirective.calculateFpCropInfo(image, resizeWidth, resizeHeight);
		assertEquals("CropInfo.x does not match the expected value", expectedCropRect.x, actualCropRect.x);
		assertEquals("CropInfo.y does not match the expected value", expectedCropRect.y, actualCropRect.y);
		assertEquals("CropInfo.width does not match the expected value", expectedCropRect.width, actualCropRect.width);
		assertEquals("CropInfo.height does not match the expected value", expectedCropRect.height, actualCropRect.height);
	}
	
}
