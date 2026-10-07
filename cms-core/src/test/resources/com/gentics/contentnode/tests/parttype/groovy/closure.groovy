def tagnames = []

cms.page.tags.each {
	tagname, tag -> tagnames.add(tagname)
}

return tagnames