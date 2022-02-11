package com.humio.perftest

import java.time.format.DateTimeFormatter
import java.time.{ Instant, ZonedDateTime }
import java.util.concurrent.TimeUnit
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.apache.commons.math3.distribution.UniformRealDistribution
import org.fusesource.scalate.{ Binding, TemplateEngine }

import scala.concurrent.duration._
import scala.util.Random

object HECTemplateSimulation {
  val random = new Random()
  val eventsPerBulk = Option(System.getProperty("bulksize")).getOrElse("100").toInt
  val dataspaces = Option(System.getProperty("dataspaces")).getOrElse("1").toInt
  val templateFile = Option(System.getProperty("template")).getOrElse("templates/test.ssp")
  val simTemplate = new SimTemplate(templateFile)

  def request(): String = (for (i <- 0 until eventsPerBulk) yield simTemplate.generate).mkString("\n")
}

import HECTemplateSimulation._
class HECTemplateSimulation extends Simulation {
  val dataspaceFeeder = Iterator.continually(Map("dataspace" -> ("dataspace" + random.nextInt(dataspaces))))
  val requestFeeder = Iterator.continually(Map("request" -> request()))
  val users = Option(System.getProperty("users")).getOrElse("3").toInt
  val timeInMinutes = Option(System.getProperty("time")).getOrElse("300").toInt
  val token = Option(System.getProperty("token")).getOrElse("developer")
  val meanPauseDurationMs = Option(System.getProperty("pausetime")).getOrElse("10").toInt
  val baseUrlString = Option(System.getProperty("baseurls")).getOrElse("https://testcloud01.humio.com")
  val baseUrls = baseUrlString.split(",").toList

  println(s"Configuration:\n")
  println(s"users=$users")
  println(s"time=$timeInMinutes minutes")
  println(s"token=$token")
  println(s"baseurls=${baseUrlString}  (Comma-separated)")
  println(s"dataspaces=$dataspaces")
  println(s"bulksize=$eventsPerBulk")
  println(s"template=$templateFile")
  println(s"meanPauseDurationMs=$meanPauseDurationMs")

  override def before(step: => Unit): Unit = super.before(step)

  val httpConf = http
    .baseUrls(baseUrls)
    .contentTypeHeader("text/plain; charset=utf-8")
    .acceptHeader("application/json")
    .header("Content-Encoding", "gzip")
    .acceptEncodingHeader("*")
    .userAgentHeader("gatling client")
    .authorizationHeader(s"Bearer ${token}")

  val scn = scenario("HEC ingestion")
    .during(timeInMinutes minutes) {
      feed(dataspaceFeeder).feed(requestFeeder)
        .exec(http("request_1")
          .post("/api/v1/ingest/hec")
          .body(StringBody("${request}"))
          .processRequestBody(gzipBody)
          .check(status.is(200))
        ).pause(Duration(meanPauseDurationMs, TimeUnit.MILLISECONDS))
    }

  setUp(
    scn.inject(
      rampUsers(users) during(5 seconds)
    )
      .exponentialPauses
      .protocols(httpConf)
  )
}
