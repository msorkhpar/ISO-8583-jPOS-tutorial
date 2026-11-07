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
        String name = adaptor.getAttributeValue("name");
        return new Element("mux")
                .setAttribute("name", muxName)
                .setAttribute("class", "org.jpos.q2.iso.QMUX")
                .setAttribute("logger", "Q2")
                .addContent(new Element("in").setText(adaptor.getChildText("out")))
                .addContent(new Element("out").setText(adaptor.getChildText("in")))
                .addContent(new Element("ready").setText(name + ".ready"))
                .addContent(new Element("unhandled").setText(name + "-unhandled"));
    }

    private static Element property(String name, String value) {
        return new Element("property").setAttribute("name", name).setAttribute("value", value);
    }
}
