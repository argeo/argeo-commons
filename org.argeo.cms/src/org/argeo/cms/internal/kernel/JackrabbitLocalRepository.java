package org.argeo.cms.internal.kernel;

import java.util.Map;
import java.util.TreeMap;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.argeo.node.NodeConstants;

class JackrabbitLocalRepository extends LocalRepository {
	private final static Log log = LogFactory.getLog(JackrabbitLocalRepository.class);
	final String SECURITY_WORKSPACE = "security";

	private Map<String, CmsWorkspaceIndexer> workspaceMonitors = new TreeMap<>();

	public JackrabbitLocalRepository(RepositoryImpl repository, String cn) {
		super(repository, cn);
//		Session session = KernelUtils.openAdminSession(repository);
//		try {
//			if (NodeConstants.NODE.equals(cn))
//				for (String workspaceName : session.getWorkspace().getAccessibleWorkspaceNames()) {
//					addMonitor(workspaceName);
//				}
//		} catch (RepositoryException e) {
//			throw new IllegalStateException(e);
//		} finally {
//			JcrUtils.logoutQuietly(session);
//		}
	}

	protected RepositoryImpl getJackrabbitrepository(String workspaceName) {
		return (RepositoryImpl) getRepository(workspaceName);
	}

	@Override
	protected synchronized void processNewSession(Session session, String workspaceName) {
//		String realWorkspaceName = session.getWorkspace().getName();
//		addMonitor(realWorkspaceName);
	}

	private void addMonitor(String realWorkspaceName) {
		if (realWorkspaceName.equals(SECURITY_WORKSPACE))
			return;
		if (!NodeConstants.NODE.equals(getCn()))
			return;

		if (!workspaceMonitors.containsKey(realWorkspaceName)) {
			try {
				CmsWorkspaceIndexer workspaceMonitor = new CmsWorkspaceIndexer(
						getJackrabbitrepository(realWorkspaceName), getCn(), realWorkspaceName);
				workspaceMonitors.put(realWorkspaceName, workspaceMonitor);
				workspaceMonitor.init();
				if (log.isDebugEnabled())
					log.debug("Registered " + workspaceMonitor);
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void destroy() {
		for (String workspaceName : workspaceMonitors.keySet()) {
			workspaceMonitors.get(workspaceName).destroy();
		}
	}

}
