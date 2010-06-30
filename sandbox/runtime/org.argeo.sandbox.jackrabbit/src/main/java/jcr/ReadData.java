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

public class ReadData extends Base {
    public ReadData() {
    }

    public static void main(String[] args) throws IOException, RepositoryException {
        ReadData readdata = new ReadData();
        readdata.run();
    }

    private void run() throws IOException, RepositoryException {
        Repository repository = getRepository();

        Session session = getSession(repository);

        Node rootnode = session.getRootNode();

        Node childnode = null;
        try {
            childnode = rootnode.getNode("foo");
            try {
                Property prop = childnode.getProperty("bar");
                System.out.println("value of /foo@bar: " + prop.getString());
            } catch (PathNotFoundException pnfe) {
                System.out.println("/foo@bar not found.");
            }
        } catch (PathNotFoundException pnfe) {
            System.out.println("/foo not found.");
        }

        logout(session);
    }
}
