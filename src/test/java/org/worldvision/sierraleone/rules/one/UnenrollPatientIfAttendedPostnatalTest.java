package org.worldvision.sierraleone.rules.one;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commons.api.DataProvider;
import org.motechproject.event.MotechEvent;
import org.motechproject.tasks.domain.ActionEvent;
import org.motechproject.tasks.domain.ActionParameter;
import org.motechproject.tasks.service.TaskTriggerHandler;
import org.worldvision.sierraleone.MotechEventBuilder;
import org.worldvision.sierraleone.rules.RuleTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.motechproject.messagecampaign.EventKeys.CAMPAIGN_NAME_KEY;
import static org.motechproject.messagecampaign.EventKeys.EXTERNAL_ID_KEY;
import static org.motechproject.messagecampaign.EventKeys.UNENROLL_USER_SUBJECT;
import static org.worldvision.sierraleone.constants.Commcare.ATTENDED_PNC;

public class UnenrollPatientIfAttendedPostnatalTest extends RuleTest {
    private static final String COMMCARE_PROVIDER_ID = "eec2fa1b5d77bd5c536def1b150bdae7";
    private static final String CASE_ID_VALUE = "caseId";

    @Mock
    private DataProvider commcareDataProvider;

    private CaseInfo caseInfo;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        super.setUp();

        mockCurrentDate(new LocalDate(2013, 8, 14));

        handler = new TaskTriggerHandler(
                taskService, activityService, registryService, eventRelay, taskSettings
        );

        handler.addDataProvider(COMMCARE_PROVIDER_ID, commcareDataProvider);

        setTask(1, "unenroll_patient_if_attended_postnatal.json", "rule-1-unenroll-patient-if-attended-postnatal");
    }

    @Test
    public void shouldExecuteTaskCorrectly() throws Exception {
        ActionEvent actionEvent = new ActionEvent();
        actionEvent.setSubject(UNENROLL_USER_SUBJECT);
        actionEvent.addParameter(new ActionParameter(EXTERNAL_ID_KEY, EXTERNAL_ID_KEY), true);
        actionEvent.addParameter(new ActionParameter(CAMPAIGN_NAME_KEY, CAMPAIGN_NAME_KEY), true);

        Map<String, String> commcareLookup = new HashMap<>();
        commcareLookup.put("id", CASE_ID_VALUE);

        when(taskService.getActionEventFor(task.getActions().get(0))).thenReturn(actionEvent);

        caseInfo = new CaseInfo();
        caseInfo.setFieldValues(new HashMap<String, String>());
        caseInfo.getFieldValues().put(ATTENDED_PNC, "yes");

        when(commcareDataProvider.lookup("CaseInfo", commcareLookup)).thenReturn(caseInfo);

        ArgumentCaptor<MotechEvent> captor = ArgumentCaptor.forClass(MotechEvent.class);
        handler.handle(firedCampaignMessage());

        verify(eventRelay, times(2)).sendEventMessage(captor.capture());

        MotechEvent event = (MotechEvent) CollectionUtils.find(captor.getAllValues(), new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                return o instanceof MotechEvent && UNENROLL_USER_SUBJECT.equalsIgnoreCase(((MotechEvent) o).getSubject());
            }
        });

        assertNotNull(event);

        assertEquals(UNENROLL_USER_SUBJECT, event.getSubject());

        assertEquals(HEALTH_CENTER_POSTNATAL_CONSULTATION_REMINDER, event.getParameters().get(CAMPAIGN_NAME_KEY));
        assertEquals(CASE_ID_VALUE, event.getParameters().get(EXTERNAL_ID_KEY));
    }

    @Test
    public void shouldNotExecuteTaskIfNotAttendedPostnatal() throws Exception {
        Map<String, String> commcareLookup = new HashMap<>();
        commcareLookup.put("id", CASE_ID_VALUE);

        caseInfo = new CaseInfo();
        caseInfo.setFieldValues(new HashMap<String, String>());
        caseInfo.getFieldValues().put(ATTENDED_PNC, "no");

        when(commcareDataProvider.lookup("CaseInfo", commcareLookup)).thenReturn(caseInfo);

        handler.handle(firedCampaignMessage());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
    }

    @Test
    public void shouldNotExecuteTaskIfCampaignNameIsIncorrect() throws Exception {
        MotechEvent event = firedCampaignMessage();
        event.getParameters().put(CAMPAIGN_NAME_KEY, "something");

        handler.handle(event);
    }

    private MotechEvent firedCampaignMessage() {
        return new MotechEventBuilder()
                .withSubject(getTriggerSubject())
                .withParameter(CAMPAIGN_NAME_KEY, HEALTH_CENTER_POSTNATAL_CONSULTATION_REMINDER)
                .withParameter(EXTERNAL_ID_KEY, CASE_ID_VALUE)
                .build();
    }

}
