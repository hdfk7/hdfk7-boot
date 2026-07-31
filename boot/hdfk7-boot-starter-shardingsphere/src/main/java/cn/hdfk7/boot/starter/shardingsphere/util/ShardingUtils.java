package cn.hdfk7.boot.starter.shardingsphere.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class ShardingUtils {

    private ShardingUtils() {
    }

    public static String getYearTable(String logicTableName, LocalDate localDate) {
        validateDate(logicTableName, localDate);
        return getTable(logicTableName, String.valueOf(localDate.getYear()));
    }

    public static List<String> getYearTables(String logicTableName, LocalDate startTime, LocalDate endTime) {
        validateDateRange(logicTableName, startTime, endTime);

        List<String> yearTables = new ArrayList<>();
        for (int year = startTime.getYear(); year <= endTime.getYear(); year++) {
            yearTables.add(getTable(logicTableName, String.valueOf(year)));
        }
        return yearTables;
    }

    public static String getMonthTable(String logicTableName, LocalDate localDate) {
        validateDate(logicTableName, localDate);
        return getTable(logicTableName, getMonthText(YearMonth.from(localDate)));
    }

    public static List<String> getMonthTables(String logicTableName, LocalDate startTime, LocalDate endTime) {
        validateDateRange(logicTableName, startTime, endTime);

        List<String> monthTables = new ArrayList<>();
        YearMonth startMonth = YearMonth.from(startTime);
        YearMonth endMonth = YearMonth.from(endTime);
        for (YearMonth month = startMonth; !month.isAfter(endMonth); month = month.plusMonths(1)) {
            monthTables.add(getTable(logicTableName, getMonthText(month)));
        }
        return monthTables;
    }

    private static void validateDate(String logicTableName, LocalDate localDate) {
        if (logicTableName == null || localDate == null) {
            throw new IllegalArgumentException("logicTableName and localDate must not be null");
        }
    }

    private static void validateDateRange(String logicTableName, LocalDate startTime, LocalDate endTime) {
        if (logicTableName == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("logicTableName, startTime and endTime must not be null");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("startTime must not be after endTime");
        }
    }

    private static String getTable(String logicTableName, String suffix) {
        return String.join("_", logicTableName, suffix);
    }

    private static String getMonthText(YearMonth month) {
        return month.toString().replace("-", "");
    }
}
