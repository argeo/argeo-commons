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
