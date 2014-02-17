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
import static org.motechproject.sms.api.constants.EventDataKeys.MESSAGE;
import static org.motechproject.sms.api.constants.EventDataKeys.RECIPIENTS;
import static org.motechproject.sms.api.constants.EventSubjects.SEND_SMS;
import static org.motechproject.tasks.domain.ParameterType.LIST;
import static org.worldvision.sierraleone.constants.Commcare.ATTENDED_PNC;
import static org.worldvision.sierraleone.constants.Commcare.MOTHER_ALIVE;

public class SendSmsToPatientTest extends RuleTest {
    private static final String COMMCARE_PROVIDER_ID = "eec2fa1b5d77bd5c536def1b150bdae7";
    private static final String CMSLITE_PROVIDER_ID = "eec2fa1b5d77bd5c536def1b150bc6e4";
    private static final String CASE_ID_VALUE = "caseId";

    @Mock
    private DataProvider commcareDataProvider;

    @Mock
    private DataProvider cmsliteDataProvider;

    private CaseInfo caseInfo;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        super.setUp();

        mockCurrentDate(new LocalDate(2013, 8, 14));

        handler.addDataProvider(COMMCARE_PROVIDER_ID, commcareDataProvider);
        handler.addDataProvider(CMSLITE_PROVIDER_ID, cmsliteDataProvider);

        setTask(1, "send_sms_to_patient.json", "rule-1-send-sms-to-patient");
        setMessages();
    }

    @Test
    public void shouldExecuteTaskCorrectly() throws Exception {
        ActionEvent actionEvent = new ActionEvent();
        actionEvent.setSubject(SEND_SMS);
        actionEvent.addParameter(new ActionParameter(MESSAGE, MESSAGE), true);
        actionEvent.addParameter(new ActionParameter(RECIPIENTS, RECIPIENTS, LIST), true);

        Map<String, String> commcareLookup = new HashMap<>();
        commcareLookup.put("id", CASE_ID_VALUE);

        Map<String, String> cmsliteLookup = new HashMap<>();
        cmsliteLookup.put("cmslite.dataname", "PostnatalConsultationReminder");
        cmsliteLookup.put("cmslite.language", "English");

        when(taskService.getActionEventFor(task.getActions().get(0))).thenReturn(actionEvent);

        caseInfo = new CaseInfo();
        caseInfo.setFieldValues(new HashMap<String, String>());
        caseInfo.getFieldValues().put(MOTHER_ALIVE, "yes");
        caseInfo.getFieldValues().put(ATTENDED_PNC, "no");
        caseInfo.getFieldValues().put("mother_name", "mother_name");
        caseInfo.getFieldValues().put("mother_phone_number", "0777777777");

        when(commcareDataProvider.lookup("CaseInfo", commcareLookup)).thenReturn(caseInfo);
        when(cmsliteDataProvider.lookup("StringContent", cmsliteLookup)).thenReturn(mesages.get("PostnatalConsultationReminder"));

        ArgumentCaptor<MotechEvent> captor = ArgumentCaptor.forClass(MotechEvent.class);
        handler.handle(firedCampaignMessage());

        verify(eventRelay, times(2)).sendEventMessage(captor.capture());

        MotechEvent event = (MotechEvent) CollectionUtils.find(captor.getAllValues(), new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                return o instanceof MotechEvent && SEND_SMS.equalsIgnoreCase(((MotechEvent) o).getSubject());
            }
        });

        assertNotNull(event);

        assertEquals(SEND_SMS, event.getSubject());

        String message = mesages.get("PostnatalConsultationReminder").getValue();

        assertEquals(String.format(message, "mother_name"), event.getParameters().get(MESSAGE));
        assertEquals(asList("+232 777777777"), event.getParameters().get(RECIPIENTS));
    }

    @Test
    public void shouldNotExecuteTaskIfPhoneNotStartWithZero() throws Exception {
        Map<String, String> commcareLookup = new HashMap<>();
        commcareLookup.put("id", CASE_ID_VALUE);

        caseInfo = new CaseInfo();
        caseInfo.setFieldValues(new HashMap<String, String>());
        caseInfo.getFieldValues().put(MOTHER_ALIVE, "yes");
        caseInfo.getFieldValues().put(ATTENDED_PNC, "no");
        caseInfo.getFieldValues().put("mother_name", "mother_name");
        caseInfo.getFieldValues().put("mother_phone_number", "777777777");

        when(commcareDataProvider.lookup("CaseInfo", commcareLookup)).thenReturn(caseInfo);

        handler.handle(firedCampaignMessage());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(cmsliteDataProvider, never()).lookup(anyString(), anyMap());
    }

    @Test
    public void shouldNotExecuteTaskIfAttendedPostnatal() throws Exception {
        Map<String, String> commcareLookup = new HashMap<>();
        commcareLookup.put("id", CASE_ID_VALUE);

        caseInfo = new CaseInfo();
        caseInfo.setFieldValues(new HashMap<String, String>());
        caseInfo.getFieldValues().put(MOTHER_ALIVE, "yes");
        caseInfo.getFieldValues().put(ATTENDED_PNC, "yes");
        caseInfo.getFieldValues().put("mother_name", "mother_name");
        caseInfo.getFieldValues().put("mother_phone_number", "0777777777");

        when(commcareDataProvider.lookup("CaseInfo", commcareLookup)).thenReturn(caseInfo);

        handler.handle(firedCampaignMessage());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(cmsliteDataProvider, never()).lookup(anyString(), anyMap());
    }

    @Test
    public void shouldNotExecuteTaskIfNotAlive() throws Exception {
        Map<String, String> commcareLookup = new HashMap<>();
        commcareLookup.put("id", CASE_ID_VALUE);

        caseInfo = new CaseInfo();
        caseInfo.setFieldValues(new HashMap<String, String>());
        caseInfo.getFieldValues().put(MOTHER_ALIVE, "no");
        caseInfo.getFieldValues().put(ATTENDED_PNC, "no");
        caseInfo.getFieldValues().put("mother_name", "mother_name");
        caseInfo.getFieldValues().put("mother_phone_number", "0777777777");

        when(commcareDataProvider.lookup("CaseInfo", commcareLookup)).thenReturn(caseInfo);

        handler.handle(firedCampaignMessage());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(cmsliteDataProvider, never()).lookup(anyString(), anyMap());
    }

    @Test
    public void shouldNotExecuteTaskIfCampaignNameIsIncorrect() throws Exception {
        MotechEvent event = firedCampaignMessage();
        event.getParameters().put(CAMPAIGN_NAME_KEY, "something");

        handler.handle(event);

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(commcareDataProvider, never()).lookup(anyString(), anyMap());
        verify(cmsliteDataProvider, never()).lookup(anyString(), anyMap());
    }

    private MotechEvent firedCampaignMessage() {
        return new MotechEventBuilder()
                .withSubject(getTriggerSubject())
                .withParameter(CAMPAIGN_NAME_KEY, HEALTH_CENTER_POSTNATAL_CONSULTATION_REMINDER)
                .withParameter(EXTERNAL_ID_KEY, CASE_ID_VALUE)
                .build();
    }

}
