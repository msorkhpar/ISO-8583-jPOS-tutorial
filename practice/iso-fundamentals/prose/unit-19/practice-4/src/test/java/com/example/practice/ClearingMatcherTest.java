package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.practice.ClearingMatcher.Item;
import com.example.practice.ClearingMatcher.Kind;
import com.example.practice.ClearingMatcher.Result;

class ClearingMatcherTest {

    /** A new object every time, so that equal items are never the same instance. */
    private static Item item(String terminal, String rrn, long amount) {
        return new Item(new String(terminal), new String(rrn), amount);
    }

    // ---- main cases ----

    @Test
    void approvalsPresentedOnceAreMatchedInFileOrder() {
        Item a = item("29110005", "640212000311", 6140);
        Item b = item("29110005", "640212000312", 1299);
        Item c = item("29110006", "640212000420", 3000);

        List<Result> r = ClearingMatcher.match(List.of(a, b, c),
                List.of(item("29110006", "640212000420", 3000), item("29110005", "640212000311", 6140),
                        item("29110005", "640212000312", 1299)));

        assertEquals(List.of(
                new Result(Kind.MATCHED, c, item("29110006", "640212000420", 3000)),
                new Result(Kind.MATCHED, a, item("29110005", "640212000311", 6140)),
                new Result(Kind.MATCHED, b, item("29110005", "640212000312", 1299))), r);
    }

    @Test
    void aRecordWithNoApprovalIsNotAuthorized() {
        Item a = item("29110005", "640212000311", 6140);
        Item stray = item("29110006", "640212000999", 875);

        List<Result> r = ClearingMatcher.match(List.of(a),
                List.of(item("29110005", "640212000311", 6140), stray));

        assertEquals(List.of(
                new Result(Kind.MATCHED, a, item("29110005", "640212000311", 6140)),
                new Result(Kind.NOT_AUTHORIZED, null, stray)), r);
    }

    @Test
    void anApprovalNobodyPresentedIsReported() {
        Item a = item("29110005", "640212000311", 6140);
        Item b = item("29110005", "640212000312", 1299);

        List<Result> r = ClearingMatcher.match(List.of(a, b), List.of(item("29110005", "640212000311", 6140)));

        assertEquals(List.of(
                new Result(Kind.MATCHED, a, item("29110005", "640212000311", 6140)),
                new Result(Kind.NOT_PRESENTED, b, null)), r);
    }

    // ---- edge cases ----

    @Test
    void twoTerminalsMayUseTheSameRrn() {
        Item a = item("29110005", "640212000500", 6140);
        Item b = item("29110006", "640212000500", 3000);

        List<Result> r = ClearingMatcher.match(List.of(a, b),
                List.of(item("29110005", "640212000500", 6140), item("29110006", "640212000500", 3000)));

        assertEquals(List.of(
                new Result(Kind.MATCHED, a, item("29110005", "640212000500", 6140)),
                new Result(Kind.MATCHED, b, item("29110006", "640212000500", 3000))), r);
    }

    @Test
    void aSalePresentedTwiceIsADuplicateOfItsApproval() {
        Item a = item("29110006", "640212000420", 3000);

        List<Result> r = ClearingMatcher.match(List.of(a),
                List.of(item("29110006", "640212000420", 3000), item("29110006", "640212000420", 3000)));

        assertEquals(List.of(
                new Result(Kind.MATCHED, a, item("29110006", "640212000420", 3000)),
                new Result(Kind.DUPLICATE_PRESENTMENT, a, item("29110006", "640212000420", 3000))), r);

        Item tipped = item("29110005", "640212000313", 5000);
        List<Result> r2 = ClearingMatcher.match(List.of(tipped),
                List.of(item("29110005", "640212000313", 5000), item("29110005", "640212000313", 5750)));
        assertEquals(List.of(
                new Result(Kind.MATCHED, tipped, item("29110005", "640212000313", 5000)),
                new Result(Kind.DUPLICATE_PRESENTMENT, tipped, item("29110005", "640212000313", 5750))), r2);
    }

    @Test
    void aTipIsReportedAsAnAmountThatDiffers() {
        Item a = item("29110005", "640212000313", 5000);

        List<Result> r = ClearingMatcher.match(List.of(a), List.of(item("29110005", "640212000313", 5750)));

        assertEquals(List.of(new Result(Kind.AMOUNT_DIFFERS, a, item("29110005", "640212000313", 5750))), r);
    }

    @Test
    void unpresentedApprovalsComeInJournalOrder() {
        Item a = item("29110006", "640212000430", 800);
        Item b = item("29110005", "640212000311", 6140);
        Item c = item("29110005", "640212000301", 1299);

        List<Result> r = ClearingMatcher.match(List.of(a, b, c), List.of());

        assertEquals(List.of(
                new Result(Kind.NOT_PRESENTED, a, null),
                new Result(Kind.NOT_PRESENTED, b, null),
                new Result(Kind.NOT_PRESENTED, c, null)), r);
    }
}
