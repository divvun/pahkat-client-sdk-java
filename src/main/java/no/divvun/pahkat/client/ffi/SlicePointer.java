package no.divvun.pahkat.client.ffi;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Structure.FieldOrder({ "data", "len" })
public class SlicePointer extends Structure {
    public volatile Pointer data;
    public volatile NativeLong len;

    public static class ByValue extends SlicePointer implements Structure.ByValue {}

    @Nullable public String decode() {
        int v = len.intValue();

        if (v == 0 || data == Pointer.NULL) {
            return null;
        }

        byte[] array = data.getByteArray(0, v);
        return new String(array, StandardCharsets.UTF_8);
    }
}

