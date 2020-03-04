package fr.noop.subtitle.sttxml;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.noop.subtitle.model.SubtitleLine;
import fr.noop.subtitle.model.SubtitleObject;
import fr.noop.subtitle.model.SubtitleText;
import fr.noop.subtitle.util.SubtitlePlainText;
import fr.noop.subtitle.util.SubtitleStyle;
import fr.noop.subtitle.util.SubtitleStyledText;
import fr.noop.subtitle.util.SubtitleTextLine;
import fr.noop.subtitle.util.SubtitleTimeCode;

public class SttXmlSaxParser extends DefaultHandler {
    private SttXmlObject subtitle;
    private SttXmlCue currentCue;
    private SttXmlLine currentLine;
    private Float framerate;
    private String startTimecode;
    private String color;
    private String justification;

    public SttXmlObject getSttXmlObject() {
        return subtitle;
    }

    @Override
    public void startDocument() throws SAXException {
        subtitle = new SttXmlObject();

    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String value = new String(ch, start, length).trim();
        if (value.length() == 0)
            return;
        if (currentLine != null) {
            SubtitleStyle style = new SubtitleStyle();
            style.setColor(color);
            switch (justification) {
                case "left" : {
                    style.setTextAlign(SubtitleStyle.TextAlign.LEFT);
                    break;
                }
                case "right" : {
                    style.setTextAlign(SubtitleStyle.TextAlign.RIGHT);
                    break;
                }
                case "center" : {
                    style.setTextAlign(SubtitleStyle.TextAlign.CENTER);
                    break;
                }
                default : {
                    style.setTextAlign(SubtitleStyle.TextAlign.CENTER);
                    break;
                }
            }
            SubtitleStyledText text = new SubtitleStyledText(value, style);
            currentLine.addText(text);
        }

    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (qName) {
            case "media" : {
                //startTimecode = attributes.getValue("starttimecode");
                break;
            }
            case "timecode" : {
                framerate = Float.parseFloat(attributes.getValue("fps"));
                startTimecode = attributes.getValue("firstframe");
                break;
            }
            case "bloc": {
                currentCue = new SttXmlCue();
                String timecodeIn  = attributes.getValue("timecodein");
                String timecodeOut  = attributes.getValue("timecodeout");
                
                currentCue.setStartTime(readTimeCode(timecodeIn, framerate));
                currentCue.setEndTime(readTimeCode(timecodeOut, framerate));
                currentCue.subtractTime(readTimeCode(startTimecode, framerate));
                break;
            }
            case "row": {
                currentLine = new SttXmlLine();
                justification = attributes.getValue("justification");
                break;
            }
            case "style": {
                color = attributes.getValue("color");

                break;
            }
        }
    }
    private SubtitleTimeCode readTimeCode(String timeCodeString, float frameRate) {

        int hour = Integer.parseInt(timeCodeString.substring(0, 2));
        int minute = Integer.parseInt(timeCodeString.substring(3, 5));
        int second = Integer.parseInt(timeCodeString.substring(6, 8));
        int frame = Integer.parseInt(timeCodeString.substring(9, 10));

        // Frame duration in milliseconds
        float frameDuration = (1000 / frameRate);

        // Build time code
        return new SubtitleTimeCode(hour, minute, second, Math.round(frame * frameDuration));
    }
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case "bloc": {
                subtitle.addCue(currentCue);
                break;
            }
            case "row": {
                if (currentLine != null) {
                    currentCue.addLine(currentLine);
                }
                currentLine = null;
                break;
            }
        }
    }
}
