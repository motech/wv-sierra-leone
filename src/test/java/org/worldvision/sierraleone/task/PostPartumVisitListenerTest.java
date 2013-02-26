package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.motechproject.commcare.service.CommcareFormService;
import org.motechproject.event.MotechEvent;
import org.motechproject.server.messagecampaign.contract.CampaignRequest;
import org.motechproject.server.messagecampaign.service.MessageCampaignService;
import org.worldvision.sierraleone.constants.Campaign;
import org.worldvision.sierraleone.constants.EventKeys;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests!
 */
public class PostPartumVisitListenerTest {
    @Mock
    private CommcareFormService commcareFormService;

    @Mock
    private MessageCampaignService messageCampaignService;

    @InjectMocks
    private PostPartumVisitListener postPartumVisitListener = new PostPartumVisitListener();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void motherShouldBeEnrolledForReminders() {
        String gaveBirth = "yes";
        String attendedPostnatal = "no";
        String motherCaseId = "motherCaseId";
        DateTime dateOfBirth = new DateTime(2013, 1, 1, 0, 0);
        Integer daysSinceBirth = new Integer(1);

        MotechEvent event = new MotechEvent(EventKeys.POST_PARTUM_FORM_SUBJECT);
        event.getParameters().put(EventKeys.GAVE_BIRTH, gaveBirth);
        event.getParameters().put(EventKeys.ATTENDED_POSTNATAL, attendedPostnatal);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);
        event.getParameters().put(EventKeys.DATE_OF_BIRTH, dateOfBirth);
        event.getParameters().put(EventKeys.DAYS_SINCE_BIRTH, daysSinceBirth);

        postPartumVisitListener.postnatalConsultationAttendance(event);

        ArgumentCaptor<CampaignRequest> cr = ArgumentCaptor.forClass(CampaignRequest.class);
        verify(messageCampaignService, times(1)).startFor(cr.capture());

        CampaignRequest campaignRequest = cr.getValue();

        assertEquals("ExternalId does not match", motherCaseId, campaignRequest.externalId());
        assertEquals("Campaign Name does not match", Campaign.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN, campaignRequest.campaignName());
        assertEquals("Reference Date does not match", dateOfBirth.toLocalDate(), campaignRequest.referenceDate());
    }

    @Test
    public void motherShouldNotBeEnrolledForRemindersDidNotGiveBirth() {
        String gaveBirth = "no";
        String attendedPostnatal = "no";
        String motherCaseId = "motherCaseId";
        DateTime dateOfBirth = new DateTime(2013, 1, 1, 0, 0);
        Integer daysSinceBirth = new Integer(1);

        MotechEvent event = new MotechEvent(EventKeys.POST_PARTUM_FORM_SUBJECT);
        event.getParameters().put(EventKeys.GAVE_BIRTH, gaveBirth);
        event.getParameters().put(EventKeys.ATTENDED_POSTNATAL, attendedPostnatal);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);
        event.getParameters().put(EventKeys.DATE_OF_BIRTH, dateOfBirth);
        event.getParameters().put(EventKeys.DAYS_SINCE_BIRTH, daysSinceBirth);

        postPartumVisitListener.postnatalConsultationAttendance(event);

        verify(messageCampaignService, never()).startFor(Matchers.any(CampaignRequest.class));

        // also test with null
        event.getParameters().put(EventKeys.GAVE_BIRTH, null);

        postPartumVisitListener.postnatalConsultationAttendance(event);

        verify(messageCampaignService, never()).startFor(Matchers.any(CampaignRequest.class));
    }

    @Test
    public void motherShouldNotBeEnrolledForRemindersAttendedPostnatal() {
        String gaveBirth = "yes";
        String attendedPostnatal = "yes";
        String motherCaseId = "motherCaseId";
        DateTime dateOfBirth = new DateTime(2013, 1, 1, 0, 0);
        Integer daysSinceBirth = new Integer(1);

        MotechEvent event = new MotechEvent(EventKeys.POST_PARTUM_FORM_SUBJECT);
        event.getParameters().put(EventKeys.GAVE_BIRTH, gaveBirth);
        event.getParameters().put(EventKeys.ATTENDED_POSTNATAL, attendedPostnatal);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);
        event.getParameters().put(EventKeys.DATE_OF_BIRTH, dateOfBirth);
        event.getParameters().put(EventKeys.DAYS_SINCE_BIRTH, daysSinceBirth);

        postPartumVisitListener.postnatalConsultationAttendance(event);

        verify(messageCampaignService, never()).startFor(Matchers.any(CampaignRequest.class));
    }

    @Test
    public void motherShouldNotBeEnrolledForRemindersTooLate() {
        String gaveBirth = "yes";
        String attendedPostnatal = "no";
        String motherCaseId = "motherCaseId";
        DateTime dateOfBirth = new DateTime(2013, 1, 1, 0, 0);
        Integer daysSinceBirth = new Integer(45);

        MotechEvent event = new MotechEvent(EventKeys.POST_PARTUM_FORM_SUBJECT);
        event.getParameters().put(EventKeys.GAVE_BIRTH, gaveBirth);
        event.getParameters().put(EventKeys.ATTENDED_POSTNATAL, attendedPostnatal);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);
        event.getParameters().put(EventKeys.DATE_OF_BIRTH, dateOfBirth);
        event.getParameters().put(EventKeys.DAYS_SINCE_BIRTH, daysSinceBirth);

        postPartumVisitListener.postnatalConsultationAttendance(event);

        verify(messageCampaignService, never()).startFor(Matchers.any(CampaignRequest.class));

        daysSinceBirth = new Integer(46);
        event.getParameters().put(EventKeys.DAYS_SINCE_BIRTH, daysSinceBirth);

        postPartumVisitListener.postnatalConsultationAttendance(event);

        verify(messageCampaignService, never()).startFor(Matchers.any(CampaignRequest.class));
    }
}
