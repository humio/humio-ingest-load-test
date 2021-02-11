package com.humio.perftest

import com.humio.perftest.SimUtils.test
import org.fusesource.scalate._
import org.apache.commons.math3.distribution._

import scala.collection.mutable

object SimUtils {
  val minProbability = 0.0001
  val maxProbability = 0.999999

  //val templateEngine = new TemplateEngine

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

class SimUtils {
}

abstract class Sampleable(distribution: RealDistribution) {
  val icp0 = distribution.inverseCumulativeProbability(SimUtils.minProbability)
  val icp1 = distribution.inverseCumulativeProbability(SimUtils.maxProbability)
  val icpDif = icp1 - icp0

  def sampleDistribution = {
    val sample = distribution.sample()
    if (sample < icp0) 0
    else if (sample > icp1) 1
    else (sample - icp0) / icpDif
  }

  def sample: String
}

class IntRangeSampler(distribution: RealDistribution, min: Int, max: Int) extends Sampleable(distribution = distribution) {
  private val range = max - min
  override def sample: String = (min.toDouble + (range.toDouble * sampleDistribution).round).toInt.toString
}

class ArraySampler(distribution: RealDistribution, values:Array[String]) extends Sampleable(distribution = distribution) {
  override def sample: String = values(((values.length-1).toDouble * sampleDistribution).round.toInt)
}

class RealSampler(distribution: RealDistribution, min: Double, max: Double) extends Sampleable(distribution = distribution) {
  private val range = max - min
  override def sample: String = (min + (range * sampleDistribution)).toString
}

object SamplerTests extends App {
  val iterations = 100000

  val intRangeSampler = new IntRangeSampler(
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

  println("\nIntRangeSampler, 10 - 20, normal distribution: ")
  SimUtils.test(intRangeSampler, iterations)

  println("\nArraySampler, exponential distribution: ")
  SimUtils.test(arraySampler, iterations)

  println("\nRealSampler, 0.0 - 1.0, uniform distribution (10 iterations): ")
  SimUtils.test(realSampler, 10)

}

