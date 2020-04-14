package no.divvun.pahkat.client.ffi;

import com.sun.jna.Callback;
import com.sun.jna.IntegerType;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

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
        byte invoke(uint32_t tag, SlicePointer.ByValue key, uint32_t event);
    }

    public static native Pointer pahkat_prefix_package_store_create(SlicePointer.ByValue path, ErrorCallback callback);

    public static native Pointer pahkat_prefix_package_store_open(SlicePointer.ByValue path, ErrorCallback callback);

    public static native Pointer pahkat_prefix_package_store_open_or_create(SlicePointer.ByValue path, ErrorCallback callback);

    public static native byte pahkat_prefix_package_store_status(Pointer handle, SlicePointer.ByValue package_key, ErrorCallback callback);

    public static native SlicePointer.ByValue pahkat_prefix_package_store_all_statuses(Pointer handle, SlicePointer repo_record, ErrorCallback callback);

    public static native SlicePointer.ByValue pahkat_prefix_package_store_import(
            Pointer handle,
            SlicePointer package_key,
            SlicePointer installer_path,
            ErrorCallback callback);

    public static native void pahkat_prefix_package_store_clear_cache(
            Pointer handle, ErrorCallback callback);

    public static native void pahkat_prefix_package_store_refresh_repos(
            Pointer handle, ErrorCallback callback);

    public static native void pahkat_prefix_package_store_force_refresh_repos(
            Pointer handle, ErrorCallback callback);


//    public static native Pointer pahkat_prefix_package_store_repo_indexes(Pointer handle, ErrorCallback callback);

    public static native Pointer pahkat_prefix_package_store_config(Pointer handle, ErrorCallback callback);


    public static native SlicePointer.ByValue pahkat_prefix_package_store_find_package_by_key(Pointer handle,
                                                                                 SlicePointer.ByValue package_key,
                                                                                 ErrorCallback callback);

    public static native SlicePointer.ByValue pahkat_prefix_package_store_download(Pointer handle,
                                                                                   SlicePointer.ByValue packageKey,
                                                                                   DownloadCallback progress,
                                                                                   ErrorCallback callback);

    public static native Pointer pahkat_prefix_transaction_new(Pointer handle,
                                                               SlicePointer.ByValue actions,
                                                               ErrorCallback callback);

//    public static native Pointer pahkat_prefix_transaction_actions(Pointer handle, ErrorCallback callback);

    public static native void pahkat_prefix_transaction_process(Pointer handle,
                                                                uint32_t tag,
                                                                TransactionProcessCallback progress,
                                                                ErrorCallback callback);

    // StoreConfig functions

    public static native SlicePointer.ByValue pahkat_config_settings_config_dir(Pointer handle, ErrorCallback callback);

    public static native SlicePointer.ByValue pahkat_config_repos_get(Pointer handle, ErrorCallback callback);

    public static native void pahkat_config_repos_set(Pointer handle,
                                                      SlicePointer.ByValue repos,
                                                      ErrorCallback callback);

//    // General utilities

    public static native void pahkat_str_free(SlicePointer.ByValue ptr);

    public static native void pahkat_enable_logging(byte level);

    public static native void pahkat_android_init(SlicePointer.ByValue container_path, ErrorCallback callback);

    static {
        Native.register(pahkat_client.class, "pahkat_client");
    }

//    public static void main(SlicePointer.ByValue[] args) {
//        Example.INSTANCE.run();
//    }
}
