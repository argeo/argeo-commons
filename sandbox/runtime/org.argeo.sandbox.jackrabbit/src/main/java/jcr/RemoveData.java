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
