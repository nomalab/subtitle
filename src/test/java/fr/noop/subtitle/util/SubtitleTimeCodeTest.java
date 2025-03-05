/*
 *  This file is part of the noOp organization .
 *
 *  (c) Cyrille Lebeaupin <clebeaupin@noop.fr>
 *
 *  For the full copyright and license information, please view the LICENSE
 *  file that was distributed with this source code.
 *
 */

package fr.noop.subtitle.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.security.InvalidParameterException;

/**
 * Created by clebeaupin on 07/10/15.
 */
public class SubtitleTimeCodeTest  {
    private SubtitleTimeCode tested = new SubtitleTimeCode(1, 23, 12, 10);

    @Test
    public void testToString() throws Exception {
        assertEquals("01:23:12.010", tested.toString());
    }

    @Test
    public void testSingleHourTimeToString() throws Exception {
        assertEquals("1:23:12.01", tested.singleHourTimeToString());
    }

    @Test
    public void testFormatWithFramerate() throws Exception {
        float frameRate = 25;
        tested.setMillisecond(80);
        assertEquals("01:23:12:02", tested.formatWithFramerate(frameRate));
    }

    @Test
    public void testSingleHourTimeToStringException() throws Exception {
        tested.setHour(10);
        Exception exception = assertThrows(InvalidParameterException.class, () -> tested.singleHourTimeToString());
        assertEquals("Hour value must be a single digit number", exception.getMessage());
    }

    @Test
    public void testFromString() throws Exception {
        String outputTimecode = "10:55:30:24";
        float frameRate = 25;
        SubtitleTimeCode parsed = SubtitleTimeCode.fromStringWithFrames(outputTimecode, frameRate);
        SubtitleTimeCode expected = new SubtitleTimeCode(10, 55, 30, 960);
        assertEquals(expected.getTime(), parsed.getTime());
    }

    @Test
    public void testFromStringException() throws Exception {
        String outputTimecode = "00:00:00:25";
        float frameRate = 25;
        Exception exception = assertThrows(InvalidParameterException.class, () -> SubtitleTimeCode.fromStringWithFrames(outputTimecode, frameRate));
        assertEquals("A Millisecond value must be between 0 and 999", exception.getMessage());
    }

    @Test
    public void testFromStringException2() throws Exception {
        String outputTimecode = "hh:mm:ss:ff";
        float frameRate = 25;
        Exception exception = assertThrows(NumberFormatException.class, () -> SubtitleTimeCode.fromStringWithFrames(outputTimecode, frameRate));
        assertEquals("For input string: \"hh\"", exception.getMessage());
    }

    @Test
    public void testGetHour() throws Exception {
        assertEquals(1, tested.getHour());
    }

    @Test
    public void testSetHour() throws Exception {
        tested.setHour(2);
        assertEquals(2, tested.getHour());
    }

    @Test
    public void testSetHourException() throws Exception {
        Exception exception = assertThrows(InvalidParameterException.class, () -> tested.setHour(-1));
        assertEquals("Hour value must be between 0 and 23", exception.getMessage());
    }

    @Test
    public void testSetHourException2() throws Exception {
        Exception exception = assertThrows(InvalidParameterException.class, () -> tested.setHour(24));
        assertEquals("Hour value must be between 0 and 23", exception.getMessage());
    }

    @Test
    public void testGetMinute() throws Exception {
        assertEquals(23, tested.getMinute());
    }

    @Test
    public void testSetMinute() throws Exception {
        tested.setMinute(50);
        assertEquals(50, tested.getMinute());
    }

    @Test
    public void testSetMinuteException1() throws Exception {
        Exception exception = assertThrows(InvalidParameterException.class, () -> tested.setMinute(-1));
        assertEquals("Minute value must be between 0 and 59", exception.getMessage());
    }

    @Test
    public void testSetMinuteException2() throws Exception {
        Exception exception = assertThrows(InvalidParameterException.class, () -> tested.setMinute(60));
        assertEquals("Minute value must be between 0 and 59", exception.getMessage());
    }

    @Test
    public void testGetSecond() throws Exception {
        assertEquals(12, tested.getSecond());
    }

    @Test
    public void testSetSecond() throws Exception {
        tested.setSecond(50);
        assertEquals(50, tested.getSecond());
    }

    @Test
    public void testSetSecondException1() throws Exception {
        Exception exception = assertThrows(InvalidParameterException.class, () -> tested.setSecond(-1));
        assertEquals("A second value must be between 0 and 59", exception.getMessage());
    }

    @Test
    public void testSetSecondException2() throws Exception {
        Exception exception = assertThrows(InvalidParameterException.class, () -> tested.setSecond(60));
        assertEquals("A second value must be between 0 and 59", exception.getMessage());
    }

    @Test
    public void testGetMillisecond() throws Exception {
        assertEquals(10, tested.getMillisecond());
    }

    @Test
    public void testSetMillisecond() throws Exception {
        tested.setMillisecond(50);
        assertEquals(50, tested.getMillisecond());
    }

    @Test
    public void testSetMillisecondException1() throws Exception {
        Exception exception = assertThrows(InvalidParameterException.class, () -> tested.setMillisecond(-1));
        assertEquals("A Millisecond value must be between 0 and 999", exception.getMessage());
    }

    @Test
    public void testSetMillisecondException2() throws Exception {
        Exception exception = assertThrows(InvalidParameterException.class, () -> tested.setMillisecond(1000));
        assertEquals("A Millisecond value must be between 0 and 999", exception.getMessage());
    }

    @Test
    public void testGetTime() throws Exception {
        assertEquals(4992010, tested.getTime());
    }

    @Test
    public void testCompareTo() throws Exception {
        assertEquals(0, tested.compareTo(new SubtitleTimeCode(1, 23, 12, 10)));
        assertEquals(1, tested.compareTo(new SubtitleTimeCode(1, 22, 12, 10)));
        assertEquals(-1, tested.compareTo(new SubtitleTimeCode(1, 24, 12, 10)));
    }

    @Test
    public void testSubtract() throws Exception {
        // Subtract 1 hour, 10 minutes, 3 seconds and 3 milliseconds
        SubtitleTimeCode toSubtract = new SubtitleTimeCode(1, 10, 3, 3);
        SubtitleTimeCode expected = new SubtitleTimeCode(0, 13, 9, 7);
        assertEquals(expected.getTime(), tested.subtract(toSubtract).getTime());
    }

    @Test
    public void testConvert() throws Exception {
        // Convert from 0 hour, 0 minute, 0 second, 0 milliseconds to 9 hours, 59 minutes, 20 seconds and 80 milliseconds
        SubtitleTimeCode startTC = new SubtitleTimeCode(0);
        SubtitleTimeCode newStartTC = new SubtitleTimeCode(9, 59, 20, 80);
        SubtitleTimeCode expected = new SubtitleTimeCode(11, 22, 32, 90);
        assertEquals(expected.getTime(), tested.convertFromStart(newStartTC, startTC).getTime());

        // Convert from 1 hour, 0 minute, 30 seconds, 10 milliseconds to 0 hour, 0 minute, 0 second, 0 millisecond
        SubtitleTimeCode startTC2 = new SubtitleTimeCode(1, 0, 30, 10);
        SubtitleTimeCode newStartTC2 = new SubtitleTimeCode(0);
        SubtitleTimeCode expected2 = new SubtitleTimeCode(0, 22, 42, 0);
        assertEquals(expected2.getTime(), tested.convertFromStart(newStartTC2, startTC2).getTime());

        // Convert from 25 to 24 fps
        SubtitleTimeCode tested2 = new SubtitleTimeCode(0, 4, 43, 360);
        SubtitleTimeCode expected3 = new SubtitleTimeCode(0, 4, 55, 166);
        assertEquals(expected3.getTime(), tested2.convertWithFrameRate(25, 24, new SubtitleTimeCode(0)).getTime());

        // Convert from 25 to 30 fps
        SubtitleTimeCode expected4 = new SubtitleTimeCode(0, 4, 43, 360);
        assertEquals(expected4.getTime(), tested2.convertWithFrameRate(25, 30, new SubtitleTimeCode(0)).getTime());
    }

    @Test
    public void testAddOffset() throws Exception {
        // Add offset of 1 minute, 3 seconds, 200 milliseconds
        SubtitleTimeCode offset = new SubtitleTimeCode(0, 1, 3, 200);
        SubtitleTimeCode expected = new SubtitleTimeCode(1, 24, 15, 210);
        assertEquals(expected.getTime(), tested.addOffset(offset).getTime());
    }
}
