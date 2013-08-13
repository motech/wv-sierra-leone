package org.worldvision.sierraleone;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.motechproject.tasks.domain.Channel;
import org.motechproject.tasks.domain.Task;
import org.motechproject.tasks.domain.TaskDataProvider;
import org.motechproject.tasks.service.ChannelService;
import org.motechproject.tasks.service.TaskDataProviderService;
import org.motechproject.tasks.service.TaskService;
import org.motechproject.tasks.service.TriggerHandler;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ManagementTasksTest {
    private static final int TWO_SECONDS = 2 * 1000;

    @Mock
    private TriggerHandler handler;

    @Mock
    private TaskService taskService;

    @Mock
    private ChannelService channelService;

    @Mock
    private TaskDataProviderService providerService;

    @Mock
    private Resource resource;

    @Mock
    private ResourcePatternResolver resourcePatternResolver;

    private ManagementTasks mgr;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(resourcePatternResolver.getResources("/tasks/**/*.json")).thenReturn(new Resource[]{resource});
        when(resource.getFilename()).thenReturn("tasks/rule_5/send_sms_to_phu.json");

        mgr = new ManagementTasks(resourcePatternResolver);
    }

    @Test
    public void shouldRegisterTasksOnlyIfAllServicesAreAvailable() throws Exception {
        when(resource.getInputStream()).thenReturn(getTaskAsStream());
        when(channelService.getChannel(anyString())).thenReturn(new Channel());
        when(providerService.getProvider(anyString())).thenReturn(new TaskDataProvider());

        mgr.bind(new Object(), null);
        verify(handler, never()).registerHandlerFor(anyString());
        verify(taskService, never()).importTask(anyString());

        mgr.bind(handler, null);
        verify(handler, never()).registerHandlerFor(anyString());
        verify(taskService, never()).importTask(anyString());

        mgr.bind(taskService, null);
        verify(handler, never()).registerHandlerFor(anyString());
        verify(taskService, never()).importTask(anyString());

        mgr.bind(channelService, null);
        verify(handler, never()).registerHandlerFor(anyString());
        verify(taskService, never()).importTask(anyString());

        mgr.bind(providerService, null);

        // wait until TaskRegistration finishes work
        Thread.sleep(TWO_SECONDS);
        Thread.sleep(TWO_SECONDS);

        verify(taskService).importTask(getTaskAsNode().toString());
        verify(handler).registerHandlerFor(getTask().getTrigger().getSubject());
    }

    @Test
    public void shouldNotRegisterTaskIfDataSourceProviderNotExist() throws Exception {
        when(resource.getInputStream()).thenReturn(getTaskAsStream());
        when(channelService.getChannel(anyString())).thenReturn(new Channel());

        mgr.bind(new Object(), null);
        verify(handler, never()).registerHandlerFor(anyString());
        verify(taskService, never()).importTask(anyString());

        mgr.bind(handler, null);
        verify(handler, never()).registerHandlerFor(anyString());
        verify(taskService, never()).importTask(anyString());

        mgr.bind(taskService, null);
        verify(handler, never()).registerHandlerFor(anyString());
        verify(taskService, never()).importTask(anyString());

        mgr.bind(channelService, null);
        verify(handler, never()).registerHandlerFor(anyString());
        verify(taskService, never()).importTask(anyString());

        mgr.bind(providerService, null);

        // wait until TaskRegistration finishes work
        Thread.sleep(TWO_SECONDS);

        verify(taskService, never()).importTask(anyString());
        verify(handler, never()).registerHandlerFor(anyString());
    }

    @Test
    public void shouldNotRegisterTaskIfChannelNotExist() throws Exception {
        when(resource.getInputStream()).thenReturn(getTaskAsStream());
        when(channelService.getChannel(getTask().getTrigger().getModuleName())).thenReturn(new Channel());
        when(providerService.getProvider(anyString())).thenReturn(new TaskDataProvider());

        mgr.bind(new Object(), null);
        verify(handler, never()).registerHandlerFor(anyString());
        verify(taskService, never()).importTask(anyString());

        mgr.bind(handler, null);
        verify(handler, never()).registerHandlerFor(anyString());
        verify(taskService, never()).importTask(anyString());

        mgr.bind(taskService, null);
        verify(handler, never()).registerHandlerFor(anyString());
        verify(taskService, never()).importTask(anyString());

        mgr.bind(channelService, null);
        verify(handler, never()).registerHandlerFor(anyString());
        verify(taskService, never()).importTask(anyString());

        mgr.bind(providerService, null);

        // wait until TaskRegistration finishes work
        Thread.sleep(TWO_SECONDS);

        verify(taskService, never()).importTask(anyString());
        verify(handler, never()).registerHandlerFor(anyString());
    }

    @Test
    public void shouldDisabledTasksWhenTaskServiceIsUnbind() throws Exception {
        Task task = getTask();

        when(resource.getInputStream()).thenReturn(getTaskAsStream());
        when(taskService.getTask(task.getId())).thenReturn(task);

        mgr.bind(taskService, null);
        verify(handler, never()).registerHandlerFor(anyString());
        verify(taskService, never()).importTask(anyString());

        mgr.unbind(new Object(), null);
        verify(taskService, never()).save(any(Task.class));

        mgr.unbind(handler, null);
        verify(taskService, never()).save(any(Task.class));

        mgr.unbind(channelService, null);
        verify(taskService, never()).save(any(Task.class));

        mgr.unbind(providerService, null);
        verify(taskService, never()).save(any(Task.class));

        mgr.unbind(taskService, null);

        task.setEnabled(false);
        verify(taskService).save(task);
    }

    private Task getTask() throws IOException {
        return new ObjectMapper().readValue(getTaskAsNode(), Task.class);
    }

    private JsonNode getTaskAsNode() throws IOException {
        return new ObjectMapper().readTree(getTaskAsWriter().toString());
    }

    private InputStream getTaskAsStream() throws IOException {
        return getClass().getClassLoader().getResourceAsStream("tasks/rule_5/send_sms_to_phu.json");
    }

    private StringWriter getTaskAsWriter() throws IOException {
        StringWriter writer = new StringWriter();
        InputStream stream = getTaskAsStream();

        IOUtils.copy(stream, writer);

        return writer;
    }
}
