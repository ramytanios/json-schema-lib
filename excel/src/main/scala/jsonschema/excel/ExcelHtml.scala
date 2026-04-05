package jsonschema.excel

import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Deflater

object ExcelHtml:

  /** Minimal host page that bootstraps the custom-functions JS runtime. */
  val functionsHtml: String =
    """|<!DOCTYPE html>
       |<html>
       |  <head>
       |    <meta charset="UTF-8"/>
       |    <script src="https://appsforoffice.microsoft.com/lib/1/hosted/office.js"></script>
       |  </head>
       |  <body>
       |    <script src="/functions.js"></script>
       |  </body>
       |</html>
       |""".stripMargin

  /** Task-pane reference page listing all registered functions. */
  def taskpaneHtml(namespace: String): String =
    val nsPrefix = if namespace.nonEmpty then s"$namespace." else ""
    s"""|<!DOCTYPE html>
        |<html>
        |<head>
        |  <meta charset="UTF-8"/>
        |  <meta name="viewport" content="width=device-width, initial-scale=1"/>
        |  <script src="https://appsforoffice.microsoft.com/lib/1/hosted/office.js"></script>
        |  <link rel="stylesheet" href="/taskpane.css"/>
        |</head>
        |<body>
        |  <div id="header">Function Reference</div>
        |  <div id="search-box">
        |    <input id="q" type="text" placeholder="Search functions…" oninput="doFilter()"/>
        |  </div>
        |  <button id="reload-btn" onclick="reload()">Reload Functions</button>
        |  <div id="count"></div>
        |  <div id="list"></div>
        |  <div id="empty">No matching functions.</div>
        |  <script>const NS_PREFIX = "$nsPrefix";</script>
        |  <script src="/taskpane.js"></script>
        |</body>
        |</html>
        |""".stripMargin

  val taskpaneCss: String =
    scala.io.Source.fromInputStream(
      getClass.getResourceAsStream("/taskpane.css")
    ).mkString

  val taskpaneJs: String =
    scala.io.Source.fromInputStream(
      getClass.getResourceAsStream("/taskpane.js")
    ).mkString

  /** Generates a solid green (#217346) PNG of the given size using only java.util.zip. */
  def iconPng(size: Int): Array[Byte] =
    def chunk(tag: String, data: Array[Byte]): Array[Byte] =
      val crc = CRC32()
      val tagBytes = tag.getBytes("US-ASCII")
      crc.update(tagBytes)
      crc.update(data)
      ByteBuffer.allocate(4 + 4 + data.length + 4)
        .putInt(data.length)
        .put(tagBytes)
        .put(data)
        .putInt(crc.getValue.toInt)
        .array()

    val ihdr = ByteBuffer.allocate(13)
      .putInt(size).putInt(size)
      .put(8: Byte) // bit depth
      .put(2: Byte) // color type: RGB
      .put(0: Byte).put(0: Byte).put(0: Byte)
      .array()

    // One scanline = filter byte (0) + size * 3 RGB bytes
    val raw = Array.ofDim[Byte](size * (1 + size * 3))
    for y <- 0 until size do
      val row = y * (1 + size * 3)
      raw(row) = 0 // filter: None
      for x <- 0 until size do
        val px = row + 1 + x * 3
        raw(px) = 0x21.toByte; raw(px + 1) = 0x73.toByte; raw(px + 2) = 0x46.toByte

    val deflater = Deflater()
    deflater.setInput(raw)
    deflater.finish()
    val buf = Array.ofDim[Byte](raw.length + 64)
    val n = deflater.deflate(buf)
    deflater.end()

    val sig = Array[Byte](0x89.toByte, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
    sig ++ chunk("IHDR", ihdr) ++ chunk("IDAT", buf.take(n)) ++ chunk("IEND", Array.empty)
