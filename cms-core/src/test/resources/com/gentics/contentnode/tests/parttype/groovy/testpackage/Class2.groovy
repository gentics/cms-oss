package testpackage

import testpackage.Class1

class Class2 {
	static String simple() {
		return "testpackage.Class1 returning: " + Class1.simple()
	}
}
