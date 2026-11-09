package com.gentics.contentnode.rest.model.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

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
import com.gentics.contentnode.staging.StagingErrorKind;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentPackageFolderResponse extends GenericJobStatusResponse {

	private static final long serialVersionUID = -2151578063422454979L;

	private List<ContentPackageNode> nodes = new ArrayList<>();
	private List<ContentPackageChannel> channels = new ArrayList<>();

	private List<ContentPackageFolder> folders = new ArrayList<>();
	private List<ContentPackageFile> files = new ArrayList<>();
	private List<ContentPackageImage> images = new ArrayList<>();
	private Map<String, List<ContentPackagePage>> pages = new HashMap<>();
	private Map<String, List<ContentPackageForm>> forms = new HashMap<>();

	@XmlTransient
	@JsonIgnore
	public static final ContentPackageFolderResponse fromContent(List<StagingBase> items, List<StagingError> errors, StagingErrorKind filterByError) {
		ContentPackageFolderResponse response = new ContentPackageFolderResponse();

		Optional.ofNullable(items).ifPresent(i -> i.stream().forEach(item -> {
			List<StagingError> myStagedErrors = Optional.ofNullable(errors).orElse(Collections.emptyList()).stream()
					.filter(e -> e.getGlobalId().equals(item.getId()))
					.collect(Collectors.toList());

			if (filterByError != null) {
				myStagedErrors = myStagedErrors.stream().filter(e -> e.getKind().equals(filterByError)).collect(Collectors.toList());
				if (myStagedErrors.isEmpty()) {
					return;
				}
			}

			List<StagedError> myErrors = myStagedErrors.stream().map(s -> new StagedError(s.getError(), s.getKind())).collect(Collectors.toList());

			switch (item.getTypeName()) {
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
			case StagedPage.TYPE_NAME:
				ContentPackagePage page = new ContentPackagePage();
				page.clone((StagedPage) item);
				page.setErrors(myErrors);
				response.getPages().computeIfAbsent(page.getId(), k -> new ArrayList<>()).add(page);
				break;
			case StagedForm.TYPE_NAME:
				ContentPackageForm form = new ContentPackageForm();
				form.clone((StagedForm) item);
				form.setErrors(myErrors);
				response.getForms().computeIfAbsent(form.getId(), k -> new ArrayList<>()).add(form);
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
	public Map<String, List<ContentPackagePage>> getPages() {
		return pages;
	}
	public void setPages(Map<String, List<ContentPackagePage>> pages) {
		this.pages = pages;
	}
	public Map<String, List<ContentPackageForm>> getForms() {
		return forms;
	}
	public void setForms(Map<String, List<ContentPackageForm>> forms) {
		this.forms = forms;
	}
}
