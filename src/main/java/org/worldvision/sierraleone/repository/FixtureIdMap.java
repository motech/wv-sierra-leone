package org.worldvision.sierraleone.repository;

import org.motechproject.commcare.domain.CommcareFixture;
import org.motechproject.commcare.service.CommcareFixtureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.worldvision.sierraleone.constants.Commcare;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class FixtureIdMap {
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
}
