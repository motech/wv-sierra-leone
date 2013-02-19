package org.worldvision.sierraleone;


public final class Utils {
    // It is called "mother_phone_number".  Are you able to do post-processing on the number to add in the prefix +232.
    // They will be typing the number as 0## ### ###.  You would need to turn that into +232 ## ### ###
    // (i.e. remove the 0 and add the +232).
    public static String mungeMothersPhone(String phone) {
        // TODO: Implement local prefix on mothers phone number

        return phone;
    }
}
