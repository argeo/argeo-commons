/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
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
