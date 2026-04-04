package jsonschema.excel

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater

/** Generates a solid-colour PNG icon at the requested pixel size. */
object ExcelIcon:

  def png(size: Int): Array[Byte] =
    val ihdr = int32(size) ++ int32(size) ++ Array(8, 2, 0, 0, 0).map(_.toByte)
    val out = ByteArrayOutputStream()
    out.write(Array(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a).map(_.toByte))
    out.write(chunk("IHDR", ihdr))
    out.write(chunk("IDAT", deflate(scanlines(size))))
    out.write(chunk("IEND", Array.emptyByteArray))
    out.toByteArray()

  // ── Helpers ─────────────────────────────────────────────────────────────────

  // Excel-green #217346 scanlines: filter-byte 0 followed by RGB pixels per row
  private def scanlines(size: Int): Array[Byte] =
    val raw = ByteArrayOutputStream(size * (1 + size * 3))
    (0 until size).foreach: _ =>
      raw.write(0)
      (0 until size).foreach: _ =>
        raw.write(0x21); raw.write(0x73); raw.write(0x46)
    raw.toByteArray()

  private def deflate(data: Array[Byte]): Array[Byte] =
    val d = Deflater()
    val buf = Array.ofDim[Byte](8192)
    val out = ByteArrayOutputStream()
    d.setInput(data)
    d.finish()
    while !d.finished() do out.write(buf, 0, d.deflate(buf))
    out.toByteArray()

  private def chunk(tag: String, data: Array[Byte]): Array[Byte] =
    val tagBytes = tag.getBytes("US-ASCII")
    val crc = CRC32()
    crc.update(tagBytes)
    crc.update(data)
    int32(data.length) ++ tagBytes ++ data ++ int32(crc.getValue.toInt)

  private def int32(n: Int): Array[Byte] =
    Array((n >> 24).toByte, (n >> 16).toByte, (n >> 8).toByte, n.toByte)
