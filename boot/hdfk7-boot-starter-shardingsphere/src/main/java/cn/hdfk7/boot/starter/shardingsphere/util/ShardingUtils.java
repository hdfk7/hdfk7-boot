package cn.hdfk7.boot.starter.shardingsphere.util;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.LongPredicate;

public final class ShardingUtils {

    private ShardingUtils() {
    }

    public static String getYearTable(String logicTableName, LocalDate localDate) {
        validateDate(logicTableName, localDate);
        return getTable(logicTableName, String.valueOf(localDate.getYear()));
    }

    public static String getMonthTable(String logicTableName, LocalDate localDate) {
        validateDate(logicTableName, localDate);
        return getTable(logicTableName, getMonthText(YearMonth.from(localDate)));
    }

    public static String requireAvailableTable(Collection<String> availableTables, String targetTable) {
        if (!availableTables.contains(targetTable)) {
            throw new IllegalArgumentException("Sharding target table is not available: " + targetTable);
        }
        return targetTable;
    }

    public static Collection<String> doRangeSharding(Collection<String> availableTables, Range<Comparable<?>> valueRange, int monthsPerPeriod, Function<String, OptionalLong> tablePeriodResolver) {
        if (valueRange.isEmpty()) {
            return List.of();
        }
        if (!valueRange.hasLowerBound() && !valueRange.hasUpperBound()) {
            return List.copyOf(availableTables);
        }
        if (!valueRange.hasLowerBound()) {
            long upperPeriod = getBoundaryPeriod(valueRange.upperEndpoint(), valueRange.upperBoundType(), false, monthsPerPeriod);
            return filterAvailableTables(availableTables, tablePeriodResolver, period -> period <= upperPeriod);
        }
        if (!valueRange.hasUpperBound()) {
            long lowerPeriod = getBoundaryPeriod(valueRange.lowerEndpoint(), valueRange.lowerBoundType(), true, monthsPerPeriod);
            return filterAvailableTables(availableTables, tablePeriodResolver, period -> period >= lowerPeriod);
        }
        long lowerPeriod = getBoundaryPeriod(valueRange.lowerEndpoint(), valueRange.lowerBoundType(), true, monthsPerPeriod);
        long upperPeriod = getBoundaryPeriod(valueRange.upperEndpoint(), valueRange.upperBoundType(), false, monthsPerPeriod);
        return filterAvailableTables(availableTables, tablePeriodResolver, period -> period >= lowerPeriod && period <= upperPeriod);
    }

    public static LocalDate toLocalDate(Comparable<?> value) {
        return toLocalDateTime(value).toLocalDate();
    }

    public static LocalDateTime toLocalDateTime(Comparable<?> value) {
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDateTime();
        }
        if (value instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.toLocalDateTime();
        }
        if (value instanceof Instant instant) {
            return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().atStartOfDay();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof Date date) {
            return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        if (value instanceof Long timestamp) {
            return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        throw new IllegalArgumentException("Unsupported sharding value type: " + (value == null ? "null" : value.getClass().getName()));
    }

    public static long getPeriod(LocalDate date, int monthsPerPeriod) {
        if (monthsPerPeriod < 1 || monthsPerPeriod > 12 || 12 % monthsPerPeriod != 0) {
            throw new IllegalArgumentException("monthsPerPeriod must be a positive divisor of 12");
        }
        return date.getYear() * (12L / monthsPerPeriod) + (date.getMonthValue() - 1) / monthsPerPeriod;
    }

    private static long getBoundaryPeriod(Comparable<?> value, BoundType boundType, boolean lowerBound, int monthsPerPeriod) {
        LocalDateTime dateTime = toLocalDateTime(value);
        LocalDate date = dateTime.toLocalDate();
        long period = getPeriod(date, monthsPerPeriod);
        if (boundType == BoundType.CLOSED) {
            return period;
        }
        if (!lowerBound) {
            boolean firstDay = date.getDayOfMonth() == 1 && (date.getMonthValue() - 1) % monthsPerPeriod == 0;
            return firstDay && dateTime.toLocalTime().equals(LocalTime.MIDNIGHT) ? period - 1 : period;
        }
        boolean lastDay = date.getDayOfMonth() == date.lengthOfMonth() && date.getMonthValue() % monthsPerPeriod == 0;
        boolean dateOnly = value instanceof LocalDate || value instanceof java.sql.Date;
        LocalTime lastTime = (value instanceof Long || (value instanceof Date && !(value instanceof java.sql.Timestamp))) ? LocalTime.of(23, 59, 59, 999_000_000) : LocalTime.MAX;
        return lastDay && (dateOnly || dateTime.toLocalTime().equals(lastTime)) ? period + 1 : period;
    }

    private static Collection<String> filterAvailableTables(Collection<String> availableTables, Function<String, OptionalLong> tablePeriodResolver, LongPredicate periodPredicate) {
        return availableTables.stream()
                .filter(table -> {
                    OptionalLong period = tablePeriodResolver.apply(table);
                    return period.isEmpty() || periodPredicate.test(period.getAsLong());
                })
                .toList();
    }

    private static void validateDate(String logicTableName, LocalDate localDate) {
        if (logicTableName == null || localDate == null) {
            throw new IllegalArgumentException("logicTableName and localDate must not be null");
        }
    }

    private static String getTable(String logicTableName, String suffix) {
        return String.join("_", logicTableName, suffix);
    }

    private static String getMonthText(YearMonth month) {
        return month.toString().replace("-", "");
    }
}
