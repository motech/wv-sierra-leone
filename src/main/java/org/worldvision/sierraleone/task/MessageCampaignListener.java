package org.worldvision.sierraleone.task;

import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.service.CommcareCaseService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.server.messagecampaign.EventKeys;
import org.motechproject.server.messagecampaign.contract.CampaignRequest;
import org.motechproject.server.messagecampaign.service.MessageCampaignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.worldvision.sierraleone.Constants;
import org.worldvision.sierraleone.Utils;

import java.util.Map;

@Component
public class MessageCampaignListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    CommcareCaseService commcareCaseService;

    @Autowired
    MessageCampaignService messageCampaignService;

    @MotechListener(subjects = EventKeys.SEND_MESSAGE)
    public void handle(MotechEvent event) {
        logger.info("MotechEvent " + event + " received on " + EventKeys.SEND_MESSAGE);

        CaseInfo motherCase = null;

        /*
        Rule 1:
        IF patient has delivered AND patient has not attended postnatal consultation at health center THEN send SMS to
        patient every week until 45 days after delivery.
        */
        String campaignName = (String) event.getParameters().get(EventKeys.CAMPAIGN_NAME_KEY);
        String externalId = (String) event.getParameters().get(EventKeys.EXTERNAL_ID_KEY);

        logger.info("Handling event for " + campaignName);

        switch (campaignName) {
            case Constants.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN:
                // Load the mothers case
                motherCase = commcareCaseService.getCaseByCaseId(externalId);

                if (null != motherCase) {
                    // Verify she is still alive and still has not attended postnatal consultation
                    String stillAlive = motherCase.getFieldValues().get("still_alive");
                    String attendedPostnatal = motherCase.getFieldValues().get("attended_postnatal");
                    String phone = Utils.mungeMothersPhone(motherCase.getFieldValues().get("mother_phone_number"));

                    // Send SMS to her
                    if ("yes".equals(stillAlive) && "no".equals(attendedPostnatal) && null != phone) {
                        // TODO: Enable SMS service
                        // smsService.sendSMS(phone, message);
                        logger.info("Sending reminder SMS to " + phone + " for mothercase: " + externalId);
                    } else if ("no".equals(stillAlive) || "yes".equals(attendedPostnatal)) {

                        // If the mother has died or attended postnatal consultations we can unenroll the campaign
                        CampaignRequest cr = new CampaignRequest(externalId,
                                                                 Constants.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN,
                                                                 null, null, null);
                        logger.info("unenrolling mothercase: " + externalId + " stillAlive: " + stillAlive + " attended: " + attendedPostnatal);
                        messageCampaignService.stopAll(cr);
                    }
                } else {
                    // TODO: handle missing mother case in commcare
                    logger.error("Unable to find mothercase: " + externalId + " in commcare");
                }

                break;

            case Constants.MOTHER_REFERRAL_REMINDER_CAMPAIGN:
                // The externalId encodes the mother case id and the referral id.
                String[] elements = externalId.split(":");
                String motherCaseId = elements[0];
                String referralCaseId = elements[1];

                logger.info("motherCaseId: " + motherCaseId + " referralId: " + referralCaseId);

                // Load mother case
                motherCase = commcareCaseService.getCaseByCaseId(motherCaseId);

                if (null == motherCase) {
                    logger.error("Unable to load mothercase: " + motherCaseId + " from commcare");
                    return;
                }

                // Load referral case
                CaseInfo referralCase = commcareCaseService.getCaseByCaseId(referralCaseId);

                if (null == referralCase) {
                    logger.error("Unable to load referralcase: " + referralCaseId + " from commcare");
                    return;
                }

                // If open send SMS

                // If closed unenroll from message campaign

                break;

            default:
                logger.warn("Ignoring campaign event for unknown campaign: " + campaignName);
                break;
        }
    }
}
