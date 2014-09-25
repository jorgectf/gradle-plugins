package com.vivareal.gradle

class ApplicationNameValidator {
    
    def validate(name) {
	if (name == null) {
	    return false
	}
	name = name.trim()
	if (name.isEmpty() || name.length() > 100) {
	    return false
	}
	if (name.indexOf(' ') >= 0) {
	    return false
	}
	if (name.indexOf('/') >= 0) {
	    return false
	}
	true
    }

}
