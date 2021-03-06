/**

students: please put your implementation in this file!
  **/
package edu.gatech.cse8803.jaccard

import edu.gatech.cse8803.model._
import edu.gatech.cse8803.model.{EdgeProperty, VertexProperty}
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

object Jaccard {

  def jaccardSimilarityOneVsAll(graph: Graph[VertexProperty, EdgeProperty], patientID: Long): List[Long] = {
    /**
     Given a patient ID, compute the Jaccard similarity w.r.t. to all other patients.
     Return a List of top 10 patient IDs ordered by the highest to the lowest similarity.
     For ties, random order is okay. The given patientID should be excluded from the result.
     */

    val setA = graph
      .collectNeighborIds(EdgeDirection.Out)
      .lookup(patientID)
      .head
      .toSet

    val patientVerts = graph
      .vertices
      .filter( x =>
          x._2 match { case p: PatientProperty => true case _ => false})
      .map( _._1 )
      .filter( x => x != patientID )
      .collect()
      .toSet

    val jaccards = graph
      .collectNeighborIds(EdgeDirection.Out)
      .filter( x => patientVerts.contains(x._1))
      .map( x => (jaccard( setA, x._2.toSet), x._1))
      .sortBy(_._1, false)
      .take(10)
      .map( _._2.toLong )
      .toList

    jaccards
  }

  def patientSet(graph: Graph[VertexProperty, EdgeProperty], patientID: Long): Set[Long] = {

    val setA = graph
      .collectNeighborIds(EdgeDirection.Out)
      .lookup(patientID)
      .head
      .toSet

    setA
  }

  def jaccardSimilarityAllPatients(graph: Graph[VertexProperty, EdgeProperty]): RDD[(Long, Long, Double)] = {
    /**
    Given a patient, med, diag, lab graph, calculate pairwise similarity between all
    patients. Return a RDD of (patient-1-id, patient-2-id, similarity) where
    patient-1-id < patient-2-id to avoid duplications
    */

    /** Remove this placeholder and implement your code */
    val sc = graph.edges.sparkContext

    val patientIds = graph
      .vertices
      .filter( x =>
          x._2 match { case p: PatientProperty => true case _ => false})
      .map( _._1 )

    val patientSets = patientIds
      .collect()
      .map( x => (x, patientSet(graph, x)))
      .toMap

    val patientSetsBC = sc.broadcast(patientSets)

    val idPairs = patientIds
      .cartesian(patientIds)
      .filter( x => x._1 < x._2 )

    val jaccards = idPairs
      .map(x => (x._1, x._2, patientSetsBC.value.getOrElse(x._1, Set()),
                             patientSetsBC.value.getOrElse(x._2, Set())))
      .map(x => (x._1, x._2, jaccard(x._3, x._4)))

    jaccards
  }

  def jaccard[A](a: Set[A], b: Set[A]): Double = {
    /**
    Helper function

    Given two sets, compute its Jaccard similarity and return its result.
    If the union part is zero, then return 0.
    */

    /** Remove this placeholder and implement your code */
    val similarity = a.intersect(b).size.toDouble / a.union(b).size.toDouble

    if (similarity.isNaN) 0.0 else similarity
  }
}
