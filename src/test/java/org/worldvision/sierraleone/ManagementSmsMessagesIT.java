package org.worldvision.sierraleone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.cmslite.api.model.StringContent;
import org.motechproject.cmslite.api.repository.AllStreamContents;
import org.motechproject.cmslite.api.repository.AllStringContents;
import org.motechproject.cmslite.api.service.CMSLiteService;
import org.motechproject.server.config.service.PlatformSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath*:/META-INF/motech/applicationCmsLiteApi.xml",
        "classpath*:/META-INF/motech/activemqConnection.xml",
        "classpath*:/META-INF/motech/eventQueuePublisher.xml",
        "classpath*:/META-INF/motech/eventQueueConsumer.xml",
        "classpath*:/META-INF/motech/applicationPlatformConfig.xml",
        "classpath*:/META-INF/motech/applicationCommonsCouchdbContext.xml"
})
public class ManagementSmsMessagesIT {
    @Autowired
    private CMSLiteService cmsLiteService;

    @Autowired
    private AllStreamContents allStreamContents;

    @Autowired
    private AllStringContents allStringContents;

    @Autowired
    private PlatformSettingsService platformSettingsService;

    private WorldVisionSettings settings;

    private ManagementSmsMessages mgr;

    private Resource messages;

    @Before
    public void setUp() throws Exception {
        Resource language = new ClassPathResource("/wv-settings.properties", getClass());
        messages = new ClassPathResource("/sms-messages.json", getClass());

        settings = new WorldVisionSettings();
        settings.setModuleName("sierra-leone");
        settings.setConfigFiles(asList(language));
        settings.setPlatformSettingsService(platformSettingsService);

        mgr = new ManagementSmsMessages(settings);
    }

    @After
    public void tearDown() throws Exception {
        allStreamContents.removeAll();
        allStringContents.removeAll();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotLoadMessagesIfResourceNotFound() throws Exception {
        mgr.bind(cmsLiteService, null);
    }

    @Test
    public void shouldLoadMessages() throws Exception {
        settings.setRawConfigFiles(asList(messages));

        mgr.bind(cmsLiteService, null);

        assertMessages();
    }

    @Test
    public void shouldNotLoadSingleMessageTwice() throws Exception {
        settings.setRawConfigFiles(asList(messages));

        cmsLiteService.addContent(new StringContent(settings.getLanguage(), "MissedConsecutivePostPartumVisits", "CHW %s has now missed 2 scheduled visits in a row, call to check on their progress."));

        mgr.bind(cmsLiteService, null);

        assertMessages();
    }

    @Test
    public void shouldNotLoadAllMessagesTwice() throws Exception {
        settings.setRawConfigFiles(asList(messages));

        mgr.bind(cmsLiteService, null);

        assertMessages();

        mgr.bind(cmsLiteService, null);

        assertMessages();
    }

    @Test
    public void shouldChangeSettingsLanguageIfContentLanguageIsDifferent() throws Exception {
        settings.setRawConfigFiles(asList(messages));
        settings.setProperty("language", "Spanish");

        mgr.bind(cmsLiteService, null);

        assertMessages();
        assertEquals("English", settings.getLanguage());
    }

    private void assertMessages() {
        assertEquals(6, cmsLiteService.getAllContents().size());

        assertTrue(cmsLiteService.isStringContentAvailable(settings.getLanguage(), "PostnatalConsultationReminder"));
        assertTrue(cmsLiteService.isStringContentAvailable(settings.getLanguage(), "MotherReferralReminder"));
        assertTrue(cmsLiteService.isStringContentAvailable(settings.getLanguage(), "ChildVitaminAReminder"));
        assertTrue(cmsLiteService.isStringContentAvailable(settings.getLanguage(), "HomeBirthNotification"));
        assertTrue(cmsLiteService.isStringContentAvailable(settings.getLanguage(), "MissedConsecutiveChildVisits"));
        assertTrue(cmsLiteService.isStringContentAvailable(settings.getLanguage(), "MissedConsecutivePostPartumVisits"));
    }

}
