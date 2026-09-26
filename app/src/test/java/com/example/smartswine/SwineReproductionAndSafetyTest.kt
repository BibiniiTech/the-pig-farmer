package com.example.smartswine

import com.example.smartswine.model.StaffMember
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SwineReproductionAndSafetyTest {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Test
    fun testStaffMember_isRestrictedRole() {
        // Operational roles should be restricted from financial/HR data
        val herdsman = StaffMember(role = "Herdsman")
        val farmHand = StaffMember(role = "Farm Hand")
        val worker = StaffMember(role = "Worker")
        val laborer = StaffMember(role = "Pig Laborer")
        val attendant = StaffMember(role = "Barn Attendant")

        assertTrue(herdsman.isRestrictedRole())
        assertTrue(farmHand.isRestrictedRole())
        assertTrue(worker.isRestrictedRole())
        assertTrue(laborer.isRestrictedRole())
        assertTrue(attendant.isRestrictedRole())

        // Administrative / managerial roles should have access
        val owner = StaffMember(role = "Owner")
        val manager = StaffMember(role = "Farm Manager")
        val vet = StaffMember(role = "Veterinarian")
        val accountant = StaffMember(role = "Accountant")

        assertFalse(owner.isRestrictedRole())
        assertFalse(manager.isRestrictedRole())
        assertFalse(vet.isRestrictedRole())
        assertFalse(accountant.isRestrictedRole())
    }

    @Test
    fun testReproductionMilestones_gestationAndFarrowing() {
        // Gestation period: 114 days (3 months, 3 weeks, 3 days)
        val matingDateStr = "2026-01-01"
        val matingCal = Calendar.getInstance().apply {
            time = sdf.parse(matingDateStr)!!
        }

        // 1. Day 21 Heat Check
        val heatCheckCal = (matingCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 21) }
        assertEquals("2026-01-22", sdf.format(heatCheckCal.time))

        // 2. Day 110 Move to Farrowing Pen
        val farrowingPenCal = (matingCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 110) }
        assertEquals("2026-04-21", sdf.format(farrowingPenCal.time))

        // 3. Day 114 Expected Farrowing Due Date
        val farrowingDueCal = (matingCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 114) }
        assertEquals("2026-04-25", sdf.format(farrowingDueCal.time))

        // 4. Day 28 Weaning & Day 5 post-weaning heat check (WSI)
        val farrowingDateStr = "2026-04-25"
        val farrowCal = Calendar.getInstance().apply {
            time = sdf.parse(farrowingDateStr)!!
        }
        val weaningCal = (farrowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 28) }
        assertEquals("2026-05-23", sdf.format(weaningCal.time))

        val postWeanHeatCal = (weaningCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 5) }
        assertEquals("2026-05-28", sdf.format(postWeanHeatCal.time))
    }

    @Test
    fun testVeterinaryDrugWithdrawal_safeSlaughterCalculation() {
        val treatmentDateStr = "2026-06-01"
        val withdrawalPeriodDays = 28 // e.g. Ivermectin / Oxytetracycline LA
        val treatCal = Calendar.getInstance().apply {
            time = sdf.parse(treatmentDateStr)!!
        }

        val safeCal = (treatCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, withdrawalPeriodDays) }
        val safeSlaughterDateStr = sdf.format(safeCal.time)
        assertEquals("2026-06-29", safeSlaughterDateStr)

        // Pig treated on June 1 with 28-day withdrawal should be active on June 15
        val testDateWithinWithdrawal = sdf.parse("2026-06-15")!!
        val isUnderWithdrawal = safeCal.time.after(testDateWithinWithdrawal)
        assertTrue(isUnderWithdrawal)

        // Pig treated on June 1 with 28-day withdrawal should be safe on July 1
        val testDatePastWithdrawal = sdf.parse("2026-07-01")!!
        val isSafeForSlaughter = testDatePastWithdrawal.after(safeCal.time) || testDatePastWithdrawal == safeCal.time
        assertTrue(isSafeForSlaughter)
    }

    @Test
    fun testAverageDailyGain_andTargetMarketDate() {
        val initialWeight = 25.0 // kg
        val currentWeight = 55.0 // kg
        val daysBetween = 40.0 // 40 days of growth

        val weightGain = currentWeight - initialWeight // 30 kg
        val adgKgPerDay = weightGain / daysBetween // 0.75 kg/day = 750 g/day
        assertEquals(0.75, adgKgPerDay, 0.001)

        val adgGramsPerDay = adgKgPerDay * 1000.0
        assertEquals(750.0, adgGramsPerDay, 0.001)

        // Rating: >= 700 is Optimal
        val rating = when {
            adgGramsPerDay >= 700.0 -> "Optimal"
            adgGramsPerDay >= 500.0 -> "Normal"
            else -> "Low"
        }
        assertEquals("Optimal", rating)

        // Target Market Weight = 90.0 kg
        val targetMarketWeight = 90.0
        val remainingWeight = targetMarketWeight - currentWeight // 35 kg
        val daysRemainingToMarket = remainingWeight / adgKgPerDay // 35 / 0.75 = 46.666 days
        assertEquals(46.66, daysRemainingToMarket, 0.01)
    }

    @Test
    fun testUnitEconomics_costOfProductionAndBreakEven() {
        val totalFarmExpenses = 15000.0 // USD / Local currency
        val totalHerdWeightKg = 5000.0 // 5000 kg total live weight

        // Cost of Production (COP) per kg live weight
        val copPerKg = totalFarmExpenses / totalHerdWeightKg // 3.00 per kg
        assertEquals(3.0, copPerKg, 0.001)

        // 90 kg Finisher Break-Even Price
        val breakEvenPrice90kg = copPerKg * 90.0 // 270.00
        assertEquals(270.0, breakEvenPrice90kg, 0.001)

        // Target Selling Price with 20% Profit Margin
        val targetMarginMultiplier = 1.20
        val targetSellingPrice90kg = breakEvenPrice90kg * targetMarginMultiplier // 324.00
        assertEquals(324.0, targetSellingPrice90kg, 0.001)
    }

    @Test
    fun testMeatWithdrawalNotification_timing() {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val in21Days = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 21) }
        val in2Days = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 2) }
        val in1Day = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val today = (todayCal.clone() as Calendar)
        val overdue1Day = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }

        val str21Days = sdf.format(in21Days.time)
        val str2Days = sdf.format(in2Days.time)
        val str1Day = sdf.format(in1Day.time)
        val strToday = sdf.format(today.time)
        val strOverdue = sdf.format(overdue1Day.time)

        // Day 0: 21 days in the future should NOT trigger withdrawal notification
        assertFalse(com.example.smartswine.utils.DateUtils.isTaskDueOrUpcoming(str21Days, isWithdrawal = true))
        assertFalse(com.example.smartswine.utils.DateUtils.isTaskDueOrUpcoming(str21Days, isWithdrawal = false))

        // 2 days in the future: regular task triggers upcoming alert, but withdrawal MUST NOT alert early
        assertTrue(com.example.smartswine.utils.DateUtils.isTaskDueOrUpcoming(str2Days, isWithdrawal = false, maxUpcomingDays = 2))
        assertFalse(com.example.smartswine.utils.DateUtils.isTaskDueOrUpcoming(str2Days, isWithdrawal = true))

        // 1 day in the future (Tomorrow): regular task alerts, withdrawal MUST NOT alert early
        assertTrue(com.example.smartswine.utils.DateUtils.isTaskDueOrUpcoming(str1Day, isWithdrawal = false))
        assertFalse(com.example.smartswine.utils.DateUtils.isTaskDueOrUpcoming(str1Day, isWithdrawal = true))
        assertFalse(com.example.smartswine.utils.DateUtils.isTaskDueOrUpcoming("Tomorrow", isWithdrawal = true))

        // Day 21 (Today): Withdrawal is now CLEARED -> notification triggers
        assertTrue(com.example.smartswine.utils.DateUtils.isTaskDueOrUpcoming(strToday, isWithdrawal = true))
        assertTrue(com.example.smartswine.utils.DateUtils.isTaskDueOrUpcoming("Today", isWithdrawal = true))

        // Overdue / Cleared in the past: Remains active until marked done/archived
        assertTrue(com.example.smartswine.utils.DateUtils.isTaskDueOrUpcoming(strOverdue, isWithdrawal = true))
    }
}

