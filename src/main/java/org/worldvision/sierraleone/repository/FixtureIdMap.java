package org.worldvision.sierraleone.repository;

import org.motechproject.commcare.domain.CommcareFixture;
import org.motechproject.commcare.service.CommcareFixtureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.worldvision.sierraleone.constants.Commcare;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class FixtureIdMap {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Map<String, CommcareFixture> fixtures = null;

    @Autowired
    CommcareFixtureService commcareFixtureService;

    public String fixtureIdForPHUId(String phuId) {
        String ret = null;

        if (null == fixtures) {
            refreshFixtureMap();
        }

        CommcareFixture fixture = fixtures.get(phuId);
        if (null != fixture) {
            ret = fixture.getId();
        }

        return ret;
    }

    public synchronized void refreshFixtureMap() {
        if (null == fixtures) {
            fixtures = new HashMap<String, CommcareFixture>();
        }

        // Load all fixtures from commcare
        List<CommcareFixture> allFixtures = commcareFixtureService.getAllFixtures();

        // Delete all elements in the fixture map
        fixtures.clear();

        // Update the fixture map with the new data from commcare
        for (CommcareFixture fixture : allFixtures ) {
            String phuId = fixture.getFields().get(Commcare.PHU_ID);

            if (null != phuId && "phu".equals(fixture.getFixtureType())) {
                fixtures.put(phuId, fixture);
            }
        }
    }

    public String getPhoneForFixture(String phuId) {
        String fixtureId = fixtureIdForPHUId(phuId);
        if (null == fixtureId) {
            logger.error("Unable to get fixtureId for phu " + phuId);
            return null;
        }

        CommcareFixture fixture = fixtureIdForPHUIdWithRetry(fixtureId, phuId);

        if (null == fixture) {
            logger.error("Unable to load fixture " + fixtureId + " from commcare");
            return null;
        }

        String phone = fixture.getFields().get(Commcare.PHONE);
        if (null == phone) {
            logger.error("No phone for phu " + phuId + " fixture " + fixtureId);
        }

        return phone;
    }

    public String getNameForFixture(String phuId) {
        String fixtureId = fixtureIdForPHUId(phuId);
        if (null == fixtureId) {
            logger.error("Unable to get fixtureId for phu " + phuId);
            return null;
        }

        CommcareFixture fixture = fixtureIdForPHUIdWithRetry(fixtureId, phuId);

        if (null == fixture) {
            logger.error("Unable to load fixture " + fixtureId + " from commcare");
            return null;
        }

        String phone = fixture.getFields().get(Commcare.PHONE);
        if (null == phone) {
            logger.error("No name for phu " + phuId + " fixture " + fixtureId);
        }

        return phone;
    }

    private CommcareFixture fixtureIdForPHUIdWithRetry(String fixtureId, String phuId) {
        // This code could be better.  Basically I try to load from commcare.  If fixture has been updated
        // then it's fixtureId has changed and I'll get null back.  So then I refresh the in memory cache and try
        // to load it again.

        CommcareFixture fixture = commcareFixtureService.getCommcareFixtureById(fixtureId);
        if (null == fixture) {
            refreshFixtureMap();

            fixtureId = fixtureIdForPHUId(phuId);
            if (null == fixtureId) {
                logger.error("Unable to get fixtureId for phu " + phuId);
                return null;
            }

            fixture = commcareFixtureService.getCommcareFixtureById(fixtureId);
        }

        return fixture;
    }
}
