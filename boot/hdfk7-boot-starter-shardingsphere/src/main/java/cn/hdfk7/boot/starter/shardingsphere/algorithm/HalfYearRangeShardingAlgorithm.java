package cn.hdfk7.boot.starter.shardingsphere.algorithm;

import cn.hdfk7.boot.starter.shardingsphere.util.ShardingUtils;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.time.LocalDate;
import java.util.Collection;
import java.util.OptionalLong;

public class HalfYearRangeShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {
    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<Comparable<?>> preciseShardingValue) {
        String logicTableName = preciseShardingValue.getLogicTableName();
        LocalDate localDate = ShardingUtils.toLocalDate(preciseShardingValue.getValue());
        String yearTable = ShardingUtils.getYearTable(logicTableName, localDate);
        String halfYear = localDate.getMonthValue() > 6 ? "2" : "1";
        String targetTable = String.join("_", yearTable, halfYear);
        return ShardingUtils.requireAvailableTable(collection, targetTable);
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Comparable<?>> rangeShardingValue) {
        String logicTableName = rangeShardingValue.getLogicTableName();
        return ShardingUtils.doRangeSharding(collection, rangeShardingValue.getValueRange(), 6, table -> getPeriod(table, logicTableName));
    }

    private static OptionalLong getPeriod(String tableName, String logicTableName) {
        String prefix = logicTableName + "_";
        if (!tableName.startsWith(prefix)) {
            return OptionalLong.empty();
        }
        String[] parts = tableName.substring(prefix.length()).split("_");
        if (parts.length != 2 || !("1".equals(parts[1]) || "2".equals(parts[1]))) {
            return OptionalLong.empty();
        }
        try {
            int year = Integer.parseInt(parts[0]);
            int halfYear = Integer.parseInt(parts[1]);
            return OptionalLong.of(year * 2L + halfYear - 1);
        } catch (NumberFormatException ignored) {
            return OptionalLong.empty();
        }
    }
}
