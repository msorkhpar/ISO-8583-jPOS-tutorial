package com.example.practice;

import com.example.practice.RangeRouter.Range;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RangeRouterTest {

    private static final String VISA_CARD = "4111111111111111";
    private static final String MC_CARD = "5555555555554444";

    @Test
    void cardsGoToTheRangeThatHoldsThem() {
        RangeRouter router = new RangeRouter();
        router.reload(List.of(
                new Range("40000000", "49999999", "network-v"),
                new Range("51000000", "55999999", "network-m")));
        assertEquals("network-v", router.destinationFor(VISA_CARD));
        assertEquals("network-m", router.destinationFor(MC_CARD));

        router.reload(List.of(new Range("40000000", "49999999", "network-v")));
        assertNull(router.destinationFor(MC_CARD), "the new file has no range for this card");
    }

    @Test
    void theNarrowestRangeDecidesInEitherOrder() {
        Range network = new Range("41000000", "41999999", "network-v");
        Range ownCards = new Range("41111110", "41111119", "own-host");
        RangeRouter router = new RangeRouter();
        router.reload(List.of(network, ownCards));
        assertEquals("own-host", router.destinationFor(VISA_CARD));
        router.reload(List.of(ownCards, network));
        assertEquals("own-host", router.destinationFor(VISA_CARD));
    }

    @Test
    void aRangeIncludesBothOfItsEnds() {
        RangeRouter router = new RangeRouter();
        router.reload(List.of(
                new Range("40000000", "49999999", "network-v"),
                new Range("41111000", "41111111", "processor-p"),
                new Range("50000000", "59999999", "network-m"),
                new Range("55555555", "55555560", "processor-q")));
        assertEquals("processor-p", router.destinationFor(VISA_CARD), "41111111 is the range's last prefix");
        assertEquals("processor-q", router.destinationFor(MC_CARD), "55555555 is the range's first prefix");
    }

    @Test
    void aLoadedFileStaysAsItWasLoaded() {
        RangeRouter router = new RangeRouter();
        List<Range> parsed = new ArrayList<>(List.of(new Range("41000000", "41999999", "network-v")));
        router.reload(parsed);
        parsed.clear();                                   // the parser starts on the next file
        parsed.add(new Range("41000000", "41999999", "half-read"));
        assertEquals("network-v", router.destinationFor(VISA_CARD));
    }
}
