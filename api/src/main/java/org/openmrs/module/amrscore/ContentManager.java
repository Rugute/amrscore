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

package org.openmrs.module.amrscore;

/**
 * Interface for all content managers
 */
public interface ContentManager {
	
	/**
	 * Gets the priority value to determine refresh order
	 * 
	 * @return the priority value
	 */
	int getPriority();
	
	/**
	 * Refreshes the manager after a content refresh
	 */
	void refresh();
}
