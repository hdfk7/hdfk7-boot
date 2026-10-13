package cn.hdfk7.boot.starter.shardingsphere.algorithm;

import cn.hdfk7.boot.starter.shardingsphere.util.ShardingUtils;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.IntPredicate;

public class HalfYearRangeShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {
    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<Comparable<?>> preciseShardingValue) {
        String logicTableName = preciseShardingValue.getLogicTableName();
        LocalDate localDate = toLocalDate(preciseShardingValue.getValue());
        String yearTable = ShardingUtils.getYearTable(logicTableName, localDate);
        String halfYear = getHalfYear(localDate);
        String targetTable = String.join("_", yearTable, halfYear);
        if (!collection.contains(targetTable)) {
            throw new IllegalArgumentException("Sharding target table is not available: " + targetTable);
        }
        return targetTable;
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Comparable<?>> rangeShardingValue) {
        Range<Comparable<?>> valueRange = rangeShardingValue.getValueRange();
        String logicTableName = rangeShardingValue.getLogicTableName();
        if (valueRange.isEmpty()) {
            return List.of();
        }
        if (!valueRange.hasLowerBound() && !valueRange.hasUpperBound()) {
            return List.copyOf(collection);
        }
        if (!valueRange.hasLowerBound()) {
            int upperPeriod = getBoundaryPeriod(valueRange.upperEndpoint(), valueRange.upperBoundType(), false);
            return filterAvailableTables(collection, logicTableName, period -> period <= upperPeriod);
        }
        if (!valueRange.hasUpperBound()) {
            int lowerPeriod = getBoundaryPeriod(valueRange.lowerEndpoint(), valueRange.lowerBoundType(), true);
            return filterAvailableTables(collection, logicTableName, period -> period >= lowerPeriod);
        }

        int lowerPeriod = getBoundaryPeriod(valueRange.lowerEndpoint(), valueRange.lowerBoundType(), true);
        int upperPeriod = getBoundaryPeriod(valueRange.upperEndpoint(), valueRange.upperBoundType(), false);
        return filterAvailableTables(collection, logicTableName, period -> period >= lowerPeriod && period <= upperPeriod);
    }

    private static int getBoundaryPeriod(Comparable<?> value, BoundType boundType, boolean lowerBound) {
        LocalDateTime dateTime = toLocalDateTime(value);
        LocalDate date = dateTime.toLocalDate();
        int period = getPeriod(date);
        if (boundType == BoundType.CLOSED) {
            return period;
        }
        if (!lowerBound) {
            boolean firstDay = date.getDayOfMonth() == 1
                    && (date.getMonthValue() == 1 || date.getMonthValue() == 7);
            return firstDay && dateTime.toLocalTime().equals(LocalTime.MIDNIGHT) ? period - 1 : period;
        }
        boolean lastDay = date.getDayOfMonth() == date.lengthOfMonth()
                && (date.getMonthValue() == 6 || date.getMonthValue() == 12);
        boolean dateOnly = value instanceof LocalDate || value instanceof java.sql.Date;
        LocalTime lastTime = value instanceof Long
                || (value instanceof Date && !(value instanceof java.sql.Timestamp))
                ? LocalTime.of(23, 59, 59, 999_000_000) : LocalTime.MAX;
        return lastDay && (dateOnly || dateTime.toLocalTime().equals(lastTime)) ? period + 1 : period;
    }

    private static LocalDate toLocalDate(Comparable<?> value) {
        return toLocalDateTime(value).toLocalDate();
    }

    private static LocalDateTime toLocalDateTime(Comparable<?> value) {
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

    private static String getHalfYear(LocalDate localDate) {
        if (localDate.getMonthValue() > 6) {
            return "2";
        }
        return "1";
    }

    private static Collection<String> filterAvailableTables(Collection<String> availableTables, String logicTableName, IntPredicate periodPredicate) {
        return availableTables.stream()
                .filter(table -> {
                    OptionalInt period = getPeriod(table, logicTableName);
                    return period.isEmpty() || periodPredicate.test(period.getAsInt());
                })
                .toList();
    }

    private static OptionalInt getPeriod(String tableName, String logicTableName) {
        String prefix = logicTableName + "_";
        if (!tableName.startsWith(prefix)) {
            return OptionalInt.empty();
        }
        String[] parts = tableName.substring(prefix.length()).split("_");
        if (parts.length != 2 || !("1".equals(parts[1]) || "2".equals(parts[1]))) {
            return OptionalInt.empty();
        }
        try {
            int year = Integer.parseInt(parts[0]);
            int halfYear = Integer.parseInt(parts[1]);
            return OptionalInt.of(year * 2 + halfYear - 1);
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }

    private static int getPeriod(LocalDate date) {
        return date.getYear() * 2 + (date.getMonthValue() > 6 ? 1 : 0);
    }
}
