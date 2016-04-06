package org.argeo.cms;

import java.util.List;

import javax.jcr.Session;

public interface CmsExtension {
	public List<String> getDataModels();

	public List<String> getRoles();

	public void onInit(Session adminSession);

	public void onStart(Session adminSession);

	public void onShutdown(Session adminSession);

	public void onDestroy(Session adminSession);
}
