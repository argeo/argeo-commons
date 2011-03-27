package org.argeo.jcr.spring;

import org.argeo.jcr.ThreadBoundJcrSessionFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class ThreadBoundSession extends ThreadBoundJcrSessionFactory implements FactoryBean, InitializingBean, DisposableBean{
	public void afterPropertiesSet() throws Exception {
		init();
	}

	public void destroy() throws Exception {
		dispose();
	}

}
