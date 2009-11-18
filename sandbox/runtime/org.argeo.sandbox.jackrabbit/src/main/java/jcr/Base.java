package jcr;

import java.io.IOException;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.TransientRepository;

public abstract class Base {
    public Repository getRepository() throws IOException {
        return new TransientRepository();
    }

    public Session getReadonlySession(Repository repository) throws RepositoryException {
        return repository.login();
    }

    public Session getSession(Repository repository) throws RepositoryException {
        return repository.login(new SimpleCredentials("username", "password".toCharArray()));
    }

    public void logout(Session session) {
        session.logout();
    }
}
