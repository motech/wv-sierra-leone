package org.worldvision.sierraleone.task;

import org.joda.time.DateTime;
import org.joda.time.Months;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.motechproject.cmslite.api.model.ContentNotFoundException;
import org.motechproject.cmslite.api.service.CMSLiteService;
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
import org.worldvision.sierraleone.WorldVisionSettings;
import org.worldvision.sierraleone.constants.Campaign;
import org.worldvision.sierraleone.constants.Commcare;
import org.worldvision.sierraleone.repository.FixtureIdMap;

import java.util.Arrays;

import static java.lang.String.format;
import static org.worldvision.sierraleone.constants.SMSContent.CHILD_VITAMIN_A_REMINDER;

@Component
public class MessageCampaignListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CommcareCaseService commcareCaseService;

    @Autowired
    private MessageCampaignService messageCampaignService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private FixtureIdMap fixtureIdMap;

    @Autowired
    private CMSLiteService cmsLiteService;

    @Autowired
    private WorldVisionSettings settings;

    @MotechListener(subjects = EventKeys.SEND_MESSAGE)
    public void handle(MotechEvent event) throws ContentNotFoundException {
        logger.info(format("MotechEvent %s received on %s", event, EventKeys.SEND_MESSAGE));

        String referralCaseId = null;

        String campaignName = (String) event.getParameters().get(EventKeys.CAMPAIGN_NAME_KEY);
        String externalId = (String) event.getParameters().get(EventKeys.EXTERNAL_ID_KEY);

        logger.info(format("Handling event for %s", campaignName));

        switch (campaignName) {
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
                handleChildVitaminAReminderCampaign(referralCaseId, externalId);
                break;
            default:
                logger.warn(format("Ignoring campaign event for unknown campaign: %s", campaignName));
                break;
        }
    }

    private void handleChildVitaminAReminderCampaign(String referralCaseId, String externalId) throws ContentNotFoundException {
        String[] elements;
        String childCaseId;
        String motherCaseId;
        CaseInfo childCase;
        CaseInfo motherCase; // The external id encodes the child and mother case ids
        elements = externalId.split(":");
        childCaseId = elements[0];
        motherCaseId = elements[1];

        childCase = commcareCaseService.getCaseByCaseId(childCaseId);

        if (null == childCase) {
            logger.error(format("Unable to load childcase: %s from commcare", childCaseId));
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
                    String message = format(getMessage(CHILD_VITAMIN_A_REMINDER), childName, phuName);
                    smsService.sendSMS(new SendSmsRequest(Arrays.asList(phone), message));
                    logger.info(format("Sending vitamin a reminder SMS to %s for mothercase: %s referralcase: %s", phone, motherCaseId, referralCaseId));
                } else {
                    logger.info(format("No phone for mothercase: %s referralcase: %s not sending child vitamin a reminder", motherCaseId, referralCaseId));
                }
            } else {
                logger.error(format("Unable to load mothercase: %s from commcare", motherCaseId));
            }

        } else if ("yes".equals(vitaminA)) {
            CampaignRequest cr = new CampaignRequest(externalId,
                    Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN,
                    null, null, null);

            logger.info(format("unenrolling childcase: %s from %s", externalId, Campaign.CHILD_VITAMIN_A_REMINDER_CAMPAIGN));
            messageCampaignService.stopAll(cr);
        }
    }

    private String getMessage(String name) throws ContentNotFoundException {
        return cmsLiteService.getStringContent(settings.getLanguage(), name).getValue();
    }
}
