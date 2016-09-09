/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.cms.internal.backup;

import org.apache.commons.vfs2.FileObject;
import org.argeo.cms.CmsException;

/** Backups an OpenLDAP server using slapcat */
public class OpenLdapBackup extends OsCallBackup {
	private String slapcatLocation = "/usr/sbin/slapcat";
	private String slapdConfLocation = "/etc/openldap/slapd.conf";
	private String baseDn;

	public OpenLdapBackup() {
		super();
	}

	public OpenLdapBackup(String baseDn) {
		super();
		this.baseDn = baseDn;
	}

	@Override
	public void writeBackup(FileObject targetFo) {
		if (baseDn == null)
			throw new CmsException("Base DN must be set");

		if (getCommand() == null)
			setCommand(slapcatLocation
					+ " -f ${slapdConfLocation} -b '${baseDn}'");
		getVariables().put("slapdConfLocation", slapdConfLocation);
		getVariables().put("baseDn", baseDn);

		super.writeBackup(targetFo);
	}

	public void setSlapcatLocation(String slapcatLocation) {
		this.slapcatLocation = slapcatLocation;
	}

	public void setSlapdConfLocation(String slapdConfLocation) {
		this.slapdConfLocation = slapdConfLocation;
	}

	public void setBaseDn(String baseDn) {
		this.baseDn = baseDn;
	}

}
