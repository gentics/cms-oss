package com.gentics.contentnode.rest.model.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.staging.ContentPackageChannel;
import com.gentics.contentnode.rest.model.staging.ContentPackageFile;
import com.gentics.contentnode.rest.model.staging.ContentPackageFolder;
import com.gentics.contentnode.rest.model.staging.ContentPackageForm;
import com.gentics.contentnode.rest.model.staging.ContentPackageImage;
import com.gentics.contentnode.rest.model.staging.ContentPackageNode;
import com.gentics.contentnode.rest.model.staging.ContentPackagePage;
import com.gentics.contentnode.rest.model.staging.StagedError;
import com.gentics.contentnode.staging.StagedChannel;
import com.gentics.contentnode.staging.StagedFile;
import com.gentics.contentnode.staging.StagedFolder;
import com.gentics.contentnode.staging.StagedForm;
import com.gentics.contentnode.staging.StagedImage;
import com.gentics.contentnode.staging.StagedNode;
import com.gentics.contentnode.staging.StagedPage;
import com.gentics.contentnode.staging.StagingBase;
import com.gentics.contentnode.staging.StagingError;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentPackageFolderResponse extends GenericJobStatusResponse {

	private static final long serialVersionUID = -2151578063422454979L;

	private List<ContentPackageNode> nodes = new ArrayList<>();
	private List<ContentPackageChannel> channels = new ArrayList<>();

	private List<ContentPackageFolder> folders = new ArrayList<>();
	private List<ContentPackageFile> files = new ArrayList<>();
	private List<ContentPackageImage> images = new ArrayList<>();
	private List<ContentPackagePage> pages = new ArrayList<>();
	private List<ContentPackageForm> forms = new ArrayList<>();

	@XmlTransient
	@JsonIgnore
	public static final ContentPackageFolderResponse fromContent(List<StagingBase> items, List<StagingError> errors) {
		ContentPackageFolderResponse response = new ContentPackageFolderResponse();

		Optional.ofNullable(items).ifPresent(i -> i.stream().forEach(item -> {
			List<StagedError> myErrors = Optional.ofNullable(errors).orElse(Collections.emptyList()).stream()
					.filter(e -> e.getGlobalId().equals(item.getId()))
					.map(StagingError::getError)
					.map(StagedError::new)
					.collect(Collectors.toList());

			switch (item.getTypeName()) {
			case StagedPage.TYPE_NAME:
				ContentPackagePage page = new ContentPackagePage();
				page.clone((StagedPage) item);
				page.setErrors(myErrors);
				response.getPages().add(page);
				break;
			case StagedFile.TYPE_NAME:
				ContentPackageFile file = new ContentPackageFile();
				file.clone((StagedFile) item);
				file.setErrors(myErrors);
				response.getFiles().add(file);
				break;
			case StagedImage.TYPE_NAME:
				ContentPackageImage image = new ContentPackageImage();
				image.clone((StagedImage) item);
				image.setErrors(myErrors);
				response.getImages().add(image);
				break;
			case StagedForm.TYPE_NAME:
				ContentPackageForm form = new ContentPackageForm();
				form.clone((StagedForm) item);
				form.setErrors(myErrors);
				response.getForms().add(form);
				break;
			case StagedFolder.TYPE_NAME:
				ContentPackageFolder folder = new ContentPackageFolder();
				folder.clone((StagedFolder) item);
				folder.setErrors(myErrors);
				response.getFolders().add(folder);
				break;
			case StagedChannel.TYPE_NAME:
				ContentPackageChannel channel = new ContentPackageChannel();
				channel.clone((StagedChannel) item);
				channel.setErrors(myErrors);
				response.getChannels().add(channel);
				break;
			case StagedNode.TYPE_NAME:
				ContentPackageNode node = new ContentPackageNode();
				node.clone((StagedNode) item);
				node.setErrors(myErrors);
				response.getNodes().add(node);
				break;
			}
		}));
		return response;
	}

	public List<ContentPackageNode> getNodes() {
		return nodes;
	}
	public void setNodes(List<ContentPackageNode> nodes) {
		this.nodes = nodes;
	}
	public List<ContentPackageChannel> getChannels() {
		return channels;
	}
	public void setChannels(List<ContentPackageChannel> channels) {
		this.channels = channels;
	}
	public List<ContentPackageFolder> getFolders() {
		return folders;
	}
	public void setFolders(List<ContentPackageFolder> folders) {
		this.folders = folders;
	}
	public List<ContentPackageFile> getFiles() {
		return files;
	}
	public void setFiles(List<ContentPackageFile> files) {
		this.files = files;
	}
	public List<ContentPackageImage> getImages() {
		return images;
	}
	public void setImages(List<ContentPackageImage> images) {
		this.images = images;
	}
	public List<ContentPackagePage> getPages() {
		return pages;
	}
	public void setPages(List<ContentPackagePage> pages) {
		this.pages = pages;
	}
	public List<ContentPackageForm> getForms() {
		return forms;
	}
	public void setForms(List<ContentPackageForm> forms) {
		this.forms = forms;
	}
}
