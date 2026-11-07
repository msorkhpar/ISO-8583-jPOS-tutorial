package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptedMuxTest {

    private static ISOMsg message(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000007325");
        m.set(11, stan);
        m.set(41, "29110047");
        return m;
    }

    /** Records what the listener form reports. */
    static final class Heard implements ISOResponseListener {
        final List<String> events = new ArrayList<>();

        @Override
        public void responseReceived(ISOMsg resp, Object handBack) {
            try {
                events.add("response " + (resp == null ? "null" : resp.getMTI() + " 39=" + resp.getString(39)) + " " + handBack);
            } catch (org.jpos.iso.ISOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void expired(Object handBack) {
            events.add("expired " + handBack);
        }
    }

    @Test
    void theScriptDecidesEachAnswer() throws Exception {
        ScriptedMux host = new ScriptedMux().then("00", "51", ScriptedMux.SILENT);
        ISOMsg first = host.request(message("0200", "008841"), 1000);
        ISOMsg second = host.request(message("0200", "008842"), 1000);
        ISOMsg third = host.request(message("0200", "008843"), 1000);
        ISOMsg fourth = host.request(message("0200", "008844"), 1000);
        assertNotNull(first);
        assertEquals("0210", first.getMTI());
        assertEquals("00", first.getString(39));
        assertEquals("51", second.getString(39));
        assertNull(third, "a silent host gives the client null");
        assertEquals("00", fourth.getString(39), "after the script, requests are approved");
    }

    @Test
    void theListenerGetsTheAnswer() throws Exception {
        ScriptedMux host = new ScriptedMux().then("05");
        Heard heard = new Heard();
        host.request(message("0200", "008850"), 1000, heard, "till-7");
        assertEquals(List.of("response 0210 39=05 till-7"), heard.events);
    }

    @Test
    void aRequestNobodyAnsweredWasStillSent() throws Exception {
        ScriptedMux host = new ScriptedMux().then(ScriptedMux.SILENT);
        assertNull(host.request(message("0200", "008861"), 1000));
        List<ISOMsg> sent = host.sent();
        assertEquals(1, sent.size(), "the client did send it; the host just said nothing");
        assertEquals("008861", sent.get(0).getString(11));
    }

    @Test
    void theClientKeepsItsRequestUnchanged() throws Exception {
        ScriptedMux host = new ScriptedMux().then("00");
        ISOMsg request = message("0200", "008870");
        ISOMsg response = host.request(request, 1000);
        assertNotSame(request, response, "the response is a message of its own");
        assertEquals("0200", request.getMTI());
        assertFalse(request.hasField(39), "the request never gets a response code");
    }

    @Test
    void theResponseMtiFollowsTheRequest() throws Exception {
        ScriptedMux host = new ScriptedMux();
        assertEquals("0110", host.request(message("0100", "008881"), 1000).getMTI());
        assertEquals("0430", host.request(message("0420", "008882"), 1000).getMTI());
    }

    @Test
    void theListenerHearsExpiredWhenNobodyAnswers() throws Exception {
        ScriptedMux host = new ScriptedMux().then(ScriptedMux.SILENT);
        Heard heard = new Heard();
        host.request(message("0200", "008890"), 1000, heard, "till-9");
        assertEquals(List.of("expired till-9"), heard.events);
    }

    @Test
    void theResponseCarriesTheRequestsFields() throws Exception {
        ScriptedMux host = new ScriptedMux().then("00");
        ISOMsg request = message("0200", "008895");
        ISOMsg response = host.request(request, 1000);
        for (int f : new int[] { 2, 3, 4, 11, 41 })
            assertEquals(request.getString(f), response.getString(f), "field " + f);
        assertTrue(response.hasField(39));
    }
}
