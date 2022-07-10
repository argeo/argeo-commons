package org.argeo.cms.swt.rap.cli;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.api.cli.CommandsCli;
import org.argeo.api.cli.DescribedCommand;
import org.argeo.api.cms.CmsApp;
import org.argeo.api.cms.CmsContext;
import org.argeo.cms.runtime.StaticCms;
import org.argeo.cms.swt.app.CmsUserApp;
import org.argeo.cms.web.CmsWebApp;
import org.argeo.util.register.Component;
import org.argeo.util.register.ComponentRegister;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;

public class CmsRapCli extends CommandsCli {

	public CmsRapCli(String commandName) {
		super(commandName);
		addCommand("user", new Launch());
	}

	@Override
	public String getDescription() {
		return "Argeo CMS utilities.";
	}

	public static void main(String[] args) {
		mainImpl(new CmsRapCli("web"), args);
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
						CmsUserApp cmsApp = new CmsUserApp();
						Component<CmsUserApp> cmsAppC = new Component.Builder<>(cmsApp) //
								.addType(CmsApp.class) //
								.addType(CmsUserApp.class) //
								.addDependency(register.getSingleton(CmsContext.class), cmsApp::setCmsContext, null) //
								.addDependency(register.getSingleton(ProvidedRepository.class),
										cmsApp::setContentRepository, null) //
								.build(register);

						CmsWebApp cmsWebApp = new CmsWebApp();
						Component<CmsWebApp> cmsWebAppC = new Component.Builder<>(cmsWebApp) //
								.addType(ApplicationConfiguration.class) //
								.addDependency(cmsAppC.getType(CmsApp.class), cmsWebApp::setCmsApp, null) //
								.build(register);

						RwtRunner rwtRunner = new RwtRunner();
						Component<RwtRunner> rwtRunnerC = new Component.Builder<>(rwtRunner) //
								.addActivation(rwtRunner::init) //
								.addDeactivation(rwtRunner::destroy) //
								.addType(RwtRunner.class) //
								.addDependency(cmsWebAppC.getType(ApplicationConfiguration.class),
										rwtRunner::setApplicationConfiguration, null) //
								.build(register);
					}
				}

			};
			Runtime.getRuntime().addShutdownHook(new Thread(() -> staticCms.stop(), "Static CMS Shutdown"));
			staticCms.start();

			long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
			System.out.println("Static CMS available in " + jvmUptime + " ms.");

			if (ui) {
				try {
					// open browser in app mode
					Thread.sleep(2000);// wait for RWT to be ready
					Runtime.getRuntime().exec("google-chrome --app=http://localhost:"
							+ staticCms.getComponentRegister().getObject(RwtRunner.class).getEffectivePort() + "/data");
				} catch (InterruptedException | IOException e) {
					e.printStackTrace();
				}
			}

			staticCms.waitForStop();

			return null;
		}

		@Override
		public String getDescription() {
			return "Launch a static CMS.";
		}

	}
}
