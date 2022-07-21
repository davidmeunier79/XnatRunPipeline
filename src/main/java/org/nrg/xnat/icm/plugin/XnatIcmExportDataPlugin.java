
package org.nrg.xnat.icm.plugin;

import org.springframework.context.annotation.ComponentScan;
import org.nrg.framework.annotations.XnatPlugin;

@XnatPlugin(value = "xnatICMExportDataPlugin", name = "XNAT 1.7 ICM Export Data Plugin", description = "Export data from a project", log4jPropertiesFile = "xnatIcmExportDataPlugin-log4j.properties")
@ComponentScan({ "org.nrg.xnat.icm.rest" })
public class XnatIcmExportDataPlugin
{
}