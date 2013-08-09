package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.messagecampaign.contract.CampaignRequest;
import org.motechproject.messagecampaign.service.MessageCampaignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.worldvision.sierraleone.constants.Campaign;
import org.worldvision.sierraleone.constants.EventKeys;

@Component
public class MotherReferralListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MotherReferralListener.class);

    private MessageCampaignService messageCampaignService;

    @Autowired
    public MotherReferralListener(MessageCampaignService messageCampaignService) {
        this.messageCampaignService = messageCampaignService;
    }

    @MotechListener(subjects = EventKeys.MOTHER_REFERRAL_SUBJECT)
    public void motherReferralReminder(MotechEvent event) {
        String motherCaseId = EventKeys.getStringValue(event, EventKeys.MOTHER_CASE_ID);
        String referralCaseId = EventKeys.getStringValue(event, EventKeys.REFERRAL_CASE_ID);

        // Rule 2:
        // IF “Mother needs to be referred” = TRUE and “Referral Completed” = FALSE, THEN send SMS to
        // patient every 24 hours until referral is completed.
        DateTime dateOfVisit;

        try {
            dateOfVisit = (DateTime) event.getParameters().get(EventKeys.DATE_OF_VISIT);
        } catch (ClassCastException e) {
            LOGGER.error("Event: " + event + " Key: " + EventKeys.DATE_OF_VISIT + " is not a DateTime");
            return;
        }

        // Enroll mother in message campaign reminding her to attend postnatal consultation
        // I can't append extra data to an enrollment so I'm encoding the externalId as
        // caseId:referalId
        String externalId = motherCaseId + ":" + referralCaseId;
        CampaignRequest cr = new CampaignRequest(externalId,
                Campaign.MOTHER_REFERRAL_REMINDER_CAMPAIGN,
                dateOfVisit.toLocalDate(),
                null, null);

        messageCampaignService.startFor(cr);
    }
}
