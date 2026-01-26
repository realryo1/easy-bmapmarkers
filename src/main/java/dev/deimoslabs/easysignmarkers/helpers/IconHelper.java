package dev.deimoslabs.easysignmarkers.helpers;

import java.io.*;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class IconHelper {

    private static final String IMAGE_PATH = "markers/";
    private static final String RES_FOLDER = IMAGE_PATH + "dynmap";

    public static boolean copyMarkers(JarFile jar, Path webRoot, Logger logger) throws IOException {

        File targetRoot = webRoot.resolve(IMAGE_PATH).toFile();
        if (!targetRoot.exists() && !targetRoot.mkdirs()) {
            logger.warning("Cannot create folder: " + targetRoot.getAbsolutePath());
            return false;
        }

        Enumeration<JarEntry> entries = jar.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if (!entry.isDirectory()
                    && entryName.startsWith(RES_FOLDER + "/")
                    && entryName.endsWith(".png")) {

                Path entryPath = Path.of(entryName);
                Path relative = entryPath.subpath(2, entryPath.getNameCount());
                File outFile = webRoot.resolve(IMAGE_PATH).resolve(relative).toFile();

                if (outFile.exists()) {
                    continue;
                }

                if (!outFile.getParentFile().exists()) {
                    outFile.getParentFile().mkdirs();
                }

                try (InputStream in = jar.getInputStream(entry)) {
                    if (in == null) {
                        logger.warning("❌ Cannot read file from JAR: " + entryName);
                        continue;
                    }

                    try (OutputStream out = new FileOutputStream(outFile)) {
                        in.transferTo(out);
                    }
                }
            }
        }
        return true;
    }

}
