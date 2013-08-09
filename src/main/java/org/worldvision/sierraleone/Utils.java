package org.worldvision.sierraleone;


import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;

public final class Utils {
    private static final int YEAR_DIGITS_COUNT = 4;
    private static final int MONTH_DIGITS_COUNT = 2;
    private static final int DAY_DIGITS_COUNT = 2;

    private Utils() {
    }

    public static DateTime dateTimeFromCommcareDateString(String dateStr) {
        DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
                .appendYear(YEAR_DIGITS_COUNT, YEAR_DIGITS_COUNT)
                .appendLiteral('-')
                .appendMonthOfYear(MONTH_DIGITS_COUNT)
                .appendLiteral('-')
                .appendDayOfMonth(DAY_DIGITS_COUNT)
                .toFormatter();

        return dateStr == null ? null : dateFormatter.parseDateTime(dateStr);
    }

    public static <T> Collection<T> readJSON(InputStream stream,
                                             Class<? extends Collection> collection,
                                             Class<T> element) throws IOException {
        StringWriter writer = new StringWriter();
        ObjectMapper mapper = new ObjectMapper();
        IOUtils.copy(stream, writer);

        TypeFactory typeFactory = mapper.getTypeFactory();

        return mapper.readValue(
                writer.toString(),
                typeFactory.constructCollectionType(collection, element)
        );
    }
}
