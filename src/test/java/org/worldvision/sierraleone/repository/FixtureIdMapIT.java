package org.worldvision.sierraleone.repository;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.commcare.service.impl.CommcareFixtureServiceImpl;
import org.motechproject.commcare.util.CommCareAPIHttpClient;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.worldvision.sierraleone.constants.Commcare;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath*:/META-INF/motech/applicationCommcareAPI.xml",
        "classpath*:/META-INF/motech/activemqConnection.xml",
        "classpath*:/META-INF/motech/eventQueuePublisher.xml",
        "classpath*:/META-INF/motech/eventQueueConsumer.xml",
        "classpath*:/META-INF/motech/applicationPlatformConfig.xml",
        "classpath*:/META-INF/motech/applicationCommonsCouchdbContext.xml"
})
public class FixtureIdMapIT {
    private CommCareAPIHttpClient commCareAPIHttpClient;

    private FixtureIdMap fixtureIdMap;

    private ObjectMapper mapper;

    @Before
    public void setUp() {
        commCareAPIHttpClient = mock(CommCareAPIHttpClient.class);

        fixtureIdMap = new FixtureIdMap(new CommcareFixtureServiceImpl(commCareAPIHttpClient));
        mapper = new ObjectMapper();

        when(commCareAPIHttpClient.fixturesRequest()).thenReturn(getFixtures().toString());
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
        when(commCareAPIHttpClient.fixtureRequest("AAAAAAAA"))
                .thenReturn(createCommcareFixture("AAAAAAAA", "phu", "vic", "PHONE", null).toString());

        assertEquals("PHONE", fixtureIdMap.getPhoneForFixture("vic"));
    }

    @Test
    public void shouldNotFindPhoneForFixture() {
        when(commCareAPIHttpClient.fixtureRequest("BBBBBBBB"))
                .thenReturn(createCommcareFixture("BBBBBBBB", "phu", "vic", null, null).toString());

        assertNull(fixtureIdMap.getPhoneForFixture("vic"));
        assertNull(fixtureIdMap.getPhoneForFixture("gnu"));
        assertNull(fixtureIdMap.getPhoneForFixture("none"));
        assertNull(fixtureIdMap.getPhoneForFixture(null));
    }

    @Test
    public void shouldFindNameForFixture() {
        when(commCareAPIHttpClient.fixtureRequest("AAAAAAAA"))
                .thenReturn(createCommcareFixture("AAAAAAAA", "phu", "vic", null, "NAME").toString());

        assertEquals("NAME", fixtureIdMap.getNameForFixture("vic"));
    }

    @Test
    public void shouldNotFindNameForFixture() {
        when(commCareAPIHttpClient.fixtureRequest("BBBBBBBB"))
                .thenReturn(createCommcareFixture("BBBBBBBB", "phu", "vic", null, null).toString());

        assertNull(fixtureIdMap.getNameForFixture("vic"));
        assertNull(fixtureIdMap.getNameForFixture("gnu"));
        assertNull(fixtureIdMap.getNameForFixture("none"));
        assertNull(fixtureIdMap.getNameForFixture(null));
    }

    private ObjectNode getFixtures() {
        ArrayNode array = mapper.createArrayNode();
        array.add(createCommcareFixture("AAAAAAAA", "phu", "vic", null, null));
        array.add(createCommcareFixture("BBBBBBBB", "phu", "gnu", null, null));
        array.add(createCommcareFixture("CCCCCCCC", "community", "gnu", null, null));
        array.add(createCommcareFixture("DDDDDDDD", "community", null, null, null));

        ObjectNode fixtures = mapper.createObjectNode();
        fixtures.put("objects", array);

        return fixtures;
    }

    private ObjectNode createCommcareFixture(String id, String type, String phuId,
                                             String phone, String name) {
        ObjectNode fields = mapper.createObjectNode();
        fields.put(Commcare.PHU_ID, phuId);

        if (null != phone) {
            fields.put(Commcare.PHONE, phone);
        }

        if (null != name) {
            fields.put(Commcare.NAME, name);
        }

        ObjectNode fixture = mapper.createObjectNode();
        fixture.put("id", id);
        fixture.put("fixture_type", type);
        fixture.put("fields", fields);

        return fixture;
    }
}

