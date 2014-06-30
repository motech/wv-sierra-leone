package org.worldvision.sierraleone.rules.six;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.domain.CommcareFixture;
import org.motechproject.commons.api.DataProvider;
import org.motechproject.commons.date.util.DateUtil;
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

public class SendSmsToPatientTest extends RuleTest {
    private static final String COMMCARE_PROVIDER_ID = "4408354fae5368389825691bc50b09d4";
    private static final String CMSLITE_PROVIDER_ID = "4408354fae5368389825691bc50b171a";
    private static final String CASE_ID_VALUE = "caseId";
    private static final String PARENT_CASE_ID_VALUE = "parentCaseId";
    private static final String PHU_ID_VALUE = "phuId";
    private static final String EXTERNAL_ID_VALUE = String.format("%s:%s", CASE_ID_VALUE, PARENT_CASE_ID_VALUE);

    @Mock
    private DataProvider commcareDataProvider;

    @Mock
    private DataProvider cmsliteDataProvider;

    private CaseInfo caseInfo;

    private CaseInfo parentCaseInfo;

    private CommcareFixture fixture;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        super.setUp();

        mockCurrentDate(new LocalDate(2013, 8, 14));

        handler.addDataProvider(COMMCARE_PROVIDER_ID, commcareDataProvider);
        handler.addDataProvider(CMSLITE_PROVIDER_ID, cmsliteDataProvider);

        setTask(6, "send_sms_to_patient.json", "rule-6-send-sms-to-patient");
        setMessages();
    }

    @Test
    public void shouldExecuteTaskCorrectly() throws Exception {
        ActionEvent actionEvent = new ActionEvent();
        actionEvent.setSubject(SEND_SMS);
        actionEvent.addParameter(new ActionParameter(MESSAGE, MESSAGE), true);
        actionEvent.addParameter(new ActionParameter(RECIPIENTS, RECIPIENTS, LIST), true);

        when(taskService.getActionEventFor(task.getActions().get(0))).thenReturn(actionEvent);

        Map<String, String> caseLookup = new HashMap<>();
        caseLookup.put("id", CASE_ID_VALUE);

        caseInfo = new CaseInfo();
        caseInfo.setFieldValues(new HashMap<String, String>());
        caseInfo.getFieldValues().put("dob", new LocalDate(2013, 8, 14).plusMonths(8).toString("yyyy-MM-dd"));
        caseInfo.getFieldValues().put("vitamin_a_mother", "no");
        caseInfo.getFieldValues().put("child_name", "childName");

        Map<String, String> parentCaseLookup = new HashMap<>();
        parentCaseLookup.put("id", PARENT_CASE_ID_VALUE);

        parentCaseInfo = new CaseInfo();
        parentCaseInfo.setFieldValues(new HashMap<String, String>());
        parentCaseInfo.getFieldValues().put("phu_id", PHU_ID_VALUE);
        parentCaseInfo.getFieldValues().put("mother_phone_number", "777777777");

        Map<String, String> fixtureLookup = new HashMap<>();
        fixtureLookup.put("id", PHU_ID_VALUE);

        fixture = new CommcareFixture();
        fixture.setFields(new HashMap<String, String>());
        fixture.getFields().put("name", "fixtureName");

        Map<String, String> cmsliteLookup = new HashMap<>();
        cmsliteLookup.put("cmslite.dataname", "ChildVitaminAReminder");
        cmsliteLookup.put("cmslite.language", "English");

        when(commcareDataProvider.lookup("CaseInfo", caseLookup)).thenReturn(caseInfo);
        when(commcareDataProvider.lookup("CaseInfo", parentCaseLookup)).thenReturn(parentCaseInfo);
        when(commcareDataProvider.lookup("CommcareFixture", fixtureLookup)).thenReturn(fixture);
        when(cmsliteDataProvider.lookup("StringContent", cmsliteLookup)).thenReturn(mesages.get("ChildVitaminAReminder"));

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

        String message = mesages.get("ChildVitaminAReminder").getValue();

        assertEquals(String.format(message, "childName", "fixtureName"), event.getParameters().get(MESSAGE));
        assertEquals(asList("777777777"), event.getParameters().get(RECIPIENTS));
    }

    @Test
    public void shouldNotExecuteTaskIfPhoneNotExists() throws Exception {
        Map<String, String> caseLookup = new HashMap<>();
        caseLookup.put("id", CASE_ID_VALUE);

        caseInfo = new CaseInfo();
        caseInfo.setFieldValues(new HashMap<String, String>());
        caseInfo.getFieldValues().put("dob", new LocalDate(2013, 8, 14).plusMonths(8).toString("yyyy-MM-dd"));
        caseInfo.getFieldValues().put("vitamin_a_mother", "no");
        caseInfo.getFieldValues().put("child_name", "childName");

        Map<String, String> parentCaseLookup = new HashMap<>();
        parentCaseLookup.put("id", PARENT_CASE_ID_VALUE);

        parentCaseInfo = new CaseInfo();
        parentCaseInfo.setFieldValues(new HashMap<String, String>());
        parentCaseInfo.getFieldValues().put("phu_id", PHU_ID_VALUE);

        when(commcareDataProvider.lookup("CaseInfo", caseLookup)).thenReturn(caseInfo);
        when(commcareDataProvider.lookup("CaseInfo", parentCaseLookup)).thenReturn(parentCaseInfo);

        handler.handle(firedCampaignMessage());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(cmsliteDataProvider, never()).lookup(anyString(), anyMap());
    }

    @Test
    public void shouldNotExecuteTaskIfVitaminAWasTaken() throws Exception {
        Map<String, String> caseLookup = new HashMap<>();
        caseLookup.put("id", CASE_ID_VALUE);

        caseInfo = new CaseInfo();
        caseInfo.setFieldValues(new HashMap<String, String>());
        caseInfo.getFieldValues().put("dob", new LocalDate(2013, 8, 14).plusMonths(8).toString("yyyy-MM-dd"));
        caseInfo.getFieldValues().put("vitamin_a_mother", "yes");

        Map<String, String> parentCaseLookup = new HashMap<>();
        parentCaseLookup.put("id", PARENT_CASE_ID_VALUE);

        parentCaseInfo = new CaseInfo();
        parentCaseInfo.setFieldValues(new HashMap<String, String>());
        parentCaseInfo.getFieldValues().put("phu_id", PHU_ID_VALUE);

        when(commcareDataProvider.lookup("CaseInfo", caseLookup)).thenReturn(caseInfo);
        when(commcareDataProvider.lookup("CaseInfo", parentCaseLookup)).thenReturn(parentCaseInfo);

        handler.handle(firedCampaignMessage());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(cmsliteDataProvider, never()).lookup(anyString(), anyMap());
    }

    @Test
    public void shouldNotExecuteTaskIfNumberOfMonthsBetweenCurrentDateAndBirthDateIsLessThanSix() throws Exception {
        Map<String, String> caseLookup = new HashMap<>();
        caseLookup.put("id", CASE_ID_VALUE);

        caseInfo = new CaseInfo();
        caseInfo.setFieldValues(new HashMap<String, String>());
        caseInfo.getFieldValues().put("dob", new LocalDate(2013, 8, 14).plusMonths(2).toString("yyyy-MM-dd"));
        caseInfo.getFieldValues().put("vitamin_a_mother", "no");
        caseInfo.getFieldValues().put("child_name", "childName");

        Map<String, String> parentCaseLookup = new HashMap<>();
        parentCaseLookup.put("id", PARENT_CASE_ID_VALUE);

        parentCaseInfo = new CaseInfo();
        parentCaseInfo.setFieldValues(new HashMap<String, String>());
        parentCaseInfo.getFieldValues().put("phu_id", PHU_ID_VALUE);

        when(commcareDataProvider.lookup("CaseInfo", caseLookup)).thenReturn(caseInfo);
        when(commcareDataProvider.lookup("CaseInfo", parentCaseLookup)).thenReturn(parentCaseInfo);

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
                .withParameter(CAMPAIGN_NAME_KEY, CHILD_VITAMIN_A_REMINDER)
                .withParameter(EXTERNAL_ID_KEY, EXTERNAL_ID_VALUE)
                .build();
    }

}
