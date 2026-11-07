package com.example.practice;

import org.jdom2.Element;

/** Builds the deploy descriptors of one link to a host: its ChannelAdaptor, and the QMUX on top. */
public class LinkDescriptors {

    /** The ChannelAdaptor descriptor for a link, as its 10_ file in deploy/ would hold it. */
    public static Element channelAdaptor(String name, String sendQueue, String receiveQueue,
                                         String host, int port) {
        Element channel = new Element("channel")
                .setAttribute("class", "org.jpos.iso.channel.ASCIIChannel")
                .setAttribute("packager", "org.jpos.iso.packager.GenericPackager")
                .setAttribute("logger", "Q2");
        channel.addContent(property("host", host));
        channel.addContent(property("port", Integer.toString(port)));
        channel.addContent(property("packager-config", "jar:packager/iso87ascii.xml"));
        return new Element("channel-adaptor")
                .setAttribute("name", name)
                .setAttribute("class", "org.jpos.q2.iso.ChannelAdaptor")
                .setAttribute("logger", "Q2")
                .addContent(channel)
                .addContent(new Element("in").setText(sendQueue))
                .addContent(new Element("out").setText(receiveQueue))
                .addContent(new Element("reconnect-delay").setText("10000"));
    }

    /**
     * The QMUX descriptor, as its 20_ file in deploy/ would hold it, for the multiplexer named
     * {@code muxName} that sends requests over {@code adaptor} and matches the answers.
     */
    public static Element muxFor(Element adaptor, String muxName) {
        // TODO: Q2 will deploy what you return as the multiplexer of this link. Requests sent
        //       through it must reach the host over {@code adaptor}, and their answers must come
        //       back to the thread that waits. isConnected() must tell whether this link, and no
        //       other, is up. A message from this link that nobody waits for must stay where an
        //       operator can tell which link it came from. Read what you need from {@code adaptor}.
        throw new IllegalStateException("TODO: muxFor");
    }

    private static Element property(String name, String value) {
        return new Element("property").setAttribute("name", name).setAttribute("value", value);
    }
}
