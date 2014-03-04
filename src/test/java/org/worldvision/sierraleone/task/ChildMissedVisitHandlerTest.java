package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
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
import org.motechproject.commons.date.util.DateUtil;
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
import static org.worldvision.sierraleone.task.ConsecutiveMissedVisitListener.CHILD_VISITS_MESSAGE_NAME;

public class ChildMissedVisitHandlerTest {
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
    private CaseInfo childCase;

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
        String childCaseId = "childCaseId";
        String motherCaseId = "motherCaseId";
        String phuId = "phuId";
        String phone = "phone";
        String userId = "userId";
        String firstName = "firstName";
        String lastName = "lastName";
        DateTime lastVisitDate = DateUtil.now().minusMonths(3);
        DateTime aptDate1 = lastVisitDate.plusDays(7);
        DateTime aptDate2 = lastVisitDate.plusDays(14);
        DateTime aptDate3 = lastVisitDate.plusDays(21);
        DateTime aptDate4 = lastVisitDate.plusDays(28);
        DateTime aptDate5 = lastVisitDate.plusDays(35);
        DateTime aptDate6 = lastVisitDate.plusDays(42);
        DateTime aptDate7 = lastVisitDate.plusDays(49);
        DateTime aptDate8 = lastVisitDate.plusDays(56);
        DateTime aptDate9 = lastVisitDate.plusDays(63);
        DateTime aptDate10 = lastVisitDate.plusDays(70);

        Map<String, String> motherCaseFieldValues = new HashMap<>();
        motherCaseFieldValues.put(Commcare.PHU_ID, phuId);

        Map<String, String> childCaseFieldValues = new HashMap<>();
        childCaseFieldValues.put(Commcare.LAST_VISIT, lastVisitDate.toString("yyyy-MM-dd"));

        MotechEvent event = new MotechEvent();
        event.getParameters().put(EventKeys.CHILD_CASE_ID, childCaseId);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);
        event.getParameters().put(
                EventKeys.CHILD_VISIT_DATES,
                asList(
                        aptDate1, aptDate2, aptDate3, aptDate4, aptDate5, aptDate6, aptDate7,
                        aptDate8, aptDate9, aptDate10
                )
        );

        when(commcareCaseService.getCaseByCaseId(childCaseId)).thenReturn(childCase);
        when(commcareCaseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);

        when(motherCase.getFieldValues()).thenReturn(motherCaseFieldValues);
        when(motherCase.getUserId()).thenReturn(userId);
        when(childCase.getFieldValues()).thenReturn(childCaseFieldValues);

        when(fixtureIdMap.getPhoneForFixture(phuId)).thenReturn(phone);

        when(commcareUserService.getCommcareUserById(userId)).thenReturn(commcareUser);
        when(commcareUser.getFirstName()).thenReturn(firstName);
        when(commcareUser.getLastName()).thenReturn(lastName);

        when(settings.getLanguage()).thenReturn(LANGUAGE);
        when(cmsLiteService.getStringContent(LANGUAGE, CHILD_VISITS_MESSAGE_NAME)).thenReturn(new StringContent(LANGUAGE, CHILD_VISITS_MESSAGE_NAME, "%s"));

        listener.childMissedVisitHandler(event);

        ArgumentCaptor<SendSmsRequest> captor = ArgumentCaptor.forClass(SendSmsRequest.class);

        verify(smsService).sendSMS(captor.capture());

        SendSmsRequest value = captor.getValue();

        assertEquals(asList(phone), value.getRecipients());
        assertEquals(firstName + " " + lastName, value.getMessage());
    }

    @Test
    public void shouldNotSendSmsIfPhoneIsNotSet() throws Exception {
        String childCaseId = "childCaseId";
        String motherCaseId = "motherCaseId";
        String phuId = "phuId";
        DateTime lastVisitDate = DateUtil.now().minusMonths(3);
        DateTime aptDate1 = lastVisitDate.plusDays(7);
        DateTime aptDate2 = lastVisitDate.plusDays(14);
        DateTime aptDate3 = lastVisitDate.plusDays(21);
        DateTime aptDate4 = lastVisitDate.plusDays(28);
        DateTime aptDate5 = lastVisitDate.plusDays(35);
        DateTime aptDate6 = lastVisitDate.plusDays(42);
        DateTime aptDate7 = lastVisitDate.plusDays(49);
        DateTime aptDate8 = lastVisitDate.plusDays(56);
        DateTime aptDate9 = lastVisitDate.plusDays(63);
        DateTime aptDate10 = lastVisitDate.plusDays(70);

        Map<String, String> motherCaseFieldValues = new HashMap<>();
        motherCaseFieldValues.put(Commcare.PHU_ID, phuId);

        Map<String, String> childCaseFieldValues = new HashMap<>();
        childCaseFieldValues.put(Commcare.LAST_VISIT, lastVisitDate.toString("yyyy-MM-dd"));

        MotechEvent event = new MotechEvent();
        event.getParameters().put(EventKeys.CHILD_CASE_ID, childCaseId);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);
        event.getParameters().put(
                EventKeys.CHILD_VISIT_DATES,
                asList(
                        aptDate1, aptDate2, aptDate3, aptDate4, aptDate5, aptDate6, aptDate7,
                        aptDate8, aptDate9, aptDate10
                )
        );

        when(commcareCaseService.getCaseByCaseId(childCaseId)).thenReturn(childCase);
        when(commcareCaseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);

        when(motherCase.getFieldValues()).thenReturn(motherCaseFieldValues);
        when(childCase.getFieldValues()).thenReturn(childCaseFieldValues);

        when(fixtureIdMap.getPhoneForFixture(phuId)).thenReturn(null);

        listener.childMissedVisitHandler(event);

        verify(smsService, never()).sendSMS(any(SendSmsRequest.class));
    }

    @Test
    public void shouldNotSendSmsIfAppointmentsAreNotMissed() throws Exception {
        String childCaseId = "childCaseId";
        String motherCaseId = "motherCaseId";
        String phuId = "phuId";
        DateTime lastVisitDate = DateUtil.now().minusMonths(3);
        DateTime aptDate4 = lastVisitDate.plusMonths(4);
        DateTime aptDate5 = lastVisitDate.plusMonths(5);
        DateTime aptDate6 = lastVisitDate.plusMonths(6);
        DateTime aptDate7 = lastVisitDate.plusMonths(7);
        DateTime aptDate8 = lastVisitDate.plusMonths(8);
        DateTime aptDate9 = lastVisitDate.plusMonths(9);
        DateTime aptDate10 = lastVisitDate.plusMonths(10);

        Map<String, String> motherCaseFieldValues = new HashMap<>();
        motherCaseFieldValues.put(Commcare.PHU_ID, phuId);

        Map<String, String> childCaseFieldValues = new HashMap<>();
        childCaseFieldValues.put(Commcare.LAST_VISIT, lastVisitDate.toString("yyyy-MM-dd"));

        MotechEvent event = new MotechEvent();
        event.getParameters().put(EventKeys.CHILD_CASE_ID, childCaseId);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);
        event.getParameters().put(
                EventKeys.CHILD_VISIT_DATES,
                asList(aptDate4, aptDate5, aptDate6, aptDate7, aptDate8, aptDate9, aptDate10)
        );

        when(commcareCaseService.getCaseByCaseId(childCaseId)).thenReturn(childCase);
        when(commcareCaseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);

        when(motherCase.getFieldValues()).thenReturn(motherCaseFieldValues);
        when(childCase.getFieldValues()).thenReturn(childCaseFieldValues);

        listener.childMissedVisitHandler(event);

        verify(fixtureIdMap, never()).getPhoneForFixture(phuId);
        verify(smsService, never()).sendSMS(any(SendSmsRequest.class));
    }

    @Test
    public void shouldNotSendSmsIfPhuIDIsNull() throws Exception {
        String childCaseId = "childCaseId";
        String motherCaseId = "motherCaseId";

        Map<String, String> motherCaseFieldValues = new HashMap<>();
        motherCaseFieldValues.put(Commcare.PHU_ID, null);

        MotechEvent event = new MotechEvent();
        event.getParameters().put(EventKeys.CHILD_CASE_ID, childCaseId);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);

        when(commcareCaseService.getCaseByCaseId(childCaseId)).thenReturn(childCase);
        when(commcareCaseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);

        when(motherCase.getFieldValues()).thenReturn(motherCaseFieldValues);

        listener.childMissedVisitHandler(event);

        verify(fixtureIdMap, never()).getPhoneForFixture(anyString());
        verify(smsService, never()).sendSMS(any(SendSmsRequest.class));
    }

    @Test
    public void shouldNotSendSmsIfMotherCaseIsNull() throws Exception {
        String childCaseId = "childCaseId";
        String motherCaseId = "motherCaseId";

        MotechEvent event = new MotechEvent();
        event.getParameters().put(EventKeys.CHILD_CASE_ID, childCaseId);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);

        when(commcareCaseService.getCaseByCaseId(childCaseId)).thenReturn(childCase);

        listener.childMissedVisitHandler(event);

        verify(fixtureIdMap, never()).getPhoneForFixture(anyString());
        verify(smsService, never()).sendSMS(any(SendSmsRequest.class));
    }

    @Test
    public void shouldNotSendSmsIfChildCaseIsNull() throws Exception {
        String childCaseId = "childCaseId";
        String motherCaseId = "motherCaseId";

        MotechEvent event = new MotechEvent();
        event.getParameters().put(EventKeys.CHILD_CASE_ID, childCaseId);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);

        listener.childMissedVisitHandler(event);

        verify(commcareCaseService, never()).getCaseByCaseId(motherCaseId);
        verify(fixtureIdMap, never()).getPhoneForFixture(anyString());
        verify(smsService, never()).sendSMS(any(SendSmsRequest.class));
    }
}
