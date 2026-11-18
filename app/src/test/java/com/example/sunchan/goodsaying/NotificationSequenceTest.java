package com.example.sunchan.goodsaying;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class NotificationSequenceTest {
    @Test
    public void eligibleSayingsFollowCustomOrderAndWrap() {
        assertNext(10, false, -1, new Integer[]{10, 5, 8}, false, false, false);
        assertNext(5, false, 10, new Integer[]{10, 5, 8}, true, false, false);
        assertNext(8, false, 5, new Integer[]{10, 5, 8}, true, true, false);
        assertNext(10, true, 8, new Integer[]{10, 5, 8}, true, true, true);
    }

    @Test
    public void reorderSkipsSeenSayingsAndAvoidsRepeatAtCycleBoundary() {
        assertNext(5, false, 8, new Integer[]{8, 10, 5}, true, true, false);
        assertNext(10, true, 8, new Integer[]{8, 10, 5}, true, true, true);
    }

    @Test
    public void removedOrDisabledLastDoesNotLoseCycleProgress() {
        assertNext(5, false, 8, new Integer[]{10, 5}, true, false);
    }

    @Test
    public void insertedAndReenabledSayingsRespectCurrentCycle() {
        assertNext(99, false, 5, new Integer[]{10, 5, 99}, true, true, false);
        assertNext(5, false, 99, new Integer[]{10, 5, 99}, true, false, true);
    }

    @Test
    public void noEligibleSayingsAndSingleEligibleSaying() {
        assertNull(NotificationSequence.next(Collections.emptyList(), Collections.emptyList(), -1));
        assertNext(10, true, 10, new Integer[]{10}, true);
    }

    private static void assertNext(int expectedId, boolean newCycle, int lastId,
                                   Integer[] ids, Boolean... seen) {
        NotificationSequence.Choice choice = NotificationSequence.next(
                Arrays.asList(ids), Arrays.asList(seen), lastId);
        assertEquals(expectedId, choice.id);
        assertEquals(newCycle, choice.newCycle);
    }
}
