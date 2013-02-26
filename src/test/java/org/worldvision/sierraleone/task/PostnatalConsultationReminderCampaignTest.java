package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.service.CommcareCaseService;
import org.motechproject.event.MotechEvent;
import org.motechproject.server.messagecampaign.EventKeys;
import org.motechproject.server.messagecampaign.contract.CampaignRequest;
import org.motechproject.server.messagecampaign.service.MessageCampaignService;
import org.worldvision.sierraleone.constants.Campaign;
import org.worldvision.sierraleone.constants.Commcare;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests!
 */
public class PostnatalConsultationReminderCampaignTest {
    @Mock
    private CommcareCaseService caseService;

    @Mock
    private MessageCampaignService messageCampaignService;

    @InjectMocks
    private MessageCampaignListener messageCampaignListener = new MessageCampaignListener();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void motherNotFoundInCommcare() {
        String motherCaseId = "motherCaseId";

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, motherCaseId);

        when(caseService.getCaseByCaseId(motherCaseId)).thenReturn(null);

        messageCampaignListener.handle(event);

        verify(messageCampaignService, never()).stopAll(Matchers.any(CampaignRequest.class));
        //verify(smsService, never()).sendSMS(Matchers.anyString(), Matchers.anyString());
    }

    @Ignore
    public void shouldSendMotherSMS() {
        String motherCaseId = "motherCaseId";

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, motherCaseId);

        CaseInfo motherCase = new CaseInfo();
        Map<String, String> fieldValues = new HashMap<String, String>();
        fieldValues.put(Commcare.STILL_ALIVE, "yes");
        fieldValues.put(Commcare.ATTENDED_POSTNATAL, "no");
        fieldValues.put(Commcare.MOTHER_PHONE_NUMBER, "999999999");

        motherCase.setFieldValues(fieldValues);

        when(caseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);

        messageCampaignListener.handle(event);

        verify(messageCampaignService, never()).stopAll(Matchers.any(CampaignRequest.class));
        //verify(smsService, never()).sendSMS(Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void noPhoneNoAction() {
        String motherCaseId = "motherCaseId";

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, motherCaseId);

        CaseInfo motherCase = new CaseInfo();
        Map<String, String> fieldValues = new HashMap<String, String>();
        fieldValues.put(Commcare.STILL_ALIVE, "yes");
        fieldValues.put(Commcare.ATTENDED_POSTNATAL, "no");
        fieldValues.put(Commcare.MOTHER_PHONE_NUMBER, null);

        motherCase.setFieldValues(fieldValues);

        when(caseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);

        messageCampaignListener.handle(event);

        verify(messageCampaignService, never()).stopAll(Matchers.any(CampaignRequest.class));
        //verify(smsService, never()).sendSMS(Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void shouldUnenrollMotherNoLongerAlive() {
        String motherCaseId = "motherCaseId";

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, motherCaseId);

        CaseInfo motherCase = new CaseInfo();
        Map<String, String> fieldValues = new HashMap<String, String>();
        fieldValues.put(Commcare.STILL_ALIVE, "no");
        fieldValues.put(Commcare.ATTENDED_POSTNATAL, "no");
        fieldValues.put(Commcare.MOTHER_PHONE_NUMBER, "9999999");

        motherCase.setFieldValues(fieldValues);

        when(caseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);

        messageCampaignListener.handle(event);

        ArgumentCaptor<CampaignRequest> cr = ArgumentCaptor.forClass(CampaignRequest.class);
        verify(messageCampaignService, times(1)).stopAll(cr.capture());
        //verify(smsService, never()).sendSMS(Matchers.anyString(), Matchers.anyString());

        CampaignRequest campaignRequest = cr.getValue();

        assertEquals("ExternalId does not match", motherCaseId, campaignRequest.externalId());
        assertEquals("Campaign Name does not match", Campaign.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN, campaignRequest.campaignName());
    }

    @Test
    public void shouldUnenrollMotherAttendedPostnatal() {
        String motherCaseId = "motherCaseId";

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, motherCaseId);

        CaseInfo motherCase = new CaseInfo();
        Map<String, String> fieldValues = new HashMap<String, String>();
        fieldValues.put(Commcare.STILL_ALIVE, "yes");
        fieldValues.put(Commcare.ATTENDED_POSTNATAL, "yes");
        fieldValues.put(Commcare.MOTHER_PHONE_NUMBER, "9999999");

        motherCase.setFieldValues(fieldValues);

        when(caseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);

        messageCampaignListener.handle(event);

        ArgumentCaptor<CampaignRequest> cr = ArgumentCaptor.forClass(CampaignRequest.class);
        verify(messageCampaignService, times(1)).stopAll(cr.capture());
        //verify(smsService, never()).sendSMS(Matchers.anyString(), Matchers.anyString());

        CampaignRequest campaignRequest = cr.getValue();

        assertEquals("ExternalId does not match", motherCaseId, campaignRequest.externalId());
        assertEquals("Campaign Name does not match", Campaign.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN, campaignRequest.campaignName());
    }

    @Test
    public void shouldUnenrollMother() {
        String motherCaseId = "motherCaseId";

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, motherCaseId);

        CaseInfo motherCase = new CaseInfo();
        Map<String, String> fieldValues = new HashMap<String, String>();
        fieldValues.put(Commcare.STILL_ALIVE, "no");
        fieldValues.put(Commcare.ATTENDED_POSTNATAL, "yes");
        fieldValues.put(Commcare.MOTHER_PHONE_NUMBER, "9999999");

        motherCase.setFieldValues(fieldValues);

        when(caseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);

        messageCampaignListener.handle(event);

        ArgumentCaptor<CampaignRequest> cr = ArgumentCaptor.forClass(CampaignRequest.class);
        verify(messageCampaignService, times(1)).stopAll(cr.capture());
        //verify(smsService, never()).sendSMS(Matchers.anyString(), Matchers.anyString());

        CampaignRequest campaignRequest = cr.getValue();

        assertEquals("ExternalId does not match", motherCaseId, campaignRequest.externalId());
        assertEquals("Campaign Name does not match", Campaign.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN, campaignRequest.campaignName());
    }
}
