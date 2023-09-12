package com.gentics.contentnode.image;

public interface GisImageInitiator<T> {

	public T getInitiatorForeignKey();

	public boolean initiateIfNotFound();

	public default void setImageData(String webrootPath, String transform) {}
}
