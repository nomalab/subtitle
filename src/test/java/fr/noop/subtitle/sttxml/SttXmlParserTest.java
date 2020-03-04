package fr.noop.subtitle.sttxml;

import fr.noop.subtitle.model.SubtitleParsingException;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;

public class SttXmlParserTest {

    private SttXmlParser sttXmlParser = new SttXmlParser("utf-8");

    @Test
    public void test() throws IOException, SubtitleParsingException {
        FileInputStream is = new FileInputStream("src/test/resources/sttxml/test.sttxml");
        SttXmlObject sttXmlObject = sttXmlParser.parse(is);

        Assert.assertEquals(152, sttXmlObject.getCues().size());
    }
}
