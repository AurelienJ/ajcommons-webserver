package org.ajdeveloppement.webserver.services.webapi.helpers.tests;


import static org.junit.Assert.fail;

import java.util.Date;

import org.ajdeveloppement.webserver.services.webapi.helpers.JsonHelper;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

class JsonHelperTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void testToJson() {
		String returnValue;
		try {
			returnValue = JsonHelper.toJson(new TestObject(new Date(1505512800000l), null, 0, false));
			
			Assert.assertEquals("{\"uneDate\":\"2017-09-16T00:00:00.000Z\",\"uneChaine\":null,\"unNombre\":0,\"unBoolean\":false}", returnValue);
		} catch (JsonProcessingException e) {
			fail(e.toString());
		}
		
	}

}
