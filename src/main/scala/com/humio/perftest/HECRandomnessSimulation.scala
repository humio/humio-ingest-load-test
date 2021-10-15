package com.humio.perftest

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZonedDateTime}
import java.util.concurrent.TimeUnit

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.util.Random

object HECRandomnessSimulation {
  val random = new Random()
  def randomAlphaNumeric = Random.alphanumeric

  def timestampStr(): String = "%.6f".format(ZonedDateTime.now().toInstant.toEpochMilli.toDouble / 1000d)

  val eventsPerBulk = Option(System.getProperty("bulksize")).getOrElse("100").toInt
  val dataspaces = Option(System.getProperty("dataspaces")).getOrElse("1").toInt
  val eventSize = Option(System.getProperty("eventsize")).getOrElse("500").toInt
  val datasourcesPerDataspace = Option(System.getProperty("datasources")).getOrElse("20").toInt
  val fieldCount = Option(System.getProperty("fields")).getOrElse("10").toInt
  val randomness = Option(System.getProperty("randomness")).getOrElse("1").toInt

  def request(): String = {
    val events =
      for (i <- 0 until eventsPerBulk/randomness) yield {
        val time = timestampStr()
        val msgFirstPart = s"${time} loglevel=INFO hello world testField100=${random.nextInt(100)} "
        val rndSize = eventSize - 1 - msgFirstPart.size
        val rndPart = randomAlphaNumeric.take(rndSize / 10 - 5).mkString("") // Make it compress somewhat:
        val rndPart10 = new StringBuilder()
        for (i <- 0 until fieldCount - 2) {
          val r = random.nextInt(10000)
          rndPart10.append(" ").append(s"arg${i}=${rndPart}${r}")
        }
        val msg = msgFirstPart + rndPart10.toString()
        val sourceFileTag = random.nextInt(datasourcesPerDataspace)
        val source = s"file${sourceFileTag}"
        val lout = new StringBuilder()
        for (i <- 1 to randomness) {
          lout.append(createEventLine(msg, time, source)+"\n")
        }
        lout.toString
      }
    events.mkString("\n")
  }

  private def createEventLine(msg: String, time: String, source: String): String = {
/*
Example line:
{"event":{"line":"Thu Jan 10 08:15:17 UTC 2019","source":"stdout","tag":"f8738433fecc"},"time":"1547108117.687114","host":"linuxkit-025000000001"}
Example json:
{"event":{"line":{"foo":"bar"},"source":"stdout","tag":"827c2e53fd2b"},"time":"1547108008.650023","host":"linuxkit-025000000001"}
 */

    s"""{"source":"${source}","event":{"line":"${msg}","tag":"f8738433fecc"},"time":"${time}","host":"perftesthost"}"""
  }
}

import HECRandomnessSimulation._
class HECRandomnessSimulation extends Simulation {

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
    //.basicAuth("", token)
    .authorizationHeader(s"Bozon ${token}")
    //.header("Authorization", "Bearer: ${token}")

  val scn = scenario("HEC ingestion") // A scenario is a chain of requests and pauses
      .during(timeInMinutes minutes) {
        feed(dataspaceFeeder).feed(requestFeeder)
        .exec(http("request_1")
          .post("/services/collector")
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
