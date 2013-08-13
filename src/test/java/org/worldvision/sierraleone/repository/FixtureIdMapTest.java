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
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FixtureIdMapTest {
    @Mock
    private CommcareFixtureService commcareFixtureService;

    private FixtureIdMap fixtureIdMap;

    @Before
    public void setUp() {
        initMocks(this);

        fixtureIdMap = new FixtureIdMap(commcareFixtureService);
        when(commcareFixtureService.getAllFixtures()).thenReturn(getFixtures());
    }

    @Test
    public void shouldFindFixtureId() {
        assertEquals("AAAAAAAA", fixtureIdMap.fixtureIdForPHUId("vic"));
        assertEquals("BBBBBBBB", fixtureIdMap.fixtureIdForPHUId("gnu"));
    }

    @Test
    public void shouldNotFindFixtureId() {
        assertNull(fixtureIdMap.fixtureIdForPHUId("none"));
        assertNull(fixtureIdMap.fixtureIdForPHUId(null));
    }

    @Test
    public void shouldFindPhoneForFixture() {
        when(commcareFixtureService.getCommcareFixtureById("AAAAAAAA"))
                .thenReturn(createCommcareFixture("AAAAAAAA", "phu", "vic", "PHONE", null));

        assertEquals("PHONE", fixtureIdMap.getPhoneForFixture("vic"));
    }

    @Test
    public void shouldNotFindPhoneForFixture() {
        when(commcareFixtureService.getCommcareFixtureById("BBBBBBBB"))
                .thenReturn(createCommcareFixture("BBBBBBBB", "phu", "vic", null, null));

        assertNull(fixtureIdMap.getPhoneForFixture("vic"));
        assertNull(fixtureIdMap.getPhoneForFixture("gnu"));
        assertNull(fixtureIdMap.getPhoneForFixture("none"));
        assertNull(fixtureIdMap.getPhoneForFixture(null));
    }

    @Test
    public void shouldFindNameForFixture() {
        when(commcareFixtureService.getCommcareFixtureById("AAAAAAAA"))
                .thenReturn(createCommcareFixture("AAAAAAAA", "phu", "vic", null, "NAME"));

        assertEquals("NAME", fixtureIdMap.getNameForFixture("vic"));
    }

    @Test
    public void shouldNotFindNameForFixture() {
        when(commcareFixtureService.getCommcareFixtureById("BBBBBBBB"))
                .thenReturn(createCommcareFixture("BBBBBBBB", "phu", "vic", null, null));

        assertNull(fixtureIdMap.getNameForFixture("vic"));
        assertNull(fixtureIdMap.getNameForFixture("gnu"));
        assertNull(fixtureIdMap.getNameForFixture("none"));
        assertNull(fixtureIdMap.getNameForFixture(null));
    }

    private List<CommcareFixture> getFixtures() {
        List<CommcareFixture> ret = new ArrayList<>();
        ret.add(createCommcareFixture("AAAAAAAA", "phu", "vic", null, null));
        ret.add(createCommcareFixture("BBBBBBBB", "phu", "gnu", null, null));
        ret.add(createCommcareFixture("CCCCCCCC", "community", "gnu", null, null));
        ret.add(createCommcareFixture("DDDDDDDD", "community", null, null, null));

        return ret;
    }

    private CommcareFixture createCommcareFixture(String id, String type, String phuId,
                                                  String phone, String name) {
        Map<String, String> fields = new HashMap<>();
        fields.put(Commcare.PHU_ID, phuId);
        fields.put(Commcare.PHONE, phone);
        fields.put(Commcare.NAME, name);

        CommcareFixture fixture = new CommcareFixture();
        fixture.setId(id);
        fixture.setFixtureType(type);
        fixture.setFields(fields);

        return fixture;
    }
}

