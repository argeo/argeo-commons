package org.argeo.cms.cli;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.argeo.api.acr.CrAttributeType;
import org.argeo.api.cli.CLine;
import org.argeo.api.cli.DescribedCommand;
import org.argeo.api.cli.ValuedOpt;
import org.argeo.cms.runtime.StaticCms;

public class StaticCmsLaunch extends DescribedCommand<String> {

	enum Opt implements ValuedOpt {
		data, //
		;

		@Override
		public CrAttributeType type() {
			return switch (this) {
			case data -> CrAttributeType.STRING;
			default -> CrAttributeType.BOOLEAN;
			};
		}

	}

//	@Override
//	public Options getOptions() {
//		Options options = new Options();
//		dataOption = Option.builder().longOpt("data").hasArg().required()
//				.desc("path to the writable data area (mandatory)").build();
//		options.addOption(dataOption);
//		return options;
//	}

	@Override
	public String execute(CLine cLine) {
		String dataPath = cLine.get(Opt.data, String.class).orElseThrow();

		Path instancePath = Paths.get(dataPath);
		System.setProperty("osgi.instance.area", instancePath.toUri().toString());
		System.setProperty("argeo.http.port", "0");

		StaticCms staticCms = new StaticCms();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> staticCms.stop(), "Static CMS Shutdown"));
		staticCms.start();

		long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
		System.out.println("Static CMS available in " + jvmUptime + " ms.");

		staticCms.waitForStop();

		return null;
	}

//	@Override
//	public String getDescription() {
//		return "Launch a static CMS";
//	}

}
