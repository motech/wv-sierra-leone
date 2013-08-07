package org.worldvision.sierraleone.task;

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

/**
 * Tests!
 */
public class PostnatalConsultationReminderCampaignTest {
    @Mock
    private CommcareCaseService caseService;

    @Mock
    private MessageCampaignService messageCampaignService;

    @Mock
    private WorldVisionSettings settings;

    @InjectMocks
    private MessageCampaignListener messageCampaignListener = new MessageCampaignListener();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void motherNotFoundInCommcare() throws Exception {
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
    public void shouldSendMotherSMS() throws Exception {
        String motherCaseId = "motherCaseId";

        MotechEvent event = new MotechEvent(EventKeys.SEND_MESSAGE);
        event.getParameters().put(EventKeys.CAMPAIGN_NAME_KEY, Campaign.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN);
        event.getParameters().put(EventKeys.EXTERNAL_ID_KEY, motherCaseId);

        CaseInfo motherCase = new CaseInfo();
        Map<String, String> fieldValues = new HashMap<String, String>();
        fieldValues.put(Commcare.STILL_ALIVE, "yes");
        fieldValues.put(Commcare.ATTENDED_POSTNATAL, "no");
        fieldValues.put(Commcare.MOTHER_PHONE_NUMBER, "011 111 111");

        motherCase.setFieldValues(fieldValues);

        when(caseService.getCaseByCaseId(motherCaseId)).thenReturn(motherCase);

        messageCampaignListener.handle(event);

        verify(messageCampaignService, never()).stopAll(Matchers.any(CampaignRequest.class));
        //verify(smsService, never()).sendSMS(Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void noPhoneNoAction() throws Exception {
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

}
