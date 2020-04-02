package no.divvun.pahkat.client.ffi;

import com.sun.jna.Callback;
import org.jetbrains.annotations.NotNull;

public interface DownloadCallback extends Callback {
    boolean invoke(
            @NotNull String packageId,
            @NotNull pahkat_client.uint64_t current,
            @NotNull pahkat_client.uint64_t total);
}
