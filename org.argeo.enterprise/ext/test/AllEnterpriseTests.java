import org.argeo.fs.FsUtilsTest;
import org.argeo.osgi.useradmin.LdifParserTest;
import org.argeo.osgi.useradmin.UserAdminConfTest;
import org.argeo.util.test.Tester;

class AllEnterpriseTests {

	public static void main(String[] args) throws Exception {
		Tester tester = new Tester();

		// FS
		tester.execute(FsUtilsTest.class.getName());

		// User admin
		tester.execute(LdifParserTest.class.getName());
		//tester.execute(LdifUserAdminTest.class.getName());
		tester.execute(UserAdminConfTest.class.getName());
	}

}
