package com.humio.perftest

import org.fusesource.scalate._
import org.apache.commons.math3.distribution._

import java.time.ZonedDateTime
import java.util.{ Base64, Date }
import scala.collection.mutable

// template handling

object SimTemplate {
  val minProbability = 0.0001
  val maxProbability = 0.999999

  def test(sampler: Sampleable, iterations: Int) = {
    val values = new mutable.TreeMap[String, Int]

    var i:Int = 0
    while(i < iterations) {
      val sample = sampler.sample
      values.update(sample, values.get(sample).getOrElse(0) + 1)
      i = i + 1
    }
    println(iterations + " iterations, values = " + values)
  }
}

class SimTemplate(filename:String) {
  val helper = new TemplateHelper
  val templateEngine = new TemplateEngine
  templateEngine.bindings = List(
    Binding("data", "com.humio.perftest.TemplateHelper", true),
    Binding("init", "Boolean", true))
  templateEngine.escapeMarkup = false

  // initialize replacement functions, output is sent to stdout
  val output = templateEngine.layout(filename, Map("init" -> true, "data" -> helper))
  println(output)

  def generate() = {
    // generate content
    templateEngine.layout(filename, Map("init" -> false, "data" -> helper))
  }
}

// data generation

abstract class Sampleable(distribution: RealDistribution) {
  val icp0 = distribution.inverseCumulativeProbability(SimTemplate.minProbability)
  val icp1 = distribution.inverseCumulativeProbability(SimTemplate.maxProbability)
  val icpDif = icp1 - icp0

  def sampleDistribution = {
    val sample = distribution.sample()
    if (sample < icp0) 0
    else if (sample > icp1) 1
    else (sample - icp0) / icpDif
  }

  def sample: String
}

class IntSampler(distribution: RealDistribution, min: Int, max: Int) extends Sampleable(distribution = distribution) {
  private val range = max - min
  override def sample: String = (min.toDouble + (range.toDouble * sampleDistribution).round).toInt.toString
}

class ArraySampler(distribution: RealDistribution, values:Array[String]) extends Sampleable(distribution = distribution) {
  override def sample: String = values(((values.length-1).toDouble * sampleDistribution).round.toInt)
}

class RealSampler(distribution: RealDistribution, min: Double, max: Double, precision: Int = 4) extends Sampleable(distribution = distribution) {
  private val range = max - min
  def truncateAt(n: Double, p: Int): Double = { val s = math pow (10, p); (math floor n * s) / s }
  override def sample: String = truncateAt((min + (range * sampleDistribution)), precision).toString
}

// template context class

class TemplateHelper {
  // dates, timestamps
  import java.text.DateFormat
  import java.text.SimpleDateFormat
  import java.util.TimeZone
  val tz: TimeZone = TimeZone.getTimeZone("UTC")
  val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'") // Quoted "Z" to indicate UTC, no timezone offset

  // hashing
  val md = java.security.MessageDigest.getInstance("SHA-1")

  // internal template initialization flag
  var init:Boolean = true

  // distribution-based sampling
  val samplers = new mutable.HashMap[String, Sampleable]

  // sample from a registered sampler
  def sample(name:String): String = {
    samplers.get(name).get.sample
  }

  // register a sampler
  def register(name: String, sampler: Sampleable) = {
    samplers.update(name, sampler)
  }

  // simple dynamic content generation
  def timestamp(): String = "%.6f".format(ZonedDateTime.now().toInstant.toEpochMilli.toDouble / 1000d)
  def iso8601Timestamp() = df.format(new Date())
  def sha1(s: String) = Base64.getEncoder.encodeToString(md.digest(s.getBytes))
}

// things that should be real tests of some kind

object SamplerTests extends App {
  val iterations = 100000

  val intSampler = new IntSampler(
    new NormalDistribution(),
    10,
    20)

  val arraySampler = new ArraySampler(
    new ExponentialDistribution(1),
    Array("GET", "POST", "PUT", "DELETE", "OPTION")
  )

  val realSampler = new RealSampler(
    new UniformRealDistribution(),
    0.0,
    1.0
  )

  println("\nIntSampler, 10 - 20, normal distribution: ")
  SimTemplate.test(intSampler, iterations)

  println("\nArraySampler, 5 elements, exponential distribution: ")
  SimTemplate.test(arraySampler, iterations)

  println("\nRealSampler, 0.0 - 1.0, uniform distribution: ")
  SimTemplate.test(realSampler, 10)
}

object TemplateTests extends App {
  val simTemplate = new SimTemplate("templates/test.ssp")
  val t0 = System.currentTimeMillis()
  var i = 0
  while(i < 10) {
    val output = simTemplate.generate
    println(output)
    i = i + 1
  }
  val t1 = System.currentTimeMillis()
  println("\n\n" + i + " iterations, " + (t1 - t0) + "ms elapsed")
}