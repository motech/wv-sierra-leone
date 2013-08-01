package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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
import org.motechproject.messagecampaign.EventKeys;
import org.motechproject.messagecampaign.contract.CampaignRequest;
import org.motechproject.messagecampaign.service.MessageCampaignService;
import org.worldvision.sierraleone.WorldVisionSettings;
import org.worldvision.sierraleone.constants.Campaign;
import org.worldvision.sierraleone.constants.Commcare;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ChildVitaminAReminderCampaignTest {
    @Mock
    private CommcareCaseService caseService;

    @Mock
    private MessageCampaignService messageCampaignService;

    @InjectMocks
    private MessageCampaignListener messageCampaignListener = new MessageCampaignListener();

    @Mock
    private WorldVisionSettings settings;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void childNotFoundInCommcare() throws Exception {
        String motherCaseId = "motherCaseId";
        String childCaseId = "childCaseId";
        String externalId = motherCaseId + ":" + childCaseId;

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, externalId);

        when(caseService.getCaseByCaseId(childCaseId)).thenReturn(null);

        messageCampaignListener.handle(event);

        verify(messageCampaignService, never()).stopAll(Matchers.any(CampaignRequest.class));
        //verify(smsService, never()).sendSMS(Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void childHadVitaminASoUnenroll() throws Exception {
        String motherCaseId = "motherCaseId";
        String childCaseId = "childCaseId";
        String externalId = childCaseId + ":" + motherCaseId;

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, externalId);

        CaseInfo childCase = ChildCase("yes", 2);

        when(caseService.getCaseByCaseId(childCaseId)).thenReturn(childCase);

        messageCampaignListener.handle(event);

        ArgumentCaptor<CampaignRequest> cr = ArgumentCaptor.forClass(CampaignRequest.class);
        verify(messageCampaignService, times(1)).stopAll(cr.capture());
        //verify(smsService, never()).sendSMS(Matchers.anyString(), Matchers.anyString());

        CampaignRequest campaignRequest = cr.getValue();

        assertEquals("ExternalId does not match", externalId, campaignRequest.externalId());
        assertEquals("Campaign Name does not match", Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN, campaignRequest.campaignName());
    }


    @Test
    public void childTooOldAtLimitSoUnenroll() throws Exception {
        String motherCaseId = "motherCaseId";
        String childCaseId = "childCaseId";
        String externalId = childCaseId + ":" + motherCaseId;

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, externalId);

        // Test at limit
        CaseInfo childCase = ChildCase("yes", 6);

        when(caseService.getCaseByCaseId(childCaseId)).thenReturn(childCase);

        messageCampaignListener.handle(event);

        ArgumentCaptor<CampaignRequest> cr = ArgumentCaptor.forClass(CampaignRequest.class);
        verify(messageCampaignService, times(1)).stopAll(cr.capture());
        //verify(smsService, never()).sendSMS(Matchers.anyString(), Matchers.anyString());

        CampaignRequest campaignRequest = cr.getValue();

        assertEquals("ExternalId does not match", externalId, campaignRequest.externalId());
        assertEquals("Campaign Name does not match", Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN, campaignRequest.campaignName());
    }

    @Test
    public void childTooOldOverLimitSoUnenroll() throws Exception {
        String motherCaseId = "motherCaseId";
        String childCaseId = "childCaseId";
        String externalId = childCaseId + ":" + motherCaseId;

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, externalId);

        // Test at limit
        CaseInfo childCase = ChildCase("yes", 7);

        when(caseService.getCaseByCaseId(childCaseId)).thenReturn(childCase);

        messageCampaignListener.handle(event);

        ArgumentCaptor<CampaignRequest> cr = ArgumentCaptor.forClass(CampaignRequest.class);
        verify(messageCampaignService, times(1)).stopAll(cr.capture());
        //verify(smsService, never()).sendSMS(Matchers.anyString(), Matchers.anyString());

        CampaignRequest campaignRequest = cr.getValue();

        assertEquals("ExternalId does not match", externalId, campaignRequest.externalId());
        assertEquals("Campaign Name does not match", Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN, campaignRequest.campaignName());
    }

    @Test
    public void shoulNotdSendReminderToMotherButMotherNotFound() throws Exception {
        String motherCaseId = "motherCaseId";
        String childCaseId = "childCaseId";
        String externalId = childCaseId + ":" + motherCaseId;

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, externalId);

        CaseInfo childCase = ChildCase("no", 2);

        when(caseService.getCaseByCaseId(childCaseId)).thenReturn(childCase);
        when(caseService.getCaseByCaseId(motherCaseId)).thenReturn(null);

        messageCampaignListener.handle(event);

        verify(messageCampaignService, never()).stopAll(Matchers.any(CampaignRequest.class));
        //verify(smsService, never()).sendSMS(Matchers.anyString(), Matchers.anyString());
    }

    @Ignore
    public void shouldNotSendReminderToMotherButNoPhone() throws Exception {
        String motherCaseId = "motherCaseId";
        String childCaseId = "childCaseId";
        String externalId = childCaseId + ":" + motherCaseId;

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, externalId);

        CaseInfo childCase = ChildCase("no", 2);
        CaseInfo motherCase = MotherCase(false);

        when(caseService.getCaseByCaseId(childCaseId)).thenReturn(childCase);
        when(caseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);

        messageCampaignListener.handle(event);

        verify(messageCampaignService, never()).stopAll(Matchers.any(CampaignRequest.class));
        //verify(smsService, times(1)).sendSMS(Matchers.anyString(), Matchers.anyString());
    }

    @Ignore
    public void shouldSendReminderToMother() throws Exception {
        String motherCaseId = "motherCaseId";
        String childCaseId = "childCaseId";
        String externalId = childCaseId + ":" + motherCaseId;

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, externalId);

        CaseInfo childCase = ChildCase("no", 2);
        CaseInfo motherCase = MotherCase(true);

        when(caseService.getCaseByCaseId(childCaseId)).thenReturn(childCase);
        when(caseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);

        messageCampaignListener.handle(event);

        verify(messageCampaignService, never()).stopAll(Matchers.any(CampaignRequest.class));
        //verify(smsService, times(1)).sendSMS(Matchers.anyString(), Matchers.anyString());
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

    private CaseInfo ChildCase(String hadVitaminA, int ageInMonths) {
        CaseInfo childCase = new CaseInfo();
        Map<String, String> fieldValues = new HashMap<String, String>();
        fieldValues.put(Commcare.VITAMIN_A, hadVitaminA);

        DateTime now = new DateTime();
        DateTime dateOfBirth = now.minusMonths(ageInMonths);

        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
        String formattedDate = dateOfBirth.toString(fmt);

        fieldValues.put(Commcare.DATE_OF_BIRTH, formattedDate);

        childCase.setFieldValues(fieldValues);

        return childCase;
    }

}
