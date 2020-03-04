package fr.noop.subtitle.sttxml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.noop.subtitle.model.SubtitleLine;
import fr.noop.subtitle.model.SubtitleParser;
import fr.noop.subtitle.model.SubtitleParsingException;
import fr.noop.subtitle.model.SubtitleText;
import fr.noop.subtitle.util.*;

public class SttXmlParser implements SubtitleParser {

    private enum TagStatus {
        NONE, OPEN, CLOSE
    }

    private String charset; // Charset of the input files

    public SttXmlParser(String charset) {
        this.charset = charset;
    }

    @Override
    public SttXmlObject parse(InputStream is) throws IOException, SubtitleParsingException {
        return parse(is, true);
    }

    @Override
    public SttXmlObject parse(InputStream is, boolean strict) throws IOException, SubtitleParsingException {
        // parse xml from is
        // get 1 cue

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser;
        SttXmlSaxParser test = new SttXmlSaxParser();
        try {
            saxParser = factory.newSAXParser();
            saxParser.parse(is, test);
        } catch (ParserConfigurationException | SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
        return test.getSttXmlObject();
        
        
 

        
            
        // End of cue
        

        
    }

    

    private SubtitleTimeCode parseTimeCode(String timeCodeString) throws SubtitleParsingException {
        try {
            int hour = Integer.parseInt(timeCodeString.substring(0, 2));
            int minute = Integer.parseInt(timeCodeString.substring(3, 5));
            int second = Integer.parseInt(timeCodeString.substring(6, 8));
            int millisecond = Integer.parseInt(timeCodeString.substring(9, 12));
            return new SubtitleTimeCode(hour, minute, second, millisecond);
        } catch (NumberFormatException e) {
            throw new SubtitleParsingException(String.format(
                    "Unable to parse time code: %s", timeCodeString));
        }
    }
}
