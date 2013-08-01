package org.worldvision.sierraleone;

import org.motechproject.server.config.SettingsFacade;

public class WorldVisionSettings extends SettingsFacade {
    public static final String LANGUAGE_KEY = "language";

    public String getLanguage() {
        return getProperty(LANGUAGE_KEY);
    }

    @Override
    protected String constructSymbolicName() {
        return "org.worldvision.wv-sierra-leone-bundle";
    }
}
