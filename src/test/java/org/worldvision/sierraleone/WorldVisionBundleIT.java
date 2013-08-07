package org.worldvision.sierraleone;

import org.ektorp.DocumentNotFoundException;
import org.motechproject.cmslite.api.service.CMSLiteService;
import org.motechproject.tasks.service.TaskService;
import org.motechproject.testing.osgi.BaseOsgiIT;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.isEqualCollection;
import static org.worldvision.sierraleone.constants.SMSContent.CHILD_VITAMIN_A_REMINDER;
import static org.worldvision.sierraleone.constants.SMSContent.HOME_BIRTH_NOTIFICATION;
import static org.worldvision.sierraleone.constants.SMSContent.MISSED_CONSECUTIVE_CHILD_VISITS;
import static org.worldvision.sierraleone.constants.SMSContent.MISSED_CONSECUTIVE_POST_PARTUM_VISITS;
import static org.worldvision.sierraleone.constants.SMSContent.MOTHER_REFERRAL_REMINDER;
import static org.worldvision.sierraleone.constants.SMSContent.POSTNATAL_CONSULTATION_REMINDER;

public class WorldVisionBundleIT extends BaseOsgiIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldVisionBundleIT.class);

    private static final String MESSAGE_LANG = "English";
    private static final Integer TRIES_COUNT = 50;

    public void testLoadSmsMessages() {
        CMSLiteService service = getService(CMSLiteService.class);

        for (String name : getSMSContentNames()) {
            assertTrue(service.isStringContentAvailable(MESSAGE_LANG, name));
        }
    }

    public void testLoadTasks() throws Exception {
        TaskService taskService = getService(TaskService.class);
        List<String> taskIDs = getTaskIDs();
        Set<String> IDs = new HashSet<>();
        int tries = 0;

        do {
            for (String id : taskIDs) {
                try {
                    IDs.add(taskService.getTask(id).getId());
                } catch (DocumentNotFoundException e) {
                    LOGGER.warn(String.format("task with ID: %s does not exist", id));
                }
            }

            ++tries;
            Thread.sleep(500);
        } while (!isEqualCollection(taskIDs, IDs) && tries < TRIES_COUNT);

        assertTrue("All tasks were not loaded.", isEqualCollection(taskIDs, IDs));
    }

    private <T> T getService(Class<T> clazz) {
        ServiceReference serviceReference = bundleContext.getServiceReference(clazz.getName());
        assertNotNull(serviceReference);
        Object service = bundleContext.getService(serviceReference);
        assertNotNull(service);

        return clazz.cast(service);
    }

    private List<String> getSMSContentNames() {
        return asList(
                CHILD_VITAMIN_A_REMINDER,
                HOME_BIRTH_NOTIFICATION,
                MISSED_CONSECUTIVE_CHILD_VISITS,
                MISSED_CONSECUTIVE_POST_PARTUM_VISITS,
                MOTHER_REFERRAL_REMINDER,
                POSTNATAL_CONSULTATION_REMINDER
        );
    }

    private List<String> getTaskIDs() {
        return asList(
                "rule-1-enroll-patient",
                "rule-1-send-sms-to-patient",
                "rule-1-unenroll-patient-if-attended-postnatal",
                "rule-1-unenroll_patient-if-no-alive"
        );
    }

}
