package org.worldvision.sierraleone;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.motechproject.server.config.service.PlatformSettingsService;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.worldvision.sierraleone.WorldVisionSettings.SYMBOLIC_NAME;

public class WorldVisionSettingsTest {
    private static final String FILE_NAME = "wv-settings.properties";

    @Mock
    private PlatformSettingsService platformSettingsService;

    @Mock
    private Resource resource;

    private WorldVisionSettings settings;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream("language=English".getBytes()));
        when(resource.getFilename()).thenReturn(FILE_NAME);

        settings = new WorldVisionSettings();
        settings.setModuleName("sierra-leone");
    }

    @Test
    public void shouldReturnLanguage() throws Exception {
        settings.setPlatformSettingsService(platformSettingsService);
        settings.setConfigFiles(asList(resource));

        Properties p = new Properties();
        p.setProperty("language", "English");

        when(platformSettingsService.getBundleProperties(SYMBOLIC_NAME, FILE_NAME)).thenReturn(p);

        assertEquals("English", settings.getLanguage());
    }

    @Test
    public void shouldReturnCorrectSymbolicName() throws Exception {
        assertEquals(SYMBOLIC_NAME, settings.getSymbolicName());
    }
}
