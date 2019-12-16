package no.divvun.pahkat.client.ffi;

import com.sun.jna.Callback;
import com.sun.jna.IntegerType;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import no.divvun.pahkat.client.PrefixPackageStore;

public class pahkat_client {
    public static class uint32_t extends IntegerType {
        public uint32_t() {
            this(0);
        }

        public uint32_t(int value) {
            super(4, value, true);
        }
    }

    public static class uint64_t extends IntegerType {
        public uint64_t() {
            this(0);
        }

        public uint64_t(int value) {
            super(8, value, true);
        }
    }

    public interface TransactionProcessCallback extends Callback {
        byte invoke(uint32_t tag, String key, uint32_t event);
    }

    // TODO: slice pointers might still actually be char pointers? fuu

    public static native Pointer pahkat_prefix_package_store_create(String path, ErrorCallback callback);

    public static native Pointer pahkat_prefix_package_store_open(String path, ErrorCallback callback);

    public static native byte pahkat_prefix_package_store_status(Pointer handle, String package_key, ErrorCallback callback);

    public static native Pointer pahkat_prefix_package_store_all_statuses(Pointer handle, String repo_record, ErrorCallback callback);

    public static native SlicePointer.ByValue pahkat_prefix_package_store_import(
            Pointer handle,
            String package_key,
            String installer_path,
            ErrorCallback callback);

    public static native void pahkat_prefix_package_store_clear_cache(
            Pointer handle, ErrorCallback callback);

    public static native void pahkat_prefix_package_store_refresh_repos(
            Pointer handle, ErrorCallback callback);

    public static native void pahkat_prefix_package_store_force_refresh_repos(
            Pointer handle, ErrorCallback callback);


    public static native Pointer pahkat_prefix_package_store_repo_indexes(Pointer handle, ErrorCallback callback);

    public static native Pointer pahkat_prefix_package_store_config(Pointer handle, ErrorCallback callback);


    public static native Pointer pahkat_prefix_package_store_find_package_by_key(Pointer handle,
                                                                                 String package_key,
                                                                                 ErrorCallback callback);

    public static native SlicePointer.ByValue pahkat_prefix_package_store_download(Pointer handle,
                                                                                   String packageKey,
                                                                                   DownloadCallback progress,
                                                                                   ErrorCallback callback);

    public static native Pointer pahkat_prefix_transaction_new(Pointer handle,
                                                               String actions,
                                                               ErrorCallback callback);

    public static native Pointer pahkat_prefix_transaction_actions(Pointer handle, ErrorCallback callback);

    public static native void pahkat_prefix_transaction_process(Pointer handle,
                                                                uint32_t tag,
                                                                TransactionProcessCallback progress,
                                                                ErrorCallback callback);

    // StoreConfig functions
    public static native SlicePointer.ByValue pahkat_store_config_config_path(Pointer handle, ErrorCallback callback);

    public static native void pahkat_store_config_set_ui_value(Pointer handle,
                                                               String key,
                                                               String value,
                                                               ErrorCallback callback);

    public static native Pointer pahkat_store_config_ui_value(Pointer handle,
                                                              String key,
                                                              ErrorCallback callback);

    public static native void pahkat_store_config_set_cache_base_url(Pointer handle,
                                                                     String path,
                                                                     ErrorCallback callback);

    public static native Pointer pahkat_store_config_cache_base_url(Pointer handle,
                                                                    ErrorCallback callback);

    public static native Pointer pahkat_store_config_skipped_package(Pointer handle,
                                                                     String package_key,
                                                                     ErrorCallback callback);

    public static native Pointer pahkat_store_config_repos(Pointer handle, ErrorCallback callback);

    public static native void pahkat_store_config_set_repos(Pointer handle,
                                                            String repos,
                                                            ErrorCallback callback);

    // General utilities

    public static native void pahkat_str_free(Pointer ptr);

    public static native void pahkat_enable_logging();

    static {
        Native.register("pahkat_client");
        pahkat_enable_logging();
    }

    public static void main(String[] args) {
        Example.INSTANCE.run();
    }
}
