package org.worldvision.sierraleone;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.motechproject.config.service.ConfigurationService;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.worldvision.sierraleone.WorldVisionSettings.SYMBOLIC_NAME;

public class WorldVisionSettingsTest {
    private static final String FILE_NAME = "wv-settings.properties";
    private static final String MODULE_NAME = "sierra-leone";

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private Resource resource;

    private WorldVisionSettings settings;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream("language=English".getBytes()));
        when(resource.getFilename()).thenReturn(FILE_NAME);

        settings = new WorldVisionSettings();
        settings.setModuleName(MODULE_NAME);
    }

    @Test
    public void shouldReturnLanguage() throws Exception {
        Properties p = new Properties();
        p.setProperty("language", "English");

        when(configurationService.getModuleProperties(eq(MODULE_NAME), eq(FILE_NAME), any(Properties.class))).thenReturn(p);

        settings.setConfigurationService(configurationService);
        settings.setConfigFiles(asList(resource));

        assertEquals("English", settings.getLanguage());
    }

    @Test
    public void shouldReturnCorrectSymbolicName() throws Exception {
        assertEquals(SYMBOLIC_NAME, settings.getSymbolicName());
    }
}
