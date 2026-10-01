package com.novax.util;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 计算间隔天数
     * ex 2024-12-20 23:59:50  2024-12-21 23:59:49  间隔2天
     */
    public static long getIntervalDays(Date startDate, Date endDate) {
        LocalDate start = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate end = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    /**
     * 计算相差天数
     * ex 2024-12-20 23:59:50  2024-12-21 23:59:49  相差0天
     */
    public static long getDiscrepantDays(Date startDate, Date endDate) {
        LocalDateTime start = LocalDateTime.parse(
                FMT_DATETIME.format(startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()),
                FMT_DATETIME);
        LocalDateTime end = LocalDateTime.parse(
                FMT_DATETIME.format(endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()),
                FMT_DATETIME);
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 获取当前时间 n 天之前的凌晨时间
     */
    public static Date getZeroTime(int n) {
        LocalDate date = LocalDate.now().minusDays(n - 1);
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取当前时间 n 天之前的凌点时间
     */
    public static Date getZeroTime2(int n) {
        LocalDate date = LocalDate.now().minusDays(n);
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取当前时间n天之前的时间 具体到秒
     */
    public static Date addDay(Date date, int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(5, n);
        return calendar.getTime();
    }

    // 本周一时间
    public static Date getThisMonday() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return Date.from(monday.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // 判断是否周一
    public static boolean isNotMonday() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek != Calendar.MONDAY;
    }

    // 判断是否同一天
    public static boolean isSameDay(String day1, Date day2) {
        String createTimeStr = new SimpleDateFormat("yyyy-MM-dd").format(day2);
        return day1.equals(createTimeStr);
    }

    public static void main(String[] args) {
        System.out.println(isNotMonday());
        System.out.println(getThisMonday());
        System.out.println(getZeroTime2(1));
    }
}
