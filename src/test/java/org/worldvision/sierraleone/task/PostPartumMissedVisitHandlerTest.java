package org.worldvision.sierraleone.task;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.cmslite.api.model.StringContent;
import org.motechproject.cmslite.api.service.CMSLiteService;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.domain.CommcareUser;
import org.motechproject.commcare.service.CommcareCaseService;
import org.motechproject.commcare.service.CommcareUserService;
import org.motechproject.event.MotechEvent;
import org.motechproject.sms.api.service.SendSmsRequest;
import org.motechproject.sms.api.service.SmsService;
import org.worldvision.sierraleone.WorldVisionSettings;
import org.worldvision.sierraleone.constants.Commcare;
import org.worldvision.sierraleone.constants.EventKeys;
import org.worldvision.sierraleone.repository.FixtureIdMap;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.worldvision.sierraleone.task.ConsecutiveMissedVisitListener.POST_PARTUM_VISITS_MESSAGE_NAME;

public class PostPartumMissedVisitHandlerTest {
    private static final String LANGUAGE = "English";

    @Mock
    private CommcareCaseService commcareCaseService;

    @Mock
    private CommcareUserService commcareUserService;

    @Mock
    private FixtureIdMap fixtureIdMap;

    @Mock
    private SmsService smsService;

    @Mock
    private CMSLiteService cmsLiteService;

    @Mock
    private WorldVisionSettings settings;

    @Mock
    private CaseInfo motherCase;

    @Mock
    private CommcareUser commcareUser;

    private ConsecutiveMissedVisitListener listener;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        listener = new ConsecutiveMissedVisitListener(
                commcareCaseService, commcareUserService,
                fixtureIdMap, smsService, cmsLiteService, settings
        );
    }

    @Test
    public void shouldSendSmsWhenTwoAppointmentsAreMissed() throws Exception {
        String motechCaseId = "motechCaseId";
        String phuId = "phuId";
        String phone = "phone";
        String userId = "userId";
        String firstName = "firstName";
        String lastName = "lastName";

        Map<String, String> motherCaseFieldValues = new HashMap<>();
        motherCaseFieldValues.put(Commcare.PHU_ID, phuId);

        MotechEvent event = new MotechEvent();
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motechCaseId);

        when(commcareCaseService.getCaseByCaseId(motechCaseId)).thenReturn(motherCase);
        when(motherCase.getFieldValues()).thenReturn(motherCaseFieldValues);
        when(motherCase.getUserId()).thenReturn(userId);

        when(fixtureIdMap.getPhoneForFixture(phuId)).thenReturn(phone);

        when(commcareUserService.getCommcareUserById(userId)).thenReturn(commcareUser);
        when(commcareUser.getFirstName()).thenReturn(firstName);
        when(commcareUser.getLastName()).thenReturn(lastName);

        when(settings.getLanguage()).thenReturn(LANGUAGE);
        when(cmsLiteService.getStringContent(LANGUAGE, POST_PARTUM_VISITS_MESSAGE_NAME)).thenReturn(new StringContent(LANGUAGE, POST_PARTUM_VISITS_MESSAGE_NAME, "%s"));

        listener.postPartumMissedVisitHandler(event);

        ArgumentCaptor<SendSmsRequest> captor = ArgumentCaptor.forClass(SendSmsRequest.class);

        verify(smsService).sendSMS(captor.capture());

        SendSmsRequest value = captor.getValue();

        assertEquals(asList(phone), value.getRecipients());
        assertEquals(firstName + " " + lastName, value.getMessage());
    }

    @Test
    public void shouldNotSendSmsIfPhoneIsNotSet() throws Exception {
        String motechCaseId = "motechCaseId";
        String phuId = "phuId";

        Map<String, String> motherCaseFieldValues = new HashMap<>();
        motherCaseFieldValues.put(Commcare.PHU_ID, phuId);

        MotechEvent event = new MotechEvent();
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motechCaseId);

        when(commcareCaseService.getCaseByCaseId(motechCaseId)).thenReturn(motherCase);
        when(motherCase.getFieldValues()).thenReturn(motherCaseFieldValues);

        when(fixtureIdMap.getPhoneForFixture(phuId)).thenReturn(null);

        listener.postPartumMissedVisitHandler(event);

        verify(smsService, never()).sendSMS(any(SendSmsRequest.class));
    }

    @Test
    public void shouldNotSendSmsIfPhuIDIsNull() throws Exception {
        String motechCaseId = "motechCaseId";

        Map<String, String> motherCaseFieldValues = new HashMap<>();
        motherCaseFieldValues.put(Commcare.PHU_ID, null);

        MotechEvent event = new MotechEvent();
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motechCaseId);

        when(commcareCaseService.getCaseByCaseId(motechCaseId)).thenReturn(motherCase);
        when(motherCase.getFieldValues()).thenReturn(motherCaseFieldValues);

        listener.postPartumMissedVisitHandler(event);

        verify(fixtureIdMap, never()).getPhoneForFixture(anyString());
        verify(smsService, never()).sendSMS(any(SendSmsRequest.class));
    }

    @Test
    public void shouldNotSendSmsIfMotherCaseIsNull() throws Exception {
        String motechCaseId = "motechCaseId";

        MotechEvent event = new MotechEvent();
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motechCaseId);

        when(commcareCaseService.getCaseByCaseId(motechCaseId)).thenReturn(null);

        listener.postPartumMissedVisitHandler(event);

        verify(fixtureIdMap, never()).getPhoneForFixture(anyString());
        verify(smsService, never()).sendSMS(any(SendSmsRequest.class));
    }

}
