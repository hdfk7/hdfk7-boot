package cn.hdfk7.boot.starter.shardingsphere.algorithm;

import cn.hdfk7.boot.starter.shardingsphere.util.ShardingUtils;
import com.google.common.collect.Range;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.IntPredicate;

public class HalfYearRangeShardingAlgorithm implements StandardShardingAlgorithm<Long> {
    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<Long> preciseShardingValue) {
        String logicTableName = preciseShardingValue.getLogicTableName();
        LocalDate localDate = toLocalDate(preciseShardingValue.getValue());
        String yearTable = ShardingUtils.getYearTable(logicTableName, localDate);
        String halfYear = getHalfYear(localDate);
        return String.join("_", yearTable, halfYear);
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        Range<Long> valueRange = rangeShardingValue.getValueRange();
        String logicTableName = rangeShardingValue.getLogicTableName();
        if (!valueRange.hasLowerBound() && !valueRange.hasUpperBound()) {
            return List.copyOf(collection);
        }
        if (!valueRange.hasLowerBound()) {
            int upperPeriod = getPeriod(toLocalDate(valueRange.upperEndpoint()));
            return filterAvailableTables(collection, logicTableName, period -> period <= upperPeriod);
        }
        if (!valueRange.hasUpperBound()) {
            int lowerPeriod = getPeriod(toLocalDate(valueRange.lowerEndpoint()));
            return filterAvailableTables(collection, logicTableName, period -> period >= lowerPeriod);
        }

        int lowerPeriod = getPeriod(toLocalDate(valueRange.lowerEndpoint()));
        int upperPeriod = getPeriod(toLocalDate(valueRange.upperEndpoint()));
        return filterAvailableTables(collection, logicTableName, period -> period >= lowerPeriod && period <= upperPeriod);
    }

    private static LocalDate toLocalDate(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
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
