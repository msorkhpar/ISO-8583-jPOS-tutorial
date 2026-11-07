package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * The acquirer's loyalty programme travels in field 63. From its interface document:
 *
 * <pre>
 * Field 63  LOYALTY DATA   ans..999, three-digit ASCII length, sub-fields in order, no bitmap
 *   63.1  programme code        an 2, fixed
 *   63.2  member number         n ..19, two-digit ASCII length
 *   63.3  points to redeem      n 6, fixed, zero-filled on the left (requests)
 *   63.4  points balance        n 8, fixed, zero-filled on the left (responses)
 * </pre>
 */
public class Loyalty {
    private static final String LINK_XML =
        "<?xml version='1.0' encoding='UTF-8'?>\n"
      + "<!DOCTYPE isopackager PUBLIC \"-//jPOS/jPOS Generic Packager DTD 1.0//EN\"\n"
      + "  \"http://jpos.org/dtd/generic-packager-1.0.dtd\">\n"
      + "<isopackager>\n"
      + "  <isofield id='0' length='4' name='MTI' class='org.jpos.iso.IFA_NUMERIC'/>\n"
      + "  <isofield id='1' length='16' name='BITMAP' class='org.jpos.iso.IFA_BITMAP'/>\n"
      + "  <isofield id='2' length='19' name='PAN' class='org.jpos.iso.IFA_LLNUM'/>\n"
      + "  <isofield id='3' length='6' name='PROCESSING CODE' class='org.jpos.iso.IFA_NUMERIC'/>\n"
      + "  <isofield id='4' length='12' name='AMOUNT' class='org.jpos.iso.IFA_NUMERIC'/>\n"
      + "  <isofield id='11' length='6' name='STAN' class='org.jpos.iso.IFA_NUMERIC'/>\n"
      + "  <isofield id='39' length='2' name='RESPONSE CODE' class='org.jpos.iso.IF_CHAR'/>\n"
      + "  <isofield id='41' length='8' name='TERMINAL ID' class='org.jpos.iso.IF_CHAR'/>\n"
      + "%FIELD63%"
      + "</isopackager>\n";

    /** The packager of the acquirer's link. */
    public static GenericPackager packager() throws ISOException {
        String xml = LINK_XML.replace("%FIELD63%", field63());
        return new GenericPackager(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    /** A purchase; for a member, with the loyalty data. The redemption step runs before the member step. */
    public static ISOMsg purchase(String pan, long amountMinor, String stan,
                                  String programme, String memberNumber, int pointsToRedeem)
            throws ISOException {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, pan);
        m.set(3, "000000");
        m.set(4, ISOUtil.zeropad(Long.toString(amountMinor), 12));
        m.set(11, stan);
        m.set(41, "29110001");
        if (pointsToRedeem > 0)
            m.set("63.3", ISOUtil.zeropad(Integer.toString(pointsToRedeem), 6));
        if (memberNumber != null)
            addMember(m, programme, memberNumber);
        return m;
    }

    /** The declaration of field 63 in the link's packager XML: one element, as the document says. */
    static String field63() {
        // TODO: The XML for field 63 exactly as the interface document above describes it,
        //       so that requests pack with their sub-fields and responses unpack into them.
        throw new IllegalStateException("TODO: field63");
    }

    /** Adds the member's programme code and member number to the request. */
    static void addMember(ISOMsg request, String programme, String memberNumber) {
        // TODO: Other steps may already have put their own loyalty data into the request.
        throw new IllegalStateException("TODO: addMember");
    }

    /** The member's points balance from the host's response, or null when the response has none. */
    static String pointsBalance(ISOMsg response) {
        // TODO: Responses for customers who are not members carry no field 63 at all.
        throw new IllegalStateException("TODO: pointsBalance");
    }
}
