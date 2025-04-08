package edu.prakriti.mealmate;

import org.junit.Test;
import static org.junit.Assert.*;

import edu.prakriti.mealmate.utils.AgeCalculate;

public class AgeCalculateTest {

    @Test
    public void testCalculateAge_ValidDate() {
        AgeCalculate ageCalc = new AgeCalculate();

        // Assume DOB: 01/01/2000 — should calculate correctly
        int age = ageCalc.calculateAge("01/01/2000");

        // Age will depend on current year, so we assert age > 0
        assertTrue("Age should be greater than 0", age > 0);
    }

    @Test
    public void testCalculateAge_TodayBirthday() {
        AgeCalculate ageCalc = new AgeCalculate();

        // Get today's date in dd/MM/yyyy
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
        String todayStr = sdf.format(new java.util.Date());

        // Age for today’s DOB should be 0
        int age = ageCalc.calculateAge(todayStr);
        assertEquals(0, age);
    }

    @Test
    public void testCalculateAge_InvalidDateFormat() {
        AgeCalculate ageCalc = new AgeCalculate();

        // Wrong format input
        int age = ageCalc.calculateAge("2020-01-01");

        // Should return 0 for invalid format
        assertEquals(0, age);
    }

    @Test
    public void testCalculateAge_FutureDate() {
        AgeCalculate ageCalc = new AgeCalculate();

        // Future date, should return negative or 0
        int age = ageCalc.calculateAge("01/01/2100");
        assertTrue("Age should not be positive for future DOB", age <= 0);
    }
}
