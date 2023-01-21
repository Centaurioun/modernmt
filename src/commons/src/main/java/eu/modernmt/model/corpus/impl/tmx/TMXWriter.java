package eu.modernmt.model.corpus.impl.tmx;

import eu.modernmt.io.FileProxy;
import eu.modernmt.lang.Language;
import eu.modernmt.model.corpus.TUWriter;
import eu.modernmt.model.corpus.TranslationUnit;
import eu.modernmt.xml.XMLUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.io.IOUtils;

/** Created by davide on 01/12/16. */
class TMXWriter implements TUWriter {

  private final SimpleDateFormat dateFormat = new SimpleDateFormat(TMXCorpus.TMX_DATE_FORMAT);

  private boolean headerWritten = false;

  private final OutputStream stream;
  private final XMLStreamWriter writer;

  public TMXWriter(FileProxy tmx) throws IOException {
    this.dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    OutputStream stream = null;
    XMLStreamWriter writer = null;

    try {
      stream = tmx.getOutputStream(false);
      writer = XMLUtils.createStreamWriter(stream);
    } catch (XMLStreamException e) {
      throw new IOException("Error while creating XMLStreamWriter for TMX " + tmx, e);
    } finally {
      if (writer == null) IOUtils.closeQuietly(stream);
    }

    this.stream = stream;
    this.writer = writer;
  }

  @Override
  public void write(TranslationUnit tu) throws IOException {
    try {
      if (!headerWritten) {
        headerWritten = true;
        writeHeader(tu.language.source);
      }

      writer.writeStartElement("tu");
      writer.writeAttribute("srclang", tu.language.source.toLanguageTag());
      writer.writeAttribute("datatype", "plaintext");

      if (tu.timestamp != null)
        writer.writeAttribute("creationdate", dateFormat.format(tu.timestamp));

      writeTuv(tu.language.source, tu.source);
      writeTuv(tu.language.target, tu.target);

      writer.writeEndElement();
    } catch (XMLStreamException e) {
      throw new IOException("Error while writing XMLStreamWriter", e);
    }
  }

  private void writeTuv(Language lang, String segment) throws XMLStreamException {
    writer.writeStartElement("tuv");
    writer.writeAttribute("xml", TMXCorpus.XML_NAMESPACE, "lang", lang.toLanguageTag());
    writer.writeStartElement("seg");
    writer.writeCharacters(segment);
    writer.writeEndElement();
    writer.writeEndElement();
  }

  @Override
  public void flush() throws IOException {
    try {
      writer.flush();
    } catch (XMLStreamException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      this.closeDocument();
      writer.close();
    } catch (XMLStreamException e) {
      throw new IOException("Error while closing XMLStreamWriter", e);
    } finally {
      IOUtils.closeQuietly(stream);
    }
  }

  private void writeHeader(Language sourceLanguage) throws XMLStreamException {
    writer.writeStartDocument("UTF-8", "1.0");
    writer.writeStartElement("tmx");
    writer.writeAttribute("version", "1.4");

    writer.writeEmptyElement("header");
    writer.writeAttribute("creationtool", "ModernMT - modernmt.eu");
    writer.writeAttribute("creationtoolversion", "1.0");
    writer.writeAttribute("datatype", "plaintext");
    writer.writeAttribute("o-tmf", "ModernMT");
    writer.writeAttribute("segtype", "sentence");
    writer.writeAttribute("adminlang", "en");
    if (sourceLanguage != null) writer.writeAttribute("srclang", sourceLanguage.toLanguageTag());

    writer.writeStartElement("body");
  }

  private void closeDocument() throws XMLStreamException {
    if (!headerWritten) writeHeader(null);

    writer.writeEndElement();
    writer.writeEndElement();
    writer.writeEndDocument();
    writer.flush();
  }
}
