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

import javax.jcr.*;
import org.apache.jackrabbit.core.TransientRepository;
import java.io.FileInputStream;

/**
 * Third Jackrabbit example application. Imports an example XML file
 * and outputs the contents of the entire workspace.
 */
public class ThirdHop {

    /** Runs the ThirdHop example. */
    public static void main(String[] args) throws Exception {
        // Set up a Jackrabbit repository with the specified
        // configuration file and repository directory
        Repository repository = new TransientRepository();

        // Login to the default workspace as a dummy user
        Session session = repository.login(
            new SimpleCredentials("username", "password".toCharArray()));
        try {
            // Use the root node as a starting point
            Node root = session.getRootNode();

            // Import the XML file unless already imported
            if (!root.hasNode("importxml")) {
                System.out.print("Importing xml... ");
                // Create an unstructured node under which to import the XML
                Node node = root.addNode("importxml", "nt:unstructured");
                // Import the file "test.xml" under the created node
                FileInputStream xml = new FileInputStream("test.xml");
                session.importXML(
                    "/importxml", xml, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                xml.close();
                // Save the changes to the repository
                session.save();
                System.out.println("done.");
            }

            dump(root);
        } finally {
            session.logout();
        }
    }

    /** Recursively outputs the contents of the given node. */
    private static void dump(Node node) throws RepositoryException {
        // First output the node path
        System.out.println(node.getPath());
        // Skip the virtual (and large!) jcr:system subtree
        if (node.getName().equals("jcr:system")) {
            return;
        }

        // Then output the properties
        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            if (property.getDefinition().isMultiple()) {
                // A multi-valued property, print all values
                Value[] values = property.getValues();
                for (int i = 0; i < values.length; i++) {
                    System.out.println(
                        property.getPath() + " = " + values[i].getString());
                }
            } else {
                // A single-valued property
                System.out.println(
                    property.getPath() + " = " + property.getString());
            }
        }

        // Finally output all the child nodes recursively
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            dump(nodes.nextNode());
        }
    }

}
