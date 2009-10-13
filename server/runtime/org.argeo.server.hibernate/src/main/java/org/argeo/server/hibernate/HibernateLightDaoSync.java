package org.argeo.server.hibernate;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.server.dao.LightDaoSupport;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class HibernateLightDaoSync {
	private final static Log log = LogFactory
			.getLog(HibernateLightDaoSync.class);

	private String externalSuffix = LightDaoInterceptor.DEFAULT_EXTERNAL_SUFFIX;

	private SessionFactory sessionFactory;

	private LightDaoSupport lightDaoSupport;

	private List<Class<?>> classes = new ArrayList<Class<?>>();

	public void sync() {
		List<Class<?>> lst;
		if (classes.size() > 0)
			lst = classes;
		else
			lst = lightDaoSupport.getSupportedClasses();

		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();
		try {
			for (Class<?> clss : lst) {
				String entityName = clss.getSimpleName() + externalSuffix;
				int count = 0;
				for (Object obj : lightDaoSupport.list(clss, null)) {
					session.save(entityName, obj);
					count++;
				}
				if (log.isDebugEnabled())
					log.debug("Synchronized " + count + "\tentities '"
							+ entityName + "'");
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			session.getTransaction().rollback();
		}
	}

	public void setClasses(List<Class<?>> classes) {
		this.classes = classes;
	}

	public void setExternalSuffix(String externalSuffix) {
		this.externalSuffix = externalSuffix;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setLightDaoSupport(LightDaoSupport lightDaoSupport) {
		this.lightDaoSupport = lightDaoSupport;
	}
}
