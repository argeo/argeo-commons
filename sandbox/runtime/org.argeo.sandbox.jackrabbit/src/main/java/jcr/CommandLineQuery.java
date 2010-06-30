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
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.QueryManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CommandLineQuery extends Base {
    public CommandLineQuery() {
    }

    public static void main(String[] args) throws IOException, RepositoryException {
        CommandLineQuery clq=new CommandLineQuery();
        clq.run();
    }

    private void run() throws IOException, RepositoryException {
        Repository repository=getRepository();
        Session session=getReadonlySession(repository);
        Workspace workspace=session.getWorkspace();
        QueryManager qm=workspace.getQueryManager();
        BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
        for(;;) {
            System.out.print("JCRQL> ");
            String queryString=reader.readLine();
            if(queryString.equals("quit")) {
                break;
            }
            if(queryString.length()==0 || queryString.startsWith("#")) {
                continue;
            }

            int resultCounter=0;
            try {
                Query query=qm.createQuery(queryString, Query.XPATH);
                QueryResult queryResult=query.execute();
                NodeIterator nodeIterator=queryResult.getNodes();
                while(nodeIterator.hasNext()) {
                    Node node=nodeIterator.nextNode();
                    dump(node);
                    resultCounter++;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }

            System.out.println("result count: "+resultCounter);
        }
        logout(session);
    }

    private void dump(Node node) throws RepositoryException {
        StringBuilder sb=new StringBuilder();
        String sep=",";
        sb.append(node.getName());
        sb.append("["+node.getPath());
        PropertyIterator propIterator=node.getProperties();
        while(propIterator.hasNext()) {
            Property prop=propIterator.nextProperty();
            sb.append(sep);
            sb.append("@"+prop.getName()+"=\""+prop.getString()+"\"");
        }
        sb.append("]");
        System.out.println(sb.toString());
    }
}
