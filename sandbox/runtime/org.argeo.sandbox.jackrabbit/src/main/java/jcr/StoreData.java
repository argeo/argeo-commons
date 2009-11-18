package jcr;

import javax.jcr.*;
import java.io.IOException;

public class StoreData extends Base {
    public StoreData() {
    }

    public static void main(String[] args) throws IOException, RepositoryException {
        StoreData sd=new StoreData();
        sd.run();
    }

    private void run() throws IOException, RepositoryException {
        Repository repository=getRepository();

        Session session=getSession(repository);

        Node rootnode=session.getRootNode();

        Node childnode=null;
        try {
            childnode=rootnode.getNode("foo");
        } catch(PathNotFoundException pnfe) {
            childnode=rootnode.addNode("foo");
            childnode.setProperty("bar", "this is some data");
            session.save();
        }

        logout(session);
    }
}
