package com.inmobi.grill.lib.query;

/*
 * #%L
 * Grill Query Library
 * %%
 * Copyright (C) 2014 Inmobi
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * Zip file formatter.
 * 
 * Creates a zip on hadoop compatible file system, with ability to split output
 * across multiple part files and provide a final zip output file.
 * 
 */
public class ZipFileFormatter extends AbstractFileFormatter {
  public static String PART_SUFFIX = "_part-";
  private Path tmpPath;
  private ZipOutputStream zipOut;
  private FileSystem fs;
  private String resultFileExtn;
  private int currentPart = 0;
  private OutputStreamWriter out;
  private long maxSplitRows;
  private String encoding;
  boolean closed = false;

  public void setupOutputs() throws IOException {
    resultFileExtn = ctx.getOuptutFileExtn();
    maxSplitRows = ctx.getMaxResultSplitRows();

    String pathStr = ctx.getResultSetParentDir();
    if (StringUtils.isBlank(pathStr)) {
      throw new IllegalArgumentException("No output path specified");
    }
    finalPath = new Path(pathStr, ctx.getQueryHandle().toString() + ".zip");
    tmpPath = new Path(pathStr, ctx.getQueryHandle().toString() + ".tmp.zip");

    fs = finalPath.getFileSystem(ctx.getConf());

    zipOut = new ZipOutputStream((fs.create(tmpPath)));
    ZipEntry zipEntry = new ZipEntry(getQueryResultFileName());
    zipOut.putNextEntry(zipEntry);
    encoding = ctx.getResultEncoding();
    // Write the UTF-16LE BOM (FF FE)
    if (encoding.equals(GrillFileOutputFormat.UTF16LE)) {
      zipOut.write(0xFF);
      zipOut.write(0xFE);
      out = new OutputStreamWriter(zipOut, encoding);
    } else {
      out = new OutputStreamWriter(zipOut, encoding);
    }
    System.out.println("Setup outputs done");
  }

  private String getQueryResultFileName() {
    return ctx.getQueryHandle().toString() + PART_SUFFIX + currentPart + resultFileExtn;
  }

  @Override
  public void commit() throws IOException {
    close();
    fs.rename(tmpPath, finalPath);
    finalPath = finalPath.makeQualified(fs);
    ctx.setResultSetPath(getFinalOutputPath());
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      out.flush();
      zipOut.closeEntry();
      zipOut.close();
      out.close();
      closed = true;
    }
  }

  private String cachedHeader = null;
  public void writeHeader(String header) throws IOException {
    out.write(header);
    out.write("\n");
    this.cachedHeader = header;
  }

  public void writeFooter(String footer) throws IOException {
    out.write(footer);
    out.write("\n");
  }

  public void writeRow(String row) throws IOException {
    // close zip entry and add new one, if numRows has crossed max rows in the
    // cuurent file
    if (numRows != 0 && numRows % maxSplitRows == 0) {
      currentPart++;
      out.flush();
      zipOut.closeEntry();

      // Making new zip-entry.
      ZipEntry zipEntry = new ZipEntry(getQueryResultFileName());
      zipOut.putNextEntry(zipEntry);
      if (encoding.equals(GrillFileOutputFormat.UTF16LE)) {
        zipOut.write(0xFF);
        zipOut.write(0xFE);
      }
      writeHeader();
    }
    out.write(row);
    out.write("\n");
    numRows++;
  }

  @Override
  public void writeHeader() throws IOException {
    if (cachedHeader != null) {
      writeHeader(cachedHeader);
    }
  }

  @Override
  public Path getTmpPath() {
    return tmpPath;
  }

  @Override
  public String getEncoding() {
    return out.getEncoding();
  }

}
