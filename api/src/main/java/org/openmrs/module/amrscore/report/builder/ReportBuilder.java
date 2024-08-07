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

package org.openmrs.module.amrscore.report.builder;

import org.openmrs.module.amrscore.report.ReportDescriptor;
import org.openmrs.module.reporting.report.definition.ReportDefinition;

/**
 * Interface for a class that can build a report
 */
public interface ReportBuilder {
	
	/**
	 * Builds a report definition for the given report
	 * 
	 * @param report the report
	 * @return the report definition
	 */
	ReportDefinition build(ReportDescriptor report);
}
