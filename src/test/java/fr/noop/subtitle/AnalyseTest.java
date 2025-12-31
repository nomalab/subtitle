package fr.noop.subtitle;

import fr.noop.subtitle.stl.StlParser;
import fr.noop.subtitle.model.SubtitleObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.InputStream;

public class AnalyseTest {
    @Test
    public void test() throws Exception {
        InputStream is = new FileInputStream("src/test/resources/stl/test.stl");
        StlParser stlParser = new StlParser();
        SubtitleObject stl = stlParser.parse(is);

        Analyse analyse = new Analyse();
        JSONObject report = analyse.getProperties(stl);

        Assertions.assertEquals(25, report.get("frame_rate_numerator"));
        Assertions.assertEquals("10:00:00:00", report.get("start_timecode"));
        Assertions.assertEquals("10:00:06:08", report.get("first_cue"));
        Assertions.assertEquals("10:00:45:21", report.get("last_cue"));
    }
}
