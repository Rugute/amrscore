/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.amrscore.converter;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.amrscore.ConceptReference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts a String to a ConceptReference
 */
@Component
public class StringToConceptReferenceConverter implements Converter<String, ConceptReference> {
	
	/**
	 * @see Converter#convert(Object)
	 */
	@Override
	public ConceptReference convert(String s) {
		return StringUtils.isEmpty(s) ? null : new ConceptReference(s);
	}
}
