package com.example.practice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Matches the day's approvals in the acquirer's journal with the records of a clearing file. */
public final class ClearingMatcher {

    /** One side's view of a sale: terminal (DE 41), retrieval reference number (DE 37), minor units. */
    public record Item(String terminal, String rrn, long amount) { }

    public enum Kind { MATCHED, AMOUNT_DIFFERS, DUPLICATE_PRESENTMENT, NOT_AUTHORIZED, NOT_PRESENTED }

    /** One finding: the journal's item and the clearing record, either of them null when absent. */
    public record Result(Kind kind, Item ours, Item theirs) { }

    private ClearingMatcher() {
    }

    /**
     * One result per clearing record, in the file's order, then one NOT_PRESENTED per approval no
     * record presented, in the journal's order.
     */
    public static List<Result> match(List<Item> journal, List<Item> clearing) {
        Map<String, Item> open = new LinkedHashMap<>();       // approvals not presented yet
        for (Item i : journal)
            open.put(key(i), i);
        Map<String, Item> presented = new HashMap<>();        // approvals already presented
        List<Result> results = new ArrayList<>();
        for (Item c : clearing)
            results.add(pair(c, open, presented));
        results.addAll(leftOver(open));
        return results;
    }

    /** What identifies one sale in both lists. */
    static String key(Item i) {
        return i.terminal() + "/" + i.rrn();
    }

    /** The finding for one clearing record; moves its approval from open to presented. */
    static Result pair(Item record, Map<String, Item> open, Map<String, Item> presented) {
        String k = key(record);
        Item ours = open.remove(k);
        if (ours != null) {
            presented.put(k, ours);
            return new Result(ours.amount() == record.amount() ? Kind.MATCHED : Kind.AMOUNT_DIFFERS, ours, record);
        }
        return new Result(Kind.NOT_AUTHORIZED, null, record);
    }

    /** The findings for the approvals no clearing record presented. */
    static List<Result> leftOver(Map<String, Item> open) {
        List<Result> left = new ArrayList<>();
        for (Item ours : open.values())
            left.add(new Result(Kind.NOT_PRESENTED, ours, null));
        return left;
    }
}
