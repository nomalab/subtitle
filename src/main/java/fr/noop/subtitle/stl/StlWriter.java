package fr.noop.subtitle.stl;

import fr.noop.subtitle.model.SubtitleCue;
import fr.noop.subtitle.model.SubtitleObject;
import fr.noop.subtitle.model.SubtitleRegionCue;
import fr.noop.subtitle.model.SubtitleWriter;
import fr.noop.subtitle.util.SubtitleTimeCode;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

public class StlWriter implements SubtitleWriter {
    public StlWriter() {
    }

    @Override
    public void write(SubtitleObject subtitleObject, OutputStream os) throws IOException {
        StlGsi gsi = this.writeGsi(subtitleObject, os);
        StlObject stlObject = new StlObject(gsi);
        try {
            int subtitleIndex = 0;
            for (SubtitleCue cue : subtitleObject.getCues()) {
                StlTti tti = this.writeTti(cue, gsi, subtitleIndex);
                stlObject.addTti(tti);
                subtitleIndex++;
            }
            this.writeGsiToFile(stlObject, os);
            this.writeTtiToFile(stlObject, os);
        } catch (UnsupportedEncodingException e) {
            throw new IOException("Encoding error in input subtitle");
        }
    }

    private void writeGsiToFile(StlObject stlObject, OutputStream os) throws IOException {
        DateFormat df = new SimpleDateFormat("yyMMdd");
        StlGsi gsi = stlObject.getGsi();
        float frameRate = gsi.getDfc().getFrameRate();
        float frameDuration = (1000 / frameRate);
        // 0..2 3 Code Page Number
        os.write((gsi.getCpn().getValue() >> 16));
        os.write((gsi.getCpn().getValue() >> 8));
        os.write((gsi.getCpn().getValue() >> 0));
        // 3..10 8 Disk Format Code DFC
        os.write(gsi.getDfc().getValue().getBytes("utf-8"), 0, 8);
        // 11 1 Display Standard Code DSC
        os.write(gsi.getDsc().getValue());
        // 12..13 2 Character Code Table number CCT
        os.write(gsi.getCct().getValue() >> 8);
        os.write(gsi.getCct().getValue() >> 0);
        // 14..15 2 Language Code LC
        os.write(gsi.getLc().getValue() >> 8);
        os.write(gsi.getLc().getValue() >> 0);
        // 16..47 32 Original Programme Title OPT
        os.write(StringUtils.rightPad(gsi.getOpt(), 32).getBytes("utf-8"), 0, 32);
        // 48..79 32 Original Episode Title OET
        os.write(StringUtils.rightPad(gsi.getOet(), 32).getBytes("utf-8"), 0, 32);
        // 80..111 32 Translated Programme Title TPT
        os.write(StringUtils.rightPad(gsi.getTpt(), 32).getBytes("utf-8"), 0, 32);
        // 112..143 32 Translated Episode Title TET
        os.write(StringUtils.rightPad(gsi.getTet(), 32).getBytes("utf-8"), 0, 32);
        // 144..175 32 Translator's Name TN
        os.write(StringUtils.rightPad(gsi.getTn(), 32).getBytes("utf-8"), 0, 32);
        // 176..207 32 Translator's Contact Details TCD
        os.write(StringUtils.rightPad(gsi.getTcd(), 32).getBytes("utf-8"), 0, 32);
        // 208..223 16 Subtitle List Reference Code SLR
        os.write(StringUtils.rightPad(gsi.getTcd(), 16).getBytes("utf-8"), 0, 16);
        // 224..229 6 Creation Date CD
        os.write(df.format(gsi.getCd()).getBytes("utf-8"), 0, 6);
        // 230..235 6 Revision Date RD
        os.write(df.format(gsi.getRd()).getBytes("utf-8"), 0, 6);
        // 236..237 2 Revision number RN
        os.write(String.format("%02d", gsi.getRn()).getBytes("utf-8"), 0, 2);
        // 238..242 5 Total Number of Text and Timing Information (TTI) blocks TNB
        os.write(String.format("%05d", gsi.getTnb()).getBytes("utf-8"), 0, 5);
        // 243..247 5 Total Number of Subtitles TNS
        os.write(String.format("%05d", gsi.getTns()).getBytes("utf-8"), 0, 5);
        // 248..250 3 Total Number of Subtitle Groups TNG
        os.write(String.format("%03d", gsi.getTng()).getBytes("utf-8"), 0, 3);
        // 251..252 2 Maximum Number of Displayable Characters in any text row MNC
        os.write(String.format("%02d", gsi.getMnc()).getBytes("utf-8"), 0, 2);
        // 253..254 2 Maximum Number of Displayable Rows MNR
        os.write(String.format("%02d", gsi.getMnr()).getBytes("utf-8"), 0, 2);
        // 255 1 Time Code: Status TCS
        os.write(gsi.getTcs().getValue());
        // 256..263 8 Time Code: Start-of-Programme TCP
        os.write(String.format("%02d", gsi.getTcp().getHour()).getBytes("utf-8"), 0, 2);
        os.write(String.format("%02d", gsi.getTcp().getMinute()).getBytes("utf-8"), 0, 2);
        os.write(String.format("%02d", gsi.getTcp().getSecond()).getBytes("utf-8"), 0, 2);
        os.write(String.format("%02d", Math.round(gsi.getTcp().getMillisecond() / frameDuration)).getBytes("utf-8"), 0, 2);
        // 264..271 8 Time Code: First In-Cue TCF
        os.write(String.format("%02d", gsi.getTcf().getHour()).getBytes("utf-8"), 0, 2);
        os.write(String.format("%02d", gsi.getTcf().getMinute()).getBytes("utf-8"), 0, 2);
        os.write(String.format("%02d", gsi.getTcf().getSecond()).getBytes("utf-8"), 0, 2);
        os.write(String.format("%02d", Math.round(gsi.getTcf().getMillisecond() / frameDuration)).getBytes("utf-8"), 0, 2);
        // 272 1 Total Number of Disks TND
        os.write(String.format("%d", gsi.getTnd()).getBytes("utf-8"));
        // 273 1 Disk Sequence Number DSN
        os.write(String.format("%d", gsi.getDsn()).getBytes("utf-8"));
        // 274..276 3 Country of Origin
        os.write(gsi.getCo().getBytes("utf-8"), 0, 3);
        // 277..308 32 Publisher PUB
        os.write(StringUtils.rightPad(gsi.getPub(), 32).getBytes("utf-8"), 0, 32);
        // 309..340 32 Editor's Name EN
        os.write(StringUtils.rightPad(gsi.getEn(), 32).getBytes("utf-8"), 0, 32);
        // 341..372 32 Editor's Contact Details ECD
        os.write(StringUtils.rightPad(gsi.getEcd(), 32).getBytes("utf-8"), 0, 32);
        // 373..447 75 Spare Bytes
        os.write(new byte[75]);
        // 448..1023 576 User-Defined Area
        os.write(StringUtils.rightPad(gsi.getUda(), 576).getBytes("utf-8"), 0, 576);
    }

    private StlGsi writeGsi(SubtitleObject subtitleObject, OutputStream os) throws IOException {
        // Write GSI block
        // GSI block is 1024 bytes long
        StlGsi gsi = new StlGsi();

        // CodePageNumber
        gsi.setCpn(StlGsi.Cpn.getEnum(0x383530));

        // DiskFormatCode
        if (subtitleObject.hasProperty(SubtitleObject.Property.FRAME_RATE)) {
            gsi.setDfc(StlGsi.Dfc.getEnumFromFloat((float) subtitleObject.getProperty(SubtitleObject.Property.FRAME_RATE)));
        } else {
            gsi.setDfc(StlGsi.Dfc.getEnum("STL25.01"));
        }

        // DisplayStandardCode
        gsi.setDsc(StlGsi.Dsc.getEnum(0x31));

        // CharacterCodeTableNumber
        gsi.setCct(StlGsi.Cct.getEnum(0x3030));

        // LanguageCode
        gsi.setLc(LanguageCode.Lc.getEnum(0x3046));

        // OriginalProgrammeTitle
        if (subtitleObject.hasProperty(SubtitleObject.Property.TITLE)) {
            gsi.setOpt((String) subtitleObject.getProperty(SubtitleObject.Property.TITLE));
        } else {
            gsi.setOpt("");
        }

        // OriginalEpisodeTitle
        gsi.setOet("");

        // TranslatedProgrammeTitle
        gsi.setTpt("");

        // TranslatedEpisodeTitle
        gsi.setTet("");

        // TranslatorsName
        gsi.setTn("");

        // TranslatorsContactDetails
        gsi.setTcd("");

        // SubtitleListReferenceCode
        gsi.setSlr("");

        // CreationDate
        gsi.setCd(new Date());

        // RevisionDate
        gsi.setRd(new Date());

        // RevisionNumber
        gsi.setRn(0);

        // TotalNumberOfTextAndTimingInformationBlocks
        gsi.setTnb(subtitleObject.getCues().size());

        // TotalNumberOfSubtitles
        gsi.setTns(subtitleObject.getCues().size());

        // TotalNumberOfSubtitleGroups
        gsi.setTng(1);

        // MaximumNumb erOfDisplayableCharactersInAnyTextRow
        gsi.setMnc(40);

        // MaximumNumberOfDisplayableRows
        gsi.setMnr(23);

        // TimeCodeStatus
        gsi.setTcs(StlGsi.Tcs.getEnum(0x31));

        // TimeCodeStartOfProgramme
        if (subtitleObject.hasProperty(SubtitleObject.Property.START_TIMECODE_PRE_ROLL)) {
            gsi.setTcp((SubtitleTimeCode) subtitleObject.getProperty(SubtitleObject.Property.START_TIMECODE_PRE_ROLL));
        } else {
            gsi.setTcp(new SubtitleTimeCode(0));
        }

        // TimeCodeFirstInCue
        gsi.setTcf(subtitleObject.getCues().get(0).getStartTime());

        // TotalNumberOfDisks
        gsi.setTnd((short) 1);

        // DiskSequenceNumber
        gsi.setDsn((short) 1);

        // CountryOfOrigin
        gsi.setCo("FRA");

        // Publisher
        gsi.setPub("");

        // EditorsName
        gsi.setEn("");

        // EditorsContactDetails
        gsi.setEcd("");

        // UserDefinedArea
        gsi.setUda("");

        return gsi;
    }

    private void writeTtiToFile(StlObject stlObject, OutputStream os) throws IOException {
        StlGsi gsi = stlObject.getGsi();
        float frameRate = gsi.getDfc().getFrameRate();
        float frameDuration = (1000 / frameRate);
        for (StlTti tti : stlObject.getTtis()) {
            // 0 1 Subtitle Group Number SGN
            os.write(tti.getSgn());
            // 1..2 2 Subtitle Number SN
            os.write(tti.getSn() >> 8);
            os.write(tti.getSn() >> 0);
            // 3 1 Extension Block Number EBN
            os.write(tti.getEbn());
            // 4 1 Cumulative Status CS
            os.write(tti.getCs());
            // 5..8 4 Time Code In TCI
            os.write(tti.getTci().getHour());
            os.write(tti.getTci().getMinute());
            os.write(tti.getTci().getSecond());
            os.write(Math.round(tti.getTci().getMillisecond() / frameDuration));
            // 9..12 4 Time Code Out TCO
            os.write(tti.getTco().getHour());
            os.write(tti.getTco().getMinute());
            os.write(tti.getTco().getSecond());
            os.write(Math.round(tti.getTco().getMillisecond() / frameDuration));
            // 13 1 Vertical Position VP
            os.write(tti.getVp());
            // 14 1 Justification Code JC
            os.write(tti.getJc().getValue());
            // 15 1 Comment Flag CF
            os.write(tti.getCf());
            // 16..127 112 Text Field TF
            byte[] tfBytes = new byte[112];
            Arrays.fill(tfBytes, (byte)0x8F);
            byte[] text = tti.getTf().getBytes(gsi.getCct().getCharset());
            System.arraycopy(text, 0, tfBytes, 0, text.length);
            os.write(tfBytes, 0, 112);
        }
    }

    private StlTti writeTti(SubtitleCue cue, StlGsi gsi, int subtitleNumber) throws IOException {
        // Write TTI block
        // Each TTI block is 128 bytes long
        StlTti tti = new StlTti();

        // SubtitleGroupNumber
        tti.setSgn((short) 0x00);

        // SubtitleNumber
        tti.setSn(subtitleNumber);

        // ExtensionBlockNumber
        tti.setEbn((short) 0xFF);

        // CumulativeStatus
        tti.setCs((short) 0x00);

        // TimeCodeIn
        tti.setTci(cue.getStartTime());

        // TimeCodeOut
        tti.setTco(cue.getEndTime());

        // VerticalPosition
        int verticalPos = ((SubtitleRegionCue) cue).getRegion().getVerticalPosition();
        tti.setVp((short) verticalPos);

        // JustificationCode
        tti.setJc(StlTti.Jc.getEnum(0x02));

        // CommentFlag
        tti.setCf((short) 0x00);

        // TextField
        tti.setTf(cue.getCharacterCodes());

        return tti;
    }
}