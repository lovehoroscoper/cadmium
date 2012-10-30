/**
 *    Copyright 2012 meltmedia
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.meltmedia.cadmium.email.jersey;

import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import static org.mockito.Mockito.*;
import org.junit.Test;

import com.meltmedia.cadmium.core.ContentService;
import com.meltmedia.cadmium.email.config.EmailComponentConfiguration;
import com.meltmedia.cadmium.email.config.EmailComponentConfiguration.Field;

public class TestEmailFormValidator  extends EmailFormValidator {
	
	
	
	@Test
	public void testEmailForm() {
		ContentService service = mock(ContentService.class);
		when(service.getContentRoot()).thenReturn("target/classes");
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameter("path")).thenReturn("index.html");
		EmailComponentConfiguration emailConfig = new EmailComponentConfiguration();
		emailConfig.setFromAddress("test.haha@domain.com");
		emailConfig.setFromName("From");
		emailConfig.setToAddress("toAddress@domain.com");
		emailConfig.setToName("To");
		emailConfig.setSubject("subject");
		emailConfig.setFields(new HashSet<Field>());
		
		// Positive Test
		try {
			validate(request,emailConfig,service);
		} catch (ValidationException e) {
			
		}
		
		// Negative Tests
		emailConfig.setFromAddress("Invalid Address");
		try {
			validate(request,emailConfig,service);
		} catch (ValidationException e) {
			Assert.assertEquals("Expected Fail", 1, e.getErrors().length);
			Assert.assertEquals("Message check", "fromAddress is an invalid email address.", e.getErrors()[0].getMessage());
		}
		emailConfig.setToName("");
		try {
			validate(request,emailConfig,service);
		} catch (ValidationException e) {
			Assert.assertEquals("Expected Fail", 2, e.getErrors().length);
		}
	
	}
	
	
	
}
