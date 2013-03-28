package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.joda.time.Months;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.service.CommcareCaseService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.messagecampaign.EventKeys;
import org.motechproject.messagecampaign.contract.CampaignRequest;
import org.motechproject.messagecampaign.service.MessageCampaignService;
import org.motechproject.sms.api.service.SendSmsRequest;
import org.motechproject.sms.api.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.worldvision.sierraleone.Utils;
import org.worldvision.sierraleone.constants.Campaign;
import org.worldvision.sierraleone.constants.Commcare;
import org.worldvision.sierraleone.constants.SMSContent;
import org.worldvision.sierraleone.repository.FixtureIdMap;

import java.util.Arrays;

@Component
public class MessageCampaignListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    CommcareCaseService commcareCaseService;

    @Autowired
    MessageCampaignService messageCampaignService;

    @Autowired
    SmsService smsService;

    @Autowired
    FixtureIdMap fixtureIdMap;

    @MotechListener(subjects = EventKeys.SEND_MESSAGE)
    public void handle(MotechEvent event) {
        logger.info("MotechEvent " + event + " received on " + EventKeys.SEND_MESSAGE);

        CaseInfo motherCase = null;
        CaseInfo childCase = null;
        CaseInfo referralCase = null;

        String motherCaseId = null;
        String childCaseId = null;
        String referralCaseId = null;

        String[] elements = null;

        String campaignName = (String) event.getParameters().get(EventKeys.CAMPAIGN_NAME_KEY);
        String externalId = (String) event.getParameters().get(EventKeys.EXTERNAL_ID_KEY);

        logger.info("Handling event for " + campaignName);

        switch (campaignName) {
            /*
             Rule 1:
             IF patient has delivered AND patient has not attended postnatal consultation at health center THEN send SMS to
             patient every week until 45 days after delivery.
            */
            case Campaign.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN:
                // Load the mothers case
                motherCase = commcareCaseService.getCaseByCaseId(externalId);

                if (null != motherCase) {
                    // Verify she is still alive and still has not attended postnatal consultation
                    String stillAlive = motherCase.getFieldValues().get(Commcare.STILL_ALIVE);
                    String attendedPostnatal = motherCase.getFieldValues().get(Commcare.ATTENDED_POSTNATAL);
                    String phone = Utils.mungeMothersPhone(motherCase.getFieldValues().get(Commcare.MOTHER_PHONE_NUMBER));
                    String motherName = motherCase.getFieldValues().get(Commcare.MOTHER_NAME);

                    // Send SMS to her
                    if ("yes".equals(stillAlive) && "no".equals(attendedPostnatal)) {
                        if (null != phone) {
                            String message = String.format(SMSContent.POSTNATAL_CONSULTATION_REMINDER, motherName);
                            smsService.sendSMS(new SendSmsRequest(Arrays.asList(phone), message));
                            logger.info("Sending reminder SMS to " + phone + " for mothercase: " + externalId);
                        } else {
                            logger.info("No phone for mothercase " + externalId + " not sending postnatal consultation remidner");
                        }
                    } else if ("no".equals(stillAlive) || "yes".equals(attendedPostnatal)) {

                        // If the mother has died or attended postnatal consultations we can unenroll the campaign
                        CampaignRequest cr = new CampaignRequest(externalId,
                                                                 Campaign.POSTNATAL_CONSULTATION_REMINDER_CAMPAIGN,
                                                                 null, null, null);
                        logger.info("unenrolling mothercase: " + externalId + " stillAlive: " + stillAlive + " attended: " + attendedPostnatal);
                        messageCampaignService.stopAll(cr);
                    }
                } else {
                    logger.error("Unable to find mothercase: " + externalId + " in commcare");
                }

                break;

            /*
             Rule 2:
	         IF “Mother needs to be referred” = TRUE and “Referral Completed” = FALSE, THEN send SMS to patient
	         every 24 hours until referral is completed.
            */
            case Campaign.MOTHER_REFERRAL_REMINDER_CAMPAIGN:
                // The externalId encodes the mother case id and the referral id.
                elements = externalId.split(":");
                motherCaseId = elements[0];
                referralCaseId = elements[1];

                logger.info("motherCaseId: " + motherCaseId + " referralId: " + referralCaseId);

                // Load mother case
                motherCase = commcareCaseService.getCaseByCaseId(motherCaseId);

                if (null == motherCase) {
                    logger.error("Unable to load mothercase: " + motherCaseId + " from commcare");
                    return;
                }

                // Load referral case
                referralCase = commcareCaseService.getCaseByCaseId(referralCaseId);

                if (null == referralCase) {
                    logger.error("Unable to load referralcase: " + referralCaseId + " from commcare");
                    return;
                }

                String caseOpen = referralCase.getFieldValues().get(Commcare.OPEN);

                if ("true".equals(caseOpen)) {
                    // If open send SMS
                    String phone = Utils.mungeMothersPhone(motherCase.getFieldValues().get(Commcare.MOTHER_PHONE_NUMBER));
                    String motherName = motherCase.getFieldValues().get(Commcare.MOTHER_NAME);

                    if (null != phone) {
                        String message = String.format(SMSContent.MOTHER_REFERRAL_REMINDER, motherName);
                        smsService.sendSMS(new SendSmsRequest(Arrays.asList(phone), message));
                        logger.info("Sending reminder SMS to " + phone + " for mothercase: " + motherCaseId + " referralcase: " + referralCaseId);
                    } else {
                        logger.info("No phone number for mothercase: " + motherCaseId + " referralcase: " + referralCaseId + " not sending mother referral reminder");
                    }

                } else {
                    // If closed unenroll from message campaign
                    CampaignRequest cr = new CampaignRequest(externalId,
                            Campaign.MOTHER_REFERRAL_REMINDER_CAMPAIGN,
                            null, null, null);

                    logger.info("unenrolling mothercase: " + motherCaseId + " referralcase: " + referralCaseId + " from " + Campaign.MOTHER_REFERRAL_REMINDER_CAMPAIGN);
                    messageCampaignService.stopAll(cr);
                }


                break;

            /*
             Rule 6:
	          IF Child has missed vitamin A dose, send a message to mother reminding her
	          (NN: I looked back at this SMS again, and what it is saying is to send an SMS to the mother.  This SMS should
	          go out once per month until the field "has received it in last 6 months" is changed to yes.  I'll be
	          seperately alerting the CHW in the app to go check if this has been done, so the CHW will mark that the
	          vitamin A dose has been received.  So you would filter on: DOB > 6 months ago AND vitamin_A_received = 'No'
	          then just send that once monthly until it changes to yes.)
            */
            case Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN:
                // The external id encodes the child and mother case ids
                elements = externalId.split(":");
                childCaseId = elements[0];
                motherCaseId = elements[1];

                childCase = commcareCaseService.getCaseByCaseId(childCaseId);

                if (null == childCase) {
                    logger.error("Unable to load childcase: " + childCaseId + " from commcare");
                    return;
                }

                String vitaminA = childCase.getFieldValues().get(Commcare.VITAMIN_A);
                String dob = childCase.getFieldValues().get(Commcare.DATE_OF_BIRTH);

                DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
                        .appendYear(4, 4)
                        .appendLiteral('-')
                        .appendMonthOfYear(2)
                        .appendLiteral('-')
                        .appendDayOfMonth(2)
                        .toFormatter();

                String childName = childCase.getFieldValues().get(Commcare.CHILD_NAME);

                DateTime dateOfBirth = dateFormatter.parseDateTime(dob);
                if (Months.monthsBetween(dateOfBirth, new DateTime()).getMonths() > 6 && "no".equals(vitaminA)) {
                    motherCase = commcareCaseService.getCaseByCaseId(motherCaseId);

                    if (null != motherCase) {
                        String phone = motherCase.getFieldValues().get(Commcare.MOTHER_PHONE_NUMBER);
                        String phuId = motherCase.getFieldValues().get(Commcare.PHU_ID);
                        String phuName = fixtureIdMap.getNameForFixture(phuId);

                        if (null != phone) {
                            String message = String.format(SMSContent.CHILD_VITAMIN_A_REMINDER, childName, phuName);
                            smsService.sendSMS(new SendSmsRequest(Arrays.asList(phone), message));
                            logger.info("Sending vitamin a reminder SMS to " + phone + " for mothercase: " + motherCaseId + " referralcase: " + referralCaseId);
                        } else {
                            logger.info("No phone for mothercase: " + motherCaseId + " referralcase: " + referralCaseId + " not sending child vitamin a reminder");
                        }
                    } else {
                        logger.error("Unable to load mothercase: " + motherCaseId + " from commcare");
                    }

                } else if ("yes".equals(vitaminA)) {
                    CampaignRequest cr = new CampaignRequest(externalId,
                            Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN,
                            null, null, null);

                    logger.info("unenrolling childcase: " + externalId + " from " + Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN);
                    messageCampaignService.stopAll(cr);
                }
                break;

            default:
                logger.warn("Ignoring campaign event for unknown campaign: " + campaignName);
                break;
        }
    }
}
