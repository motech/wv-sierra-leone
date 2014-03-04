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
import org.worldvision.sierraleone.constants.EventKeys;

@Component
public class MotherReferralListener {
    public static final String MOTHER_REFERRAL_REMINDER_CAMPAIGN = "Mother Referral Reminder";
    private static final Logger LOGGER = LoggerFactory.getLogger(MotherReferralListener.class);

    private MessageCampaignService messageCampaignService;

    @Autowired
    public MotherReferralListener(MessageCampaignService messageCampaignService) {
        this.messageCampaignService = messageCampaignService;
    }

    @MotechListener(subjects = EventKeys.MOTHER_REFERRAL_SUBJECT)
    public void motherReferralReminder(MotechEvent event) {
        LOGGER.debug(String.format("MotechEvent received on %s", EventKeys.MOTHER_REFERRAL_SUBJECT));
        // Rule 2:
        // IF “Mother needs to be referred” = TRUE and “Referral Completed” = FALSE, THEN send SMS to
        // patient every 24 hours until referral is completed.

        String motherCaseId = EventKeys.getStringValue(event, EventKeys.MOTHER_CASE_ID);
        String referralCaseId = EventKeys.getStringValue(event, EventKeys.REFERRAL_CASE_ID);
        DateTime lastVisit = EventKeys.getDateTimeValue(event, EventKeys.LAST_VISIT);

        if (null == lastVisit) {
            LOGGER.error(String.format("Event: %s does not contain key: %s", event, EventKeys.LAST_VISIT));
            return;
        }

        // Enroll mother in message campaign reminding her to attend postnatal consultation
        // I can't append extra data to an enrollment so I'm encoding the externalId as
        // caseId:referalId
        String externalId = motherCaseId + ":" + referralCaseId;
        CampaignRequest cr = new CampaignRequest(externalId,
                MOTHER_REFERRAL_REMINDER_CAMPAIGN,
                lastVisit.toLocalDate(), null, null);

        messageCampaignService.startFor(cr);
    }
}
