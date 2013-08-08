package org.worldvision.sierraleone;

import org.eclipse.gemini.blueprint.service.importer.OsgiServiceLifecycleListener;
import org.motechproject.cmslite.api.model.CMSLiteException;
import org.motechproject.cmslite.api.model.Content;
import org.motechproject.cmslite.api.model.StringContent;
import org.motechproject.cmslite.api.service.CMSLiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class ManagementSmsMessages implements OsgiServiceLifecycleListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementSmsMessages.class);
    private static final String FILE_NAME = "sms-messages.json";

    private boolean loadedMessages;

    private WorldVisionSettings settings;

    @Autowired
    public ManagementSmsMessages(WorldVisionSettings settings) {
        this.settings = settings;
    }

    @Override
    public void bind(Object service, Map properties) throws CMSLiteException {
        if (service instanceof CMSLiteService && !loadedMessages) {
            loadMessages((CMSLiteService) service);
        }
    }

    @Override
    public void unbind(Object service, Map properties) {
        if (service instanceof CMSLiteService) {
            LOGGER.info("CMSLiteService is unavailable");
        }
    }

    private void loadMessages(CMSLiteService cmsLiteService) throws CMSLiteException {
        Collection<StringContent> messages = getMessages();

        for (StringContent content : messages) {
            try {
                if (!cmsLiteService.isStringContentAvailable(content.getLanguage(), content.getName())) {
                    cmsLiteService.addContent(content);
                }
            } catch (CMSLiteException e) {
                LOGGER.error(String.format("Can't add content: %s", content), e);
                throw e;
            }
        }

        loadedMessages = true;
    }

    private Collection<StringContent> getMessages() {
        InputStream stream = settings.getRawConfig(FILE_NAME);
        LOGGER.info("Read resource: " + FILE_NAME);

        Collection<StringContent> contents;

        if (stream != null) {
            try {
                contents = Utils.readJSON(stream, List.class, StringContent.class);
                LOGGER.info(String.format("Read all messages from: %s", FILE_NAME));
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                throw new IllegalStateException(String.format("Can't read resource: %s", FILE_NAME), e);
            }
        } else {
            String errorMessage = String.format("Resource not exists: %s", FILE_NAME);

            LOGGER.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        for (Content content : contents) {
            String contentLanguage = content.getLanguage();
            String settingsLanguage = settings.getLanguage();

            if (!contentLanguage.equalsIgnoreCase(settingsLanguage)) {
                LOGGER.warn(String.format("Incorrect language in settings file. Change from %s to %s", settingsLanguage, contentLanguage));
                settings.setProperty(WorldVisionSettings.LANGUAGE_KEY, contentLanguage);
            }
        }

        return contents;
    }
}
