package org.worldvision.sierraleone.rules.two;

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
import org.worldvision.sierraleone.MotechEventBuilder;
import org.worldvision.sierraleone.rules.RuleTest;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.motechproject.messagecampaign.EventKeys.CAMPAIGN_NAME_KEY;
import static org.motechproject.messagecampaign.EventKeys.EXTERNAL_ID_KEY;
import static org.motechproject.messagecampaign.EventKeys.UNENROLL_USER_SUBJECT;
import static org.motechproject.sms.api.constants.EventDataKeys.MESSAGE;
import static org.motechproject.sms.api.constants.EventDataKeys.RECIPIENTS;
import static org.motechproject.sms.api.constants.EventSubjects.SEND_SMS;

public class UnenrollPatientIfClosedTest extends RuleTest {
    private static final String COMMCARE_PROVIDER_ID = "3980fa00249eb3bf73e200bd85062954";
    private static final String REFERRAL_CASE_ID_VALUE = "referralCaseId";
    private static final String MOTHER_CASE_ID_VALUE = "motherCaseId";
    private static final String EXTERNAL_ID_VALUE = MOTHER_CASE_ID_VALUE + ":" + REFERRAL_CASE_ID_VALUE;

    @Mock
    private DataProvider commcareDataProvider;

    private CaseInfo referralCaseInfo;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        super.setUp();

        mockCurrentDate(new LocalDate(2013, 8, 14));

        handler.addDataProvider(COMMCARE_PROVIDER_ID, commcareDataProvider);

        setTask(2, "unenroll_patient_if_closed.json", "rule-2-unenroll-patient-if-closed");
        setMessages();
    }

    @Test
    public void shouldExecuteTaskCorrectly() throws Exception {
        ActionEvent actionEvent = new ActionEvent();
        actionEvent.setSubject(UNENROLL_USER_SUBJECT);
        actionEvent.addParameter(new ActionParameter(EXTERNAL_ID_KEY, EXTERNAL_ID_KEY), true);
        actionEvent.addParameter(new ActionParameter(CAMPAIGN_NAME_KEY, CAMPAIGN_NAME_KEY), true);

        Map<String, String> referralLookup = new HashMap<>();
        referralLookup.put("id", REFERRAL_CASE_ID_VALUE);

        when(taskService.getActionEventFor(task.getActions().get(0))).thenReturn(actionEvent);

        referralCaseInfo = new CaseInfo();
        referralCaseInfo.setFieldValues(new HashMap<String, String>());
        referralCaseInfo.getFieldValues().put("open", "false");

        when(commcareDataProvider.lookup("CaseInfo", referralLookup)).thenReturn(referralCaseInfo);

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

        assertEquals(MOTHER_REFERRAL_REMINDER, event.getParameters().get(CAMPAIGN_NAME_KEY));
        assertEquals(EXTERNAL_ID_VALUE, event.getParameters().get(EXTERNAL_ID_KEY));
    }

    @Test
    public void shouldNotExecuteTaskIfNotClosed() throws Exception {
        ActionEvent actionEvent = new ActionEvent();
        actionEvent.setSubject(UNENROLL_USER_SUBJECT);
        actionEvent.addParameter(new ActionParameter(EXTERNAL_ID_KEY, EXTERNAL_ID_KEY), true);
        actionEvent.addParameter(new ActionParameter(CAMPAIGN_NAME_KEY, CAMPAIGN_NAME_KEY), true);

        Map<String, String> referralLookup = new HashMap<>();
        referralLookup.put("id", REFERRAL_CASE_ID_VALUE);

        when(taskService.getActionEventFor(task.getActions().get(0))).thenReturn(actionEvent);

        referralCaseInfo = new CaseInfo();
        referralCaseInfo.setFieldValues(new HashMap<String, String>());
        referralCaseInfo.getFieldValues().put("open", "true");

        when(commcareDataProvider.lookup("CaseInfo", referralLookup)).thenReturn(referralCaseInfo);

        handler.handle(firedCampaignMessage());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
    }

    @Test
    public void shouldNotExecuteTaskIfCampaignNameIsIncorrect() throws Exception {
        MotechEvent event = firedCampaignMessage();
        event.getParameters().put(CAMPAIGN_NAME_KEY, "something");

        handler.handle(event);

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(commcareDataProvider, never()).lookup(anyString(), anyMap());
    }

    private MotechEvent firedCampaignMessage() {
        return new MotechEventBuilder()
                .withSubject(getTriggerSubject())
                .withParameter(CAMPAIGN_NAME_KEY, MOTHER_REFERRAL_REMINDER)
                .withParameter(EXTERNAL_ID_KEY, EXTERNAL_ID_VALUE)
                .build();
    }

}
