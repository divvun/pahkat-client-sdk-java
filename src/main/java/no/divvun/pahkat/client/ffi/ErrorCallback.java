package no.divvun.pahkat.client.ffi;

import com.sun.jna.Callback;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

public interface ErrorCallback extends Callback {
    void invoke(Pointer error, pahkat_client.uint64_t size);
}
