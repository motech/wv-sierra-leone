package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.motechproject.commcare.service.CommcareFormService;
import org.motechproject.event.MotechEvent;
import org.motechproject.messagecampaign.contract.CampaignRequest;
import org.motechproject.messagecampaign.service.MessageCampaignService;
import org.worldvision.sierraleone.constants.EventKeys;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests!
 */
public class ChildVisitListenerTest {
    @Mock
    private CommcareFormService commcareFormService;

    @Mock
    private MessageCampaignService messageCampaignService;

    @InjectMocks
    private ChildVisitListener childVisitListener = new ChildVisitListener();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void childWithoutVitaminAShouldBeEnrolled() {
        MotechEvent event = new MotechEvent(EventKeys.CHILD_VISIT_FORM_SUBJECT);
        event.getParameters().put(EventKeys.CHILD_CASE_ID, "childCaseId");
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, "motherCaseId");
        event.getParameters().put(EventKeys.VITAMIN_A, "no");
        event.getParameters().put(EventKeys.DATE_OF_BIRTH, new DateTime(2013, 1, 1, 0, 0));

        childVisitListener.childVitaminAReminder(event);

        verify(messageCampaignService, times(1)).startFor(Matchers.any(CampaignRequest.class));
    }

    @Test
    public void childWithVitaminAShouldNotBeEnrolled() {
        MotechEvent event = new MotechEvent(EventKeys.CHILD_VISIT_FORM_SUBJECT);
        event.getParameters().put(EventKeys.CHILD_CASE_ID, "childCaseId");
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, "motherCaseId");
        event.getParameters().put(EventKeys.VITAMIN_A, "yes");
        event.getParameters().put(EventKeys.DATE_OF_BIRTH, new DateTime(2013, 1, 1, 0, 0));

        childVisitListener.childVitaminAReminder(event);

        verify(messageCampaignService, never()).startFor(Matchers.any(CampaignRequest.class));
    }
}
