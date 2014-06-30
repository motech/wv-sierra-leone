package org.worldvision.sierraleone.rules.two;

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
import org.motechproject.event.MotechEvent;
import org.motechproject.tasks.domain.ActionEvent;
import org.motechproject.tasks.domain.ActionParameter;
import org.worldvision.sierraleone.MotechEventBuilder;
import org.worldvision.sierraleone.rules.RuleTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

public class SendSmsToPHUTest extends RuleTest {
    private static final String COMMCARE_PROVIDER_ID = "3980fa00249eb3bf73e200bd85062954";
    private static final String CMSLITE_PROVIDER_ID = "3980fa00249eb3bf73e200bd85061c11";
    private static final String REFERRAL_CASE_ID_VALUE = "referralCaseId";
    private static final String MOTHER_CASE_ID_VALUE = "motherCaseId";
    private static final String EXTERNAL_ID_VALUE = MOTHER_CASE_ID_VALUE + ":" + REFERRAL_CASE_ID_VALUE;
    private static final String PHU_ID_VALUE = "phuId";
    private static final String PHONE_VALUE = "777777777";

    @Mock
    private DataProvider commcareDataProvider;

    @Mock
    private DataProvider cmsliteDataProvider;

    private CommcareFixture commcareFixture;

    private CaseInfo referralCaseInfo;

    private CaseInfo motherCaseInfo;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        super.setUp();

        mockCurrentDate(new LocalDate(2013, 8, 14));

        handler.addDataProvider(COMMCARE_PROVIDER_ID, commcareDataProvider);
        handler.addDataProvider(CMSLITE_PROVIDER_ID, cmsliteDataProvider);

        setTask(2, "send_sms_to_phu.json", "rule-2-send-sms-to-phu");
        setMessages();
    }

    @Test
    public void shouldExecuteTaskCorrectly() throws Exception {
        ActionEvent actionEvent = new ActionEvent();
        actionEvent.setSubject(SEND_SMS);
        actionEvent.addParameter(new ActionParameter(MESSAGE, MESSAGE), true);
        actionEvent.addParameter(new ActionParameter(RECIPIENTS, RECIPIENTS, LIST), true);

        Map<String, String> referralLookup = new HashMap<>();
        referralLookup.put("id", REFERRAL_CASE_ID_VALUE);

        Map<String, String> motherLookup = new HashMap<>();
        motherLookup.put("id", MOTHER_CASE_ID_VALUE);

        Map<String, String> cmsliteLookup = new HashMap<>();
        cmsliteLookup.put("cmslite.dataname", "MotherReferralReminder_PHU");
        cmsliteLookup.put("cmslite.language", "English");

        when(taskService.getActionEventFor(task.getActions().get(0))).thenReturn(actionEvent);

        referralCaseInfo = new CaseInfo();
        referralCaseInfo.setFieldValues(new HashMap<String, String>());
        referralCaseInfo.getFieldValues().put("open", "true");

        motherCaseInfo = new CaseInfo();
        motherCaseInfo.setFieldValues(new HashMap<String, String>());
        motherCaseInfo.getFieldValues().put("mother_name", "mother_name");
        motherCaseInfo.getFieldValues().put("phu_id", PHU_ID_VALUE);

        Map<String, String> fixtureLookup = new HashMap<>();
        fixtureLookup.put("id", PHU_ID_VALUE);

        commcareFixture = new CommcareFixture();
        commcareFixture.setFields(new HashMap<String, String>());
        commcareFixture.getFields().put("phone", PHONE_VALUE);

        when(commcareDataProvider.lookup("CaseInfo", referralLookup)).thenReturn(referralCaseInfo);
        when(commcareDataProvider.lookup("CaseInfo", motherLookup)).thenReturn(motherCaseInfo);
        when(commcareDataProvider.lookup("CommcareFixture", fixtureLookup)).thenReturn(commcareFixture);
        when(cmsliteDataProvider.lookup("StringContent", cmsliteLookup)).thenReturn(mesages.get("MotherReferralReminder_PHU"));

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

        String message = mesages.get("MotherReferralReminder_PHU").getValue();

        assertEquals(String.format(message, "mother_name"), event.getParameters().get(MESSAGE));
        assertEquals(Arrays.asList(PHONE_VALUE), event.getParameters().get(RECIPIENTS));
    }

    @Test
    public void shouldNotExecuteTaskIfPHUIdDoesNotExist() throws Exception {
        Map<String, String> referralLookup = new HashMap<>();
        referralLookup.put("id", REFERRAL_CASE_ID_VALUE);

        Map<String, String> motherLookup = new HashMap<>();
        motherLookup.put("id", MOTHER_CASE_ID_VALUE);

        Map<String, String> cmsliteLookup = new HashMap<>();
        cmsliteLookup.put("cmslite.dataname", "MotherReferralReminder");
        cmsliteLookup.put("cmslite.language", "English");

        referralCaseInfo = new CaseInfo();
        referralCaseInfo.setFieldValues(new HashMap<String, String>());
        referralCaseInfo.getFieldValues().put("open", "true");

        motherCaseInfo = new CaseInfo();
        motherCaseInfo.setFieldValues(new HashMap<String, String>());
        motherCaseInfo.getFieldValues().put("mother_name", "mother_name");

        when(commcareDataProvider.lookup("CaseInfo", referralLookup)).thenReturn(referralCaseInfo);
        when(commcareDataProvider.lookup("CaseInfo", motherLookup)).thenReturn(motherCaseInfo);
        when(cmsliteDataProvider.lookup("StringContent", cmsliteLookup)).thenReturn(mesages.get("MotherReferralReminder"));

        handler.handle(firedCampaignMessage());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(cmsliteDataProvider, never()).lookup(anyString(), anyMap());
    }

    @Test
    public void shouldNotExecuteTaskIfMotherNameNotExists() throws Exception {
        Map<String, String> referralLookup = new HashMap<>();
        referralLookup.put("id", REFERRAL_CASE_ID_VALUE);

        Map<String, String> motherLookup = new HashMap<>();
        motherLookup.put("id", MOTHER_CASE_ID_VALUE);

        Map<String, String> cmsliteLookup = new HashMap<>();
        cmsliteLookup.put("cmslite.dataname", "MotherReferralReminder");
        cmsliteLookup.put("cmslite.language", "English");

        referralCaseInfo = new CaseInfo();
        referralCaseInfo.setFieldValues(new HashMap<String, String>());
        referralCaseInfo.getFieldValues().put("open", "true");

        motherCaseInfo = new CaseInfo();
        motherCaseInfo.setFieldValues(new HashMap<String, String>());
        motherCaseInfo.getFieldValues().put("mother_phone_number", "0777777777");

        when(commcareDataProvider.lookup("CaseInfo", referralLookup)).thenReturn(referralCaseInfo);
        when(commcareDataProvider.lookup("CaseInfo", motherLookup)).thenReturn(motherCaseInfo);
        when(cmsliteDataProvider.lookup("StringContent", cmsliteLookup)).thenReturn(mesages.get("MotherReferralReminder"));

        handler.handle(firedCampaignMessage());

        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
        verify(cmsliteDataProvider, never()).lookup(anyString(), anyMap());
    }

    @Test
    public void shouldNotExecuteTaskIfNotOpen() throws Exception {
        Map<String, String> referralLookup = new HashMap<>();
        referralLookup.put("id", REFERRAL_CASE_ID_VALUE);

        referralCaseInfo = new CaseInfo();
        referralCaseInfo.setFieldValues(new HashMap<String, String>());
        referralCaseInfo.getFieldValues().put("open", "no");

        when(commcareDataProvider.lookup("CaseInfo", referralLookup)).thenReturn(referralCaseInfo);

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
                .withParameter(CAMPAIGN_NAME_KEY, MOTHER_REFERRAL_REMINDER)
                .withParameter(EXTERNAL_ID_KEY, EXTERNAL_ID_VALUE)
                .build();
    }

}
