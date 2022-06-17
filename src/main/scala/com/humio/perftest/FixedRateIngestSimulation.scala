package com.humio.perftest

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZonedDateTime}
import java.util.concurrent.TimeUnit

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.util.Random

object FixedRateIngestSimulation {
  val random = new Random()

  def randomAlphaNumeric = Random.alphanumeric

  val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

  def timestampStr(): String = dateTimeFormatter.format(ZonedDateTime.now())

  val indexline: String = {
    s"""{"index":{"_index":"filebeat-2016.12.21","_type":"log"}}"""
  }

  val eventsPerRequest = 100
  val eventSize = 500
  val dataspaces = 1
  val datasourcesPerDataspace = 5

  def request(): String = {
    val time = timestampStr()
    val events =
      for (i <- 0 until eventsPerRequest) yield {
        val msgFirstPart = s"${time} loglevel=INFO hello world testField100=${random.nextInt(100)} "
        val rndSize = eventSize - 1 - msgFirstPart.size
        val rndPart = randomAlphaNumeric.take(rndSize / 10).mkString("") // Make it compress somewhat:
        val rndPart10 = rndPart + rndPart + rndPart + rndPart + rndPart + rndPart + rndPart + rndPart + rndPart + rndPart
        val msg = msgFirstPart + rndPart10
        val source = s"logfile${random.nextInt(datasourcesPerDataspace)}"
        s"${indexline}\n${createEventJson(msg, Seq("@source"), Map("@source" -> source))}"
      }
    events.mkString("\n")
  }

  private def createEventJson(msg: String, tagFields: Seq[String], fields: Map[String, String]): String = {

    val tagFieldsStr =
      if (tagFields.isEmpty) {
        ""
      } else {
        """"@tags":[""" + tagFields.map(s => "\"" + s + "\"").mkString(",") + "], "
      }

    val initStr = s"""{${tagFieldsStr}"@type": "perftest-parser""""
    val fieldsJson =
      fields.foldLeft(initStr) { case (str, (key, value)) =>
        str + s""", "${key}": "${value}""""
      } + "}"

    val json =
      s"""{
         |  "@timestamp": "${Instant.ofEpochMilli(System.currentTimeMillis())}",
         |  "beat": {
         |    "hostname": "testhostname",
         |    "name": "testhostname",
         |    "version": "5.0.2"
         |  },
         |  "input_type": "log",
         |  "message": "$msg",
         |  "offset": 45,
         |  "source": "test.log",
         |  "type": "log",
         |  "fields": $fieldsJson
         |}
         |""".stripMargin.replace("\n", "")
    json
  }
}

import com.humio.perftest.FixedRateIngestSimulation._

class FixedRateIngestSimulation extends Simulation {

  val dataspaceFeeder = Iterator.continually(Map("dataspace" -> ("dataspace" + random.nextInt(dataspaces))))
  val requestFeeder = Iterator.continually(Map("request" -> request()))

  val tensGbPerDay = Option(System.getProperty("tensGbPerDay")).getOrElse("10").toInt
  val timeInMinutes = Option(System.getProperty("time")).getOrElse("120").toInt
  val token = Option(System.getProperty("token")).getOrElse("developer")

  val baseUrls = Option(System.getProperty("baseurls"))
    .map { str => str.split(",").toList }
    .getOrElse(List("https://testcloud01.humio.com", "https://testcloud02.humio.com", "https://testcloud04.humio.com", "https://testcloud02.humio.com", "https://testcloud04.humio.com"))

  println(s"configured tensGbPerDay: $tensGbPerDay")
  println(s"configured time: $timeInMinutes minutes")
  println(s"token: XXXXXXXnotprinting")
  println(s"base urls: ${baseUrls}")


  override def before(step: => Unit): Unit = {
    super.before(step)
  }

  val httpConf = http
    .baseUrls(baseUrls) // Here is the root for all relative URLs
    .contentTypeHeader("application/json")
    .acceptHeader("application/json") // Here are the common headers
    .header("Content-Encoding", "gzip") // Matches the processRequestBody(gzipBody)
    .acceptEncodingHeader("*") // "*" or "gzip" or "deflate" or "compress" or "identity"
    .userAgentHeader("gatling client")
    .basicAuth(s"$token", "")  
    // .header("Authorization", "Bearer: ${token}")
  //.authorizationHeader("Bearer qcSmluq1kkS9xuheGLdFagWRuEBpD5gu")
  //.header("Authorization", "Bearer: qcSmluq1kkS9xuheGLdFagWRuEBpD5gu")

  val scn = scenario("filebeat ingestion") // A scenario is a chain of requests and pauses
    .during(timeInMinutes minutes) {
    feed(dataspaceFeeder).feed(requestFeeder)
      .pace(Duration(402, MILLISECONDS))
      .exec(http("request_1")
        .post("/api/v1/ingest/elastic-bulk")
        .body(StringBody("${request}"))
        .processRequestBody(gzipBody)
        .check(status.is(200))
      )
  }

  setUp(
    scn.inject(
      //constantUsersPerSec(1) during (10 minutes)
      rampUsers(tensGbPerDay) during (5 seconds)
      //atOnceUsers(100)
      //constantUsersPerSec(50) during(15 minutes)
    )
      .protocols(httpConf)
  )
}
