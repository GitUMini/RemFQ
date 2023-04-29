package ki.zq.remfq.ocr;

import java.io.File;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

public class FileUtil {
    public static File getSaveFile(@NotNull Context context) {
        return new File(context.getFilesDir(), "pic.jpg");
    }
}
