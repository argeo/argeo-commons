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

import javax.jcr.*;
import java.io.IOException;

public class RemoveData extends Base {
    public RemoveData() {
    }

    public static void main(String[] args) throws IOException, RepositoryException {
        RemoveData sd = new RemoveData();
        sd.run();
    }

    private void run() throws IOException, RepositoryException {
        Repository repository = getRepository();

        Session session = getSession(repository);

        Node rootnode = session.getRootNode();

        Node childnode = null;
        try {
            childnode = rootnode.getNode("foo");
            childnode.remove();
            session.save();
        } catch (PathNotFoundException pnfe) {
            System.out.println("/foo not found; not removed.");
        }

        logout(session);
    }
}
