package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.event.MotechEvent;
import org.motechproject.messagecampaign.contract.CampaignRequest;
import org.motechproject.messagecampaign.service.MessageCampaignService;
import org.worldvision.sierraleone.constants.EventKeys;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class MotherReferralListenerTest {
    @Mock
    private MessageCampaignService messageCampaignService;

    private MotherReferralListener motherReferralListener;

    @Before
    public void setUp() {
        initMocks(this);

        motherReferralListener = new MotherReferralListener(messageCampaignService);
    }

    @Test
    public void shouldEnrollMother() {
        String motherCaseId = "motherCaseId";
        String referralCaseId = "referralCaseId";
        DateTime dateOfVisit = new DateTime(2013, 1, 1, 0, 0);

        MotechEvent event = new MotechEvent(EventKeys.MOTHER_REFERRAL_SUBJECT);
        event.getParameters().put(EventKeys.REFERRAL_CASE_ID, referralCaseId);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);
        event.getParameters().put(EventKeys.DATE_OF_VISIT, dateOfVisit);

        motherReferralListener.motherReferralReminder(event);

        ArgumentCaptor<CampaignRequest> cr = ArgumentCaptor.forClass(CampaignRequest.class);
        verify(messageCampaignService).startFor(cr.capture());

        CampaignRequest campaignRequest = cr.getValue();

        assertEquals(motherCaseId + ":" + referralCaseId, campaignRequest.externalId());
        assertEquals(MotherReferralListener.MOTHER_REFERRAL_REMINDER_CAMPAIGN, campaignRequest.campaignName());
        assertEquals(dateOfVisit.toLocalDate(), campaignRequest.referenceDate());
    }

    @Test
    public void shouldNotEnrollMotherIfDateOfVisitIsNull() {
        String motherCaseId = "motherCaseId";
        String referralCaseId = "referralCaseId";

        MotechEvent event = new MotechEvent(EventKeys.MOTHER_REFERRAL_SUBJECT);
        event.getParameters().put(EventKeys.REFERRAL_CASE_ID, referralCaseId);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);
        event.getParameters().put(EventKeys.DATE_OF_VISIT, null);

        motherReferralListener.motherReferralReminder(event);

        verify(messageCampaignService, never()).startFor(any(CampaignRequest.class));
    }
}
