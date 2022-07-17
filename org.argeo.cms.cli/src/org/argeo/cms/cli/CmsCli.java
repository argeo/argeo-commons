package org.argeo.cms.cli;

import org.argeo.api.cli.CommandsCli;

public class CmsCli extends CommandsCli {

	public CmsCli(String commandName) {
		super(commandName);
		addCommand("launch", new StaticCmsLaunch());
	}

	@Override
	public String getDescription() {
		return "Static CMS utilities.";
	}

	public static void main(String[] args) {
		mainImpl(new CmsCli("cms"), args);
	}

}
