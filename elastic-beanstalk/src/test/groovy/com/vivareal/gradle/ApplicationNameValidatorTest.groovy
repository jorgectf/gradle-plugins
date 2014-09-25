package com.vivareal.gradle;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

class ApplicationNameValidatorTest {
    
    def instance
    
    @Before
    void setup() {
	instance = new ApplicationNameValidator()
    }

    @Test
    public void invalidNames() {
	assertFalse(instance.validate(null))
	assertFalse(instance.validate(""))
	assertFalse(instance.validate("    "))
	assertFalse(instance.validate("My Application"))
	assertFalse(instance.validate("/teste"))
	assertFalse(instance.validate("askdjalksdjksjdhqiudehoiaxnmanciuherqdqwexdqdascmxxmcnlaijsdfqoweuqoisjalksalskdalkcmiqwejqdwmascmxas"))
	
    }
    
    @Test
    public void validNames() {
	assertTrue(instance.validate("teste"))
	assertTrue(instance.validate("askdjalksdjksjdhqiudehoiaxnmanciuherqdqwexdqdascmxxmcnlajsdfqoweuqoisjalksalskdalkcmiqwejqdwmascmxas"))
	
    }

}
