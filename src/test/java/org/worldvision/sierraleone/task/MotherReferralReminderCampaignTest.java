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
import org.motechproject.sms.api.service.SendSmsRequest;
import org.motechproject.sms.api.service.SmsService;
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
public class MotherReferralReminderCampaignTest {
    @Mock
    private CommcareCaseService caseService;

    @Mock
    private MessageCampaignService messageCampaignService;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private MessageCampaignListener messageCampaignListener = new MessageCampaignListener();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void motherNotFoundInCommcare() {
        String motherCaseId = "motherCaseId";
        String referralCaseId = "referralCaseId";
        String externalId = motherCaseId + ":" + referralCaseId;

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.MOTHER_REFERRAL_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, externalId);

        when(caseService.getCaseByCaseId(motherCaseId)).thenReturn(null);

        messageCampaignListener.handle(event);

        verify(messageCampaignService, never()).stopAll(Matchers.any(CampaignRequest.class));
        verify(smsService, never()).sendSMS(Matchers.any(SendSmsRequest.class));
    }

    @Test
    public void referralNotFoundInCommcare() {
        String motherCaseId = "motherCaseId";
        String referralCaseId = "referralCaseId";
        String externalId = motherCaseId + ":" + referralCaseId;

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.MOTHER_REFERRAL_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, externalId);

        when(caseService.getCaseByCaseId(motherCaseId)).thenReturn(new CaseInfo());
        when(caseService.getCaseByCaseId(referralCaseId)).thenReturn(null);

        messageCampaignListener.handle(event);

        verify(messageCampaignService, never()).stopAll(Matchers.any(CampaignRequest.class));
        verify(smsService, never()).sendSMS(Matchers.any(SendSmsRequest.class));
    }

    @Test
    public void caseClosedUnenrollMother() {
        String motherCaseId = "motherCaseId";
        String referralCaseId = "referralCaseId";
        String externalId = motherCaseId + ":" + referralCaseId;

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.MOTHER_REFERRAL_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, externalId);

        CaseInfo motherCase = MotherCase(true);
        CaseInfo referralCase = ReferralCase("false");

        when(caseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);
        when(caseService.getCaseByCaseId(referralCaseId)).thenReturn(referralCase);

        messageCampaignListener.handle(event);

        ArgumentCaptor<CampaignRequest> cr = ArgumentCaptor.forClass(CampaignRequest.class);
        verify(messageCampaignService, times(1)).stopAll(cr.capture());
        verify(smsService, never()).sendSMS(Matchers.any(SendSmsRequest.class));

        CampaignRequest campaignRequest = cr.getValue();

        assertEquals("ExternalId does not match", externalId, campaignRequest.externalId());
        assertEquals("Campaign Name does not match", Campaign.MOTHER_REFERRAL_REMINDER_CAMPAIGN, campaignRequest.campaignName());
    }

    @Test
    public void caseOpenSendSMS() {
        String motherCaseId = "motherCaseId";
        String referralCaseId = "referralCaseId";
        String externalId = motherCaseId + ":" + referralCaseId;

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.MOTHER_REFERRAL_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, externalId);

        CaseInfo motherCase = MotherCase(true);
        CaseInfo referralCase = ReferralCase("true");

        when(caseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);
        when(caseService.getCaseByCaseId(referralCaseId)).thenReturn(referralCase);

        messageCampaignListener.handle(event);

        verify(messageCampaignService, never()).stopAll(Matchers.any(CampaignRequest.class));
        verify(smsService, times(1)).sendSMS(Matchers.any(SendSmsRequest.class));
    }

    @Test
    public void noPhoneNoAction() {
        String motherCaseId = "motherCaseId";
        String referralCaseId = "referralCaseId";
        String externalId = motherCaseId + ":" + referralCaseId;

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.MOTHER_REFERRAL_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, externalId);

        CaseInfo motherCase = MotherCase(false);
        CaseInfo referralCase = ReferralCase("true");

        when(caseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);
        when(caseService.getCaseByCaseId(referralCaseId)).thenReturn(referralCase);

        messageCampaignListener.handle(event);

        verify(messageCampaignService, never()).stopAll(Matchers.any(CampaignRequest.class));
        verify(smsService, never()).sendSMS(Matchers.any(SendSmsRequest.class));
    }

    private CaseInfo MotherCase(boolean includePhone) {
        CaseInfo motherCase = new CaseInfo();
        Map<String, String> fieldValues = new HashMap<String, String>();

        if (includePhone) {
            fieldValues.put(Commcare.MOTHER_PHONE_NUMBER, "011 111 111");
        } else {
            fieldValues.put(Commcare.MOTHER_PHONE_NUMBER, null);
        }

        motherCase.setFieldValues(fieldValues);

        return motherCase;
    }

    private CaseInfo ReferralCase(String isOpen) {
        CaseInfo referralCase = new CaseInfo();
        Map<String, String> fieldValues = new HashMap<String, String>();
        fieldValues.put(Commcare.OPEN, isOpen);

        referralCase.setFieldValues(fieldValues);

        return referralCase;
    }
}
