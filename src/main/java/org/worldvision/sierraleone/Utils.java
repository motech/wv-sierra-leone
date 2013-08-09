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
    private Utils() { }

    public static DateTime dateTimeFromCommcareDateString(String dateStr) {
        DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
                .appendYear(4, 4)
                .appendLiteral('-')
                .appendMonthOfYear(2)
                .appendLiteral('-')
                .appendDayOfMonth(2)
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
