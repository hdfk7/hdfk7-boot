package cn.hdfk7.boot.starter.shardingsphere.algorithm;

import cn.hdfk7.boot.starter.shardingsphere.util.ShardingUtils;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        String logicTableName = rangeShardingValue.getLogicTableName();
        LocalDate startTime = toLocalDate(rangeShardingValue.getValueRange().lowerEndpoint());
        LocalDate endTime = toLocalDate(rangeShardingValue.getValueRange().upperEndpoint());

        List<String> yearTables = ShardingUtils.getYearTables(logicTableName, startTime, endTime);
        List<String> firstHalf = yearTables.stream()
                .map(yearTable -> String.join("_", yearTable, "1"))
                .collect(Collectors.toList());
        List<String> nextHalf = yearTables.stream()
                .map(yearTable -> String.join("_", yearTable, "2"))
                .toList();

        firstHalf.addAll(nextHalf);

        if (Objects.equals("2", getHalfYear(startTime))) {
            firstHalf.remove(String.join("_", ShardingUtils.getYearTable(logicTableName, startTime), "1"));
        }
        if (Objects.equals("1", getHalfYear(endTime))) {
            firstHalf.remove(String.join("_", ShardingUtils.getYearTable(logicTableName, endTime), "2"));
        }

        return firstHalf;
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
}
