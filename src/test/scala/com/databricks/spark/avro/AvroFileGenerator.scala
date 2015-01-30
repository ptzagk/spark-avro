package com.databricks.spark.avro

import java.io.File
import java.util.HashMap
import java.util.Vector
import java.nio.ByteBuffer

import scala.util.Random

import org.apache.avro._
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic._

/**
 * This object allows you to generate large avro files that can be used for speed benchmarking.
 * See README on how to use it.
 */
object AvroFileGenerator {
  
  val defaultNumberOfRecords = 1000000
  val defaultNumberOfFiles = 1
  val outputDir = "src/test/resources/avroForBenchmark/"
  val schemaPath = "src/test/resources/benchmarkSchema.avsc"
  val objectSize = 100 // Maps, arrays and strings in our generated file have this size

  def generateRandomMap(rand: Random): java.util.Map[String, Int] = {
    val jMap = new HashMap[String, Int]()
    for (i <- 0 until objectSize) {
      jMap.put(rand.nextString(5), i)
    }
    jMap
  }
  
  def generateRandomArray(rand: Random): Vector[Boolean] = {
    val vec = new Vector[Boolean]()
    for (i <- 0 until objectSize) {
      vec.add(rand.nextBoolean)
    }
    vec
  }
  
  def generateRandomByteBuffer(rand: Random): ByteBuffer = {
    val bb = ByteBuffer.allocate(objectSize)
    val arrayOfBytes = new Array[Byte](objectSize)
    rand.nextBytes(arrayOfBytes)
    bb.put(arrayOfBytes)
  }

  def generateAvroFile(numberOfRecords: Int, fileIdx: Int) = {
    val schema = new Schema.Parser().parse(new File(schemaPath))
    val outputFile = new File(outputDir + "part" + fileIdx + ".avro")
    val datumWriter = new GenericDatumWriter[GenericRecord](schema)
    val dataFileWriter = new DataFileWriter[GenericRecord](datumWriter)
    dataFileWriter.create(schema, outputFile)

    // Create data that we will put into the avro file
    val avroRec = new GenericData.Record(schema)
    val innerRec = new GenericData.Record(schema.getField("inner_record").schema())
    innerRec.put("value_field", "Inner string")
    val rand = new Random()

    var idx = 0
    while (idx < numberOfRecords) {
      avroRec.put("string", rand.nextString(objectSize))
      avroRec.put("simple_map", generateRandomMap(rand))
      avroRec.put("union_int_long_null", rand.nextInt)
      avroRec.put("union_float_double", rand.nextDouble)
      avroRec.put("inner_record", innerRec)
      avroRec.put("array_of_boolean", generateRandomArray(rand))
      avroRec.put("bytes", generateRandomByteBuffer(rand))

      dataFileWriter.append(avroRec)
      idx += 1
    }

    dataFileWriter.close()
  }

  def main(args: Array[String]) {
    var numberOfRecords = defaultNumberOfRecords
    var numberOfFiles = defaultNumberOfFiles

    if (args.size > 0) {
      numberOfRecords = args(0).toInt
    }

    if (args.size > 1) {
      numberOfFiles = args(1).toInt
    }

    new File(outputDir).mkdir() // Create directory for output files
    for (fileIdx <- 0 until numberOfFiles) {
      generateAvroFile(numberOfRecords, fileIdx)
    }
  }
}
