package org.argeo.maintenance.backup.vfs;

import org.apache.commons.vfs2.FileObject;
import org.argeo.maintenance.MaintenanceException;

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
			throw new MaintenanceException("Base DN must be set");

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
