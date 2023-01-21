package eu.modernmt.xml;

import eu.modernmt.io.UTF8Charset;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

/** Created by davide on 04/07/16. */
public class XMLUtils {

  public static String escapeText(String string) {
    StringBuilder builder = null;

    for (int i = 0; i < string.length(); i++) {
      char c = string.charAt(i);

      if (builder == null && (c == '&' || c == '<' || c == '>')) {
        builder = new StringBuilder((int) (string.length() * 1.4));
        builder.append(string, 0, i);
      }

      switch (c) {
        case '&':
          builder.append("&amp;");
          break;
        case '<':
          builder.append("&lt;");
          break;
        case '>':
          builder.append("&gt;");
          break;
        default:
          if (builder != null) builder.append(c);
          break;
      }
    }

    return builder == null ? string : builder.toString();
  }

  public static XMLEventReader createEventReader(InputStream stream) throws XMLStreamException {
    Charset charset = UTF8Charset.get();

    BOMInputStream bomStream =
        new BOMInputStream(
            stream, false, ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE);
    try {
      if (bomStream.hasBOM()) charset = Charset.forName(bomStream.getBOMCharsetName());
    } catch (IOException e) {
      throw new XMLStreamException(e);
    }

    XMLInputFactory factory = XMLInputFactory.newInstance();
    return factory.createXMLEventReader(new XMLFixInputStreamReader(bomStream, charset));
  }

  public static void closeQuietly(XMLEventReader reader) {
    if (reader != null)
      try {
        reader.close();
      } catch (XMLStreamException e) {
        // Ignore
      }
  }

  public static XMLStreamWriter createStreamWriter(OutputStream stream) throws XMLStreamException {
    Charset charset = UTF8Charset.get();

    XMLOutputFactory factory = XMLOutputFactory.newInstance();
    return new IndentingXMLStreamWriter(
        factory.createXMLStreamWriter(new OutputStreamWriter(stream, charset)));
  }

  public static String getLocalName(StartElement element) {
    return element.getName().getLocalPart();
  }

  public static String getLocalName(EndElement element) {
    return element.getName().getLocalPart();
  }

  public static String getAttributeValue(
      StartElement element, String namespaceURI, String localPart) {
    QName name =
        new QName(namespaceURI == null ? XMLConstants.NULL_NS_URI : namespaceURI, localPart);
    Attribute attribute = element.getAttributeByName(name);
    return attribute == null ? null : attribute.getValue();
  }

  public static String getXMLContent(
      XMLEventReader reader, StartElement element, boolean includeTags) throws XMLStreamException {
    String rootElementName = getLocalName(element);

    StringWriter buffer = new StringWriter(1024);

    StartElement pendingElement = null;
    String pendingElementName = null;

    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();

      if (pendingElement != null) {
        boolean skip = false;

        if (event.isEndElement() && pendingElementName.equals(getLocalName(event.asEndElement()))) {
          if (includeTags) writeAsEncodedUnicode(pendingElement, buffer, true); // empty tag
          skip = true; // skip this end tag
        } else {
          if (includeTags) writeAsEncodedUnicode(pendingElement, buffer, false);
        }

        pendingElement = null;
        pendingElementName = null;

        if (skip) continue;
      }

      if (event.isEndElement()) {
        EndElement endElement = event.asEndElement();
        String name = getLocalName(endElement);

        if (rootElementName.equals(name)) return buffer.toString();

        if (includeTags) writeAsEncodedUnicode(endElement, buffer);
      } else if (event.isStartElement()) {
        pendingElement = event.asStartElement();
        pendingElementName = getLocalName(pendingElement);
      } else if (event.isCharacters() && !includeTags) {
        buffer.append(event.asCharacters().getData());
      } else {
        if (includeTags) event.writeAsEncodedUnicode(buffer);
      }
    }

    throw new XMLStreamException(
        format("Missing closing tag for '" + rootElementName + "' element", element));
  }

  private static void writeAsEncodedUnicode(StartElement element, Writer writer, boolean isEmpty)
      throws XMLStreamException {
    try {
      // Write start tag.
      writer.write('<');
      QName name = element.getName();

      String prefix = name.getPrefix();
      if (prefix != null && prefix.length() > 0) {
        writer.write(prefix);
        writer.write(':');
      }
      writer.write(name.getLocalPart());

      // Write namespace declarations.
      Iterator nsIter = element.getNamespaces();
      while (nsIter.hasNext()) {
        Namespace ns = (Namespace) nsIter.next();
        writer.write(' ');
        ns.writeAsEncodedUnicode(writer);
      }

      // Write attributes
      Iterator attrIter = element.getAttributes();
      while (attrIter.hasNext()) {
        Attribute attr = (Attribute) attrIter.next();
        writer.write(' ');
        attr.writeAsEncodedUnicode(writer);
      }

      if (isEmpty) writer.write('/');
      writer.write('>');
    } catch (IOException ioe) {
      throw new XMLStreamException(ioe);
    }
  }

  private static void writeAsEncodedUnicode(EndElement element, Writer writer)
      throws XMLStreamException {
    try {
      // Write end tags.
      writer.write("</");
      QName name = element.getName();
      String prefix = name.getPrefix();
      if (prefix != null && prefix.length() > 0) {
        writer.write(prefix);
        writer.write(':');
      }
      writer.write(name.getLocalPart());
      writer.write('>');
    } catch (IOException ioe) {
      throw new XMLStreamException(ioe);
    }
  }

  private static String format(String message, XMLEvent event) {
    Location location = event == null ? null : event.getLocation();
    return location == null ? message : (message + " at line " + location.getLineNumber());
  }
}
