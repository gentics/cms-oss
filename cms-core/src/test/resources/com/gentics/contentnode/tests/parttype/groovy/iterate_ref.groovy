def retVal;

cms.page.each { key, value -> 
	if (key == "folder") {
		// When resolving e.g. "folder" by iterating the map, we should
		// get the String representation of the folder, not the folder instance itself
		retVal = value.getClass().getSimpleName()
	}
}

return retVal