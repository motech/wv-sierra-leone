package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.commons.date.util.DateUtil;
import org.motechproject.event.MotechEvent;
import org.motechproject.messagecampaign.dao.AllCampaignEnrollments;
import org.motechproject.messagecampaign.dao.AllMessageCampaigns;
import org.motechproject.messagecampaign.domain.campaign.CampaignEnrollment;
import org.motechproject.messagecampaign.loader.CampaignJsonLoader;
import org.motechproject.messagecampaign.service.MessageCampaignService;
import org.motechproject.messagecampaign.userspecified.CampaignRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.worldvision.sierraleone.constants.EventKeys;

import java.io.InputStream;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.worldvision.sierraleone.constants.EventKeys.MOTHER_REFERRAL_SUBJECT;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath*:/META-INF/motech/applicationMessageCampaign.xml",
        "classpath*:/META-INF/motech/activemqConnection.xml",
        "classpath*:/META-INF/motech/eventQueuePublisher.xml",
        "classpath*:/META-INF/motech/eventQueueConsumer.xml",
        "classpath*:/META-INF/motech/applicationPlatformConfig.xml",
        "classpath*:/META-INF/motech/applicationCommonsCouchdbContext.xml",
        "classpath*:/META-INF/motech/applicationScheduler.xml"
})
public class MotherReferralListenerIT {

    @Autowired
    private MessageCampaignService messageCampaignService;

    @Autowired
    private AllMessageCampaigns allMessageCampaigns;

    @Autowired
    private AllCampaignEnrollments allCampaignEnrollments;

    private MotherReferralListener motherReferralListener;

    @Before
    public void setUp() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("message-campaigns.json");
        List<CampaignRecord> records = new CampaignJsonLoader().loadCampaigns(stream);

        for (CampaignRecord record : records) {
            allMessageCampaigns.saveOrUpdate(record);
        }

        motherReferralListener = new MotherReferralListener(messageCampaignService);
    }

    @After
    public void tearDown() throws Exception {
        allMessageCampaigns.removeAll();
        allCampaignEnrollments.removeAll();
    }

    @Test
    public void shouldEnrollMother() throws InterruptedException {
        String motherCaseId = "motherCaseId";
        String referralCaseId = "referralCaseId";
        DateTime dateOfVisit = DateUtil.now().plusDays(1);

        MotechEvent event = new MotechEvent(MOTHER_REFERRAL_SUBJECT);
        event.getParameters().put(EventKeys.REFERRAL_CASE_ID, referralCaseId);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);
        event.getParameters().put(EventKeys.DATE_OF_VISIT, dateOfVisit);

        motherReferralListener.motherReferralReminder(event);

        List<CampaignEnrollment> enrollments = allCampaignEnrollments.getAll();

        assertEquals(1, enrollments.size());

        CampaignEnrollment enrollment = enrollments.get(0);

        assertEquals(motherCaseId + ":" + referralCaseId, enrollment.getExternalId());
        assertEquals(MotherReferralListener.MOTHER_REFERRAL_REMINDER_CAMPAIGN, enrollment.getCampaignName());
        assertEquals(dateOfVisit.toLocalDate(), enrollment.getReferenceDate());
    }

    @Test
    public void shouldNotEnrollMotherIfDateOfVisitIsNull() {
        String motherCaseId = "motherCaseId";
        String referralCaseId = "referralCaseId";

        MotechEvent event = new MotechEvent(MOTHER_REFERRAL_SUBJECT);
        event.getParameters().put(EventKeys.REFERRAL_CASE_ID, referralCaseId);
        event.getParameters().put(EventKeys.MOTHER_CASE_ID, motherCaseId);
        event.getParameters().put(EventKeys.DATE_OF_VISIT, null);

        motherReferralListener.motherReferralReminder(event);

        assertTrue(allCampaignEnrollments.getAll().isEmpty());
    }

}
