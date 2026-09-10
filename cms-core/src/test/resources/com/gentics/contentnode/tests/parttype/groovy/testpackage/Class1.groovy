package testpackage

import com.gentics.api.Loader

class Class1 {
	static String simple() {
		"Simple from testpackage.Class1"
	}

	static String pageName(int pageId) {
		return Loader.page(pageId).name
	}
}
