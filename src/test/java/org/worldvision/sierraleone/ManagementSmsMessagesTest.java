package org.worldvision.sierraleone;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.motechproject.cmslite.api.model.Content;
import org.motechproject.cmslite.api.model.StringContent;
import org.motechproject.cmslite.api.service.CMSLiteService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ManagementSmsMessagesTest {
    private static final String FILE_NAME = "/sms-messages.json";

    @Mock
    private CMSLiteService cmsLiteService;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private WorldVisionSettings settings;

    @Mock
    private Resource resource;

    private ManagementSmsMessages mgr;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        mgr = new ManagementSmsMessages(resourceLoader, settings);

        when(settings.getLanguage()).thenReturn("English");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotLoadMessagesIfResourceNotFound() throws Exception {
        when(resourceLoader.getResource(FILE_NAME)).thenReturn(null);

        mgr.bind(cmsLiteService, null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotLoadMessagesIfResourceNotExists() throws Exception {
        when(resourceLoader.getResource(FILE_NAME)).thenReturn(resource);
        when(resource.exists()).thenReturn(false);

        mgr.bind(cmsLiteService, null);
    }

    @Test
    public void shouldLoadMessages() throws Exception {
        when(resourceLoader.getResource(FILE_NAME)).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(getResourcesAsStream());

        mgr.bind(cmsLiteService, null);

        for (Content content : getResourcesAsList()) {
            verify(cmsLiteService).isStringContentAvailable(content.getLanguage(), content.getName());
            verify(cmsLiteService).addContent(content);
        }
    }

    @Test
    public void shouldNotLoadSingleMessageTwice() throws Exception {
        when(resourceLoader.getResource(FILE_NAME)).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(getResourcesAsStream());

        List<Content> contents = getResourcesAsList();
        Content loaded = contents.get(1);
        contents.remove(1);

        when(cmsLiteService.isStringContentAvailable(loaded.getLanguage(), loaded.getName())).thenReturn(true);

        mgr.bind(cmsLiteService, null);

        for (Content content : contents) {
            verify(cmsLiteService).isStringContentAvailable(content.getLanguage(), content.getName());
            verify(cmsLiteService).addContent(content);
        }

        verify(cmsLiteService).isStringContentAvailable(loaded.getLanguage(), loaded.getName());
        verify(cmsLiteService, never()).addContent(loaded);
    }

    @Test
    public void shouldNotLoadAllMessagesTwice() throws Exception {
        when(resourceLoader.getResource(FILE_NAME)).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(getResourcesAsStream());

        mgr.bind(cmsLiteService, null);

        for (Content content : getResourcesAsList()) {
            verify(cmsLiteService).isStringContentAvailable(content.getLanguage(), content.getName());
            verify(cmsLiteService).addContent(content);
        }

        mgr.bind(cmsLiteService, null);

        // each of these methods were executed one time
        // by the previous execution of the 'bind' method

        verify(resourceLoader, times(1)).getResource(FILE_NAME);
        verify(resource, times(1)).exists();
        verify(resource, times(1)).getInputStream();

        for (Content content : getResourcesAsList()) {
            verify(cmsLiteService, times(1)).isStringContentAvailable(content.getLanguage(), content.getName());
            verify(cmsLiteService, times(1)).addContent(content);
        }
    }

    @Test
    public void shouldChangeSettingsLanguageIfContentLanguageIsDifferent() throws Exception {
        when(resourceLoader.getResource(FILE_NAME)).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(getResourcesAsStream());
        when(settings.getLanguage()).thenReturn("Spanish");

        mgr.bind(cmsLiteService, null);

        for (Content content : getResourcesAsList()) {
            verify(cmsLiteService).isStringContentAvailable(content.getLanguage(), content.getName());
            verify(cmsLiteService).addContent(content);
        }
    }

    private List<Content> getResourcesAsList() {
        List<Content> contents = new ArrayList<>();
        for (int i = 1; i < 4; ++i) {
            contents.add(new StringContent("English", "test-content-" + i, "test-value-" + i));
        }

        return contents;
    }

    private InputStream getResourcesAsStream() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        for (int i = 1; i < 4; ++i) {
            builder.append("{");
            builder.append("\"language\":");
            builder.append("\"English\",");
            builder.append("\"name\":");
            builder.append(String.format("\"test-content-%d\",", i));
            builder.append("\"value\":");
            builder.append(String.format("\"test-value-%d\"", i));
            builder.append("}");

            // last
            if (i != 3) {
                builder.append(",");
            }
        }

        builder.append("]");

        return new ByteArrayInputStream(builder.toString().getBytes());
    }

}
