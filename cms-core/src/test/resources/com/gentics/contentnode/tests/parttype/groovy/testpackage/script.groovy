package testpackage

import groovy.transform.Field

@Field def String param = "default"

return "This is the script from testpackage called with param [$param]"
