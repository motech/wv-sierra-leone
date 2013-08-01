package org.worldvision.sierraleone;

import org.eclipse.gemini.blueprint.service.importer.OsgiServiceLifecycleListener;
import org.motechproject.cmslite.api.model.CMSLiteException;
import org.motechproject.cmslite.api.model.Content;
import org.motechproject.cmslite.api.model.StringContent;
import org.motechproject.cmslite.api.service.CMSLiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class ManagementSmsMessages implements OsgiServiceLifecycleListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementSmsMessages.class);
    private static final String FILE_NAME = "sms-messages.json";

    private boolean loadedMessages;

    private ResourceLoader resourceLoader;
    private WorldVisionSettings settings;

    @Autowired
    public ManagementSmsMessages(ResourceLoader resourceLoader, WorldVisionSettings settings) {
        this.resourceLoader = resourceLoader;
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
        String resourceName = String.format("/%s", FILE_NAME);
        Resource resource = resourceLoader.getResource(resourceName);
        LOGGER.info("Read resource: " + resourceName);

        Collection<StringContent> contents;

        if (resource != null && resource.exists()) {
            try {
                contents = Utils.readJSON(
                        resource.getInputStream(),
                        List.class,
                        StringContent.class
                );
                LOGGER.info(String.format("Read all messages from: %s", resourceName));
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                throw new IllegalStateException(String.format("Can't read resource: %s", resourceName), e);
            }
        } else {
            String errorMessage = String.format("Resource not exists: %s", resourceName);

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
