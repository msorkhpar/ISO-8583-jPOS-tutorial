package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class InboxTest {

    /** Values are built at run time, so a comparison with == instead of equals fails. */
    private static ISOMsg msg(String mti, String stan, String terminal) throws Exception {
        ISOMsg m = new ISOMsg(new String(mti));
        m.set(11, new String(stan));
        if (terminal != null)
            m.set(41, new String(terminal));
        return m;
    }

    private static ISOMsg answer(String mti, String stan, String terminal, String rc) throws Exception {
        ISOMsg m = msg(mti, stan, terminal);
        m.set(39, rc);
        return m;
    }

    @Test
    void anAnswerReachesTheMessageItAnswers() throws Exception {
        Inbox inbox = new Inbox();
        ISOMsg auth = msg("0100", "004417", "29110005");
        ISOMsg purchase = msg("0200", "004418", "29110006");
        ISOMsg echo = msg("0800", "004419", null);
        inbox.expect(auth);
        inbox.expect(purchase);
        inbox.expect(echo);

        assertSame(purchase, inbox.deliver(answer("0210", "004418", "29110006", "51")));
        assertSame(auth, inbox.deliver(answer("0110", "004417", "29110005", "00")));
        assertSame(echo, inbox.deliver(answer("0810", "004419", null, "00")));
        assertEquals(0, inbox.waiting());
        assertEquals("0200", purchase.getMTI(), "the sent message is unchanged");
        assertEquals("004418", purchase.getString(11));
    }

    @Test
    void aMessageNobodyWaitsForReachesNobody() throws Exception {
        Inbox inbox = new Inbox();
        inbox.expect(msg("0200", "000951", "29110007"));
        assertNull(inbox.deliver(answer("0210", "000952", "29110007", "00")), "another STAN");
        assertNull(inbox.deliver(msg("0800", "310077", null)), "the host's own echo");
        assertEquals(1, inbox.waiting());
    }

    @Test
    void anAnswerOfAnotherClassIsNotTheAnswer() throws Exception {
        Inbox inbox = new Inbox();
        inbox.expect(msg("0100", "002001", "29110008"));
        assertNull(inbox.deliver(answer("0210", "002001", "29110008", "00")));
        assertEquals(1, inbox.waiting());
    }

    @Test
    void anAdviceIsAnsweredByAnAdviceResponse() throws Exception {
        Inbox inbox = new Inbox();
        ISOMsg advice = msg("0220", "000318", "29110009");
        inbox.expect(advice);
        assertNull(inbox.deliver(answer("0210", "000318", "29110009", "00")));
        assertSame(advice, inbox.deliver(answer("0230", "000318", "29110009", "00")));
    }

    @Test
    void aRepeatIsAnsweredLikeTheOriginal() throws Exception {
        Inbox inbox = new Inbox();
        ISOMsg repeat = msg("0421", "000862", "29110010");
        inbox.expect(repeat);
        assertSame(repeat, inbox.deliver(answer("0430", "000862", "29110010", "00")));
    }

    @Test
    void anotherTerminalsAnswerIsNotYours() throws Exception {
        Inbox inbox = new Inbox();
        ISOMsg first = msg("0200", "000077", "29110011");
        ISOMsg second = msg("0200", "000077", "29110012");
        inbox.expect(first);
        inbox.expect(second);
        assertSame(second, inbox.deliver(answer("0210", "000077", "29110012", "05")));
        assertSame(first, inbox.deliver(answer("0210", "000077", "29110011", "00")));
    }
}
