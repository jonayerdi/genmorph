package ch.usi.gassert.data.types;

import org.junit.Test;

import static ch.usi.gassert.util.CollectionUtils.list;
import static org.junit.Assert.*;

public class SequenceTest {

    @Test
    public void testTruncateArray() {
        final Sequence s = Sequence.fromValue(new int[] { 1, 2, 3, 4, 5 });
        final Sequence s0 = s.truncate(0);
        final Sequence s1 = s.truncate(1);
        final Sequence s2 = s.truncate(2);
        final Sequence s3 = s.truncate(-1);
        final Sequence s4 = s.truncate(-2);
        final Sequence s5 = s.truncate(s.length());
        final Sequence s6 = s.truncate(s.length()+1);
        final Sequence s7 = s.truncate(-s.length());
        final Sequence s8 = s.truncate(-s.length()-1);
        final Sequence s9 = s.truncate(-s.length()+1);
        assertArrayEquals(new Integer[] {}, (Integer[])s0.getValue());
        assertArrayEquals(new Integer[] { 1 }, (Integer[])s1.getValue());
        assertArrayEquals(new Integer[] { 1, 2 }, (Integer[])s2.getValue());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, (Integer[])s3.getValue());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, (Integer[])s4.getValue());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, (Integer[])s5.getValue());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, (Integer[])s6.getValue());
        assertArrayEquals(new Integer[] {}, (Integer[])s7.getValue());
        assertArrayEquals(new Integer[] {}, (Integer[])s8.getValue());
        assertArrayEquals(new Integer[] { 5 }, (Integer[])s9.getValue());
    }

    @Test
    public void testTruncateList() {
        final Sequence s = Sequence.fromValue(list(1, 2, 3, 4, 5));
        final Sequence s0 = s.truncate(0);
        final Sequence s1 = s.truncate(1);
        final Sequence s2 = s.truncate(2);
        final Sequence s3 = s.truncate(-1);
        final Sequence s4 = s.truncate(-2);
        final Sequence s5 = s.truncate(s.length());
        final Sequence s6 = s.truncate(s.length()+1);
        final Sequence s7 = s.truncate(-s.length());
        final Sequence s8 = s.truncate(-s.length()-1);
        final Sequence s9 = s.truncate(-s.length()+1);
        assertEquals(list(), s0.getValue());
        assertEquals(list(1), s1.getValue());
        assertEquals(list(1, 2), s2.getValue());
        assertEquals(list(2, 3, 4, 5), s3.getValue());
        assertEquals(list(3, 4, 5), s4.getValue());
        assertEquals(list(1, 2, 3, 4, 5 ), s5.getValue());
        assertEquals(list(1, 2, 3, 4, 5 ), s6.getValue());
        assertEquals(list(), s7.getValue());
        assertEquals(list(), s8.getValue());
        assertEquals(list(5), s9.getValue());
    }

    @Test
    public void testTruncateString() {
        final Sequence s = Sequence.fromValue("Hello");
        final Sequence s0 = s.truncate(0);
        final Sequence s1 = s.truncate(1);
        final Sequence s2 = s.truncate(2);
        final Sequence s3 = s.truncate(-1);
        final Sequence s4 = s.truncate(-2);
        final Sequence s5 = s.truncate(s.length());
        final Sequence s6 = s.truncate(s.length()+1);
        final Sequence s7 = s.truncate(-s.length());
        final Sequence s8 = s.truncate(-s.length()-1);
        final Sequence s9 = s.truncate(-s.length()+1);
        assertEquals("", s0.getValue());
        assertEquals("H", s1.getValue());
        assertEquals("He", s2.getValue());
        assertEquals("ello", s3.getValue());
        assertEquals("llo", s4.getValue());
        assertEquals("Hello", s5.getValue());
        assertEquals("Hello", s6.getValue());
        assertEquals("", s7.getValue());
        assertEquals("", s8.getValue());
        assertEquals("o", s9.getValue());
    }
    
}
