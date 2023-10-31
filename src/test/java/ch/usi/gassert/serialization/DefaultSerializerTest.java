package ch.usi.gassert.serialization;

import org.junit.Test;

import ch.usi.gassert.data.types.Sequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DefaultSerializerTest {

    static class ImmutableClass {
        public static final int IMMUTABLE_DATA = 20;
    }

    @Test
    public void test1() {
        final Map<String, Object> vars = new HashMap<>();
        final int obj = 2;
        DefaultSerializer.getInstance().serialize(obj, vars, true);
        assertEquals(1, vars.size());
        assertEquals(2, vars.get(""));
    }

    @Test
    public void test2() {
        final Map<String, Object> vars = new HashMap<>();
        final short obj = 2;
        DefaultSerializer.getInstance().serialize(obj, vars, true);
        assertEquals(1, vars.size());
        assertEquals((short)2, vars.get(""));
    }

    @Test
    public void test3() {
        final Map<String, Object> vars = new HashMap<>();
        final char obj = 2;
        DefaultSerializer.getInstance().serialize(obj, vars, true);
        assertEquals(0, vars.size());
    }

    @Test
    public void test4() {
        final Map<String, Object> vars = new HashMap<>();
        final Short obj = 2;
        DefaultSerializer.getInstance().serialize(obj, vars, true);
        assertEquals(1, vars.size());
        assertEquals((short)2, vars.get(""));
    }

    @Test
    public void test5() {
        final Map<String, Object> vars = new HashMap<>();
        final List<Integer> obj = new ArrayList<>();
        obj.add(1);
        obj.add(2);
        obj.add(3);
        DefaultSerializer.getInstance().serialize(obj, vars, true);
        assertEquals(1, vars.size());
        assertEquals(obj, ((Sequence)vars.get("")).getValue());
    }

    @Test
    public void test6() {
        final Map<String, Object> vars = new HashMap<>();
        final String obj = "abc";
        DefaultSerializer.getInstance().serialize(obj, vars, true);
        assertEquals(1, vars.size());
        assertEquals(obj, ((Sequence)vars.get("")).getValue());
    }

    @Test
    public void test7() {
        final Map<String, Object> vars = new HashMap<>();
        final byte[] obj = new byte[] { 0, 0, 0, 0 };
        DefaultSerializer.getInstance().serialize(obj, vars, true);
        assertEquals(1, vars.size());
        assertArrayEquals(new Byte[] { 0, 0, 0, 0 }, (Byte[])((Sequence)vars.get("")).getValue());
    }

    @Test
    public void test8() {
        final Map<String, Object> vars = new HashMap<>();
        DefaultSerializer.getInstance().serialize(ImmutableClass.class, null, vars, false);
        assertEquals(0, vars.size());
    }

}
