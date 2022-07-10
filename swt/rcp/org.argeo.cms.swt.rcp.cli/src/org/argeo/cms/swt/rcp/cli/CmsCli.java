package org.argeo.cms.swt.rcp.cli;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.api.cli.CommandsCli;
import org.argeo.api.cli.DescribedCommand;
import org.argeo.api.cms.CmsApp;
import org.argeo.cms.runtime.StaticCms;
import org.argeo.cms.swt.app.CmsUserApp;
import org.argeo.cms.ui.rcp.CmsRcpDisplayFactory;
import org.argeo.util.register.Component;
import org.argeo.util.register.ComponentRegister;

public class CmsCli extends CommandsCli {

	public CmsCli(String commandName) {
		super(commandName);
		addCommand("static", new Launch());
	}

	@Override
	public String getDescription() {
		return "Argeo CMS utilities.";
	}

	static class Launch implements DescribedCommand<String> {
		private Option dataOption;
		private Option uiOption;

		@Override
		public Options getOptions() {
			Options options = new Options();
			dataOption = Option.builder().longOpt("data").hasArg().required()
					.desc("path to the writable data area (mandatory)").build();
			uiOption = Option.builder().longOpt("ui").desc("open a user interface").build();
			options.addOption(dataOption);
			options.addOption(uiOption);
			return options;
		}

		@Override
		public String apply(List<String> args) {
			CommandLine cl = toCommandLine(args);
			String dataPath = cl.getOptionValue(dataOption);
			boolean ui = cl.hasOption(uiOption);

			Path instancePath = Paths.get(dataPath);
			System.setProperty("osgi.instance.area", instancePath.toUri().toString());

			StaticCms staticCms = new StaticCms() {
				@Override
				protected void addComponents(ComponentRegister register) {
					if (ui) {
						Component<? extends ProvidedRepository> contentRepositoryC = register
								.find(ProvidedRepository.class, null).first();
						CmsUserApp cmsApp = new CmsUserApp();
						Component<CmsUserApp> cmsAppC = new Component.Builder<>(cmsApp) //
								.addType(CmsApp.class) //
								.addType(CmsUserApp.class) //
								.addDependency(contentRepositoryC.getType(ProvidedRepository.class),
										cmsApp::setContentRepository, null) //
								.build(register);

						CmsRcpDisplayFactory displayFactory = new CmsRcpDisplayFactory();
						Component<CmsRcpDisplayFactory> displayFactoryC = new Component.Builder<>(displayFactory) //
								.addActivation(displayFactory::init) //
								.addDeactivation(displayFactory::destroy) //
								.build(register);

					}
				}

				@Override
				protected void postActivation(ComponentRegister register) {
					if (ui) {
						Component<? extends CmsUserApp> cmsAppC = register.find(CmsUserApp.class, null).first();
						CmsRcpDisplayFactory.openCmsApp(null, cmsAppC.get(), "data", (e) -> {
							// asynchronous in order to avoid deadlock in UI thread
							ForkJoinPool.commonPool().execute(() -> stop());
						});
					}
				}

			};
			Runtime.getRuntime().addShutdownHook(new Thread(() -> staticCms.stop(), "Static CMS Shutdown"));
			staticCms.start();

			long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
			System.out.println("Static CMS available in " + jvmUptime + " ms.");

			staticCms.waitForStop();

			return null;
		}

		@Override
		public String getDescription() {
			return "Launch a static CMS.";
		}

	}
}
