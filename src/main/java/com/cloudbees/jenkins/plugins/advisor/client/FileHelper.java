package com.cloudbees.jenkins.plugins.advisor.client;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FileHelper {

  private static final Logger LOG = Logger.getLogger(FileHelper.class.getName());

  private FileHelper() {
    throw new UnsupportedOperationException("Unable to instantiate class");
  }

  public static String getFileMetadata(File file) {
    try {
      Path path = file.toPath();
      boolean isDirectory = path.toFile().isDirectory();
      boolean isHidden = Files.isHidden(path);
      boolean isReadable = Files.isReadable(path);
      boolean isRegularFile = path.toFile().isFile();
      boolean isSymbolicLink = Files.isSymbolicLink(path);
      boolean isWritable = Files.isWritable(path);

      return String.format(
        "isDirectory: [%s], isHidden: [%s], isReadable: [%s], isRegularFile: [%s], isSymbolicLink: [%s], isWritable: [%s]",
        isDirectory, isHidden, isReadable, isRegularFile, isSymbolicLink, isWritable);
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Unable to retrieve file metadata", e.getCause());
      return "File metadata unavailable";
    }
  }
}
