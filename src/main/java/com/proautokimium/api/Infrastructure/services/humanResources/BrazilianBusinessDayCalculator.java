package com.proautokimium.api.Infrastructure.services.humanResources;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.HashSet;
import java.util.Set;

@Component
public class BrazilianBusinessDayCalculator {

    private static final Set<MonthDay> FIXED_HOLIDAYS = Set.of(
            MonthDay.of(1, 1),   // Confraternização Universal
            MonthDay.of(4, 21),  // Tiradentes
            MonthDay.of(5, 1),   // Dia do Trabalho
            MonthDay.of(9, 7),   // Independência
            MonthDay.of(10, 12), // Nossa Senhora Aparecida
            MonthDay.of(11, 2),  // Finados
            MonthDay.of(11, 15), // Proclamação da República
            MonthDay.of(12, 25)  // Natal
    );

    public long countBusinessDays(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) return 0;

        Set<LocalDate> holidays = new HashSet<>();
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            holidays.addAll(getHolidaysForYear(year));
        }

        long count = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            DayOfWeek dow = current.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !holidays.contains(current)) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }

    private Set<LocalDate> getHolidaysForYear(int year) {
        Set<LocalDate> holidays = new HashSet<>();
        for (MonthDay md : FIXED_HOLIDAYS) {
            holidays.add(md.atYear(year));
        }
        LocalDate easter = easterDate(year);
        holidays.add(easter.minusDays(47)); // Carnaval segunda
        holidays.add(easter.minusDays(46)); // Carnaval terça
        holidays.add(easter.minusDays(2));  // Sexta-feira Santa
        holidays.add(easter.plusDays(60));   // Corpus Christi
        return holidays;
    }

    private LocalDate easterDate(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }

}
