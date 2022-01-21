package com.humio.perftest

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZonedDateTime}
import java.util.concurrent.TimeUnit

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.util.Random

object FilebeatSimulation {
  val random = new Random()
  def randomAlphaNumeric = Random.alphanumeric
  
  val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  
  def timestampStr(): String = dateTimeFormatter.format(ZonedDateTime.now())

  val indexline: String = {
    s"""{"index":{"_index":"filebeat-2016.12.21","_type":"log"}}"""
  }
  
  val eventsPerBulk = Option(System.getProperty("bulksize")).getOrElse("100").toInt
  val dataspaces = Option(System.getProperty("dataspaces")).getOrElse("1").toInt
  val eventSize = Option(System.getProperty("eventsize")).getOrElse("500").toInt
  val datasourcesPerDataspace = Option(System.getProperty("datasources")).getOrElse("20").toInt
  val fieldCount = Option(System.getProperty("fields")).getOrElse("10").toInt

  def request(): String = {
    val sourceFileTag = random.nextInt(datasourcesPerDataspace)
    val source = s"file${sourceFileTag}"
    val time = timestampStr()
    val events =
      for (i <- 0 until eventsPerBulk) yield {
        val msgFirstPart = s"${time} loglevel=INFO hello world testField100=${random.nextInt(100)} "
        val rndSize = eventSize - 1 - msgFirstPart.size
        val rndPart = randomAlphaNumeric.take(rndSize / 10 - 5).mkString("") // Make it compress somewhat:
        val rndPart10 = new StringBuilder()
        for (i <- 0 until fieldCount - 2) {
          val r = random.nextInt(10000)
          rndPart10.append(" ").append(s"arg${i}=${rndPart}${r}")
        }
        val msg = msgFirstPart + rndPart10.toString()
        s"${indexline}\n${createEventJson(msg, Map("source" -> source))}"
      }
    events.mkString("\n")
  }

  private def createEventJson(msg: String, fields: Map[String, String]): String = {

    val initStr = s"""{"type": "perftest-parser""""
    val fieldsJson =
      fields.foldLeft(initStr) {case (str, (key, value)) =>
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

import FilebeatSimulation._
class FilebeatSimulation extends Simulation {

  val dataspaceFeeder = Iterator.continually(Map("dataspace" -> ("dataspace" + random.nextInt(dataspaces))))
  val requestFeeder = Iterator.continually(Map("request" -> request()))
  
  val users = Option(System.getProperty("users")).getOrElse("3").toInt
  val timeInMinutes = Option(System.getProperty("time")).getOrElse("300").toInt
  val token = Option(System.getProperty("token")).getOrElse("developer")

  val baseUrlString = Option(System.getProperty("baseurls")).getOrElse("https://testcloud01.humio.com")

  val baseUrls = baseUrlString.split(",").toList

  println(s"configured users=$users")
  println(s"configured time=$timeInMinutes minutes")
  println(s"token=$token")
  println(s"baseurls=${baseUrlString}  (Comma-separated)")
  println(s"fields=$fieldCount")
  println(s"datasources=$datasourcesPerDataspace")
  println(s"eventsize=$eventSize")
  println(s"dataspaces=$dataspaces")
  println(s"bulksize=$eventsPerBulk")

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
    //.basicAuth("qcSmluq1kkS9xuheGLdFagWRuEBpD5gu", "")
    .basicAuth(s"$token", "")  
    //.authorizationHeader("Bearer qcSmluq1kkS9xuheGLdFagWRuEBpD5gu")  
    //.header("Authorization", "Bearer: qcSmluq1kkS9xuheGLdFagWRuEBpD5gu")

  val scn = scenario("filebeat ingestion") // A scenario is a chain of requests and pauses
      .during(timeInMinutes minutes) {
        feed(dataspaceFeeder).feed(requestFeeder)
        .exec(http("request_1")
          .post("/api/v1/dataspaces/${dataspace}/ingest/elasticsearch")
          .body(StringBody("${request}"))
          .processRequestBody(gzipBody)
          .check(status.is(200))
        )
        .pause(Duration(10, TimeUnit.MILLISECONDS))
      }

  setUp(
    scn.inject(
      //constantUsersPerSec(1) during (10 minutes)
      rampUsers(users) during(5 seconds)
      //atOnceUsers(100)
      //constantUsersPerSec(50) during(15 minutes)
    )
    .protocols(httpConf)
  )
}
