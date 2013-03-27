package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.motechproject.event.MotechEvent;
import org.motechproject.messagecampaign.contract.CampaignRequest;
import org.motechproject.messagecampaign.service.MessageCampaignService;
import org.worldvision.sierraleone.constants.Campaign;
import org.worldvision.sierraleone.constants.EventKeys;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests!
 */
public class MotherReferralListenerTest {
    @Mock
    private MessageCampaignService messageCampaignService;

    @InjectMocks
    private MotherReferralListener motherReferralListener = new MotherReferralListener();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void verifyMotherCorrectlyEnrolled() {
        String motherCaseId = "motherCaseId";
        String referralCaseId = "referralCaseId";
        DateTime dateOfVisit = new DateTime(2013, 1, 1, 0, 0);

        MotechEvent event = new MotechEvent(EventKeys.MOTHER_REFERRAL_SUBJECT);
        event.getParameters().put(EventKeys.REFERRAL_CASE_ID, referralCaseId);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);
        event.getParameters().put(EventKeys.DATE_OF_VISIT, dateOfVisit);

        motherReferralListener.motherReferralReminder(event);

        ArgumentCaptor<CampaignRequest> cr = ArgumentCaptor.forClass(CampaignRequest.class);
        verify(messageCampaignService, times(1)).startFor(cr.capture());

        CampaignRequest campaignRequest = cr.getValue();

        assertEquals("ExternalId does not match", (motherCaseId + ":" + referralCaseId), campaignRequest.externalId());
        assertEquals("Campaign Name does not match", Campaign.MOTHER_REFERRAL_REMINDER_CAMPAIGN, campaignRequest.campaignName());
        assertEquals("Reference Date does not match", dateOfVisit.toLocalDate(), campaignRequest.referenceDate());
    }
}
