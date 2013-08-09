package org.worldvision.sierraleone.repository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.motechproject.commcare.domain.CommcareFixture;
import org.motechproject.commcare.service.CommcareFixtureService;
import org.worldvision.sierraleone.constants.Commcare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FixtureIdMapTests {
    @Mock
    CommcareFixtureService commcareFixtureService;

    private FixtureIdMap fixtureIdMap;

    @Before
    public void setUp() {
        initMocks(this);

        fixtureIdMap = new FixtureIdMap(commcareFixtureService);
    }

    @Test
    public void shouldFindFixtureId() {
        // Mock the call to getAllFixtures
        when(commcareFixtureService.getAllFixtures()).thenReturn(getFixtures());

        // call refreshFixtureMap
        assertEquals("Should find fixture", "AAAAAAAA", fixtureIdMap.fixtureIdForPHUId("vic"));
        assertEquals("Should find fixture", "BBBBBBBB", fixtureIdMap.fixtureIdForPHUId("gnu"));
    }

    @Test
    public void shouldNotFindFixtureId() {
        // Mock the call to getAllFixtures
        when(commcareFixtureService.getAllFixtures()).thenReturn(getFixtures());

        // call refreshFixtureMap
        assertEquals("Should find fixture", null, fixtureIdMap.fixtureIdForPHUId("none"));
    }

    @Test
    public void shouldNotFailOnNullInput() {
        // Mock the call to getAllFixtures
        when(commcareFixtureService.getAllFixtures()).thenReturn(getFixtures());

        // call refreshFixtureMap
        assertEquals("Should find fixture", null, fixtureIdMap.fixtureIdForPHUId(null));
    }

    private List<CommcareFixture> getFixtures() {
        List<CommcareFixture> ret = new ArrayList<>();

        CommcareFixture fixture = new CommcareFixture();
        fixture.setId("AAAAAAAA");
        fixture.setFixtureType("phu");

        Map<String, String> fields = new HashMap<>();
        fields.put(Commcare.PHU_ID, "vic");

        fixture.setFields(fields);

        ret.add(fixture);

        fields = new HashMap<>();

        fixture = new CommcareFixture();
        fixture.setId("BBBBBBBB");
        fixture.setFixtureType("phu");

        fields = new HashMap<>();
        fields.put(Commcare.PHU_ID, "gnu");

        fixture.setFields(fields);

        ret.add(fixture);

        fields = new HashMap<>();

        fixture = new CommcareFixture();
        fixture.setId("CCCCCCCC");
        fixture.setFixtureType("community");

        fields = new HashMap<>();
        fields.put(Commcare.PHU_ID, "gnu");

        fixture.setFields(fields);

        ret.add(fixture);

        return ret;
    }
}

