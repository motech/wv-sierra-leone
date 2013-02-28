package org.worldvision.sierraleone;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 */
public class UtilTest {
    @Test
    public void validPhoneNumber() {
        String phone = "011 111 111";

        assertEquals("Phone number", "+232 11 111 111", Utils.mungeMothersPhone(phone));
    }

    @Test
    public void invalidPhoneNumberHasLetters() {
        String phone = "011 1A1 111";

        assertEquals("Phone number", null, Utils.mungeMothersPhone(phone));
    }

    @Test
    public void invalidPhoneNumberDoesntStartWithZero() {
        String phone = "111 111 111";

        assertEquals("Phone number", null, Utils.mungeMothersPhone(phone));
    }

    @Test
    public void invalidPhoneNumberTooShort() {
        String phone = "011 111 11";

        assertEquals("Phone number", null, Utils.mungeMothersPhone(phone));
    }

    @Test
    public void invalidPhoneNumberTooLong() {
        String phone = "011 111 1111";

        assertEquals("Phone number", null, Utils.mungeMothersPhone(phone));
    }

    @Test
    public void invalidPhoneNumberNoSpaces() {
        String phone = "011111111";

        assertEquals("Phone number", null, Utils.mungeMothersPhone(phone));
    }


    @Test
    public void invalidPhoneNumberIsNull() {
        String phone = null;

        assertEquals("Phone number", null, Utils.mungeMothersPhone(phone));
    }
}
