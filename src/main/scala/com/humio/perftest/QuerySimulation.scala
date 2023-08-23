package com.humio.perftest

import java.util.concurrent.TimeUnit

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.util.Random

object QuerySimulation {
  val random = new Random()

  val dataspaces = 1

}

import com.humio.perftest.QuerySimulation._

class QuerySimulation extends Simulation {


  val timeInMinutes = Option(System.getProperty("time")).getOrElse("120").toInt

  val repo = Option(System.getProperty("repo")).getOrElse("humio")

  val dataspaceFeeder = Iterator.continually(Map("dataspace" -> ("dataspace" + random.nextInt(dataspaces)), "repo" -> repo))

  val searchQuery = Option(System.getProperty("searchQuery")).getOrElse("count()")
  val searchDuration = Option(System.getProperty("searchDuration")).getOrElse("24hours")
  val token = Option(System.getProperty("token")).getOrElse("developer")

  val baseUrls = Option(System.getProperty("baseurls"))
    .map { str => str.split(",").toList }
    .getOrElse(List("https://testcloud01.humio.com", "https://testcloud02.humio.com", "https://testcloud04.humio.com", "https://testcloud02.humio.com", "https://testcloud04.humio.com"))

  println(s"configured time: $timeInMinutes minutes")
  println(s"base urls: ${baseUrls}")


  override def before(step: => Unit): Unit = {
    super.before(step)
  }

  val httpConf = http
    .baseUrls(baseUrls)
    .contentTypeHeader("application/json")
    .acceptHeader("application/json") // Here are the common headers
    .header("Content-Encoding", "gzip") // Matches the processRequestBody(gzipBody)
    .acceptEncodingHeader("*") // "*" or "gzip" or "deflate" or "compress" or "identity"
    .authorizationHeader(s"Bearer ${token}")
    .userAgentHeader("gatling client")

  val countScenario = scenario("Create count query") // A scenario is a chain of requests and pauses
    .during(timeInMinutes minutes) {
    feed(dataspaceFeeder)
      .pace(Duration(1, TimeUnit.MINUTES))
      .exec(_.set("queryDone", "false"))
      .exec(http("create_queryjob")
        .post("/api/v1/dataspaces/${repo}/queryjobs")
        .body(StringBody(s"""{"queryString": "${searchQuery}", "start": "${searchDuration}"}"""))
        .processRequestBody(gzipBody)
        .check(status.is(200))
        .check(jsonPath("$.id").saveAs("queryJobId"))
      )
      .asLongAs(session => session("queryDone").as[String] != "true") {
        exec(http("lookup_queryjob")
          .get("/api/v1/dataspaces/${repo}/queryjobs/${queryJobId}")
          .check(jsonPath("$.done").saveAs("queryDone")
          )
        )
          .pause(Duration(1, TimeUnit.SECONDS))
      }
  }

  setUp(
    countScenario.inject(
      //constantUsersPerSec(1) during (10 minutes)
      rampUsers(1) during (5 seconds)
      //atOnceUsers(100)
      //constantUsersPerSec(50) during(15 minutes)
    )
      .protocols(httpConf)
  )
}
