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

package org.openmrs.module.amrscore.metadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.amrscore.ContentManager;
import org.openmrs.module.metadatadeploy.api.MetadataDeployService;
import org.openmrs.module.metadatadeploy.bundle.MetadataBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Metadata package manager
 */
@Component
public class MetadataManager implements ContentManager {
	
	protected static final Log log = LogFactory.getLog(MetadataManager.class);
	
	protected static final String SYSTEM_PROPERTY_SKIP_REFRESH = "skipMetadataRefresh";
	
	@Autowired
	private MetadataDeployService deployService;
	
	/**
	 * @see ContentManager#getPriority()
	 */
	@Override
	public int getPriority() {
		return 10; // Second (only after the requirement manager) because others will use metadata loaded by it
	}
	
	/**
	 * @see ContentManager#refresh()
	 */
	@Override
	public synchronized void refresh() {
		// Allow skipping of metadata refresh - useful for developers
		if (Boolean.parseBoolean(System.getProperty(SYSTEM_PROPERTY_SKIP_REFRESH))) {
			log.warn("Skipping metadata refresh");
			return;
		}
		// Install bundle components
		deployService.installBundles(Context.getRegisteredComponents(MetadataBundle.class));
	}
}
