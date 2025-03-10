package com.gentics.contentnode.rest.model.response;

import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response for request to render a page
 */
@XmlRootElement
public class PageRenderResponse extends GenericResponse {

	/**
	 * Rendered content
	 */
	protected String content;
	
	/**
	 * Rendered Inherit Content
	 */
	protected String inheritedContent;

	/**
	 * Map of properties
	 */
	protected Map<String, String> properties;
	
	/**
	 * Map of Inherit Properties
	 */
	protected Map<String, String> inheritedProperties;

	/**
	 * List of tags found in the rendered page
	 */
	protected List<Tag> tags;

	/**
	 * List of meta editables found in the rendered page
	 */
	protected List<MetaEditable> metaeditables;

	/**
	 * Render time
	 */
	protected Long time;

	/**
	 * Create an instance
	 */
	public PageRenderResponse() {}

	/**
	 * Create an instance with a message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public PageRenderResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Get the content
	 * @return content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Get the properties
	 * @return properties
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Get the tags
	 * @return tags
	 */
	public List<Tag> getTags() {
		return tags;
	}

	/**
	 * Get the meta editables
	 * @return meta editables
	 */
	public List<MetaEditable> getMetaeditables() {
		return metaeditables;
	}

	/**
	 * Rrender time in milliseconds
	 * @return render time in milliseconds
	 */
	public Long getTime() {
		return time;
	}

	/**
	 * Set the content
	 * @param content content
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * Set the properties
	 * @param properties properties
	 */
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	/**
	 * Set the tags
	 * @param tags tags
	 */
	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	/**
	 * Set the meta editables
	 * @param metaeditables meta editables
	 */
	public void setMetaeditables(List<MetaEditable> metaeditables) {
		this.metaeditables = metaeditables;
	}

	/**
	 * Set the render time in milliseconds
	 * @param time render time in milliseconds
	 */
	public void setTime(Long time) {
		this.time = time;
	}
	
	/**
	 * Get inheritedContent
	 * @return the inheritedContent
	 */
	public String getInheritedContent() {
		return inheritedContent;
	}

	/**
	 * Set inheritedContent
	 * @param inheritedContent the inheritedContent to set
	 */
	public void setInheritedContent(String inheritedContent) {
		this.inheritedContent = inheritedContent;
	}

	/**
	 * Get inheritedProperties
	 * @return the inheritedProperties
	 */
	public Map<String, String> getInheritedProperties() {
		return inheritedProperties;
	}

	/**
	 * Set inheritedProperties
	 * @param inheritedProperties the inheritedProperties to set
	 */
	public void setInheritedProperties(Map<String, String> inheritedProperties) {
		this.inheritedProperties = inheritedProperties;
	}


	/**
	 * Class for instances of tags
	 */
	public static class Tag {

		/**
		 * ID of the DOM element of the tag
		 */
		protected String element;

		/**
		 * Tagname
		 */
		protected String tagname;

		/**
		 * True if the tag only contains editables, false if not
		 */
		protected boolean onlyeditables;

		/**
		 * List of editables (if the tag contains any)
		 */
		protected List<Editable> editables;

		/**
		 * Empty constructor
		 */
		public Tag() {}

		/**
		 * Create an instance of the tag
		 * @param element element id
		 * @param tagname tagname
		 */
		public Tag(String element, String tagname) {
			this.element = element;
			this.tagname = tagname;
		}

		/**
		 * @return the element
		 */
		public String getElement() {
			return element;
		}

		/**
		 * @param element the element to set
		 */
		public void setElement(String element) {
			this.element = element;
		}

		/**
		 * @return the tagname
		 */
		public String getTagname() {
			return tagname;
		}

		/**
		 * @return true if the tag only contains editables, false if not
		 */
		public boolean isOnlyeditables() {
			return onlyeditables;
		}

		/**
		 * @param tagname the tagname to set
		 */
		public void setTagname(String tagname) {
			this.tagname = tagname;
		}

		/**
		 * @return the editables
		 */
		public List<Editable> getEditables() {
			return editables;
		}

		/**
		 * @param editables the editables to set
		 */
		public void setEditables(List<Editable> editables) {
			this.editables = editables;
		}

		/**
		 * Set whether the tag only contains editables
		 * @param onlyeditables
		 */
		public void setOnlyeditables(boolean onlyeditables) {
			this.onlyeditables = onlyeditables;
		}
	}

	/**
	 * Class for instances of editables
	 */
	public static class Editable {

		/**
		 * ID of the DOM element of the editable
		 */
		protected String element;

		/**
		 * Partname
		 */
		protected String partname;

		/**
		 * Readonly
		 */
		protected boolean readonly;

		/**
		 * Empty constructor
		 */
		public Editable() {}

		/**
		 * Create an instance for an editable
		 * @param element id of the DOM element of the editable
		 * @param partname partname
		 * @param readonly true if the editable is readonly
		 */
		public Editable(String element, String partname,
				boolean readonly) {
			this.element = element;
			this.partname = partname;
			this.readonly = readonly;
		}

		/**
		 * @return the element id
		 */
		public String getElement() {
			return element;
		}

		/**
		 * @param element the element id to set
		 */
		public void setElement(String element) {
			this.element = element;
		}

		/**
		 * @return the partname
		 */
		public String getPartname() {
			return partname;
		}

		/**
		 * @param partname the partname to set
		 */
		public void setPartname(String partname) {
			this.partname = partname;
		}

		/**
		 * @return the readonly
		 */
		public boolean isReadonly() {
			return readonly;
		}

		/**
		 * @param readonly the readonly to set
		 */
		public void setReadonly(boolean readonly) {
			this.readonly = readonly;
		}
	}

	/**
	 * Class for instances of meta editables
	 */
	public static class MetaEditable {

		/**
		 * ID of the DOM element of the editable
		 */
		protected String element;

		/**
		 * Meta property that is edited
		 */
		protected String metaproperty;

		/**
		 * Empty constructor
		 */
		public MetaEditable() {}

		/**
		 * Create an instance
		 * @param element ID of the DOM element
		 * @param metaproperty meta property
		 */
		public MetaEditable(String element, String metaproperty) {
			this.element = element;
			this.metaproperty = metaproperty;
		}

		/**
		 * @return the element
		 */
		public String getElement() {
			return element;
		}

		/**
		 * @param element the element to set
		 */
		public void setElement(String element) {
			this.element = element;
		}

		/**
		 * @return the metaproperty
		 */
		public String getMetaproperty() {
			return metaproperty;
		}

		/**
		 * @param metaproperty the metaproperty to set
		 */
		public void setMetaproperty(String metaproperty) {
			this.metaproperty = metaproperty;
		}
	}
}
