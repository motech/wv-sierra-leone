package org.worldvision.sierraleone.rules;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.CollectionType;
import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.Before;
import org.mockito.Mock;
import org.motechproject.cmslite.api.model.StringContent;
import org.motechproject.event.listener.EventListenerRegistryService;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.server.config.SettingsFacade;
import org.motechproject.tasks.domain.Task;
import org.motechproject.tasks.domain.TriggerEvent;
import org.motechproject.tasks.service.TaskActivityService;
import org.motechproject.tasks.service.TaskService;
import org.motechproject.tasks.service.TaskTriggerHandler;
import org.motechproject.testing.utils.BaseUnitTest;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class RuleTest extends BaseUnitTest {
    protected static final String HEALTH_CENTER_POSTNATAL_CONSULTATION_REMINDER = "Health Center Postnatal Consultation Reminder";

    @Mock
    protected TaskService taskService;

    @Mock
    protected TaskActivityService activityService;

    @Mock
    protected EventListenerRegistryService registryService;

    @Mock
    protected EventRelay eventRelay;

    @Mock
    protected SettingsFacade taskSettings;

    @Mock
    protected TriggerEvent triggerEvent;

    protected TaskTriggerHandler handler;

    protected ObjectMapper mapper = new ObjectMapper();
    protected Map<String, StringContent> mesages = new HashMap<>();
    protected Task task;

    @Before
    public void setUp() throws Exception {
        handler = new TaskTriggerHandler(
                taskService, activityService, registryService, eventRelay, taskSettings
        );
    }

    protected void setTask(int ruleNumber, String fileName, String ruleId) throws Exception {
        StringWriter writer = new StringWriter();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(String.format("tasks/rule_%d/%s", ruleNumber, fileName));

        IOUtils.copy(stream, writer);

        task = mapper.readValue(writer.toString(), Task.class);

        assertEquals(ruleId, task.getId());

        when(taskService.findTrigger(getTriggerSubject())).thenReturn(triggerEvent);
        when(taskService.findTasksForTrigger(triggerEvent)).thenReturn(asList(task));
    }

    protected void setMessages() throws Exception {
        StringWriter writer = new StringWriter();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(String.format("sms-messages.json"));

        IOUtils.copy(stream, writer);

        TypeFactory typeFactory = mapper.getTypeFactory();
        CollectionType collectionType = typeFactory.constructCollectionType(List.class, StringContent.class);

        List<StringContent> contents = mapper.readValue(writer.toString(), collectionType);

        for (StringContent content : contents) {
            mesages.put(content.getName(), content);
        }
    }

    protected String getTriggerSubject() {
        return task != null ? task.getTrigger().getSubject() : StringUtils.EMPTY;
    }

}
