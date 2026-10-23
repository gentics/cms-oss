package com.gentics.contentnode.rest.model.response.devtools;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.devtools.PackageFile;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Response containing a list of files/directories in the files-internal storage of a devtool package
 */
@XmlRootElement
public class PackageFileListResponse extends AbstractListResponse<PackageFile> {
}
