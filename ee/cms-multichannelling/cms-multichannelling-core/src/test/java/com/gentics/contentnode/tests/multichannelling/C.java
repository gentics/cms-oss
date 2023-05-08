package com.gentics.contentnode.tests.multichannelling;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

/**
 * This class is used for storing static constant values for the needs
 * of channel related tests. 
 * 
 * Each subclass represents a storage for each junit test class.
 * 
 * @author Antoniy Chonkov <antoniy@web-teh.net>
 */
abstract class C {
	public static final class page_channel_test {
		public final static int new_page_template_id = 79;
		public final static String new_page_description = "Some description";
		public final static String new_page_filename = "testFile.html";
		public final static String new_page_name = "SomeName";
	    
		public final static String new_page_for_l18n_description = "Some description2";
		public final static String new_page_for_l18n_filename = "testFile2.html";
		public final static String new_page_for_l18n_name = "SomeName2";
	    
		public final static String test_folder_name = "pageTestFolder";
	}
	
	public static final class folder_channel_test {
		public final static String test_folder_name = "testFolder";
		
		public final static String new_folder_name = "This is the inherited folder";
	}
	
	public static final class file_channel_test {
		public final static String test_folder_name = "fileTestFolder";
		
		public final static String first_file_name = "firstTestFile";
		public final static String first_file_description = "firstTestFileDescription";
		public final static byte[] first_file_data = new byte[] { 1, 5, 7, 3, 2};
		
		public final static String second_file_name = "secondTestFile";
		public final static String second_file_description = "secondTestFileDescription";
		public final static byte[] second_file_data = new byte[] { 6, 7, 3, 1};
	}
	
	public static final class image_channel_test {
		public final static String test_folder_name = "imageTestFolder";
		
		public final static String first_image_name = "firstTestImage";
		public final static String first_image_description = "firstTestImageDescription";
		public final static byte[] first_image_data = Resource.getImageJpg();
		public final static String first_image_type = "image/jpeg";
		
		public final static String second_image_name = "secondTestImage";
		public final static String second_image_description = "secondTestImageDescription";
		public final static byte[] second_image_data = Resource.getImagePng();
		public final static String second_image_type = "image/png";
	}
	
	public static final class template_channel_test {
		public final static String test_folder_name = "templateTestFolder";
		public final static String template_content = "<html><head></head><body><b>test</b></body></html>";
		
		public final static String first_template_name = "firstTestTemplate";
		public final static String first_template_description = "firstTestTemplateDescription";
		
		public final static String second_template_name = "secondTestTemplate";
		public final static String second_template_description = "secondTestTemplateDescription";
	}
	
	public static final class overview_test_data {
		public final static String 	folder_name = "OverviewTests";
		public final static int 	folder_id = 46;
		
		public static final class templates {
			public final static String template1_name = "OverviewTemplate";
			public final static int template1_id = 82;
		}
		
		public static final class pages {
			public final static String 	page1_name = "TestPage1";
			public final static int 	page1_id = 56;
			public final static boolean	page1_published = true;
			
			public final static String 	page2_name = "TestPage2";
			public final static int 	page2_id = 54;
			public final static boolean	page2_published = true;
			
			public final static String 	page3_name = "TestPage3";
			public final static int 	page3_id = 55;
			public final static boolean	page3_published = false;
		}
		
		public static final class images {
			public final static String 	image1_name = "TestImage1.jpg";
			public final static int 	image1_id = 18;
			
			public final static String 	image2_name = "TestImage2.png";
			public final static int 	image2_id = 17;
		}
		
		public static final class files {
			public final static String 	file1_name = "TestFile1.txt";
			public final static int 	file1_id = 22;
			
			public final static String 	file2_name = "TestFile2.txt";
			public final static int 	file2_id = 21;
		}
		
		public static final class testFolder1 {
			public final static String 	folder_name = "TestFolder1";
			public final static int 	folder_id = 48;
		}
		
		public static final class testFolder2 {
			public final static String 	folder_name = "TestFolder2";
			public final static int 	folder_id = 47;
			
			public static final class pages {
				public final static String 	page4_name = "TestPage4";
				public final static int 	page4_id = 59;
				public final static boolean	page4_published = true;
				
				public final static String 	page5_name = "TestPage5";
				public final static int 	page5_id = 57;
				public final static boolean	page5_published = true;
				
				public final static String 	page6_name = "TestPage6";
				public final static int 	page6_id = 58;
				public final static boolean	page6_published = false;
			}
			
			public static final class images {
				public final static String 	image3_name = "TestImage3.jpg";
				public final static int 	image3_id = 20;
				
				public final static String 	image4_name = "TestImage4.png";
				public final static int 	image4_id = 19;
			}
			
			public static final class files {
				public final static String 	file3_name = "TestFile3.txt";
				public final static int 	file3_id = 24;
				
				public final static String 	file4_name = "TestFile4.txt";
				public final static int 	file4_id = 23;
			}
		}
	}
	
	public static final class reference_test_data {
		public final static int folder_id = 49;
		public final static int template1_id = 83;
		public final static byte[] image_data = Resource.getImageJpg();
		public final static String page_tag_master_value = "master-tag";
		public final static String page_tag_channel_value = "channel-tag";
	}
	
	public static final class channel_sync {
		public final static String image_name_jpg = "genticsImage.jpg";
		public final static byte[] image_data_jpg = Resource.getImageJpg();
		
		public final static String image_name_png = "genticsImage.png";
		public final static byte[] image_data_png = Resource.getImagePng();
	}
	
	public static final class Resource {
		private static byte[] getImage(String filename) {
			try {
				return IOUtils.toByteArray(Resource.class.getResourceAsStream(filename));
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
			return null;
		}
		
		public static byte[] getImageJpg() {
			return getImage("gentics.jpg");
		}
		
		public static byte[] getImagePng() {
			return getImage("gentics.png");
		}
	}
}
