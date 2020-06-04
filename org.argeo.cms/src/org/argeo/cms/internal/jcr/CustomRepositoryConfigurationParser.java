package org.argeo.cms.internal.jcr;

import java.util.Properties;

import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.apache.jackrabbit.core.config.WorkspaceSecurityConfig;
import org.apache.jackrabbit.core.util.db.ConnectionFactory;
import org.w3c.dom.Element;

/**
 * A {@link RepositoryConfigurationParser} providing more flexibility with
 * classloaders.
 */
@SuppressWarnings("restriction")
class CustomRepositoryConfigurationParser extends RepositoryConfigurationParser {
	private ClassLoader accessControlProviderClassLoader = null;

	public CustomRepositoryConfigurationParser(Properties variables) {
		super(variables);
	}

	public CustomRepositoryConfigurationParser(Properties variables, ConnectionFactory connectionFactory) {
		super(variables, connectionFactory);
	}

	@Override
	protected RepositoryConfigurationParser createSubParser(Properties variables) {
		Properties props = new Properties(getVariables());
		props.putAll(variables);
		CustomRepositoryConfigurationParser subParser = new CustomRepositoryConfigurationParser(props,
				connectionFactory);
		subParser.setAccessControlProviderClassLoader(accessControlProviderClassLoader);
		return subParser;
	}

	@Override
	public WorkspaceSecurityConfig parseWorkspaceSecurityConfig(Element parent) throws ConfigurationException {
		WorkspaceSecurityConfig workspaceSecurityConfig = super.parseWorkspaceSecurityConfig(parent);
		workspaceSecurityConfig.getAccessControlProviderConfig().setClassLoader(accessControlProviderClassLoader);
		return workspaceSecurityConfig;
	}

	public void setAccessControlProviderClassLoader(ClassLoader accessControlProviderClassLoader) {
		this.accessControlProviderClassLoader = accessControlProviderClassLoader;
	}

}
