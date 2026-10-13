package cn.hdfk7.boot.starter.shardingsphere.algorithm;

import cn.hdfk7.boot.starter.shardingsphere.util.ShardingUtils;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.time.LocalDate;
import java.util.Collection;
import java.util.OptionalLong;

public class MonthRangeShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {
    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<Comparable<?>> preciseShardingValue) {
        String logicTableName = preciseShardingValue.getLogicTableName();
        LocalDate localDate = ShardingUtils.toLocalDate(preciseShardingValue.getValue());
        String targetTable = ShardingUtils.getMonthTable(logicTableName, localDate);
        return ShardingUtils.requireAvailableTable(collection, targetTable);
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Comparable<?>> rangeShardingValue) {
        String logicTableName = rangeShardingValue.getLogicTableName();
        return ShardingUtils.doRangeSharding(collection, rangeShardingValue.getValueRange(), 1, table -> getPeriod(table, logicTableName));
    }

    private static OptionalLong getPeriod(String tableName, String logicTableName) {
        String prefix = logicTableName + "_";
        if (!tableName.startsWith(prefix)) {
            return OptionalLong.empty();
        }
        String suffix = tableName.substring(prefix.length());
        if (!suffix.matches("[0-9]{6}")) {
            return OptionalLong.empty();
        }
        int year = Integer.parseInt(suffix.substring(0, 4));
        int month = Integer.parseInt(suffix.substring(4));
        if (month < 1 || month > 12) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(year * 12L + month - 1);
    }
}
