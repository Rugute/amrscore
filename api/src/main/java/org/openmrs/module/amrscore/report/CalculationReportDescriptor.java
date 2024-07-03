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

package org.openmrs.module.amrscore.report;

import org.openmrs.calculation.patient.PatientCalculation;

/**
 * Describes a calculation based cohort report
 */
public class CalculationReportDescriptor extends CohortReportDescriptor {
	
	protected Class<? extends PatientCalculation> calculation;
	
	/**
	 * Gets the calculation class
	 * 
	 * @return the class
	 */
	public Class<? extends PatientCalculation> getCalculation() {
		return calculation;
	}
	
	/**
	 * Sets the calculation class
	 * 
	 * @param calculation the class
	 */
	public void setCalculation(Class<? extends PatientCalculation> calculation) {
		this.calculation = calculation;
	}
}
